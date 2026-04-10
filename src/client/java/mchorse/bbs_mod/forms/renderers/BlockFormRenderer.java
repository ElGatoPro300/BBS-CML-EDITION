package mchorse.bbs_mod.forms.renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.opengl.GlStateManager;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.BlockForm;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.joml.Vectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Matrix4f;

public class BlockFormRenderer extends FormRenderer<BlockForm>
{
    public static final Color color = new Color();

    public BlockFormRenderer(BlockForm form)
    {
        super(form);
    }

    @Override
    public void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        context.batcher.flush();

        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        PoseStack matrices = new PoseStack();

        Matrix4f uiMatrix = ModelFormRenderer.getUIMatrix(context, x1, y1, x2, y2);

        matrices.pushPose();
        MatrixStackUtils.multiply(matrices, uiMatrix);
        matrices.scale(this.form.uiScale.get(), this.form.uiScale.get(), this.form.uiScale.get());
        matrices.translate(-0.5F, 0F, -0.5F);

        matrices.last().normal().getScale(Vectors.EMPTY_3F);
        matrices.last().normal().scale(1F / Vectors.EMPTY_3F.x, -1F / Vectors.EMPTY_3F.y, 1F / Vectors.EMPTY_3F.z);

        Color set = this.form.color.get();

        consumers.setSubstitute(BBSRendering.getColorConsumer(set));
        consumers.setUI(true);
        // TODO: adapt to 26.1 block renderer entrypoint
        this.renderBlockEntity(matrices, consumers, 240, OverlayTexture.NO_OVERLAY);
        consumers.draw();
        consumers.setUI(false);
        consumers.setSubstitute(null);

        matrices.popPose();
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        int light = context.light;

        context.stack.pushPose();
        context.stack.translate(-0.5F, 0F, -0.5F);

        if (context.isPicking())
        {
            CustomVertexConsumerProvider.hijackVertexFormat((layer) ->
            {
                this.setupTarget(context, BBSShaders.getPickerModelsProgram());
                // RenderSystem.setShader(BBSShaders.getPickerModelsProgram());
            });

            light = 0;
        }
        else
        {
            CustomVertexConsumerProvider.hijackVertexFormat((l) -> GlStateManager._enableBlend());
        }

        Color set = this.form.color.get();

        color.set(context.color);
        color.mul(set);

        consumers.setSubstitute(BBSRendering.getColorConsumer(set));
        // TODO: adapt to 26.1 block renderer entrypoint

        if (!context.isPicking())
        {
            this.renderBlockEntity(context.stack, consumers, light, context.overlay);
        }

        consumers.draw();
        consumers.setSubstitute(null);

        CustomVertexConsumerProvider.clearRunnables();

        context.stack.popPose();

        GlStateManager._enableDepthTest();
    }

    private void renderBlockEntity(PoseStack stack, CustomVertexConsumerProvider consumers, int light, int overlay)
    {
        if (!(this.form.blockState.get().getBlock() instanceof EntityBlock provider))
        {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        BlockEntity blockEntity = provider.newBlockEntity(BlockPos.ZERO, this.form.blockState.get());

        if (blockEntity == null)
        {
            return;
        }

        if (client.level != null)
        {
            blockEntity.setLevel(client.level);
        }

        // TODO 1.21.11: migrate to BlockEntityRenderer<T, S> state/queue rendering API.
    }
}

