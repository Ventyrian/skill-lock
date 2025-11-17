package com.skilllock;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.FontID;
import net.runelite.api.Point;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.*;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;


@Slf4j
public class SkillLockOverlay extends Overlay
{


    private Font skillFont;

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
        Widget skillWidget = plugin.getSkillWidget();
        if ( skillWidget == null || skillWidget.isHidden() || skillWidget.getCanvasLocation() == null || lockImage == null )
        {
            return null;
        }


        for ( SkillLockPlugin.SkillLocation skillLocation : plugin.skillLocations )
        {

            int x = skillLocation.x;
            int y = skillLocation.y;
            int w = RECT_WIDTH;
            int h = RECT_HEIGHT;

            if (skillLocation.isLocked)
            {
                // Draw background image to hide skill level
                Point canvasPoint = new Point(x+OFFSET_X,y+OFFSET_Y);
                OverlayUtil.renderImageLocation(graphics,canvasPoint,backgroundImage);
                // Greyscale the widget
                graphics.setColor(GREYSCALE_FILL);
                graphics.fillRect(x,y,w,h);
                // Draw the lock image over the greyscale
                OverlayUtil.renderImageLocation(graphics,canvasPoint,lockImage);

            }
            else if ( skillLocation.level > 0 ) // else if skillLocation.level > 0 && mouse cursor within bounds && shift not being held
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
}
