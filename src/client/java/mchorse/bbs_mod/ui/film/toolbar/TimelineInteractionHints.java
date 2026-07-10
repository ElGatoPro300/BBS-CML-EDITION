package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
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

    /**
     * Viewport interaction hint: always bottom-left of the preview area, drawn in
     * {@link UIContext#postRender()} so it stays above the 3D viewport chrome
     * (gizmo bar, icon row).
     */
    public static void renderViewportHint(UIContext context, Area viewport, IKey hint, int bottomReserve)
    {
        if (hint == null)
        {
            return;
        }

        String text = hint.get();
        int[] card = computeHintCardPosition(viewport, bottomReserve, text, context.batcher.getFont());

        context.drawForegroundInteractionHint(text, card[0], card[1]);
    }

    public static void renderHint(UIContext context, Area area, IKey hint, TimelineToolbarDock dock)
    {
        if (hint == null)
        {
            return;
        }

        String text = hint.get();
        int[] card = computeHintCardPosition(area, dock, text, context.batcher.getFont());

        drawHintCard(context.batcher, card[0], card[1], text);
    }

    /**
     * Draw a full interaction hint card (primary glow, background, white border,
     * centered text). Used directly and from {@link UIContext#postRender()}.
     */
    public static void drawHintCard(Batcher2D batcher, int cardX, int cardY, String text)
    {
        FontRenderer font = batcher.getFont();
        int[] size = measureHintCard(font, text);
        int cardW = size[0];
        int cardH = size[1];
        int cardEx = cardX + cardW;
        int cardEy = cardY + cardH;
        int textX = cardX + TimelineToolbarSettings.INTERACTION_HINT_PADDING_X;
        int textY = cardY + computeHintTextY(font, cardH);

        batcher.dropShadow(
            cardX,
            cardY,
            cardEx,
            cardEy,
            TimelineToolbarSettings.MENU_SHADOW_OFFSET,
            TimelineToolbarSettings.getMenuShadowInner(),
            TimelineToolbarSettings.getMenuShadowOuter());
        batcher.box(cardX, cardY, cardEx, cardEy, TimelineToolbarSettings.INTERACTION_HINT_BACKGROUND);
        batcher.outline(cardX, cardY, cardEx, cardEy, TimelineToolbarSettings.INTERACTION_HINT_BORDER);
        batcher.text(text, textX, textY, TimelineToolbarSettings.INTERACTION_HINT_FG, false);
    }

    public static int[] measureHintCard(FontRenderer font, String text)
    {
        int textW = font.getWidth(text);
        int fontHeight = font.getRenderer().fontHeight;
        int cardW = textW + TimelineToolbarSettings.INTERACTION_HINT_PADDING_X * 2;
        int cardH = fontHeight + TimelineToolbarSettings.INTERACTION_HINT_PADDING_Y * 2;

        return new int[] {cardW, cardH};
    }

    private static int computeHintTextY(FontRenderer font, int cardH)
    {
        int fontHeight = font.getRenderer().fontHeight;

        return (cardH - fontHeight) / 2;
    }

    private static int[] computeHintCardPosition(Area viewport, int bottomReserve, String text, FontRenderer font)
    {
        int[] size = measureHintCard(font, text);
        int margin = TimelineToolbarSettings.INTERACTION_HINT_MARGIN;
        int cardX = viewport.x + margin;
        int cardY = viewport.ey() - size[1] - margin - bottomReserve;

        return new int[] {cardX, cardY};
    }

    private static int[] computeHintCardPosition(Area area, TimelineToolbarDock dock, String text, FontRenderer font)
    {
        int[] size = measureHintCard(font, text);
        int margin = TimelineToolbarSettings.INTERACTION_HINT_MARGIN;
        int cardW = size[0];
        int cardH = size[1];
        int cardX;
        int cardY;

        if (dock == null)
        {
            dock = TimelineToolbarDock.BOTTOM;
        }

        switch (dock)
        {
            case TOP:
                cardX = area.x + margin;
                cardY = area.y + margin;
                break;
            case LEFT:
                cardX = area.x + margin;
                cardY = area.ey() - cardH - margin;
                break;
            case RIGHT:
                cardX = area.ex() - cardW - margin;
                cardY = area.ey() - cardH - margin;
                break;
            case BOTTOM:
            default:
                cardX = area.x + margin;
                cardY = area.ey() - cardH - margin;
                break;
        }

        return new int[] {cardX, cardY};
    }

    private TimelineInteractionHints()
    {}
}
