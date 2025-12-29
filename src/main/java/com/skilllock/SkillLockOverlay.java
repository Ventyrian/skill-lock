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
import java.awt.BasicStroke;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import static java.util.Map.entry;


@Slf4j
public class SkillLockOverlay extends Overlay
{


    private Font skillFont;
    // Hashmap of skills to not render when key is hovered by cursor
    private Map<String, ArrayList<String>> hoverHideMap;

    private final SkillLockPlugin plugin;
    private BufferedImage lockImage;
    private BufferedImage backgroundImage;
    // Width and Height of Skills Widget
    public static final int RECT_WIDTH = 63;
    public static final int RECT_HEIGHT = 30;

    // Offsets to move image or rectangle over levels
    public static final int OFFSET_X = 31;
    public static final int OFFSET_Y = 5;

    // Glow constants
    public static final long GLOW_DURATION_MS = 1200;  // 1.2s to match audio
    private static final Color GLOW_COLOR = new Color(255, 255, 100);  // Bright yellow (adjust as needed)
    private static final float PULSE_SPEED = 2.5f;  // Higher = faster pulse
    private static final int RING_THICKNESS = 4;    // Thickness of the glowing ring
    private static final int INNER_PADDING = 2;     // Distance from edge (keeps it inside)

    // Boundaries for Tooltip logic
    private static final int RIGHT_SKILL_BOUNDARY_X = 684;
    private static final int LEFT_SKILL_BOUNDARY_X = 607;
    private static final int SKILL_BOUNDARY_Y = 19;

    private static final int TOOLTIP_ID = 20971553;

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
        Rectangle skillTooltip = null;
        boolean shiftDown = client.isKeyPressed(KeyCode.KC_SHIFT);
        if (!hoverSkill.isEmpty() && !client.isMenuOpen())
        {
            // Get the bounds of the skillTooltip
            skillTooltip = getSkillExpTooltipBounds();
            // Make sure the skillTooltip is displayed before we calculate notRenderedSkills
            if (skillTooltip != null)
            {
                loadHoverHideMap(skillTooltip);
                // reset skillToolTip after loading HoverMap
                skillTooltip = null;
                notRenderedSkills.addAll(hoverHideMap.get(hoverSkill));
            }

        }



        for ( SkillLockPlugin.SkillLocation skillLocation : plugin.skillLocations )
        {

            // If skill is within notRenderedSkills skip the rendering of it
            if (notRenderedSkills.contains(skillLocation.name))
            {
                continue;
            }

            // If the hover skill is the current skill and the shift key is being pressed skip rendering
            if (hoverSkill.equals(skillLocation.name) && shiftDown)
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

            // Glow logic
            Long startTime = plugin.getGlowingSkills().get(skillLocation.name);
            Rectangle bounds = new Rectangle(x, y, RECT_WIDTH, RECT_HEIGHT);
            if (startTime != null) {
                drawPulsingRingGlow(graphics, bounds, startTime);
            }
        }

