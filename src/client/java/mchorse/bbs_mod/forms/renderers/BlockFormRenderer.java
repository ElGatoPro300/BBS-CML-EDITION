package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.BlockForm;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.forms.renderers.utils.FormColorBlend;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.joml.Vectors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.OverlayVertexConsumer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.client.util.math.MatrixStack;

import org.joml.Matrix4f;

import com.mojang.blaze3d.opengl.GlStateManager;

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
        context.batcher.getContext().drawDeferredElements();

        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        MatrixStack matrices = new MatrixStack();

        Matrix4f uiMatrix = ModelFormRenderer.getUIMatrix(context, x1, y1, x2, y2);

        matrices.push();
        MatrixStackUtils.multiply(matrices, uiMatrix);
        matrices.scale(this.form.uiScale.get(), this.form.uiScale.get(), this.form.uiScale.get());
        matrices.translate(-0.5F, 0F, -0.5F);

        matrices.peek().getNormalMatrix().getScale(Vectors.EMPTY_3F);
        matrices.peek().getNormalMatrix().scale(1F / Vectors.EMPTY_3F.x, -1F / Vectors.EMPTY_3F.y, 1F / Vectors.EMPTY_3F.z);

        Color set = Color.white();

        set.mul(this.form.color.get());
        FormColorBlend.blendFormGlowBrighten(set, this.form.glowSettings.get(), this.form.glowingColor.get());

        MinecraftClient.getInstance().gameRenderer.getDiffuseLighting().setShaderLights(DiffuseLighting.Type.ENTITY_IN_UI);

        consumers.setSubstitute(BBSRendering.getColorConsumer(set));
        consumers.setUI(true);
        MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(this.form.blockState.get(), matrices, consumers, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);
        this.renderBlockEntity(matrices, consumers, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);

        int breakingLevel = this.form.breaking.get();
        if (breakingLevel > 0 && breakingLevel <= 10)
        {
            RenderLayer crackingLayer = ModelBaker.BLOCK_DESTRUCTION_RENDER_LAYERS.get(breakingLevel - 1);
            VertexConsumer delegateConsumer = consumers.getBuffer(crackingLayer);
            VertexConsumer crackingConsumer = new OverlayVertexConsumer(delegateConsumer, matrices.peek(), 1.0F);
            consumers.setSubstitute((vertexConsumer) -> crackingConsumer);
            MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(this.form.blockState.get(), matrices, consumers, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);
        }

        consumers.draw();
        consumers.setUI(false);
        consumers.setSubstitute(null);

        MinecraftClient.getInstance().gameRenderer.getDiffuseLighting().setShaderLights(DiffuseLighting.Type.LEVEL);

        matrices.pop();
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        int light = context.light;

        context.stack.push();
        context.stack.translate(-0.5F, 0F, -0.5F);

        if (context.isPicking())
        {
            /* 1.21.11 render: block model faces are drawn through whichever vanilla RenderLayer the block model
             * declares (via CustomVertexConsumerProvider -> renderBlockAsEntity), each bound to its own baked
             * RenderPipeline; there is no more "last bound shader wins" hack to force those faces through our
             * picker pipeline instead (see .port_1.21.11_notes.md #5/#6). Block form picking is therefore not
             * pixel-accurate anymore — this.setupTarget still records the picking index for whatever else
             * consults it, but the block itself renders through its normal layer. */
            this.setupTarget(context, null);

            light = 0;
        }

        color.set(context.color);
        color.mul(this.form.color.get());
        FormColorBlend.blendFormGlowBrighten(color, this.form.glowSettings.get(), this.form.glowingColor.get());

        PaintSettings paintSettings = this.form.paintSettings.get();
        Color legacyPaint = this.form.paintColor.get();
        Color resolvedPaint = new Color();

        paintSettings.resolveColor(legacyPaint, resolvedPaint);
        resolvedPaint.a = paintSettings.resolveIntensity(legacyPaint);

        consumers.setSubstitute(BBSRendering.getColorConsumer(color, resolvedPaint));
        MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(this.form.blockState.get(), context.stack, consumers, light, context.overlay);

        if (!context.isPicking())
        {
            this.renderBlockEntity(context.stack, consumers, light, context.overlay);
        }

        int breakingLevel = this.form.breaking.get();
        if (!context.isPicking() && breakingLevel > 0 && breakingLevel <= 10)
        {
            RenderLayer crackingLayer = ModelBaker.BLOCK_DESTRUCTION_RENDER_LAYERS.get(breakingLevel - 1);
            VertexConsumer delegateConsumer = consumers.getBuffer(crackingLayer);
            VertexConsumer crackingConsumer = new OverlayVertexConsumer(delegateConsumer, context.stack.peek(), 1.0F);
            consumers.setSubstitute((vertexConsumer) -> crackingConsumer);
            MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(this.form.blockState.get(), context.stack, consumers, light, context.overlay);
        }

        consumers.draw();
        consumers.setSubstitute(null);


        context.stack.pop();

        GlStateManager._enableDepthTest();
    }

    /**
     * Renders the block-entity part of blocks that have one (chests, signs, etc.) on top of the base block
     * model. 1.21.11 changed {@code BlockEntityRenderer#render} to
     * {@code render(RenderState, MatrixStack, OrderedRenderCommandQueue, CameraRenderState)} — it now needs a
     * render state built via {@code createRenderState()}/{@code updateRenderState(...)} plus a
     * {@code CameraRenderState} that only exists inside the main world render loop, so this detached form
     * preview has nothing to hand it. Faithfully reproducing that is out of scope for this migration pass; the
     * base block model (see {@code renderBlockAsEntity} above, unaffected by this change) still renders fine,
     * this only skips the extra block-entity decoration layer.
     */
    private void renderBlockEntity(MatrixStack stack, CustomVertexConsumerProvider consumers, int light, int overlay)
    {}
}
