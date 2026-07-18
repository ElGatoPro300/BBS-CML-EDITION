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
 * Runtime opacity fix for Complementary / BSL. Translucent BBS forms are delayed until
 * after Iris {@code beginTranslucents} (VL clouds/fog already composited) so soft fades
 * never punch sky holes or let clouds draw over the mesh. Near-opaque ({@code #f2+}) stays
 * on the live Iris path with depth writes so Complementary shading is not flattened.
 * Fully opaque film {@code renderDepth} actors also use the post-deferred queue.
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
        "gbuffers_block_translucent",
        /* Billboard/shape deferred draws use position_tex_color → Iris basic/textured. */
        "gbuffers_basic",
        "gbuffers_textured",
        "gbuffers_textured_lit"
    };

    private static final List<PostDeferredEntry> postDeferredForms = new ArrayList<>();
    private static boolean postDeferredPhase;
    private static boolean flushingPostDeferred;
    private static boolean flushingDepthWrite = true;
    private static boolean forceLiveDepthWrite;
    private static boolean suppressLiveDepthWrite;

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

    public static void setSuppressLiveDepthWrite(boolean suppress)
    {
        suppressLiveDepthWrite = suppress;
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
        else if (suppressLiveDepthWrite)
        {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(org.lwjgl.opengl.GL11.GL_LEQUAL);
            RenderSystem.depthMask(false);
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
     * Near-opaque floor. Below this, forms join the post-deferred queue (after VL clouds).
     * At/above it they stay live with depth writes. {@code #f2} (≈0.949) sits just under
     * this gate so slight translucency still waits for clouds; fully solid keeps live shading.
     */
    public static final float LIVE_DEPTH_WRITE_ALPHA = 0.95F;

    /**
     * Queue translucent forms until after deferred/VL clouds so soft opacity never punches
     * sky holes or lets clouds composite over the mesh. Near-opaque stays live for pack
     * lighting. Opaque film {@code renderDepth} still joins. Never delay the shadow pass.
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

        /* Any visible translucency — including #e9 — must wait for clouds. */
        if (alpha < LIVE_DEPTH_WRITE_ALPHA)
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

    /**
     * Fallback only: if a translucent form somehow stays on the live path, do not stamp
     * depth before VL clouds ({@code z0 > 0.56} hole punch). Prefer
     * {@link #shouldDelayUntilPostDeferred} instead — suppress lets clouds draw over the mesh.
     */
    public static boolean shouldSuppressDepthWrite(float alpha)
    {
        if (!isActive() || flushingPostDeferred || alpha <= 0.001F || alpha >= LIVE_DEPTH_WRITE_ALPHA)
        {
            return false;
        }

        try
        {
            return mchorse.bbs_mod.client.BBSRendering.isIrisShadersEnabled()
                && !mchorse.bbs_mod.client.BBSRendering.isIrisShadowPass();
        }
        catch (Throwable t)
        {
            return false;
        }
    }

    public static boolean shouldForceLiveDepthWrite(float alpha)
    {
        /* Near-opaque live path only — translucents are post-deferred. */
        return isActive() && alpha >= LIVE_DEPTH_WRITE_ALPHA;
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
        /* After Iris beginTranslucents — VL clouds/fog already run; translucent BBS forms
         * draw here with depth so they sit in front of clouds without sky holes. */
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
                /* Same setup as ModelVAORenderer.submitDeferredTranslucentModel — identity MV
                 * + deferred translucent pass so billboards/shapes stay visible and depth-test. */
                MatrixStackUtils.pushIdentityModelView();
                mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer.beginDeferredTranslucentModelPass(entry.depthWrite, true);
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
                    mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer.endDeferredTranslucentModelPass();
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

        patched = LITERAL_POINT_ONE_COMPARE.matcher(patched).replaceAll("$1.a < " + LOW_ALPHA_TEST_REF);

        patched = processShadowCasterAlpha(patched);

        return processShadowOpacity(patched);
    }

    /**
     * Complementary/BSL shadow map programs: multiply texture alpha by vertex color alpha
     * and dither-discard so per-model replay {@code shadow_opacity} can lighten/darken
     * individual casters (hard shadow maps are otherwise binary).
     */
    public static String processShadowCasterAlpha(String source)
    {
        if (!isActive() || source == null || source.isEmpty())
        {
            return source;
        }

        if (source.contains("BBS_SHADOW_CASTER_DITHER"))
        {
            return source;
        }

        /* Complementary shadow.glsl */
        if (source.contains("DoNaturalShadowCalculation") && source.contains("gl_FragData[0] = color1;"))
        {
            String dither =
                "/* BBS_SHADOW_CASTER_DITHER */\n"
                    + "    color1.a *= glColor.a;\n"
                    + "    if (color1.a < 0.999){\n"
                    + "        float bbsShadowDither = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453);\n"
                    + "        if (bbsShadowDither > color1.a) discard;\n"
                    + "    }\n";

            return source.replace(
                "    /* DRAWBUFFERS:0 */\n    gl_FragData[0] = color1; // Shadow Color",
                dither + "    /* DRAWBUFFERS:0 */\n    gl_FragData[0] = color1; // Shadow Color"
            );
        }

        /* BSL shadow.glsl */
        if (source.contains("float premult = float(mat > 0.98") && source.contains("gl_FragData[0] = albedo;"))
        {
            String dither =
                "\t/* BBS_SHADOW_CASTER_DITHER */\n"
                    + "\talbedo.a *= color.a;\n"
                    + "\tif (albedo.a < 0.999){\n"
                    + "\t\tfloat bbsShadowDither = fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453);\n"
                    + "\t\tif (bbsShadowDither > albedo.a) discard;\n"
                    + "\t}\n";

            if (source.contains("gl_FragData[0] = albedo;"))
            {
                return source.replace("\tgl_FragData[0] = albedo;", dither + "\tgl_FragData[0] = albedo;");
            }
        }

        return source;
    }

    /**
     * Injects {@code bbs_shader_shadow_opacity} into Complementary/BSL shaders that sample
     * shadow maps and scales sampled shadow visibility: 1 = full shadows, 0 = no shadows.
     */
    public static String processShadowOpacity(String source)
    {
        if (!isActive() || source == null || source.isEmpty())
        {
            return source;
        }

        if (!containsShadowSampler(source))
        {
            return source;
        }

        ensureShadowOpacityVariable();

        String patched = insertShadowOpacityHelpers(source);

        patched = wrapShadowTextureCalls(patched, "texture");
        patched = wrapShadowTextureCalls(patched, "texture2D");
        patched = wrapShadowTextureCalls(patched, "textureLod");
        patched = wrapShadowTextureCalls(patched, "textureGrad");
        patched = wrapShadowTextureCalls(patched, "shadow2D");
        patched = wrapShadowTextureCalls(patched, "shadow2DLod");

        return patched;
    }

    public static void ensureShadowOpacityVariable()
    {
        if (!isActive())
        {
            return;
        }

        ShaderCurves.ShaderVariable variable = ShaderCurves.variableMap.get(ShaderCurves.SHADER_SHADOW_OPACITY);

        if (variable == null)
        {
            variable = new ShaderCurves.ShaderVariable(ShaderCurves.SHADER_SHADOW_OPACITY, "1.0", false);
            ShaderCurves.variableMap.put(ShaderCurves.SHADER_SHADOW_OPACITY, variable);
        }

        syncShadowOpacityDefault(variable);
    }

    public static void syncShadowOpacityDefault()
    {
        ShaderCurves.ShaderVariable variable = ShaderCurves.variableMap.get(ShaderCurves.SHADER_SHADOW_OPACITY);

        if (variable != null)
        {
            syncShadowOpacityDefault(variable);
        }
    }

    private static void syncShadowOpacityDefault(ShaderCurves.ShaderVariable variable)
    {
        float value = 1F;

        if (BBSSettings.shaderShadowOpacity != null)
        {
            value = BBSSettings.shaderShadowOpacity.get();
        }

        variable.defaultValue = Math.max(0F, Math.min(1F, value));
    }

    private static boolean containsShadowSampler(String source)
    {
        return source.contains("shadowtex0")
            || source.contains("shadowtex1")
            || source.contains("shadowtex0HW")
            || source.contains("shadowtex1HW")
            || source.contains("waterShadow");
    }

    private static String insertShadowOpacityHelpers(String source)
    {
        String uniform = "bbs_" + ShaderCurves.SHADER_SHADOW_OPACITY;

        if (source.contains(uniform))
        {
            return source;
        }

        int version = source.indexOf("#version");

        if (version < 0)
        {
            return source;
        }

        int nextNewLine = source.indexOf('\n', version);

        if (nextNewLine < 0)
        {
            return source;
        }

        String helpers =
            "uniform float " + uniform + ";\n"
                + "#ifndef BBS_SHADOW_OPACITY_HELPERS\n"
                + "#define BBS_SHADOW_OPACITY_HELPERS\n"
                + "float bbsApplyShadowOpacity(float s){return mix(1.0,s," + uniform + ");}\n"
                + "vec2 bbsApplyShadowOpacity(vec2 s){return mix(vec2(1.0),s," + uniform + ");}\n"
                + "vec3 bbsApplyShadowOpacity(vec3 s){return mix(vec3(1.0),s," + uniform + ");}\n"
                + "vec4 bbsApplyShadowOpacity(vec4 s){return mix(vec4(1.0),s," + uniform + ");}\n"
                + "#endif\n";

        return source.substring(0, nextNewLine + 1) + helpers + source.substring(nextNewLine + 1);
    }

    /**
     * Wraps {@code func(shadowtexN...)} calls with {@code bbsApplyShadowOpacity(...)} so pack
     * lighting still runs, but shadow darkness scales with the BBS uniform / curve.
     */
    private static String wrapShadowTextureCalls(String source, String functionName)
    {
        String marker = "bbsApplyShadowOpacity(";
        StringBuilder out = new StringBuilder(source.length() + 64);
        int i = 0;

        while (i < source.length())
        {
            int found = indexOfIdentifierCall(source, functionName, i);

            if (found < 0)
            {
                out.append(source, i, source.length());
                break;
            }

            out.append(source, i, found);

            int open = found + functionName.length();

            while (open < source.length() && Character.isWhitespace(source.charAt(open)))
            {
                open++;
            }

            if (open >= source.length() || source.charAt(open) != '(')
            {
                out.append(source, found, found + functionName.length());
                i = found + functionName.length();
                continue;
            }

            int close = findMatchingParen(source, open);

            if (close < 0)
            {
                out.append(source, found, source.length());
                break;
            }

            String call = source.substring(found, close + 1);
            String args = source.substring(open + 1, close).trim();

            if (isShadowSamplerArg(args) && !isAlreadyWrapped(source, found, marker))
            {
                out.append(marker).append(call).append(')');
            }
            else
            {
                out.append(call);
            }

            i = close + 1;
        }

        return out.toString();
    }

    private static boolean isAlreadyWrapped(String source, int callStart, String marker)
    {
        int lookBehind = Math.max(0, callStart - marker.length() - 8);
        String before = source.substring(lookBehind, callStart);

        return before.contains(marker);
    }

    private static boolean isShadowSamplerArg(String args)
    {
        if (args.isEmpty())
        {
            return false;
        }

        int comma = findTopLevelComma(args);
        String sampler = (comma < 0 ? args : args.substring(0, comma)).trim();

        return sampler.equals("shadowtex0")
            || sampler.equals("shadowtex1")
            || sampler.equals("shadowtex0HW")
            || sampler.equals("shadowtex1HW")
            || sampler.equals("waterShadow");
    }

    private static int findTopLevelComma(String args)
    {
        int depth = 0;

        for (int i = 0; i < args.length(); i++)
        {
            char c = args.charAt(i);

            if (c == '(')
            {
                depth++;
            }
            else if (c == ')')
            {
                depth--;
            }
            else if (c == ',' && depth == 0)
            {
                return i;
            }
        }

        return -1;
    }

    private static int indexOfIdentifierCall(String source, String name, int from)
    {
        int index = from;

        while (index < source.length())
        {
            int found = source.indexOf(name, index);

            if (found < 0)
            {
                return -1;
            }

            boolean startOk = found == 0 || !isIdentChar(source.charAt(found - 1));
            int after = found + name.length();
            boolean endOk = after >= source.length() || !isIdentChar(source.charAt(after));

            if (startOk && endOk)
            {
                int probe = after;

                while (probe < source.length() && Character.isWhitespace(source.charAt(probe)))
                {
                    probe++;
                }

                if (probe < source.length() && source.charAt(probe) == '(')
                {
                    return found;
                }
            }

            index = found + 1;
        }

        return -1;
    }

    private static boolean isIdentChar(char c)
    {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static int findMatchingParen(String source, int openIndex)
    {
        int depth = 0;

        for (int i = openIndex; i < source.length(); i++)
        {
            char c = source.charAt(i);

            if (c == '(')
            {
                depth++;
            }
            else if (c == ')')
            {
                depth--;

                if (depth == 0)
                {
                    return i;
                }
            }
        }

        return -1;
    }
}
