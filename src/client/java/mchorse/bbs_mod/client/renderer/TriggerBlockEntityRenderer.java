package mchorse.bbs_mod.client.renderer;

import mchorse.bbs_mod.blocks.entities.TriggerBlockEntity;
import mchorse.bbs_mod.graphics.Draw;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.HashSet;
import java.util.Set;

public class TriggerBlockEntityRenderer implements BlockEntityRenderer<TriggerBlockEntity, TriggerBlockEntityRenderer.TriggerBlockRenderState>
{
    public static final Set<TriggerBlockEntity> capturedTriggerBlocks = new HashSet<>();

    public TriggerBlockEntityRenderer(BlockEntityRendererFactory.Context ctx)
    {}

    @Override
    public TriggerBlockRenderState createRenderState()
    {
        return new TriggerBlockRenderState();
    }

    @Override
    public void updateRenderState(TriggerBlockEntity blockEntity, TriggerBlockRenderState state, float tickDelta, Vec3d cameraPos, ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay)
    {
        BlockEntityRenderer.super.updateRenderState(blockEntity, state, tickDelta, cameraPos, crumblingOverlay);

        state.entity = blockEntity;
        state.tickDelta = tickDelta;
    }

    @Override
    public void render(TriggerBlockRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState)
    {
        TriggerBlockEntity entity = state.entity;

        if (entity == null)
        {
            return;
        }

        capturedTriggerBlocks.add(entity);

        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.getDebugHud().shouldShowDebugHud())
        {
            matrices.push();
            matrices.translate(0.5D, 0, 0.5D);
            /* Render green debug box for triggers */
            Draw.renderBox(matrices, -0.5D, 0, -0.5D, 1, 1, 1, 0, 1F, 0.5F, 0.5F);
            matrices.pop();

            if (entity.region.get())
            {
                Box box = entity.getRegionBoxRelative();

                /* Render white debug box for region triggers */
                Draw.renderBox(matrices, box.minX, box.minY, box.minZ, box.maxX - box.minX, box.maxY - box.minY, box.maxZ - box.minZ, 1F, 1F, 1F, 0.5F);
            }
        }
    }

    public static class TriggerBlockRenderState extends BlockEntityRenderState
    {
        public TriggerBlockEntity entity;
        public float tickDelta;
    }
}
