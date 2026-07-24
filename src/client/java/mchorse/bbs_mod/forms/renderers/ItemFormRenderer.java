package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.forms.forms.ItemForm;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.forms.renderers.utils.FormColorBlend;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.BatchingRenderCommandQueue;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.World;

import org.joml.Matrix4f;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.logging.LogUtils;

import org.lwjgl.opengl.GL11;

import org.slf4j.Logger;

public class ItemFormRenderer extends FormRenderer<ItemForm>
{
    private static final Logger LOGGER = LogUtils.getLogger();

    /* Reused per render to avoid per-frame allocation; the form renderers run single-threaded on the
     * client render thread (same assumption as BlockFormRenderer.color). clearAndUpdate() wipes it first. */
    private static final ItemRenderState renderState = new ItemRenderState();

    public ItemFormRenderer(ItemForm form)
    {
        super(form);
    }

    @Override
    public void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        context.batcher.getContext().drawDeferredElements();

        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        MatrixStack matrices = new MatrixStack();
        Matrix4f uiMatrix = ModelFormRenderer.getUIMatrix(context, x1, y1, x2, y2);

        matrices.push();
        MatrixStackUtils.multiply(matrices, uiMatrix);
        matrices.scale(this.form.uiScale.get(), this.form.uiScale.get(), this.form.uiScale.get());

        MinecraftClient.getInstance().gameRenderer.getDiffuseLighting().setShaderLights(DiffuseLighting.Type.ENTITY_IN_UI);

        Color set = Color.white();
        set.mul(this.form.color.get());
        FormColorBlend.blendFormGlowBrighten(set, this.form.glowSettings.get(), this.form.glowingColor.get());

        consumers.setSubstitute(BBSRendering.getColorConsumer(set));
        consumers.setUI(true);
        renderItem(this.form.stack.get(), this.form.modelTransform.get(), matrices, consumers, MinecraftClient.getInstance().world, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);
        consumers.draw();
        consumers.setUI(false);
        consumers.setSubstitute(null);

        MinecraftClient.getInstance().gameRenderer.getDiffuseLighting().setShaderLights(DiffuseLighting.Type.LEVEL);

        matrices.pop();
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
        boolean isDropped = context.type == FormRenderType.ITEM;
        boolean useDroppedMode = this.shouldUseDroppedMode(isDropped);
        ItemDisplayContext mode = this.getRenderMode(useDroppedMode);
        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        int light = context.light;

        context.stack.push();
        this.applyDroppedAnimation(context, useDroppedMode);

        if (context.isPicking())
        {
            /* TODO(1.21.11 render): RenderSystem.setShader and ShaderProgram-based setupTarget were
             * removed in 1.21.5. The picker_models pipeline must be bound via its RenderLayer and the
             * per-object Target uniform supplied through the pipeline's UBO/DynamicUniforms. */
            this.setupTarget(context, null);

            light = 0;
        }

        BlockFormRenderer.color.set(context.color);
        BlockFormRenderer.color.mul(this.form.color.get());
        FormColorBlend.blendFormGlowBrighten(BlockFormRenderer.color, this.form.glowSettings.get(), this.form.glowingColor.get());

        consumers.setSubstitute(BBSRendering.getColorConsumer(BlockFormRenderer.color));

        World world = context.entity == null ? null : context.entity.getWorld();

        renderItem(this.form.stack.get(), mode, context.stack, consumers, world, light, context.overlay);
        consumers.draw();
        consumers.setSubstitute(null);

        CustomVertexConsumerProvider.clearRunnables();
        GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        context.stack.pop();
    }

    /**
     * Faithful 1.21.11 replacement for the removed high-level {@code ItemRenderer.renderItem(ItemStack, ...,
     * VertexConsumerProvider, ...)} overload used by the 1.21.1 renderer.
     */
    private static void renderItem(ItemStack stack, ItemDisplayContext displayContext, MatrixStack matrices, CustomVertexConsumerProvider consumers, World world, int light, int overlay)
    {
        if (stack == null || stack.isEmpty())
        {
            return;
        }

        ItemModelManager modelManager = MinecraftClient.getInstance().getItemModelManager();

        modelManager.clearAndUpdate(renderState, stack, displayContext, world, null, 0);

        OrderedRenderCommandQueueImpl queue = new OrderedRenderCommandQueueImpl();

        renderState.render(matrices, queue, light, overlay, 0);

        for (BatchingRenderCommandQueue batch : queue.getBatchingQueues().values())
        {
            for (OrderedRenderCommandQueueImpl.ItemCommand command : batch.getItemCommands())
            {
                matrices.push();
                matrices.peek().copy(command.positionMatrix());
                ItemRenderer.renderItem(
                    command.displayContext(),
                    matrices,
                    consumers,
                    command.lightCoords(),
                    command.overlayCoords(),
                    command.tintLayers(),
                    command.quads(),
                    command.renderLayer(),
                    command.glintType()
                );
                matrices.pop();
            }
        }
    }

    boolean shouldUseDroppedMode(boolean isDropped)
    {
        return isDropped || this.form.sameAnimationWhenDropped.get();
    }

    ItemDisplayContext getRenderMode(boolean useDroppedMode)
    {
        if (useDroppedMode)
        {
            if (this.form.sameAnimationWhenDropped.get())
            {
                LOGGER.debug("Forced dropped animation for form {} using GROUND transform", this.form.getFormId());
            }
            else
            {
                LOGGER.debug("Dropped context for form {} using GROUND transform", this.form.getFormId());
            }

            return ItemDisplayContext.GROUND;
        }

        return this.form.modelTransform.get();
    }

    void applyDroppedAnimation(FormRenderingContext context, boolean useDroppedMode)
    {
        if (!useDroppedMode || context.entity == null || context.entity.getWorld() == null)
        {
            return;
        }

        float age = context.entity.getAge() + context.getTransition();
        float uniqueOffset = this.getDroppedUniqueOffset();
        float bob = MathHelper.sin(age / 10F + uniqueOffset) * 0.1F + 0.1F;
        float angle = (age / 20F + uniqueOffset) * 57.295776F;

        context.stack.translate(0F, bob + 0.25F, 0F);
        context.stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
    }

    private float getDroppedUniqueOffset()
    {
        int hash = this.form.stack.get().hashCode();

        return (hash & 65535) / 65535F * 6.2831855F;
    }
}
