package mchorse.bbs_mod.ui.dashboard.textures;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.color.UIColorPicker;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/**
 * Inline-only color picker for texture editor side panel.
 * Unlike popup pickers, this widget never removes itself from parent.
 */
public class UITextureInlineColorPicker extends UIColorPicker
{
    public UITextureInlineColorPicker(Consumer<Integer> callback)
    {
        super(callback);

        this.input.background(true).noBorder();
        this.input.setColor(0xf0f0f0, false);
        this.input.relative(this).set(30, 5, 0, 20).w(1, -34);
        this.favorite.setVisible(false);
        this.recent.setVisible(false);
    }

    @Override
    public void resize()
    {
        super.resize();

        int x = this.area.x + 5;
        int y = this.area.y + COLOR_PICKER_TOP;
        int width = this.area.w - 10;
        int alphaSpace = this.editAlpha ? COLOR_PICKER_BAR_WIDTH + COLOR_PICKER_GAP : 0;
        int selectorWidth = Math.max(64, width - COLOR_PICKER_BAR_WIDTH - COLOR_PICKER_GAP - alphaSpace);
        int selectorHeight = Math.max(56, this.area.h - COLOR_PICKER_TOP - 8);

        this.red.set(x, y, selectorWidth, selectorHeight);
        this.green.set(this.red.ex() + COLOR_PICKER_GAP, y, COLOR_PICKER_BAR_WIDTH, selectorHeight);
        this.blue.set(this.green.ex() + COLOR_PICKER_GAP, y, 0, 0);

        if (this.editAlpha)
        {
            this.alpha.set(this.green.ex() + COLOR_PICKER_GAP, y, COLOR_PICKER_BAR_WIDTH, selectorHeight);
        }
        else
        {
            this.alpha.set(0, 0, 0, 0);
        }
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (!this.area.isInside(context))
        {
            return false;
        }

        return super.subMouseClicked(context);
    }

    @Override
    protected boolean subKeyPressed(UIContext context)
    {
        if (context.isPressed(GLFW.GLFW_KEY_ESCAPE))
        {
            return true;
        }

        return super.subKeyPressed(context);
    }

    @Override
    public void render(UIContext context)
    {
        if (this.dragging >= 0)
        {
            if (this.dragging == 1)
            {
                float saturation = MathUtils.clamp((context.mouseX - this.red.x) / (float) this.red.w, 0F, 1F);
                float value = 1F - MathUtils.clamp((context.mouseY - this.red.y) / (float) this.red.h, 0F, 1F);
                this.hsv.g = saturation;
                this.hsv.b = value;
            }
            else if (this.dragging == 2)
            {
                float hue = MathUtils.clamp((context.mouseY - this.green.y) / (float) this.green.h, 0F, 1F);
                this.hsv.r = hue;
            }
            else if (this.dragging == 4 && this.editAlpha)
            {
                float alpha = 1F - MathUtils.clamp((context.mouseY - this.alpha.y) / (float) this.alpha.h, 0F, 1F);
                this.hsv.a = alpha;
            }

            Colors.HSVtoRGB(this.color, this.hsv.r, this.hsv.g, this.hsv.b);
            this.color.a = this.hsv.a;
            this.updateColor();
        }

        this.area.render(context.batcher, 0xff10141b);
        context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xff1f2733);
        context.batcher.box(this.input.area.x - 2, this.input.area.y - 2, this.input.area.ex() + 2, this.input.area.ey() + 2, 0xff080a0f);
        context.batcher.outline(this.input.area.x - 2, this.input.area.y - 2, this.input.area.ex() + 2, this.input.area.ey() + 2, 0xff252d3a);

        Color temp = new Color();
        Colors.HSVtoRGB(temp, this.hsv.r, 1F, 1F);
        context.batcher.box(this.red.x, this.red.y, this.red.ex(), this.red.ey(), temp.getARGBColor());
        context.batcher.gradientHBox(this.red.x, this.red.y, this.red.ex(), this.red.ey(), Colors.WHITE, Colors.setA(Colors.WHITE, 0F));
        context.batcher.gradientVBox(this.red.x, this.red.y, this.red.ex(), this.red.ey(), 0, 0xff000000);

        for (int i = 0; i < 6; i++)
        {
            Colors.HSVtoRGB(temp, i / 6F, 1F, 1F);
            int top = temp.getARGBColor();
            Colors.HSVtoRGB(temp, (i + 1) / 6F, 1F, 1F);
            int bottom = temp.getARGBColor();
            int y1 = this.green.y + (int) (this.green.h * (i / 6F));
            int y2 = this.green.y + (int) (this.green.h * ((i + 1) / 6F));
            context.batcher.gradientVBox(this.green.x, y1, this.green.ex(), y2, top, bottom);
        }

        if (this.editAlpha)
        {
            context.batcher.iconArea(Icons.CHECKBOARD, this.alpha.x, this.alpha.y, this.alpha.w, this.alpha.h);
            Colors.HSVtoRGB(temp, this.hsv.r, this.hsv.g, this.hsv.b);
            temp.a = 1F;
            int top = temp.getARGBColor();
            temp.a = 0F;
            int bottom = temp.getARGBColor();
            context.batcher.gradientVBox(this.alpha.x, this.alpha.y, this.alpha.ex(), this.alpha.ey(), top, bottom);
        }

        context.batcher.outline(this.red.x, this.red.y, this.red.ex(), this.red.ey(), Colors.A75);
        context.batcher.outline(this.green.x, this.green.y, this.green.ex(), this.green.ey(), Colors.A75);

        if (this.editAlpha)
        {
            context.batcher.outline(this.alpha.x, this.alpha.y, this.alpha.ex(), this.alpha.ey(), Colors.A75);
        }

        int sx = this.red.x + (int) (this.red.w * this.hsv.g);
        int sy = this.red.y + (int) (this.red.h * (1F - this.hsv.b));
        int hx = this.green.mx();
        int hy = this.green.y + (int) (this.green.h * this.hsv.r);

        context.batcher.box(sx - 4, sy - 4, sx + 4, sy + 4, Colors.A100);
        context.batcher.box(sx - 3, sy - 3, sx + 3, sy + 3, Colors.WHITE);
        context.batcher.box(hx - 4, hy - 4, hx + 4, hy + 4, Colors.A100);
        context.batcher.box(hx - 3, hy - 3, hx + 3, hy + 3, Colors.WHITE);

        if (this.editAlpha)
        {
            int ax = this.alpha.mx();
            int ay = this.alpha.y + (int) (this.alpha.h * (1F - this.hsv.a));
            context.batcher.box(ax - 4, ay - 4, ax + 4, ay + 4, Colors.A100);
            context.batcher.box(ax - 3, ay - 3, ax + 3, ay + 3, Colors.WHITE);
        }

        this.input.render(context);
        this.renderLockedArea(context);
    }
}
