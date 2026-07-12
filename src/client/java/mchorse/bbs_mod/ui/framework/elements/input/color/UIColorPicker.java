package mchorse.bbs_mod.ui.framework.elements.input.color;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.settings.values.ui.ValueColors;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.elements.utils.EventPropagation;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;

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
    public static final int PRIMARY_INPUT_TOP = 5;
    public static final int PRIMARY_INPUT_HEIGHT = 20;
    public static final int WRAP_HEADER_HEIGHT = 14;
    public static final int WRAP_INPUT_HEIGHT = 20;
    public static final int WRAP_OPACITY_HEIGHT = 16;
    public static final int SECTION_GAP = 5;

    public static ValueColors recentColors = new ValueColors("recent");

    public Color color = new Color();
    public Color wrapColor = new Color().set(Colors.WHITE);
    public float wrapOpacity;
    public Consumer<Integer> callback;
    public Consumer<Integer> wrapColorChangeCallback;
    public Consumer<Float> wrapOpacityChangeCallback;

    public UITextbox input;
    public UITextbox wrapInput;
    public UITrackpad wrapOpacityInput;
    public UIColorPalette recent;
    public UIColorPalette favorite;

    public boolean editAlpha;
    public boolean wrapSectionEnabled = true;
    public boolean wrapExpanded = false;

    public Area red = new Area();
    public Area green = new Area();
    public Area blue = new Area();
    public Area alpha = new Area();
    public Area wrapHeader = new Area();
    public Area wrapSwatch = new Area();

    protected int dragging = -1;
    protected Color hsv = new Color();
    protected UIColorPicker nestedWrapPicker;

    private int storedFavoriteH;
    private int storedRecentH;

    public static void renderAlphaPreviewQuad(Batcher2D batcher, int x1, int y1, int x2, int y2, Color color)
    {
        Matrix4f matrix4f = batcher.getContext().getMatrices().peek().getPositionMatrix();
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();

        builder.vertex(matrix4f, x1, y1, 0F).color(color.r, color.g, color.b, 1);
        builder.vertex(matrix4f, x1, y2, 0F).color(color.r, color.g, color.b, 1);
        builder.vertex(matrix4f, x2, y1, 0F).color(color.r, color.g, color.b, 1);
        builder.vertex(matrix4f, x2, y1, 0F).color(color.r, color.g, color.b, color.a);
        builder.vertex(matrix4f, x1, y2, 0F).color(color.r, color.g, color.b, color.a);
        builder.vertex(matrix4f, x2, y2, 0F).color(color.r, color.g, color.b, color.a);

        BufferRenderer.drawWithGlobalProgram(builder.end());
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

        this.wrapInput = new UITextbox(7, (string) ->
        {
            this.setWrapColor(Colors.parse(string), true);
        });
        this.wrapInput.setVisible(false);

        this.wrapOpacityInput = new UITrackpad((value) ->
        {
            this.setWrapOpacity(value.floatValue(), true);
        });
        this.wrapOpacityInput.limit(0D, 1D).increment(0.01D).values(0.1D, 0.01D, 0.05D);
        this.wrapOpacityInput.tooltip(UIKeys.COLOR_WRAP_OPACITY);
        this.wrapOpacityInput.setVisible(false);

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

        this.input.relative(this).set(5, PRIMARY_INPUT_TOP, 0, PRIMARY_INPUT_HEIGHT).w(1, -35);
        this.wrapInput.relative(this).w(1, -35).h(WRAP_INPUT_HEIGHT);
        this.wrapOpacityInput.relative(this).h(WRAP_OPACITY_HEIGHT).w(1, -10);
        this.favorite.relative(this).w(1F, -10);
        this.recent.relative(this).w(1F, -10);

        this.add(this.input, this.wrapInput, this.wrapOpacityInput, this.favorite, this.recent);

        this.eventPropagataion(EventPropagation.BLOCK_INSIDE).add(this.input, this.wrapInput, this.wrapOpacityInput, this.favorite, this.recent);
    }

    public UIColorPicker editAlpha()
    {
        this.editAlpha = true;
        this.input.textbox.setLength(9);

        return this;
    }

    public UIColorPicker withoutWrapSection()
    {
        this.wrapSectionEnabled = false;

        return this;
    }

    public void setWrapColorChangeCallback(Consumer<Integer> wrapColorChangeCallback)
    {
        this.wrapColorChangeCallback = wrapColorChangeCallback;
    }

    public void setWrapOpacityChangeCallback(Consumer<Float> wrapOpacityChangeCallback)
    {
        this.wrapOpacityChangeCallback = wrapOpacityChangeCallback;
    }

    public void setWrapColor(int color, boolean notify)
    {
        this.wrapColor.set(color, false);
        this.updateWrapField();

        if (notify)
        {
            this.notifyWrapColorChange();
        }
    }

    public void setWrapOpacity(float opacity, boolean notify)
    {
        this.wrapOpacity = MathUtils.clamp(opacity, 0F, 1F);
        this.wrapOpacityInput.setValue(this.wrapOpacity);

        if (notify)
        {
            this.notifyWrapOpacityChange();
        }
    }

    protected void notifyWrapColorChange()
    {
        if (this.wrapColorChangeCallback != null)
        {
            this.wrapColorChangeCallback.accept(this.wrapColor.getRGBColor());
        }
    }

    protected void notifyWrapOpacityChange()
    {
        if (this.wrapOpacityChangeCallback != null)
        {
            this.wrapOpacityChangeCallback.accept(this.wrapOpacity);
        }
    }

    public void updateWrapField()
    {
        this.wrapInput.setText(this.wrapColor.stringify(false));
    }

    protected int getWrapBlockHeight()
    {
        if (!this.wrapSectionEnabled)
        {
            return 0;
        }

        int height = WRAP_HEADER_HEIGHT;

        if (this.wrapExpanded)
        {
            height += SECTION_GAP + WRAP_INPUT_HEIGHT + SECTION_GAP + WRAP_OPACITY_HEIGHT;
        }

        return height;
    }

    protected int getWrapSectionTop()
    {
        return COLOR_PICKER_TOP + COLOR_PICKER_SIZE + SECTION_GAP;
    }

    protected int getPalettesTop()
    {
        int top = this.getWrapSectionTop();

        if (this.wrapSectionEnabled)
        {
            top += this.getWrapBlockHeight() + SECTION_GAP;
        }

        return top;
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
        this.resize();
    }

    private void setupSize()
    {
        int width = 10 + COLOR_PICKER_SIZE + COLOR_PICKER_GAP + COLOR_PICKER_BAR_WIDTH + (this.editAlpha ? COLOR_PICKER_GAP + COLOR_PICKER_BAR_WIDTH : 0);
        int recent = this.recent.colors.isEmpty() ? 0 : this.recent.getHeight(width - 10);
        int favorite = this.favorite.colors.isEmpty() ? 0 : this.favorite.getHeight(width - 10);
        int base = this.getPalettesTop() + 10;

        this.storedFavoriteH = favorite;
        this.storedRecentH = recent;

        this.w(width);
        base += favorite > 0 ? favorite + 15 : 0;
        base += recent > 0 ? recent + 15 : 0;

        this.h(base);
        this.favorite.h(favorite);
        this.recent.h(recent);
    }

    private void toggleWrapSection()
    {
        this.wrapExpanded = !this.wrapExpanded;
        this.wrapInput.setVisible(this.wrapExpanded);
        this.wrapOpacityInput.setVisible(this.wrapExpanded);
        this.updateWrapField();
        this.wrapOpacityInput.setValue(this.wrapOpacity);
        this.setupSize();
        this.resize();
    }

    private void openNestedWrapPicker(UIContext context)
    {
        if (this.nestedWrapPicker == null)
        {
            this.nestedWrapPicker = new UIColorPicker((color) ->
            {
                this.setWrapColor(color, true);
            }).withoutWrapSection();
        }

        if (this.nestedWrapPicker.hasParent())
        {
            this.nestedWrapPicker.removeFromParent();

            return;
        }

        UIElement parent = this.getParent();

        if (parent == null)
        {
            return;
        }

        parent.add(this.nestedWrapPicker);
        this.nestedWrapPicker.setColor(this.wrapColor.getRGBColor());
        this.nestedWrapPicker.setup(context.globalX(this.wrapSwatch.ex() + 4), context.globalY(this.wrapSwatch.y));
        this.nestedWrapPicker.bounds(context.menu.main, 2);
        this.nestedWrapPicker.resize();
    }

    private void renderWrapSection(UIContext context)
    {
        if (!this.wrapSectionEnabled)
        {
            return;
        }

        int arrowX = this.wrapHeader.x + 2;
        int arrowY = this.wrapHeader.my();

        context.batcher.icon(this.wrapExpanded ? Icons.DOWNLOAD : Icons.UPLOAD, arrowX, arrowY);
        context.batcher.text(UIKeys.COLOR_WRAP.get(), this.wrapHeader.x + 14, this.wrapHeader.y + 3);

        if (this.wrapExpanded)
        {
            int opacityLabelY = this.wrapOpacityInput.area.y - 10;

            context.batcher.text(UIKeys.COLOR_WRAP_OPACITY.get(), this.wrapOpacityInput.area.x, opacityLabelY, Colors.GRAY);

            int swatchHover = this.wrapSwatch.isInside(context) ? Colors.A50 : Colors.A25;

            context.batcher.box(this.wrapSwatch.x, this.wrapSwatch.y, this.wrapSwatch.ex(), this.wrapSwatch.ey(), this.wrapColor.getARGBColor());
            context.batcher.outline(this.wrapSwatch.x, this.wrapSwatch.y, this.wrapSwatch.ex(), this.wrapSwatch.ey(), swatchHover);
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
        if (this.wrapSectionEnabled && this.wrapExpanded)
        {
            int bodyY = this.getWrapSectionTop() + WRAP_HEADER_HEIGHT + SECTION_GAP;
            int opacityY = bodyY + WRAP_INPUT_HEIGHT + SECTION_GAP;

            this.wrapInput.set(5, bodyY, 0, WRAP_INPUT_HEIGHT).w(1, -35);
            this.wrapOpacityInput.set(5, opacityY, 0, WRAP_OPACITY_HEIGHT).w(1, -10);
        }

        int palettesTop = this.getPalettesTop();

        this.favorite.xy(5, palettesTop);

        if (this.storedFavoriteH > 0)
        {
            this.recent.xy(5, palettesTop + this.storedFavoriteH + 15);
        }
        else
        {
            this.recent.xy(5, palettesTop);
        }

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

        if (this.wrapSectionEnabled)
        {
            int headerY = this.getWrapSectionTop();
            int headerWidth = this.area.w - 10;

            this.wrapHeader.set(this.area.x + 5, this.area.y + headerY, headerWidth, WRAP_HEADER_HEIGHT);

            if (this.wrapExpanded)
            {
                int bodyY = headerY + WRAP_HEADER_HEIGHT + SECTION_GAP;

                this.wrapSwatch.set(this.area.ex() - 25, this.area.y + bodyY, 20, WRAP_INPUT_HEIGHT);
            }
            else
            {
                this.wrapSwatch.set(0, 0, 0, 0);
            }
        }
        else
        {
            this.wrapHeader.set(0, 0, 0, 0);
            this.wrapSwatch.set(0, 0, 0, 0);
        }
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (this.wrapSectionEnabled && this.wrapHeader.isInside(context) && context.mouseButton == 0)
        {
            this.toggleWrapSection();

            return true;
        }

        if (this.wrapSectionEnabled && this.wrapExpanded && this.wrapSwatch.isInside(context) && context.mouseButton == 0)
        {
            this.openNestedWrapPicker(context);

            return true;
        }

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

        if (!this.area.isInside(context) && (this.nestedWrapPicker == null || !this.nestedWrapPicker.hasParent()))
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
            if (this.nestedWrapPicker != null && this.nestedWrapPicker.hasParent())
            {
                this.nestedWrapPicker.removeFromParent();

                return true;
            }

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
        this.renderRect(context.batcher, this.area.ex() - 25, this.area.y + PRIMARY_INPUT_TOP, this.area.ex() - 5, this.area.y + PRIMARY_INPUT_TOP + PRIMARY_INPUT_HEIGHT);

        context.batcher.outline(this.area.ex() - 25, this.area.y + PRIMARY_INPUT_TOP, this.area.ex() - 5, this.area.y + PRIMARY_INPUT_TOP + PRIMARY_INPUT_HEIGHT, Colors.A25);

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

        this.renderWrapSection(context);

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
