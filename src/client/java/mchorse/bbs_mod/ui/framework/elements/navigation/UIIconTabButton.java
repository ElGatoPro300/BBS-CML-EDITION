package mchorse.bbs_mod.ui.framework.elements.navigation;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.function.Consumer;

public class UIIconTabButton extends UIButton
{
    private static final int TOP_CORNER_RADIUS = 5;
    private static final int[] PIXEL_TOP_CORNER_INSETS = {5, 3, 2, 1, 1};
    private static final int REMOVE_GUTTER = 24;
    private static final int REMOVE_BUTTON_SIZE = 14;
    private static final int CONTENT_PADDING_LEFT = 6;

    private final Icon icon;
    private Consumer<UIIconTabButton> removeCallback;
    private final Color gradient = new Color();

    public UIIconTabButton(IKey label, Icon icon, Consumer<UIButton> callback)
    {
        super(label, callback);

        this.icon = icon;
    }

    public UIIconTabButton removable(Consumer<UIIconTabButton> callback)
    {
        this.removeCallback = callback;

        return this;
    }

    public boolean isRemovable()
    {
        return this.removeCallback != null;
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (context.mouseButton == 0 && this.isRemovable() && this.isRemoveInside(context))
        {
            UIUtils.playClick();
            this.removeCallback.accept(this);

            return true;
        }

        return super.subMouseClicked(context);
    }

    @Override
    protected void renderSkin(UIContext context)
    {
        int color = Colors.A100 + (this.custom ? this.customColor : BBSSettings.primaryColor.get());

        if (this.hover)
        {
            color = Colors.mulRGB(color, 0.85F);
        }

        if (this.background)
        {
            this.renderPixelTopRoundedBackground(context, color);
        }

        FontRenderer font = context.batcher.getFont();
        int removeGutter = this.isRemovable() ? REMOVE_GUTTER : 0;
        String rawLabel = this.label.get();
        boolean iconOnly = this.icon != null && (rawLabel == null || rawLabel.isEmpty());
        String label = iconOnly ? "" : font.limitToWidth(rawLabel, this.area.w - 26 - removeGutter - CONTENT_PADDING_LEFT);
        int iconWidth = this.icon == null ? 0 : this.icon.w;
        int iconHeight = this.icon == null ? 0 : this.icon.h;
        int fontHeight = font.getHeight();
        int contentHeight = Math.max(iconHeight, fontHeight);
        int contentY = this.area.my(contentHeight);
        int gap = this.icon == null || iconOnly ? 0 : 4;
        int startX = iconOnly ? this.area.mx(iconWidth) : this.area.x + CONTENT_PADDING_LEFT;
        int iconY = contentY + (contentHeight - iconHeight) / 2;
        int y = contentY + (contentHeight - fontHeight) / 2;

        if (this.icon != null)
        {
            context.batcher.icon(this.icon, this.textColor, startX, iconY);
        }

        context.batcher.text(label, startX + iconWidth + gap, y, Colors.mulRGB(this.textColor, this.hover ? 0.9F : 1F), this.textShadow);

        if (this.isRemovable())
        {
            boolean removeHover = this.isRemoveInside(context);
            int removeAreaX1 = this.area.ex() - REMOVE_GUTTER + 6;
            int removeColor = removeHover ? Colors.WHITE : Colors.LIGHTEST_GRAY;

            context.batcher.textShadow("X", removeAreaX1 + 4, y, removeColor);
        }

        this.renderLockedArea(context);
    }

    private boolean isRemoveInside(UIContext context)
    {
        return this.area.isInside(context) && context.mouseX >= this.area.ex() - REMOVE_GUTTER;
    }

    private void renderPixelTopRoundedBackground(UIContext context, int color)
    {
        int x1 = this.area.x;
        int y1 = this.area.y;
        int x2 = this.area.ex();
        int y2 = this.area.ey();
        int width = x2 - x1;
        int height = y2 - y1;

        if (width <= 2 || height <= 2)
        {
            this.area.render(context.batcher, color);

            return;
        }

        int baseColor = color & Colors.RGB;
        int topColor = baseColor;
        int bottomColor = Colors.A75 | baseColor;
        int stripeColor = Colors.A100 | baseColor;
        int stripeHeight = Math.min(2, height);
        int gradientBottom = y2 - stripeHeight;
        int gradientHeight = Math.max(1, gradientBottom - y1);
        int rows = Math.min(Math.min(TOP_CORNER_RADIUS, height), PIXEL_TOP_CORNER_INSETS.length);

        for (int y = y1; y < gradientBottom; y++)
        {
            float factor = gradientHeight <= 0 ? 1F : ((y + 0.5F) - y1) / (float) gradientHeight;
            factor = Math.max(0F, Math.min(1F, factor));
            Colors.interpolate(this.gradient, topColor, bottomColor, factor);

            int inset = y - y1 < rows ? PIXEL_TOP_CORNER_INSETS[y - y1] : 0;
            int rowX1 = x1 + inset;
            int rowX2 = x2 - inset;

            if (rowX2 > rowX1)
            {
                context.batcher.box(rowX1, y, rowX2, y + 1, this.gradient.getARGBColor());
            }
        }

        if (stripeHeight > 0)
        {
            context.batcher.box(x1, gradientBottom, x2, y2, stripeColor);
        }
    }
}
