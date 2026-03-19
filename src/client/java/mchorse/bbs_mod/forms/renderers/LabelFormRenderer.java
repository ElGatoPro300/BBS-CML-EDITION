package mchorse.bbs_mod.forms.renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.LabelForm;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.joml.Vectors;
import mchorse.bbs_mod.utils.FontUtils;
import mchorse.bbs_mod.utils.TextureFont;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

public class LabelFormRenderer extends FormRenderer<LabelForm>
{
    private float nametagAlpha = 1F;
    public static void fillQuad(BufferBuilder builder, MatrixStack stack, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float b, float a)
    {
        Matrix4f matrix4f = stack.peek().getPositionMatrix();

        /* 1 - BR, 2 - BL, 3 - TL, 4 - TR */
        builder.vertex(matrix4f, x1, y1, z1).color(r, g, b, a);
        builder.vertex(matrix4f, x2, y2, z2).color(r, g, b, a);
        builder.vertex(matrix4f, x3, y3, z3).color(r, g, b, a);
        builder.vertex(matrix4f, x1, y1, z1).color(r, g, b, a);
        builder.vertex(matrix4f, x3, y3, z3).color(r, g, b, a);
        builder.vertex(matrix4f, x4, y4, z4).color(r, g, b, a);
    }

    public LabelFormRenderer(LabelForm form)
    {
        super(form);
    }

    @Override
    public void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        int color = this.form.color.get().getARGBColor();
        String text = StringUtils.processColoredText(this.form.text.get());
        List<String> wrap = context.batcher.getFont().wrap(text, x2 - x1 - 4);

        int th = context.batcher.getFont().getHeight();
        int lineHeight = th + 4;
        int h = th + (wrap.size() - 1) * lineHeight;
        int y = (y2 + y1) / 2 - h / 2;

