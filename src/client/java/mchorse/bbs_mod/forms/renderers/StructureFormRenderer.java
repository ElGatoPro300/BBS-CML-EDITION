package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.forms.StructureForm;
import mchorse.bbs_mod.forms.forms.utils.StructureLightSettings;
import mchorse.bbs_mod.forms.renderers.utils.RecolorVertexConsumer;
import mchorse.bbs_mod.forms.renderers.utils.StructureVirtualBlockRenderView;
import mchorse.bbs_mod.forms.renderers.utils.VirtualBlockRenderView;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.joml.Vectors;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.render.BlockRenderLayers;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.mojang.blaze3d.opengl.GlStateManager;

import org.lwjgl.opengl.GL11;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * StructureForm Renderer
 *
 * Implements NBT loading and basic rendering by iterating blocks.
 * To minimize files, the NBT loader is integrated here.
 */
public class StructureFormRenderer extends FormRenderer<StructureForm>
{
    private final List<BlockEntry> blocks = new ArrayList<>();

    private String lastFile = null;

    private BlockPos size = BlockPos.ORIGIN;
    private BlockPos boundsMin = null;
    private BlockPos boundsMax = null;

    private VirtualBlockRenderView.Entry[] entriesCache = null;
    private StructureVirtualBlockRenderView cachedView = null;

    public StructureFormRenderer(StructureForm form)
    {
        super(form);
    }

    @Override
    public void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        /* Ensure current UI batch is flushed before drawing 3D */
        context.batcher.getContext().drawDeferredElements();

        this.ensureLoaded();

        /* 1.21.11 render: context.batcher.getContext().getMatrices() now returns a 2D Matrix3x2fStack for
         * GUI transforms, not a MatrixStack (see .port_1.21.11_notes.md #4); build our own stack instead. */
        MatrixStack matrices = new MatrixStack();
        Matrix4f uiMatrix = ModelFormRenderer.getUIMatrix(context, x1, y1, x2, y2);

        matrices.push();
        MatrixStackUtils.multiply(matrices, uiMatrix);

        /* To draw 3D content inside UI, use standard depth test and restore it at the end to avoid affecting other panels. */
        GlStateManager._depthFunc(GL11.GL_LEQUAL);

        /* Autoscale: adjust so the structure fits in the cell without clipping */
        float cellW = x2 - x1;
        float cellH = y2 - y1;
        float baseScale = cellH / 2.5F; /* same as in ModelFormRenderer#getUIMatrix */
        float targetPixels = Math.min(cellW, cellH) * 0.9F; /* 10% margin */

        int wUnits;
        int hUnits;
        int dUnits;
        int maxUnits;
        float auto;
        float finalScale;

        if (this.boundsMin != null && this.boundsMax != null)
        {
            wUnits = Math.max(1, this.boundsMax.getX() - this.boundsMin.getX() + 1);
            hUnits = Math.max(1, this.boundsMax.getY() - this.boundsMin.getY() + 1);
            dUnits = Math.max(1, this.boundsMax.getZ() - this.boundsMin.getZ() + 1);
        }
        else
        {
            wUnits = Math.max(1, this.size.getX());
            hUnits = Math.max(1, this.size.getY());
            dUnits = Math.max(1, this.size.getZ());
        }

        maxUnits = Math.max(wUnits, Math.max(hUnits, dUnits));
        auto = maxUnits > 0 ? targetPixels / (baseScale * maxUnits) : 1F;

        /* Do not exceed user defined scale; only reduce if necessary */
        finalScale = this.form.uiScale.get() * Math.min(1F, auto);
        matrices.scale(finalScale, finalScale, finalScale);

        matrices.peek().getNormalMatrix().getScale(Vectors.EMPTY_3F);
        matrices.peek().getNormalMatrix().scale(1F / Vectors.EMPTY_3F.x, -1F / Vectors.EMPTY_3F.y, 1F / Vectors.EMPTY_3F.z);

        /* 1.21.11 render: DiffuseLighting.disableGuiDepthLighting()/RenderSystem.setupLevelDiffuseLighting()
         * are gone; DiffuseLighting is now an instance obtained from the game renderer (see
         * .port_1.21.11_notes.md #3). */
        MinecraftClient.getInstance().gameRenderer.getDiffuseLighting().setShaderLights(DiffuseLighting.Type.ENTITY_IN_UI);

