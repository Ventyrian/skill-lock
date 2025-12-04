package com.skilllock;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.WorldType;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.events.WorldChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@PluginDescriptor(
	name = "Skill Lock"
)
public class SkillLockPlugin extends Plugin
{
	@Inject
	private Client client;

    @Inject
    private ChatboxPanelManager chatboxPanelManager;

    @Inject
    private ClientThread clientThread;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private SkillLockOverlay overlay;

    @Inject
    private ConfigManager configManager;

    private boolean skillsTabWasOpen = false;
    private boolean needsLocationUpdate = false;
    public ArrayList<SkillLocation> skillLocations =  new ArrayList<>();
    public boolean membersWorld = false;
    private final String[] SKILL_NAMES = {"attack","hitpoints","mining","strength","agility","smithing","defense","herblore","fishing","ranged","thieving","cooking","prayer","crafting","firemaking","magic","fletching","woodcutting","runecraft","slayer","farming","construction","hunter","sailing"};


    @Override
	protected void startUp() throws Exception
	{
		log.debug("Skill Lock started!");
        skillsTabWasOpen = false;
        needsLocationUpdate = false;
        skillLocations.clear();
        updateWorldType();
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.debug("Skill Lock stopped!");
        overlayManager.remove(overlay);
	}

    private void checkAndUpdateOverlay()
    {
        boolean isOpen = isSkillsTabOpen();

        if (isOpen && !skillsTabWasOpen)
        {
            skillLocations = createSkillLocations(); // ← Recalculate fresh
            overlayManager.add(overlay);
            log.debug("Skills tab opened → overlay added + locations recalculated");
        }
        else if (!isOpen && skillsTabWasOpen)
        {
            overlayManager.remove(overlay);
            log.debug("Skills tab closed → overlay removed");
        }

        skillsTabWasOpen = isOpen;
    }

    public Widget getSkillWidget()
    {
        return client.getWidget(WidgetInfo.SKILLS_CONTAINER);
    }

    @Data
    public static class SkillLocation
    {
        public final String name;
        public final int x;
        public final int y;
        public final boolean isLocked;
        public final int level;
    }

    public ArrayList<SkillLocation> createSkillLocations() {


        final Widget skillContainer = getSkillWidget();
        // Make sure skillContainer is not null
        if (skillContainer == null || skillContainer.isHidden() || skillContainer.getCanvasLocation() == null)
        {
            return new  ArrayList<>();
        }


        // Define starting values for X and Y
        final int baseX = skillContainer.getCanvasLocation().getX();
        final int baseY = skillContainer.getCanvasLocation().getY() + 1;

        // Create an array list with the correct length
        ArrayList<SkillLocation> locations = new ArrayList<>(SKILL_NAMES.length);

        for (int i = 0; i< SKILL_NAMES.length; i++ )
        {
            String name =  SKILL_NAMES[i];
            String val = configManager.getConfiguration("skilllock",name,String.class);
            boolean locked = "true".equalsIgnoreCase(val);
            val = configManager.getConfiguration("skilllock",name+"_level",String.class);
            int level = Integer.parseInt(val);

            // 3 columns, 8 rows
            int col = i % 3;
            int row = i / 3;

            int RECT_WIDTH = SkillLockOverlay.RECT_WIDTH;
            int x = baseX + col * RECT_WIDTH;
            int RECT_HEIGHT = SkillLockOverlay.RECT_HEIGHT;
            int y = baseY + row * RECT_HEIGHT;

            locations.add(new SkillLocation(name, x, y, locked, level));
        }

        return locations;
    }

    private boolean isSkillsTabOpen()
    {
        Widget skillsRoot = getSkillWidget();
        return skillsRoot != null && !skillsRoot.isHidden();
    }

    private boolean getSkillLockState(String skill)
    {
        // Use ConfigManager directly — no switch needed!
        String value = configManager.getConfiguration("skilllock", skill, String.class);
        return "true".equalsIgnoreCase(value);
    }

    private void toggleSkillLock(String skill)
    {
        boolean current = getSkillLockState(skill);
        boolean newState = !current;

        // Update config
        configManager.setConfiguration("skilllock", skill, newState);

        // If the skill is being locked also update it's static level
        if (newState)
        {
            configManager.setConfiguration("skilllock", skill+"_level", 0);
        }

        // Force immediate update
        needsLocationUpdate = true;

        log.debug("Toggled {} lock: {}", skill, newState ? "LOCKED" : "UNLOCKED");
    }

