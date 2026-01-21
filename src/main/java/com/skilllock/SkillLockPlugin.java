package com.skilllock;

import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Skill;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    private static final List<Skill> SKILLS_UI_ORDER = List.of(
            Skill.ATTACK, Skill.HITPOINTS, Skill.MINING,
            Skill.STRENGTH, Skill.AGILITY, Skill.SMITHING,
            Skill.DEFENCE, Skill.HERBLORE, Skill.FISHING,
            Skill.RANGED, Skill.THIEVING, Skill.COOKING,
            Skill.PRAYER, Skill.CRAFTING, Skill.FIREMAKING,
            Skill.MAGIC, Skill.FLETCHING, Skill.WOODCUTTING,
            Skill.RUNECRAFT, Skill.SLAYER, Skill.FARMING,
            Skill.CONSTRUCTION, Skill.HUNTER, Skill.SAILING);
    @Getter
    private final Map<String, Long> glowingSkills = new ConcurrentHashMap<>();
    public List<SkillLocation> skillLocations =  new ArrayList<>();

    @Override
	protected void startUp() throws Exception
	{
		log.debug("Skill Lock started!");
        skillsTabWasOpen = false;
        skillLocations.clear();
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.debug("Skill Lock stopped!");
        skillLocations.clear();
        overlayManager.remove(overlay);
	}

    @Subscribe
    public void onMenuOpened (MenuOpened event)
    {
        addMenuOptions(event);
    }

    @Subscribe
    public void onClientTick(ClientTick event)
    {
        boolean currentlyOpen = isSkillsTabOpen();

        // ONLY runs if the state just changed from closed to open
        if (currentlyOpen && !skillsTabWasOpen)
        {
            // We check the location. If it's 0,0, the UI isn't ready, so we wait for the next tick.
            Widget skillContainer = client.getWidget(ComponentID.SKILLS_CONTAINER);
            if (skillContainer == null || skillContainer.getCanvasLocation().getX() <= 0)
            {
                return;
            }

            // NOW we calculate, because we know the X is not 0
            skillLocations = createSkillLocations();
            overlayManager.add(overlay);
            skillsTabWasOpen = true;
        }
        // ONLY runs if the state just changed from open to closed
        else if (!currentlyOpen && skillsTabWasOpen)
        {
            overlayManager.remove(overlay);
            skillsTabWasOpen = false;
        }
        // Cleanup glow
        glowingSkills.entrySet().removeIf( entry -> System.currentTimeMillis() - entry.getValue() > SkillLockOverlay.GLOW_DURATION_MS);
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {

        if (!event.getGroup().equals("skilllock"))
        {
            return;
        }

        String key = event.getKey();

        // Trigger a location refresh if a level was changed
        if (key.endsWith("_level"))
        {
            clientThread.invokeLater(() -> {
                if (skillsTabWasOpen)
                {
                    skillLocations = createSkillLocations();
                }
            });
            return;
        }

        // Handle sound and glow logic for toggling locks
        if (isSkillKey(key))
        {
            // Fetch the new state from the config
            boolean isLocked = getSkillLockState(key);

            if (isLocked)
            {
                Integer currentLevel = configManager.getConfiguration("skilllock",key+"_level",Integer.class);
                if (currentLevel != null && currentLevel != 0)
                {
                    // Update config immediately so it is ready for the refresh
                    configManager.setConfiguration("skilllock", key+"_level", 0);
                }
            }

            clientThread.invokeLater(() -> {
                if (isSkillsTabOpen())
                {
                    skillLocations = createSkillLocations();

                    // Handle visuals/sounds
                    if (isLocked)
                    {
                        client.playSoundEffect(1351); // Locked
                    }
                    else
                    {
                        client.playSoundEffect(1493); // Unlocked
                        glowingSkills.put(key, System.currentTimeMillis());
                    }
                }
            });
        }
    }

	@Provides
    SkillLockConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SkillLockConfig.class);
	}

    private List<SkillLocation> createSkillLocations()
    {
        // Create an array list with the correct length
        List<SkillLocation> locations = new ArrayList<>();

        for (int i = 0; i< SKILLS_UI_ORDER.size(); i++ )
        {
            Skill skill = SKILLS_UI_ORDER.get(i);
            String skillName =  skill.getName().toLowerCase();
            // Fetch config value
            Integer level = configManager.getConfiguration("skilllock", skillName + "_level", Integer.class);
            boolean isLocked = getSkillLockState(skillName);

            // 3 columns, 8 rows
            int col = i % 3;
            int row = i / 3;

            int offX = col * SkillLockOverlay.RECT_WIDTH;
            int offY = row * SkillLockOverlay.RECT_HEIGHT;

            locations.add(new SkillLocation(skillName, offX, offY, (level == null ? 0 : level), isLocked));
            // Add total level to list
            if (skill.equals(Skill.CONSTRUCTION))
            {
                locations.add(new SkillLocation("total",offX + 2,offY + 30,0, false));
            }
        }
        return locations;
    }

    public boolean isSkillsTabOpen()
    {
        Widget skillsRoot = client.getWidget(ComponentID.SKILLS_CONTAINER);
        return skillsRoot != null && !skillsRoot.isHidden();
    }

    private void addMenuOptions(MenuOpened event)
    {
        MenuEntry[] entries = event.getMenuEntries();
        if (entries.length == 0)
        {
            return;
        }

        // Get the last entry
        MenuEntry lastEntry = entries[entries.length - 1];


        // Get the text from the Option
        String optionText = lastEntry.getOption();


        // Check if it's actually a skill guide entry
        if (!optionText.startsWith("View") || !optionText.endsWith("guide"))
        {
            return;
        }

        // Clean the text to get just the skill name
        String skillName = Text.removeTags(optionText)
                .replace("View ", "")
                .replace(" guide", "")
                .trim()
                .toLowerCase();

        Skill skill = findSkill(skillName);

        if (skill == null)
        {
            return;
        }

        // Create new "Lock/Unlock" entry
        client.createMenuEntry(-1)
                .setOption(getSkillLockState(skillName) ? "Unlock" : "Lock")
                .setTarget("<col=ff981f>" + skill.getName() + "</col> level")
                .setType(MenuAction.RUNELITE)
                .onClick(e -> toggleSkillLock(skillName));
        // Only add set entry if the skill is unlocked
        if (!getSkillLockState(skillName))
        {
            // Create new "Set" entry
            client.createMenuEntry(-1)
                    .setOption("Set")
                    .setTarget("<col=ff981f>" + skill.getName() + "</col> level")
                    .setType(MenuAction.RUNELITE)
                    .onClick(e -> openLevelInputDialog(skillName));
        }


    }

    private boolean getSkillLockState(String skill)
    {
        Boolean value = configManager.getConfiguration("skilllock", skill, Boolean.class);
        return Boolean.TRUE.equals(value);
    }

    // Helper function to find the SKill enum based on the name
    private Skill findSkill(String name)
    {
        for (Skill skill : Skill.values())
        {
            if (skill.getName().equalsIgnoreCase(name))
            {
                return skill;
            }
        }
        return null;
    }

    // Helper function to ensure we are only reacting to actual skill names
    private boolean isSkillKey(String key)
    {
        for (Skill skill : Skill.values()) {
            if (skill.getName().toLowerCase().equals(key)) return true;
        }
        return false;
    }

    private void toggleSkillLock(String skill)
    {
        boolean isLocked = !getSkillLockState(skill);

        // Update config
        configManager.setConfiguration("skilllock", skill, isLocked);

        log.debug("Toggled {} lock: {}", skill, isLocked ? "LOCKED" : "UNLOCKED");
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
}
