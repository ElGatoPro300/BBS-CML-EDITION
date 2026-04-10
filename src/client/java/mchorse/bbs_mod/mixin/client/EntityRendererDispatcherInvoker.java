package mchorse.bbs_mod.mixin.client;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EntityRenderDispatcher.class)
public interface EntityRendererDispatcherInvoker
{
    /* @Invoker("renderShadow")
    public static void bbs$renderShadow(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Entity entity, float opacity, float tickDelta, WorldView world, float radius)
    {} */
}
