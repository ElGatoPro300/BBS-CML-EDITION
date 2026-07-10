package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Colors;

/**
 * Shared hint card and pulse animation helpers for timeline interaction modes.
 */
public final class TimelineInteractionHints
{
    public static float getPulseAlpha(float minAlpha, float maxAlpha)
    {
        float t = (System.currentTimeMillis() % TimelineToolbarSettings.INTERACTION_TRACK_PULSE_PERIOD_MS)
            / (float) TimelineToolbarSettings.INTERACTION_TRACK_PULSE_PERIOD_MS;
        float wave = 0.5F + 0.5F * (float) Math.sin(t * (float) (Math.PI * 2D));

        return MathUtils.clamp(minAlpha + wave * (maxAlpha - minAlpha), minAlpha, maxAlpha);
    }

    public static float getPulseAlpha(float maxAlpha)
    {
        return getPulseAlpha(0F, maxAlpha);
    }

    public static void renderPulsingTickColumn(UIContext context, int x, int y, int ey)
    {
        float alpha = getPulseAlpha(
            TimelineToolbarSettings.INTERACTION_TICK_PULSE_MIN_ALPHA,
            TimelineToolbarSettings.INTERACTION_TICK_PULSE_MAX_ALPHA);
        int color = Colors.setA(Colors.WHITE, alpha);
        int w = TimelineToolbarSettings.INTERACTION_TICK_COLUMN_WIDTH;

        context.batcher.box(x, y, x + w, ey, color);
    }

    public static void renderHint(UIContext context, Area area, IKey hint, UIElement source)
    {
        renderHint(context, area, hint, TimelineToolbarDockLayout.findDockFor(source));
    }

    public static void renderHint(UIContext context, Area area, IKey hint, TimelineToolbarDock dock)
    {
        if (hint == null)
        {
            return;
        }

        FontRenderer font = context.batcher.getFont();
        String text = hint.get();
        int padding = TimelineToolbarSettings.INTERACTION_HINT_PADDING;
        int textW = font.getWidth(text);
        int textH = font.getHeight();
        int cardW = textW + padding * 2;
        int cardH = textH + padding;
        int margin = TimelineToolbarSettings.INTERACTION_HINT_MARGIN;
        int x;
        int y;

        if (dock == null)
        {
            dock = TimelineToolbarDock.BOTTOM;
        }

        switch (dock)
        {
            case TOP:
                x = area.x + margin;
                y = area.y + margin;
                break;
            case LEFT:
                x = area.x + margin;
                y = area.ey() - cardH - margin;
                break;
            case RIGHT:
                x = area.ex() - cardW - margin;
                y = area.ey() - cardH - margin;
                break;
            case BOTTOM:
            default:
                x = area.x + margin;
                y = area.ey() - cardH - margin;
                break;
        }

        context.batcher.box(x, y, x + cardW, y + cardH, TimelineToolbarSettings.INTERACTION_HINT_BACKGROUND);
        context.batcher.outline(x, y, x + cardW, y + cardH, TimelineToolbarSettings.INTERACTION_HINT_BORDER);
        context.batcher.text(text, x + padding, y + padding / 2 + 1, TimelineToolbarSettings.INTERACTION_HINT_FG,
            false);
    }

    private TimelineInteractionHints()
    {}
}
