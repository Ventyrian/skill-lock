package com.skilllock;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.KeyCode;
import net.runelite.api.Point;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.*;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static java.util.Map.entry;


@Slf4j
public class SkillLockOverlay extends Overlay
{


    private Font skillFont;
    // Hashmap of skills to not render when key is hovered by cursor
    private Map<String, Set<String>> hoverHideMap;

    private final SkillLockPlugin plugin;
    private BufferedImage lockImage;
    private BufferedImage backgroundImage;
    // Width and Height of Skills Widget
    public static final int RECT_WIDTH = 63;
    public static final int RECT_HEIGHT = 30;

    // Offsets to move image or rectangle over levels
    public static final int OFFSET_X = 31;
    public static final int OFFSET_Y = 5;

    private static final Color GREYSCALE_FILL = new Color(0, 0, 0, 128);

    @Inject
    private Client client;


    @Inject
    public SkillLockOverlay(final SkillLockPlugin plugin)
    {
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);

        loadLockImage();
        loadBackgroundImage();
        loadHoverHideMap();
    }


    @Override
    public Dimension render(Graphics2D graphics)
    {
        // Check to see if skillWidget is visible and assets are loaded
        Widget skillWidget = plugin.getSkillWidget();
        if ( skillWidget == null || skillWidget.isHidden() || skillWidget.getCanvasLocation() == null || lockImage == null || backgroundImage == null )
        {
            return null;
        }

        // Initialize empty HashMap for possible skill hover
        HashSet notRenderedSkills = new HashSet();

        // Check to see if a skill is hovered and whether the shift key is being pressed
        String hoverSkill = findHoverSkill();
        boolean shiftDown = client.isKeyPressed(KeyCode.KC_SHIFT);
        if (!hoverSkill.isEmpty())
        {
            notRenderedSkills.addAll(hoverHideMap.get(hoverSkill));
        }


        for ( SkillLockPlugin.SkillLocation skillLocation : plugin.skillLocations )
        {

            // If skill is within notRenderedSkills skip the rendering of it
            if (notRenderedSkills.contains(skillLocation.name))
            {
                continue;
            }

            // If the hover skill is the current skill and the shift key is being pressed skip rendering
            if (hoverSkill.equals(skillLocation.name) && shiftDown && !skillLocation.isLocked)
            {
                continue;
            }


            int x = skillLocation.x;
            int y = skillLocation.y;


            if (skillLocation.isLocked)
            {
                // Draw background image to hide skill level
                Point canvasPoint = new Point(x+OFFSET_X,y+OFFSET_Y);
                OverlayUtil.renderImageLocation(graphics,canvasPoint,backgroundImage);
                // Greyscale the widget
                graphics.setColor(GREYSCALE_FILL);
                graphics.fillRect(x,y,RECT_WIDTH,RECT_HEIGHT);
                // Draw the lock image over the greyscale
                OverlayUtil.renderImageLocation(graphics,canvasPoint,lockImage);

            }
            else if ( skillLocation.level > 0 )
            {

                // Draw background image to hide skill level
                Point canvasPoint = new Point(x+OFFSET_X,y+OFFSET_Y);
                OverlayUtil.renderImageLocation(graphics,canvasPoint,backgroundImage);

                // Draw Level Text
                String text = String.valueOf(skillLocation.level);

                // Load the exact RS bold font for skill levels
                if (skillFont==null)
                {
                    skillFont = FontManager.getRunescapeBoldFont();
                }
                // Use the same font RuneLite uses for skill levels (looks native)
                graphics.setFont(skillFont);

                FontMetrics fm = graphics.getFontMetrics();
                int textWidth = fm.stringWidth(text);
                int textHeight = fm.getHeight();

                // Center the text inside the skill rectangle (63×30)
                int textX = x + ((RECT_WIDTH + OFFSET_X - textWidth) / 2);
                // Additional +2 for correct height can fine tune this later.
                int textY = y + ((RECT_HEIGHT - OFFSET_Y + 2 + textHeight) / 2) ;

                // Optional: slight shadow for better readability
                graphics.setColor(Color.BLACK);
                graphics.drawString(text, textX + 1, textY + 1);

                // Main text color - bright yellow like vanilla skills
                graphics.setColor(Color.YELLOW);
                graphics.drawString(text, textX, textY);

            }
        }

        return null;

    }

    private void loadLockImage()
    {
        try (InputStream is = SkillLockOverlay.class.getResourceAsStream("/com/skilllock/lock.png"))
        {
            if (is == null)
            {
                log.error("lock.png not found! Check: src/main/resources/com/skilllock/lock.png");
                lockImage = null;
                return;
            }

            BufferedImage original = ImageIO.read(is);
            log.debug("Lock image loaded: {}x{}", original.getWidth(), original.getHeight());


            // Choose a target size that covers the numbers
            final int TARGET_W = 50;   // you can tweak this
            final int TARGET_H = 31;   // keep it square, or change to 32×28 etc.
            final int OFF_X = -10;     // best offset to center on numbers
            final int OFF_Y = -5;      // best offset to center on numbers


            // Create a new BufferedImage with transparency
            lockImage = new BufferedImage(TARGET_W, TARGET_H, BufferedImage.TYPE_INT_ARGB);

            // Draw the original image scaled smoothly onto the new canvas
            Graphics2D g2d = lockImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);


            g2d.drawImage(original, OFF_X, OFF_Y, TARGET_W, TARGET_H, null);
            g2d.dispose();

            log.debug("Lock image loaded and resized to {}x{}", TARGET_W, TARGET_H);
        }
        catch (IOException e)
        {
            log.error("Failed to load or resize lock image", e);
            lockImage = null;
        }
    }

    private void loadBackgroundImage()
    {
        try (InputStream is = SkillLockOverlay.class.getResourceAsStream("/com/skilllock/background-sm.png"))
        {
            if (is == null)
            {
                log.error("lock.png not found! Check: src/main/resources/com/skilllock/background-sm.png");
                backgroundImage = null;
                return;
            }

            backgroundImage = ImageIO.read(is);
            log.debug("Background image loaded: {}x{}", backgroundImage.getWidth(), backgroundImage.getHeight());
        }
        catch (IOException e)
        {
            log.error("Failed to load or resize background image", e);
            backgroundImage = null;
        }
    }

    private String findHoverSkill()
    {
        Point mousePos = client.getMouseCanvasPosition();
        for (SkillLockPlugin.SkillLocation skillLocation : plugin.skillLocations)
        {
            //Precise skill box bounds
            Rectangle skillBox = new Rectangle( skillLocation.x, skillLocation.y, RECT_WIDTH, RECT_HEIGHT);
            if (skillBox.contains(mousePos.getX(), mousePos.getY()))
            {
                return skillLocation.name;
            }
        }
        return "";
    }

    private void loadHoverHideMap()
    {
        if (plugin.membersWorld)
        {
            hoverHideMap = Map.ofEntries(
                    entry("attack", Set.of("strength", "agility", "defense", "herblore")),
                    entry("hitpoints", Set.of("agility", "smithing", "herblore", "fishing")),
                    entry("mining", Set.of("agility", "smithing", "herblore", "fishing")),
                    entry("strength", Set.of("defense", "ranged", "herblore", "thieving")),
                    entry("agility", Set.of("herblore", "thieving", "fishing", "cooking")),
                    entry("smithing", Set.of("herblore", "thieving", "fishing", "cooking")),
                    entry("defense", Set.of("ranged", "thieving", "prayer", "crafting")),
                    entry("herblore", Set.of("thieving", "crafting", "cooking", "firemaking", "ranged")),
                    entry("fishing", Set.of("thieving", "crafting", "cooking", "firemaking")),
                    entry("ranged", Set.of("prayer", "magic", "crafting", "fletching")),
                    entry("thieving", Set.of("crafting", "fletching", "firemaking", "woodcutting", "prayer")),
                    entry("cooking", Set.of("crafting", "fletching", "firemaking", "woodcutting")),
                    entry("prayer", Set.of("magic", "runecraft", "fletching", "slayer")),
                    entry("crafting", Set.of("fletching", "slayer", "woodcutting", "farming")),
                    entry("firemaking", Set.of("fletching", "slayer", "woodcutting", "farming")),
                    entry("magic", Set.of("runecraft", "construction", "slayer", "hunter")),
                    entry("fletching", Set.of("slayer", "hunter", "farming", "sailing", "construction", "runecraft")),
                    entry("woodcutting", Set.of("slayer", "hunter", "farming", "sailing")),
                    entry("runecraft", Set.of("construction", "hunter")),
                    entry("slayer", Set.of("hunter", "sailing")),
                    entry("farming", Set.of("hunter", "sailing")),
                    entry("construction", Set.of("magic", "fletching", "runecraft", "slayer", "woodcutting", "farming")),
                    entry("hunter", Set.of("fletching", "slayer", "woodcutting", "farming")),
                    entry("sailing", Set.of("fletching", "slayer", "woodcutting", "farming"))
            );
        }
        else
        {
            hoverHideMap = Map.ofEntries(
                    entry("attack", Set.of("strength", "agility", "defense", "herblore")),
                    entry("hitpoints", Set.of("agility", "smithing", "herblore", "fishing")),
                    entry("mining", Set.of("agility", "smithing", "herblore", "fishing")),
                    entry("strength", Set.of("defense", "ranged", "herblore", "thieving")),
                    entry("agility", Set.of("herblore", "fishing")),
                    entry("smithing", Set.of("herblore", "thieving", "fishing", "cooking")),
                    entry("defense", Set.of("ranged", "thieving", "prayer", "crafting")),
                    entry("herblore", Set.of("thieving", "cooking", "ranged")),
                    entry("fishing", Set.of("thieving", "crafting", "cooking", "firemaking")),
                    entry("ranged", Set.of("prayer", "magic", "crafting", "fletching")),
                    entry("thieving", Set.of("crafting", "firemaking", "prayer")),
                    entry("cooking", Set.of("crafting", "fletching", "firemaking", "woodcutting")),
                    entry("prayer", Set.of("magic", "runecraft", "fletching", "slayer")),
                    entry("crafting", Set.of("fletching", "slayer", "woodcutting", "farming")),
                    entry("firemaking", Set.of("fletching", "slayer", "woodcutting", "farming")),
                    entry("magic", Set.of("runecraft", "construction", "slayer", "hunter")),
                    entry("fletching", Set.of("slayer", "farming", "runecraft")),
                    entry("woodcutting", Set.of("slayer", "hunter", "farming", "sailing")),
                    entry("runecraft", Set.of("construction", "hunter")),
                    entry("slayer", Set.of("hunter", "sailing")),
                    entry("farming", Set.of("hunter", "sailing")),
                    entry("construction", Set.of("runecraft", "slayer", "farming")),
                    entry("hunter", Set.of("slayer",  "farming")),
                    entry("sailing", Set.of("slayer", "farming"))
            );

        }
    }
}
