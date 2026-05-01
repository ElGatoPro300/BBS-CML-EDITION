package mchorse.bbs_mod.mixin.client.iris;

import net.irisshaders.iris.pipeline.IrisRenderingPipeline;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(IrisRenderingPipeline.class)
public interface IrisRenderingPipelineAccessor
{
    @Accessor(value = "loadedShaders", remap = false)
    public Set bbs$loadedShaders();
}