package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSRendering;

import net.minecraft.client.util.Window;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Window.class)
public abstract class WindowMixin
{
    @Shadow
    public abstract int getScaleFactor();

    @Inject(method = "getWidth", at = @At("HEAD"), cancellable = true)
    public void onGetWidth(CallbackInfoReturnable<Integer> info)
    {
        if (BBSRendering.canReplaceFramebuffer())
        {
            info.setReturnValue(BBSRendering.getVideoWidth());
        }
    }

    @Inject(method = "getHeight", at = @At("HEAD"), cancellable = true)
    public void onGetHeight(CallbackInfoReturnable<Integer> info)
    {
        if (BBSRendering.canReplaceFramebuffer())
        {
            info.setReturnValue(BBSRendering.getVideoHeight());
        }
    }

    @Inject(method = "getFramebufferWidth", at = @At("HEAD"), cancellable = true)
    public void onGetFramebufferWidth(CallbackInfoReturnable<Integer> info)
    {
        if (BBSRendering.canReplaceFramebuffer())
        {
            info.setReturnValue((int) (BBSRendering.getVideoWidth() * BBSModClient.getOriginalFramebufferScale()));
        }
    }

    @Inject(method = "getFramebufferHeight", at = @At("HEAD"), cancellable = true)
    public void onGetFramebufferHeight(CallbackInfoReturnable<Integer> info)
    {
        if (BBSRendering.canReplaceFramebuffer())
        {
            info.setReturnValue((int) (BBSRendering.getVideoHeight() * BBSModClient.getOriginalFramebufferScale()));
        }
    }

    @Inject(method = "getScaledWidth", at = @At("HEAD"), cancellable = true)
    public void onGetScaledWidth(CallbackInfoReturnable<Integer> info)
    {
        if (BBSRendering.canReplaceFramebuffer())
        {
            info.setReturnValue((int) (BBSRendering.getVideoWidth() / this.getScaleFactor() * BBSModClient.getOriginalFramebufferScale()));
        }
    }

    @Inject(method = "getScaledHeight", at = @At("HEAD"), cancellable = true)
    public void onGetScaledHeight(CallbackInfoReturnable<Integer> info)
    {
        if (BBSRendering.canReplaceFramebuffer())
        {
            info.setReturnValue((int) (BBSRendering.getVideoHeight() / this.getScaleFactor() * BBSModClient.getOriginalFramebufferScale()));
        }
    }
}