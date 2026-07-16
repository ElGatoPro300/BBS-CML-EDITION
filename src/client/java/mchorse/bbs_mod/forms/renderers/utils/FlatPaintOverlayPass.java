package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.forms.forms.utils.EffectTransform;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gl.ShaderProgram;

import org.lwjgl.opengl.GL11;

/**
 * Second-pass paint for flat textured forms (billboards, trails). Samples the caller-bound
 * texture and tints it toward the paint color; does not switch to the block atlas shader.
 */
public class FlatPaintOverlayPass
{
    private FlatPaintOverlayPass()
    {
    }

    public static void render(EffectTransform transform, Runnable draw)
    {
        render(draw);
    }

    public static void render(Runnable draw)
    {
        if (draw == null)
        {
            return;
        }

        boolean savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean savedPolygonOffsetFill = GL11.glGetBoolean(GL11.GL_POLYGON_OFFSET_FILL);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);

        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1F, -1F);

        ShaderProgram program = BBSShaders.getFlatPaintOverlayProgram();

        if (program != null)
        {
            RenderSystem.setShader(() -> program);
        }

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

        try
        {
            draw.run();
        }
        finally
        {
            GL11.glPolygonOffset(0F, 0F);

            if (!savedPolygonOffsetFill)
            {
                GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            }

            RenderSystem.depthMask(savedDepthMask);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            RenderSystem.defaultBlendFunc();
        }
    }
}
