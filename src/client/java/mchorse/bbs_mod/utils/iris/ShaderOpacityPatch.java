package mchorse.bbs_mod.utils.iris;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.mixin.client.iris.IrisRenderingPipelineAccessor;
import mchorse.bbs_mod.utils.MatrixStackUtils;

import net.irisshaders.iris.gl.blending.AlphaTest;
import net.irisshaders.iris.gl.blending.AlphaTestFunction;
import net.irisshaders.iris.helpers.OptionalBoolean;
import net.irisshaders.iris.shaderpack.properties.ShaderProperties;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;

import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;

/**
 * Runtime opacity fix for Complementary / BSL. Keeps mid/high-opacity BBS forms on the Iris
 * lighting path (pack shading + shadow maps + depth). Only very low alpha is deferred past
 * VL clouds. Never suppress live depth for near-opaque alphas — that flattened Complementary
 * shading (e.g. at {@code #f2}).
 */
public class ShaderOpacityPatch
{
    public static final float LOW_ALPHA_TEST_REF = 0.0001F;

    private static final Pattern ALPHA_TEST_REF_COMPARE = Pattern.compile(
        "\\b([A-Za-z_][\\w.]*)\\.a\\s*<\\s*alphaTestRef\\b"
    );
    private static final Pattern LITERAL_POINT_ONE_COMPARE = Pattern.compile(
        "\\b([A-Za-z_][\\w.]*)\\.a\\s*<\\s*0\\.1\\b"
    );

    private static final String[] ALPHA_TEST_PASSES = {
        "gbuffers_entities",
        "gbuffers_entities_translucent",
        "gbuffers_block",
        "gbuffers_block_translucent"
    };

    private static final List<PostDeferredEntry> postDeferredForms = new ArrayList<>();
    private static boolean postDeferredPhase;
    private static boolean flushingPostDeferred;
    private static boolean flushingDepthWrite = true;
    private static boolean forceLiveDepthWrite;

    private static String loadingPackName = "";

    private static final class PostDeferredEntry
    {
        private final double renderDepth;
        private final double distanceSq;
        private final boolean depthWrite;
        private final boolean irisCamera;
        private final Matrix4f projection;
        private final Matrix4f modelView;
        private final Runnable draw;

        private PostDeferredEntry(double renderDepth, double distanceSq, boolean depthWrite, boolean irisCamera, Matrix4f projection, Matrix4f modelView, Runnable draw)
        {
            this.renderDepth = renderDepth;
            this.distanceSq = distanceSq;
            this.depthWrite = depthWrite;
            this.irisCamera = irisCamera;
            this.projection = projection;
            this.modelView = modelView;
            this.draw = draw;
        }
    }

    public static void setLoadingPackName(String name)
    {
        loadingPackName = name == null ? "" : name;
    }

    public static void clearLoadingPackName()
    {
        loadingPackName = "";
    }

    public static boolean isComplementaryPack(String name)
    {
        return name != null && name.toLowerCase(Locale.ROOT).contains("complementary");
    }

    public static boolean isBslPack(String name)
    {
        return name != null && name.toLowerCase(Locale.ROOT).contains("bsl");
    }

    public static boolean isActive()
    {
        String pack = resolvePackName();

        if (pack.isEmpty())
        {
            return false;
        }

        if (BBSSettings.complementaryOpacityFix != null && BBSSettings.complementaryOpacityFix.get()
            && isComplementaryPack(pack))
        {
            return true;
        }

        return BBSSettings.bslOpacityFix != null && BBSSettings.bslOpacityFix.get() && isBslPack(pack);
    }

    private static String resolvePackName()
    {
        if (loadingPackName != null && !loadingPackName.isEmpty())
        {
            return loadingPackName;
        }

        try
        {
            String current = net.irisshaders.iris.Iris.getCurrentPackName();

            return current == null ? "" : current;
        }
        catch (Throwable t)
        {
            return "";
        }
    }

    public static boolean isFlushingPostDeferred()
    {
        return flushingPostDeferred;
    }

    public static void setForceLiveDepthWrite(boolean force)
    {
        forceLiveDepthWrite = force;
    }

    public static void reassertPostDeferredDepthState()
    {
        if (flushingPostDeferred)
        {
            reassertPostDeferredDepthState(flushingDepthWrite);

            return;
        }

        if (forceLiveDepthWrite)
        {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
        }
    }

