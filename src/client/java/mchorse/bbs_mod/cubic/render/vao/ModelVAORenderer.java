package mchorse.bbs_mod.cubic.render.vao;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.EffectTransformMath;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

public class ModelVAORenderer
{
    /* FS-style paint overlay uniform state (rgb + strength). Set by form renderers before a draw and reset after.
     * "base" holds the whole-form paint; "current" is what the uniform uses and can be overridden per model group (bone). */
    private static float baseR;
    private static float baseG;
    private static float baseB;
    private static float baseStrength;

    private static float paintR;
    private static float paintG;
    private static float paintB;
    private static float paintStrength;

    private static float baseGlowR;
    private static float baseGlowG;
    private static float baseGlowB;
    private static float baseGlowStrength;

    private static float glowR;
    private static float glowG;
    private static float glowB;
    private static float glowStrength;
    private static boolean glowPaintOnly;

    private static boolean glowingUniformActive;

    private static boolean textureBlendActive;
    private static float textureBlendFactor;
    private static Link textureBlendTo;

    /* When true, the model is being drawn as a shader-pack paint overlay pass. Groups still sample their
     * real skin texture so transparent UV regions are discarded; only textured pixels receive paint. */
    private static boolean paintPass;
    private static boolean paintOverlayPass;
    private static boolean paintOverlaySynced;
    /* Multiply Iris-lit pixels by FormColorTint inside the color mask (keeps pack lighting/shadows). */
    private static boolean colorTintOverlayPass;
    /* Captured-matrix redraw after Iris (or immediate low-opacity bypass) — not the paint-overlay shader branch. */
    private static boolean deferredTranslucentPass;

    private static final Matrix4f formRootInverse = new Matrix4f();
    private static final Matrix4f paintEffectInverse = new Matrix4f();
    private static final Vector3f paintMaskHalf = new Vector3f(EffectTransformMath.MODEL_MASK_HALF_BASE);
    private static final Matrix4f colorEffectInverse = new Matrix4f();
    private static final Vector3f colorMaskHalf = new Vector3f(EffectTransformMath.MODEL_MASK_HALF_BASE);
    private static final Matrix4f overlayFormRootInverse = new Matrix4f();
    private static float paintMaskShape;
    private static float colorMaskShape;
    private static boolean paintEffectActive;
    private static boolean colorEffectActive;
    private static boolean paintMaskBottomAnchored = true;
    private static boolean colorMaskBottomAnchored = true;
    private static float formColorR = 1F;
    private static float formColorG = 1F;
    private static float formColorB = 1F;
    private static float formColorA = 1F;
    private static boolean colorTintMasked;
    private static boolean suppressShapeKeyMainPassGlow;

    /* 1x1 white texture used as the albedo source during the paint overlay pass. */
    private static NativeImageBackedTexture whiteTexture;

    /* Saved GL state for the paint overlay pass (restored in endPaintOverlayPass). */
    private static int savedDepthFunc;
    private static boolean savedDepthMask;
    private static boolean savedPolygonOffsetFill;
    private static boolean savedCullEnabled;

    private static final List<PaintOverlayEntry> paintOverlayQueue = new ArrayList<>();

    private static final class PaintOverlayEntry
    {
        private final Matrix4f projection;
        private final Matrix4f modelView;
        private final boolean synced;
        private final boolean fullModel;
        private final boolean colorTint;
        private final boolean depthWrite;
        private final boolean depthTest;
        private final Runnable draw;

        private PaintOverlayEntry(Matrix4f projection, Matrix4f modelView, boolean synced, boolean fullModel, boolean colorTint, boolean depthWrite, boolean depthTest, Runnable draw)
        {
            this.projection = projection;
            this.modelView = modelView;
            this.synced = synced;
            this.fullModel = fullModel;
            this.colorTint = colorTint;
            this.depthWrite = depthWrite;
            this.depthTest = depthTest;
            this.draw = draw;
        }
    }

    /**
     * Full root matrix for deferred Iris paint overlays (terrain/camera matrix already baked in).
     */
    public static Matrix4f capturePaintOverlayRootMatrix(Matrix4f rootStackMatrix)
    {
        return new Matrix4f(RenderSystem.getModelViewMatrix()).mul(rootStackMatrix);
    }

    public static void clearPaintOverlayQueue()
    {
        paintOverlayQueue.clear();
    }

