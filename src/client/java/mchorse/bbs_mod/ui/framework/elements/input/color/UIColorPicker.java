package mchorse.bbs_mod.ui.framework.elements.input.color;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.settings.values.ui.ValueColors;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.elements.utils.EventPropagation;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;

import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;

import org.joml.Matrix4f;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;

import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/**
 * Color picker element
 *
 * This is the one that is responsible for picking colors
 */
public class UIColorPicker extends UIElement
{
    public static final int COLOR_PICKER_SIZE = 120;
    public static final int COLOR_PICKER_TOP = 30;
    public static final int COLOR_PICKER_GAP = 4;
    public static final int COLOR_PICKER_BAR_WIDTH = 14;

    public static ValueColors recentColors = new ValueColors("recent");

    public Color color = new Color();
    public Consumer<Integer> callback;

    public UITextbox input;
    public UIColorPalette recent;
    public UIColorPalette favorite;

    public boolean editAlpha;

    public Area red = new Area();
    public Area green = new Area();
    public Area blue = new Area();
    public Area alpha = new Area();

    protected int dragging = -1;
    protected Color hsv = new Color();

    public static void renderAlphaPreviewQuad(Batcher2D batcher, int x1, int y1, int x2, int y2, Color color)
    {
        Matrix4f matrix4f = new Matrix4f();
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        GlStateManager._enableBlend();

        builder.vertex(matrix4f, x1, y1, 0F).color(color.r, color.g, color.b, 1);
        builder.vertex(matrix4f, x1, y2, 0F).color(color.r, color.g, color.b, 1);
        builder.vertex(matrix4f, x2, y1, 0F).color(color.r, color.g, color.b, 1);
        builder.vertex(matrix4f, x2, y1, 0F).color(color.r, color.g, color.b, color.a);
        builder.vertex(matrix4f, x1, y2, 0F).color(color.r, color.g, color.b, color.a);
        builder.vertex(matrix4f, x2, y2, 0F).color(color.r, color.g, color.b, color.a);

        RenderLayers.debugFilledBox().draw(builder.end());
    }

    public UIColorPicker(Consumer<Integer> callback)
    {
        super();

        this.callback = callback;

        this.input = new UITextbox(7, (string) ->
        {
            this.setValue(Colors.parse(string));
            this.callback();
        });
        this.input.context((menu) -> menu.action(Icons.FAVORITE, UIKeys.COLOR_CONTEXT_FAVORITES_ADD, () -> this.addToFavorites(this.color)));

        this.recent = new UIColorPalette((color) ->
        {
            this.setColor(color.getARGBColor());
            this.updateColor();
        }).colors(recentColors.getCurrentColors());

        this.recent.context((menu) ->
        {
            int index = this.recent.getIndex(this.getContext());

            if (this.recent.hasColor(index))
            {
                menu.action(Icons.FAVORITE, UIKeys.COLOR_CONTEXT_FAVORITES_ADD, () -> this.addToFavorites(this.recent.colors.get(index)));
            }
        });

        this.favorite = new UIColorPalette((color) ->
        {
            this.setColor(color.getARGBColor());
            this.updateColor();
        }).colors(BBSSettings.favoriteColors.getCurrentColors());

        this.favorite.context((menu) ->
        {
            int index = this.favorite.getIndex(this.getContext());

            if (this.favorite.hasColor(index))
            {
                menu.action(Icons.REMOVE, UIKeys.COLOR_CONTEXT_FAVORITES_REMOVE, () -> this.removeFromFavorites(index));
            }
        });

        this.input.relative(this).set(5, 5, 0, 20).w(1, -35);
        this.favorite.relative(this).xy(5, COLOR_PICKER_TOP + COLOR_PICKER_SIZE + 10).w(1F, -10);
        this.recent.relative(this.favorite).w(1F);

        this.eventPropagataion(EventPropagation.BLOCK_INSIDE).add(this.input, this.favorite, this.recent);
    }

    public UIColorPicker editAlpha()
    {
        this.editAlpha = true;
        this.input.textbox.setLength(9);

        return this;
    }

    public void updateField()
    {
        this.input.setText(this.color.stringify(this.editAlpha));
    }

    public void updateColor()
    {
        this.updateField();
        this.callback();
    }

    protected void callback()
    {
        if (this.callback != null)
        {
            this.callback.accept(this.editAlpha ? this.color.getARGBColor() : this.color.getRGBColor());
        }
    }

    public void setColor(int color)
    {
        this.setValue(color);
        this.updateField();
    }

    public void setValue(int color)
    {
        this.color.set(color, this.editAlpha);
        Colors.RGBtoHSV(this.hsv, this.color.r, this.color.g, this.color.b);
        this.hsv.a = this.color.a;
    }

    public void setup(int x, int y)
    {
        this.xy(x, y);
        this.setupSize();
    }

    private void setupSize()
    {
        int width = 10 + COLOR_PICKER_SIZE + COLOR_PICKER_GAP + COLOR_PICKER_BAR_WIDTH + (this.editAlpha ? COLOR_PICKER_GAP + COLOR_PICKER_BAR_WIDTH : 0);
        int recent = this.recent.colors.isEmpty() ? 0 : this.recent.getHeight(width - 10);
        int favorite = this.favorite.colors.isEmpty() ? 0 : this.favorite.getHeight(width - 10);
        int base = COLOR_PICKER_TOP + COLOR_PICKER_SIZE + 10;

        this.w(width);
        base += favorite > 0 ? favorite + 15 : 0;
        base += recent > 0 ? recent + 15 : 0;

        this.h(base);
        this.favorite.h(favorite);
        this.recent.h(recent);
        this.favorite.y(COLOR_PICKER_TOP + COLOR_PICKER_SIZE + 10);

        if (favorite > 0)
        {
            this.recent.y(1F, 15);
        }
        else
        {
            this.recent.y(0);
        }
    }