    public static void reassertPostDeferredDepthState(boolean depthWrite)
    {
        if (!flushingPostDeferred)
        {
            return;
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
        RenderSystem.depthMask(depthWrite);
    }

    /**
     * True while Iris is in the post-deferred translucent phase (after clouds are composited).
     */
    public static boolean isPostDeferredPhase()
    {
        return postDeferredPhase;
    }

    /**
     * Below this, translucent forms may join the post-deferred queue to avoid VL sky holes.
     * Above it they stay on the live Iris path so pack shading and ground shadows remain.
     */
    public static final float POST_DEFERRED_TRANSLUCENT_ALPHA = 0.35F;

    /**
     * Queue forms until after deferred so VL clouds composite first. Only very translucent
     * forms (or opaque film {@code renderDepthFrame}) join — mid/high opacity stays live for
     * pack lighting and shadow maps. Never delay during the shadow pass.
     */
    public static boolean shouldDelayUntilPostDeferred(float alpha)
    {
        return shouldDelayUntilPostDeferred(alpha, false);
    }

    public static boolean shouldDelayUntilPostDeferred(float alpha, boolean filmRenderDepth)
    {
        if (!isActive() || postDeferredPhase || flushingPostDeferred || alpha <= 0.001F)
        {
            return false;
        }

        try
        {
            if (!mchorse.bbs_mod.client.BBSRendering.isIrisShadersEnabled())
            {
                return false;
            }

            /* Casters must hit the shadow map live — post-deferred never writes shadows. */
            if (mchorse.bbs_mod.client.BBSRendering.isIrisShadowPass())
            {
                return false;
            }
        }
        catch (Throwable t)
        {
            return false;
        }

        /* Very translucent only — mid/near-opaque (#f2 etc.) must stay live even in the film
         * editor; delaying them when renderDepthFrame is set flattened Complementary shading. */
        if (alpha < POST_DEFERRED_TRANSLUCENT_ALPHA)
        {
            return true;
        }

        /* Fully opaque film actors may still share the post-deferred depth queue. */
        return filmRenderDepth && alpha >= 0.999F;
    }

    public static boolean shouldJoinPostDeferredQueue(float alpha)
    {
        return shouldDelayUntilPostDeferred(alpha, true);
    }

    public static boolean shouldForceLiveDepthWrite(float alpha)
    {
        /* Any live Iris draw under the opacity patch needs depth for Complementary shading.
         * Only post-deferred very-low-alpha skips the live path entirely. */
        return isActive() && alpha >= POST_DEFERRED_TRANSLUCENT_ALPHA;
    }

    /**
     * Iris-lit mesh: restore camera ModelView; pass entity-local stack matrices in {@code draw}.
     */
    public static void submitPostDeferredForm(double renderDepth, boolean depthWrite, Runnable draw)
    {
        submit(renderDepth, 0D, depthWrite, true, draw);
    }

    public static void submitPostDeferredForm(double renderDepth, double distanceSq, boolean depthWrite, Runnable draw)
    {
        submit(renderDepth, distanceSq, depthWrite, true, draw);
    }

    /**
     * BBS model-shader flat: identity ModelView; pass camera-baked stack matrices in {@code draw}.
     */
    public static void submitPostDeferredBbsForm(double renderDepth, boolean depthWrite, Runnable draw)
    {
        submit(renderDepth, 0D, depthWrite, false, draw);
    }

    public static void submitPostDeferredBbsForm(double renderDepth, double distanceSq, boolean depthWrite, Runnable draw)
    {
        submit(renderDepth, distanceSq, depthWrite, false, draw);
    }

    public static void submitPostDeferredForm(Runnable draw)
    {
        submitPostDeferredForm(0D, 0D, true, draw);
    }

    private static void submit(double renderDepth, double distanceSq, boolean depthWrite, boolean irisCamera, Runnable draw)
    {
        if (draw == null)
        {
            return;
        }

        postDeferredForms.add(new PostDeferredEntry(
            renderDepth,
            distanceSq,
            depthWrite,
            irisCamera,
            new Matrix4f(RenderSystem.getProjectionMatrix()),
            new Matrix4f(RenderSystem.getModelViewMatrix()),
            draw
        ));
    }

    public static void onBeginTranslucents()
    {
        postDeferredPhase = true;
        flushPostDeferredForms();
    }

    public static void onWorldRenderBegin()
    {
        postDeferredForms.clear();
        postDeferredPhase = false;
        flushingPostDeferred = false;
    }

    public static void onWorldRenderEnd()
    {
        flushPostDeferredForms();
        postDeferredPhase = false;
    }

    public static void flushPostDeferredForms()
    {
        if (postDeferredForms.isEmpty())
        {
            return;
        }

        flushingPostDeferred = true;

        try
        {
            /* Same order as film entities: lower render depth first; within a depth, farther
             * first so closer forms depth-test against what is already in the buffer. */
            postDeferredForms.sort(Comparator
                .comparingDouble((PostDeferredEntry entry) -> entry.renderDepth)
                .thenComparing((PostDeferredEntry a, PostDeferredEntry b) -> Double.compare(b.distanceSq, a.distanceSq))
            );

            preparePostDeferredFramebufferAndDepth();

            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();

            if (mc != null && mc.gameRenderer != null)
            {
                mc.gameRenderer.getLightmapTextureManager().enable();
                mc.gameRenderer.getOverlayTexture().setupOverlayColor();
            }

            for (PostDeferredEntry entry : postDeferredForms)
            {
                runEntry(entry);
            }
        }
        finally
        {
            postDeferredForms.clear();
            flushingPostDeferred = false;
            RenderSystem.depthMask(true);
        }
    }

    /**
     * Complementary/BSL deferred can leave the live depth buffer unusable for occlusion. Iris
     * snapshots opaque depth into {@code depthtex1} at {@code beginTranslucents}; copy it back
     * so translucent BBS forms depth-test against models/terrain in front (render depth).
     */
    private static void preparePostDeferredFramebufferAndDepth()
    {
        try
        {
            mchorse.bbs_mod.client.BBSRendering.ensurePaintOverlayTargetFramebuffer();

            net.irisshaders.iris.pipeline.WorldRenderingPipeline pipeline =
                net.irisshaders.iris.Iris.getPipelineManager().getPipelineNullable();

            if (!(pipeline instanceof net.irisshaders.iris.pipeline.IrisRenderingPipeline irisPipeline))
            {
                return;
            }

            IrisRenderingPipelineAccessor access = (IrisRenderingPipelineAccessor) irisPipeline;
            net.irisshaders.iris.targets.RenderTargets targets = access.bbs$renderTargets();

            if (targets == null)
            {
                return;
            }

            int width = targets.getCurrentWidth();
            int height = targets.getCurrentHeight();
            int opaqueDepth = targets.getDepthTextureNoTranslucents().getTextureId();
            int liveDepth = targets.getDepthTexture();

            if (width > 0 && height > 0 && opaqueDepth > 0 && liveDepth > 0)
            {
                net.irisshaders.iris.gl.texture.DepthCopyStrategy.fastest(false)
                    .copy(null, opaqueDepth, null, liveDepth, width, height);
            }

            access.bbs$bindDefault();
        }
        catch (Throwable ignored)
        {
            /* Iris API drift — still attempt draws with whatever depth is bound. */
        }
    }

    private static void runEntry(PostDeferredEntry entry)
    {
        Matrix4f savedProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        Matrix4f savedModelView = new Matrix4f(RenderSystem.getModelViewMatrix());
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        boolean savedDepthMask = org.lwjgl.opengl.GL11.glGetBoolean(org.lwjgl.opengl.GL11.GL_DEPTH_WRITEMASK);

        try
        {
            RenderSystem.setProjectionMatrix(entry.projection, VertexSorter.BY_Z);
            flushingDepthWrite = entry.depthWrite;
            RenderSystem.depthMask(entry.depthWrite);

            if (entry.irisCamera)
            {
                modelViewStack.pushMatrix();
                modelViewStack.set(entry.modelView);
                RenderSystem.applyModelViewMatrix();
            }
            else
            {
                MatrixStackUtils.pushIdentityModelView();
            }

            try
            {
                reassertPostDeferredDepthState(entry.depthWrite);
                entry.draw.run();
            }
            finally
            {
                if (entry.irisCamera)
                {
                    modelViewStack.popMatrix();
                    RenderSystem.applyModelViewMatrix();
                }
                else
                {
                    MatrixStackUtils.popModelView();
                }
            }
        }
        finally
        {
            RenderSystem.depthMask(savedDepthMask);
            RenderSystem.setProjectionMatrix(savedProjection, VertexSorter.BY_Z);
            modelViewStack.pushMatrix();
            modelViewStack.set(savedModelView);
            RenderSystem.applyModelViewMatrix();
            modelViewStack.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }

    public static String patchPropertiesContents(String contents)
    {
        if (!isActive() || contents == null)
        {
            return contents;
        }

        if (contents.contains("separateEntityDraws"))
        {
            return contents.replaceAll("(?m)^\\s*separateEntityDraws\\s*=.*$", "separateEntityDraws=true");
        }

        return "separateEntityDraws=true\n" + contents;
    }

    public static void applyAlphaTestOverrides(ShaderProperties properties)
    {
        if (!isActive() || properties == null)
        {
            return;
        }

        Object2ObjectMap<String, AlphaTest> map = properties.getAlphaTestOverrides();
        AlphaTest low = new AlphaTest(AlphaTestFunction.GREATER, LOW_ALPHA_TEST_REF);

        for (String pass : ALPHA_TEST_PASSES)
        {
            map.put(pass, low);
        }
    }

    public static void applySeparateEntityDraws(Consumer<OptionalBoolean> setter)
    {
        if (!isActive() || setter == null)
        {
            return;
        }

        setter.accept(OptionalBoolean.TRUE);
    }

    public static String processSource(String source)
    {
        if (!isActive() || source == null || source.isEmpty())
        {
            return source;
        }

        /* Only relax alpha discards. Do not rewrite translucentMult — forcing it to 1.0 made
         * Complementary show full-brightness sky through forms (washed / background-tinted). */
        String patched = ALPHA_TEST_REF_COMPARE.matcher(source).replaceAll("$1.a < " + LOW_ALPHA_TEST_REF);

        return LITERAL_POINT_ONE_COMPARE.matcher(patched).replaceAll("$1.a < " + LOW_ALPHA_TEST_REF);
    }
}
