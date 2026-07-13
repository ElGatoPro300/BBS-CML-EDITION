package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.cubic.render.vao.ModelVAO;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.forms.forms.ExtrudedForm;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.forms.forms.utils.TextureBlend;
import mchorse.bbs_mod.forms.renderers.utils.FormColorBlend;
import mchorse.bbs_mod.forms.renderers.utils.FormTextureBlendRenderer;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.joml.Vectors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;

import java.util.function.Supplier;

public class ExtrudedFormRenderer extends FormRenderer<ExtrudedForm>
{
    public ExtrudedFormRenderer(ExtrudedForm form)
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
        stack.scale(1.5F, 1.5F, 4F);
        stack.scale(this.form.uiScale.get(), this.form.uiScale.get(), this.form.uiScale.get());

        /* Shading fix */
        stack.peek().getNormalMatrix().getScale(Vectors.EMPTY_3F);
        stack.peek().getNormalMatrix().scale(1F / Vectors.EMPTY_3F.x, -1F / Vectors.EMPTY_3F.y, 1F / Vectors.EMPTY_3F.z);

        Vector3f light0 = new Vector3f(0.85F, 0.85F, -1F).normalize();
        Vector3f light1 = new Vector3f(-0.85F, 0.85F, 1F).normalize();
        RenderSystem.setupLevelDiffuseLighting(light0, light1);

        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        this.renderModel(BBSShaders::getModel,
            stack,
            OverlayTexture.DEFAULT_UV, LightmapTextureManager.MAX_LIGHT_COORDINATE, Colors.WHITE,
            context.getTransition(),
            null,
            true,
            false,
            null
        );
        RenderSystem.depthFunc(GL11.GL_ALWAYS);

        DiffuseLighting.disableGuiDepthLighting();

        stack.pop();
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
        boolean shading = this.form.shading.get();

        if (BBSRendering.isIrisShadersEnabled())
        {
            shading = true;
        }

