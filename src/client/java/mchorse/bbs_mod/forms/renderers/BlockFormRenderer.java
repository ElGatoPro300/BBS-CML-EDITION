package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.BlockForm;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.forms.renderers.utils.FormColorBlend;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.interps.Lerps;
import mchorse.bbs_mod.utils.joml.Vectors;
import mchorse.bbs_mod.utils.pose.Transform;
import org.joml.Vector3f;

import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.OverlayVertexConsumer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;

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
        context.batcher.getContext().draw();

        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        MatrixStack matrices = context.batcher.getContext().getMatrices();

        Matrix4f uiMatrix = ModelFormRenderer.getUIMatrix(context, x1, y1, x2, y2);

        matrices.push();
        MatrixStackUtils.multiply(matrices, uiMatrix);
        matrices.scale(this.form.uiScale.get(), this.form.uiScale.get(), this.form.uiScale.get());

        Color set = Color.white();

        set.mul(this.form.color.get());
        FormColorBlend.blendFormGlowBrighten(set, this.form.glowSettings.get(), this.form.glowingColor.get());

        consumers.setSubstitute(BBSRendering.getColorConsumer(set));
        consumers.setUI(true);
        this.renderRepeatedBlocks(null, matrices, consumers, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, false, true);

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

        context.stack.push();

        if (context.isPicking())
        {
            CustomVertexConsumerProvider.hijackVertexFormat((layer) ->
            {
                this.setupTarget(context, BBSShaders.getPickerModelsProgram());
                RenderSystem.setShader(BBSShaders::getPickerModelsProgram);
            });

            light = 0;
        }
        else
        {
            CustomVertexConsumerProvider.hijackVertexFormat((l) ->
            {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
            });
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
        this.renderRepeatedBlocks(context, context.stack, consumers, light, context.overlay, context.isPicking(), false);

        consumers.draw();
        consumers.setSubstitute(null);

        CustomVertexConsumerProvider.clearRunnables();
        RenderSystem.defaultBlendFunc();

        context.stack.pop();

        RenderSystem.enableDepthTest();
    }

    private void renderRepeatedBlocks(FormRenderingContext context, MatrixStack stack, CustomVertexConsumerProvider consumers, int light, int overlay, boolean picking, boolean ui)
    {
        int repeatX = this.form.repeatX.get();
        int repeatY = this.form.repeatY.get();
        int repeatZ = this.form.repeatZ.get();
        int startX = BlockForm.repeatAxisStart(repeatX, this.form.repeatCenterX.get());
        int startY = BlockForm.repeatAxisStart(repeatY, this.form.repeatCenterY.get());
        int startZ = BlockForm.repeatAxisStart(repeatZ, this.form.repeatCenterZ.get());

        for (int y = 0; y < repeatY; y++)
        {
            for (int z = 0; z < repeatZ; z++)
            {
                for (int x = 0; x < repeatX; x++)
                {
                    stack.push();
                    stack.translate(startX + x, startY + y, startZ + z);

                    int blockLight = context == null ? light : this.resolveBlockLight(context, startX + x, startY + y, startZ + z, light);

                    this.renderSingleBlock(stack, consumers, blockLight, overlay, picking, ui);
                    stack.pop();
                }
            }
        }
    }

    /**
     * Samples world skylight/blocklight at each repeated block's world position.
     * Uses the entity/world matrix instead of the camera-relative render matrix.
     */
    private int resolveBlockLight(FormRenderingContext context, int localX, int localY, int localZ, int fallback)
    {
        if (this.form.repeatX.get() == 1 && this.form.repeatY.get() == 1 && this.form.repeatZ.get() == 1)
        {
            return fallback;
        }

        World world = null;

        if (context.entity != null)
        {
            world = context.entity.getWorld();
        }

        if (world == null)
        {
            world = MinecraftClient.getInstance().world;
        }

        if (world == null)
        {
            return fallback;
        }

        BlockPos blockPos = this.getRepeatBlockWorldPos(context, localX, localY, localZ);

        if (blockPos == null)
        {
            return fallback;
        }

        int sampled = WorldRenderer.getLightmapCoordinates(world, blockPos);
        float lf = 1F - MathUtils.clamp(this.form.lighting.get(), 0F, 1F);
        int u = sampled & '\uffff';
        int v = sampled >> 16 & '\uffff';

        u = (int) Lerps.lerp(u, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, lf);

        return u | v << 16;
    }

    private BlockPos getRepeatBlockWorldPos(FormRenderingContext context, int localX, int localY, int localZ)
    {
        if (context.world != null)
        {
            MatrixStack probe = new MatrixStack();

            probe.peek().getPositionMatrix().set(context.world.peek().getPositionMatrix());
            probe.translate(localX, localY, localZ);

            Vector3f translation = probe.peek().getPositionMatrix().getTranslation(new Vector3f());

            return BlockPos.ofFloored(translation.x, translation.y + 0.5D, translation.z);
        }

        if (context.entity == null)
        {
            return null;
        }

        Transform transform = this.createTransform();
        Vector3f offset = transform.createMatrix().transformPosition(new Vector3f(localX + 0.5F, localY, localZ + 0.5F), new Vector3f());
        float transition = context.getTransition();
        double x = Lerps.lerp(context.entity.getPrevX(), context.entity.getX(), transition) + offset.x;
        double y = Lerps.lerp(context.entity.getPrevY(), context.entity.getY(), transition) + offset.y;
        double z = Lerps.lerp(context.entity.getPrevZ(), context.entity.getZ(), transition) + offset.z;

        return BlockPos.ofFloored(x, y, z);
    }

    private void renderSingleBlock(MatrixStack stack, CustomVertexConsumerProvider consumers, int light, int overlay, boolean picking, boolean ui)
    {
        stack.push();
        stack.translate(-0.5F, 0F, -0.5F);

        /* UI preview uses fixed diffuse lights; world rendering relied on vanilla block lighting before repeat. */
        if (ui && !picking)
        {
            stack.peek().getNormalMatrix().getScale(Vectors.EMPTY_3F);
            stack.peek().getNormalMatrix().scale(1F / Vectors.EMPTY_3F.x, -1F / Vectors.EMPTY_3F.y, 1F / Vectors.EMPTY_3F.z);
        }

        MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(this.form.blockState.get(), stack, consumers, light, overlay);

        if (!picking)
        {
            this.renderBlockEntity(stack, consumers, light, overlay);
        }

        int breakingLevel = this.form.breaking.get();

        if (!picking && breakingLevel > 0 && breakingLevel <= 10)
        {
            RenderLayer crackingLayer = ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(breakingLevel - 1);
            VertexConsumer delegateConsumer = consumers.getBuffer(crackingLayer);
            VertexConsumer crackingConsumer = new OverlayVertexConsumer(delegateConsumer, stack.peek().getPositionMatrix(), stack.peek().getNormalMatrix(), 1.0F);
            consumers.setSubstitute((vertexConsumer) -> crackingConsumer);
            MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(this.form.blockState.get(), stack, consumers, light, overlay);
        }

        stack.pop();
    }

    private void renderBlockEntity(MatrixStack stack, CustomVertexConsumerProvider consumers, int light, int overlay)
    {
        if (!(this.form.blockState.get().getBlock() instanceof BlockEntityProvider provider))
        {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        BlockEntity blockEntity = provider.createBlockEntity(BlockPos.ORIGIN, this.form.blockState.get());

        if (blockEntity == null)
        {
            return;
        }

        if (client.world != null)
        {
            blockEntity.setWorld(client.world);
        }

        BlockEntityRenderDispatcher dispatcher = client.getBlockEntityRenderDispatcher();
        BlockEntityRenderer<?> renderer = dispatcher.get(blockEntity);

        if (renderer == null)
        {
            return;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        BlockEntityRenderer raw = (BlockEntityRenderer) renderer;

        raw.render(blockEntity, 0F, stack, consumers, light, overlay);
    }
}
