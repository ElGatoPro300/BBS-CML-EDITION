package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.client.BBSRendering;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4fc;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public class WorldRendererMixin
{
    @Inject(method = "prepareChunkRenders", at = @At("HEAD"))
    public void onSetupFrustum(Matrix4fc matrix4f, CallbackInfoReturnable<?> info)
    {
        BBSRendering.camera.set(new Matrix4f(matrix4f));
    }

    @Inject(at = @At("RETURN"), method = "initOutline")
    private void onLoadEntityOutlineShader(CallbackInfo info)
    {
        BBSRendering.resizeExtraFramebuffers();
    }

    @Inject(at = @At("HEAD"), method = "renderLevel")
    private void onRenderLevelStart(CallbackInfo info)
    {
        if (BBSSettings.chromaSkyEnabled.get() && !BBSSettings.chromaSkyTerrain.get())
        {
            BBSRendering.camera.identity();
        }
    }

    @Inject(at = @At("RETURN"), method = "resize")
    private void onResized(int width, int height, CallbackInfo info)
    {
        BBSRendering.resizeExtraFramebuffers();
    }
}
