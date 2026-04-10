package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.renderer.MorphRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin
{
    @Unique
    private static final Map<EntityRenderState, Entity> bbs$stateEntityMap = Collections.synchronizedMap(new WeakHashMap<>());

    @Inject(
        method = "extractEntity",
        at = @At("RETURN")
    )
    private void bbs$trackStateEntity(Entity entity, float tickDelta, CallbackInfoReturnable<EntityRenderState> cir)
    {
        EntityRenderState state = cir.getReturnValue();

        if (state != null && entity != null)
        {
            bbs$stateEntityMap.put(state, entity);
        }
    }

    @Inject(
        method = "submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/client/renderer/state/CameraRenderState;DDDLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void bbs$renderMorph(
        EntityRenderState state,
        CameraRenderState cameraRenderState,
        double x,
        double y,
        double z,
        PoseStack matrices,
        SubmitNodeCollector queue,
        CallbackInfo ci
    )
    {
        Entity entity = bbs$stateEntityMap.get(state);

        if (!(entity instanceof LivingEntity livingEntity))
        {
            return;
        }

        float yaw = livingEntity.getViewYRot(0F);
        int light = state.lightCoords;
        boolean hurt = livingEntity.hurtTime > 0 || livingEntity.deathTime > 0;

        if (state instanceof LivingEntityRenderState livingState)
        {
            yaw = livingState.bodyRot;
            hurt = livingState.hasRedOverlay || livingState.deathTime > 0F;
        }

        int u = OverlayTexture.u(0F);
        int v = OverlayTexture.v(hurt);
        int overlay = u | (v << 16);
        MultiBufferSource consumers = Minecraft.getInstance().renderBuffers().bufferSource();

        if (MorphRenderer.renderLivingEntity(livingEntity, yaw, 0F, matrices, consumers, light, overlay))
        {
            if (consumers instanceof MultiBufferSource.BufferSource immediate)
            {
                immediate.endBatch();
            }

            ci.cancel();
        }
    }
}