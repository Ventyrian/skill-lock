package com.skilllock;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;

@Slf4j
public class SkillLockOverlay extends Overlay
{
    private final SkillLockPlugin plugin;
    private long lastDebugLogTime = 0;
    private static final long DEBUG_LOG_INTERVAL = 2000;
    public static final int RECT_WIDTH = 63; //66
    public static final int RECT_HEIGHT = 30; //36


    // Hard‑coded black with 50 % opacity (0‑255)
    private static final Color GREYSCALE_FILL = new Color(0, 0, 0, 128);

    @Inject
    public SkillLockOverlay(final SkillLockPlugin plugin)
    {
        this.plugin = plugin;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        Widget skillWidget = plugin.getSkillWidget();
        if ( skillWidget == null || skillWidget.isHidden() )
        {
            return null;
        }

        if (skillWidget.getCanvasLocation() == null)
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
                graphics.setColor(GREYSCALE_FILL);

                //Throttled logging
                long currentTime = System.currentTimeMillis();
//                if ( currentTime - lastDebugLogTime >= DEBUG_LOG_INTERVAL )
//                {
//                    log.debug("Drawing rect at x={}, y={}, w={}, h={}", x, y, w, h);
//                    lastDebugLogTime = currentTime;
//                }

                graphics.fillRect(x,y,w,h);
            }
        }

        return null;

    }

}
