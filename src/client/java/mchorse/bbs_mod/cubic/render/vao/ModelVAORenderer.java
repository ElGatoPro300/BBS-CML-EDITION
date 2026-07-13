package mchorse.bbs_mod.cubic.render.vao;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.client.render.picker.BBSPickerRenderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;

public class ModelVAORenderer
{
    /* Paint overlay state. Used by CubicVAORenderer and ModelFormRenderer */
    private static float baseR;
    private static float baseG;
    private static float baseB;
    private static float baseStrength;

    private static float paintR;
    private static float paintG;
    private static float paintB;
    private static float paintStrength;
    /* When true, the model is being drawn as a shader-pack paint overlay pass. Groups still sample their
     * real skin texture so transparent UV regions are discarded; only textured pixels receive paint. */
    private static boolean paintPass;
    private static boolean paintOverlayPass;
    private static boolean paintOverlaySynced;


    public static void beginPaintPass()
    {
        paintPass = true;
    }

    public static void endPaintPass()
    {
        paintPass = false;
    }


    public static boolean isPaintOverlayPass()
    {
        return paintOverlayPass;
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
        if (strength > 0F)
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
    }

    public static boolean isGlowingUniformActive()
    {
        return glowingUniformActive;
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
    /**
     * Draw an {@link IModelVAO} through the immediate model RenderLayer. The 1.21.11 rewrite removed
     * ShaderProgram.bind()/unbind() and the imperative uniform/sampler/fog/light setup; the built-in
     * uniforms now live in the std140 UBOs (DynamicTransforms/Projection/Fog/Lighting) that
     * {@link BBSShaders#getModelLayer()} uploads per draw. The geometry is baked CPU-side into a
     * BufferBuilder (matching the cubic immediate path) and submitted through that layer.
     */
    public static void render(IModelVAO modelVAO, MatrixStack stack, float r, float g, float b, float a, int light, int overlay)
    {
        BuiltBuffer built = write(modelVAO, stack, r, g, b, a, light, overlay);

        if (built != null)
        {
            BBSShaders.getModelLayer().draw(built);
        }
    }

    /**
     * Draw an {@link IModelVAO} through the picker_models pipeline into the active picking target,
     * for stencil/picking passes. Replaces the old {@code ModelVAORenderer.render(pickerShader, ...)}
     * overload; the picker uniform (Target index) is uploaded by {@link BBSPickerRenderer}.
     */
    public static void renderPicking(IModelVAO modelVAO, MatrixStack stack, float r, float g, float b, float a, int light, int overlay)
    {
        BuiltBuffer built = write(modelVAO, stack, r, g, b, a, light, overlay);

        if (built != null)
        {
            BBSPickerRenderer.draw(BBSShaders.getPickerModelsProgram(), built, RenderSystem.getModelViewMatrix());
        }
    }

    private static BuiltBuffer write(IModelVAO modelVAO, MatrixStack stack, float r, float g, float b, float a, int light, int overlay)
    {
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);

        modelVAO.writeImmediate(builder, stack, r, g, b, a, light, overlay);

        return builder.endNullable();
    }
    private static void setModelViewUniform(MatrixStack stack, ShaderProgram shader)
    {
        Matrix4f modelView;

        if (paintOverlayPass)
        {
            /* Overlay stack already carries the full terrain + entity transform captured at enqueue;
             * RenderSystem model-view is identity during overlay draws. */
            modelView = new Matrix4f(stack.peek().getPositionMatrix());
        }
        else
        {
            modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(stack.peek().getPositionMatrix());
        }

        shader.modelViewMat.set(modelView);
    }
}
