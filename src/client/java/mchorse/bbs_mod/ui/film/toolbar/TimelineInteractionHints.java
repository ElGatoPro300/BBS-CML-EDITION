package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.MathUtils;

/**
 * Shared hint card and pulse animation helpers for timeline interaction modes.
 */
public final class TimelineInteractionHints
{
    public static float getPulseAlpha(float maxAlpha)
    {
        float t = (System.currentTimeMillis() % TimelineToolbarSettings.INTERACTION_TRACK_PULSE_PERIOD_MS)
            / (float) TimelineToolbarSettings.INTERACTION_TRACK_PULSE_PERIOD_MS;
        float wave = 0.5F + 0.5F * (float) Math.sin(t * (float) (Math.PI * 2D));

        return MathUtils.clamp(wave * maxAlpha, 0F, maxAlpha);
    }

    public static void renderHint(UIContext context, Area area, IKey hint)
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
        int x = area.x + TimelineToolbarSettings.INTERACTION_HINT_MARGIN;
        int y = area.ey() - cardH - TimelineToolbarSettings.INTERACTION_HINT_MARGIN;

        context.batcher.box(x, y, x + cardW, y + cardH, TimelineToolbarSettings.INTERACTION_HINT_BACKGROUND);
        context.batcher.outline(x, y, x + cardW, y + cardH, TimelineToolbarSettings.INTERACTION_HINT_BORDER);
        context.batcher.text(text, x + padding, y + padding / 2 + 1, TimelineToolbarSettings.INTERACTION_HINT_FG,
            false);
    }

    private TimelineInteractionHints()
    {}
}