        for (String s : wrap)
        {
            context.batcher.textShadow(s, x1 + 2, y, color);

            y += lineHeight;
        }
    }

    @Override
    public void render3D(FormRenderingContext context)
    {
        context.stack.push();

        if (this.form.billboard.get())
        {
            Matrix4f modelMatrix = context.stack.peek().getPositionMatrix();
            Vector3f scale = new Vector3f();

            modelMatrix.getScale(scale);

            modelMatrix.m00(1).m01(0).m02(0);
            modelMatrix.m10(0).m11(1).m12(0);
            modelMatrix.m20(0).m21(0).m22(1);

            if (!context.modelRenderer && !context.isPicking())
            {
                modelMatrix.mul(context.camera.view);
            }

            modelMatrix.scale(scale);

            context.stack.peek().getNormalMatrix().identity();
            context.stack.peek().getNormalMatrix().scale(1F / scale.x, 1F / scale.y, 1F / scale.z);
        }

        TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        float fontSize = this.form.fontSize.get();
        float scale = (1F / 16F) * (fontSize <= 0 ? 1F : fontSize);
        int light = context.light;

        this.nametagAlpha = 1F;

        if (this.form.nametag.get() && context.entity != null && context.entity.isSneaking())
        {
            context.stack.translate(0F, -0.5F, 0F);
            this.nametagAlpha = 0.125F;
        }

        MatrixStackUtils.scaleStack(context.stack, scale, -scale, scale);

        RenderSystem.disableCull();

        if (context.isPicking())
        {
            CustomVertexConsumerProvider.hijackVertexFormat((layer) ->
            {
                this.setupTarget(context, BBSShaders.getPickerModelsProgram());
                RenderSystem.setShader(BBSShaders.getPickerModelsProgram());
            });

            light = 0;
        }

        if (this.form.max.get() <= 10)
        {
            this.renderString(context, consumers, renderer, light);
        }
        else
        {
            this.renderLimitedString(context, consumers, renderer, light);
        }

        CustomVertexConsumerProvider.clearRunnables();

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();

        context.stack.pop();
    }

    private String applyStyles(String content)
    {
        StringBuilder prefix = new StringBuilder();
        if (this.form.fontWeight.get() >= 700) prefix.append("\u00A7l");
        if (this.form.fontStyle.get() >= 1) prefix.append("\u00A7o");
        if (this.form.underline.get()) prefix.append("\u00A7n");
        if (this.form.strikethrough.get()) prefix.append("\u00A7m");
        
        return prefix.toString() + content;
    }

    private void renderTextShadow(FormRenderingContext context, CustomVertexConsumerProvider consumers, TextRenderer renderer, TextureFont customFont, String content, float x, float y, float letterSpacing, int light, Color shadowColor)
    {
        if (shadowColor.a <= 0)
        {
            return;
        }

        context.stack.push();
        context.stack.translate(0F, 0F, -0.05F);

        float sx = this.form.shadowX.get();
        float sy = this.form.shadowY.get();
        float blur = this.form.shadowBlur.get();

        if (blur > 0)
        {
            int originalColor = shadowColor.getARGBColor();
            int alpha = (originalColor >> 24) & 0xFF;
            int rgb = originalColor & 0x00FFFFFF;
            int blurAlpha = Math.max(1, alpha / 4);
            int blurColor = (blurAlpha << 24) | rgb;

            this.drawSimpleText(context, consumers, renderer, customFont, content, x + sx - blur, y + sy, letterSpacing, light, blurColor);
            this.drawSimpleText(context, consumers, renderer, customFont, content, x + sx + blur, y + sy, letterSpacing, light, blurColor);
            this.drawSimpleText(context, consumers, renderer, customFont, content, x + sx, y + sy - blur, letterSpacing, light, blurColor);
            this.drawSimpleText(context, consumers, renderer, customFont, content, x + sx, y + sy + blur, letterSpacing, light, blurColor);
        }
        else
        {
            this.drawSimpleText(context, consumers, renderer, customFont, content, x + sx, y + sy, letterSpacing, light, shadowColor.getARGBColor());
        }

        context.stack.pop();
    }

    private void drawSimpleText(FormRenderingContext context, CustomVertexConsumerProvider consumers, TextRenderer renderer, TextureFont customFont, String content, float x, float y, float letterSpacing, int light, int color)
    {
        if (customFont != null)
        {
            customFont.draw(content, x, y, color, color, letterSpacing, 0F, context.stack.peek().getPositionMatrix(), consumers, light);
        }
        else
        {
            renderer.draw(
                content,
                x,
                y,
                color, false,
                context.stack.peek().getPositionMatrix(),
                consumers,
                TextRenderer.TextLayerType.NORMAL,
                0,
                light
            );
        }
    }

    private void renderString(FormRenderingContext context, CustomVertexConsumerProvider consumers, TextRenderer renderer, int light)
    {
        String content = applyStyles(StringUtils.processColoredText(this.form.text.get()));
        String fontName = this.form.font.get();
        TextureFont customFont = null;
        
        if (!fontName.isEmpty())
        {
            int style = java.awt.Font.PLAIN;
            if (this.form.fontWeight.get() >= 700) style |= java.awt.Font.BOLD;
            if (this.form.fontStyle.get() >= 1) style |= java.awt.Font.ITALIC;
            
            customFont = FontUtils.getFont(fontName, style);
        }

        float transition = context.getTransition();
        float letterSpacing = this.form.letterSpacing.get();
        int w = customFont != null ? customFont.getWidth(content, letterSpacing) : renderer.getWidth(content) - 1;
        int h = customFont != null ? customFont.getHeight() : renderer.fontHeight - 2;
        int x = (int) (-w * this.form.anchorX.get());
        int y = (int) (-h * this.form.anchorY.get());

        Color shadowColor = this.form.shadowColor.get().copy();
        Color color = this.form.color.get().copy();

        shadowColor.a *= this.nametagAlpha;
        color.a *= this.nametagAlpha;
        
        float opacity = this.form.opacity.get();
        color.a *= opacity;
        shadowColor.a *= opacity;

        color.mul(context.color);
        shadowColor.mul(context.color);

        this.renderTextShadow(context, consumers, renderer, customFont, content, x, y, letterSpacing, light, shadowColor);

        if (this.form.outline.get())
        {
            Color outlineColor = this.form.outlineColor.get().copy();
            outlineColor.a *= opacity;
            int oc = outlineColor.getARGBColor();
            float ow = this.form.outlineWidth.get();
            
            context.stack.push();
            context.stack.translate(0, 0, -0.025F);
            
            if (customFont != null)
            {
                customFont.draw(content, x - ow, y, oc, oc, letterSpacing, 0F, context.stack.peek().getPositionMatrix(), consumers, light);
                customFont.draw(content, x + ow, y, oc, oc, letterSpacing, 0F, context.stack.peek().getPositionMatrix(), consumers, light);
                customFont.draw(content, x, y - ow, oc, oc, letterSpacing, 0F, context.stack.peek().getPositionMatrix(), consumers, light);
                customFont.draw(content, x, y + ow, oc, oc, letterSpacing, 0F, context.stack.peek().getPositionMatrix(), consumers, light);
            }
            else
            {
                renderer.draw(content, x - ow, y, oc, false, context.stack.peek().getPositionMatrix(), consumers, TextRenderer.TextLayerType.NORMAL, 0, light);
                renderer.draw(content, x + ow, y, oc, false, context.stack.peek().getPositionMatrix(), consumers, TextRenderer.TextLayerType.NORMAL, 0, light);
                renderer.draw(content, x, y - ow, oc, false, context.stack.peek().getPositionMatrix(), consumers, TextRenderer.TextLayerType.NORMAL, 0, light);
                renderer.draw(content, x, y + ow, oc, false, context.stack.peek().getPositionMatrix(), consumers, TextRenderer.TextLayerType.NORMAL, 0, light);
            }
            
            context.stack.pop();
        }

        if (customFont != null)
        {
            int c1 = color.getARGBColor();
            int c2 = c1;

            if (this.form.gradient.get())
            {
                Color gradientColor = this.form.gradientEndColor.get().copy();
                
                gradientColor.a *= opacity;
                gradientColor.mul(context.color);
                c2 = gradientColor.getARGBColor();
            }

            customFont.draw(content, x, y, c1, c2, letterSpacing, 0F, context.stack.peek().getPositionMatrix(), consumers, light, this.form.gradientOffset.get());
        }
        else
        {
            renderer.draw(
                content,
                x,
                y,
                color.getARGBColor(), false,
                context.stack.peek().getPositionMatrix(),
                consumers,
                TextRenderer.TextLayerType.NORMAL,
                0,
                light
            );
        }

        RenderSystem.enableDepthTest();

        consumers.draw();

        this.renderShadow(context, x, y, w, h);
    }

    private void renderLimitedString(FormRenderingContext context, CustomVertexConsumerProvider consumers, TextRenderer renderer, int light)
    {
        float transition = context.getTransition();
        int w = 0;
        int h = renderer.fontHeight - 2;
        String content = applyStyles(StringUtils.processColoredText(this.form.text.get()));
        
        String fontName = this.form.font.get();
        TextureFont customFont = null;
        
        if (!fontName.isEmpty())
        {
            int style = java.awt.Font.PLAIN;
            if (this.form.fontWeight.get() >= 700) style |= java.awt.Font.BOLD;
            if (this.form.fontStyle.get() >= 1) style |= java.awt.Font.ITALIC;
            
            customFont = FontUtils.getFont(fontName, style);
        }

        float letterSpacing = this.form.letterSpacing.get();
        List<String> lines;
        
        if (customFont != null)
        {
            lines = customFont.wrap(content, this.form.max.get(), letterSpacing);
        }
        else
        {
            lines = FontRenderer.wrap(renderer, content, this.form.max.get());
        }

        if (lines.size() <= 1)
        {
            this.renderString(context, consumers, renderer, light);
            return;
        }

        for (int i = 0; i < lines.size(); i++)
        {
            lines.set(i, lines.get(i).trim());
        }

        for (String line : lines)
        {
            int lw = customFont != null ? customFont.getWidth(line, letterSpacing) : renderer.getWidth(line) - 1;
            w = Math.max(lw, w);
        }

        int fh = customFont != null ? customFont.getHeight() : renderer.fontHeight;
        int lineHeight = (int) (fh + this.form.lineHeight.get());
        int totalHeight = (lines.size() - 1) * lineHeight + fh - 2;

        int x = (int) (-w * this.form.anchorX.get());
        int y = (int) (-totalHeight * this.form.anchorY.get());

        Color shadowColor = this.form.shadowColor.get().copy();
        Color color = this.form.color.get().copy();
        
        float opacity = this.form.opacity.get();
        color.a *= opacity;
        shadowColor.a *= opacity;

        color.mul(context.color);
        shadowColor.mul(context.color);
        shadowColor.a *= this.nametagAlpha;
        
        int align = this.form.textAlign.get(); // 0: Left, 1: Center, 2: Right

        for (String line : lines)
        {
            int lw = customFont != null ? customFont.getWidth(line, letterSpacing) : renderer.getWidth(line) - 1;
            int lx = x;
            
            if (align == 1) lx = x + (w - lw) / 2;
            else if (align == 2) lx = x + (w - lw);

            this.renderTextShadow(context, consumers, renderer, customFont, line, lx, y, letterSpacing, light, shadowColor);
            
            if (this.form.outline.get())
            {
                Color outlineColor = this.form.outlineColor.get().copy();
                outlineColor.a *= opacity;
                int oc = outlineColor.getARGBColor();
                float ow = this.form.outlineWidth.get();
                
                context.stack.push();
                context.stack.translate(0, 0, -0.025F);
                
                if (customFont != null)
                {
                    customFont.draw(line, lx - ow, y, oc, oc, letterSpacing, 0F, context.stack.peek().getPositionMatrix(), consumers, light);
                    customFont.draw(line, lx + ow, y, oc, oc, letterSpacing, 0F, context.stack.peek().getPositionMatrix(), consumers, light);
                    customFont.draw(line, lx, y - ow, oc, oc, letterSpacing, 0F, context.stack.peek().getPositionMatrix(), consumers, light);
                    customFont.draw(line, lx, y + ow, oc, oc, letterSpacing, 0F, context.stack.peek().getPositionMatrix(), consumers, light);
                }
                else
                {
                    renderer.draw(line, lx - ow, y, oc, false, context.stack.peek().getPositionMatrix(), consumers, TextRenderer.TextLayerType.NORMAL, 0, light);
                    renderer.draw(line, lx + ow, y, oc, false, context.stack.peek().getPositionMatrix(), consumers, TextRenderer.TextLayerType.NORMAL, 0, light);
                    renderer.draw(line, lx, y - ow, oc, false, context.stack.peek().getPositionMatrix(), consumers, TextRenderer.TextLayerType.NORMAL, 0, light);
                    renderer.draw(line, lx, y + ow, oc, false, context.stack.peek().getPositionMatrix(), consumers, TextRenderer.TextLayerType.NORMAL, 0, light);
                }
                context.stack.pop();
            }

            if (customFont != null)
            {
                int c1 = color.getARGBColor();
                int c2 = c1;

                if (this.form.gradient.get())
                {
                    Color gradientColor = this.form.gradientEndColor.get().copy();
                    
                    gradientColor.a *= opacity;
                    gradientColor.mul(context.color);
                    c2 = gradientColor.getARGBColor();
                }

                customFont.draw(line, lx, y, c1, c2, letterSpacing, 0F, context.stack.peek().getPositionMatrix(), consumers, light);
            }
            else
            {
                renderer.draw(
                    line,
                    lx,
                    y,
                    color.getARGBColor(), false,
                    context.stack.peek().getPositionMatrix(),
                    consumers,
                    TextRenderer.TextLayerType.NORMAL,
                    0,
                    light
                );
            }

            y += lineHeight;
        }

        RenderSystem.enableDepthTest();

        consumers.draw();

        this.renderShadow(context, x, y, w, totalHeight);
    }

    private void renderShadow(FormRenderingContext context, int x, int y, int w, int h)
    {
        float offset = this.form.offset.get();
        Color color = this.form.background.get().copy();

        color.mul(context.color);

        if (color.a <= 0)
        {
            return;
        }

        context.stack.push();
        context.stack.translate(0, 0, -0.2F);

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        fillQuad(
            builder, context.stack,
            x + w + offset, y - offset, 0,
            x - offset, y - offset, 0,
            x - offset, y + h + offset, 0,
            x + w + offset, y + h + offset, 0,
            color.r, color.g, color.b, color.a
        );

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        BufferRenderer.drawWithGlobalProgram(builder.end());
        context.stack.pop();
    }
}