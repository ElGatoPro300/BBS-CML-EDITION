package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.renderer.MorphRenderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderManager.class)
public class EntityRenderDispatcherMixin
{
    @Unique
    private static final Map<EntityRenderState, Entity> bbs$stateEntityMap = Collections.synchronizedMap(new WeakHashMap<>());

    @Inject(
        method = "getAndUpdateRenderState",
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
        method = "render(Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/render/state/CameraRenderState;DDDLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void bbs$renderMorph(
        EntityRenderState state,
        CameraRenderState cameraRenderState,
        double x,
        double y,
        double z,
        MatrixStack matrices,
        OrderedRenderCommandQueue queue,
        CallbackInfo ci
    )
    {
        Entity entity = bbs$stateEntityMap.get(state);

        if (!(entity instanceof LivingEntity livingEntity))
        {
            return;
        }

        float yaw = livingEntity.getYaw(0F);
        int light = state.light;
        boolean hurt = livingEntity.hurtTime > 0 || livingEntity.deathTime > 0;

        if (state instanceof LivingEntityRenderState livingState)
        {
            yaw = livingState.bodyYaw;
            hurt = livingState.hurt || livingState.deathTime > 0F;
        }

        int u = OverlayTexture.getU(0F);
        int v = OverlayTexture.getV(hurt);
        int overlay = u | (v << 16);
        VertexConsumerProvider consumers = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

        if (MorphRenderer.renderLivingEntity(livingEntity, yaw, 0F, matrices, consumers, light, overlay))
        {
            if (consumers instanceof VertexConsumerProvider.Immediate immediate)
            {
                immediate.draw();
            }

            ci.cancel();
        }
    }
}