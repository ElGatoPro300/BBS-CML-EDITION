package mchorse.bbs_mod.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.opengl.GlStateManager;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.blocks.entities.ModelProperties;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.entity.ActorEntity;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.MobForm;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.mixin.client.EntityRendererDispatcherInvoker;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.model_blocks.UIModelBlockPanel;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class ModelBlockEntityRenderer
{
    private static ActorEntity entity;

    public static void renderShadow(MultiBufferSource provider, PoseStack matrices, float tickDelta, double x, double y, double z, float tx, float ty, float tz)
    {
        renderShadow(provider, matrices, tickDelta, x, y, z, tx, ty, tz, 0.5F, 1F);
    }

    public static void renderShadow(MultiBufferSource provider, PoseStack matrices, float tickDelta, double x, double y, double z, float tx, float ty, float tz, float radius, float opacity)
    {
        ClientLevel world = Minecraft.getInstance().level;

        if (entity == null || entity.getEntityWorld() != world)
        {
            entity = new ActorEntity(BBSMod.ACTOR_ENTITY, world);
        }

        entity.setPos(x, y, z);
        entity.lastRenderX = x;
        entity.lastRenderY = y;
        entity.lastRenderZ = z;

        double distance = Minecraft.getInstance().getEntityRenderDispatcher().distanceToSqr(entity);

        opacity = (float) ((1D - distance / 256D) * opacity);

        matrices.pushPose();
        matrices.translate(tx, ty, tz);

        /* EntityRendererDispatcherInvoker.bbs$renderShadow(matrices, provider, entity, opacity, tickDelta, entity.getWorld(), radius); */

        matrices.popPose();
    }

    private static float getHeadYaw(float constraint, float yawDelta, float travel)
    {
        float headLimit = (float) Math.toRadians(constraint);
        float headYawBase = MathUtils.clamp(yawDelta, -headLimit, headLimit);

        float syncStart = (float) Math.toRadians(315D);
        float syncRange = (float) Math.toRadians(45D);
        float t = 0F;

        if (travel >= syncStart)
        {
            t = Math.min(1F, (travel - syncStart) / syncRange);
        }

        return headYawBase * (1F - t);
    }

    public ModelBlockEntityRenderer(BlockEntityRendererProvider.Context ctx)
    {}

    public boolean rendersOutsideBoundingBox(ModelBlockEntity blockEntity)
    {
        return blockEntity.getProperties().isGlobal();
    }

    public void render(ModelBlockEntity entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay)
    {
        Minecraft mc = Minecraft.getInstance();
        ModelProperties properties = entity.getProperties();
        Transform transform = properties.getTransform();
        BlockPos pos = entity.getPos();
        boolean appliedRuntimeOverlay = false;

        matrices.pushPose();
        matrices.translate(0.5F, 0F, 0.5F);

        if (properties.getForm() != null && this.canRender(entity))
        {
            matrices.pushPose();

            Transform applied = transform;

            if (properties.isLookAt())
            {
                applied = this.applyLookingAnimation(mc, entity, properties, tickDelta);
            }
            else
            {
                IEntity iEntity = entity.getEntity();

                entity.resetLookYaw();
                iEntity.setHeadYaw(0F);
                iEntity.setPrevHeadYaw(0F);
                iEntity.setPitch(0F);
                iEntity.setPrevPitch(0F);
            }

            MatrixStackUtils.applyTransform(matrices, applied);

            int lightAbove = LevelRenderer.getLightColor(entity.getWorld(), pos.offset((int) transform.translate.x, (int) transform.translate.y, (int) transform.translate.z));
            Camera camera = mc.gameRenderer.getMainCamera();

            GlStateManager._enableDepthTest();
            FormUtilsClient.render(properties.getForm(), new FormRenderingContext()
                .set(FormRenderType.MODEL_BLOCK, entity.getEntity(), matrices, lightAbove, overlay, tickDelta)
                .camera(camera));
            GlStateManager._disableDepthTest();

            if (this.canRenderAxes(entity) && UIBaseMenu.renderAxes)
            {
                matrices.pushPose();
                MatrixStackUtils.scaleBack(matrices);
                Draw.coolerAxes(matrices, 0.5F, 0.01F, 0.51F, 0.02F);
                matrices.popPose();
            }

            matrices.popPose();
        }

        GlStateManager._disableDepthTest();

        if (mc.getDebugOverlay().showDebugScreen())
        {
            Draw.renderBox(matrices, -0.5D, 0, -0.5D, 1, 1, 1, 0, 0.5F, 1F, 0.5F);
        }

        matrices.popPose();

        if (properties.isShadow())
        {
            float tx = 0.5F + transform.translate.x;
            float ty = transform.translate.y;
            float tz = 0.5F + transform.translate.z;
            double x = pos.getX() + tx;
            double y = pos.getY() + ty;
            double z = pos.getZ() + tz;

            renderShadow(vertexConsumers, matrices, tickDelta, x, y, z, tx, ty, tz);
        }

        if (appliedRuntimeOverlay && properties.getForm() instanceof ModelForm modelForm)
        {
            modelForm.poseOverlay.setRuntimeValue(null);
        }
    }

    private Transform applyLookingAnimation(Minecraft mc, ModelBlockEntity entity, ModelProperties properties, float tickDelta)
    {
        Transform transform = properties.getTransform();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 position = !mc.options.getCameraType().isFirstPerson() && mc.player != null
            ? mc.player.getEyePosition(tickDelta)
            : camera.position();

        BlockPos pos = entity.getPos();
        double x = pos.getX() + 0.5D + transform.translate.x;
        double y = pos.getY() + transform.translate.y;
        double z = pos.getZ() + 0.5D + transform.translate.z;

        double dx = position.x - x;
        double dz = position.z - z;
        double distance = Math.sqrt(dx * dx + dz * dz);

        float initialYaw = transform.rotate.y;
        float yaw = (float) Math.atan2(dx, dz);
        float yawContinuous = entity.updateLookYawContinuous(yaw);
        float yawDelta = yawContinuous - initialYaw;
        float travel = Math.abs(yawDelta) % (MathUtils.PI * 2F);

        Transform finalTransform = transform.copy();
        Form form = properties.getForm();
        boolean lookAt = form instanceof MobForm;
        float headHeight = form.hitboxHeight.get() * form.hitboxEyeHeight.get() * finalTransform.scale.y;
        float constraint = 45F;
        boolean isPitching = true;

        if (form instanceof ModelForm modelForm)
        {
            ModelInstance model = ModelFormRenderer.getModel(modelForm);

            if (model != null && model.view != null)
            {
                String headKey = model.view.headBone;

                lookAt = true;
                constraint = model.view.constraint;
                isPitching = model.view.pitch;

                if (FormUtilsClient.getBones(modelForm).contains(headKey))
                {
                    MatrixCache matrices = new MatrixCache();

                    model.captureMatrices(matrices);

                    Matrix4f matrix = matrices.get(headKey).matrix();

                    if (matrix != null)
                    {
                        headHeight = matrix.getTranslation(new Vector3f()).y * finalTransform.scale.y;
                    }
                }
            }
        }

        finalTransform.rotate.y = yawContinuous;

        if (lookAt)
        {
            IEntity iEntity = entity.getEntity();
            double deltaHead = position.y - (y + headHeight);
            float pitch = MathUtils.clamp((float) Math.atan2(deltaHead, distance), -MathUtils.PI / 2F, MathUtils.PI / 2F);
            float headYaw = getHeadYaw(constraint, yawDelta, travel);
            float anchorYaw = yawDelta - headYaw;

            if (travel >= (float) Math.toRadians(359D))
            {
                headYaw = 0F;
                anchorYaw = 0F;

                entity.snapLookYawToBase(yaw, initialYaw);
            }

            finalTransform.rotate.y = initialYaw + anchorYaw;
            headYaw = -MathUtils.toDeg(headYaw);
            pitch = -MathUtils.toDeg(isPitching ? pitch : 0F);

            iEntity.setHeadYaw(headYaw);
            iEntity.setPrevHeadYaw(headYaw);
            iEntity.setPitch(pitch);
            iEntity.setPrevPitch(pitch);
        }

        return finalTransform;
    }

    public int getRenderDistance()
    {
        return 512;
    }

    private boolean canRenderAxes(ModelBlockEntity entity)
    {
        if (UIScreen.getCurrentMenu() instanceof UIDashboard dashboard)
        {
            return dashboard.getPanels().panel instanceof UIModelBlockPanel modelBlockPanel;
        }

        return false;
    }

    private boolean canRender(ModelBlockEntity entity)
    {
        if (!entity.getProperties().isEnabled())
        {
            return false;
        }

        if (!BBSSettings.renderAllModelBlocks.get())
        {
            return false;
        }

        if (UIScreen.getCurrentMenu() instanceof UIDashboard dashboard)
        {
            if (dashboard.getPanels().panel instanceof UIModelBlockPanel modelBlockPanel)
            {
                return !modelBlockPanel.isEditing(entity) || UIModelBlockPanel.toggleRendering;
            }
        }

        return true;
    }
}
