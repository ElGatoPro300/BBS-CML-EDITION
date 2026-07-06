package mchorse.bbs_mod.cubic.render.vao;

import mchorse.bbs_mod.client.BBSRendering;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

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

    /* When true, the model is being drawn as a shader-pack paint overlay pass: every group samples a flat
     * white texture and uses the paint colour as its vertex colour, so the albedo becomes the paint colour
     * (not the original skin texture) even when an external shader pack replaced the BBS model shader. */
    private static boolean paintPass;
    private static boolean paintOverlayPass;

    /* 1x1 white texture used as the albedo source during the paint overlay pass. */
    private static NativeImageBackedTexture whiteTexture;

    /* Saved GL state for the paint overlay pass (restored in endPaintOverlayPass). */
    private static int savedDepthFunc;
    private static boolean savedDepthMask;
    private static boolean savedPolygonOffsetFill;

    public static void beginPaintPass()
    {
        paintPass = true;
    }

    public static void endPaintPass()
    {
        paintPass = false;
    }

    /**
     * Second-pass paint overlay for external shader packs. Draws the same geometry again with a flat white
     * texture and paint as vertex colour, alpha-blended on top of the first pass. Uses LEQUAL depth with a
     * slight polygon offset (avoids z-fighting streaks) and no depth writes (avoids silhouette halos).
     * All GL state is saved and fully restored so later world/UI rendering is not corrupted.
     */
    public static void beginPaintOverlayPass()
    {
        beginPaintPass();
        paintOverlayPass = true;

        savedDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        savedPolygonOffsetFill = GL11.glGetBoolean(GL11.GL_POLYGON_OFFSET_FILL);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_ALWAYS);
        RenderSystem.depthMask(false);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(1F, 1F);
    }

    public static void endPaintOverlayPass()
    {
        if (savedPolygonOffsetFill)
        {
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        }
        else
        {
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        }

        endPaintPass();
        paintOverlayPass = false;

        RenderSystem.depthMask(savedDepthMask);
        RenderSystem.depthFunc(savedDepthFunc);
        RenderSystem.defaultBlendFunc();
    }

    public static boolean isPaintOverlayPass()
    {
        return paintOverlayPass;
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

    public static void render(ShaderProgram shader, IModelVAO modelVAO, MatrixStack stack, float r, float g, float b, float a, int light, int overlay)
    {
        int currentVAO = GL30.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int currentElementArrayBuffer = GL30.glGetInteger(GL30.GL_ELEMENT_ARRAY_BUFFER_BINDING);

        setupUniforms(stack, shader);

        shader.bind();

        int textureID = RenderSystem.getShaderTexture(0);
        GlStateManager._activeTexture(GL30.GL_TEXTURE0);
        GlStateManager._bindTexture(textureID);

        modelVAO.render(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, r, g, b, a, light, overlay);
        shader.unbind();

        GL30.glBindVertexArray(currentVAO);
        GL30.glBindBuffer(GL30.GL_ELEMENT_ARRAY_BUFFER, currentElementArrayBuffer);
    }

    public static void setupUniforms(MatrixStack stack, ShaderProgram shader)
    {
        Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(stack.peek().getPositionMatrix());

        for (int i = 0; i < 12; i++)
        {
            shader.addSamplerTexture("Sampler" + i, RenderSystem.getShaderTexture(i));
        }

        if (shader.projectionMat != null)
        {
            shader.projectionMat.set(RenderSystem.getProjectionMatrix());
        }

        if (shader.modelViewMat != null)
        {
            shader.modelViewMat.set(modelView);
        }

        /* NormalMat is present by default in Iris' shaders, but when there is no Iris,
         * the BBS mod's model.json shader is being used instead that provides NormalMat
         * uniform.
         */
        GlUniform normalUniform = shader.getUniform("NormalMat");

        if (normalUniform != null)
        {
            if (BBSRendering.isIrisShadersEnabled())
            {
                normalUniform.set(modelView.normal(new Matrix3f()));
            }
            else
            {
                normalUniform.set(stack.peek().getNormalMatrix());
            }
        }

        Fog fog = RenderSystem.getShaderFog();
        GlUniform paintUniform = shader.getUniform("PaintColor");

        if (paintUniform != null)
        {
            paintUniform.set(paintR, paintG, paintB, paintStrength);
        }

        GlUniform paintOverlayUniform = shader.getUniform("PaintOverlay");

        if (paintOverlayUniform != null)
        {
            paintOverlayUniform.set(paintOverlayPass ? 1F : 0F);
        }

        if (shader.fogStart != null)
        {
            shader.fogStart.set(fog.start());
        }

        if (shader.fogEnd != null)
        {
            shader.fogEnd.set(fog.end());
        }

        if (shader.fogColor != null)
        {
            shader.fogColor.set(fog.red(), fog.green(), fog.blue(), fog.alpha());
        }

        if (shader.fogShape != null)
        {
            shader.fogShape.set(fog.shape().getId());
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
}
