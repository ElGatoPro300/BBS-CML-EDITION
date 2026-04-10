package mchorse.bbs_mod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import mchorse.bbs_mod.bridge.IEntityRenderState;
import mchorse.bbs_mod.client.renderer.MorphRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class PlayerEntityRendererRenderMixin
{
    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    public void onRender(LivingEntityRenderState state, PoseStack matrixStack, SubmitNodeCollector queue, CameraRenderState cameraRenderState, CallbackInfo info)
    {
        if ((Object) this instanceof AvatarRenderer)
        {
            if (state instanceof AvatarRenderState playerState)
            {
                Entity entity = ((IEntityRenderState) state).bbs$getEntity();

                if (entity instanceof AbstractClientPlayer abstractClientPlayerEntity)
                {
                    MultiBufferSource consumers = Minecraft.getInstance().renderBuffers().bufferSource();

                    if (MorphRenderer.renderPlayer(abstractClientPlayerEntity, 0F, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false), matrixStack, consumers, state.lightCoords))
                    {
                        if (consumers instanceof MultiBufferSource.BufferSource immediate)
                        {
                            immediate.endBatch();
                        }

                        info.cancel();
                    }
                }
            }
        }
    }
}

