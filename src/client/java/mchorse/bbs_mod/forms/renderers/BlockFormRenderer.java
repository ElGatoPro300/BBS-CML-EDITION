package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.BlockForm;
import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.forms.renderers.utils.BlockEffectOverlayUniforms;
import mchorse.bbs_mod.forms.renderers.utils.FormColorBlend;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.interps.Lerps;
import mchorse.bbs_mod.utils.joml.Vectors;
import mchorse.bbs_mod.utils.pose.Transform;

import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
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
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;

import java.util.function.Function;

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

        GlowSettings glowSettings = this.form.glowSettings.get();
        Color legacyGlow = this.form.glowingColor.get();
        float glowIntensity = glowSettings.resolveIntensity(legacyGlow);

        if (glowIntensity < 0F)
        {
            FormColorBlend.blendFormGlowBrighten(set, glowSettings, legacyGlow);
        }

        Color resolvedPaint = FormColorBlend.resolvePaintColor(this.form.paintSettings.get(), this.form.paintColor.get());
        boolean positivePaint = FormColorBlend.hasPositivePaint(this.form.paintSettings.get(), this.form.paintColor.get());

        Vector3f light0 = new Vector3f(0.85F, 0.85F, -1F).normalize();
        Vector3f light1 = new Vector3f(-0.85F, 0.85F, 1F).normalize();
        RenderSystem.setupLevelDiffuseLighting(light0, light1);

        consumers.setSubstitute(this.getBlockMainConsumer(set, resolvedPaint));
        consumers.setUI(true);
        this.renderRepeatedBlocks(null, matrices, consumers, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, false, true, false, false);

        consumers.draw();

        if (positivePaint)
        {
            this.renderPaintOverlay(null, matrices, consumers, resolvedPaint, set.a, OverlayTexture.DEFAULT_UV, true, this.form.paintSettings.get().transform);
        }

        if (glowIntensity > 0F)
        {
            this.renderGlowOverlay(null, matrices, consumers, glowSettings, legacyGlow, glowIntensity, set.a, OverlayTexture.DEFAULT_UV, true);
        }

        consumers.setUI(false);
        consumers.setSubstitute(null);

        DiffuseLighting.disableGuiDepthLighting();

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

        GlowSettings glowSettings = this.form.glowSettings.get();
        Color legacyGlow = this.form.glowingColor.get();
        float glowIntensity = glowSettings.resolveIntensity(legacyGlow);
        boolean positiveGlow = !context.isPicking() && glowIntensity > 0F;

        if (glowIntensity < 0F)
        {
            FormColorBlend.blendFormGlowBrighten(color, glowSettings, legacyGlow);
        }

        PaintSettings paintSettings = this.form.paintSettings.get();
        Color legacyPaint = this.form.paintColor.get();
        Color resolvedPaint = FormColorBlend.resolvePaintColor(paintSettings, legacyPaint);
        boolean positivePaint = !context.isPicking() && FormColorBlend.hasPositivePaint(paintSettings, legacyPaint);

        consumers.setSubstitute(this.getBlockMainConsumer(color, resolvedPaint));
        this.renderRepeatedBlocks(context, context.stack, consumers, light, context.overlay, context.isPicking(), false, false, false);

        consumers.draw();
        consumers.setSubstitute(null);

        if (positivePaint)
        {
            if (BBSRendering.isIrisWorldPaintDeferral())
            {
                this.submitDeferredBlockPaintOverlay(context, resolvedPaint, color.a, context.overlay, paintSettings.transform);
            }
            else
            {
                this.renderPaintOverlay(context, context.stack, consumers, resolvedPaint, color.a, context.overlay, false, paintSettings.transform);
            }
        }

        if (positiveGlow)
        {
            this.renderGlowOverlay(context, context.stack, consumers, glowSettings, legacyGlow, glowIntensity, color.a, context.overlay, false);
        }
        else
        {
            CustomVertexConsumerProvider.clearRunnables();
        }

        RenderSystem.defaultBlendFunc();

        context.stack.pop();

        RenderSystem.enableDepthTest();
    }

    private Function<VertexConsumer, VertexConsumer> getBlockMainConsumer(Color color, Color resolvedPaint)
    {
        if (resolvedPaint != null && resolvedPaint.a < 0F)
        {
            return BBSRendering.getBlockPaintConsumer(color, resolvedPaint);
        }

        return BBSRendering.getColorConsumer(color);
    }

    private void renderRepeatedBlocks(FormRenderingContext context, MatrixStack stack, CustomVertexConsumerProvider consumers, int light, int overlay, boolean picking, boolean ui, boolean glowOverlay, boolean paintOverlay)
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

                    this.renderSingleBlock(stack, consumers, blockLight, overlay, picking, ui, glowOverlay, paintOverlay);
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

    private void renderSingleBlock(MatrixStack stack, CustomVertexConsumerProvider consumers, int light, int overlay, boolean picking, boolean ui, boolean glowOverlay, boolean paintOverlay)
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

        if (!picking && !glowOverlay && !paintOverlay)
        {
            this.renderBlockEntity(stack, consumers, light, overlay);
        }

        int breakingLevel = this.form.breaking.get();

        if (!picking && !glowOverlay && !paintOverlay && breakingLevel > 0 && breakingLevel <= 10)
        {
            RenderLayer crackingLayer = ModelLoader.BLOCK_DESTRUCTION_RENDER_LAYERS.get(breakingLevel - 1);
            VertexConsumer delegateConsumer = consumers.getBuffer(crackingLayer);
            VertexConsumer crackingConsumer = new OverlayVertexConsumer(delegateConsumer, stack.peek(), 1.0F);
            Function<VertexConsumer, VertexConsumer> previousSubstitute = consumers.getSubstitute();

            consumers.setSubstitute((vertexConsumer) -> crackingConsumer);

            try
            {
                MinecraftClient.getInstance().getBlockRenderManager().renderBlockAsEntity(this.form.blockState.get(), stack, consumers, light, overlay);
            }
            finally
            {
                consumers.setSubstitute(previousSubstitute);
            }
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

    private void submitDeferredBlockPaintOverlay(FormRenderingContext context, Color resolvedPaint, float alpha, int overlay, EffectTransform transform)
    {
        MatrixStack stack = context.stack;
        Matrix4f positionMatrix = ModelVAORenderer.capturePaintOverlayRootMatrix(new Matrix4f(stack.peek().getPositionMatrix()));
        Matrix3f normalMatrix = new Matrix3f(stack.peek().getNormalMatrix());
        Color paintOverlay = new Color(resolvedPaint.r, resolvedPaint.g, resolvedPaint.b, resolvedPaint.a);

        paintOverlay.a *= alpha;

        ModelVAORenderer.submitPaintOverlay(false, () ->
        {
            CustomVertexConsumerProvider overlayConsumers = FormUtilsClient.getProvider();
            MatrixStack overlayStack = new MatrixStack();

            overlayStack.peek().getPositionMatrix().set(positionMatrix);
            overlayStack.peek().getNormalMatrix().set(normalMatrix);

            this.renderPaintOverlayPass(null, overlayStack, overlayConsumers, paintOverlay, overlay, false, transform);
        });
    }

    private void renderPaintOverlay(FormRenderingContext context, MatrixStack stack, CustomVertexConsumerProvider consumers, Color resolvedPaint, float alpha, int overlay, boolean ui, EffectTransform transform)
    {
        Color paintOverlay = new Color(resolvedPaint.r, resolvedPaint.g, resolvedPaint.b, resolvedPaint.a);

        paintOverlay.a *= alpha;

        this.renderPaintOverlayPass(context, stack, consumers, paintOverlay, overlay, ui, transform);
    }

    private void renderPaintOverlayPass(FormRenderingContext context, MatrixStack stack, CustomVertexConsumerProvider consumers, Color paintOverlay, int overlay, boolean ui, EffectTransform transform)
    {
        CustomVertexConsumerProvider.clearRunnables();
        CustomVertexConsumerProvider.hijackVertexFormat((l) -> BlockEffectOverlayUniforms.configurePaintOverlayRenderState(transform));

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.depthMask(false);

        consumers.setSubstitute(BBSRendering.getBlockPaintOverlayConsumer(paintOverlay));

        try
        {
            this.renderRepeatedBlocks(context, stack, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay, false, ui, false, true);
            consumers.draw();
        }
        finally
        {
            consumers.setSubstitute(null);
            RenderSystem.depthMask(true);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            CustomVertexConsumerProvider.clearRunnables();
        }
    }

    private void renderGlowOverlay(FormRenderingContext context, MatrixStack stack, CustomVertexConsumerProvider consumers, GlowSettings glowSettings, Color legacyGlow, float glowIntensity, float alpha, int overlay, boolean ui)
    {
        CustomVertexConsumerProvider.clearRunnables();
        CustomVertexConsumerProvider.hijackVertexFormat((l) ->
        {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        });

        int layers = FormColorBlend.resolveGlowOverlayLayers(glowIntensity);
        Color glowColor = FormColorBlend.resolveGlowOverlayColor(glowSettings, legacyGlow, alpha, glowIntensity, layers);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.depthMask(false);

        consumers.setSubstitute(BBSRendering.getGlowOverlayConsumer(glowColor));

        try
        {
            for (int layer = 0; layer < layers; layer++)
            {
                this.renderRepeatedBlocks(context, stack, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay, false, ui, true, false);
                consumers.draw();
            }
        }
        finally
        {
            consumers.setSubstitute(null);
            RenderSystem.depthMask(true);
            CustomVertexConsumerProvider.clearRunnables();
        }
    }
}