        /* 1.21.11 render: LightmapTextureManager.enable()/disable() and OverlayTexture.setupOverlayColor()/
         * teardownOverlayColor() are gone - the lightmap/overlay textures are now bound declaratively per
         * RenderLayer (via RenderSetup.useLightmap()/useOverlay(), already set on the vanilla block
         * RenderLayers used below), not toggled through a global enable/disable call before drawing. */
        VertexConsumerProvider consumers = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

        try
        {
            FormRenderingContext uiContext = new FormRenderingContext()
                .set(FormRenderType.PREVIEW, null, matrices, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0F);

            this.renderStructureCulledWorld(uiContext, matrices, consumers, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);

            if (consumers instanceof VertexConsumerProvider.Immediate immediate)
            {
                immediate.draw();
            }
        }
        catch (Throwable ignored)
        {}

        MinecraftClient.getInstance().gameRenderer.getDiffuseLighting().setShaderLights(DiffuseLighting.Type.LEVEL);

        matrices.pop();

        /* Restore depth state expected by UI system */
        GlStateManager._depthFunc(GL11.GL_ALWAYS);
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
        this.ensureLoaded();

        context.stack.push();

        try
        {
            if (context.isPicking())
            {
                /* 1.21.11 render: structure picking loses pixel accuracy, same as BlockFormRenderer/
                 * LabelFormRenderer (see .port_1.21.11_notes.md #5/#6) - each block quad now draws through
                 * whichever vanilla RenderLayer its own model declares, and there is no more "hijack the
                 * bound shader" trick to force it through our picker pipeline instead. The picking index is
                 * still recorded for whatever else consults it. */
                this.setupTarget(context, null);
            }

            int light = context.isPicking() ? 0 : context.light;

            /* 1.21.11 render: LightmapTextureManager.enable()/disable() and OverlayTexture.setupOverlayColor()/
             * teardownOverlayColor() are gone - the lightmap/overlay textures are now bound declaratively per
             * RenderLayer (via RenderSetup.useLightmap()/useOverlay(), already set on the vanilla block
             * RenderLayers used below), not toggled through a global enable/disable call before drawing. */
            VertexConsumerProvider consumers = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

            try
            {
                this.renderStructureCulledWorld(context, context.stack, consumers, light, context.overlay);

                if (consumers instanceof VertexConsumerProvider.Immediate immediate)
                {
                    immediate.draw();
                }
            }
            catch (Throwable ignored)
            {}

            GlStateManager._enableDepthTest();
            GlStateManager._depthFunc(GL11.GL_LEQUAL);

            CustomVertexConsumerProvider.clearRunnables();
        }
        finally
        {
            context.stack.pop();
        }
    }

    private static class RenderInfo
    {
        public float pivotX;
        public float pivotY;
        public float pivotZ;
        public VirtualBlockRenderView view;
        public BlockPos anchor;
    }

    private RenderInfo calculateRenderInfo(FormRenderingContext context, boolean forceMaxSkyLight)
    {
        RenderInfo info = new RenderInfo();
        float cx;
        float cy;
        float cz;
        float parityXAuto = 0F;
        float parityZAuto = 0F;

        if (this.boundsMin != null && this.boundsMax != null)
        {
            cx = (this.boundsMin.getX() + this.boundsMax.getX()) / 2F;
            cz = (this.boundsMin.getZ() + this.boundsMax.getZ()) / 2F;
            /* Keep it on the ground: use the minimum Y as base */
            cy = this.boundsMin.getY();
        }
        else
        {
            /* Fallback if no bounds calculated */
            cx = this.size.getX() / 2F;
            cy = 0F;
            cz = this.size.getZ() / 2F;
        }

        if (this.boundsMin != null && this.boundsMax != null)
        {
            int widthX = this.boundsMax.getX() - this.boundsMin.getX() + 1;
            int widthZ = this.boundsMax.getZ() - this.boundsMin.getZ() + 1;

            parityXAuto = (widthX % 2 == 1) ? -0.5F : 0F;
            parityZAuto = (widthZ % 2 == 1) ? -0.5F : 0F;
        }

        info.pivotX = cx - parityXAuto;
        info.pivotY = cy;
        info.pivotZ = cz - parityZAuto;

        if (this.entriesCache == null || this.entriesCache.length != this.blocks.size())
        {
            this.entriesCache = new VirtualBlockRenderView.Entry[this.blocks.size()];

            for (int i = 0; i < this.blocks.size(); i++)
            {
                BlockEntry be = this.blocks.get(i);
                this.entriesCache[i] = new VirtualBlockRenderView.Entry(be.state, be.pos);
            }
        }

        StructureLightSettings slRuntime = this.form.structureLight.getRuntimeValue();
        boolean lightsEnabled;
        int lightIntensity;

        /* Resolve unified structure light settings with legacy fallback */
        if (slRuntime != null)
        {
            lightsEnabled = slRuntime.enabled;
            lightIntensity = slRuntime.intensity;
        }
        else
        {
            lightsEnabled = this.form.emitLight.get();
            lightIntensity = this.form.lightIntensity.get();
        }

        if (this.cachedView == null)
        {
            this.cachedView = new StructureVirtualBlockRenderView(Arrays.asList(this.entriesCache));
        }

        info.view = this.cachedView
            .setBiomeOverride(this.form.biomeId.get())
            .setLightsEnabled(lightsEnabled)
            .setLightIntensity(lightIntensity);

        if (lightsEnabled)
        {
            this.cachedView.setVirtualMode(true, lightIntensity)
                .setIgnoreWorldBlockLight(false);
        }
        else
        {
            this.cachedView.setVirtualMode(false, 0)
                .setIgnoreWorldBlockLight(true);
        }

        /* World anchor: for items/UI use player position (more stable) */
        /* to avoid anchoring at (0,0,0) and getting low world light. */
        boolean isItemContext = (context.type == FormRenderType.ITEM
            || context.type == FormRenderType.ITEM_FP
            || context.type == FormRenderType.ITEM_TP
            || context.type == FormRenderType.ITEM_INVENTORY);

        if (isItemContext || context.entity == null)
        {
            MinecraftClient mc = MinecraftClient.getInstance();

            info.anchor = (mc.player != null) ? mc.player.getBlockPos() : BlockPos.ORIGIN;
        }
        else
        {
            info.anchor = new BlockPos(
                (int) Math.floor(context.entity.getX()),
                (int) Math.floor(context.entity.getY()),
                (int) Math.floor(context.entity.getZ())
            );
        }

        /* Define base offset from center/parity so BlockRenderView */
        /* can translate light/color queries to real world coordinates. */
        int baseDx = (int) Math.floor(-info.pivotX);
        int baseDy = (int) Math.floor(-info.pivotY);
        int baseDz = (int) Math.floor(-info.pivotZ);

        info.view.setWorldAnchor(info.anchor, baseDx, baseDy, baseDz)
            /* In UI/thumbnail/inventory item, force max sky light to avoid darkening.
               EXCEPT during VAO capture, where we want real virtual lighting baked. */
            .setForceMaxSkyLight(context.ui
                || context.type == FormRenderType.PREVIEW
                || context.type == FormRenderType.ITEM_INVENTORY || forceMaxSkyLight);

        return info;
    }

    /**
     * Render with culling using virtual BlockRenderView to leverage vanilla logic. Keeps the same
     * centering and parity as renderStructure.
     *
     * <p>1.21.11 render: draws every block through the entity-compatible {@code RenderLayer} variant
     * ({@link BlockRenderLayers#getEntityBlockLayer}/{@link BlockRenderLayers#getMovingBlockLayer}) so
     * this detached preview goes through the same {@code VertexConsumerProvider} path as entities/other
     * forms. The old chunk-building {@code BlockRenderLayer} enum returned by
     * {@link BlockRenderLayers#getBlockLayer} is no longer usable here: it now feeds the deferred
     * chunk-batching pipeline, not {@code VertexConsumerProvider} (see .port_1.21.11_notes.md #5), so the
     * previous distinction between a "shaders" entity path and a plain chunk-layer path is gone -
     * everything renders through the entity path. Block-entity decorations (chests, signs, skulls, etc.)
     * are intentionally NOT rendered here anymore: {@code BlockEntityRenderer#render} now requires a
     * {@code CameraRenderState} that only exists inside the main world render loop (same limitation
     * documented in {@code BlockFormRenderer#renderBlockEntity}), which this detached structure preview
     * has no access to. The base block models still render fine; only the extra BE decoration layer is
     * skipped.</p>
     */
    private void renderStructureCulledWorld(FormRenderingContext context, MatrixStack stack, VertexConsumerProvider consumers, int light, int overlay)
    {
        RenderInfo info = this.calculateRenderInfo(context, false);
        float globalAlpha = this.form.color.get().a;

        for (BlockEntry entry : this.blocks)
        {
            RenderLayer layer;
            VertexConsumer vc;
            Color tint;
            Function<VertexConsumer, VertexConsumer> recolor;

            stack.push();

            try
            {
                stack.translate(entry.pos.getX() - info.pivotX, entry.pos.getY() - info.pivotY, entry.pos.getZ() - info.pivotZ);

                /* Animated blocks (portals, fire, fluids) get the "moving" texture variant so their
                 * animation keeps advancing; everything else uses the plain entity-block layer. */
                layer = this.isAnimatedTexture(entry.state)
                    ? BlockRenderLayers.getMovingBlockLayer(entry.state)
                    : BlockRenderLayers.getEntityBlockLayer(entry.state);

                /* If there is global opacity (<1), force translucent layer for all blocks */
                /* of the structure, so alpha is applied even to solid/cutout geometry. */
                if (globalAlpha < 0.999F)
                {
                    layer = RenderLayers.entityTranslucent(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
                }

                vc = consumers.getBuffer(layer);
                /* Wrap the consumer with tint/opacity to ensure coloration */
                /* also when using entity buffers (shader compatibility). */
                tint = this.form.color.get();
                recolor = BBSRendering.getColorConsumer(tint);

                if (recolor != null)
                {
                    vc = recolor.apply(vc);
                }

                if (!entry.state.getFluidState().isEmpty())
                {
                    VertexConsumer fluidVc = consumers.getBuffer(layer);

                    if (recolor != null)
                    {
                        fluidVc = recolor.apply(fluidVc);
                    }

                    fluidVc = new TransformingVertexConsumer(fluidVc, stack.peek(), entry.pos, true);
                    MinecraftClient.getInstance().getBlockRenderManager().renderFluid(entry.pos, info.view, fluidVc, entry.state, entry.state.getFluidState());
                }

                if (entry.state.getRenderType() != BlockRenderType.INVISIBLE)
                {
                    this.renderBlockModel(entry.state, entry.pos, info.view, stack, vc);
                }
            }
            finally
            {
                stack.pop();
            }
        }

        /* Important: if Sodium/Iris is active, the recolor wrapper uses */
        /* global static state (RecolorVertexConsumer.newColor). Ensure */
        /* it is reset after this pass so UI doesn't inherit the tint. */
        RecolorVertexConsumer.newColor = null;
    }

    /**
     * Renders a single block's baked model quads. 1.21.11 changed
     * {@code BlockRenderManager#renderBlock} to take the already-resolved {@code List<BlockModelPart>}
     * instead of a {@code Random} directly; the model lookup and per-position random seeding that used
     * to happen inside that call now happens here, mirroring vanilla's own chunk/entity renderers.
     */
    private void renderBlockModel(BlockState state, BlockPos pos, VirtualBlockRenderView view, MatrixStack stack, VertexConsumer vc)
    {
        Random random = Random.create();

        random.setSeed(state.getRenderingSeed(pos));

        BlockStateModel model = MinecraftClient.getInstance().getBlockRenderManager().getModel(state);
        List<BlockModelPart> parts = model.getParts(random);

        MinecraftClient.getInstance().getBlockRenderManager().renderBlock(state, pos, view, stack, vc, true, parts);
    }

    /** Determines if the block requires texture animation (portal/water/lava). */
    private boolean isAnimatedTexture(BlockState state)
    {
        FluidState fs;

        if (state == null)
        {
            return false;
        }

        /* Nether Portal */
        if (state.isOf(Blocks.NETHER_PORTAL))
        {
            return true;
        }

        /* Fire */
        if (state.isOf(Blocks.FIRE) || state.isOf(Blocks.SOUL_FIRE))
        {
            return true;
        }

        /* Fluids: water and lava (including flowing variants). */
        fs = state.getFluidState();

        if (fs != null)
        {
            if (fs.getFluid() == Fluids.WATER || fs.getFluid() == Fluids.FLOWING_WATER ||
                fs.getFluid() == Fluids.LAVA || fs.getFluid() == Fluids.FLOWING_LAVA)
            {
                return true;
            }
        }

        return false;
    }

    private void ensureLoaded()
    {
        String file = this.form.structureFile.get();

        if (file == null || file.isEmpty())
        {
            /* Nothing selected; clear to avoid ghost render. */
            this.blocks.clear();
            this.size = BlockPos.ORIGIN;
            this.boundsMin = null;
            this.boundsMax = null;
            this.entriesCache = null;
            this.cachedView = null;
            this.lastFile = null;

            return;
        }

        if (file.equals(this.lastFile) && !this.blocks.isEmpty())
        {
            return;
        }

        File nbtFile = BBSMod.getProvider().getFile(Link.create(file));

        this.blocks.clear();
        this.size = BlockPos.ORIGIN;
        this.boundsMin = null;
        this.boundsMax = null;
        this.lastFile = file;
        this.entriesCache = null;
        this.cachedView = null;

        try
        {
            /* Equivalent of the old "fancy graphics" toggle for leaves cutout vs solid classification
             * (see .port_1.21.11_notes.md - BlockRenderLayers.setCutoutLeaves replaces the removed
             * RenderLayers.setFancyGraphicsOrBetter for this purpose). */
            GraphicsMode gm = MinecraftClient.getInstance().options.getPreset().getValue();

            BlockRenderLayers.setCutoutLeaves(gm != GraphicsMode.FAST);
        }
        catch (Throwable ignored)
        {}

        /* Try reading as external file if exists; otherwise use internal assets InputStream. */
        if (nbtFile != null && nbtFile.exists())
        {
            try
            {
                NbtCompound root = NbtIo.readCompressed(nbtFile.toPath(), NbtSizeTracker.ofUnlimitedBytes());

                this.parseStructure(root);

                return;
            }
            catch (IOException e)
            {}
        }

        /* If no File (internal assets), read via provider InputStream. */
        try (InputStream is = BBSMod.getProvider().getAsset(Link.create(file)))
        {
            try
            {
                NbtCompound root = NbtIo.readCompressed(is, NbtSizeTracker.ofUnlimitedBytes());

                this.parseStructure(root);
            }
            catch (IOException e)
            {}
        }
        catch (Exception e)
        {}
    }

    /**
     * 1.21.11 render: {@code NbtCompound}/{@code NbtList} lost their {@code contains(String, byte type)}
     * and 2-arg {@code getXxx(String, byte type)} overloads (unrelated to the RenderPipeline migration,
     * but required for this file to compile against the current mappings); the single-arg getters now
     * return {@code Optional}, and {@code getXxxOrEmpty(...)}/{@code getXxx(key, default)} convenience
     * overloads replace the old contains()-then-get() pattern used throughout this parser.
     */
    private void parseStructure(NbtCompound root)
    {
        /* Size */
        int[] sz = root.getIntArray("size").orElse(null);

        if (sz != null && sz.length >= 3)
        {
            this.size = new BlockPos(sz[0], sz[1], sz[2]);
        }

        /* Palette -> state list */
        List<BlockState> paletteStates = new ArrayList<>();
        NbtList palette = root.getListOrEmpty("palette");

        for (int i = 0; i < palette.size(); i++)
        {
            NbtCompound entry = palette.getCompoundOrEmpty(i);
            BlockState state = this.readBlockState(entry);

            paletteStates.add(state);
        }

        /* Blocks */
        NbtList list = root.getListOrEmpty("blocks");

        if (!list.isEmpty())
        {
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;

            for (int i = 0; i < list.size(); i++)
            {
                NbtCompound be = list.getCompoundOrEmpty(i);
                BlockPos pos = this.readBlockPos(be.getListOrEmpty("pos"));
                int stateIndex = be.getInt("state", -1);

                if (stateIndex >= 0 && stateIndex < paletteStates.size())
                {
                    BlockState state = paletteStates.get(stateIndex);

                    if (state == null || state.isAir())
                    {
                        continue;
                    }

                    NbtCompound nbt = be.getCompound("nbt").orElse(null);
                    BlockEntry blockEntry = new BlockEntry(state, pos, nbt);

                    this.blocks.add(blockEntry);

                    /* Update bounds */
                    if (pos.getX() < minX) minX = pos.getX();
                    if (pos.getY() < minY) minY = pos.getY();
                    if (pos.getZ() < minZ) minZ = pos.getZ();
                    if (pos.getX() > maxX) maxX = pos.getX();
                    if (pos.getY() > maxY) maxY = pos.getY();
                    if (pos.getZ() > maxZ) maxZ = pos.getZ();
                }
            }

            if (!this.blocks.isEmpty())
            {
                this.boundsMin = new BlockPos(minX, minY, minZ);
                this.boundsMax = new BlockPos(maxX, maxY, maxZ);
            }
        }
    }

    private BlockPos readBlockPos(NbtList list)
    {
        int x;
        int y;
        int z;

        if (list == null || list.size() < 3)
        {
            return BlockPos.ORIGIN;
        }

        x = list.getInt(0, 0);
        y = list.getInt(1, 0);
        z = list.getInt(2, 0);

        return new BlockPos(x, y, z);
    }

    private BlockState readBlockState(NbtCompound entry)
    {
        String name = entry.getString("Name", "");
        Block block;
        BlockState state;

        try
        {
            Identifier id = Identifier.of(name);

            block = Registries.BLOCK.get(id);

            if (block == null)
            {
                block = Blocks.AIR;
            }
        }
        catch (Exception e)
        {
            block = Blocks.AIR;
        }

        if ("minecraft:jigsaw".equals(name) || block == Blocks.JIGSAW)
        {
            return Blocks.AIR.getDefaultState();
        }

        state = block.getDefaultState();

        NbtCompound props = entry.getCompound("Properties").orElse(null);

        if (props != null)
        {
            for (String key : props.getKeys())
            {
                String value = props.getString(key, "");
                Property<?> property = block.getStateManager().getProperty(key);

                if (property != null)
                {
                    Optional<?> parsed = property.parse(value);

                    if (parsed.isPresent())
                    {
                        try
                        {
                            @SuppressWarnings({"rawtypes", "unchecked"})
                            Property raw = property;
                            @SuppressWarnings("unchecked")
                            Comparable c = (Comparable) parsed.get();

                            state = state.with(raw, c);
                        }
                        catch (Exception ignored)
                        {}
                    }
                }
            }
        }

        return state;
    }

    private static class BlockEntry
    {
        final BlockState state;
        final BlockPos pos;
        final NbtCompound nbt;

        BlockEntry(BlockState state, BlockPos pos, NbtCompound nbt)
        {
            this.state = state;
            this.pos = pos;
            this.nbt = nbt;
        }
    }

    private static class TransformingVertexConsumer implements VertexConsumer
    {
        private final VertexConsumer parent;
        private final Matrix4f positionMatrix;
        private final Matrix3f normalMatrix;
        private final BlockPos offset;
        private final boolean injectOverlay;

        public TransformingVertexConsumer(VertexConsumer parent, MatrixStack.Entry entry, BlockPos offset, boolean injectOverlay)
        {
            this.parent = parent;
            this.positionMatrix = new Matrix4f(entry.getPositionMatrix());
            this.normalMatrix = new Matrix3f(entry.getNormalMatrix());
            this.offset = offset;
            this.injectOverlay = injectOverlay;
        }

        @Override
        public VertexConsumer lineWidth(float width)
        {
            this.parent.lineWidth(width);
            return this;
        }

        @Override
        public VertexConsumer color(int argb)
        {
            this.parent.color(argb);
            return this;
        }

        @Override
        public VertexConsumer vertex(float x, float y, float z)
        {
            float nx = x - this.offset.getX();
            float ny = y - this.offset.getY();
            float nz = z - this.offset.getZ();

            float tx = this.positionMatrix.m00() * nx + this.positionMatrix.m10() * ny + this.positionMatrix.m20() * nz + this.positionMatrix.m30();
            float ty = this.positionMatrix.m01() * nx + this.positionMatrix.m11() * ny + this.positionMatrix.m21() * nz + this.positionMatrix.m31();
            float tz = this.positionMatrix.m02() * nx + this.positionMatrix.m12() * ny + this.positionMatrix.m22() * nz + this.positionMatrix.m32();

            this.parent.vertex(tx, ty, tz);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha)
        {
            this.parent.color(red, green, blue, alpha);
            return this;
        }

        @Override
        public VertexConsumer texture(float u, float v)
        {
            this.parent.texture(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlay(int u, int v)
        {
            this.parent.overlay(u, v);
            return this;
        }

        @Override
        public VertexConsumer light(int u, int v)
        {
            if (this.injectOverlay)
            {
                this.parent.overlay(0, 10);
            }
            this.parent.light(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z)
        {
            float tx = this.normalMatrix.m00() * x + this.normalMatrix.m10() * y + this.normalMatrix.m20() * z;
            float ty = this.normalMatrix.m01() * x + this.normalMatrix.m11() * y + this.normalMatrix.m21() * z;
            float tz = this.normalMatrix.m02() * x + this.normalMatrix.m12() * y + this.normalMatrix.m22() * z;

            this.parent.normal(tx, ty, tz);
            return this;
        }
    }
}