    private void openLevelInputDialog( String skillName)
    {
        boolean isLocked = getSkillLockState(skillName);
        AtomicInteger updatedLevel = new AtomicInteger();

        // Only allow if the skill is unlocked
        if (!isLocked)
        {
            clientThread.invokeLater( () ->
                    chatboxPanelManager.openTextInput("<col=ff9040>SkillLock</col> - Set level for <col=ffff00>" + skillName + "</col>")
                            .prompt("Enter 1-99 (or 0 to disable)")
                            .onDone( inputText ->
                            {
                                clientThread.invokeLater( () ->
                                {
                                    String value = inputText.trim();
                                    if (value.isEmpty())
                                    {
                                        return;
                                    }
                                    else
                                    {
                                        try
                                        {
                                            int level  = Integer.parseInt(value);
                                            if (level < 0 || level > 99)
                                            {
                                                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Level must be 0-99 for " + skillName, null);
                                                return;
                                            }
                                            updatedLevel.set(level);
                                            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", skillName.substring(0,1).toUpperCase() + skillName.substring(1) + " level set to " + level, null);
                                        }
                                        catch (NumberFormatException e)
                                        {
                                            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Invalid number for " + skillName, null);
                                            return;
                                        }

                                    }
                                    configManager.setConfiguration("skilllock", skillName+"_level", updatedLevel.get());
                                });
                            })
                            .build()
            );
        }
    }

    private void updateWorldType()
    {
        EnumSet<WorldType> types = client.getWorldType();
        membersWorld = types.contains(WorldType.MEMBERS);
    }

    @Subscribe
    public void onMenuOpened (MenuOpened event)
    {
        // Make sure skills tab is open
        if (!isSkillsTabOpen()) return;

        // Get menu entries and make sure it is a skill we have right-clicked
        MenuEntry[] menuEntries = event.getMenuEntries();
        if (menuEntries[3] == null || !menuEntries[3].getOption().startsWith("View") || !menuEntries[3].getOption().endsWith("guide")) return;

        // Get the skill name from the menu entry
        String skillName = menuEntries[3].getOption().split(" ")[1].replaceAll("<col=\\w{6}>([^<]+)</col>", "$1");
        // Normalize it for the config name
        String configName = skillName.toLowerCase();

        // Create the custom menu entry
        MenuEntry toggle = client.createMenuEntry(0)
                .setOption(getSkillLockState(configName) ? "Unlock" : "Lock")
                .setTarget("<col=ff981f>" + skillName + "</col> level")
                .setType(MenuAction.RUNELITE)
                .onClick(e -> {
                    toggleSkillLock(configName);
                });

        MenuEntry setSkill = client.createMenuEntry(0)
                .setOption("Set")
                .setTarget("<col=ff981f>" + skillName + "</col> level")
                .setType(MenuAction.RUNELITE)
                .onClick(e -> {
                    openLevelInputDialog(configName);
                });

        boolean isLocked = getSkillLockState(configName);
        MenuEntry[] newMenuEntries = new MenuEntry[]{menuEntries[0], menuEntries[1],menuEntries[2],menuEntries[3]};

        // If the skill is locked only show the toggle button
        if (isLocked)
        {
            newMenuEntries = new MenuEntry[]{menuEntries[0], toggle, menuEntries[1], menuEntries[2], menuEntries[3]};
        }
        // Also show the set skill option
        else
        {
            newMenuEntries = new MenuEntry[]{menuEntries[0], setSkill, toggle, menuEntries[1], menuEntries[2], menuEntries[3]};
        }

        client.setMenuEntries(newMenuEntries);

    }


    @Subscribe
    public void onGameTick (GameTick event)
    {
        // First tick: safe to access client
        if (!skillsTabWasOpen && isSkillsTabOpen())
        {
            // First time skills tab is detected open
            skillLocations = createSkillLocations();
        }

        checkAndUpdateOverlay();

        if (needsLocationUpdate && isSkillsTabOpen())
        {
            skillLocations = createSkillLocations();
            needsLocationUpdate = false;
            //log.debug("Skill locations updated from config change: {}", skillLocations);
        }
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event)
    {
        if (event.getGroupId() == WidgetInfo.SKILLS_CONTAINER.getGroupId())
        {
            skillLocations = createSkillLocations();
            //log.debug("skill locations {}", skillLocations);
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if ("skilllock".equals(event.getGroup()))
        {
            needsLocationUpdate = true;
            //log.debug("Config changed, scheduled to update skill locations");
        }
    }

    @Subscribe
    public void onWorldChanged(WorldChanged event)
    {
        updateWorldType();
    }


	@Provides
    SkillLockConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SkillLockConfig.class);
	}
}
