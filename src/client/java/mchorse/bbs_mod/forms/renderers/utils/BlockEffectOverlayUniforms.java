package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.EffectTransformMath;

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
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        ShaderProgram program = BBSShaders.getBlockPaintOverlayProgram();

        if (program != null)
        {
            RenderSystem.setShader(() -> program);
            bindPaint(program, transform);
        }

        RenderSystem.setShaderTexture(0, PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    public static void bindPaint(ShaderProgram shader, EffectTransform transform)
    {
        if (shader == null)
        {
            return;
        }

        boolean active = EffectTransformMath.isTransformActive(transform);

        if (active)
        {
            EffectTransformMath.buildInverseMatrix(transform, paintEffectInverse);
            EffectTransformMath.resolveBlockMaskHalfExtents(transform, paintMaskHalf);
        }
        else
        {
            paintEffectInverse.identity();
            paintMaskHalf.set(0.5F, 0.5F, 0.5F);
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
    }
}