    public static void enqueuePaintOverlay(Matrix4f projection, Matrix4f modelView, Runnable draw)
    {
        enqueuePaintOverlay(projection, modelView, false, false, false, true, true, draw);
    }

    public static void enqueuePaintOverlay(Matrix4f projection, Matrix4f modelView, boolean synced, Runnable draw)
    {
        enqueuePaintOverlay(projection, modelView, synced, false, false, true, true, draw);
    }

    /**
     * Queues a full translucent mesh redraw for after Iris compositing.
     * {@code depthWrite} true = character meshes (self-occlusion); false = flat panels (keep scene depth / fog).
     */
    public static void submitDeferredTranslucentModel(Runnable draw)
    {
        /* Flat / thin translucent meshes z-fight when depth is rewritten after composite.
         * Character self-occlusion uses the two-arg overload with depthWrite true. */
        submitDeferredTranslucentModel(draw, false, true);
    }

    public static void submitDeferredTranslucentModel(Runnable draw, boolean depthWrite)
    {
        submitDeferredTranslucentModel(draw, depthWrite, true);
    }

    /**
     * @param depthTest false for zero-thickness billboards — post-Iris depth does not match
     *                  captured matrices and LEQUAL produces stippled grass bleed-through.
     */
    public static void submitDeferredTranslucentModel(Runnable draw, boolean depthWrite, boolean depthTest)
    {
        enqueuePaintOverlay(
            new Matrix4f(RenderSystem.getProjectionMatrix()),
            new Matrix4f(RenderSystem.getModelViewMatrix()),
            false,
            true,
            false,
            depthWrite,
            depthTest,
            draw
        );
    }

    private static void enqueuePaintOverlay(Matrix4f projection, Matrix4f modelView, boolean synced, boolean fullModel, boolean depthWrite, Runnable draw)
    {
        enqueuePaintOverlay(projection, modelView, synced, fullModel, false, depthWrite, true, draw);
    }

    private static void enqueuePaintOverlay(Matrix4f projection, Matrix4f modelView, boolean synced, boolean fullModel, boolean depthWrite, boolean depthTest, Runnable draw)
    {
        enqueuePaintOverlay(projection, modelView, synced, fullModel, false, depthWrite, depthTest, draw);
    }

    private static void enqueuePaintOverlay(Matrix4f projection, Matrix4f modelView, boolean synced, boolean fullModel, boolean colorTint, boolean depthWrite, boolean depthTest, Runnable draw)
    {
        PaintOverlayEntry entry = new PaintOverlayEntry(
            new Matrix4f(projection),
            new Matrix4f(modelView),
            synced,
            fullModel,
            colorTint,
            depthWrite,
            depthTest,
            draw
        );

        if (BBSRendering.shouldDeferPaintOverlayToFrameEnd())
        {
            paintOverlayQueue.add(entry);
        }
        else
        {
            ModelVAORenderer.runPaintOverlayEntry(entry, false);
        }
    }

    private static void runPaintOverlayEntry(PaintOverlayEntry entry, boolean restoreFramebuffer)
    {
        if (restoreFramebuffer)
        {
            BBSRendering.ensurePaintOverlayTargetFramebuffer();
        }

        GameRenderer gameRenderer = MinecraftClient.getInstance().gameRenderer;

        gameRenderer.getLightmapTextureManager().enable();
        gameRenderer.getOverlayTexture().setupOverlayColor();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.setShader(BBSShaders::getModel);

        Matrix4f savedProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        Matrix4f savedModelView = new Matrix4f(RenderSystem.getModelViewMatrix());

        try
        {
            paintOverlaySynced = entry.synced;

            RenderSystem.setProjectionMatrix(entry.projection, VertexSorter.BY_Z);

            MatrixStackUtils.pushIdentityModelView();

            if (entry.fullModel)
            {
                beginDeferredTranslucentModelPass(entry.depthWrite, entry.depthTest);
            }
            else if (entry.colorTint)
            {
                beginColorTintOverlayPass();
            }
            else
            {
                beginPaintOverlayPass(entry.synced);
            }

            try
            {
                entry.draw.run();
            }
            finally
            {
                if (entry.fullModel)
                {
                    endDeferredTranslucentModelPass();
                }
                else if (entry.colorTint)
                {
                    endColorTintOverlayPass();
                }
                else
                {
                    endPaintOverlayPass();
                }

                MatrixStackUtils.popModelView();
            }
        }
        finally
        {
            RenderSystem.setProjectionMatrix(savedProjection, VertexSorter.BY_Z);

            Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();

            modelViewStack.pushMatrix();
            modelViewStack.set(savedModelView);
            RenderSystem.applyModelViewMatrix();
            modelViewStack.popMatrix();
            RenderSystem.applyModelViewMatrix();

            gameRenderer.getLightmapTextureManager().disable();
            gameRenderer.getOverlayTexture().teardownOverlayColor();
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        }
    }

