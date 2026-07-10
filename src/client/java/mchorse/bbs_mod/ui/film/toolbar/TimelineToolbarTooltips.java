package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;

/**
 * Screen-edge aware positioning for timeline toolbar hover cards (collapsed
 * section labels, drag handle, menu disabled reasons).
 */
public final class TimelineToolbarTooltips
{
    /**
     * Default {@link mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D#textCard}
     * background/text padding.
     */
    private static final float TEXT_CARD_OFFSET = 3F;

    public static void drawForeground(UIContext context, String text, int mouseX, int mouseY, int color,
        int background)
    {
        if (text == null || text.isEmpty())
        {
            return;
        }

        FontRenderer font = context.batcher.getFont();
        int[] pos = computeTextPosition(context, font, text, mouseX, mouseY);

        context.drawForegroundTextCard(text, pos[0], pos[1], color, background);
    }

    public static int[] computeTextPosition(UIContext context, FontRenderer font, String text, int mouseX,
        int mouseY)
    {
        int offsetX = TimelineToolbarSettings.TOOLTIP_CURSOR_OFFSET_X;
        int offsetY = TimelineToolbarSettings.TOOLTIP_CURSOR_OFFSET_Y;
        int margin = TimelineToolbarSettings.MENU_SCREEN_MARGIN;
        int textW = font.getWidth(text);
        int textH = font.getHeight();
        int padding = (int) TEXT_CARD_OFFSET;

        int x = mouseX + offsetX;
        int y = mouseY + offsetY;
        int screenW = context.menu.width;
        int screenH = context.menu.height;

        if (x + textW + padding > screenW - margin)
        {
            /* Flip horizontally: card ends at the cursor instead of starting there. */
            x = mouseX - offsetX - textW;
        }

        if (y + textH + padding > screenH - margin)
        {
            /* Flip vertically: card ends at the cursor instead of starting there. */
            y = mouseY - offsetY - textH;
        }

        if (x - padding < margin)
        {
            x = margin + padding;
        }

        if (x + textW + padding > screenW - margin)
        {
            x = screenW - margin - textW - padding;
        }

        if (y - padding < margin)
        {
            y = margin + padding;
        }

        if (y + textH + padding > screenH - margin)
        {
            y = screenH - margin - textH - padding;
        }

        return new int[] {x, y};
    }

    private TimelineToolbarTooltips()
    {}
}
