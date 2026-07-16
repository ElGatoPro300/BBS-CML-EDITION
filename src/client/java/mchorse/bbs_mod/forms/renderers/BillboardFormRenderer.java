package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.forms.forms.BillboardForm;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.forms.renderers.utils.FlatGlowOverlayPass;
import mchorse.bbs_mod.forms.renderers.utils.FlatPaintOverlayPass;
import mchorse.bbs_mod.forms.renderers.utils.FormColorBlend;
import mchorse.bbs_mod.forms.renderers.utils.FormTextureBlendRenderer;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.Quad;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.joml.Vectors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.function.Supplier;

public class BillboardFormRenderer extends FormRenderer<BillboardForm>
{
    private static final Quad quad = new Quad();
    private static final Quad uvQuad = new Quad();

    private static final Matrix4f matrix = new Matrix4f();
    private static final float FACE_Z_BIAS = 0.0005F;

    public BillboardFormRenderer(BillboardForm form)
    {
        super(form);
    }

    @Override
    public void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        MatrixStack stack = context.batcher.getContext().getMatrices();

        stack.push();

        Matrix4f uiMatrix = ModelFormRenderer.getUIMatrix(context, x1, y1, x2, y2);

        this.applyTransforms(uiMatrix, context.getTransition());
        MatrixStackUtils.multiply(stack, uiMatrix);
        stack.translate(0F, 1F, 0F);
        stack.scale(1.5F, 1.5F, 1.5F);
        stack.scale(this.form.uiScale.get(), this.form.uiScale.get(), this.form.uiScale.get());

        Vector3f light0 = new Vector3f(0.85F, 0.85F, -1F).normalize();
        Vector3f light1 = new Vector3f(-0.85F, 0.85F, 1F).normalize();
        RenderSystem.setupLevelDiffuseLighting(light0, light1);

        VertexFormat format = VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL;

        this.renderModel(format, GameRenderer::getRenderTypeEntityTranslucentProgram,
            stack,
            OverlayTexture.DEFAULT_UV, LightmapTextureManager.MAX_LIGHT_COORDINATE, Colors.WHITE,
            context.getTransition(),
            null,
            true,
            false,
            null
        );

        DiffuseLighting.disableGuiDepthLighting();

