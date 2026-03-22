package mchorse.bbs_mod.ui.framework.elements.utils;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.resources.Pixels;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import org.joml.Matrix4f;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.InputStream;


public class Batcher2D
{
    private static FontRenderer fontRenderer = new FontRenderer();

    private DrawContext context;
    private FontRenderer font;
    private static final Map<Integer, Identifier> NATIVE_IDS = new HashMap<>();

    public static FontRenderer getDefaultTextRenderer()
    {
        fontRenderer.setRenderer(MinecraftClient.getInstance().textRenderer);

        return fontRenderer;
    }

    public Batcher2D(DrawContext context)
    {
        this.context = context;
        this.font = getDefaultTextRenderer();
    }

    public void setContext(DrawContext context)
    {
        this.context = context;
    }

    public DrawContext getContext()
    {
        return this.context;
    }

    public FontRenderer getFont()
    {
        return this.font;
    }

    /* Screen space clipping */

    public void clip(Area area, UIContext context)
    {
        this.clip(area.x, area.y, area.w, area.h, context);
    }

    public void clip(int x, int y, int w, int h, UIContext context)
    {
        this.clip(context.globalX(x), context.globalY(y), w, h, context.menu.width, context.menu.height);
    }

    /**
     * Scissor (clip) the screen
     */
    public void clip(int x, int y, int w, int h, int sw, int sh)
    {
        this.context.enableScissor(x, y, x + w, y + h);
    }

    public void unclip(UIContext context)
    {
        this.unclip(context.menu.width, context.menu.height);
    }

    public void unclip(int sw, int sh)
    {
        this.context.disableScissor();
    }

    /* Solid rectangles */

    public void normalizedBox(float x1, float y1, float x2, float y2, int color)
    {
        float temp = x1;

        x1 = Math.min(x1, x2);
        x2 = Math.max(temp, x2);

        temp = y1;

        y1 = Math.min(y1, y2);
        y2 = Math.max(temp, y2);

        this.box(x1, y1, x2, y2, color);
    }

    public void box(float x1, float y1, float x2, float y2, int color)
    {
        this.context.fill(RenderPipelines.GUI, (int) x1, (int) y1, (int) x2, (int) y2, color);
    }

    public void box(float x, float y, float w, float h, int color1, int color2, int color3, int color4)
    {
        int top = color1;
        int bottom = color4;
        this.context.fillGradient((int) x, (int) y, (int) (x + w), (int) (y + h), top, bottom);
    }

    /**
     * Draw an anti-aliased-looking line segment by rendering a thin quad between two points.
     * The line is axis-independent (supports arbitrary angle) with given thickness in pixels.
     */
    public void line(float x1, float y1, float x2, float y2, float thickness, int color)
    {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);

        if (len <= 0.0001f)
        {
            // Fallback to a small box when points overlap
            this.box(x1 - thickness * 0.5f, y1 - thickness * 0.5f, x1 + thickness * 0.5f, y1 + thickness * 0.5f, color);
            return;
        }

