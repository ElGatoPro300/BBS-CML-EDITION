package mchorse.bbs_mod.ui.framework.elements.utils;

import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class Batcher2D
{
    private static final Matrix4f GUI_MATRIX = new Matrix4f();
    private static final Map<String, Identifier> ATLAS_IDS = new HashMap<>();
    private static final Set<String> ATLAS_FAILED = new HashSet<>();
    private static final Map<Integer, Identifier> RAW_TEXTURE_IDS = new HashMap<>();
    private static boolean DRAW_TEXTURE_SIGNATURES_LOGGED = false;
    private static boolean CONTEXT_SIGNATURES_LOGGED = false;
    private static FontRenderer fontRenderer = new FontRenderer();

    private GuiGraphicsExtractor context;
    private FontRenderer font;

    public static FontRenderer getDefaultTextRenderer()
    {
        fontRenderer.setRenderer(Minecraft.getInstance().font);

        return fontRenderer;
    }

    public Batcher2D(GuiGraphicsExtractor context)
    {
        this.context = context;
        this.font = getDefaultTextRenderer();

        this.logContextTextureMethodsOnce();
    }

    public GuiGraphicsExtractor getContext()
    {
        return this.context;
    }

    public void setContext(GuiGraphicsExtractor context)
    {
        this.context = context;
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
        this.box(x1, y1, x2 - x1, y2 - y1, color, color, color, color);
    }

    public void box(float x, float y, float w, float h, int color1, int color2, int color3, int color4)
    {
        int left = Math.round(Math.min(x, x + w));
        int top = Math.round(Math.min(y, y + h));
        int right = Math.round(Math.max(x, x + w));
        int bottom = Math.round(Math.max(y, y + h));

        if (left == right || top == bottom)
        {
            return;
        }

        if (color1 == color2 && color2 == color3 && color3 == color4)
        {
            this.context.fill(RenderPipelines.GUI, left, top, right, bottom, color1);
            return;
        }

        this.context.fillGradient(left, top, right, bottom, color1, color3);
    }

    /**
     * Draw an anti-aliased-looking line segment by rendering a thin quad between two points.
     * The line is axis-independent (supports arbitrary angle) with given thickness in pixels.
     */
    public void line(float x1, float y1, float x2, float y2, float thickness, int color)
    {
        float half = Math.max(1F, thickness) / 2F;
        float left = Math.min(x1, x2) - half;
        float top = Math.min(y1, y2) - half;
        float right = Math.max(x1, x2) + half;
        float bottom = Math.max(y1, y2) + half;

        this.box(left, top, right, bottom, color);
    }

    public void fillRect(BufferBuilder builder, Matrix4f matrix4f, float x, float y, float w, float h, int color1, int color2, int color3, int color4)
    {
        /* c1 ---- c2
         * |        |
         * c3 ---- c4 */
        builder.addVertex(matrix4f, x, y, 0).setColor(color1);
        builder.addVertex(matrix4f, x, y + h, 0).setColor(color3);
        builder.addVertex(matrix4f, x + w, y + h, 0).setColor(color4);
        builder.addVertex(matrix4f, x + w, y, 0).setColor(color2);
    }

    public void dropShadow(int left, int top, int right, int bottom, int offset, int opaque, int shadow)
    {
        if (offset <= 0)
        {
            this.box(left, top, right, bottom, opaque);
            return;
        }

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
        if (radius <= 0 || segments <= 2)
        {
            return;
        }

        for (int iy = -radius; iy <= radius; iy++)
        {
            int width = (int) Math.sqrt(radius * radius - iy * iy);
            float t = Math.abs(iy) / (float) radius;
            int color = this.mixColor(opaque, shadow, t);
            this.context.fill(RenderPipelines.GUI, x - width, y + iy, x + width + 1, y + iy + 1, color);
        }
    }

    public void dropCircleShadow(int x, int y, int radius, int offset, int segments, int opaque, int shadow)
    {
        if (offset >= radius)
        {
            this.dropCircleShadow(x, y, radius, segments, opaque, shadow);

            return;
        }

        if (offset <= 0)
        {
            this.dropCircleShadow(x, y, radius, segments, opaque, shadow);
            return;
        }

        for (int iy = -radius; iy <= radius; iy++)
        {
            int outerWidth = (int) Math.sqrt(radius * radius - iy * iy);
            int clamped = Math.min(offset * offset, iy * iy);
            int innerWidth = (int) Math.sqrt(offset * offset - clamped);

            if (innerWidth > 0)
            {
                this.context.fill(RenderPipelines.GUI, x - innerWidth, y + iy, x + innerWidth + 1, y + iy + 1, opaque);
            }

            int leftOuter = x - outerWidth;
            int leftInner = x - innerWidth;
            int rightInner = x + innerWidth + 1;
            int rightOuter = x + outerWidth + 1;
            float t = (outerWidth - innerWidth) / (float) Math.max(1, radius - offset);
            int blend = this.mixColor(opaque, shadow, Math.max(0F, Math.min(1F, t)));

            if (leftOuter < leftInner)
            {
                this.context.fill(RenderPipelines.GUI, leftOuter, y + iy, leftInner, y + iy + 1, blend);
            }

            if (rightInner < rightOuter)
            {
                this.context.fill(RenderPipelines.GUI, rightInner, y + iy, rightOuter, y + iy + 1, blend);
            }
        }
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
        this.box(x1, y1, x1 + border, y2, color);
        this.box(x2 - border, y1, x2, y2, color);
        this.box(x1 + border, y1, x2 - border, y1 + border, color);
        this.box(x1 + border, y2 - border, x2 - border, y2, color);
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

        x -= icon.w * ax;
        y -= icon.h * ay;

        this.texturedBox(BBSModClient.getTextures().getTexture(icon.texture), color, x, y, icon.w, icon.h, icon.x, icon.y, icon.x + icon.w, icon.y + icon.h, icon.textureW, icon.textureH);
    }

    public void iconArea(Icon icon, float x, float y, float w, float h)
    {
        this.iconArea(icon, Colors.WHITE, x, y, w, h);
    }

    public void iconArea(Icon icon, int color, float x, float y, float w, float h)
    {
        this.texturedArea(BBSModClient.getTextures().getTexture(icon.texture), color, x, y, w, h, icon.x, icon.y, icon.w, icon.h, icon.textureW, icon.textureH);
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
        Link link = this.findLinkForTexture(texture);
        if (link != null)
        {
            Identifier id = this.getOrCreateAtlasId(link);
            if (id != null && this.drawTextureIdentifier(id, color, x, y, w, h, u1, v1, u2, v2, textureW, textureH))
            {
                return;
            }
        }

        this.texturedBox(texture.id, color, x, y, w, h, u1, v1, u2, v2, textureW, textureH);
    }

    public void texturedBox(int texture, int color, float x, float y, float w, float h, float u1, float v1, float u2, float v2, int textureW, int textureH)
    {
        this.texturedBox((Supplier<GlProgram>) null, texture, color, x, y, w, h, u1, v1, u2, v2, textureW, textureH);
    }

    public void texturedBox(Supplier<GlProgram> shader, int texture, int color, float x, float y, float w, float h, float u1, float v1, float u2, float v2, int textureW, int textureH)
    {
        if (shader != null)
        {
            shader.get();
        }

        this.drawTexturedBox(texture, color, x, y, w, h, u1, v1, u2, v2, textureW, textureH);
    }

    public void texturedBox(GlProgram shader, int texture, int color, float x, float y, float w, float h, float u1, float v1, float u2, float v2, int textureW, int textureH)
    {
        this.drawTexturedBox(texture, color, x, y, w, h, u1, v1, u2, v2, textureW, textureH);
    }

    private void fillTexturedBox(BufferBuilder builder, Matrix4f matrix, int color, float x, float y, float w, float h, float u1, float v1, float u2, float v2, int textureW, int textureH)
    {
        builder.addVertex(matrix, x, y + h, 0F).setUv(u1 / (float) textureW, v2 / (float) textureH).setColor(color);
        builder.addVertex(matrix, x + w, y + h, 0F).setUv(u2 / (float) textureW, v2 / (float) textureH).setColor(color);
        builder.addVertex(matrix, x + w, y, 0F).setUv(u2 / (float) textureW, v1 / (float) textureH).setColor(color);
        builder.addVertex(matrix, x, y + h, 0F).setUv(u1 / (float) textureW, v2 / (float) textureH).setColor(color);
        builder.addVertex(matrix, x + w, y, 0F).setUv(u2 / (float) textureW, v1 / (float) textureH).setColor(color);
        builder.addVertex(matrix, x, y, 0F).setUv(u1 / (float) textureW, v1 / (float) textureH).setColor(color);
    }

    private void drawTexturedBox(int texture, int color, float x, float y, float w, float h, float u1, float v1, float u2, float v2, int textureW, int textureH)
    {
        if (w == 0 || h == 0 || textureW <= 0 || textureH <= 0)
        {
            return;
        }

        if (this.drawTextureRawByContext(texture, color, x, y, w, h, u1, v1, u2, v2, textureW, textureH))
        {
            return;
        }

        GlStateManager._activeTexture(GL30.GL_TEXTURE0);
        GlStateManager._bindTexture(texture);

        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);
        this.fillTexturedBox(builder, this.resolveMatrix(), color, x, y, w, h, u1, v1, u2, v2, textureW, textureH);
        RenderTypes.cutoutMovingBlock().draw(builder.buildOrThrow());
    }

    private boolean drawTextureRawByContext(int texture, int color, float x, float y, float w, float h, float u1, float v1, float u2, float v2, int textureW, int textureH)
    {
        Identifier rawId = this.getOrCreateRawTextureId(texture);

        if (rawId != null && this.drawTextureIdentifier(rawId, color, x, y, w, h, u1, v1, u2, v2, textureW, textureH))
        {
            return true;
        }

        return false;
    }

    /* Repeatable textured box */

    public void texturedArea(Texture texture, int color, float x, float y, float w, float h, float u, float v, float tileW, float tileH, int tw, int th)
    {
        if (w <= 0 || h <= 0 || tileW <= 0 || tileH <= 0)
        {
            return;
        }

        int countX = (int) Math.ceil(w / tileW);
        int countY = (int) Math.ceil(h / tileH);
        
        if (countX <= 0) countX = 1;
        if (countY <= 0) countY = 1;
        
        long c = (long) countX * countY;
        
        if (c <= 0 || c > 10000) // Safety limit to prevent freeze/crash
        {
             return;
        }

        float fillerX = w - (countX - 1) * tileW;
        float fillerY = h - (countY - 1) * tileH;

        Matrix4f matrix = this.resolveMatrix();
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX_COLOR);
        GlStateManager._activeTexture(GL30.GL_TEXTURE0);
        GlStateManager._bindTexture(texture.id);

        for (int i = 0; i < c; i ++)
        {
            float ix = i % countX;
            float iy = i / countX;
            float xx = x + ix * tileW;
            float yy = y + iy * tileH;
            float xw = ix == countX - 1 ? fillerX : tileW;
            float yh = iy == countY - 1 ? fillerY : tileH;

            this.fillTexturedBox(builder, matrix, color, xx, yy, xw, yh, u, v, u + xw, v + yh, tw, th);
        }

        RenderTypes.cutoutMovingBlock().draw(builder.buildOrThrow());
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
        this.text(label, x, y, Colors.WHITE, true);
    }

    public void textShadow(String label, float x, float y, int color)
    {
        this.text(label, x, y, color, true);
    }

    public void text(String label, float x, float y, int color, boolean shadow)
    {
        this.context.text(this.font.getRenderer(), label, (int) x, (int) y, color, shadow);
    }

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

    private Matrix4f resolveMatrix()
    {
        try
        {
            Object poseStack = this.context.getClass().getMethod("pose").invoke(this.context);
            Method last = poseStack.getClass().getMethod("last");
            Object pose = last.invoke(poseStack);

            try
            {
                Method poseMethod = pose.getClass().getMethod("pose");
                Object matrix = poseMethod.invoke(pose);

                if (matrix instanceof Matrix4f matrix4f)
                {
                    return matrix4f;
                }
            }
            catch (Exception ignored)
            {
            }

            try
            {
                Method getPositionMatrix = pose.getClass().getMethod("getPositionMatrix");
                Object matrix = getPositionMatrix.invoke(pose);

                if (matrix instanceof Matrix4f matrix4f)
                {
                    return matrix4f;
                }
            }
            catch (Exception ignored)
            {
            }
        }
        catch (Exception ignored)
        {
        }

        try
        {
            Object matrices = this.context.getClass().getMethod("getMatrices").invoke(this.context);
            Method peek = matrices.getClass().getMethod("peek");
            Object entry = peek.invoke(matrices);
            Method getPositionMatrix = entry.getClass().getMethod("getPositionMatrix");
            Object matrix = getPositionMatrix.invoke(entry);

            if (matrix instanceof Matrix4f matrix4f)
            {
                return matrix4f;
            }
        }
        catch (Exception ignored)
        {
        }

        return GUI_MATRIX;
    }

    private Link findLinkForTexture(Texture texture)
    {
        for (Map.Entry<Link, Texture> entry : BBSModClient.getTextures().textures.entrySet())
        {
            if (entry.getValue() == texture)
            {
                return entry.getKey();
            }
        }

        return null;
    }

    private Identifier getOrCreateAtlasId(Link link)
    {
        String key = link.toString();
        Identifier cached = ATLAS_IDS.get(key);

        if (cached != null)
        {
            return cached;
        }

        if (ATLAS_FAILED.contains(key))
        {
            return null;
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
            DynamicTexture tex = new DynamicTexture(() -> "bbs_icons", img);
            String safe = sanitizeIdentifierPath(link.path);
            Identifier id = Identifier.fromNamespaceAndPath("bbs_dyn", "atlas_" + safe);

            Minecraft.getInstance().getTextureManager().register(id, tex);
            ATLAS_IDS.put(key, id);

            return id;
        }
        catch (Exception e)
        {
            ATLAS_FAILED.add(key);
            return null;
        }
    }

    private String sanitizeIdentifierPath(String path)
    {
        StringBuilder builder = new StringBuilder(path.length() + 16);
        String lower = path.toLowerCase(Locale.ROOT);

        for (int i = 0; i < lower.length(); i++)
        {
            char c = lower.charAt(i);
            boolean valid = (c >= 'a' && c <= 'z') ||
                (c >= '0' && c <= '9') ||
                c == '/' || c == '.' || c == '_' || c == '-';

            builder.append(valid ? c : '_');
        }

        if (builder.length() == 0)
        {
            builder.append("texture");
        }

        builder.append('_').append(Integer.toHexString(path.hashCode()));
        return builder.toString();
    }

    private boolean drawTextureIdentifier(Identifier id, int color, float x, float y, float w, float h, float u1, float v1, float u2, float v2, int textureW, int textureH)
    {
        int left = Math.round(x);
        int top = Math.round(y);
        int width = Math.round(w);
        int height = Math.round(h);
        int u = Math.round(u1);
        int v = Math.round(v1);
        int regionW = Math.round(u2 - u1);
        int regionH = Math.round(v2 - v1);

        if (regionW == 0) regionW = u2 >= u1 ? 1 : -1;
        if (regionH == 0) regionH = v2 >= v1 ? 1 : -1;

        if (width == 0 || height == 0)
        {
            return true;
        }

        try
        {
            this.invokeBest(this.context, "blit", RenderPipelines.GUI_TEXTURED, id, left, top, (float) u, (float) v, width, height, regionW, regionH, textureW, textureH);
            return true;
        }
        catch (Exception ignored)
        {
        }

        try
        {
            this.invokeBest(this.context, "blit", RenderPipelines.GUI_TEXTURED, id, left, top, (float) u, (float) v, width, height, textureW, textureH);
            return true;
        }
        catch (Exception ignored)
        {
        }

        try
        {
            this.invokeBest(this.context, "blit", RenderPipelines.GUI_TEXTURED, id, left, top, (float) u, (float) v, width, height, regionW, regionH, textureW);
            return true;
        }
        catch (Exception ignored)
        {
        }

        try
        {
            float nu1 = u1 / (float) textureW;
            float nv1 = v1 / (float) textureH;
            float nu2 = u2 / (float) textureW;
            float nv2 = v2 / (float) textureH;

            this.invokeBest(this.context, "blit", id, left, top, width, height, nu1, nv1, nu2, nv2);
            return true;
        }
        catch (Exception ignored)
        {
        }

        try
        {
            this.invokeBest(this.context, "blit", RenderPipelines.GUI_TEXTURED, id, left, top, (float) u, (float) v, width, height, regionW, regionH);
            return true;
        }
        catch (Exception ignored)
        {
        }

        try
        {
            this.invokeBest(this.context, "blit", RenderPipelines.GUI_TEXTURED, id, left, top, (float) u, (float) v, width, height, regionW, regionH, textureW, textureH, color);
            return true;
        }
        catch (Exception ignored)
        {
        }

        try
        {
            this.invokeBest(this.context, "blit", RenderPipelines.GUI_TEXTURED, id, left, top, (float) u, (float) v, width, height, textureW, textureH, color);
            return true;
        }
        catch (Exception ignored)
        {
            this.logDrawTextureSignaturesOnce();
            return false;
        }
    }

    private void logDrawTextureSignaturesOnce()
    {
        if (DRAW_TEXTURE_SIGNATURES_LOGGED)
        {
            return;
        }

        DRAW_TEXTURE_SIGNATURES_LOGGED = true;

        try
        {
            System.out.println("[BBS][Batcher2D] drawTexture overload resolution failed for " + this.context.getClass().getName());

            for (Method method : this.context.getClass().getMethods())
            {
                if (!method.getName().equals("drawTexture"))
                {
                    continue;
                }

                StringBuilder line = new StringBuilder("[BBS][Batcher2D] drawTexture(");
                Class<?>[] params = method.getParameterTypes();

                for (int i = 0; i < params.length; i++)
                {
                    if (i > 0)
                    {
                        line.append(", ");
                    }

                    line.append(params[i].getSimpleName());
                }

                line.append(")");
                System.out.println(line);
            }
        }
        catch (Exception ignored)
        {
        }
    }

    private void logContextTextureMethodsOnce()
    {
        if (CONTEXT_SIGNATURES_LOGGED)
        {
            return;
        }

        CONTEXT_SIGNATURES_LOGGED = true;

        try
        {
            Class<?> clazz = this.context.getClass();

            System.out.println("[BBS][Batcher2D] GUI context class: " + clazz.getName());

            for (Method method : clazz.getMethods())
            {
                String name = method.getName();

                if (!(name.contains("Texture") || name.contains("texture") || name.contains("blit") || name.contains("Blit") || name.equals("pose")))
                {
                    continue;
                }

                StringBuilder line = new StringBuilder("[BBS][Batcher2D] ").append(name).append('(');
                Class<?>[] params = method.getParameterTypes();

                for (int i = 0; i < params.length; i++)
                {
                    if (i > 0)
                    {
                        line.append(", ");
                    }

                    line.append(params[i].getSimpleName());
                }

                line.append(')');
                System.out.println(line);
            }
        }
        catch (Exception ignored)
        {
        }
    }

    private Identifier getOrCreateRawTextureId(int texture)
    {
        Identifier cached = RAW_TEXTURE_IDS.get(texture);

        if (cached != null)
        {
            return cached;
        }

        try
        {
            Identifier id = Identifier.fromNamespaceAndPath("bbs_dyn", "raw_" + Integer.toHexString(texture));
            Minecraft.getInstance().getTextureManager().register(id, new RawGlTexture(texture));
            RAW_TEXTURE_IDS.put(texture, id);
            return id;
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private Object invokeBest(Object target, String methodName, Object... args) throws Exception
    {
        Method[] methods = target.getClass().getMethods();
        Method bestMethod = null;
        Object[] bestArgs = null;
        int bestScore = Integer.MIN_VALUE;

        for (Method method : methods)
        {
            if (!method.getName().equals(methodName))
            {
                continue;
            }

            Class<?>[] params = method.getParameterTypes();
            if (params.length != args.length)
            {
                continue;
            }

            boolean ok = true;
            Object[] invokeArgs = new Object[args.length];
            int score = 0;

            for (int i = 0; i < params.length; i++)
            {
                if (args[i] == null)
                {
                    invokeArgs[i] = null;
                    continue;
                }

                CoerceResult coerced = this.coerceArgument(params[i], args[i]);

                if (coerced == INVALID_COERCE)
                {
                    ok = false;
                    break;
                }

                invokeArgs[i] = coerced.value;
                score += coerced.score;
            }

            if (ok && score > bestScore)
            {
                bestScore = score;
                bestMethod = method;
                bestArgs = invokeArgs;
            }
        }

        if (bestMethod != null)
        {
            return bestMethod.invoke(target, bestArgs);
        }

        throw new NoSuchMethodException(methodName);
    }

    private static final CoerceResult INVALID_COERCE = new CoerceResult(null, Integer.MIN_VALUE);

    private static class CoerceResult
    {
        public final Object value;
        public final int score;

        public CoerceResult(Object value, int score)
        {
            this.value = value;
            this.score = score;
        }
    }

    private CoerceResult coerceArgument(Class<?> paramClass, Object arg)
    {
        Class<?> argClass = arg.getClass();

        if (paramClass == argClass)
        {
            return new CoerceResult(arg, 100);
        }

        if (paramClass.isAssignableFrom(argClass))
        {
            return new CoerceResult(arg, 80);
        }

        if (arg instanceof Number number)
        {
            if (paramClass == int.class || paramClass == Integer.class) return new CoerceResult(number.intValue(), 60);
            if (paramClass == float.class || paramClass == Float.class) return new CoerceResult(number.floatValue(), 60);
            if (paramClass == double.class || paramClass == Double.class) return new CoerceResult(number.doubleValue(), 60);
            if (paramClass == long.class || paramClass == Long.class) return new CoerceResult(number.longValue(), 60);
            if (paramClass == short.class || paramClass == Short.class) return new CoerceResult(number.shortValue(), 60);
            if (paramClass == byte.class || paramClass == Byte.class) return new CoerceResult(number.byteValue(), 60);
        }

        if ((paramClass == boolean.class || paramClass == Boolean.class) && arg instanceof Boolean)
        {
            return new CoerceResult(arg, 70);
        }

        if ((paramClass == char.class || paramClass == Character.class) && arg instanceof Character)
        {
            return new CoerceResult(arg, 70);
        }

        return INVALID_COERCE;
    }

    private static class RawGlTexture extends AbstractTexture
    {
        private final int glId;

        public RawGlTexture(int glId)
        {
            this.glId = glId;
        }

        public void load(ResourceManager manager)
        {
        }

        public int getGlId()
        {
            return this.glId;
        }
    }

    private int mixColor(int a, int b, float t)
    {
        float clamped = Math.max(0F, Math.min(1F, t));
        int aa = (a >> 24) & 0xff;
        int ar = (a >> 16) & 0xff;
        int ag = (a >> 8) & 0xff;
        int ab = a & 0xff;
        int ba = (b >> 24) & 0xff;
        int br = (b >> 16) & 0xff;
        int bg = (b >> 8) & 0xff;
        int bb = b & 0xff;

        int ra = (int) (aa + (ba - aa) * clamped);
        int rr = (int) (ar + (br - ar) * clamped);
        int rg = (int) (ag + (bg - ag) * clamped);
        int rb = (int) (ab + (bb - ab) * clamped);

        return (ra << 24) | (rr << 16) | (rg << 8) | rb;
    }
}
