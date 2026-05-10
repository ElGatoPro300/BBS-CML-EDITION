package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.ItemForm;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.joml.Vectors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

import org.joml.Matrix4f;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;

import org.slf4j.Logger;

public class ItemFormRenderer extends FormRenderer<ItemForm>
{
    private static final Logger LOGGER = LogUtils.getLogger();

    public ItemFormRenderer(ItemForm form)
    {
        super(form);
    }

    @Override
    public void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        context.batcher.flush();

        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        MatrixStack matrices = new MatrixStack();

        Matrix4f uiMatrix = ModelFormRenderer.getUIMatrix(context, x1, y1, x2, y2);

        matrices.push();
        MatrixStackUtils.multiply(matrices, uiMatrix);
        matrices.scale(this.form.uiScale.get(), this.form.uiScale.get(), this.form.uiScale.get());

        matrices.peek().getNormalMatrix().getScale(Vectors.EMPTY_3F);
        matrices.peek().getNormalMatrix().scale(1F / Vectors.EMPTY_3F.x, -1F / Vectors.EMPTY_3F.y, 1F / Vectors.EMPTY_3F.z);

        Color set = this.form.color.get();

        consumers.setSubstitute(BBSRendering.getColorConsumer(set));
        consumers.setUI(true);
        consumers.draw();
        consumers.setUI(false);
        consumers.setSubstitute(null);

        matrices.pop();
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        int light = context.light;
        boolean isDropped = context.type == FormRenderType.ITEM;
        boolean useDroppedMode = this.shouldUseDroppedMode(isDropped);
        ItemDisplayContext mode = this.getRenderMode(useDroppedMode);

        context.stack.push();
        this.applyDroppedAnimation(context, useDroppedMode);

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

        BlockFormRenderer.color.set(context.color);
        BlockFormRenderer.color.mul(set);

        consumers.setSubstitute(BBSRendering.getColorConsumer(BlockFormRenderer.color));
        MinecraftClient.getInstance().getItemRenderer().renderItem(
            context.entity instanceof LivingEntity ? (LivingEntity) context.entity : null,
            this.form.stack.get(),
            mode,
            false,
            context.stack,
            consumers,
            context.entity != null ? context.entity.getWorld() : null,
            light,
            context.overlay,
            0
        );
        consumers.draw();
        consumers.setSubstitute(null);

        CustomVertexConsumerProvider.clearRunnables();

        context.stack.pop();

        GlStateManager._enableDepthTest();
    }

    private boolean shouldUseDroppedMode(boolean isDropped)
    {
        return isDropped || this.form.sameAnimationWhenDropped.get();
    }

    private ItemDisplayContext getRenderMode(boolean useDroppedMode)
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

    private void applyDroppedAnimation(FormRenderingContext context, boolean useDroppedMode)
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
