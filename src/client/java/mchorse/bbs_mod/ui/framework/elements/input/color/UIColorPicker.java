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

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

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
    public static final int COLOR_PICKER_GAP = 4;
    public static final int COLOR_PICKER_BAR_WIDTH = 14;
    public static final int PRIMARY_INPUT_TOP = 5;
    public static final int PRIMARY_INPUT_HEIGHT = 20;
    public static final int SECONDARY_HEADER_HEIGHT = 14;
    public static final int SECONDARY_BODY_HEIGHT = 20;
    public static final int SECTION_GAP = 5;
    public static final int COLOR_PICKER_TOP = PRIMARY_INPUT_TOP + PRIMARY_INPUT_HEIGHT + SECTION_GAP;

    public static ValueColors recentColors = new ValueColors("recent");

    public Color color = new Color();
    public Color secondaryColor = new Color().set(0F, 0F, 0F, 1F);
    public Consumer<Integer> callback;
    public Consumer<Integer> secondaryChangeCallback;

    public UITextbox input;
    public UITextbox secondaryInput;
    public UIColorPalette recent;
    public UIColorPalette favorite;

    public boolean editAlpha;
    public boolean secondarySectionEnabled = true;
    public boolean secondaryExpanded = false;

    public Area red = new Area();
    public Area green = new Area();
    public Area blue = new Area();
    public Area alpha = new Area();
    public Area secondaryHeader = new Area();
    public Area secondarySwatch = new Area();

    protected int dragging = -1;
    protected Color hsv = new Color();
    protected UIColorPicker nestedSecondaryPicker;

    /* Cached palette heights set in setupSize() so resize() can use them before super.resize() runs. */
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

        this.secondaryInput = new UITextbox(7, (string) ->
        {
            this.setSecondaryColor(Colors.parse(string), true);
        });

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
        this.secondaryInput.relative(this).w(1, -35).h(SECONDARY_BODY_HEIGHT);
        this.favorite.relative(this).w(1F, -10);
        this.recent.relative(this).w(1F, -10);

        this.add(this.input, this.secondaryInput, this.favorite, this.recent);
        this.secondaryInput.setVisible(false);

        this.eventPropagataion(EventPropagation.BLOCK_INSIDE).add(this.input, this.secondaryInput, this.favorite, this.recent);
    }

    public UIColorPicker editAlpha()
    {
        this.editAlpha = true;
        this.input.textbox.setLength(9);

        return this;
    }

    public UIColorPicker withoutSecondarySection()
    {
        this.secondarySectionEnabled = false;

        return this;
    }

    public void setSecondaryChangeCallback(Consumer<Integer> secondaryChangeCallback)
    {
        this.secondaryChangeCallback = secondaryChangeCallback;
    }

    public void setSecondaryColor(int color, boolean notify)
    {
        this.secondaryColor.set(color, false);
        this.updateSecondaryField();
        Colors.pickerHSVFromRGB(this.hsv, this.color, this.secondaryColor);

        if (notify)
        {
            this.notifySecondaryChange();
        }
    }

    protected void notifySecondaryChange()
    {
        if (this.secondaryChangeCallback != null)
        {
            this.secondaryChangeCallback.accept(this.secondaryColor.getRGBColor());
        }
    }

    public void updateSecondaryField()
    {
        this.secondaryInput.setText(this.secondaryColor.stringify(false));
    }

    protected int getSecondaryBlockHeight()
    {
        if (!this.secondarySectionEnabled)
        {
            return 0;
        }

        int height = SECONDARY_HEADER_HEIGHT;

        if (this.secondaryExpanded)
        {
            height += SECTION_GAP + SECONDARY_BODY_HEIGHT;
        }

        return height;
    }

    protected int getColorSquareTop()
    {
        return PRIMARY_INPUT_TOP + PRIMARY_INPUT_HEIGHT + SECTION_GAP;
    }

    protected int getSecondarySectionTop()
    {
        return this.getColorSquareTop() + COLOR_PICKER_SIZE + SECTION_GAP;
    }

    protected int getPalettesTop()
    {
        int top = this.getSecondarySectionTop();

        if (this.secondarySectionEnabled)
        {
            top += this.getSecondaryBlockHeight() + SECTION_GAP;
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
        Colors.pickerHSVFromRGB(this.hsv, this.color, this.secondaryColor);
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

    private void toggleSecondarySection()
    {
        this.secondaryExpanded = !this.secondaryExpanded;
        this.secondaryInput.setVisible(this.secondaryExpanded);
        this.updateSecondaryField();
        this.setupSize();
        this.resize();
    }

    private void openNestedSecondaryPicker(UIContext context)
    {
        if (this.nestedSecondaryPicker == null)
        {
            this.nestedSecondaryPicker = new UIColorPicker((color) ->
            {
                this.setSecondaryColor(color, true);
            }).withoutSecondarySection();
        }

        if (this.nestedSecondaryPicker.hasParent())
        {
            this.nestedSecondaryPicker.removeFromParent();

            return;
        }

        UIElement parent = this.getParent();

        if (parent == null)
        {
            return;
        }

        parent.add(this.nestedSecondaryPicker);
        this.nestedSecondaryPicker.setColor(this.secondaryColor.getRGBColor());
        this.nestedSecondaryPicker.setup(context.globalX(this.secondarySwatch.ex() + 4), context.globalY(this.secondarySwatch.y));
        this.nestedSecondaryPicker.bounds(context.menu.main, 2);
        this.nestedSecondaryPicker.resize();
    }

    /* GuiElement overrides */

    @Override
    public void resize()
    {
        /* Step 1: update children flex values BEFORE super.resize() so they are applied correctly. */

        /* Position secondary input when the section is expanded */
        if (this.secondarySectionEnabled && this.secondaryExpanded)
        {
            int bodyY = this.getSecondarySectionTop() + SECONDARY_HEADER_HEIGHT + SECTION_GAP;

            this.secondaryInput.set(5, bodyY, 0, SECONDARY_BODY_HEIGHT).w(1, -35);
        }

        /* Position palette elements using heights stored by the last setupSize() call */
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

        /* Step 2: let the framework apply all flex values to actual pixel areas */
        super.resize();

        /* Step 3: compute manual areas that depend on this.area being finalised */
        int x = this.area.x + 5;
        int y = this.area.y + this.getColorSquareTop();
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

        if (this.secondarySectionEnabled)
        {
            int headerY = this.getSecondarySectionTop();
            int headerWidth = this.area.w - 10;

            this.secondaryHeader.set(this.area.x + 5, this.area.y + headerY, headerWidth, SECONDARY_HEADER_HEIGHT);

            if (this.secondaryExpanded)
            {
                int bodyY = headerY + SECONDARY_HEADER_HEIGHT + SECTION_GAP;

                this.secondarySwatch.set(this.area.ex() - 25, this.area.y + bodyY, 20, SECONDARY_BODY_HEIGHT);
            }
            else
            {
                this.secondarySwatch.set(0, 0, 0, 0);
            }
        }
        else
        {
            this.secondaryHeader.set(0, 0, 0, 0);
            this.secondarySwatch.set(0, 0, 0, 0);
        }
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (this.secondarySectionEnabled && this.secondaryHeader.isInside(context) && context.mouseButton == 0)
        {
            this.toggleSecondarySection();

            return true;
        }

        if (this.secondarySectionEnabled && this.secondaryExpanded && this.secondarySwatch.isInside(context) && context.mouseButton == 0)
        {
            this.openNestedSecondaryPicker(context);

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

        if (!this.area.isInside(context) && (this.nestedSecondaryPicker == null || !this.nestedSecondaryPicker.hasParent()))
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
            if (this.nestedSecondaryPicker != null && this.nestedSecondaryPicker.hasParent())
            {
                this.nestedSecondaryPicker.removeFromParent();

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

            Colors.pickerColorFromHSV(this.color, this.secondaryColor, this.hsv.r, this.hsv.g, this.hsv.b);
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
        context.batcher.gradientVBox(this.red.x, this.red.y, this.red.ex(), this.red.ey(), 0, this.secondaryColor.getARGBColor());

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

        if (this.secondarySectionEnabled)
        {
            this.renderSecondarySection(context);
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

    protected void renderSecondarySection(UIContext context)
    {
        boolean headerHover = this.secondaryHeader.isInside(context);
        int headerBackground = Colors.setA(BBSSettings.primaryColor.get(), headerHover ? 0.5F : 0.3F);
        int textHeight = context.batcher.getFont().getHeight();

        context.batcher.box(this.secondaryHeader.x, this.secondaryHeader.y, this.secondaryHeader.ex(), this.secondaryHeader.ey(), headerBackground);
        context.batcher.icon(this.secondaryExpanded ? Icons.ARROW_DOWN : Icons.ARROW_RIGHT, Colors.WHITE, this.secondaryHeader.x + 4, this.secondaryHeader.my(), 0F, 0.5F);
        context.batcher.textShadow(UIKeys.COLOR_SECONDARY.get(), this.secondaryHeader.x + 18, this.secondaryHeader.my() - textHeight / 2, Colors.WHITE);

        if (this.secondaryExpanded)
        {
            context.batcher.box(this.secondarySwatch.x, this.secondarySwatch.y, this.secondarySwatch.ex(), this.secondarySwatch.ey(), this.secondaryColor.getARGBColor());

            if (this.secondarySwatch.isInside(context))
            {
                context.batcher.outline(this.secondarySwatch.x, this.secondarySwatch.y, this.secondarySwatch.ex(), this.secondarySwatch.ey(), Colors.WHITE);
            }
            else
            {
                context.batcher.outline(this.secondarySwatch.x, this.secondarySwatch.y, this.secondarySwatch.ex(), this.secondarySwatch.ey(), Colors.A25);
            }
        }
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