    /* Managing recent and favorite colors */

    private void addToRecent()
    {
        recentColors.addColor(this.color);
    }

    private void addToFavorites(Color color)
    {
        BBSSettings.favoriteColors.addColor(color);

        this.setupSize();
        this.resize();
    }

    private void removeFromFavorites(int index)
    {
        BBSSettings.favoriteColors.remove(index);

        this.setupSize();
        this.resize();
    }

    /* GuiElement overrides */

    @Override
    public void resize()
    {
        super.resize();

        int x = this.area.x + 5;
        int y = this.area.y + COLOR_PICKER_TOP;
        int width = this.area.w - 10;
        int alphaSpace = this.editAlpha ? COLOR_PICKER_BAR_WIDTH + COLOR_PICKER_GAP : 0;
        int maxSquareWidth = width - COLOR_PICKER_BAR_WIDTH - COLOR_PICKER_GAP - alphaSpace;
        int squareSize = Math.max(40, Math.min(COLOR_PICKER_SIZE, maxSquareWidth));

        this.red.set(x, y, squareSize, squareSize);
        this.green.set(this.red.ex() + COLOR_PICKER_GAP, y, COLOR_PICKER_BAR_WIDTH, squareSize);
        this.blue.set(this.green.ex() + COLOR_PICKER_GAP, y, 0, 0);

        if (this.editAlpha)
        {
            this.alpha.set(this.green.ex() + COLOR_PICKER_GAP, y, COLOR_PICKER_BAR_WIDTH, squareSize);
        }
        else
        {
            this.alpha.set(0, 0, 0, 0);
        }
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (this.red.isInside(context))
        {
            this.dragging = 1;

            return true;
        }
        else if (this.green.isInside(context))
        {
            this.dragging = 2;

            return true;
        }
        else if (this.alpha.isInside(context) && this.editAlpha)
        {
            this.dragging = 4;

            return true;
        }

        if (!this.area.isInside(context))
        {
            this.removeFromParent();
            this.addToRecent();
        }

        return super.subMouseClicked(context);
    }

    @Override
    public boolean subMouseReleased(UIContext context)
    {
        this.dragging = -1;

        return super.subMouseReleased(context);
    }

    @Override
    protected boolean subKeyPressed(UIContext context)
    {
        if (context.isPressed(GLFW.GLFW_KEY_ESCAPE))
        {
            this.removeFromParent();
            this.addToRecent();

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

        this.area.render(context.batcher, Colors.LIGHTEST_GRAY);
        this.renderRect(context.batcher, this.area.ex() - 25, this.area.y + 5, this.area.ex() - 5, this.area.y + 25);

        context.batcher.outline(this.area.ex() - 25, this.area.y + 5, this.area.ex() - 5, this.area.y + 25, Colors.A25);

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

        context.batcher.outline(this.red.x, this.red.y, this.red.ex(), this.red.ey(), 0x44000000);
        context.batcher.outline(this.green.x, this.green.y, this.green.ex(), this.green.ey(), 0x44000000);

        if (this.editAlpha)
        {
            context.batcher.outline(this.alpha.x, this.alpha.y, this.alpha.ex(), this.alpha.ey(), 0x44000000);
        }

        this.renderMarker(context.batcher, this.red.x + (int) (this.red.w * this.hsv.g), this.red.y + (int) (this.red.h * (1F - this.hsv.b)));
        this.renderMarker(context.batcher, this.green.mx(), this.green.y + (int) (this.green.h * this.hsv.r));

        if (this.editAlpha)
        {
            this.renderMarker(context.batcher, this.alpha.mx(), this.alpha.y + (int) (this.alpha.h * (1F - this.hsv.a)));
        }

        if (!this.favorite.colors.isEmpty())
        {
            context.batcher.text(UIKeys.COLOR_FAVORITE.get(), this.favorite.area.x, this.favorite.area.y - 10, Colors.GRAY);
        }

        if (!this.recent.colors.isEmpty())
        {
            context.batcher.text(UIKeys.COLOR_RECENT.get(), this.recent.area.x, this.recent.area.y - 10, Colors.GRAY);
        }

        super.render(context);
    }

    public void renderRect(Batcher2D batcher, int x1, int y1, int x2, int y2)
    {
        if (this.editAlpha)
        {
            batcher.iconArea(Icons.CHECKBOARD, x1, y1, x2 - x1, y2 - y1);
            renderAlphaPreviewQuad(batcher, x1, y1, x2, y2, this.color);
        }
        else
        {
            batcher.box(x1, y1, x2, y2, this.color.getARGBColor());
        }
    }

    private void renderMarker(Batcher2D batcher, int x, int y)
    {
        batcher.box(x - 4, y - 4, x + 4, y + 4, Colors.A100);
        batcher.box(x - 3, y - 3, x + 3, y + 3, Colors.WHITE);
        batcher.box(x - 2, y - 2, x + 2, y + 2, Colors.LIGHTEST_GRAY);
    }
}
