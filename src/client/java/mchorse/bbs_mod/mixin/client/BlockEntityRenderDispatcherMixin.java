package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.client.BBSRendering;

import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderManager.class)
public class BlockEntityRenderDispatcherMixin
{
    @Inject(method = "render(Lnet/minecraft/client/render/block/entity/BlockEntityRenderer;Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V", at = @At("HEAD"), cancellable = true)
    private static void onRenderMain(CallbackInfo info)
    {
        if (BBSRendering.isChromaSkyEnabled() && !BBSRendering.isChromaSkyTerrain())
        {
            info.cancel();
        }
    }

    @Inject(method = "render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V", at = @At("HEAD"), cancellable = true)
    private void onRenderToo(CallbackInfo info)
    {
        if (BBSRendering.isChromaSkyEnabled() && !BBSRendering.isChromaSkyTerrain())
        {
            info.cancel();
        }
    }


/*     @Inject(method = "renderEntity", at = @At("HEAD"), cancellable = true)
    public void onRenderEntity(CallbackInfoReturnable<Boolean> info)
    {
        if (BBSRendering.isChromaSkyEnabled() && !BBSRendering.isChromaSkyTerrain())
        {
            info.setReturnValue(false);
        }
    } */
}