    /**
     * Queues a paint/glow overlay for {@link #flushPaintOverlayQueue()} at the end of the
     * world frame.
     */
    public static void submitPaintOverlay(boolean synced, Runnable draw)
    {
        ModelVAORenderer.enqueuePaintOverlay(
            new Matrix4f(RenderSystem.getProjectionMatrix()),
            new Matrix4f(RenderSystem.getModelViewMatrix()),
            synced,
            draw
        );
    }

    /**
     * Queues a multiply color-mask overlay after Iris composite so FormColorTint keeps pack
     * lighting/shadows instead of redrawing the whole mesh with the unlit BBS path.
     */
    public static void submitColorTintOverlay(Runnable draw)
    {
        ModelVAORenderer.enqueuePaintOverlay(
            new Matrix4f(RenderSystem.getProjectionMatrix()),
            new Matrix4f(RenderSystem.getModelViewMatrix()),
            false,
            false,
            true,
            true,
            true,
            draw
        );
    }

    /**
     * Queues a paint/glow overlay for {@link #flushPaintOverlayQueue()} at the end of the
     * world frame.
     */
    public static void submitPaintOverlay(Matrix4f projection, Matrix4f modelView, boolean synced, Runnable draw)
    {
        enqueuePaintOverlay(projection, modelView, synced, draw);
    }

    public static void submitPaintOverlay(Matrix4f projection, Matrix4f modelView, Runnable draw)
    {
        enqueuePaintOverlay(projection, modelView, draw);
    }

    /**
     * Runs deferred paint overlay draws after Iris has finished compositing the world frame.
     * The BBS model shader cannot render correctly during Iris' entity/gbuffer pass, but it
     * works on the final framebuffer at the end of {@code renderWorld}.
     */
    public static void flushPaintOverlayQueue()
    {
        if (paintOverlayQueue.isEmpty())
        {
            return;
        }

        try
        {
            for (PaintOverlayEntry entry : paintOverlayQueue)
            {
                ModelVAORenderer.runPaintOverlayEntry(entry, true);
            }
        }
        finally
        {
            paintOverlayQueue.clear();
        }
    }

    public static void beginPaintPass()
    {
        paintPass = true;
    }

    public static void endPaintPass()
    {
        paintPass = false;
    }

