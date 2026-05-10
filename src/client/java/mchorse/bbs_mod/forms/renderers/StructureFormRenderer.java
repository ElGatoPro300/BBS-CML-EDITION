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
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
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

import org.joml.Matrix4f;

import com.mojang.blaze3d.opengl.GlStateManager;
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
        this.ensureLoaded();
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
        this.ensureLoaded();

        context.stack.push();

        boolean optimize = true;
        boolean picking = context.isPicking();

        IModelVAO vao = this.getStructureVao();

        StructureLightSettings sl = this.form.structureLight.getRuntimeValue();
        boolean currentEmitLight = (sl != null) ? sl.enabled : this.form.emitLight.get();
        int currentLightIntensity = (sl != null) ? sl.intensity : this.form.lightIntensity.get();

        if (currentEmitLight != this.lastEmitLight || currentLightIntensity != this.lastLightIntensity)
        {
            this.vaoDirty = true;
            this.lastEmitLight = currentEmitLight;
            this.lastLightIntensity = currentLightIntensity;
        }

        if (optimize && (vao == null || this.vaoDirty))
        {
            this.buildStructureVAO();
            vao = this.getStructureVao();
        }

        if (!optimize)
        {
            /* If picking, render with VAO (picking) and picking shader to get full silhouette */
            if (picking)
            {
                IModelVAO pickingVao = this.getStructureVaoPicking();

                if (pickingVao == null || this.vaoPickingDirty)
                {
                    this.buildStructureVAOPicking();
                    pickingVao = this.getStructureVaoPicking();
                }

                Color tint3D = this.form.color.get();
                int light = 0;
                GameRenderer gameRenderer = MinecraftClient.getInstance().gameRenderer;

                /* lightmap state handled by renderer in 1.21.11 */
                /* overlay state handled by renderer in 1.21.11 */

                this.setupTarget(context, BBSShaders.getPickerModelsProgram());
                /* shader binding handled by render pipeline */
                GlStateManager._enableBlend();
                /* texture binding handled by render pipeline */

                ModelVAORenderer.render(BBSShaders.getPickerModelsProgram(), pickingVao, context.stack, tint3D.r, tint3D.g, tint3D.b, tint3D.a, light, context.overlay);

                /* lightmap state handled by renderer in 1.21.11 */
                /* overlay state handled by renderer in 1.21.11 */

                GlStateManager._disableBlend();
                GlStateManager._enableDepthTest();
                GlStateManager._depthFunc(GL11.GL_LEQUAL);
            }
            else
            {
                /* BufferBuilder mode: use vanilla/culling pipeline with better lighting */
                int light = context.light;
                boolean shaders = this.isShadersActive();
                VertexConsumerProvider consumers = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
                GameRenderer gameRenderer = MinecraftClient.getInstance().gameRenderer;

                /* Align state handling with VAO path to avoid state leaks affecting the first model rendered after. */
                /* lightmap state handled by renderer in 1.21.11 */
                /* overlay state handled by renderer in 1.21.11 */
                /* Ensure block atlas is active when starting the pass */
                /* texture binding handled by render pipeline */

                try
                {
                    this.renderStructureCulledWorld(context, context.stack, consumers, light, context.overlay, shaders);

                    if (consumers instanceof VertexConsumerProvider.Immediate immediate)
                    {
                        immediate.draw();
                    }
                }
                catch (Throwable ignored)
                {}

                /* Restore state after BufferBuilder pass to avoid contaminating next render (models, UI, etc.) */
                /* lightmap state handled by renderer in 1.21.11 */
                /* overlay state handled by renderer in 1.21.11 */

                GlStateManager._disableBlend();
                GlStateManager._enableDepthTest();
                GlStateManager._depthFunc(GL11.GL_LEQUAL);
            }
        }
        else if (vao != null)
        {
            Color tint3D = this.form.color.get();
            int light = context.isPicking() ? 0 : context.light;
            GameRenderer gameRenderer = MinecraftClient.getInstance().gameRenderer;

            /* lightmap state handled by renderer in 1.21.11 */
            /* overlay state handled by renderer in 1.21.11 */

            if (context.isPicking())
            {
                IModelVAO pickingVao = this.getStructureVaoPicking();

                if (pickingVao == null || this.vaoPickingDirty)
                {
                    this.buildStructureVAOPicking();
                    pickingVao = this.getStructureVaoPicking();
                }

                this.setupTarget(context, BBSShaders.getPickerModelsProgram());
                /* shader binding handled by render pipeline */
                GlStateManager._enableBlend();
                /* texture binding handled by render pipeline */

                ModelVAORenderer.render(BBSShaders.getPickerModelsProgram(), pickingVao, context.stack, tint3D.r, tint3D.g, tint3D.b, tint3D.a, light, context.overlay);
            }
            else
            {
                /* VAO with shader compatible with packs: use translucent entity program when Iris is active */
                ShaderProgram shader = BBSShaders.getModel();

                if (BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld())
                {
                    shader = BBSShaders.getModel();
                }

                /* texture binding handled by render pipeline */
                GlStateManager._enableBlend();
                GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

                ModelVAORenderer.render(shader, vao, context.stack, tint3D.r, tint3D.g, tint3D.b, tint3D.a, light, context.overlay);

                if (this.hasBlockEntityLayer)
                {
                    try
                    {
                        VertexConsumerProvider beConsumers = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

                        this.renderBlockEntitiesOnly(context, context.stack, beConsumers, light, context.overlay);

                        if (beConsumers instanceof VertexConsumerProvider.Immediate immediate)
                        {
                            immediate.draw();
                        }
                    }
                    catch (Throwable ignored)
                    {}
                }

                if (this.hasBiomeTintedLayer)
                {
                    try
                    {
                        VertexConsumerProvider.Immediate tintConsumers = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

                        this.renderBiomeTintedBlocksVanilla(context, context.stack, tintConsumers, light, context.overlay);
                        tintConsumers.draw();
                    }
                    catch (Throwable ignored)
                    {}
                }

                if (this.hasAnimatedLayer)
                {
                    try
                    {
                        VertexConsumerProvider.Immediate animConsumers = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();

                        this.renderAnimatedBlocksVanilla(context, context.stack, animConsumers, light, context.overlay);
                        animConsumers.draw();
                    }
                    catch (Throwable ignored)
                    {}
                }
            }

            /* lightmap state handled by renderer in 1.21.11 */
            /* overlay state handled by renderer in 1.21.11 */

            /* Restore state if VAO was used */
            GlStateManager._disableBlend();
            GlStateManager._enableDepthTest();
            GlStateManager._depthFunc(GL11.GL_LEQUAL);
        }

        CustomVertexConsumerProvider.clearRunnables();
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
            .setForceMaxSkyLight(!this.capturingVAO && (context.ui
                || context.type == FormRenderType.PREVIEW
                || context.type == FormRenderType.ITEM_INVENTORY || forceMaxSkyLight));

        return info;
    }

    /**
     * Render with culling using virtual BlockRenderView to leverage vanilla logic.
     * Keeps the same centering and parity as renderStructure.
     */
    private void renderStructureCulledWorld(FormRenderingContext context, MatrixStack stack, VertexConsumerProvider consumers, int light, int overlay, boolean useEntityLayers)
    {
        RenderInfo info = this.calculateRenderInfo(context, false);
        float globalAlpha;

        for (BlockEntry entry : this.blocks)
        {
            RenderLayer layer;
            VertexConsumer vc;
            Color tint;
            Function<VertexConsumer, VertexConsumer> recolor;
            Block block;

            stack.push();
            stack.translate(entry.pos.getX() - info.pivotX, entry.pos.getY() - info.pivotY, entry.pos.getZ() - info.pivotZ);

            /* During normal VAO capture, skip blocks with animated textures */
            /* or biome tint to avoid double drawing and flickering. */
            /* In picking capture (capturingIncludeSpecialBlocks=true), include them. */
            if (this.capturingVAO && !this.capturingIncludeSpecialBlocks && (this.isAnimatedTexture(entry.state) || this.isBiomeTinted(entry.state)))
            {
                stack.pop();
                continue;
            }

            /* Use entity layer for blocks when rendering with the entity vertex provider */
            /* of WorldRenderer. This ensures compatibility */
            /* with shaders (Iris/Sodium) for translucent and special layers. */
            layer = useEntityLayers
                ? RenderLayers.translucentMovingBlock()
                : RenderLayers.cutout();

            /* If there is global opacity (<1), force translucent layer for all blocks */
            /* of the structure, so alpha is applied even to solid/cutout geometry. */
            /* In shaders mode (useEntityLayers=true) use the translucent entity variant WITH CULL */
            /* to preserve culling and avoid double faces with packs. */
            globalAlpha = this.form.color.get().a;

            if (globalAlpha < 0.999F)
            {
                layer = useEntityLayers
                    ? RenderLayers.translucentMovingBlock()
                    : RenderLayers.translucentMovingBlock();
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

            MinecraftClient.getInstance().getBlockRenderManager().renderBlock(entry.state, entry.pos, info.view, stack, vc, true, null);

            /* Render blocks with entity (chests, beds, signs, skulls, etc.) */
            block = entry.state.getBlock();

            if (!this.capturingVAO && block instanceof BlockEntityProvider)
            {
                /* Align BE position with the real location where it is drawn */
                int dx = (int) Math.floor(entry.pos.getX() - info.pivotX);
                int dy = (int) Math.floor(entry.pos.getY() - info.pivotY);
                int dz = (int) Math.floor(entry.pos.getZ() - info.pivotZ);
                BlockPos worldPos = info.anchor.add(dx, dy, dz);
                BlockEntity be = ((BlockEntityProvider) block).createBlockEntity(worldPos, entry.state);

                if (be != null)
                {
                    /* Associate real world so renderer can query light and effects */
                    if (MinecraftClient.getInstance().world != null)
                    {
                        be.setWorld(MinecraftClient.getInstance().world);
                    }

                    /* BlockEntity render path changed in 1.21.11 (state/command based). */
                }
            }

            stack.pop();
        }

        /* Important: if Sodium/Iris is active, the recolor wrapper uses */
        /* global static state (RecolorVertexConsumer.newColor). Ensure */
        /* it is reset after this pass so UI doesn't inherit the tint. */
        RecolorVertexConsumer.newColor = null;
    }

    /**
     * Specialized render: draws only blocks with animated textures (portal, water, lava)
     * using the vanilla TranslucentMovingBlock layer to get continuous animation.
     * Reuses the same centering/parity and virtual world view.
     */
    private void renderAnimatedBlocksVanilla(FormRenderingContext context, MatrixStack stack, VertexConsumerProvider consumers, int light, int overlay)
    {
        /* Ensure block atlas is active */
        /* texture binding handled by render pipeline */

        RenderInfo info = this.calculateRenderInfo(context, false);

        for (BlockEntry entry : this.animatedBlocks)
        {
            boolean shadersEnabled;
            RenderLayer layer;
            float globalAlphaAnim;
            VertexConsumer vc;
            Color tint;
            Function<VertexConsumer, VertexConsumer> recolor;

            if (!this.isAnimatedTexture(entry.state))
            {
                continue;
            }

            stack.push();
            stack.translate(entry.pos.getX() - info.pivotX, entry.pos.getY() - info.pivotY, entry.pos.getZ() - info.pivotZ);

            /* Layer selection: in shaders use entity variant so the pack processes the animation */
            shadersEnabled = BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld();
            layer = shadersEnabled
                ? RenderLayers.translucentMovingBlock()
                : RenderLayers.translucentMovingBlock();

            /* If global alpha exists, prefer translucent entity layer in shaders to ensure smooth fade */
            globalAlphaAnim = this.form.color.get().a;

            if (globalAlphaAnim < 0.999F)
            {
                layer = shadersEnabled
                    ? RenderLayers.translucentMovingBlock()
                    : RenderLayers.translucentMovingBlock();
            }

            /* Apply global alpha as recolor */
            vc = consumers.getBuffer(layer);
            tint = this.form.color.get();
            recolor = BBSRendering.getColorConsumer(tint);

            if (recolor != null)
            {
                vc = recolor.apply(vc);
            }

            MinecraftClient.getInstance().getBlockRenderManager().renderBlock(entry.state, entry.pos, info.view, stack, vc, true, null);
            stack.pop();
        }

        /* Reset global color state (Sodium/Iris) after animated pass */
        RecolorVertexConsumer.newColor = null;
    }

    /** Renders blocks that require biome tint (leaves, grass, vines, lily pad) using vanilla layers. */
    private void renderBiomeTintedBlocksVanilla(FormRenderingContext context, MatrixStack stack, VertexConsumerProvider consumers, int light, int overlay)
    {
        /* Ensure correct blending state for translucent layers */
        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        /* Ensure block atlas is active */
        /* texture binding handled by render pipeline */

        RenderInfo info = this.calculateRenderInfo(context, false);

        for (BlockEntry entry : this.biomeTintedBlocks)
        {
            boolean shadersEnabledTint;
            RenderLayer layer;
            float globalAlpha;
            VertexConsumer vc;
            Color tint;
            Function<VertexConsumer, VertexConsumer> recolor;

            if (!this.isBiomeTinted(entry.state))
            {
                continue;
            }

            stack.push();
            stack.translate(entry.pos.getX() - info.pivotX, entry.pos.getY() - info.pivotY, entry.pos.getZ() - info.pivotZ);

            /* Layer according to state; in shaders use entity variant for packs */
            shadersEnabledTint = BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld();
            layer = shadersEnabledTint
                ? RenderLayers.translucentMovingBlock()
                : RenderLayers.cutout();

            /* If there is global opacity (<1), force translucent layer so alpha */
            /* applies to materials originally cutout/cull and they don't "disappear". */
            globalAlpha = this.form.color.get().a;

            if (globalAlpha < 0.999F)
            {
                layer = shadersEnabledTint ? RenderLayers.translucentMovingBlock() : RenderLayers.translucentMovingBlock();
            }

            vc = consumers.getBuffer(layer);
            tint = this.form.color.get();
            recolor = BBSRendering.getColorConsumer(tint);

            if (recolor != null)
            {
                vc = recolor.apply(vc);
            }

            MinecraftClient.getInstance().getBlockRenderManager().renderBlock(entry.state, entry.pos, info.view, stack, vc, true, null);
            stack.pop();
        }

        /* Restore state */
        GlStateManager._disableBlend();
        /* Reset global color state (Sodium/Iris) to avoid UI tinting */
        RecolorVertexConsumer.newColor = null;
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
        /* Disabled until the block-entity state renderer path is fully ported to 1.21.11. */
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
        /* Capture geometry in a VAO using vanilla pipeline but substituting the consumer. */
        CustomVertexConsumerProvider provider = FormUtilsClient.getProvider();
        StructureVAOCollector collector = new StructureVAOCollector();
        LightmapStructureVAOCollector lightWrapper = new LightmapStructureVAOCollector(collector);
        MatrixStack captureStack = new MatrixStack();
        FormRenderingContext captureContext;
        boolean useEntityLayers = false; /* capture with block layers */
        ModelVAOData data;

        /* Substitute any consumer with our collector. */
        provider.setSubstitute(vc -> lightWrapper);

        captureContext = new FormRenderingContext()
            .set(FormRenderType.PREVIEW, null, captureStack, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0F);

        try
        {
            GraphicsMode gm = MinecraftClient.getInstance().options.getPreset().getValue();

            /* graphics mode hint removed in 1.21.11 */
        }
        catch (Throwable ignored)
        {}

        /* Avoid rendering BlockEntities during capture to avoid mixing atlases. */
        this.capturingVAO = true;
        this.capturingIncludeSpecialBlocks = false; /* for normal VAO, skip animated/biome. */

        try
        {
            this.renderStructureCulledWorld(captureContext, captureStack, provider, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, useEntityLayers);
        }
        finally
        {
            this.capturingVAO = false;
            this.capturingIncludeSpecialBlocks = false;
        }

        provider.draw();
        provider.setSubstitute(null);

        data = collector.toData();

        if (this.lastFile != null)
        {
            VaoHolder holder = VAO_CACHE.computeIfAbsent(this.lastFile, k -> new VaoHolder());

            if (holder.vao instanceof ModelVAO)
            {
                ((ModelVAO) holder.vao).delete();
            }

            if (holder.vao instanceof LightmapModelVAO)
            {
                ((LightmapModelVAO) holder.vao).delete();
            }

            holder.vao = new LightmapModelVAO(data, lightWrapper.getLightmapData());
        }

        this.vaoDirty = false;
    }

    /**
     * Builds a picking VAO that includes animated and biome tinted blocks,
     * so selection silhouette covers the whole structure.
     */
    private void buildStructureVAOPicking()
    {
        CustomVertexConsumerProvider provider = FormUtilsClient.getProvider();
        StructureVAOCollector collector = new StructureVAOCollector();
        MatrixStack captureStack = new MatrixStack();
        FormRenderingContext captureContext;
        boolean useEntityLayers = false;
        ModelVAOData data;

        provider.setSubstitute(vc -> collector);

        captureContext = new FormRenderingContext()
            .set(FormRenderType.PREVIEW, null, captureStack, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, 0F);

        try
        {
            GraphicsMode gm = MinecraftClient.getInstance().options.getPreset().getValue();

            /* graphics mode hint removed in 1.21.11 */
        }
        catch (Throwable ignored)
        {}

        this.capturingVAO = true;
        this.capturingIncludeSpecialBlocks = true; /* include animated and biome for picking. */

        try
        {
            this.renderStructureCulledWorld(captureContext, captureStack, provider, LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, useEntityLayers);
        }
        finally
        {
            this.capturingVAO = false;
            this.capturingIncludeSpecialBlocks = false;
        }

        provider.draw();
        provider.setSubstitute(null);

        data = collector.toData();

        if (this.lastFile != null)
        {
            VaoHolder holder = VAO_CACHE.computeIfAbsent(this.lastFile, k -> new VaoHolder());

            if (holder.picking instanceof ModelVAO)
            {
                ((ModelVAO) holder.picking).delete();
            }

            holder.picking = new ModelVAO(data);
        }

        this.vaoPickingDirty = false;
    }

    private IModelVAO getStructureVao()
    {
        if (this.lastFile == null)
        {
            return null;
        }

        VaoHolder holder = VAO_CACHE.get(this.lastFile);

        return holder != null ? holder.vao : null;
    }

    private IModelVAO getStructureVaoPicking()
    {
        if (this.lastFile == null)
        {
            return null;
        }

        VaoHolder holder = VAO_CACHE.get(this.lastFile);

        return holder != null ? holder.picking : null;
    }

    private void clearCachedVao()
    {
        if (this.lastFile == null)
        {
            return;
        }

        VaoHolder holder = VAO_CACHE.remove(this.lastFile);

        if (holder != null)
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
        public VertexConsumer color(int color)
        {
            this.delegate.color(color);
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

        @Override
        public VertexConsumer lineWidth(float width)
        {
            this.delegate.lineWidth(width);
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
    }

    private void parseStructure(NbtCompound root)
    {
        /* Size */
        if (root.contains("size"))
        {
            int[] sz = root.getIntArray("size").orElse(new int[0]);

            if (sz.length >= 3)
            {
                this.size = new BlockPos(sz[0], sz[1], sz[2]);
            }
        }

        /* Palette -> state list */
        List<BlockState> paletteStates = new ArrayList<>();

        if (root.contains("palette"))
        {
            NbtList palette = root.getList("palette").orElse(new NbtList());

            for (int i = 0; i < palette.size(); i++)
            {
                NbtCompound entry = palette.getCompound(i).orElse(new NbtCompound());
                BlockState state = this.readBlockState(entry);

                paletteStates.add(state);
            }
        }

        /* Blocks */
        if (root.contains("blocks"))
        {
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;
            NbtList list = root.getList("blocks").orElse(new NbtList());

            for (int i = 0; i < list.size(); i++)
            {
                NbtCompound be = list.getCompound(i).orElse(new NbtCompound());
                BlockPos pos = this.readBlockPos(be.getList("pos").orElse(new NbtList()));
                int stateIndex = be.getInt("state").orElse(0);

                if (stateIndex >= 0 && stateIndex < paletteStates.size())
                {
                    BlockState state = paletteStates.get(stateIndex);

                    if (state == null || state.isAir())
                    {
                        continue;
                    }

                    BlockEntry blockEntry = new BlockEntry(state, pos);

                    this.blocks.add(blockEntry);

                    RenderLayer baseLayer = RenderLayers.cutout();

                    if (baseLayer == RenderLayers.translucentMovingBlock() || baseLayer == RenderLayers.translucentMovingBlock())
                    {
                        this.hasTranslucentLayer = true;
                    }
                    else if (baseLayer == RenderLayers.cutout())
                    {
                        this.hasCutoutLayer = true;
                    }

                    if (this.isAnimatedTexture(state))
                    {
                        this.animatedBlocks.add(blockEntry);
                        this.hasAnimatedLayer = true;
                    }

                    if (this.isBiomeTinted(state))
                    {
                        this.biomeTintedBlocks.add(blockEntry);
                        this.hasBiomeTintedLayer = true;
                    }

                    if (state.getBlock() instanceof BlockEntityProvider)
                    {
                        this.blockEntitiesList.add(blockEntry);
                        this.hasBlockEntityLayer = true;
                    }

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

        x = list.getInt(0).orElse(0);
        y = list.getInt(1).orElse(0);
        z = list.getInt(2).orElse(0);

        return new BlockPos(x, y, z);
    }

    private BlockState readBlockState(NbtCompound entry)
    {
        String name = entry.getString("Name").orElse("minecraft:air");
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

        if (entry.contains("Properties"))
        {
            NbtCompound props = entry.getCompound("Properties").orElse(new NbtCompound());

            for (String key : props.getKeys())
            {
                String value = props.getString(key).orElse("");
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

        BlockEntry(BlockState state, BlockPos pos)
        {
            this.state = state;
            this.pos = pos;
        }
    }
}
