package mchorse.bbs_mod.mixin.client;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererInvoker
{
    /* @Invoker("getAnimationCounter")
    public float bbs$getAnimationCounter(LivingEntity entity, float tickDelta); */
}