    /**
     * Second-pass paint overlay for external shader packs. Re-draws the same geometry with the BBS
     * model shader so paint can be alpha-blended over the shader-pack first pass using the same
     * mix semantics as the no-shader path: mix(litTextureRgb, paintRgb, paintStrength).
     */
    public static void beginPaintOverlayPass(boolean synced)
    {
        beginPaintPass();
        paintOverlayPass = true;
        paintOverlaySynced = synced;

        savedDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        savedPolygonOffsetFill = GL11.glGetBoolean(GL11.GL_POLYGON_OFFSET_FILL);
        savedCullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);

        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1F, -1F);
    }

    /**
     * Multiply the Iris-lit framebuffer by FormColorTint inside the color mask. Keeps pack
     * lighting/shadows while applying the spatial Color transform.
     */
    public static void beginColorTintOverlayPass()
    {
        colorTintOverlayPass = true;
        paintOverlayPass = false;
        paintOverlaySynced = false;

        savedDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        savedPolygonOffsetFill = GL11.glGetBoolean(GL11.GL_POLYGON_OFFSET_FILL);
        savedCullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
            com.mojang.blaze3d.platform.GlStateManager.SrcFactor.DST_COLOR,
            com.mojang.blaze3d.platform.GlStateManager.DstFactor.ZERO,
            com.mojang.blaze3d.platform.GlStateManager.SrcFactor.DST_ALPHA,
            com.mojang.blaze3d.platform.GlStateManager.DstFactor.ZERO
        );

        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);

        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1F, -1F);
    }

    public static void endColorTintOverlayPass()
    {
        colorTintOverlayPass = false;

        GL11.glPolygonOffset(0F, 0F);

        if (savedPolygonOffsetFill)
        {
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        }
        else
        {
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        }

        RenderSystem.depthMask(savedDepthMask);
        RenderSystem.depthFunc(savedDepthFunc);
        RenderSystem.defaultBlendFunc();

        if (savedCullEnabled)
        {
            RenderSystem.enableCull();
        }
        else
        {
            RenderSystem.disableCull();
        }
    }

    /**
     * Full translucent redraw after Iris composite — BBS model shader keeps low form alpha.
     * {@code depthWrite} true matches the no-shader path so render-depth panels can occlude
     * forms behind them. {@code depthTest} false for zero-thickness billboards whose captured
     * depth does not match the post-Iris depth buffer (stippled bleed-through).
     */
    public static void beginDeferredTranslucentModelPass(boolean depthWrite)
    {
        beginDeferredTranslucentModelPass(depthWrite, true);
    }

    public static void beginDeferredTranslucentModelPass(boolean depthWrite, boolean depthTest)
    {
        savedDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        savedCullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        /* Captured full camera+entity matrix while RenderSystem model-view is identity.
         * Do not set paintOverlayPass — that enables the paint-only shader branch and would
         * discard textured geometry when PaintColor.a is 0. */
        deferredTranslucentPass = true;

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
            com.mojang.blaze3d.platform.GlStateManager.SrcFactor.SRC_ALPHA,
            com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
            com.mojang.blaze3d.platform.GlStateManager.SrcFactor.ONE,
            com.mojang.blaze3d.platform.GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA
        );

        if (depthTest)
        {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
        }
        else
        {
            RenderSystem.disableDepthTest();
        }

        RenderSystem.depthMask(depthWrite);
        RenderSystem.enableCull();
    }

    public static void beginDeferredTranslucentModelPass()
    {
        beginDeferredTranslucentModelPass(true, true);
    }

    public static void endDeferredTranslucentModelPass()
    {
        deferredTranslucentPass = false;
        RenderSystem.depthMask(savedDepthMask);
        RenderSystem.depthFunc(savedDepthFunc);
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();

        if (savedCullEnabled)
        {
            RenderSystem.enableCull();
        }
        else
        {
            RenderSystem.disableCull();
        }
    }

    public static void endPaintOverlayPass()
    {
        endPaintPass();
        paintOverlayPass = false;
        paintOverlaySynced = false;

        GL11.glPolygonOffset(0F, 0F);

        if (savedPolygonOffsetFill)
        {
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        }
        else
        {
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        }

        RenderSystem.depthMask(savedDepthMask);
        RenderSystem.depthFunc(savedDepthFunc);
        RenderSystem.defaultBlendFunc();

        if (savedCullEnabled)
        {
            RenderSystem.enableCull();
        }
        else
        {
            RenderSystem.disableCull();
        }
    }

    public static boolean isPaintOverlayPass()
    {
        return paintOverlayPass;
    }

    public static boolean isColorTintOverlayPass()
    {
        return colorTintOverlayPass;
    }

    public static boolean isDeferredTranslucentPass()
    {
        return deferredTranslucentPass;
    }

    private static boolean usesCapturedModelView()
    {
        return paintOverlayPass || deferredTranslucentPass || colorTintOverlayPass;
    }

    /**
     * Temporarily toggles the paint-overlay shader branch while a deferred Iris overlay draw is running.
     */
    public static void runWithPaintOverlayPass(boolean paintOverlay, Runnable draw)
    {
        boolean previous = paintOverlayPass;

        paintOverlayPass = paintOverlay;

        try
        {
            draw.run();
        }
        finally
        {
            paintOverlayPass = previous;
        }
    }

    public static boolean isPaintOverlaySynced()
    {
        return paintOverlaySynced;
    }

    public static boolean isPaintPass()
    {
        return paintPass;
    }

    public static float getBasePaintR()
    {
        return baseR;
    }

    public static float getBasePaintG()
    {
        return baseG;
    }

    public static float getBasePaintB()
    {
        return baseB;
    }

    public static float getBasePaintStrength()
    {
        return baseStrength;
    }

    /**
     * Lazily builds (on the render thread) a 1x1 fully-white texture and returns its GL id. Sampling this
     * texture yields white, so a shader's texel * vertexColour becomes the vertex colour verbatim.
     */
    public static int getWhiteTextureId()
    {
        if (whiteTexture == null)
        {
            try
            {
                BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

                image.setRGB(0, 0, 0xFFFFFFFF);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                ImageIO.write(image, "png", baos);

                whiteTexture = new NativeImageBackedTexture(NativeImage.read(new ByteArrayInputStream(baos.toByteArray())));
            }
            catch (Exception e)
            {
                return 0;
            }
        }

        return whiteTexture.getGlId();
    }

    public static void setPaint(float r, float g, float b, float strength)
    {
        baseR = r;
        baseG = g;
        baseB = b;
        baseStrength = strength;

        paintR = r;
        paintG = g;
        paintB = b;
        paintStrength = strength;
    }

    public static void setGroupPaint(float r, float g, float b, float strength)
    {
        if (strength != 0F)
        {
            paintR = r;
            paintG = g;
            paintB = b;
            paintStrength = strength;
        }
        else
        {
            paintR = baseR;
            paintG = baseG;
            paintB = baseB;
            paintStrength = baseStrength;
        }
    }

    public static void setGlow(GlowSettings settings, float colorR, float colorG, float colorB)
    {
        setGlow(settings, colorR, colorG, colorB, null);
    }

    public static void setGlow(GlowSettings settings, float colorR, float colorG, float colorB, Color legacyColor)
    {
        float strength = settings.resolveIntensity(legacyColor);

        baseGlowR = colorR;
        baseGlowG = colorG;
        baseGlowB = colorB;
        baseGlowStrength = strength;

        glowR = colorR;
        glowG = colorG;
        glowB = colorB;
        glowStrength = strength;
        glowPaintOnly = settings != null && settings.resolvePaintOnly();
    }

    public static void setGlowing(float r, float g, float b, float strength, float radius)
    {
        GlowSettings settings = new GlowSettings(strength, radius);

        setGlow(settings, r, g, b);
    }

    public static void setGroupGlowing(float r, float g, float b, float strength)
    {
        glowR = r;
        glowG = g;
        glowB = b;
        glowStrength = strength;
    }

    public static void clearGlowing()
    {
        baseGlowR = 0F;
        baseGlowG = 0F;
        baseGlowB = 0F;
        baseGlowStrength = 0F;

        glowR = 0F;
        glowG = 0F;
        glowB = 0F;
        glowStrength = 0F;
        glowPaintOnly = false;
    }

    public static boolean isGlowPaintOnly()
    {
        return glowPaintOnly;
    }

    public static boolean isGlowingUniformActive()
    {
        return glowingUniformActive;
    }

    /**
     * CPU mesh builders emit vertices before draw-time uniform upload. Probe the active shader early so
     * shape-key geometry can skip vanilla brighten/light boosts when the BBS GlowingColor uniform applies.
     */
    public static boolean isSuppressShapeKeyMainPassGlow()
    {
        return suppressShapeKeyMainPassGlow;
    }

    public static void setSuppressShapeKeyMainPassGlow(boolean suppress)
    {
        suppressShapeKeyMainPassGlow = suppress;
    }

    public static void beginCpuGeometry(ShaderProgram shader)
    {
        GlUniform glowingUniform = shader.getUniform("GlowingColor");

        glowingUniformActive = glowingUniform != null;
    }

    public static float getBaseGlowingStrength()
    {
        return baseGlowStrength;
    }

    public static float getBaseGlowingR()
    {
        return baseGlowR;
    }

    public static float getBaseGlowingG()
    {
        return baseGlowG;
    }

    public static float getBaseGlowingB()
    {
        return baseGlowB;
    }

    public static void clearPaint()
    {
        baseR = 0F;
        baseG = 0F;
        baseB = 0F;
        baseStrength = 0F;

        paintR = 0F;
        paintG = 0F;
        paintB = 0F;
        paintStrength = 0F;
    }

    public static void setTextureBlend(Link toTexture, float blend)
    {
        ModelVAORenderer.textureBlendActive = toTexture != null && blend > 0F && blend < 1F;
        ModelVAORenderer.textureBlendFactor = blend;
        ModelVAORenderer.textureBlendTo = toTexture;
    }

    public static void clearTextureBlend()
    {
        ModelVAORenderer.textureBlendActive = false;
        ModelVAORenderer.textureBlendFactor = 0F;
        ModelVAORenderer.textureBlendTo = null;
    }

    public static void setPaintEffectTransform(Matrix4f formRootInverseMatrix, EffectTransform transform, Vector3f maskHalf)
    {
        setPaintEffectTransform(formRootInverseMatrix, transform, maskHalf, true);
    }

    public static void setPaintEffectTransform(Matrix4f formRootInverseMatrix, EffectTransform transform, Vector3f maskHalf, boolean bottomAnchoredY)
    {
        if (formRootInverseMatrix != null)
        {
            formRootInverse.set(formRootInverseMatrix);
        }
        else
        {
            formRootInverse.identity();
        }

        EffectTransformMath.buildInverseMatrix(transform, paintEffectInverse);
        paintEffectActive = EffectTransformMath.isTransformActive(transform);
        paintMaskShape = transform == null || transform.shape == null ? 0F : transform.shape.id;

        if (maskHalf != null)
        {
            paintMaskHalf.set(maskHalf);
        }
        else
        {
            EffectTransformMath.resolveModelMaskHalfExtents(transform, paintMaskHalf);
        }

        paintMaskBottomAnchored = bottomAnchoredY;
    }

    public static void clearPaintEffectTransform()
    {
        if (!colorEffectActive)
        {
            formRootInverse.identity();
        }

        paintEffectInverse.identity();
        paintEffectActive = false;
        paintMaskBottomAnchored = true;
        paintMaskShape = 0F;
        paintMaskHalf.set(EffectTransformMath.MODEL_MASK_HALF_BASE, EffectTransformMath.MODEL_MASK_HALF_BASE * EffectTransformMath.MODEL_MASK_Y_BIAS, EffectTransformMath.MODEL_MASK_HALF_BASE);
    }

    public static void setColorEffectTransform(Matrix4f formRootInverseMatrix, EffectTransform transform, Vector3f maskHalf)
    {
        if (formRootInverseMatrix != null)
        {
            formRootInverse.set(formRootInverseMatrix);
        }
        else
        {
            formRootInverse.identity();
        }

        EffectTransformMath.buildInverseMatrix(transform, colorEffectInverse);
        colorEffectActive = EffectTransformMath.isTransformActive(transform);
        colorMaskShape = transform == null || transform.shape == null ? 0F : transform.shape.id;

        if (maskHalf != null)
        {
            colorMaskHalf.set(maskHalf);
        }
        else
        {
            EffectTransformMath.resolveModelMaskHalfExtents(transform, colorMaskHalf);
        }

        colorMaskBottomAnchored = true;
    }

    public static void setFormColorTint(float r, float g, float b, float a)
    {
        formColorR = r;
        formColorG = g;
        formColorB = b;
        formColorA = a;
        colorTintMasked = true;
    }

    public static void clearFormColorTint()
    {
        formColorR = 1F;
        formColorG = 1F;
        formColorB = 1F;
        formColorA = 1F;
        colorTintMasked = false;
    }

    public static void clearColorEffectTransform()
    {
        if (!paintEffectActive)
        {
            formRootInverse.identity();
        }

        colorEffectInverse.identity();
        colorEffectActive = false;
        colorMaskBottomAnchored = true;
        colorMaskShape = 0F;
        colorMaskHalf.set(EffectTransformMath.MODEL_MASK_HALF_BASE, EffectTransformMath.MODEL_MASK_HALF_BASE * EffectTransformMath.MODEL_MASK_Y_BIAS, EffectTransformMath.MODEL_MASK_HALF_BASE);
    }

    private static Matrix4f overlayFormRootInverse()
    {
        if (usesCapturedModelView())
        {
            return overlayFormRootInverse.identity();
        }

        return formRootInverse;
    }

    public static void render(ShaderProgram shader, IModelVAO modelVAO, MatrixStack stack, float r, float g, float b, float a, int light, int overlay)
    {
        int currentVAO = GL30.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int currentElementArrayBuffer = GL30.glGetInteger(GL30.GL_ELEMENT_ARRAY_BUFFER_BINDING);

        if (ModelVAORenderer.textureBlendActive && ModelVAORenderer.textureBlendTo != null)
        {
            BBSModClient.getTextures().bindTexture(ModelVAORenderer.textureBlendTo, 3);
        }

        setupUniforms(stack, shader);

        RenderSystem.setShader(() -> shader);
        shader.bind();
        mchorse.bbs_mod.utils.iris.ShaderOpacityPatch.reassertPostDeferredDepthState();
        modelVAO.render(shader.getFormat(), r, g, b, a, light, overlay);
        shader.unbind();

        GL30.glBindVertexArray(currentVAO);
        GL30.glBindBuffer(GL30.GL_ELEMENT_ARRAY_BUFFER, currentElementArrayBuffer);
    }

    public static void setupUniforms(MatrixStack stack, ShaderProgram shader)
    {
        for (int i = 0; i < 12; i++)
        {
            shader.addSampler("Sampler" + i, RenderSystem.getShaderTexture(i));
        }

        if (shader.projectionMat != null)
        {
            shader.projectionMat.set(RenderSystem.getProjectionMatrix());
        }

        if (shader.modelViewMat != null)
        {
            ModelVAORenderer.setModelViewUniform(stack, shader);
        }

        /* NormalMat is present by default in Iris' shaders, but when there is no Iris,
         * the BBS mod's model.json shader is being used instead that provides NormalMat
         * uniform.
         */
        GlUniform normalUniform = shader.getUniform("NormalMat");

        if (normalUniform != null)
        {
            normalUniform.set(stack.peek().getNormalMatrix());
        }

        GlUniform paintUniform = shader.getUniform("PaintColor");

        if (paintUniform != null)
        {
            paintUniform.set(paintR, paintG, paintB, paintStrength);
        }

        GlUniform glowingUniform = shader.getUniform("GlowingColor");

        glowingUniformActive = glowingUniform != null;

        if (glowingUniform != null)
        {
            glowingUniform.set(glowR, glowG, glowB, glowStrength);
        }

        GlUniform glowPaintOnlyUniform = shader.getUniform("GlowPaintOnly");

        if (glowPaintOnlyUniform != null)
        {
            glowPaintOnlyUniform.set(glowPaintOnly ? 1F : 0F);
        }

        GlUniform paintOverlayUniform = shader.getUniform("PaintOverlay");

        if (paintOverlayUniform != null)
        {
            paintOverlayUniform.set(paintOverlayPass ? 1F : 0F);
        }

        GlUniform textureBlendFactorUniform = shader.getUniform("TextureBlendFactor");

        if (textureBlendFactorUniform != null)
        {
            textureBlendFactorUniform.set(ModelVAORenderer.textureBlendActive ? ModelVAORenderer.textureBlendFactor : 0F);
        }

        GlUniform textureBlendActiveUniform = shader.getUniform("TextureBlendActive");

        if (textureBlendActiveUniform != null)
        {
            textureBlendActiveUniform.set(ModelVAORenderer.textureBlendActive ? 1F : 0F);
        }

        GlUniform formRootInverseUniform = shader.getUniform("FormRootInverse");

        if (formRootInverseUniform != null)
        {
            formRootInverseUniform.set(overlayFormRootInverse());
        }

        GlUniform paintEffectInverseUniform = shader.getUniform("PaintEffectInverse");

        if (paintEffectInverseUniform != null)
        {
            paintEffectInverseUniform.set(paintEffectInverse);
        }

        GlUniform paintEffectActiveUniform = shader.getUniform("PaintEffectActive");

        if (paintEffectActiveUniform != null)
        {
            paintEffectActiveUniform.set(paintEffectActive ? 1F : 0F);
        }

        GlUniform paintMaskHalfUniform = shader.getUniform("PaintMaskHalf");

        if (paintMaskHalfUniform != null)
        {
            paintMaskHalfUniform.set(paintMaskHalf.x, paintMaskHalf.y, paintMaskHalf.z);
        }

        GlUniform paintMaskBottomAnchoredUniform = shader.getUniform("PaintMaskBottomAnchored");

        if (paintMaskBottomAnchoredUniform != null)
        {
            paintMaskBottomAnchoredUniform.set(paintMaskBottomAnchored ? 1F : 0F);
        }

        GlUniform paintMaskShapeUniform = shader.getUniform("PaintMaskShape");

        if (paintMaskShapeUniform != null)
        {
            paintMaskShapeUniform.set(paintMaskShape);
        }

        GlUniform colorEffectInverseUniform = shader.getUniform("ColorEffectInverse");

        if (colorEffectInverseUniform != null)
        {
            colorEffectInverseUniform.set(colorEffectInverse);
        }

        GlUniform colorEffectActiveUniform = shader.getUniform("ColorEffectActive");

        if (colorEffectActiveUniform != null)
        {
            colorEffectActiveUniform.set(colorEffectActive ? 1F : 0F);
        }

        GlUniform colorMaskHalfUniform = shader.getUniform("ColorMaskHalf");

        if (colorMaskHalfUniform != null)
        {
            colorMaskHalfUniform.set(colorMaskHalf.x, colorMaskHalf.y, colorMaskHalf.z);
        }

        GlUniform colorMaskBottomAnchoredUniform = shader.getUniform("ColorMaskBottomAnchored");

        if (colorMaskBottomAnchoredUniform != null)
        {
            colorMaskBottomAnchoredUniform.set(colorMaskBottomAnchored ? 1F : 0F);
        }

        GlUniform colorMaskShapeUniform = shader.getUniform("ColorMaskShape");

        if (colorMaskShapeUniform != null)
        {
            colorMaskShapeUniform.set(colorMaskShape);
        }

        GlUniform formColorTintUniform = shader.getUniform("FormColorTint");

        if (formColorTintUniform != null)
        {
            formColorTintUniform.set(formColorR, formColorG, formColorB, formColorA);
        }

        GlUniform colorTintMaskedUniform = shader.getUniform("ColorTintMasked");

        if (colorTintMaskedUniform != null)
        {
            colorTintMaskedUniform.set(colorTintMasked ? 1F : 0F);
        }

        GlUniform colorTintOverlayUniform = shader.getUniform("ColorTintOverlay");

        if (colorTintOverlayUniform != null)
        {
            colorTintOverlayUniform.set(colorTintOverlayPass ? 1F : 0F);
        }

        /* After Iris composite, RenderSystem fog is often collapsed (FogEnd≈1) or left as
         * dense atmospheric fog — linear_fog then replaces the whole mesh with FogColor
         * (featureless sky-tinted silhouette, texture gone). Captured-matrix redraws already
         * sit on the final image; skip fog so low-opacity / render-depth fades keep albedo. */
        if (usesCapturedModelView())
        {
            if (shader.fogStart != null)
            {
                shader.fogStart.set(1_000_000F);
            }

            if (shader.fogEnd != null)
            {
                shader.fogEnd.set(1_000_001F);
            }

            if (shader.fogColor != null)
            {
                shader.fogColor.set(0F, 0F, 0F, 0F);
            }

            if (shader.fogShape != null)
            {
                shader.fogShape.set(0);
            }
        }
        else
        {
            if (shader.fogStart != null)
            {
                shader.fogStart.set(RenderSystem.getShaderFogStart());
            }

            if (shader.fogEnd != null)
            {
                shader.fogEnd.set(RenderSystem.getShaderFogEnd());
            }

            if (shader.fogColor != null)
            {
                shader.fogColor.set(RenderSystem.getShaderFogColor());
            }

            if (shader.fogShape != null)
            {
                shader.fogShape.set(RenderSystem.getShaderFogShape().getId());
            }
        }

        if (shader.colorModulator != null)
        {
            shader.colorModulator.set(1F, 1F, 1F, 1F);
        }

        if (shader.gameTime != null)
        {
            shader.gameTime.set(RenderSystem.getShaderGameTime());
        }

        if (shader.textureMat != null)
        {
            shader.textureMat.set(RenderSystem.getTextureMatrix());
        }

        RenderSystem.setupShaderLights(shader);
    }

    private static void setModelViewUniform(MatrixStack stack, ShaderProgram shader)
    {
        Matrix4f modelView;

        if (usesCapturedModelView())
        {
            /* Overlay/deferred stack already carries the full terrain + entity transform captured
             * at enqueue; RenderSystem model-view is identity during these draws. */
            modelView = new Matrix4f(stack.peek().getPositionMatrix());
        }
        else
        {
            modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(stack.peek().getPositionMatrix());
        }

        shader.modelViewMat.set(modelView);
    }
}