        VertexFormat format = shading ? VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL : VertexFormats.POSITION_TEXTURE_COLOR;
        Supplier<ShaderProgram> normalShader = shading
            ? () -> {
                RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_ENTITY_TRANSLUCENT);
                return RenderSystem.getShader();
            }
            : () -> {
                RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
                return RenderSystem.getShader();
            };
        Supplier<ShaderProgram> pickingShader = shading
            ? BBSShaders::getPickerBillboardProgram
            : BBSShaders::getPickerBillboardNoShadingProgram;
        Supplier<ShaderProgram> shader = this.getShader(context, normalShader, pickingShader);

        this.renderModel(shader, context.stack, context.overlay, context.light, context.color, context.getTransition(), context.camera, false, context.modelRenderer || context.isPicking(), context.world);
    }

    private void renderModel(Supplier<ShaderProgram> shader, MatrixStack matrices, int overlay, int light, int overlayColor, float transition, Camera camera, boolean invertY, boolean modelRenderer, MatrixStack world)
    {
        Link texture = this.form.texture.get();
        ModelVAO data = BBSModClient.getTextures().getExtruder().get(texture);

        if (data != null)
        {
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
                matrices.peek().getNormalMatrix().scale(1F / scale.x, 1F / scale.y, 1F / scale.z);
            }

            Color color = Colors.COLOR.set(overlayColor, true);
            GameRenderer gameRenderer = MinecraftClient.getInstance().gameRenderer;
            Color formColor = this.form.color.get();

            color.mul(formColor);

            GlowSettings glow = this.form.glowSettings.get();
            Color legacyGlow = this.form.glowingColor.get();
            boolean hasGlow = glow.resolveIntensity(legacyGlow) != 0F;
            Color resolvedGlow = new Color();

            glow.resolveColor(legacyGlow, resolvedGlow);

            PaintSettings paint = this.form.paintSettings.get();
            Color legacyPaint = this.form.paintColor.get();
            Color paintColor = new Color();

            paint.resolveColor(legacyPaint, paintColor);

            float paintStrength = paint.resolveIntensity(legacyPaint);

            paintColor.a = paintStrength;

            boolean irisWorldPaintDeferral = BBSRendering.isIrisWorldPaintDeferral();
            boolean paintActive = paintStrength != 0F;
            boolean deferPaintToOverlay = irisWorldPaintDeferral && paintActive;
            Supplier<ShaderProgram> renderShader = shader;
            boolean bbsModelShader = !BBSRendering.isIrisWorldModelPass();
            boolean syncedGlow = hasGlow && glow.resolveSync();
            boolean shaderOverlay = irisWorldPaintDeferral && syncedGlow && !paintActive;
            boolean deferGlowWithPaint = deferPaintToOverlay && syncedGlow;
            boolean deferGlowToOverlay = shaderOverlay;

            if (!bbsModelShader && !shaderOverlay && !deferPaintToOverlay)
            {
                FormColorBlend.blendFormGlowBrighten(color, glow, legacyGlow);
            }

            if (paintActive)
            {
                this.form.shaderShadow.setRuntimeValue(paint.effectiveShaderShadow(legacyPaint) > 0.001F);
            }

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            gameRenderer.getLightmapTextureManager().enable();
            gameRenderer.getOverlayTexture().setupOverlayColor();

            if (deferPaintToOverlay)
            {
                ModelVAORenderer.setPaint(0F, 0F, 0F, 0F);
            }
            else if (paintActive)
            {
                ModelVAORenderer.setPaint(paintColor.r, paintColor.g, paintColor.b, paintStrength);
            }
            else
            {
                ModelVAORenderer.setPaint(0F, 0F, 0F, 0F);
            }

            if (deferGlowToOverlay || deferGlowWithPaint)
            {
                GlowSettings glowOff = glow.copy();

                glowOff.intensity = 0F;
                ModelVAORenderer.setGlow(glowOff, resolvedGlow.r, resolvedGlow.g, resolvedGlow.b, legacyGlow);
            }
            else
            {
                ModelVAORenderer.setGlow(glow, resolvedGlow.r, resolvedGlow.g, resolvedGlow.b, legacyGlow);
            }

            TextureBlend textureBlend = this.form.textureBlend;
            boolean useShaderBlend = bbsModelShader && FormTextureBlendRenderer.isBlending(textureBlend);
            TextureBlend textureBlendSnapshot = textureBlend == null ? null : new TextureBlend(textureBlend.from, textureBlend.to, textureBlend.blend);

            try
            {
                if (useShaderBlend)
                {
                    Link fromTexture = FormTextureBlendRenderer.resolveFrom(textureBlend, texture);
                    Link toTexture = FormTextureBlendRenderer.resolveTo(textureBlend, texture);
                    ModelVAO fromData = BBSModClient.getTextures().getExtruder().get(fromTexture);

                    if (fromData != null)
                    {
                        ModelVAORenderer.setTextureBlend(toTexture, textureBlend.blend);

                        try
                        {
                            BBSModClient.getTextures().bindTexture(fromTexture);
                            ModelVAORenderer.render(renderShader.get(), fromData, matrices, color.r, color.g, color.b, color.a, light, overlay);
                        }
                        finally
                        {
                            ModelVAORenderer.clearTextureBlend();
                        }
                    }
                }
                else
                {
                    FormTextureBlendRenderer.draw(textureBlend, texture, (link, alphaFactor) ->
                    {
                        ModelVAO passData = BBSModClient.getTextures().getExtruder().get(link);

                        if (passData == null)
                        {
                            return;
                        }

                        Color passColor = color.copy();

                        passColor.a *= alphaFactor;
                        BBSModClient.getTextures().bindTexture(link);
                        ModelVAORenderer.render(renderShader.get(), passData, matrices, passColor.r, passColor.g, passColor.b, passColor.a, light, overlay);
                    });
                }

                if (deferPaintToOverlay)
                {
                    Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(matrices.peek().getPositionMatrix()));
                    Matrix3f normalMatrix = new Matrix3f(matrices.peek().getNormalMatrix());
                    float cr = color.r;
                    float cg = color.g;
                    float cb = color.b;
                    float ca = color.a;
                    float pr = paintColor.r;
                    float pg = paintColor.g;
                    float pb = paintColor.b;
                    float pa = paintStrength;
                    int overlayLight = light;
                    int overlayOverlay = overlay;

                    ModelVAORenderer.submitPaintOverlay(deferGlowWithPaint, () ->
                    {
                        ModelVAORenderer.setPaint(pr, pg, pb, pa);

                        if (deferGlowWithPaint)
                        {
                            ModelVAORenderer.setGlow(glow, resolvedGlow.r, resolvedGlow.g, resolvedGlow.b, legacyGlow);
                        }
                        else
                        {
                            GlowSettings glowOff = glow.copy();

                            glowOff.intensity = 0F;
                            ModelVAORenderer.setGlow(glowOff, resolvedGlow.r, resolvedGlow.g, resolvedGlow.b, legacyGlow);
                        }

                        try
                        {
                            MatrixStack overlayStack = new MatrixStack();

                            overlayStack.peek().getPositionMatrix().set(positionMatrix);
                            overlayStack.peek().getNormalMatrix().set(normalMatrix);

                            if (useShaderBlend && textureBlendSnapshot != null)
                            {
                                Link fromTexture = FormTextureBlendRenderer.resolveFrom(textureBlendSnapshot, texture);
                                Link toTexture = FormTextureBlendRenderer.resolveTo(textureBlendSnapshot, texture);
                                ModelVAO fromData = BBSModClient.getTextures().getExtruder().get(fromTexture);

                                if (fromData != null)
                                {
                                    ModelVAORenderer.setTextureBlend(toTexture, textureBlendSnapshot.blend);

                                    try
                                    {
                                        BBSModClient.getTextures().bindTexture(fromTexture);
                                        ModelVAORenderer.render(BBSShaders.getModel(), fromData, overlayStack, cr, cg, cb, ca, overlayLight, overlayOverlay);
                                    }
                                    finally
                                    {
                                        ModelVAORenderer.clearTextureBlend();
                                    }
                                }
                            }
                            else
                            {
                                FormTextureBlendRenderer.draw(textureBlendSnapshot, texture, (link, alphaFactor) ->
                                {
                                    ModelVAO passData = BBSModClient.getTextures().getExtruder().get(link);

                                    if (passData == null)
                                    {
                                        return;
                                    }

                                    BBSModClient.getTextures().bindTexture(link);
                                    ModelVAORenderer.render(BBSShaders.getModel(), passData, overlayStack, cr, cg, cb, ca * alphaFactor, overlayLight, overlayOverlay);
                                });
                            }
                        }
                        finally
                        {
                            ModelVAORenderer.clearPaint();
                            ModelVAORenderer.clearGlowing();
                        }
                    });
                }
                else if (shaderOverlay)
                {
                    Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(matrices.peek().getPositionMatrix()));
                    Matrix3f normalMatrix = new Matrix3f(matrices.peek().getNormalMatrix());
                    float cr = color.r;
                    float cg = color.g;
                    float cb = color.b;
                    float ca = color.a;
                    int overlayLight = light;
                    int overlayOverlay = overlay;

                    ModelVAORenderer.submitPaintOverlay(false, () ->
                    {
                        ModelVAORenderer.setPaint(0F, 0F, 0F, 0F);
                        ModelVAORenderer.setGlow(glow, resolvedGlow.r, resolvedGlow.g, resolvedGlow.b, legacyGlow);

                        try
                        {
                            MatrixStack overlayStack = new MatrixStack();

                            overlayStack.peek().getPositionMatrix().set(positionMatrix);
                            overlayStack.peek().getNormalMatrix().set(normalMatrix);

                            if (useShaderBlend && textureBlendSnapshot != null)
                            {
                                Link fromTexture = FormTextureBlendRenderer.resolveFrom(textureBlendSnapshot, texture);
                                Link toTexture = FormTextureBlendRenderer.resolveTo(textureBlendSnapshot, texture);
                                ModelVAO fromData = BBSModClient.getTextures().getExtruder().get(fromTexture);

                                if (fromData != null)
                                {
                                    ModelVAORenderer.setTextureBlend(toTexture, textureBlendSnapshot.blend);

                                    try
                                    {
                                        BBSModClient.getTextures().bindTexture(fromTexture);
                                        ModelVAORenderer.render(BBSShaders.getModel(), fromData, overlayStack, cr, cg, cb, ca, overlayLight, overlayOverlay);
                                    }
                                    finally
                                    {
                                        ModelVAORenderer.clearTextureBlend();
                                    }
                                }
                            }
                            else
                            {
                                FormTextureBlendRenderer.draw(textureBlendSnapshot, texture, (link, alphaFactor) ->
                                {
                                    ModelVAO passData = BBSModClient.getTextures().getExtruder().get(link);

                                    if (passData == null)
                                    {
                                        return;
                                    }

                                    BBSModClient.getTextures().bindTexture(link);
                                    ModelVAORenderer.render(BBSShaders.getModel(), passData, overlayStack, cr, cg, cb, ca * alphaFactor, overlayLight, overlayOverlay);
                                });
                            }
                        }
                        finally
                        {
                            ModelVAORenderer.clearPaint();
                            ModelVAORenderer.clearGlowing();
                        }
                    });
                }
            }
            finally
            {
                ModelVAORenderer.clearPaint();
                ModelVAORenderer.clearGlowing();
            }

            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();

            gameRenderer.getLightmapTextureManager().disable();
            gameRenderer.getOverlayTexture().teardownOverlayColor();
        }
    }
}
