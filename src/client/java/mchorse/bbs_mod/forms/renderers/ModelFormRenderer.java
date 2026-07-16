package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.bobj.BOBJBone;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.client.ItemUseRenderState;
import mchorse.bbs_mod.client.renderer.entity.ActorEntityRenderer;
import mchorse.bbs_mod.cubic.IModel;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.animation.ActionsConfig;
import mchorse.bbs_mod.cubic.animation.Animator;
import mchorse.bbs_mod.cubic.animation.IAnimator;
import mchorse.bbs_mod.cubic.animation.ProceduralAnimator;
import mchorse.bbs_mod.cubic.constraints.JointLimitEnforcer;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.ik.LimbConstraintProcessor;
import mchorse.bbs_mod.cubic.model.ArmorSlot;
import mchorse.bbs_mod.cubic.model.ArmorType;
import mchorse.bbs_mod.cubic.model.bobj.BOBJModel;
import mchorse.bbs_mod.cubic.physics.DynamicBoneOrchestrator;
import mchorse.bbs_mod.cubic.render.ShapeKeyGlowPass;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.film.FormRenderDepth;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.ITickable;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.forms.forms.BodyPart;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.EffectTransformMath;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.forms.forms.utils.TextureBlend;
import mchorse.bbs_mod.forms.renderers.utils.FormColorBlend;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCacheEntry;
import mchorse.bbs_mod.obj.shapes.ShapeKeys;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.core.ValuePose;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.ui.framework.elements.utils.UILoader;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.interps.Lerps;
import mchorse.bbs_mod.utils.joml.Vectors;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.resources.LinkUtils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public class ModelFormRenderer extends FormRenderer<ModelForm> implements ITickable
{
    private static Matrix4f uiMatrix = new Matrix4f();

    private MatrixCache bones = new MatrixCache();

    private ActionsConfig lastConfigs;
    private IAnimator animator;
    private ModelInstance lastModel;
    private boolean ikAppliedThisRender;
    private boolean physicsAppliedThisRender;
    private boolean constraintsAppliedThisRender;

    private int lastAge = -1;

    private IEntity entity = new StubEntity();

    /* Transient additive pose applied by the film "Look at" constraint */
    private Pose lookAtPose;

    @Override
    protected void applyTransforms(MatrixStack stack, boolean origin, float transition)
    {
        super.applyTransforms(stack, origin, transition);

        ModelInstance model = this.getModel();

        if (model != null)
        {
            stack.scale(model.scale.x, model.scale.y, model.scale.z);
        }
    }

    @Override
    protected void applyTransforms(Matrix4f matrix, float transition)
    {
        super.applyTransforms(matrix, transition);

        ModelInstance model = this.getModel();

        if (model != null)
        {
            matrix.scale(model.scale.x, model.scale.y, model.scale.z);
        }
    }

    public static Matrix4f getUIMatrix(UIContext context, int x1, int y1, int x2, int y2)
    {
        float scale = (y2 - y1) / 2.5F;
        int x = x1 + (x2 - x1) / 2;
        float y = y1 + (y2 - y1) * 0.85F;
        float angle = MathUtils.toRad(context.mouseX - (x1 + x2) / 2) + MathUtils.PI;

        if (BBSSettings.freezeModels.get())
        {
            angle = -MathUtils.PI + MathUtils.PI / 8;
        }

        uiMatrix.identity();
        uiMatrix.translate(x, y, 40);
        uiMatrix.scale(scale, -scale, scale);
        uiMatrix.rotateX(MathUtils.PI / 8);
        uiMatrix.rotateY(angle);

        return uiMatrix;
    }

    public static ModelInstance getModel(ModelForm form)
    {
        return BBSModClient.getModels().getModel(form.model.get());
    }

    public static boolean isBobjModel(ModelForm form)
    {
        ModelInstance instance = getModel(form);

        return instance != null && instance.model instanceof BOBJModel;
    }

    public static boolean isBobjModel(IModel model)
    {
        return model instanceof BOBJModel;
    }

    public ModelFormRenderer(ModelForm form)
    {
        super(form);
    }

    public IAnimator getAnimator()
    {
        return this.animator;
    }

    public void invalidateCachedModel()
    {
        /* No-op: UI now uses the source model directly to avoid copy/setup GPU churn. */
    }

    public ModelInstance getModel()
    {
        return BBSModClient.getModels().getModel(this.form.model.get());
    }

    public Pose getPose()
    {
        Pose pose = this.form.pose.get().copy();
        Pose overlay = this.form.poseOverlay.get();

        ModelInstance model = this.getModel();

        if (model != null)
        {
            this.applyPose(pose, model.parts);
        }

        this.applyPose(pose, overlay);

        for (ValuePose newPose : this.form.additionalOverlays)
        {
            this.applyPose(pose, newPose.get());
        }

        if (this.lookAtPose != null)
        {
            this.applyPose(pose, this.lookAtPose);
        }

        return pose;
    }

    /**
     * Sets a transient additive pose used by the film controller's "Look at"
     * constraint (per bone lock weights). It's set right before rendering an
     * entity and cleared right after, so it never gets serialized.
     */
    public void setLookAtPose(Pose pose)
    {
        this.lookAtPose = pose;
    }

    private void applyPose(Pose targetPose, Pose pose)
    {
        for (Map.Entry<String, PoseTransform> entry : pose.transforms.entrySet())
        {
            PoseTransform poseTransform = targetPose.get(entry.getKey());
            PoseTransform value = entry.getValue();

            if (value.fix != 0)
            {
                poseTransform.fix = value.fix;
                poseTransform.translate.lerp(value.translate, value.fix);
                poseTransform.scale.lerp(value.scale, value.fix);
                poseTransform.rotate.lerp(value.rotate, value.fix);
                poseTransform.rotate2.lerp(value.rotate2, value.fix);
                poseTransform.pivot.lerp(value.pivot, value.fix);
            }
            else
            {
                poseTransform.translate.add(value.translate);
                poseTransform.scale.add(value.scale).sub(1, 1, 1);
                poseTransform.rotate.add(value.rotate);
                poseTransform.rotate2.add(value.rotate2);
                poseTransform.pivot.add(value.pivot);
            }

            if (value.fix != 0)
            {
                poseTransform.color.lerp(value.color, value.fix);
                poseTransform.paintColor.lerp(value.paintColor, value.fix);
                poseTransform.glowingColor.lerp(value.glowingColor, value.fix);
                poseTransform.glowIntensity = Lerps.lerp(poseTransform.glowIntensity, value.glowIntensity, value.fix);
                poseTransform.glowRadius = Lerps.lerp(poseTransform.glowRadius, value.glowRadius, value.fix);
                poseTransform.lighting = Lerps.lerp(poseTransform.lighting, value.lighting, value.fix);
            }
            else
            {
                poseTransform.color.mul(value.color);
                poseTransform.paintColor.lerp(value.paintColor, value.paintColor.a);
                poseTransform.glowingColor.lerp(value.glowingColor, Math.abs(value.glowIntensity));
                poseTransform.glowIntensity = Lerps.lerp(poseTransform.glowIntensity, value.glowIntensity, Math.abs(value.glowIntensity));
                poseTransform.glowRadius = Lerps.lerp(poseTransform.glowRadius, value.glowRadius, Math.abs(value.glowRadius) > 0F ? Math.abs(value.glowRadius) : 1F);
                poseTransform.lighting += value.lighting;
            }

            if (value.texture != null)
            {
                poseTransform.texture = LinkUtils.copy(value.texture);
                poseTransform.textureBlend = value.textureBlend;
            }
        }
    }

    public void resetAnimator()
    {
        this.animator = null;
        this.lastModel = null;
    }

    private void applyPBRTextureIntensity()
    {
        BBSRendering.setPBRTextureIntensity(this.form.pbrNormalIntensity.get(), this.form.pbrSpecularIntensity.get());
    }

    private void clearPBRTextureIntensity()
    {
        BBSRendering.clearPBRTextureIntensity();
    }

    public void ensureAnimator(float transition)
    {
        ModelInstance model = this.getModel();
        ActionsConfig actionsConfig = this.resolveActionsConfig(model);

        if (model == null)
        {
            return;
        }

        if (this.lastModel == model && this.animator != null)
        {
            /* Update the config */
            if (!Objects.equals(actionsConfig, this.lastConfigs))
            {
                this.animator.setup(model, actionsConfig, true);

                this.lastConfigs = new ActionsConfig();
                this.lastConfigs.copy(actionsConfig);
            }

            return;
        }

        this.animator = model.procedural ? new ProceduralAnimator() : new Animator();
        this.animator.setup(model, actionsConfig, false);

        this.lastConfigs = new ActionsConfig();
        this.lastConfigs.copy(actionsConfig);
        this.lastModel = model;
    }

    private ActionsConfig resolveActionsConfig(ModelInstance model)
    {
        ActionsConfig output = new ActionsConfig();
        ActionsConfig formActions = this.form.actions.get();

        if (formActions != null)
        {
            output.copy(formActions);
        }

        if (model == null || model.actions == null)
        {
            return output;
        }

        if (output.geckoAnimations.isDefault() && !model.actions.geckoAnimations.isDefault())
        {
            output.geckoAnimations.copy(model.actions.geckoAnimations);

            if ((output.geckoAnimationsJavascript == null || output.geckoAnimationsJavascript.isBlank()) && model.actions.geckoAnimationsJavascript != null)
            {
                output.geckoAnimationsJavascript = model.actions.geckoAnimationsJavascript;
            }
        }

        return output;
    }

    @Override
    public List<String> getBones()
    {
        ModelInstance model = this.getModel();

        return model == null ? Collections.emptyList() : new ArrayList<>(model.model.getAllGroupKeys());
    }

    @Override
    public void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        context.batcher.flush();

        this.ensureAnimator(context.getTransition());

        ModelInstance model = this.getModel();

        if (this.animator != null && model != null)
        {
            MatrixStack stack = context.batcher.getContext().getMatrices();

            stack.push();

            Matrix4f uiMatrix = getUIMatrix(context, x1, y1, x2, y2);

            this.applyTransforms(uiMatrix, context.getTransition());

            Link link = this.form.texture.get();
            Link texture = link == null ? model.texture : link;
            Color color = Color.white();

            color.mul(this.form.color.get());

            float scale = this.form.uiScale.get() * model.uiScale;

            model.model.resetPose();

            this.animator.applyActions(null, model, context.getTransition());
            model.model.applyPose(this.getPose());

            MatrixStackUtils.multiply(stack, uiMatrix);
            stack.scale(scale, scale, scale);

            this.applyPBRTextureIntensity();
            BBSModClient.getTextures().bindTexture(texture);
            this.clearPBRTextureIntensity();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);

            Vector3f light0 = new Vector3f(0.85F, 0.85F, -1F).normalize();
            Vector3f light1 = new Vector3f(-0.85F, 0.85F, 1F).normalize();
            RenderSystem.setupLevelDiffuseLighting(light0, light1);

            Supplier<ShaderProgram> mainShader = this.getModelShader(model);

            this.renderModel(this.entity, mainShader, stack, model, LightmapTextureManager.pack(15, 15), OverlayTexture.DEFAULT_UV, color, true, null, context.getTransition(), true, null, null);

            /* Render body parts */
            stack.push();
            stack.peek().getNormalMatrix().getScale(Vectors.EMPTY_3F);
            stack.peek().getNormalMatrix().scale(1F / Vectors.EMPTY_3F.x, -1F / Vectors.EMPTY_3F.y, 1F / Vectors.EMPTY_3F.z);

            this.renderBodyParts(new FormRenderingContext()
                .set(FormRenderType.ENTITY, this.entity, stack, LightmapTextureManager.pack(15, 15), OverlayTexture.DEFAULT_UV, context.getTransition())
                .inUI());

            stack.pop();
            stack.pop();

            DiffuseLighting.disableGuiDepthLighting();
            RenderSystem.depthFunc(GL11.GL_ALWAYS);
        }
        else
        {
            String modelId = this.form.model.get();
            if (modelId != null && BBSModClient.getModels().isLoading(modelId))
            {
                float cx = x1 + (x2 - x1) / 2.0F;
                float cy = y1 + (y2 - y1) / 2.0F;
                UILoader.draw(context, cx, cy, 1.25F, null);
            }
        }
    }

    private void renderModel(IEntity target, Supplier<ShaderProgram> program, MatrixStack stack, ModelInstance model, int light, int overlay, Color color, boolean ui, StencilMap stencilMap, float transition, boolean renderEquipment, MatrixStack world, FormRenderingContext renderContext)
    {
        this.ikAppliedThisRender = false;
        this.physicsAppliedThisRender = false;
        this.constraintsAppliedThisRender = false;

        if (!model.culling)
        {
            RenderSystem.disableCull();
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        GameRenderer gameRenderer = MinecraftClient.getInstance().gameRenderer;

        gameRenderer.getLightmapTextureManager().enable();
        gameRenderer.getOverlayTexture().setupOverlayColor();

        MatrixStack newStack = new MatrixStack();

        MatrixStackUtils.multiply(newStack, stack.peek().getPositionMatrix());
        newStack.peek().getNormalMatrix().set(stack.peek().getNormalMatrix());

        if (ui)
        {
            newStack.peek().getNormalMatrix().getScale(Vectors.EMPTY_3F);
            newStack.peek().getNormalMatrix().scale(1F / Vectors.EMPTY_3F.x, -1F / Vectors.EMPTY_3F.y, 1F / Vectors.EMPTY_3F.z);
        }

        Matrix4f baseTransform = ui ? null : new Matrix4f((world != null ? world : stack).peek().getPositionMatrix());

        this.ikAppliedThisRender = false;
        this.physicsAppliedThisRender = false;
        this.constraintsAppliedThisRender = false;
        this.applyIKOnce(model, baseTransform);
        this.applyPhysicsOnce(target, model, transition, baseTransform);
        this.applyConstraintsOnce(model);

        /* Pass form-level texture so VAO renderer can respect it */
        Link link = this.form.texture.get();
        Link defaultTexture = link == null ? model.texture : link;

        if (renderContext != null && renderContext.textureOverride != null)
        {
            defaultTexture = renderContext.textureOverride;
        }

        TextureBlend textureBlend = this.form.textureBlend;

        if (renderContext != null && renderContext.textureBlendOverride != null)
        {
            textureBlend = renderContext.textureBlendOverride;
        }

        this.applyPBRTextureIntensity();

        if (stencilMap != null)
        {
            try
            {
                ModelVAORenderer.clearPaint();
                ModelVAORenderer.clearGlowing();
                this.renderModelGeometry(newStack, program, model, light, overlay, stencilMap, color, defaultTexture, textureBlend);
            }
            finally
            {
                this.clearPBRTextureIntensity();
                ModelVAORenderer.clearPaint();
                ModelVAORenderer.clearGlowing();
            }

            gameRenderer.getLightmapTextureManager().disable();
            gameRenderer.getOverlayTexture().teardownOverlayColor();
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();

            if (!model.culling)
            {
                RenderSystem.enableCull();
            }

            this.captureMatrices(model);

            return;
        }

        PaintSettings paint = this.form.paintSettings.get();
        Color legacyPaint = this.form.paintColor.get();
        Color paintColor = new Color();

        paint.resolveColor(legacyPaint, paintColor);

        float paintStrength = paint.resolveIntensity(legacyPaint);

        paintColor.a = paintStrength;
        GlowSettings glow = this.form.glowSettings.get();
        Color legacyGlow = this.form.glowingColor.get();
        Color glowColor = new Color();

        glow.resolveColor(legacyGlow, glowColor);

        ModelVAORenderer.setGlow(glow, glowColor.r, glowColor.g, glowColor.b, legacyGlow);

        boolean irisWorldPaintDeferral = BBSRendering.isIrisWorldPaintDeferral();
        boolean paintActive = this.hasAnyPaint(model);
        boolean bbsModelShader = this.usesBbsModelShader(model);
        boolean hasGlow = this.hasAnyGlow(model);
        boolean syncedGlow = hasGlow && glow.resolveSync();
        boolean deferPaintToOverlay = model.supportsBbsModelShaderEffects() && paintActive && irisWorldPaintDeferral;
        boolean shaderOverlay = model.supportsBbsModelShaderEffects() && irisWorldPaintDeferral && syncedGlow && !paintActive;
        boolean deferGlowToOverlay = shaderOverlay;
        boolean paintOnlyGlow = glow.resolvePaintOnly();
        boolean shapeKeyPositiveOverlay = model.hasShapeKeys() && this.hasAnyPositiveGlow(model, glow, legacyGlow);
        boolean glowDeferredToOverlay = deferGlowToOverlay || (deferPaintToOverlay && hasGlow && !paintOnlyGlow);
        boolean stripMainPassGlow = deferGlowToOverlay || (deferPaintToOverlay && hasGlow && paintOnlyGlow);
        GlowSettings mainPassGlow = this.resolveMainPassGlow(glow, legacyGlow, stripMainPassGlow, shapeKeyPositiveOverlay);

        if (!bbsModelShader && !shaderOverlay && !deferPaintToOverlay && !paintOnlyGlow && !shapeKeyPositiveOverlay)
        {
            FormColorBlend.blendFormGlowBrighten(color, glow, legacyGlow);
        }

        if (paintActive)
        {
            float effectiveShaderShadow = paint.effectiveShaderShadow(legacyPaint);

            if (model != null && model.getModel() != null)
            {
                for (ModelGroup group : model.getModel().getAllGroups())
                {
                    if (group.paintColor.a != 0F)
                    {
                        effectiveShaderShadow = Math.min(effectiveShaderShadow, PaintSettings.resolveAutoShaderShadowForPoseAlpha(group.paintColor.a));
                    }
                }
            }

            this.form.shaderShadow.setRuntimeValue(effectiveShaderShadow > 0.001F);
        }

        Matrix4f formRootInverse = new Matrix4f(newStack.peek().getPositionMatrix()).invert();
        Vector3f paintMaskHalf = new Vector3f();

        EffectTransformMath.resolveModelMaskHalfExtents(paint.transform, paintMaskHalf);

        EffectTransform paintTransformSnapshot = paint.transform.copy();
        Matrix4f formRootInverseSnapshot = new Matrix4f(formRootInverse);
        Vector3f paintMaskHalfSnapshot = new Vector3f(paintMaskHalf);

        if (paintActive && bbsModelShader)
        {
            ModelVAORenderer.setPaintEffectTransform(formRootInverse, paint.transform, paintMaskHalf);
        }

        try
        {
            TextureBlend textureBlendSnapshot = textureBlend == null ? null : new TextureBlend(textureBlend.from, textureBlend.to, textureBlend.blend);

            if (deferPaintToOverlay)
            {
                /* Iris base pass uses the vanilla entity shader; paint is applied only in the
                 * deferred BBS model overlay so the base pass cannot leak a root-origin pixel. */
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

            if (stripMainPassGlow || shapeKeyPositiveOverlay)
            {
                ModelVAORenderer.setGlow(mainPassGlow, glowColor.r, glowColor.g, glowColor.b, legacyGlow);
            }
            else if (hasGlow)
            {
                ModelVAORenderer.setGlow(glow, glowColor.r, glowColor.g, glowColor.b, legacyGlow);
            }

            this.renderModelGeometryWithEmission(newStack, program, model, light, overlay, stencilMap, color, defaultTexture, textureBlend, glow, glowColor, legacyGlow, paintColor, glowDeferredToOverlay);

            if (deferPaintToOverlay)
            {
                Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(newStack.peek().getPositionMatrix()));
                Matrix3f normalMatrix = new Matrix3f(newStack.peek().getNormalMatrix());
                Matrix4f baseTransformSnapshot = baseTransform == null ? null : new Matrix4f(baseTransform);
                Color colorSnapshot = color.copy();
                Color paintSnapshot = paintColor.copy();
                Pose poseSnapshot = this.getPose().copy();
                float transitionSnapshot = transition;
                int overlayLight = light;
                int overlayOverlay = overlay;
                Link defaultTextureSnapshot = defaultTexture;
                TextureBlend textureBlendSnapshotFinal = textureBlendSnapshot;
                boolean hasGlowSnapshot = hasGlow;
                boolean paintOnlyGlowSnapshot = paintOnlyGlow;

                ModelVAORenderer.submitPaintOverlay(false, () ->
                {
                    this.applyOverlayPosePipeline(target, model, transitionSnapshot, poseSnapshot, baseTransformSnapshot);

                    try
                    {
                        ModelVAORenderer.setPaintEffectTransform(new Matrix4f().identity(), paintTransformSnapshot, paintMaskHalfSnapshot);

                        MatrixStack overlayStack = new MatrixStack();

                        overlayStack.peek().getPositionMatrix().set(positionMatrix);
                        overlayStack.peek().getNormalMatrix().set(normalMatrix);

                        ModelVAORenderer.setPaint(paintSnapshot.r, paintSnapshot.g, paintSnapshot.b, paintSnapshot.a);

                        if (hasGlowSnapshot)
                        {
                            ModelVAORenderer.setGlow(glow, glowColor.r, glowColor.g, glowColor.b, legacyGlow);
                        }
                        else
                        {
                            GlowSettings glowOff = glow.copy();

                            glowOff.intensity = 0F;
                            ModelVAORenderer.setGlow(glowOff, glowColor.r, glowColor.g, glowColor.b, legacyGlow);
                        }

                        this.renderModelGeometry(overlayStack, BBSShaders::getModel, model, overlayLight, overlayOverlay, stencilMap, colorSnapshot, defaultTextureSnapshot, textureBlendSnapshotFinal);
                    }
                    finally
                    {
                        ModelVAORenderer.clearPaintEffectTransform();
                        ModelVAORenderer.clearPaint();
                        ModelVAORenderer.clearGlowing();
                    }
                });

                if (hasGlowSnapshot && !paintOnlyGlowSnapshot)
                {
                    ModelVAORenderer.submitPaintOverlay(false, () ->
                    {
                        this.applyOverlayPosePipeline(target, model, transitionSnapshot, poseSnapshot, baseTransformSnapshot);

                        try
                        {
                            MatrixStack overlayStack = new MatrixStack();

                            overlayStack.peek().getPositionMatrix().set(positionMatrix);
                            overlayStack.peek().getNormalMatrix().set(normalMatrix);

                            this.renderDeferredGlowEmission(overlayStack, model, overlayLight, overlayOverlay, stencilMap, colorSnapshot, defaultTextureSnapshot, textureBlendSnapshotFinal, glow, glowColor, legacyGlow);
                        }
                        finally
                        {
                            ModelVAORenderer.clearPaint();
                            ModelVAORenderer.clearGlowing();
                        }
                    });
                }
            }
            else if (shaderOverlay)
            {
                Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(newStack.peek().getPositionMatrix()));
                Matrix3f normalMatrix = new Matrix3f(newStack.peek().getNormalMatrix());
                Matrix4f baseTransformSnapshot = baseTransform == null ? null : new Matrix4f(baseTransform);
                Color colorSnapshot = color.copy();
                Pose poseSnapshot = this.getPose().copy();
                float transitionSnapshot = transition;
                int overlayLight = light;
                int overlayOverlay = overlay;
                boolean applyPoseSnapshot = syncedGlow;
                Link defaultTextureSnapshot = defaultTexture;

                ModelVAORenderer.submitPaintOverlay(false, () ->
                {
                    if (applyPoseSnapshot)
                    {
                        this.applyOverlayPosePipeline(target, model, transitionSnapshot, poseSnapshot, baseTransformSnapshot);
                    }

                    try
                    {
                        MatrixStack overlayStack = new MatrixStack();

                        overlayStack.peek().getPositionMatrix().set(positionMatrix);
                        overlayStack.peek().getNormalMatrix().set(normalMatrix);

                        this.renderDeferredGlowEmission(overlayStack, model, overlayLight, overlayOverlay, stencilMap, colorSnapshot, defaultTextureSnapshot, textureBlendSnapshot, glow, glowColor, legacyGlow);
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
            this.clearPBRTextureIntensity();
            ModelVAORenderer.clearPaintEffectTransform();
            ModelVAORenderer.clearPaint();
            ModelVAORenderer.clearGlowing();
        }

        gameRenderer.getLightmapTextureManager().disable();
        gameRenderer.getOverlayTexture().teardownOverlayColor();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();

        if (!model.culling)
        {
            RenderSystem.enableCull();
        }

        /* Render items */
        this.captureMatrices(model);

        if (stencilMap == null && renderEquipment)
        {
            this.renderItems(target, model, stack, EquipmentSlot.MAINHAND, ModelTransformationMode.THIRD_PERSON_RIGHT_HAND, model.itemsMain, model.itemsMainTransform, color, overlay, light);
            this.renderItems(target, model, stack, EquipmentSlot.OFFHAND, ModelTransformationMode.THIRD_PERSON_LEFT_HAND, model.itemsOff, model.itemsOffTransform, color, overlay, light);

            for (Map.Entry<ArmorType, ArmorSlot> entry : model.armorSlots.entrySet())
            {
                this.renderArmor(target, stack, entry.getKey(), entry.getValue(), color, overlay, light);
            }

            this.resetPostEquipmentRenderState();
        }
    }

    private void applyIKOnce(ModelInstance model, Matrix4f baseTransform)
    {
        if (this.ikAppliedThisRender)
        {
            return;
        }

        this.ikAppliedThisRender = true;
        model.form = this.form;

        boolean hasOverrides = baseTransform != null && this.form != null
            && (!this.form.ikTargetOverrides.isEmpty()
                || !this.form.poleTargetOverrides.isEmpty()
                || !this.form.ikTipRotationOverrides.isEmpty()
                || !this.form.limbParamOverrides.isEmpty());

        if (!hasOverrides)
        {
            LimbConstraintProcessor.process(model, null, null);
            return;
        }

        Matrix4f inv = new Matrix4f(baseTransform).invert();
        Map<String, Vector3f> local = toModelSpace(this.form.ikTargetOverrides, inv);
        Map<String, Vector3f> poleLocal = toModelSpace(this.form.poleTargetOverrides, inv);
        Map<String, Quaternionf> tipLocal = toModelSpaceRotation(this.form.ikTipRotationOverrides, inv);

        if (local.isEmpty() && poleLocal.isEmpty() && tipLocal.isEmpty() && this.form.limbParamOverrides.isEmpty())
        {
            LimbConstraintProcessor.process(model, null, null);
            return;
        }

        LimbConstraintProcessor.process(
            model,
            local.isEmpty() ? null : local,
            poleLocal.isEmpty() ? null : poleLocal,
            tipLocal.isEmpty() ? null : tipLocal,
            null
        );
    }

    private static Map<String, Quaternionf> toModelSpaceRotation(Map<String, Quaternionf> world, Matrix4f inv)
    {
        Map<String, Quaternionf> local = new HashMap<>(world.size() * 2);
        Quaternionf invRot = inv.getNormalizedRotation(new Quaternionf());

        for (Map.Entry<String, Quaternionf> entry : world.entrySet())
        {
            String key = entry.getKey();
            Quaternionf worldRot = entry.getValue();

            if (key == null || key.isEmpty() || worldRot == null)
            {
                continue;
            }

            local.put(key, invRot.mul(worldRot, new Quaternionf()));
        }

        return local;
    }

    /** World-space target overrides into the model's local space (the space the solver and pivot frames use). */
    private static Map<String, Vector3f> toModelSpace(Map<String, Vector3f> world, Matrix4f inv)
    {
        Map<String, Vector3f> local = new HashMap<>(world.size() * 2);

        for (Map.Entry<String, Vector3f> entry : world.entrySet())
        {
            String key = entry.getKey();
            Vector3f worldPos = entry.getValue();

            if (key == null || key.isEmpty() || worldPos == null)
            {
                continue;
            }

            Vector3f pos = new Vector3f(worldPos);
            inv.transformPosition(pos);
            local.put(key, pos);
        }

        return local;
    }

    private void applyPhysicsOnce(IEntity target, ModelInstance model, float transition, Matrix4f baseTransform)
    {
        if (this.physicsAppliedThisRender)
        {
            return;
        }

        this.physicsAppliedThisRender = true;
        model.lastBaseTransform = baseTransform;
        model.form = this.form;
        DynamicBoneOrchestrator.apply(target, model, transition, baseTransform);
    }

    private void applyConstraintsOnce(ModelInstance model)
    {
        if (this.constraintsAppliedThisRender)
        {
            return;
        }

        this.constraintsAppliedThisRender = true;
        JointLimitEnforcer.enforce(model);
    }

    /**
     * Replays the same pose pipeline used before the base pass so deferred Iris paint/glow
     * overlays match animated bones when the shared model instance was touched by other draws.
     */
    private void applyAnimatedPoseForOverlay(IEntity target, ModelInstance model, float transition, Pose poseSnapshot)
    {
        if (this.animator == null || model == null)
        {
            return;
        }

        model.model.resetPose();
        this.animator.applyActions(target, model, transition);
        model.model.applyPose(poseSnapshot);
    }

    private void applyOverlayPosePipeline(IEntity target, ModelInstance model, float transition, Pose poseSnapshot, Matrix4f baseTransform)
    {
        this.applyAnimatedPoseForOverlay(target, model, transition, poseSnapshot);

        this.ikAppliedThisRender = false;
        this.physicsAppliedThisRender = false;
        this.constraintsAppliedThisRender = false;
        this.applyIKOnce(model, baseTransform);
        this.applyPhysicsOnce(target, model, transition, baseTransform);
        this.applyConstraintsOnce(model);
    }

    private void renderDeferredGlowEmission(MatrixStack stack, ModelInstance model, int light, int overlay, StencilMap stencilMap, Color color, Link defaultTexture, TextureBlend textureBlend, GlowSettings glow, Color glowColor, Color legacyGlow)
    {
        if (model.hasShapeKeys() && this.hasAnyPositiveGlow(model, glow, legacyGlow))
        {
            this.renderShapeKeyGlowOverlay(stack, model, overlay, stencilMap, color, defaultTexture, textureBlend, glow, legacyGlow);

            return;
        }

        ModelVAORenderer.runWithPaintOverlayPass(false, () ->
        {
            ModelVAORenderer.setPaint(0F, 0F, 0F, 0F);
            ModelVAORenderer.setGlow(glow, glowColor.r, glowColor.g, glowColor.b, legacyGlow);

            Color emission = color.copy();

            emission.r = 0F;
            emission.g = 0F;
            emission.b = 0F;

            boolean savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);

            RenderSystem.enableBlend();
            RenderSystem.depthMask(false);
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

            try
            {
                this.renderModelGeometry(stack, BBSShaders::getModel, model, LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay, stencilMap, emission, defaultTexture, textureBlend);
            }
            finally
            {
                RenderSystem.depthMask(savedDepthMask);
                RenderSystem.defaultBlendFunc();
            }
        });
    }

    private void renderModelGeometryWithEmission(MatrixStack stack, Supplier<ShaderProgram> program, ModelInstance model, int light, int overlay, StencilMap stencilMap, Color color, Link defaultTexture, TextureBlend textureBlend, GlowSettings glow, Color glowColor, Color legacyGlow, Color paint, boolean glowDeferredToOverlay)
    {
        boolean shapeKeyGlowOverlay = ShapeKeyGlowPass.shouldUseGlowOverlay(model, this.hasAnyPositiveGlow(model, glow, legacyGlow), glowDeferredToOverlay);

        try
        {
            ModelVAORenderer.setSuppressShapeKeyMainPassGlow(shapeKeyGlowOverlay);

            if (shapeKeyGlowOverlay)
            {
                this.renderModelGeometry(stack, program, model, light, overlay, stencilMap, color, defaultTexture, textureBlend);
                this.renderShapeKeyGlowOverlay(stack, model, overlay, stencilMap, color, defaultTexture, textureBlend, glow, legacyGlow);

                return;
            }

            this.renderModelGeometry(stack, program, model, light, overlay, stencilMap, color, defaultTexture, textureBlend);
        }
        finally
        {
            ModelVAORenderer.setSuppressShapeKeyMainPassGlow(false);
        }
    }

    private void renderShapeKeyGlowOverlay(MatrixStack stack, ModelInstance model, int overlay, StencilMap stencilMap, Color color, Link defaultTexture, TextureBlend textureBlend, GlowSettings glow, Color legacyGlow)
    {
        boolean formPositive = FormColorBlend.hasPositiveGlow(glow, legacyGlow);
        boolean bonePositive = this.hasBonePositiveGlow(model);

        if (!formPositive && !bonePositive)
        {
            return;
        }

        ShapeKeys shapeKeys = this.form.shapeKeys.get();

        if (formPositive)
        {
            float formIntensity = glow.resolveIntensity(legacyGlow);

            ShapeKeyGlowPass.renderOverlay(glow, legacyGlow, color.a, formIntensity, (glowLayerColor) ->
            {
                this.drawShapeKeyGlowOverlayLayer(stack, model, overlay, stencilMap, shapeKeys, defaultTexture, textureBlend, glowLayerColor, false, formIntensity, null, bonePositive);
            });
        }

        if (bonePositive && model.getModel() != null)
        {
            for (ModelGroup group : model.getModel().getAllGroups())
            {
                if (group.glowIntensity <= 0F)
                {
                    continue;
                }

                float boneIntensity = group.glowIntensity;

                ShapeKeyGlowPass.renderOverlay(glow, legacyGlow, color.a, boneIntensity, (glowLayerColor) ->
                {
                    this.drawShapeKeyGlowOverlayLayer(stack, model, overlay, stencilMap, shapeKeys, defaultTexture, textureBlend, glowLayerColor, true, boneIntensity, group.id, false);
                });
            }
        }
    }

    private void drawShapeKeyGlowOverlayLayer(MatrixStack stack, ModelInstance model, int overlay, StencilMap stencilMap, ShapeKeys shapeKeys, Link defaultTexture, TextureBlend textureBlend, Color glowLayerColor, boolean boneGlowOnly, float overlayIntensity, String targetGroupId, boolean skipBoneGlowGroups)
    {
        Link drawTexture = defaultTexture;

        if (textureBlend != null)
        {
            float blend = textureBlend.blend;
            Link fromTexture = textureBlend.from == null ? defaultTexture : textureBlend.from;
            Link toTexture = textureBlend.to == null ? defaultTexture : textureBlend.to;

            if (blend <= 0F)
            {
                drawTexture = fromTexture;
            }
            else if (blend >= 1F)
            {
                drawTexture = toTexture;
            }
            else
            {
                drawTexture = fromTexture;
            }
        }

        model.renderShapeKeyGlowOverlay(stack, glowLayerColor, overlay, stencilMap, shapeKeys, drawTexture, boneGlowOnly, overlayIntensity, targetGroupId, skipBoneGlowGroups);
    }

    private boolean hasBonePositiveGlow(ModelInstance model)
    {
        if (model == null || model.getModel() == null)
        {
            return false;
        }

        for (ModelGroup group : model.getModel().getAllGroups())
        {
            if (group.glowIntensity > 0F)
            {
                return true;
            }
        }

        return false;
    }

    private GlowSettings resolveMainPassGlow(GlowSettings glow, Color legacyGlow, boolean stripDeferredGlow, boolean shapeKeyPositiveOverlay)
    {
        GlowSettings result = glow.copy();
        float intensity = result.resolveIntensity(legacyGlow);

        if (stripDeferredGlow)
        {
            result.intensity = 0F;

            return result;
        }

        if (shapeKeyPositiveOverlay && intensity > 0F)
        {
            result.intensity = 0F;
        }

        return result;
    }

    private boolean hasAnyPositiveGlow(ModelInstance model, GlowSettings glow, Color legacyGlow)
    {
        if (FormColorBlend.hasPositiveGlow(glow, legacyGlow))
        {
            return true;
        }

        if (model != null && model.getModel() != null)
        {
            for (ModelGroup group : model.getModel().getAllGroups())
            {
                if (group.glowIntensity > 0F)
                {
                    return true;
                }
            }
        }

        return false;
    }

    private void renderModelGeometry(MatrixStack stack, Supplier<ShaderProgram> program, ModelInstance model, int light, int overlay, StencilMap stencilMap, Color color, Link defaultTexture, TextureBlend textureBlend)
    {
        ShapeKeys shapeKeys = this.form.shapeKeys.get();

        if (textureBlend == null)
        {
            ModelVAORenderer.clearTextureBlend();
            model.render(stack, program, color, light, overlay, stencilMap, shapeKeys, defaultTexture);

            return;
        }

        float blend = textureBlend.blend;
        Link fromTexture = textureBlend.from == null ? defaultTexture : textureBlend.from;
        Link toTexture = textureBlend.to == null ? defaultTexture : textureBlend.to;

        if (blend <= 0F)
        {
            ModelVAORenderer.clearTextureBlend();
            model.render(stack, program, color, light, overlay, stencilMap, shapeKeys, fromTexture);
        }
        else if (blend >= 1F)
        {
            ModelVAORenderer.clearTextureBlend();
            model.render(stack, program, color, light, overlay, stencilMap, shapeKeys, toTexture);
        }
        else if (model.supportsBbsModelShaderEffects() && (program.get() == BBSShaders.getModel() || ModelVAORenderer.isPaintOverlayPass()))
        {
            /* Single-pass shader blend: per-pixel alpha crossfade avoids two-pass holes when both skins are opaque.
             * Iris world pass uses vanilla two-pass below; BBS blend is allowed during paint overlay redraws. */
            Supplier<ShaderProgram> blendProgram = BBSShaders::getModel;

            ModelVAORenderer.setTextureBlend(toTexture, blend);

            try
            {
                RenderSystem.setShader(blendProgram);
                model.render(stack, blendProgram, color, light, overlay, stencilMap, shapeKeys, fromTexture);
            }
            finally
            {
                ModelVAORenderer.clearTextureBlend();
            }
        }
        else
        {
            /* Iris VAO + cube mesh: vanilla shader has no texture-blend uniforms. */
            ModelVAORenderer.clearTextureBlend();

            Color colorFrom = color.copy();

            colorFrom.a *= 1F - blend;
            model.render(stack, program, colorFrom, light, overlay, stencilMap, shapeKeys, fromTexture);

            Color colorTo = color.copy();

            colorTo.a *= blend;
            model.render(stack, program, colorTo, light, overlay, stencilMap, shapeKeys, toTexture);
        }
    }

    private Supplier<ShaderProgram> getModelShader(ModelInstance model)
    {
        if (!model.supportsBbsModelShaderEffects())
        {
            return GameRenderer::getRenderTypeEntityTranslucentCullProgram;
        }

        if (this.hasAnyBoneTextureBlend(model))
        {
            return BBSShaders::getModel;
        }

        if (BBSRendering.isIrisWorldModelPass())
        {
            return GameRenderer::getRenderTypeEntityTranslucentCullProgram;
        }

        return BBSShaders::getModel;
    }

    private boolean hasAnyBoneTextureBlend(ModelInstance model)
    {
        if (model == null || model.model == null)
        {
            return false;
        }

        if (model.model instanceof Model cubic)
        {
            for (ModelGroup group : cubic.getAllGroups())
            {
                if (group.textureOverride != null && group.textureBlend > 0F && group.textureBlend < 1F)
                {
                    return true;
                }
            }
        }
        else if (model.model instanceof BOBJModel bobj)
        {
            for (BOBJBone bone : bobj.getArmature().orderedBones)
            {
                if (bone.texture != null && bone.textureBlend > 0F && bone.textureBlend < 1F)
                {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean usesBbsModelShader(ModelInstance model)
    {
        if (model == null || !model.supportsBbsModelShaderEffects())
        {
            return false;
        }

        return !BBSRendering.isIrisWorldModelPass();
    }

    /**
     * Whether glow intensity is active on the whole form or any bone.
     */
    private boolean hasAnyGlow(ModelInstance model)
    {
        Color legacyGlow = this.form.glowingColor.get();

        if (this.form.glowSettings.get().resolveIntensity(legacyGlow) != 0F)
        {
            return true;
        }

        if (model != null && model.getModel() != null)
        {
            for (ModelGroup group : model.getModel().getAllGroups())
            {
                if (group.glowIntensity != 0F)
                {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Whether the whole-form paint or any bone (model group) paint is currently active, which decides
     * if a deferred paint overlay pass should run after Iris world rendering.
     */
    private boolean hasAnyPaint(ModelInstance model)
    {
        PaintSettings paint = this.form.paintSettings.get();
        Color legacyPaint = this.form.paintColor.get();

        if (paint.resolveIntensity(legacyPaint) != 0F)
        {
            return true;
        }

        if (model != null && model.getModel() != null)
        {
            for (ModelGroup group : model.getModel().getAllGroups())
            {
                if (group.paintColor != null && group.paintColor.a != 0F)
                {
                    return true;
                }
            }
        }

        return false;
    }

    private void resetPostEquipmentRenderState()
    {
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    private void renderArmor(IEntity target, MatrixStack stack, ArmorType type, ArmorSlot armorSlot, Color color, int overlay, int light)
    {
        Matrix4f matrix = this.bones.get(armorSlot.group.get()).matrix();

        if (matrix != null)
        {
            CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();

            stack.push();
            MatrixStackUtils.multiply(stack, matrix);
            MatrixStackUtils.applyTransform(stack, armorSlot.transform);
            stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180F));

            CustomVertexConsumerProvider.hijackVertexFormat((l) -> RenderSystem.enableBlend());

            ActorEntityRenderer.armorRenderer.renderArmorSlot(stack, consumers, target, type.slot, type, light);
            consumers.draw();

            CustomVertexConsumerProvider.clearRunnables();

            stack.pop();

            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
        }
    }

    private void renderItems(IEntity target, ModelInstance model, MatrixStack stack, EquipmentSlot slot, ModelTransformationMode mode, List<ArmorSlot> items, ArmorSlot globalTransform, Color color, int overlay, int light)
    {
        ItemStack itemStack = target.getEquipmentStack(slot);

        if (itemStack != null && itemStack.isEmpty())
        {
            return;
        }

        for (ArmorSlot armorSlot : items)
        {
            Matrix4f matrix = this.bones.get(armorSlot.group.get()).matrix();

            if (matrix != null)
            {
                CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();

                stack.push();
                MatrixStackUtils.multiply(stack, matrix);
                stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90F));
                stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180F));
                stack.translate(0F, 0.125F, 0F);

                if (globalTransform != null)
                {
                    MatrixStackUtils.applyTransform(stack, globalTransform.transform);
                }

                MatrixStackUtils.applyTransform(stack, armorSlot.transform);

                Hand activeHand = target.getActiveHand();
                EquipmentSlot activeSlot = activeHand == Hand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
                LivingEntity itemEntity = slot == activeSlot
                    ? ItemUseRenderState.prepareProxy(target.getWorld(), target, slot, itemStack)
                    : null;

                CustomVertexConsumerProvider.hijackVertexFormat((l) -> RenderSystem.enableBlend());

                consumers.setSubstitute(BBSRendering.getColorConsumer(color));

                /* For some reason, due to Sodium and my color consumer, in some cases items like Trident,
                 * shield, etc. not get rendered, but if in another arm there is another item, it does render...
                 * So, I render a 0 size oak button to circumvent that bug! */
                if (model.model instanceof BOBJModel)
                {
                    stack.push();
                    stack.scale(0F, 0F, 0F);
                    MinecraftClient.getInstance().getItemRenderer().renderItem(null, new ItemStack(Items.OAK_BUTTON), mode, mode == ModelTransformationMode.THIRD_PERSON_LEFT_HAND, stack, consumers, target.getWorld(), light, overlay, 0);
                    consumers.draw();
                    stack.pop();
                }

                MinecraftClient.getInstance().getItemRenderer().renderItem(itemEntity, itemStack, mode, mode == ModelTransformationMode.THIRD_PERSON_LEFT_HAND, stack, consumers, target.getWorld(), light, overlay, 0);
                consumers.draw();
                consumers.setSubstitute(null);

                CustomVertexConsumerProvider.clearRunnables();

                stack.pop();

                RenderSystem.enableDepthTest();
            }
        }
    }

    @Override
    public boolean renderArm(MatrixStack matrices, int light, AbstractClientPlayerEntity player, Hand hand)
    {
        this.ensureAnimator(MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(true));
        ModelInstance model = this.getModel();

        if (this.animator != null && model != null)
        {
            ArmorSlot slot = hand == Hand.MAIN_HAND ? model.fpMain : model.fpOffhand;

            if (slot == null)
            {
                return false;
            }

            Link link = this.form.texture.get();
            Link texture = link == null ? model.texture : link;
            Color color = Color.white();

            color.mul(this.form.color.get());

            for (ModelGroup group : model.getModel().getAllGroups())
            {
                ModelGroup g = group;
                boolean visible = false;

                while (g != null)
                {
                    if (g.id.equals(slot.group.get()))
                    {
                        visible = true;

                        break;
                    }

                    g = g.parent;
                }

                group.visible = visible;
            }

            model.model.resetPose();

            matrices.push();
            matrices.multiply(RotationAxis.POSITIVE_Y.rotation(MathUtils.PI));
            MatrixStackUtils.applyTransform(matrices, slot.transform);

            this.applyPBRTextureIntensity();
            BBSModClient.getTextures().bindTexture(texture);
            this.clearPBRTextureIntensity();

            Supplier<ShaderProgram> mainShader = this.getModelShader(model);

            RenderSystem.enableDepthTest();
            RenderSystem.enableBlend();

            this.renderModel(this.entity, mainShader, matrices, model, light, OverlayTexture.DEFAULT_UV, color, false, null, 0F, true, null, null);

            for (ModelGroup group : model.getModel().getAllGroups())
            {
                group.visible = true;
            }

            matrices.pop();

            return true;
        }

        return super.renderArm(matrices, light, player, hand);
    }

    @Override
    public void render3D(FormRenderingContext context)
    {
        this.ensureAnimator(context.getTransition());

        ModelInstance model = this.getModel();

        if (this.animator != null && model != null)
        {
            Link link = this.form.texture.get();
            Link texture = link == null ? model.texture : link;

            if (context.textureOverride != null)
            {
                texture = context.textureOverride;
            }

            Color color = new Color().set(context.color, true);

            color.mul(this.form.color.get());
            model.model.resetPose();

            this.animator.applyActions(context.entity, model, context.getTransition());
            model.model.applyPose(this.getPose());

            context.stack.multiply(RotationAxis.POSITIVE_Y.rotation(MathUtils.PI));
            if (context.world != null)
            {
                context.world.multiply(RotationAxis.POSITIVE_Y.rotation(MathUtils.PI));
            }

            if (texture != null)
            {
                this.applyPBRTextureIntensity();
                BBSModClient.getTextures().bindTexture(texture);
                this.clearPBRTextureIntensity();
            }

            Supplier<ShaderProgram> mainShader = this.getModelShader(model);
            Supplier<ShaderProgram> shader = this.getShader(context, mainShader, BBSShaders::getPickerModelsProgram);
            boolean deferParentMesh = context.renderDepthFrame != null && !this.form.parts.getAllTyped().isEmpty();

            if (deferParentMesh)
            {
                this.captureMatrices(model);

                return;
            }

            this.renderModel(context.entity, shader, context.stack, model, context.light, context.overlay, color, false, context.stencilMap, context.getTransition(), context.renderEquipment, context.world, context);
        }
    }

    private void renderParentMesh(FormRenderingContext context)
    {
        ModelInstance model = this.getModel();

        if (this.animator == null || model == null)
        {
            return;
        }

        Link link = this.form.texture.get();
        Link texture = link == null ? model.texture : link;

        if (context.textureOverride != null)
        {
            texture = context.textureOverride;
        }

        Color color = new Color().set(context.color, true);

        color.mul(this.form.color.get());

        if (texture != null)
        {
            this.applyPBRTextureIntensity();
            BBSModClient.getTextures().bindTexture(texture);
            this.clearPBRTextureIntensity();
        }

        Supplier<ShaderProgram> mainShader = this.getModelShader(model);
        Supplier<ShaderProgram> shader = this.getShader(context, mainShader, BBSShaders::getPickerModelsProgram);

        this.renderModel(context.entity, shader, context.stack, model, context.light, context.overlay, color, false, context.stencilMap, context.getTransition(), context.renderEquipment, context.world, context);
    }

    @Override
    protected void updateStencilMap(FormRenderingContext context)
    {
        ModelInstance model = this.getModel();

        if (model == null || model.model == null || context.stencilMap == null)
        {
            return;
        }

        model.fillStencilMap(context.stencilMap, this.form);
    }

    private void captureMatrices(ModelInstance model)
    {
        /* this.bones.clear()? */
        model.captureMatrices(this.bones);
    }

    @Override
    public void renderBodyParts(FormRenderingContext context)
    {
        List<BodyPart> parts = this.getSortedBodyParts(context);

        if (parts.isEmpty())
        {
            return;
        }

        context.stack.push();
        if (context.world != null)
        {
            context.world.push();
        }

        if (context.renderDepthFrame != null)
        {
            this.renderDepthSortedBodyParts(context, parts);
        }
        else
        {
            this.renderBodyPartLayers(context, parts);
        }

        this.bones.clear();
        context.stack.pop();
        if (context.world != null)
        {
            context.world.pop();
        }
    }

    private void renderDepthSortedBodyParts(FormRenderingContext context, List<BodyPart> parts)
    {
        Form sourceRoot = context.renderDepthFrame.sourceRootForm;
        List<DepthLayer> layers = new ArrayList<>();
        Double parentDepth = FormRenderDepth.getEnabledDepth(this.form, FormRenderDepth.getSourceForm(sourceRoot, this.form));

        layers.add(new DepthLayer(parentDepth == null ? 0D : parentDepth, null));

        for (BodyPart part : parts)
        {
            Form child = part.getForm();
            Double depth = child == null ? 0D : FormRenderDepth.getEnabledDepth(child, FormRenderDepth.getSourceForm(sourceRoot, child));

            layers.add(new DepthLayer(depth == null ? 0D : depth, part));
        }

        layers.sort(Comparator.comparingDouble(layer -> layer.depth));

        for (DepthLayer layer : layers)
        {
            if (layer.part == null)
            {
                this.renderParentMesh(context);
            }
            else
            {
                this.renderBodyPartLayer(context, layer.part);
            }
        }
    }

    private void renderBodyPartLayers(FormRenderingContext context, List<BodyPart> parts)
    {
        for (BodyPart part : parts)
        {
            this.renderBodyPartLayer(context, part);
        }
    }

    private void renderBodyPartLayer(FormRenderingContext context, BodyPart part)
    {
        Matrix4f matrix = this.bones.get(part.bone.get()).matrix();

        context.stack.push();
        if (context.world != null)
        {
            context.world.push();
        }

        if (matrix != null)
        {
            MatrixStackUtils.multiply(context.stack, matrix);
            if (context.world != null)
            {
                MatrixStackUtils.multiply(context.world, matrix);
            }
        }
        else
        {
            context.stack.multiply(RotationAxis.POSITIVE_Y.rotation(MathUtils.PI));
            if (context.world != null)
            {
                context.world.multiply(RotationAxis.POSITIVE_Y.rotation(MathUtils.PI));
            }
        }

        this.renderBodyPart(part, context);

        context.stack.pop();
        if (context.world != null)
        {
            context.world.pop();
        }
    }

    private static final class DepthLayer
    {
        private final double depth;
        private final BodyPart part;

        private DepthLayer(double depth, BodyPart part)
        {
            this.depth = depth;
            this.part = part;
        }
    }

    @Override
    public void collectMatrices(IEntity entity, MatrixStack stack, MatrixCache matrices, String prefix, float transition)
    {
        ModelInstance model = this.getModel();
        Matrix4f mm = new Matrix4f();
        Matrix4f oo = new Matrix4f();

        stack.push();
        this.applyTransforms(stack, true, transition);
        oo.set(stack.peek().getPositionMatrix());
        stack.pop();

        stack.push();
        this.applyTransforms(stack, false, transition);
        mm.set(stack.peek().getPositionMatrix());

        matrices.put(prefix, mm, oo);

        /* Collect bones and add them to matrix list */
        if (this.animator != null && model != null)
        {
            model.model.resetPose();

            this.animator.applyActions(entity, model, transition);
            model.model.applyPose(this.getPose());

            stack.multiply(RotationAxis.POSITIVE_Y.rotation(MathUtils.PI));
            this.captureMatrices(model);
        }

        for (Map.Entry<String, MatrixCacheEntry> entry : this.bones.entrySet())
        {
            Matrix4f matrix = new Matrix4f();
            Matrix4f o = new Matrix4f();

            stack.push();
            MatrixStackUtils.multiply(stack, entry.getValue().matrix());
            matrix.set(stack.peek().getPositionMatrix());
            stack.pop();

            stack.push();
            MatrixStackUtils.multiply(stack, entry.getValue().origin());
            o.set(stack.peek().getPositionMatrix());
            stack.pop();

            matrices.put(StringUtils.combinePaths(prefix, entry.getKey()), matrix, o);
        }

        int i = 0;

        /* Recursively do the same thing with body parts */
        for (BodyPart part : this.form.parts.getAllTyped())
        {
            Form form = part.getForm();

            if (form != null)
            {
                Matrix4f matrix = this.bones.get(part.bone.get()).matrix();

                stack.push();

                if (matrix != null)
                {
                    MatrixStackUtils.multiply(stack, matrix);
                }
                else
                {
                    stack.multiply(RotationAxis.POSITIVE_Y.rotation(MathUtils.PI));
                }

                MatrixStackUtils.applyTransform(stack, part.transform.get());

                FormUtilsClient.getRenderer(form).collectMatrices(part.useTarget.get() ? entity : part.getEntity(), stack, matrices, StringUtils.combinePaths(prefix, String.valueOf(i)), transition);

                stack.pop();
            }

            i += 1;
        }

        stack.pop();

        this.bones.clear();
    }

    @Override
    public void tick(IEntity entity)
    {
        int age = entity.getAge();

        if (this.lastAge != -1 && age != this.lastAge + 1)
        {
            this.resetAnimator();
        }

        this.ensureAnimator(0F);

        if (this.animator != null)
        {
            this.animator.update(entity);
        }

        this.lastAge = age;
    }
}
