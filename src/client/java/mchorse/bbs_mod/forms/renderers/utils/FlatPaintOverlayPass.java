package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.forms.forms.utils.EffectTransform;

import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;

public class FlatPaintOverlayPass
{
    private FlatPaintOverlayPass()
    {
    }

    public static void render(EffectTransform transform, Runnable draw)
    {
        if (draw == null)
        {
            return;
        }

        boolean savedDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean savedPolygonOffsetFill = GL11.glGetBoolean(GL11.GL_POLYGON_OFFSET_FILL);

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.depthMask(false);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1F, -1F);
        BlockEffectOverlayUniforms.configurePaintOverlayRenderState(transform);
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

    public static void render(Runnable draw)
    {
        render(null, draw);
    }
}
