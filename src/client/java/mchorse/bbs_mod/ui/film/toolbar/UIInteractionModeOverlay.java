package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs.UIKeyframeDopeSheet;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Colors;

import org.lwjgl.glfw.GLFW;

/**
 * Manages an in-progress timeline interaction (pick a track, later pick a tick,
 * etc.). Renders the hint above the toolbar and coordinates cancel / confirm
 * input. Intended to grow into the full Phase 2d/3 interaction system.
 */
public class UIInteractionModeOverlay
{
    private TimelineInteractionState state;

    public boolean isActive()
    {
        return this.state != null;
    }

    public void enter(TimelineInteractionState state)
    {
        this.state = state;
    }

    public void cancel()
    {
        this.state = null;
    }

    public boolean isSheetEligible(UIKeyframeSheet sheet)
    {
        return this.state != null && sheet != null && this.state.eligible.test(sheet);
    }

    /**
     * Pulsing alpha for eligible track backgrounds (0 to max, smooth sine).
     */
    public static float getTrackPulseAlpha()
    {
        float t = (System.currentTimeMillis() % TimelineToolbarSettings.INTERACTION_TRACK_PULSE_PERIOD_MS)
            / (float) TimelineToolbarSettings.INTERACTION_TRACK_PULSE_PERIOD_MS;
        float wave = 0.5F + 0.5F * (float) Math.sin(t * (float) (Math.PI * 2D));

        return MathUtils.clamp(wave * TimelineToolbarSettings.INTERACTION_TRACK_PULSE_MAX_ALPHA, 0F,
            TimelineToolbarSettings.INTERACTION_TRACK_PULSE_MAX_ALPHA);
    }

    /**
     * @return {@code true} if the event was consumed
     */
    public boolean handleMouseClicked(UIKeyframes keyframes, UIContext context)
    {
        if (this.state == null)
        {
            return false;
        }

        if (context.mouseButton == 2)
        {
            this.cancel();

            return true;
        }

        if (context.mouseButton != 0 || !keyframes.area.isInside(context))
        {
            return context.mouseButton != 1;
        }

        if (!(keyframes.getGraph() instanceof UIKeyframeDopeSheet dopeSheet))
        {
            return true;
        }

        UIKeyframeSheet sheet = dopeSheet.getSheet(context.mouseY);

        if (sheet != null && this.state.eligible.test(sheet))
        {
            this.state.onConfirm.accept(sheet);
            this.cancel();
        }

        return true;
    }

    /**
     * @return {@code true} if the event was consumed
     */
    public boolean handleKeyPressed(UIContext context)
    {
        if (this.state == null)
        {
            return false;
        }

        if (context.isPressed(GLFW.GLFW_KEY_ESCAPE))
        {
            this.cancel();

            return true;
        }

        return false;
    }

    public void renderHint(UIContext context, Area area)
    {
        if (this.state == null)
        {
            return;
        }

        FontRenderer font = context.batcher.getFont();
        String text = this.state.hint.get();
        int padding = TimelineToolbarSettings.INTERACTION_HINT_PADDING;
        int textW = font.getWidth(text);
        int textH = font.getHeight();
        int cardW = textW + padding * 2;
        int cardH = textH + padding;
        int x = area.x + TimelineToolbarSettings.INTERACTION_HINT_MARGIN;
        int y = area.ey() - cardH - TimelineToolbarSettings.INTERACTION_HINT_MARGIN;

        context.batcher.box(x, y, x + cardW, y + cardH, TimelineToolbarSettings.INTERACTION_HINT_BACKGROUND);
        context.batcher.outline(x, y, x + cardW, y + cardH, TimelineToolbarSettings.INTERACTION_HINT_BORDER);
        context.batcher.text(text, x + padding, y + padding / 2 + 1, TimelineToolbarSettings.INTERACTION_HINT_FG,
            false);
    }

    public boolean hasAnyEligibleTrack(UIKeyframes keyframes)
    {
        if (this.state == null || !(keyframes.getGraph() instanceof UIKeyframeDopeSheet dopeSheet))
        {
            return false;
        }

        for (UIKeyframeSheet sheet : dopeSheet.getSheets())
        {
            if (this.state.eligible.test(sheet))
            {
                return true;
            }
        }

        return false;
    }
}
