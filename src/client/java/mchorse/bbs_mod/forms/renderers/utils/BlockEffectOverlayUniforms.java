package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.forms.forms.utils.EffectTransformMath;
import mchorse.bbs_mod.utils.colors.Color;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.screen.PlayerScreenHandler;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

/**
 * Uploads spatial paint mask uniforms for block/billboard overlay shaders.
 */
public final class BlockEffectOverlayUniforms
{
    private static final Matrix4f formRootInverse = new Matrix4f();
    private static final Matrix4f paintEffectInverse = new Matrix4f();
    private static final Vector3f paintMaskHalf = new Vector3f(0.5F, 0.5F, 0.5F);

    private BlockEffectOverlayUniforms()
    {}

    public static boolean hasPaintOverlayShader()
    {
        return BBSShaders.getBlockPaintOverlayProgram() != null;
    }

    public static void configurePaintOverlayRenderState(EffectTransform transform)
    {
        configurePaintOverlayRenderState(null, transform, true, null, null, 0F, 1F);
    }

    public static void configurePaintOverlayRenderState(EffectTransform transform, GlowSettings glow, Color legacyGlow, float glowIntensity, float alpha)
    {
        configurePaintOverlayRenderState(null, transform, true, glow, legacyGlow, glowIntensity, alpha);
    }

    public static void configurePaintOverlayRenderState(Matrix4f rootInverse, EffectTransform transform, GlowSettings glow, Color legacyGlow, float glowIntensity, float alpha)
    {
        configurePaintOverlayRenderState(rootInverse, transform, true, glow, legacyGlow, glowIntensity, alpha);
    }

    public static void configurePaintOverlayRenderState(Matrix4f rootInverse, EffectTransform transform, boolean bottomAnchored, GlowSettings glow, Color legacyGlow, float glowIntensity, float alpha)
    {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        ShaderProgram program = BBSShaders.getBlockPaintOverlayProgram();

        if (program != null)
        {
            RenderSystem.setShader(() -> program);
            bindFormRootInverse(program, rootInverse);
            bindPaint(program, transform, bottomAnchored);
            bindGlowOverlay(program, glow, legacyGlow, glowIntensity, alpha);
        }

        RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    public static void bindFormRootInverse(ShaderProgram shader, Matrix4f rootInverse)
    {
        if (shader == null)
        {
            return;
        }

        if (rootInverse != null)
        {
            formRootInverse.set(rootInverse);
        }
        else
        {
            formRootInverse.identity();
        }

        GlUniform uniform = shader.getUniform("FormRootInverse");

        if (uniform != null)
        {
            uniform.set(formRootInverse);
        }
    }

    public static void bindPaint(ShaderProgram shader, EffectTransform transform)
    {
        bindPaint(shader, transform, true);
    }

    public static void bindPaint(ShaderProgram shader, EffectTransform transform, boolean bottomAnchored)
    {
        if (shader == null)
        {
            return;
        }

        boolean active = EffectTransformMath.isTransformActive(transform);

        if (active)
        {
            EffectTransformMath.buildInverseMatrix(transform, paintEffectInverse);

            if (bottomAnchored)
            {
                EffectTransformMath.resolveBlockMaskHalfExtents(transform, paintMaskHalf);
            }
            else
            {
                EffectTransformMath.resolveItemMaskHalfExtents(transform, paintMaskHalf);
            }
        }
        else
        {
            paintEffectInverse.identity();

            if (bottomAnchored)
            {
                paintMaskHalf.set(0.5F, 0.5F, 0.5F);
            }
            else
            {
                EffectTransformMath.resolveItemMaskHalfExtents(null, paintMaskHalf);
            }
        }

        GlUniform inverseUniform = shader.getUniform("PaintEffectInverse");

        if (inverseUniform != null)
        {
            inverseUniform.set(paintEffectInverse);
        }

        GlUniform halfUniform = shader.getUniform("PaintMaskHalf");

        if (halfUniform != null)
        {
            halfUniform.set(paintMaskHalf.x, paintMaskHalf.y, paintMaskHalf.z);
        }

        GlUniform activeUniform = shader.getUniform("PaintEffectActive");

        if (activeUniform != null)
        {
            activeUniform.set(active ? 1F : 0F);
        }

        GlUniform anchorUniform = shader.getUniform("PaintMaskBottomAnchored");

        if (anchorUniform != null)
        {
            anchorUniform.set(bottomAnchored ? 1F : 0F);
        }

        GlUniform shapeUniform = shader.getUniform("PaintMaskShape");

        if (shapeUniform != null)
        {
            float shape = transform == null || transform.shape == null ? 0F : transform.shape.id;

            shapeUniform.set(shape);
        }
    }

    public static void bindGlowOverlay(ShaderProgram shader, GlowSettings glow, Color legacyGlow, float glowIntensity, float alpha)
    {
        GlUniform glowUniform = shader == null ? null : shader.getUniform("GlowOverlayColor");
        float glowR = 0F;
        float glowG = 0F;
        float glowB = 0F;
        float glowStrength = 0F;

        if (glow != null && glow.resolvePaintOnly() && glowIntensity > 0F)
        {
            Color resolved = new Color();

            glow.resolveColor(legacyGlow, resolved);
            glowR = resolved.r;
            glowG = resolved.g;
            glowB = resolved.b;
            glowStrength = glowIntensity * alpha;
        }

        if (glowUniform != null)
        {
            glowUniform.set(glowR, glowG, glowB, glowStrength);
        }
    }
}