        stack.pop();
    }

    @Override
    public void render3D(FormRenderingContext context)
    {
        boolean shading = this.form.shading.get();

        if (BBSRendering.isIrisShadersEnabled())
        {
            shading = true;
        }

        VertexFormat format = shading ? VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL : VertexFormats.POSITION_TEXTURE_COLOR;
        Supplier<ShaderProgram> shader = this.getShader(context,
            shading ? GameRenderer::getRenderTypeEntityTranslucentProgram : GameRenderer::getPositionTexColorProgram,
            shading ? BBSShaders::getPickerBillboardProgram : BBSShaders::getPickerBillboardNoShadingProgram
        );

        this.renderModel(format, shader, context.stack, context.overlay, context.light, context.color, context.getTransition(), context.camera, false, context.modelRenderer || context.isPicking(), context);
    }

    private void renderModel(VertexFormat format, Supplier<ShaderProgram> shader, MatrixStack matrices, int overlay, int light, int overlayColor, float transition, Camera camera, boolean invertY, boolean modelRenderer, FormRenderingContext deferContext)
    {
        Link defaultLink = this.form.texture.get();

        if (defaultLink == null)
        {
            return;
        }

        FormTextureBlendRenderer.draw(this.form.textureBlend, defaultLink, (link, alphaFactor) ->
        {
            Texture texture = BBSModClient.getTextures().getTexture(link);

            if (texture == null)
            {
                return;
            }

            this.renderModelPass(format, texture, shader, matrices, overlay, light, overlayColor, transition, camera, invertY, modelRenderer, alphaFactor, deferContext, link);
        });
    }

    private void renderModelPass(VertexFormat format, Texture texture, Supplier<ShaderProgram> shader, MatrixStack matrices, int overlay, int light, int overlayColor, float transition, Camera camera, boolean invertY, boolean modelRenderer, float alphaFactor, FormRenderingContext deferContext, Link textureLink)
    {
        float w = texture.width;
        float h = texture.height;
        float ow = w;
        float oh = h;

        /* TL = top left, BR = bottom right*/
        Vector4f crop = this.form.crop.get();
        float uvTLx = crop.x / w;
        float uvTLy = crop.y / h;
        float uvBRx = 1 - crop.z / w;
        float uvBRy = 1 - crop.w / h;

        uvQuad.p1.set(uvTLx, uvTLy, 0);
        uvQuad.p2.set(uvBRx, uvTLy, 0);
        uvQuad.p3.set(uvTLx, uvBRy, 0);
        uvQuad.p4.set(uvBRx, uvBRy, 0);

        float uvFinalTLx = uvTLx;
        float uvFinalTLy = uvTLy;
        float uvFinalBRx = uvBRx;
        float uvFinalBRy = uvBRy;

        if (this.form.resizeCrop.get())
        {
            uvFinalTLx = uvFinalTLy = 0F;
            uvFinalBRx = uvFinalBRy = 1F;

            w = w - crop.x - crop.z;
            h = h - crop.y - crop.w;
        }

        /* Calculate quad's size (vertices, not UV) */
        float ratioX = w > h ? h / w : 1F;
        float ratioY = h > w ? w / h : 1F;
        float TLx = (uvFinalTLx - 0.5F) * ratioY;
        float TLy = -(uvFinalTLy - 0.5F) * ratioX;
        float BRx = (uvFinalBRx - 0.5F) * ratioY;
        float BRy = -(uvFinalBRy - 0.5F) * ratioX;

        quad.p1.set(TLx, TLy, 0);
        quad.p2.set(BRx, TLy, 0);
        quad.p3.set(TLx, BRy, 0);
        quad.p4.set(BRx, BRy, 0);

        float offsetX = this.form.offsetX.get();
        float offsetY = this.form.offsetY.get();
        float rotation = this.form.rotation.get();

        if (offsetX != 0F || offsetY != 0F || rotation != 0F)
        {
            float centerX = (crop.x + (ow - crop.z)) / 2F / ow;
            float centerY = (crop.y + (oh - crop.w)) / 2F / ow;

            matrix.identity()
                .translate(centerX, centerY, 0)
                .rotateZ(MathUtils.toRad(rotation))
                .translate(offsetX / ow, offsetY / oh, 0)
                .translate(-centerX, -centerY, 0);

            uvQuad.transform(matrix);
        }

        this.renderQuad(format, texture, shader, matrices, overlay, light, overlayColor, transition, camera, invertY, modelRenderer, alphaFactor, deferContext, textureLink);
    }

    private void renderQuad(VertexFormat format, Texture texture, Supplier<ShaderProgram> shader, MatrixStack matrices, int overlay, int light, int overlayColor, float transition, Camera camera, boolean invertY, boolean modelRenderer, float alphaFactor, FormRenderingContext deferContext, Link textureLink)
    {
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, format);
        Color color = new Color().set(overlayColor, true);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        MatrixStack.Entry entry = matrices.peek();

        color.mul(this.form.color.get());
        color.a *= alphaFactor;

        /* Main pass: negative paint only; positive paint is drawn in a separate overlay pass */
        PaintSettings paintSettings = this.form.paintSettings.get();
        Color legacyPaint = this.form.paintColor.get();
        float paintStrength = paintSettings.resolveIntensity(legacyPaint);

        if (paintStrength < 0F)
        {
            FormColorBlend.applyPaintBlend(color, paintSettings, legacyPaint);
        }

        boolean positivePaint = FormColorBlend.hasPositivePaint(paintSettings, legacyPaint);
        Color resolvedPaint = positivePaint ? FormColorBlend.resolvePaintColor(paintSettings, legacyPaint) : null;

        GlowSettings glowSettings = this.form.glowSettings.get();
        Color legacyGlow = this.form.glowingColor.get();
        float glowIntensity = glowSettings.resolveIntensity(legacyGlow);

        if (glowIntensity < 0F)
        {
            FormColorBlend.blendFormGlowBrighten(color, glowSettings, legacyGlow);
        }

        if (this.form.billboard.get())
        {
            Matrix4f modelMatrix = matrices.peek().getPositionMatrix();
            Vector3f scale = new Vector3f();

            modelMatrix.getScale(scale);

            if (invertY)
            {
                scale.y = -scale.y;
            }

            modelMatrix.m00(1).m01(0).m02(0);
            modelMatrix.m10(0).m11(1).m12(0);
            modelMatrix.m20(0).m21(0).m22(1);

            if (camera != null && !modelRenderer)
            {
                modelMatrix.mul(camera.view);
            }

            modelMatrix.scale(scale);

            matrices.peek().getNormalMatrix().identity();

            if (camera != null && !modelRenderer)
            {
                matrices.peek().getNormalMatrix().set(camera.view);
            }

            matrices.peek().getNormalMatrix().scale(1F / scale.x, 1F / scale.y, 1F / scale.z);
        }

        GameRenderer gameRenderer = MinecraftClient.getInstance().gameRenderer;
        if (format == VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL)
        {
            gameRenderer.getLightmapTextureManager().enable();
            gameRenderer.getOverlayTexture().setupOverlayColor();
        }

        BBSModClient.getTextures().bindTexture(texture);
        RenderSystem.setShader(shader);

        texture.bind();
        texture.setFilterMipmap(this.form.linear.get(), this.form.mipmap.get());

        RenderSystem.disableCull();

        /* Front */
        this.fill(format, builder, matrix, quad.p3.x, quad.p3.y, FACE_Z_BIAS, color, uvQuad.p3.x, uvQuad.p3.y, overlay, light, entry, 1F);
        this.fill(format, builder, matrix, quad.p2.x, quad.p2.y, FACE_Z_BIAS, color, uvQuad.p2.x, uvQuad.p2.y, overlay, light, entry, 1F);
        this.fill(format, builder, matrix, quad.p1.x, quad.p1.y, FACE_Z_BIAS, color, uvQuad.p1.x, uvQuad.p1.y, overlay, light, entry, 1F);

        this.fill(format, builder, matrix, quad.p3.x, quad.p3.y, FACE_Z_BIAS, color, uvQuad.p3.x, uvQuad.p3.y, overlay, light, entry, 1F);
        this.fill(format, builder, matrix, quad.p4.x, quad.p4.y, FACE_Z_BIAS, color, uvQuad.p4.x, uvQuad.p4.y, overlay, light, entry, 1F);
        this.fill(format, builder, matrix, quad.p2.x, quad.p2.y, FACE_Z_BIAS, color, uvQuad.p2.x, uvQuad.p2.y, overlay, light, entry, 1F);

        /* Back */
        this.fill(format, builder, matrix, quad.p1.x, quad.p1.y, -FACE_Z_BIAS, color, uvQuad.p1.x, uvQuad.p1.y, overlay, light, entry, -1F);
        this.fill(format, builder, matrix, quad.p2.x, quad.p2.y, -FACE_Z_BIAS, color, uvQuad.p2.x, uvQuad.p2.y, overlay, light, entry, -1F);
        this.fill(format, builder, matrix, quad.p3.x, quad.p3.y, -FACE_Z_BIAS, color, uvQuad.p3.x, uvQuad.p3.y, overlay, light, entry, -1F);

        this.fill(format, builder, matrix, quad.p2.x, quad.p2.y, -FACE_Z_BIAS, color, uvQuad.p2.x, uvQuad.p2.y, overlay, light, entry, -1F);
        this.fill(format, builder, matrix, quad.p4.x, quad.p4.y, -FACE_Z_BIAS, color, uvQuad.p4.x, uvQuad.p4.y, overlay, light, entry, -1F);
        this.fill(format, builder, matrix, quad.p3.x, quad.p3.y, -FACE_Z_BIAS, color, uvQuad.p3.x, uvQuad.p3.y, overlay, light, entry, -1F);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        BufferRenderer.drawWithGlobalProgram(builder.end());

        if (positivePaint && !modelRenderer)
        {
            if (deferContext != null && BBSRendering.isIrisWorldPaintDeferral())
            {
                this.submitDeferredBillboardPaintOverlay(texture, textureLink, shader, matrices, resolvedPaint, color.a);
            }
            else
            {
                this.renderPaintOverlay(texture, shader, matrices, overlay, resolvedPaint, color.a);
            }
        }

        if (glowIntensity > 0F && !modelRenderer)
        {
            this.renderGlowOverlay(texture, shader, matrices, glowSettings, legacyGlow, color.a, glowIntensity);
        }

        RenderSystem.enableCull();

        texture.setFilterMipmap(false, false);
        if (format == VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL)
        {
            gameRenderer.getLightmapTextureManager().disable();
            gameRenderer.getOverlayTexture().teardownOverlayColor();
        }
    }

    private VertexConsumer fill(VertexFormat format, VertexConsumer consumer, Matrix4f matrix, float x, float y, float z, Color color, float u, float v, int overlay, int light, MatrixStack.Entry entry, float nz)
    {
        if (format == VertexFormats.POSITION_TEXTURE_LIGHT_COLOR)
        {
            return consumer.vertex(matrix, x, y, z).texture(u, v).light(light).color(color.r, color.g, color.b, color.a);
        }

        if (format == VertexFormats.POSITION_TEXTURE_COLOR)
        {
            return consumer.vertex(matrix, x, y, z).texture(u, v).color(color.r, color.g, color.b, color.a);
        }

        return consumer.vertex(matrix, x, y, z).color(color.r, color.g, color.b, color.a).texture(u, v).overlay(overlay).light(light).normal(entry, 0F, 0F, nz);
    }

    private void submitDeferredBillboardPaintOverlay(Texture texture, Link textureLink, Supplier<ShaderProgram> shader, MatrixStack matrices, Color resolvedPaint, float alpha)
    {
        Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(matrices.peek().getPositionMatrix()));
        Matrix3f normalMatrix = new Matrix3f(matrices.peek().getNormalMatrix());
        Color paintOverlay = new Color(resolvedPaint.r, resolvedPaint.g, resolvedPaint.b, resolvedPaint.a);

        paintOverlay.a *= alpha;

        Quad localQuad = new Quad();
        Quad localUvQuad = new Quad();

        localQuad.copy(quad);
        localUvQuad.copy(uvQuad);

        ModelVAORenderer.submitPaintOverlay(false, () ->
        {
            Texture deferredTexture = texture;

            if (textureLink != null)
            {
                Texture linkedTexture = BBSModClient.getTextures().getTexture(textureLink);

                if (linkedTexture != null)
                {
                    deferredTexture = linkedTexture;
                }
            }

            if (deferredTexture == null)
            {
                return;
            }

            MatrixStack overlayStack = new MatrixStack();

            overlayStack.peek().getPositionMatrix().set(positionMatrix);
            overlayStack.peek().getNormalMatrix().set(normalMatrix);

            this.renderPaintOverlay(deferredTexture, shader, overlayStack, OverlayTexture.DEFAULT_UV, paintOverlay, 1F, localQuad, localUvQuad);
        });
    }

    private void renderPaintOverlay(Texture texture, Supplier<ShaderProgram> shader, MatrixStack matrices, int overlay, Color resolvedPaint, float alpha)
    {
        this.renderPaintOverlay(texture, shader, matrices, overlay, resolvedPaint, alpha, quad, uvQuad);
    }

    private void renderPaintOverlay(Texture texture, Supplier<ShaderProgram> shader, MatrixStack matrices, int overlay, Color resolvedPaint, float alpha, Quad drawQuad, Quad drawUvQuad)
    {
        Color paintOverlay = new Color(resolvedPaint.r, resolvedPaint.g, resolvedPaint.b, resolvedPaint.a);

        paintOverlay.a *= alpha;

        matrices.push();
        matrices.translate(0F, 0F, 0.001F);

        Matrix4f paintMatrix = matrices.peek().getPositionMatrix();
        MatrixStack.Entry entry = matrices.peek();

        BBSModClient.getTextures().bindTexture(texture);
        texture.bind();
        texture.setFilterMipmap(this.form.linear.get(), this.form.mipmap.get());

        FlatPaintOverlayPass.render(() ->
        {
            BufferBuilder paintBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
            int paintLight = LightmapTextureManager.MAX_LIGHT_COORDINATE;

            RenderSystem.disableCull();

            this.fillPaint(paintBuilder, paintMatrix, drawQuad.p3.x, drawQuad.p3.y, FACE_Z_BIAS, paintOverlay, drawUvQuad.p3.x, drawUvQuad.p3.y, overlay, paintLight, entry, 1F);
            this.fillPaint(paintBuilder, paintMatrix, drawQuad.p2.x, drawQuad.p2.y, FACE_Z_BIAS, paintOverlay, drawUvQuad.p2.x, drawUvQuad.p2.y, overlay, paintLight, entry, 1F);
            this.fillPaint(paintBuilder, paintMatrix, drawQuad.p1.x, drawQuad.p1.y, FACE_Z_BIAS, paintOverlay, drawUvQuad.p1.x, drawUvQuad.p1.y, overlay, paintLight, entry, 1F);

            this.fillPaint(paintBuilder, paintMatrix, drawQuad.p3.x, drawQuad.p3.y, FACE_Z_BIAS, paintOverlay, drawUvQuad.p3.x, drawUvQuad.p3.y, overlay, paintLight, entry, 1F);
            this.fillPaint(paintBuilder, paintMatrix, drawQuad.p4.x, drawQuad.p4.y, FACE_Z_BIAS, paintOverlay, drawUvQuad.p4.x, drawUvQuad.p4.y, overlay, paintLight, entry, 1F);
            this.fillPaint(paintBuilder, paintMatrix, drawQuad.p2.x, drawQuad.p2.y, FACE_Z_BIAS, paintOverlay, drawUvQuad.p2.x, drawUvQuad.p2.y, overlay, paintLight, entry, 1F);

            this.fillPaint(paintBuilder, paintMatrix, drawQuad.p1.x, drawQuad.p1.y, -FACE_Z_BIAS, paintOverlay, drawUvQuad.p1.x, drawUvQuad.p1.y, overlay, paintLight, entry, -1F);
            this.fillPaint(paintBuilder, paintMatrix, drawQuad.p2.x, drawQuad.p2.y, -FACE_Z_BIAS, paintOverlay, drawUvQuad.p2.x, drawUvQuad.p2.y, overlay, paintLight, entry, -1F);
            this.fillPaint(paintBuilder, paintMatrix, drawQuad.p3.x, drawQuad.p3.y, -FACE_Z_BIAS, paintOverlay, drawUvQuad.p3.x, drawUvQuad.p3.y, overlay, paintLight, entry, -1F);

            this.fillPaint(paintBuilder, paintMatrix, drawQuad.p2.x, drawQuad.p2.y, -FACE_Z_BIAS, paintOverlay, drawUvQuad.p2.x, drawUvQuad.p2.y, overlay, paintLight, entry, -1F);
            this.fillPaint(paintBuilder, paintMatrix, drawQuad.p4.x, drawQuad.p4.y, -FACE_Z_BIAS, paintOverlay, drawUvQuad.p4.x, drawUvQuad.p4.y, overlay, paintLight, entry, -1F);
            this.fillPaint(paintBuilder, paintMatrix, drawQuad.p3.x, drawQuad.p3.y, -FACE_Z_BIAS, paintOverlay, drawUvQuad.p3.x, drawUvQuad.p3.y, overlay, paintLight, entry, -1F);

            BufferRenderer.drawWithGlobalProgram(paintBuilder.end());

            RenderSystem.enableCull();
        });

        texture.setFilterMipmap(false, false);
        RenderSystem.setShader(shader);
        matrices.pop();
    }

    private void fillPaint(BufferBuilder builder, Matrix4f matrix, float x, float y, float z, Color color, float u, float v, int overlay, int light, MatrixStack.Entry entry, float nz)
    {
        builder.vertex(matrix, x, y, z).color(color.r, color.g, color.b, color.a).texture(u, v).overlay(overlay).light(light).normal(entry, 0F, 0F, nz);
    }

    private void renderGlowOverlay(Texture texture, Supplier<ShaderProgram> shader, MatrixStack matrices, GlowSettings glowSettings, Color legacyGlow, float alpha, float glowIntensity)
    {
        matrices.push();
        matrices.translate(0F, 0F, 0.002F);

        Matrix4f glowMatrix = matrices.peek().getPositionMatrix();

        BBSModClient.getTextures().bindTexture(texture);
        texture.bind();
        texture.setFilterMipmap(this.form.linear.get(), this.form.mipmap.get());

        FlatGlowOverlayPass.render(glowSettings, legacyGlow, alpha, glowIntensity, (glowColor) ->
        {
            BufferBuilder glowBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE_COLOR);

            RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
            RenderSystem.disableCull();

            this.fillGlow(glowBuilder, glowMatrix, quad.p3.x, quad.p3.y, FACE_Z_BIAS, glowColor, uvQuad.p3.x, uvQuad.p3.y);
            this.fillGlow(glowBuilder, glowMatrix, quad.p2.x, quad.p2.y, FACE_Z_BIAS, glowColor, uvQuad.p2.x, uvQuad.p2.y);
            this.fillGlow(glowBuilder, glowMatrix, quad.p1.x, quad.p1.y, FACE_Z_BIAS, glowColor, uvQuad.p1.x, uvQuad.p1.y);

            this.fillGlow(glowBuilder, glowMatrix, quad.p3.x, quad.p3.y, FACE_Z_BIAS, glowColor, uvQuad.p3.x, uvQuad.p3.y);
            this.fillGlow(glowBuilder, glowMatrix, quad.p4.x, quad.p4.y, FACE_Z_BIAS, glowColor, uvQuad.p4.x, uvQuad.p4.y);
            this.fillGlow(glowBuilder, glowMatrix, quad.p2.x, quad.p2.y, FACE_Z_BIAS, glowColor, uvQuad.p2.x, uvQuad.p2.y);

            this.fillGlow(glowBuilder, glowMatrix, quad.p1.x, quad.p1.y, -FACE_Z_BIAS, glowColor, uvQuad.p1.x, uvQuad.p1.y);
            this.fillGlow(glowBuilder, glowMatrix, quad.p2.x, quad.p2.y, -FACE_Z_BIAS, glowColor, uvQuad.p2.x, uvQuad.p2.y);
            this.fillGlow(glowBuilder, glowMatrix, quad.p3.x, quad.p3.y, -FACE_Z_BIAS, glowColor, uvQuad.p3.x, uvQuad.p3.y);

            this.fillGlow(glowBuilder, glowMatrix, quad.p2.x, quad.p2.y, -FACE_Z_BIAS, glowColor, uvQuad.p2.x, uvQuad.p2.y);
            this.fillGlow(glowBuilder, glowMatrix, quad.p4.x, quad.p4.y, -FACE_Z_BIAS, glowColor, uvQuad.p4.x, uvQuad.p4.y);
            this.fillGlow(glowBuilder, glowMatrix, quad.p3.x, quad.p3.y, -FACE_Z_BIAS, glowColor, uvQuad.p3.x, uvQuad.p3.y);

            BufferRenderer.drawWithGlobalProgram(glowBuilder.end());

            RenderSystem.enableCull();
        });

        texture.setFilterMipmap(false, false);
        RenderSystem.setShader(shader);
        matrices.pop();
    }

    private void fillGlow(BufferBuilder builder, Matrix4f matrix, float x, float y, float z, Color color, float u, float v)
    {
        builder.vertex(matrix, x, y, z).texture(u, v).color(color.r, color.g, color.b, color.a);
    }
}