        int steps = Math.max(1, (int) Math.ceil(len));
        for (int i = 0; i <= steps; i++)
        {
            float t = i / (float) steps;
            float px = x1 + dx * t;
            float py = y1 + dy * t;
            int ix = (int) (px - thickness * 0.5f);
            int iy = (int) (py - thickness * 0.5f);
            int ex = (int) (px + thickness * 0.5f);
            int ey = (int) (py + thickness * 0.5f);
            this.context.fill(RenderPipelines.GUI, ix, iy, ex, ey, color);
        }
    }

    public void fillRect(Object builder, Matrix4f matrix4f, float x, float y, float w, float h, int color1, int color2, int color3, int color4)
    {
        this.box(x, y, w, h, color1, color2, color3, color4);
    }

    public void dropShadow(int left, int top, int right, int bottom, int offset, int opaque, int shadow)
    {
        left -= offset;
        top -= offset;
        right += offset;
        bottom += offset;

        this.context.fill(RenderPipelines.GUI, left + offset, top + offset, right - offset, bottom - offset, opaque);
        this.context.fillGradient(left, top, right, top + offset, shadow, opaque);
        this.context.fillGradient(left, bottom - offset, right, bottom, opaque, shadow);
        this.context.fill(RenderPipelines.GUI, left, top + offset, left + offset, bottom - offset, shadow);
        this.context.fill(RenderPipelines.GUI, right - offset, top + offset, right, bottom - offset, shadow);
    }

    /* Gradients */

    public void gradientHBox(float x1, float y1, float x2, float y2, int leftColor, int rightColor)
    {
        this.box(x1, y1, x2 - x1, y2 - y1, leftColor, rightColor, leftColor, rightColor);
    }

    public void gradientVBox(float x1, float y1, float x2, float y2, int topColor, int bottomColor)
    {
        this.box(x1, y1, x2 - x1, y2 - y1, topColor, topColor, bottomColor, bottomColor);
    }

    public void dropCircleShadow(int x, int y, int radius, int segments, int opaque, int shadow)
    {
        int r = Math.max(1, radius);
        for (int i = r; i > 0; i -= Math.max(1, r / 8))
        {
            float t = (float) i / (float) r;
            int col = Colors.mulA(shadow, t);
            this.context.fill(RenderPipelines.GUI, x - i, y - i, x + i, y + i, col);
        }
        this.context.fill(RenderPipelines.GUI, x - 1, y - 1, x + 1, y + 1, opaque);
    }

    public void dropCircleShadow(int x, int y, int radius, int offset, int segments, int opaque, int shadow)
    {
        int inner = Math.max(0, radius - offset);
        this.dropCircleShadow(x, y, inner, segments, opaque, opaque);
        this.dropCircleShadow(x, y, radius, segments, opaque, shadow);
    }

    /* Outline methods */

    public void outlineCenter(float x, float y, float offset, int color)
    {
        this.outlineCenter(x, y, offset, color, 1);
    }

    public void outlineCenter(float x, float y, float offset, int color, int border)
    {
        this.outline(x - offset, y - offset, x + offset, y + offset, color, border);
    }

    public void outline(float x1, float y1, float x2, float y2, int color)
    {
        this.outline(x1, y1, x2, y2, color, 1);
    }

    /**
     * Draw rectangle outline with given border.
     */
    public void outline(float x1, float y1, float x2, float y2, int color, int border)
    {
        int ix1 = (int) x1, iy1 = (int) y1, ix2 = (int) x2, iy2 = (int) y2;
        this.context.fill(RenderPipelines.GUI, ix1, iy1, ix2, iy1 + border, color); // top
        this.context.fill(RenderPipelines.GUI, ix1, iy2 - border, ix2, iy2, color); // bottom
        this.context.fill(RenderPipelines.GUI, ix1, iy1 + border, ix1 + border, iy2 - border, color); // left
        this.context.fill(RenderPipelines.GUI, ix2 - border, iy1 + border, ix2, iy2 - border, color); // right
    }

    /* Icon */

    public void icon(Icon icon, float x, float y)
    {
        this.icon(icon, Colors.WHITE, x, y);
    }

    public void icon(Icon icon, int color, float x, float y)
    {
        this.icon(icon, color, x, y, 0F, 0F);
    }

    public void icon(Icon icon, float x, float y, float ax, float ay)
    {
        this.icon(icon, Colors.WHITE, x, y, ax, ay);
    }

    public void icon(Icon icon, int color, float x, float y, float ax, float ay)
    {
        if (icon.texture == null)
        {
            return;
        }

        int xx = (int) (x - icon.w * ax);
        int yy = (int) (y - icon.h * ay);

        Identifier id = getOrRegisterAtlas(icon.texture);
        if (id != null)
        {
            this.context.drawTexture(RenderPipelines.GUI_TEXTURED, id, xx, yy, (float) icon.x, (float) icon.y, icon.w, icon.h, icon.textureW, icon.textureH, color);
        }
    }

    public void iconArea(Icon icon, float x, float y, float w, float h)
    {
        this.iconArea(icon, Colors.WHITE, x, y, w, h);
    }

    public void iconArea(Icon icon, int color, float x, float y, float w, float h)
    {
        Identifier id = getOrRegisterAtlas(icon.texture);
        if (id != null)
        {
            this.context.drawTexture(RenderPipelines.GUI_TEXTURED, id, (int) x, (int) y, (float) icon.x, (float) icon.y, (int) w, (int) h, icon.textureW, icon.textureH, color);
        }
    }

    public void outlinedIcon(Icon icon, float x, float y, float ax, float ay)
    {
        this.outlinedIcon(icon, x, y, Colors.WHITE, ax, ay);
    }

    /**
     * Draw an icon with a black outline.
     */
    public void outlinedIcon(Icon icon, float x, float y, int color, float ax, float ay)
    {
        this.icon(icon, Colors.A100, x - 1, y, ax, ay);
        this.icon(icon, Colors.A100, x + 1, y, ax, ay);
        this.icon(icon, Colors.A100, x, y - 1, ax, ay);
        this.icon(icon, Colors.A100, x, y + 1, ax, ay);
        this.icon(icon, color, x, y, ax, ay);
    }

    /* Textured box */

    public void fullTexturedBox(Texture texture, float x, float y, float w, float h)
    {
        this.fullTexturedBox(texture, Colors.WHITE, x, y, w, h);
    }

    public void fullTexturedBox(Texture texture, int color, float x, float y, float w, float h)
    {
        this.texturedBox(texture, color, x, y, w, h, 0, 0, w, h, (int) w, (int) h);
    }

    public void texturedBox(Texture texture, int color, float x, float y, float w, float h, float u1, float v1, float u2, float v2)
    {
        this.texturedBox(texture, color, x, y, w, h, u1, v1, u2, v2, texture.width, texture.height);
    }

    public void texturedBox(Texture texture, int color, float x, float y, float w, float h, float u, float v)
    {
        this.texturedBox(texture, color, x, y, w, h, u, v, u + w, v + h, texture.width, texture.height);
    }

    public void texturedBox(Texture texture, int color, float x, float y, float w, float h, float u1, float v1, float u2, float v2, int textureW, int textureH)
    {
        Identifier id = getOrRegister(texture);
        if (id == null)
        {
            this.box(x, y, x + w, y + h, color);
            return;
        }
        this.context.drawTexture(RenderPipelines.GUI_TEXTURED, id, (int) x, (int) y, u1, v1, (int) w, (int) h, textureW, textureH, color);
    }

    public void texturedBox(int texture, int color, float x, float y, float w, float h, float u1, float v1, float u2, float v2, int textureW, int textureH)
    {
        this.box(x, y, x + w, y + h, color);
    }

    public void texturedBox(Object shader, int texture, int color, float x, float y, float w, float h, float u1, float v1, float u2, float v2, int textureW, int textureH)
    {
        this.box(x, y, x + w, y + h, color);
    }

    private void fillTexturedBoxPlaceholder() {}

    /* Repeatable textured box */

    public void texturedArea(Texture texture, int color, float x, float y, float w, float h, float u, float v, float tileW, float tileH, int tw, int th)
    {
        if (w <= 0 || h <= 0 || tileW <= 0 || tileH <= 0)
        {
            return;
        }
        Identifier id = getOrRegister(texture);
        if (id == null)
        {
            this.box(x, y, x + w, y + h, color);
            return;
        }
        int countX = Math.max(1, (int) Math.ceil(w / tileW));
        int countY = Math.max(1, (int) Math.ceil(h / tileH));
        float fillerX = w - (countX - 1) * tileW;
        float fillerY = h - (countY - 1) * tileH;
        for (int ix = 0; ix < countX; ix++)
        {
            for (int iy = 0; iy < countY; iy++)
            {
                float xx = x + ix * tileW;
                float yy = y + iy * tileH;
                float xw = ix == countX - 1 ? fillerX : tileW;
                float yh = iy == countY - 1 ? fillerY : tileH;
                this.context.drawTexture(RenderPipelines.GUI_TEXTURED, id, (int) xx, (int) yy, u, v, (int) xw, (int) yh, tw, th, color);
            }
        }
    }

    /* Text with default font */

    public void text(String label, float x, float y, int color)
    {
        this.text(label, x, y, color, false);
    }

    public void text(String label, float x, float y)
    {
        this.text(label, x, y, Colors.WHITE, false);
    }

    public void textShadow(String label, float x, float y)
    {
        this.textShadow(label, x, y, Colors.WHITE);
    }

    public void textShadow(String label, float x, float y, int color)
    {
        this.text(label, x, y, color, true);
    }

    public void text(String label, float x, float y, int color, boolean shadow)
    {
        this.context.drawText(this.font.getRenderer(), label, (int) x, (int) y, color, shadow);
    }

    /* Text helpers */

    public int wallText(String text, int x, int y, int color, int width)
    {
        return this.wallText(text, x, y, color, width, 12);
    }

    public int wallText(String text, int x, int y, int color, int width, int lineHeight)
    {
        return this.wallText(text, x, y, color, width, lineHeight, 0F, 0F);
    }

    public int wallText(String text, int x, int y, int color, int width, int lineHeight, float ax, float ay)
    {
        List<String> list = this.font.wrap(text, width);
        int h = (lineHeight * (list.size() - 1)) + this.font.getHeight();

        y -= h * ay;

        for (String string : list)
        {
            this.text(string.toString(), (int) (x + (width - this.font.getWidth(string)) * ax), y, color, true);

            y += lineHeight;
        }

        return h;
    }

    public void textCard(String text, float x, float y)
    {
        this.textCard(text, x, y, Colors.WHITE, Colors.A50);
    }

    /**
     * In this context, text card is a text with some background behind it
     */
    public void textCard(String text, float x, float y, int color, int background)
    {
        this.textCard(text, x, y, color, background, 3);
    }

    public void textCard(String text, float x, float y, int color, int background, float offset)
    {
        this.textCard(text, x, y, color, background, offset, true);
    }

    public void textCard(String text, float x, float y, int color, int background, float offset, boolean shadow)
    {
        int a = background >> 24 & 0xff;

        if (a != 0)
        {
            this.box(x - offset, y - offset, x + this.font.getWidth(text) + offset - 1, y + this.font.getHeight() + offset, background);
        }

        this.text(text, x, y, color, shadow);
    }

    public void flush()
    {
    }

    public void draw(Object builder, Object shader) {}

    /* Compatibility overload to accept method references like BBSShaders::getPickerPreviewProgram */
    public void texturedBox(Supplier<?> shader, int texture, int color, float x, float y, float w, float h, float u1, float v1, float u2, float v2, int textureW, int textureH)
    {
        this.texturedBox(texture, color, x, y, w, h, u1, v1, u2, v2, textureW, textureH);
    }

    /* Texture bridge: register GL texture into TextureManager as NativeImageBackedTexture */
    private Identifier getOrRegister(Texture texture)
    {
        if (texture == null || !texture.isValid())
        {
            return null;
        }

        Identifier id = NATIVE_IDS.get(texture.id);
        if (id != null)
        {
            return id;
        }

        try
        {
            Pixels px = Texture.pixelsFromTexture(texture);
            if (px == null) return null;

            int[] argb = px.getARGB();
            BufferedImage img = new BufferedImage(texture.width, texture.height, BufferedImage.TYPE_INT_ARGB);
            img.setRGB(0, 0, texture.width, texture.height, argb, 0, texture.width);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(baos.toByteArray()));
            NativeImageBackedTexture nativeTex = new NativeImageBackedTexture(() -> "bbs_dyn", nativeImage);

            id = Identifier.of("bbs_dyn", "tex_" + texture.id + "_" + texture.width + "x" + texture.height);
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, nativeTex);
            NATIVE_IDS.put(texture.id, id);
            return id;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    /* Atlas bridge: load PNG via BBS AssetProvider and register as NativeImageBackedTexture */
    private static final Map<String, Identifier> ATLAS_IDS = new HashMap<>();

    private Identifier getOrRegisterAtlas(Link link)
    {
        if (link == null)
        {
            return null;
        }

        Identifier cached = ATLAS_IDS.get(link.toString());
        if (cached != null)
        {
            return cached;
        }

        try (InputStream in0 = BBSMod.getProvider().getAsset(link))
        {
            InputStream in = in0;
            if (in == null && "assets".equals(link.source) && !link.path.startsWith("assets/"))
            {
                in = BBSMod.getProvider().getAsset(Link.assets("assets/" + link.path));
            }
            if (in == null)
            {
                return null;
            }
            NativeImage img = NativeImage.read(in);
            NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> "bbs_icons", img);
            String safe = link.path.replace('/', '_').replace('\\', '_');
            Identifier id = Identifier.of("bbs_dyn", "atlas_" + safe);
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, tex);
            ATLAS_IDS.put(link.toString(), id);
            return id;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }
}
