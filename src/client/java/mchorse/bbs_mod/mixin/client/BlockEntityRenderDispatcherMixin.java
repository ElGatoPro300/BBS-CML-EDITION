package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.BBSSettings;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin
{
    @Inject(method = "render(Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V", at = @At("HEAD"), cancellable = true)
    private static void onRenderMain(CallbackInfo info)
    {
        if (BBSSettings.chromaSkyEnabled.get() && !BBSSettings.chromaSkyTerrain.get())
        {
            info.cancel();
        }
    }

    @Inject(method = "render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V", at = @At("HEAD"), cancellable = true)
    private void onRenderToo(CallbackInfo info)
    {
        if (BBSSettings.chromaSkyEnabled.get() && !BBSSettings.chromaSkyTerrain.get())
        {
            info.cancel();
        }
    }


}
