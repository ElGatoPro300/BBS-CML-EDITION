package mchorse.bbs_mod.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TextureFont
{
    private NativeImageBackedTexture texture;
    private Identifier textureId;
    private final Map<Character, Glyph> glyphs = new HashMap<>();
    private int height;
    private boolean initialized = false;

    public TextureFont(File fontFile)
    {
        try
        {
            Font font = Font.createFont(Font.TRUETYPE_FONT, fontFile).deriveFont(64f); /* High res for quality */
            generateTexture(font);
            this.initialized = true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public boolean isInitialized()
    {
        return initialized;
    }

    private void generateTexture(Font font) throws IOException
    {
        int imgSize = 2048; /* Increase size for more chars/quality */
        BufferedImage image = new BufferedImage(imgSize, imgSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        
        g2d.setFont(font);
        g2d.setColor(Color.WHITE);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        
        FontMetrics metrics = g2d.getFontMetrics();
        this.height = metrics.getHeight();
        
        int x = 0;
        int y = metrics.getAscent();
        
        /* Support Latin-1 Supplement */
        for (int i = 32; i < 256; i++)
        {
            char c = (char) i;
            if (!font.canDisplay(c)) continue;

            int w = metrics.charWidth(c);
            
            if (x + w >= imgSize)
            {
                x = 0;
                y += metrics.getHeight();
            }
            
            g2d.drawString(String.valueOf(c), x, y);
            
            /* Store glyph info */
            this.glyphs.put(c, new Glyph(x, y - metrics.getAscent(), w, metrics.getHeight(), imgSize));
            
            x += w + 4;
        }
        
        g2d.dispose();
        
        /* Upload to texture */
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(baos.toByteArray()));
        
        RenderSystem.recordRenderCall(() -> {
            this.texture = new NativeImageBackedTexture(nativeImage);
            this.textureId = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture("bbs_font_" + font.hashCode(), this.texture);
        });
    }

    public int getWidth(String text)
    {
        int w = 0;
        float scale = 0.25f;
        for (int i = 0; i < text.length(); i++)
        {
            char c = text.charAt(i);
            
            if (c == '\u00A7' && i + 1 < text.length())
            {
                i++;
                continue;
            }

            Glyph g = glyphs.get(c);
            if (g != null) w += g.width * scale;
        }
        return w;
    }

    public java.util.List<String> wrap(String text, int width)
    {
        java.util.List<String> lines = new java.util.ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words)
        {
            String testLine = currentLine.length() > 0 ? currentLine + " " + word : word;
            if (this.getWidth(testLine) <= width)
            {
                if (currentLine.length() > 0) currentLine.append(" ");
                currentLine.append(word);
            }
            else
            {
                if (currentLine.length() > 0)
                {
                    lines.add(currentLine.toString());
                }
                currentLine = new StringBuilder(word);
            }
        }
        
        if (currentLine.length() > 0)
        {
            lines.add(currentLine.toString());
        }
        
        return lines;
    }

    public int getHeight()
    {
        return (int) (this.height * 0.25f);
    }

    public void draw(String text, float x, float y, int color, Matrix4f matrix, VertexConsumerProvider consumers, int light)
    {
        if (this.textureId == null) return;

        VertexConsumer consumer = consumers.getBuffer(RenderLayer.getTextSeeThrough(this.textureId));
        float scale = 0.25f; /* Scale down because we generated at 64px */
        
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        float a = (color >> 24 & 255) / 255.0F;

        float cx = x;
        
        /* Simple color code parser */
        for (int i = 0; i < text.length(); i++)
        {
            char c = text.charAt(i);
            
            if (c == '\u00A7' && i + 1 < text.length())
            {
                /* 
                 * Handle color codes (simplified)
                 * Just skip the color code for now
                 */
                i++;
                continue;
            }

            Glyph glyph = glyphs.get(c);
            if (glyph == null) continue;
            
            float gw = glyph.width * scale;
            float gh = glyph.height * scale;
            
            /* Draw quad */
            drawVertex(consumer, matrix, cx, y + gh, 0, glyph.u, glyph.v + glyph.vh, r, g, b, a, light);
            drawVertex(consumer, matrix, cx + gw, y + gh, 0, glyph.u + glyph.uw, glyph.v + glyph.vh, r, g, b, a, light);
            drawVertex(consumer, matrix, cx + gw, y, 0, glyph.u + glyph.uw, glyph.v, r, g, b, a, light);
            drawVertex(consumer, matrix, cx, y, 0, glyph.u, glyph.v, r, g, b, a, light);
            
            cx += gw;
        }
    }

    private void drawVertex(VertexConsumer consumer, Matrix4f matrix, float x, float y, float z, float u, float v, float r, float g, float b, float a, int light)
    {
        consumer.vertex(matrix, x, y, z).color(r, g, b, a).texture(u, v).light(light).next();
    }

    private static class Glyph
    {
        float u, v, uw, vh;
        int width, height;

        public Glyph(int x, int y, int w, int h, int imgSize)
        {
            this.width = w;
            this.height = h;
            this.u = (float) x / imgSize;
            this.v = (float) y / imgSize;
            this.uw = (float) w / imgSize;
            this.vh = (float) h / imgSize;
        }
    }
}
