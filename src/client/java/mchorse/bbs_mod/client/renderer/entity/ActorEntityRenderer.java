package mchorse.bbs_mod.client.renderer.entity;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mchorse.bbs_mod.cubic.render.vanilla.ArmorRenderer;
import mchorse.bbs_mod.entity.ActorEntity;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;

public class ActorEntityRenderer extends EntityRenderer<ActorEntity, ActorEntityRenderer.ActorEntityState>
{
    public static class ActorEntityState extends LivingEntityRenderState {
        public ActorEntity entity;
        public float tickDelta;
        public float bodyYaw;
        public float prevBodyYaw;
        public float deathTime;
        public boolean isSleeping;
    }

    public static ArmorRenderer armorRenderer;

    public ActorEntityRenderer(EntityRendererProvider.Context ctx)
    {
        super(ctx);

        armorRenderer = new ArmorRenderer();

        // this.shadowRadius = 0.5F;
    }

    @Override
    public ActorEntityState createRenderState() {
        return new ActorEntityState();
    }

    @Override
    public void extractRenderState(ActorEntity entity, ActorEntityState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        state.entity = entity;
        state.tickDelta = tickDelta;
        state.bodyYaw = entity.yBodyRot;
        state.prevBodyYaw = entity.yBodyRotO;
        state.deathTime = (float)entity.deathTime;
        state.isSleeping = entity.isSleeping();
    }

    public Identifier getTexture(ActorEntityState state)
    {
        return Identifier.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");
    }

    public void render(ActorEntityState state, PoseStack matrices, MultiBufferSource vertexConsumers, int light)
    {
        ActorEntity livingEntity = state.entity;
        if (livingEntity == null) return;

        float tickDelta = state.tickDelta;
        
        matrices.pushPose();

        float bodyYaw = Mth.rotLerp(tickDelta, state.prevBodyYaw, state.bodyYaw);
        int overlay = OverlayTexture.NO_OVERLAY;

        this.setupTransforms(livingEntity, matrices, bodyYaw, tickDelta);

        GlStateManager._enableBlend();
        GlStateManager._enableDepthTest();
        FormUtilsClient.render(livingEntity.getForm(), new FormRenderingContext()
            .set(FormRenderType.ENTITY, livingEntity.getFormEntity(), matrices, light, overlay, tickDelta)
            .camera(Minecraft.getInstance().gameRenderer.getMainCamera()));
        GlStateManager._disableDepthTest();
        GlStateManager._disableBlend();

        matrices.popPose();
    }

    protected boolean isVisible(ActorEntity entity)
    {
        return !entity.isInvisible();
    }

    protected void setupTransforms(ActorEntity entity, PoseStack matrices, float bodyYaw, float tickDelta)
    {
        if (!entity.isSleeping())
        {
            matrices.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
        }

        if (entity.deathTime > 0)
        {
            float deathAngle = (entity.deathTime + tickDelta - 1F) / 20F * 1.6F;

            matrices.mulPose(Axis.ZP.rotationDegrees(Math.min(Mth.sqrt(deathAngle), 1F) * 90F));
        }
    }
}
