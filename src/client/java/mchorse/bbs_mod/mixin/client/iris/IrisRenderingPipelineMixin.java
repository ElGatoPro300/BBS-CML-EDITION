package mchorse.bbs_mod.mixin.client.iris;

import mchorse.bbs_mod.utils.iris.ShaderOpacityPatch;

import net.irisshaders.iris.pipeline.IrisRenderingPipeline;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IrisRenderingPipeline.class)
public class IrisRenderingPipelineMixin
{
    /**
     * After deferred (clouds/fog) has run, flush translucent BBS forms so they keep Iris
     * lighting/depth but no longer punch Complementary VL clouds.
     */
    @Inject(method = "beginTranslucents", at = @At("RETURN"), remap = false, require = 0)
    private void bbsFlushPostDeferredForms(CallbackInfo ci)
    {
        ShaderOpacityPatch.onBeginTranslucents();
    }
}
