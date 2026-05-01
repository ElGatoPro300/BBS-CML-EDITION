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
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

public class LabelFormRenderer extends FormRenderer<LabelForm>
{
    public static void fillQuad(BufferBuilder builder, MatrixStack stack, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float b, float a)
    {
        Matrix4f matrix4f = stack.peek().getPositionMatrix();

        /* 1 - BR, 2 - BL, 3 - TL, 4 - TR */
        builder.vertex(matrix4f, x1, y1, z1).color(r, g, b, a).texture(0F, 0F).next();
        builder.vertex(matrix4f, x2, y2, z2).color(r, g, b, a).texture(0F, 0F).next();
        builder.vertex(matrix4f, x3, y3, z3).color(r, g, b, a).texture(0F, 0F).next();
        builder.vertex(matrix4f, x1, y1, z1).color(r, g, b, a).texture(0F, 0F).next();
        builder.vertex(matrix4f, x3, y3, z3).color(r, g, b, a).texture(0F, 0F).next();
        builder.vertex(matrix4f, x4, y4, z4).color(r, g, b, a).texture(0F, 0F).next();
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
            Vector3f scale = Vectors.TEMP_3F;

            modelMatrix.getScale(scale);

            modelMatrix.m00(1).m01(0).m02(0);
            modelMatrix.m10(0).m11(1).m12(0);
            modelMatrix.m20(0).m21(0).m22(1);

            modelMatrix.scale(scale);

            context.stack.peek().getNormalMatrix().identity();
        }

        TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        float fontSize = this.form.fontSize.get();
        float scale = (1F / 16F) * (fontSize <= 0 ? 1F : fontSize);
        int light = context.light;

        MatrixStackUtils.scaleStack(context.stack, scale, -scale, scale);

        RenderSystem.disableCull();

        if (context.isPicking())
        {
            CustomVertexConsumerProvider.hijackVertexFormat((layer) ->
            {
                this.setupTarget(context, BBSShaders.getPickerModelsProgram());
                RenderSystem.setShader(BBSShaders::getPickerModelsProgram);
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

    private void renderString(FormRenderingContext context, CustomVertexConsumerProvider consumers, TextRenderer renderer, int light)
    {
        String content = applyStyles(StringUtils.processColoredText(this.form.text.get()));
        String fontName = this.form.font.get();
        TextureFont customFont = null;
        
        if (!fontName.isEmpty())
        {
            customFont = FontUtils.getFont(fontName);
        }

        float transition = context.getTransition();
        int w = customFont != null ? customFont.getWidth(content) : renderer.getWidth(content) - 1;
        int h = customFont != null ? customFont.getHeight() : renderer.fontHeight - 2;
        int x = (int) (-w * this.form.anchorX.get());
        int y = (int) (-h * this.form.anchorY.get());

        Color shadowColor = this.form.shadowColor.get().copy();
        Color color = this.form.color.get().copy();
        
        float opacity = this.form.opacity.get();
        color.a *= opacity;
        shadowColor.a *= opacity;

        color.mul(context.color);
        shadowColor.mul(context.color);

        if (shadowColor.a > 0)
        {
            context.stack.push();
            context.stack.translate(0F, 0F, -0.1F);
            /* Apply blur/offset */
            float sx = this.form.shadowX.get();
            float sy = this.form.shadowY.get();
            
            if (customFont != null)
            {
                customFont.draw(content, x + sx, y + sy, shadowColor.getARGBColor(), context.stack.peek().getPositionMatrix(), consumers, light);
            }
            else
            {
                renderer.draw(
                    content,
                    x + sx,
                    y + sy,
                    shadowColor.getARGBColor(), false,
                    context.stack.peek().getPositionMatrix(),
                    consumers,
                    TextRenderer.TextLayerType.NORMAL,
                    0,
                    light
                );
            }
            context.stack.pop();
        }

        if (this.form.outline.get())
        {
            Color outlineColor = this.form.outlineColor.get().copy();
            outlineColor.a *= opacity;
            int oc = outlineColor.getARGBColor();
            float ow = this.form.outlineWidth.get();
            
            context.stack.push();
            context.stack.translate(0, 0, -0.05F);
            
            if (customFont != null)
            {
                customFont.draw(content, x - ow, y, oc, context.stack.peek().getPositionMatrix(), consumers, light);
                customFont.draw(content, x + ow, y, oc, context.stack.peek().getPositionMatrix(), consumers, light);
                customFont.draw(content, x, y - ow, oc, context.stack.peek().getPositionMatrix(), consumers, light);
                customFont.draw(content, x, y + ow, oc, context.stack.peek().getPositionMatrix(), consumers, light);
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
            customFont.draw(content, x, y, color.getARGBColor(), context.stack.peek().getPositionMatrix(), consumers, light);
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
            customFont = FontUtils.getFont(fontName);
        }

        List<String> lines;
        
        if (customFont != null)
        {
            lines = customFont.wrap(content, this.form.max.get());
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
            int lw = customFont != null ? customFont.getWidth(line) : renderer.getWidth(line) - 1;
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
        
        int align = this.form.textAlign.get(); // 0: Left, 1: Center, 2: Right

        for (String line : lines)
        {
            int lw = customFont != null ? customFont.getWidth(line) : renderer.getWidth(line) - 1;
            int lx = x;
            
            if (align == 1) lx = x + (w - lw) / 2;
            else if (align == 2) lx = x + (w - lw);

            if (shadowColor.a > 0)
            {
                context.stack.push();
                context.stack.translate(0F, 0F, -0.1F);
                
                if (customFont != null)
                {
                    customFont.draw(line, lx + this.form.shadowX.get(), y + this.form.shadowY.get(), shadowColor.getARGBColor(), context.stack.peek().getPositionMatrix(), consumers, light);
                }
                else
                {
                    renderer.draw(
                        line,
                        lx + this.form.shadowX.get(),
                        y + this.form.shadowY.get(),
                        shadowColor.getARGBColor(), false,
                        context.stack.peek().getPositionMatrix(),
                        consumers,
                        TextRenderer.TextLayerType.NORMAL,
                        0,
                        light
                    );
                }
                context.stack.pop();
            }
            
            if (this.form.outline.get())
            {
                Color outlineColor = this.form.outlineColor.get().copy();
                outlineColor.a *= opacity;
                int oc = outlineColor.getARGBColor();
                float ow = this.form.outlineWidth.get();
                
                context.stack.push();
                context.stack.translate(0, 0, -0.05F);
                
                if (customFont != null)
                {
                    customFont.draw(line, lx - ow, y, oc, context.stack.peek().getPositionMatrix(), consumers, light);
                    customFont.draw(line, lx + ow, y, oc, context.stack.peek().getPositionMatrix(), consumers, light);
                    customFont.draw(line, lx, y - ow, oc, context.stack.peek().getPositionMatrix(), consumers, light);
                    customFont.draw(line, lx, y + ow, oc, context.stack.peek().getPositionMatrix(), consumers, light);
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
                customFont.draw(line, lx, y, color.getARGBColor(), context.stack.peek().getPositionMatrix(), consumers, light);
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

        BufferBuilder builder = Tessellator.getInstance().getBuffer();

        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR_TEXTURE);

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
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferRenderer.drawWithGlobalProgram(builder.end());
        context.stack.pop();
    }
}