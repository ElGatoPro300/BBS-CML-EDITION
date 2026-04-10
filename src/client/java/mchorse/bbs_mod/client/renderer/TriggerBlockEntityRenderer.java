package mchorse.bbs_mod.client.renderer;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.vertex.PoseStack;
import mchorse.bbs_mod.blocks.entities.TriggerBlockEntity;
import mchorse.bbs_mod.graphics.Draw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;

public class TriggerBlockEntityRenderer
{
    public static final Set<TriggerBlockEntity> capturedTriggerBlocks = new HashSet<>();

    public TriggerBlockEntityRenderer(BlockEntityRendererProvider.Context ctx)
    {}

    public void render(TriggerBlockEntity entity, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay)
    {
        capturedTriggerBlocks.add(entity);

        Minecraft mc = Minecraft.getInstance();
        
        if (mc.getDebugOverlay().showDebugScreen())
        {
            matrices.pushPose();
            matrices.translate(0.5D, 0, 0.5D);
            /* Render green debug box for triggers */
            Draw.renderBox(matrices, -0.5D, 0, -0.5D, 1, 1, 1, 0, 1F, 0.5F, 0.5F);
            matrices.popPose();

            if (entity.region.get())
            {
                AABB box = entity.getRegionBoxRelative();

                /* Render white debug box for region triggers */
                GlStateManager._disableDepthTest();
                Draw.renderBox(matrices, box.minX, box.minY, box.minZ, box.maxX - box.minX, box.maxY - box.minY, box.maxZ - box.minZ, 1F, 1F, 1F, 0.5F);
                GlStateManager._enableDepthTest();
            }
        }
    }
}
