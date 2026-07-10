package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.cubic.render.vao.IModelVAO;
import mchorse.bbs_mod.cubic.render.vao.LightmapModelVAO;
import mchorse.bbs_mod.cubic.render.vao.ModelVAO;
import mchorse.bbs_mod.cubic.render.vao.ModelVAOData;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.cubic.render.vao.StructureVAOCollector;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;
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

import net.minecraft.block.AttachedStemBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.GrassBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.LilyPadBlock;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.StemBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LightType;

import net.irisshaders.iris.api.v0.IrisApi;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private static class VaoHolder
    {
        public IModelVAO vao;
        public IModelVAO picking;
    }

    private static final Map<String, VaoHolder> VAO_CACHE = new HashMap<>();

    private final List<BlockEntry> blocks = new ArrayList<>();
    private final List<BlockEntry> animatedBlocks = new ArrayList<>();
    private final List<BlockEntry> biomeTintedBlocks = new ArrayList<>();
    private final List<BlockEntry> blockEntitiesList = new ArrayList<>();

    private String lastFile = null;

    private BlockPos size = BlockPos.ORIGIN;
    private BlockPos boundsMin = null;
    private BlockPos boundsMax = null;

    private boolean vaoDirty = true;
    private boolean capturingVAO = false;
    private boolean vaoPickingDirty = true;
    private boolean capturingIncludeSpecialBlocks = false;
    private boolean lastEmitLight = false;
    private int lastLightIntensity = 0;
    private boolean hasTranslucentLayer = false;
    private boolean hasCutoutLayer = false;
    private boolean hasAnimatedLayer = false;
    private boolean hasBiomeTintedLayer = false;
    private boolean hasBlockEntityLayer = false;
    private VirtualBlockRenderView.Entry[] entriesCache = null;
    private StructureVirtualBlockRenderView cachedView = null;

    public static void clearAllCachedVaos()
    {
        for (VaoHolder holder : VAO_CACHE.values())
        {
            if (holder.vao instanceof ModelVAO)
            {
                ((ModelVAO) holder.vao).delete();
            }

            if (holder.vao instanceof LightmapModelVAO)
            {
                ((LightmapModelVAO) holder.vao).delete();
            }

            if (holder.picking instanceof ModelVAO)
            {
                ((ModelVAO) holder.picking).delete();
            }
        }

        VAO_CACHE.clear();
    }

    public StructureFormRenderer(StructureForm form)
    {
        super(form);
    }

    @Override
    public void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        /* 1.21.11: renderInUI body partially disabled */
        this.ensureLoaded();

        MatrixStack matrices = new MatrixStack();
        Matrix4f uiMatrix = ModelFormRenderer.getUIMatrix(context, x1, y1, x2, y2);

        matrices.push();
        MatrixStackUtils.multiply(matrices, uiMatrix);

        /* Autoscale: adjust so the structure fits in the cell without clipping */
        float cellW = x2 - x1;
        float cellH = y2 - y1;
        float baseScale = cellH / 2.5F;
        float targetPixels = Math.min(cellW, cellH) * 0.9F;

        int wUnits = 1;
        int hUnits = 1;
        int dUnits = 1;
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

        finalScale = this.form.uiScale.get() * Math.min(1F, auto);
        matrices.scale(finalScale, finalScale, finalScale);

        matrices.peek().getNormalMatrix().getScale(Vectors.EMPTY_3F);
        matrices.peek().getNormalMatrix().scale(1F / Vectors.EMPTY_3F.x, -1F / Vectors.EMPTY_3F.y, 1F / Vectors.EMPTY_3F.z);

        Vector3f light0 = new Vector3f(0.85F, 0.85F, -1F).normalize();
        Vector3f light1 = new Vector3f(-0.85F, 0.85F, 1F).normalize();

        /* 1.21.11: VAO rendering + lighting API disabled */
        // RenderSystem.setupLevelDiffuseLighting(light0, light1);

        matrices.pop();

        // RenderSystem.depthFunc(GL11.GL_ALWAYS);
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
        /* 1.21.11: render3D disabled (ShaderProgram removed, API changes) */
        this.ensureLoaded();
        context.stack.push();
        context.stack.pop();
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
        /* 1.21.11: disabled */
        return null;
    }

    /**
     * Render with culling using virtual BlockRenderView to leverage vanilla logic.
     * Keeps the same centering and parity as renderStructure.
     */
    private void renderStructureCulledWorld(FormRenderingContext context, MatrixStack stack, VertexConsumerProvider consumers, int light, int overlay, boolean useEntityLayers)
    {
        /* 1.21.11: disabled */
    }

    /**
     * Specialized render: draws only blocks with animated textures (portal, water, lava)
     * using the vanilla TranslucentMovingBlock layer to get continuous animation.
     * Reuses the same centering/parity and virtual world view.
     */
    private void renderAnimatedBlocksVanilla(FormRenderingContext context, MatrixStack stack, VertexConsumerProvider consumers, int light, int overlay)
    {
        /* 1.21.11: disabled */
    }

    /** Renders blocks that require biome tint (leaves, grass, vines, lily pad) using vanilla layers. */
    private void renderBiomeTintedBlocksVanilla(FormRenderingContext context, MatrixStack stack, VertexConsumerProvider consumers, int light, int overlay)
    {
        /* 1.21.11: disabled */
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

    /** Heuristic: determines if the block uses biome tint (foliage/grass/vine/lily pad). */
    private boolean isBiomeTinted(BlockState state)
    {
        Block b;

        if (state == null)
        {
            return false;
        }

        b = state.getBlock();

        return (b instanceof LeavesBlock)
            || (b instanceof GrassBlock)
            || (b instanceof VineBlock)
            || (b instanceof LilyPadBlock)
            || (b instanceof RedstoneWireBlock)
            || (b instanceof StemBlock)
            || (b instanceof AttachedStemBlock)
            || state.isOf(Blocks.FERN)
            || state.isOf(Blocks.SUGAR_CANE)
            || state.isOf(Blocks.SHORT_GRASS)
            || state.isOf(Blocks.TALL_GRASS)
            || state.isOf(Blocks.LARGE_FERN);
    }

    /**
     * Renders only Block Entities (chests, beds, signs, skulls, etc.) over the structure already drawn via VAO.
     * Reuses the same centering/parity and world anchor calculation as the culled render.
     */
    private void renderBlockEntitiesOnly(FormRenderingContext context, MatrixStack stack, VertexConsumerProvider consumers, int light, int overlay)
    {
        /* 1.21.11: disabled */
    }

    /**
     * Detects if shaders are active (Iris). Avoids hard dependencies using reflection.
     */
    private boolean isShadersActive()
    {
        try
        {
            Class<?> apiClass = Class.forName("IrisApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            Object result = apiClass.getMethod("isShaderPackInUse").invoke(api);

            return result instanceof Boolean && (Boolean) result;
        }
        catch (Throwable ignored)
        {}

        return false;
    }

    private void ensureLoaded()
    {
        String file = this.form.structureFile.get();

        if (file == null || file.isEmpty())
        {
            /* Nothing selected; clear to avoid ghost render. */
            this.blocks.clear();
            this.animatedBlocks.clear();
            this.biomeTintedBlocks.clear();
            this.blockEntitiesList.clear();
            this.size = BlockPos.ORIGIN;
            this.boundsMin = null;
            this.boundsMax = null;
            this.vaoDirty = true;
            this.vaoPickingDirty = true;
            this.hasTranslucentLayer = false;
            this.hasCutoutLayer = false;
            this.hasAnimatedLayer = false;
            this.hasBiomeTintedLayer = false;
            this.hasBlockEntityLayer = false;
            this.entriesCache = null;
            this.cachedView = null;
            this.clearCachedVao();
            this.lastFile = null;

            return;
        }

        if (file.equals(this.lastFile) && !this.blocks.isEmpty())
        {
            return;
        }

        File nbtFile = BBSMod.getProvider().getFile(Link.create(file));

        this.blocks.clear();
        this.animatedBlocks.clear();
        this.biomeTintedBlocks.clear();
        this.blockEntitiesList.clear();
        this.size = BlockPos.ORIGIN;
        this.boundsMin = null;
        this.boundsMax = null;
        this.clearCachedVao();
        this.lastFile = file;
        this.vaoDirty = true;
        this.vaoPickingDirty = true;
        this.hasTranslucentLayer = false;
        this.hasCutoutLayer = false;
        this.hasAnimatedLayer = false;
        this.hasBiomeTintedLayer = false;
        this.hasBlockEntityLayer = false;
        this.entriesCache = null;
        this.cachedView = null;

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

    private void buildStructureVAO()
    {
        /* 1.21.11: disabled */
    }

    /**
     * Builds a picking VAO that includes animated and biome tinted blocks,
     * so selection silhouette covers the whole structure.
     */
    private void buildStructureVAOPicking()
    {
        /* 1.21.11: disabled */
    }

    private IModelVAO getStructureVao()
    {
        /* 1.21.11: disabled */
        return null;
    }

    private IModelVAO getStructureVaoPicking()
    {
        /* 1.21.11: disabled */
        return null;
    }

    private void clearCachedVao()
    {
        /* 1.21.11: disabled */
    }

    private static class LightmapStructureVAOCollector implements VertexConsumer
    {
        private final StructureVAOCollector delegate;
        private int[] lightData = new int[8192];
        private int lightSize = 0;
        private final int[] quadLights = new int[4];
        private int quadIndex = 0;

        public LightmapStructureVAOCollector(StructureVAOCollector delegate)
        {
            this.delegate = delegate;
        }

        public int[] getLightmapData()
        {
            return Arrays.copyOf(this.lightData, this.lightSize);
        }

        @Override
        public VertexConsumer vertex(float x, float y, float z)
        {
            this.delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha)
        {
            this.delegate.color(red, green, blue, alpha);
            return this;
        }

        @Override
        public VertexConsumer color(int argb)
        {
            this.delegate.color(argb);
            return this;
        }

        @Override
        public VertexConsumer texture(float u, float v)
        {
            this.delegate.texture(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlay(int u, int v)
        {
            this.delegate.overlay(u, v);
            return this;
        }

        @Override
        public VertexConsumer light(int u, int v)
        {
            this.quadLights[this.quadIndex] = (u & 0xFFFF) | ((v & 0xFFFF) << 16);
            this.delegate.light(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z)
        {
            this.delegate.normal(x, y, z);

            this.quadIndex++;

            if (this.quadIndex == 4)
            {
                this.addLight(this.quadLights[0]);
                this.addLight(this.quadLights[1]);
                this.addLight(this.quadLights[2]);

                this.addLight(this.quadLights[0]);
                this.addLight(this.quadLights[2]);
                this.addLight(this.quadLights[3]);

                this.quadIndex = 0;
            }

            return this;
        }

        public void fixedColor(int red, int green, int blue, int alpha)
        {
        }

        public void unfixColor()
        {
        }

        private void addLight(int l)
        {
            if (this.lightSize >= this.lightData.length)
            {
                int[] n = new int[this.lightData.length * 2];
                System.arraycopy(this.lightData, 0, n, 0, this.lightSize);
                this.lightData = n;
            }

            this.lightData[this.lightSize++] = l;
        }

        @Override
        public VertexConsumer lineWidth(float width)
        {
            return this;
        }
    }

    private void parseStructure(NbtCompound root)
    {
        /* 1.21.11: disabled */
    }

    private BlockPos readBlockPos(NbtList list)
    {
        /* 1.21.11: disabled */
        return BlockPos.ORIGIN;
    }

    private BlockState readBlockState(NbtCompound entry)
    {
        /* 1.21.11: disabled */
        return Blocks.AIR.getDefaultState();
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
        public VertexConsumer color(int argb)
        {
            this.parent.color(argb);
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

        @Override
        public VertexConsumer lineWidth(float width)
        {
            return this.parent.lineWidth(width);
        }
    }
}
