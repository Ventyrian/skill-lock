package com.skilllock;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.KeyCode;
import net.runelite.api.Point;
import net.runelite.api.widgets.ComponentID;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class SkillLockOverlay extends Overlay
{
    private final SkillLockPlugin plugin;
    private final Client client;
    // Font used for set skills and images + color used in overlay
    private Font skillFont;
    private BufferedImage lockImage;
    private BufferedImage backgroundImage;
    private static final Color GREYSCALE_FILL = new Color(0, 0, 0, 128);
    // Width and Height of Skills Widget and Total Level
    public static final int RECT_WIDTH = 63;
    public static final int RECT_HEIGHT = 30;
    private static final int TOTAL_LEVEL_WIDTH = 187;
    private static final int TOTAL_LEVEL_HEIGHT = 19;

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
    private static final int SKILL_BOUNDARY_X = 132;
    private static final int SKILL_BOUNDARY_Y = 19;

    private static final int XP_TOOLTIP_ID = 20971553;

    private static final Map<String, List<String>> BASE_HIDE_MAP = new HashMap<>();
    static
    {
        BASE_HIDE_MAP.put("attack", List.of("strength", "agility", "defence", "herblore"));
        BASE_HIDE_MAP.put("hitpoints", List.of("agility", "smithing", "herblore", "fishing"));
        BASE_HIDE_MAP.put("mining", List.of("agility", "smithing", "herblore", "fishing"));
        BASE_HIDE_MAP.put("strength", List.of("defence", "herblore", "ranged", "thieving"));
        BASE_HIDE_MAP.put("agility", List.of("herblore", "fishing", "thieving", "cooking"));
        BASE_HIDE_MAP.put("smithing", List.of("herblore", "fishing", "thieving", "cooking"));
        BASE_HIDE_MAP.put("defence", List.of("ranged", "thieving", "prayer", "crafting"));
        BASE_HIDE_MAP.put("herblore", List.of("ranged", "thieving", "cooking", "prayer", "crafting", "firemaking"));
        BASE_HIDE_MAP.put("fishing", List.of("thieving", "cooking", "crafting", "firemaking"));
        BASE_HIDE_MAP.put("ranged", List.of("prayer", "crafting", "magic", "fletching"));
        BASE_HIDE_MAP.put("thieving", List.of("prayer", "crafting", "firemaking", "magic", "fletching", "woodcutting"));
        BASE_HIDE_MAP.put("cooking", List.of("crafting", "firemaking", "fletching", "woodcutting"));
        BASE_HIDE_MAP.put("prayer", List.of("magic", "fletching", "runecraft", "slayer"));
        BASE_HIDE_MAP.put("crafting", List.of("fletching", "woodcutting", "slayer", "farming"));
        BASE_HIDE_MAP.put("firemaking", List.of("fletching", "woodcutting", "slayer", "farming"));
        BASE_HIDE_MAP.put("magic", List.of("runecraft", "slayer", "construction", "hunter"));
        BASE_HIDE_MAP.put("fletching", List.of("runecraft", "slayer", "farming", "construction", "hunter", "sailing"));
        BASE_HIDE_MAP.put("woodcutting", List.of("slayer", "farming", "hunter", "sailing"));
        BASE_HIDE_MAP.put("runecraft", List.of("construction", "hunter"));
        BASE_HIDE_MAP.put("slayer", List.of("hunter", "sailing"));
        BASE_HIDE_MAP.put("farming", List.of("hunter", "sailing"));
        BASE_HIDE_MAP.put("construction", List.of("runecraft", "slayer", "farming", "magic", "fletching", "woodcutting"));
        BASE_HIDE_MAP.put("hunter", List.of("slayer", "farming", "fletching", "woodcutting"));
        BASE_HIDE_MAP.put("sailing", List.of("slayer", "farming", "fletching", "woodcutting"));
        BASE_HIDE_MAP.put("total", List.of("construction", "hunter"));
    }

    @Inject
    public SkillLockOverlay(final SkillLockPlugin plugin, final Client client)
    {
        this.plugin = plugin;
        this.client = client;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        loadLockImage();
        loadBackgroundImage();
    }


    @Override
    public Dimension render(Graphics2D graphics)
    {
        Widget skillWidget = client.getWidget(ComponentID.SKILLS_CONTAINER);
        // Visibility check
        if(!plugin.isSkillsTabOpen() || skillWidget == null|| skillWidget.isHidden() || backgroundImage == null || lockImage == null)
        {
            return null;
        }

        // Get live location of the widget (updates when sidebar opens/resizes)
        Point basePoint = skillWidget.getCanvasLocation();
        int baseX = basePoint.getX();
        int baseY = basePoint.getY() + 1;

        String hoverSkill = findHoverSkill(baseX, baseY);
        Set<String> hiddenSkills = getHiddenSkills(hoverSkill);
        boolean shiftDown = client.isKeyPressed(KeyCode.KC_SHIFT);

        if (skillFont == null)
        {
            skillFont = FontManager.getRunescapeBoldFont();
        }
        graphics.setFont(skillFont);

        for( SkillLocation loc : plugin.skillLocations )
        {
            // Calculate the ACTUAL screen position for this time
            int screenX = baseX + loc.x;
            int screenY = baseY + loc.y;

            if (hiddenSkills.contains(loc.name) || (loc.name.equals(hoverSkill) && shiftDown))
            {
                continue;
            }

            renderSkill(graphics,loc, screenX, screenY);
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
        try (InputStream is = SkillLockOverlay.class.getResourceAsStream("/com/skilllock/background.png"))
        {
            if (is == null)
            {
                log.error("lock.png not found! Check: src/main/resources/com/skilllock/background.png");
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

    private String findHoverSkill(int baseX, int baseY)
    {
        Point mousePos = client.getMouseCanvasPosition();
        int mx = mousePos.getX();
        int my = mousePos.getY();

        for (SkillLocation loc : plugin.skillLocations)
        {
            // Calculate the screen bounds for this specific skill relative to the anchor (Skill Container)
            int sx = baseX + loc.x;
            int sy = baseY + loc.y;

            int width = loc.name.equals("total") ? TOTAL_LEVEL_WIDTH : RECT_WIDTH;
            int height = loc.name.equals("total") ? TOTAL_LEVEL_HEIGHT : RECT_HEIGHT;

            //Precise skill box bounds
            if (mx >= sx && mx <= sx + width && my >= sy && my <= sy + height)
            {
                return loc.name;
            }
        }
        return "";
    }

    private void applyDynamicBounds(String hoverSkill, Set<String> hidden, Rectangle bounds)
    {
        // 1. Side Boundary (Addition Logic)
        if (bounds.width >= SKILL_BOUNDARY_X)
        {
            switch(hoverSkill)
            {
                case "attack":
                    hidden.addAll(List.of("smithing", "fishing"));
                    break;
                case "strength":
                    hidden.addAll(List.of("fishing", "cooking"));
                    break;
                case "defence":
                    hidden.addAll(List.of("cooking", "firemaking"));
                    break;
                case "ranged":
                    hidden.addAll(List.of("firemaking", "woodcutting"));
                    break;
                case "prayer":
                    hidden.addAll(List.of("woodcutting", "farming"));
                    break;
                case "magic":
                    hidden.addAll(List.of("farming", "sailing"));
                    break;
                case "runecraft":
                case "total":
                    hidden.add("sailing");
                    break;
                case "hitpoints":
                case "mining":
                    hidden.addAll(List.of("strength", "defence"));
                    break;
                case "agility":
                case "smithing":
                    hidden.addAll(List.of("defence", "ranged"));
                    break;
                case "fishing":
                    hidden.addAll(List.of("ranged", "prayer"));
                    break;
                case "cooking":
                    hidden.addAll(List.of("prayer", "magic"));
                    break;
                case "crafting":
                case "firemaking":
                    hidden.addAll(List.of("magic", "runecraft"));
                    break;
                case "woodcutting":
                    hidden.addAll(List.of("runecraft", "construction"));
                    break;
                case "slayer":
                case "farming":
                    hidden.add("construction");
                    break;
                case "hunter":
                case "sailing":
                    hidden.addAll(List.of("runecraft", "magic"));
                    break;
            }
        }
        // 2. Height Boundary (Removal Logic)
        if (bounds.height <= SKILL_BOUNDARY_Y )
        {
            switch (hoverSkill) {
                case "attack":
                case "hitpoints":
                case "mining":
                    hidden.removeAll(List.of("defence", "herblore", "fishing"));
                    break;
                case "strength":
                case "agility":
                case "smithing":
                    hidden.removeAll(List.of("ranged", "thieving", "cooking"));
                    break;
                case "defence":
                case "herblore":
                case "fishing":
                    hidden.removeAll(List.of("prayer", "crafting", "firemaking"));
                    break;
                case "ranged":
                case "thieving":
                case "cooking":
                    hidden.removeAll(List.of("magic", "fletching", "woodcutting"));
                    break;
                case "prayer":
                case "crafting":
                case "firemaking":
                    hidden.removeAll(List.of("runecraft", "slayer", "farming"));
                    break;
                case "magic":
                case "fletching":
                case "woodcutting":
                    hidden.removeAll(List.of("construction", "hunter", "sailing"));
                    break;
                case "construction":
                case "hunter":
                case "sailing":
                    hidden.removeAll(List.of("magic", "fletching", "woodcutting"));
                    break;
            }
        }
    }

    private void renderSkill(Graphics2D graphics, SkillLocation loc, int screenX, int screenY)
    {
        Point canvasPoint = new Point(screenX + OFFSET_X,screenY + OFFSET_Y);
        // Draw background
        if (loc.level > 0 || loc.isLocked)
        {
            OverlayUtil.renderImageLocation(graphics,canvasPoint,backgroundImage);
        }

        if (loc.isLocked)
        {
            // Greyscale the widget
            graphics.setColor(GREYSCALE_FILL);
            graphics.fillRect(screenX,screenY,RECT_WIDTH,RECT_HEIGHT);
            // Draw the lock image over the greyscale
            OverlayUtil.renderImageLocation(graphics,canvasPoint,lockImage);
        }
        else if (loc.level > 0)
        {
            // Text Calculation
            String text = String.valueOf(loc.level);
            FontMetrics fm = graphics.getFontMetrics();
            int textX = screenX + ((RECT_WIDTH + OFFSET_X - fm.stringWidth(text)) / 2);
            int textY = screenY + ((RECT_HEIGHT - OFFSET_Y + 2 + fm.getHeight()) / 2);

            // Shadow
            graphics.setColor(Color.BLACK);
            graphics.drawString(text, textX + 1, textY + 1);
            // Main Yellow Text
            graphics.setColor(Color.YELLOW);
            graphics.drawString(text, textX, textY);
        }

        // Glow logic
        Long startTime = plugin.getGlowingSkills().get(loc.name);
        if (startTime != null)
        {
            Rectangle bounds = new Rectangle(screenX, screenY, RECT_WIDTH, RECT_HEIGHT);
            drawPulsingRingGlow(graphics, bounds, startTime);
        }
    }

    private Set<String> getHiddenSkills(String hoverSkill)
    {
        if (hoverSkill.isEmpty() || client.isMenuOpen())
        {
            return Collections.emptySet();
        }

        Widget tooltip = client.getWidget(XP_TOOLTIP_ID);
        if (tooltip == null || tooltip.isHidden())
        {
            return Collections.emptySet();
        }

        // Initialize the set with base values
        Set<String> hidden = new HashSet<>(BASE_HIDE_MAP.getOrDefault(hoverSkill, Collections.emptyList()));

        // Apply the dynamic logic via helper function
        applyDynamicBounds(hoverSkill, hidden, tooltip.getBounds());

        return hidden;
    }
}