        return null;

    }

    private void drawPulsingRingGlow(Graphics2D g, Rectangle bounds, long startTime) {
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed >= GLOW_DURATION_MS) return;

        // Progress from 0.0 to 1.0 over the duration
        float progress = elapsed / (float) GLOW_DURATION_MS;

        // Create a smooth pulsing alpha using sine wave (breathes in/out)
        float pulsePhase = elapsed / 1000.0f * PULSE_SPEED * (float) Math.PI * 2;
        float pulse = (float) Math.sin(pulsePhase);
        pulse = (pulse + 1) / 2;  // Convert -1..1 → 0..1

        // Base alpha starts strong and fades out over duration
        int baseAlpha = (int) (180 * (1 - progress));  // 180 → 0
        int alpha = (int) (baseAlpha * (0.6f + 0.4f * pulse));  // Pulse between ~60% and 100% of base

        if (alpha <= 0) return;

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color glowColor = new Color(
                GLOW_COLOR.getRed(),
                GLOW_COLOR.getGreen(),
                GLOW_COLOR.getBlue(),
                Math.max(10, alpha)  // Minimum 10 to avoid full vanish
        );

        g.setColor(glowColor);
        g.setStroke(new BasicStroke(RING_THICKNESS, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Inset rectangle to keep glow strictly inside bounds
        int x = bounds.x + INNER_PADDING;
        int y = bounds.y + INNER_PADDING;
        int width = bounds.width - 2 * INNER_PADDING;
        int height = bounds.height - 2 * INNER_PADDING;

        // Draw rounded rectangle ring
        g.drawRoundRect(x, y, width, height, 8, 8);
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

    private Rectangle getSkillExpTooltipBounds()
    {
        Widget tooltip = client.getWidget(TOOLTIP_ID);
        if (tooltip != null && !tooltip.isHidden())
        {
            return tooltip.getBounds();
        }
        return null;
    }

    private void loadHoverHideMap( Rectangle bounds)
    {
        int startX = bounds.x;
        int endX = bounds.x + bounds.width;
        int height = bounds.height;
        // Base hideHoverMap
        Map<String, ArrayList<String>> normal = Map.ofEntries(
                entry("attack", new ArrayList<>(Arrays.asList("strength", "agility", "defence", "herblore"))),
                entry("hitpoints", new ArrayList<>(Arrays.asList("agility", "smithing", "herblore", "fishing"))),
                entry("mining", new ArrayList<>(Arrays.asList("agility", "smithing", "herblore", "fishing"))),
                entry("strength", new ArrayList<>(Arrays.asList("defence", "herblore", "ranged", "thieving"))),
                entry("agility", new ArrayList<>(Arrays.asList("herblore", "fishing", "thieving", "cooking"))),
                entry("smithing", new ArrayList<>(Arrays.asList("herblore", "fishing", "thieving", "cooking"))),
                entry("defence", new ArrayList<>(Arrays.asList("ranged", "thieving", "prayer", "crafting"))),
                entry("herblore", new ArrayList<>(Arrays.asList("ranged", "thieving", "cooking", "prayer", "crafting", "firemaking"))),
                entry("fishing", new ArrayList<>(Arrays.asList("thieving", "cooking", "crafting", "firemaking"))),
                entry("ranged", new ArrayList<>(Arrays.asList("prayer", "crafting", "magic", "fletching"))),
                entry("thieving", new ArrayList<>(Arrays.asList("prayer", "crafting", "firemaking", "magic", "fletching", "woodcutting"))),
                entry("cooking", new ArrayList<>(Arrays.asList("crafting", "firemaking", "fletching", "woodcutting"))),
                entry("prayer", new ArrayList<>(Arrays.asList("magic", "fletching", "runecraft", "slayer"))),
                entry("crafting", new ArrayList<>(Arrays.asList("fletching", "woodcutting", "slayer", "farming"))),
                entry("firemaking", new ArrayList<>(Arrays.asList("fletching", "woodcutting", "slayer", "farming"))),
                entry("magic", new ArrayList<>(Arrays.asList("runecraft", "slayer", "construction", "hunter"))),
                entry("fletching", new ArrayList<>(Arrays.asList("runecraft", "slayer", "farming", "construction", "hunter", "sailing"))),
                entry("woodcutting", new ArrayList<>(Arrays.asList("slayer", "farming", "hunter", "sailing"))),
                entry("runecraft", new ArrayList<>(Arrays.asList("construction", "hunter"))),
                entry("slayer", new ArrayList<>(Arrays.asList("hunter", "sailing"))),
                entry("farming", new ArrayList<>(Arrays.asList("hunter", "sailing"))),
                entry("construction", new ArrayList<>(Arrays.asList("runecraft", "slayer", "farming", "magic", "fletching", "woodcutting"))),
                entry("hunter", new ArrayList<>(Arrays.asList("slayer", "farming", "fletching", "woodcutting"))),
                entry("sailing", new ArrayList<>(Arrays.asList("slayer", "farming", "fletching", "woodcutting")))
        );

        // Add additional skills for both right and left side of skill boundary
        if (endX >= RIGHT_SKILL_BOUNDARY_X)
        {
            normal.get("attack").addAll(Arrays.asList("smithing","fishing"));
            normal.get("strength").addAll(Arrays.asList("fishing", "cooking"));
            normal.get("defence").addAll(Arrays.asList("cooking", "firemaking"));
            normal.get("ranged").addAll(Arrays.asList("firemaking", "woodcutting"));
            normal.get("prayer").addAll(Arrays.asList("woodcutting", "farming"));
            normal.get("magic").addAll(Arrays.asList("farming","sailing"));
            normal.get("runecraft").add("sailing");
        }
        if ( startX <= LEFT_SKILL_BOUNDARY_X)
        {
            for ( String key : normal.keySet() )
            {
                switch (key)
                {
                    case "hitpoints":
                    case "mining":
                        normal.get(key).addAll(Arrays.asList("strength", "defence"));
                        break;
                    case "agility":
                    case "smithing":
                        normal.get(key).addAll(Arrays.asList("defence", "ranged"));
                        break;
                    case "fishing":
                        normal.get(key).addAll(Arrays.asList("ranged", "prayer"));
                        break;
                    case "cooking":
                        normal.get(key).addAll(Arrays.asList("prayer", "magic"));
                        break;
                    case "crafting":
                    case "firemaking":
                        normal.get(key).addAll(Arrays.asList("magic", "runecraft"));
                        break;
                    case "woodcutting":
                        normal.get(key).addAll(Arrays.asList("runecraft", "construction"));
                        break;
                    case "slayer":
                    case "farming":
                        normal.get(key).add("construction");
                        break;
                    case "hunter":
                    case "sailing":
                        normal.get(key).addAll(Arrays.asList("runecraft", "magic"));
                        break;
                    default:
                        break;

                }
            }
        }
        // Remove skills if height is below skill boundary
        if ( height <= SKILL_BOUNDARY_Y )
        {
            for ( String key : normal.keySet() )
            {
                switch(key)
                {
                    case "attack":
                    case "hitpoints":
                    case "mining":
                        normal.get(key).removeAll(Arrays.asList("defence", "herblore", "fishing"));
                        break;
                    case "strength":
                    case "agility":
                    case "smithing":
                        normal.get(key).removeAll(Arrays.asList("ranged", "thieving", "cooking"));
                        break;
                    case "defence":
                    case "herblore":
                    case "fishing":
                        normal.get(key).removeAll(Arrays.asList("prayer", "crafting", "firemaking"));
                        break;
                    case "ranged":
                    case "thieving":
                    case "cooking":
                        normal.get(key).removeAll(Arrays.asList("magic", "fletching", "woodcutting"));
                        break;
                    case "prayer":
                    case "crafting":
                    case "firemaking":
                        normal.get(key).removeAll(Arrays.asList("runecraft", "slayer", "farming"));
                        break;
                    case "magic":
                    case "fletching":
                    case "woodcutting":
                        normal.get(key).removeAll(Arrays.asList("construction", "hunter", "sailing"));
                        break;
                    case "construction":
                    case "hunter":
                    case "sailing":
                        normal.get(key).removeAll(Arrays.asList("magic", "fletching", "woodcutting"));
                        break;
                    default:
                        break;
                }
            }
        }
        hoverHideMap = normal;
    }
}
