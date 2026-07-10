package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin
{
/*
    @Shadow
    public Framebuffer entityOutlinesFramebuffer;
*/

    @Inject(method = "renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V", at = @At("HEAD"), cancellable = true, require = 0)
    public void onRenderSky(CallbackInfo info)
    {
        if (BBSRendering.isChromaSkyEnabled())
        {
            Color color = Color.rgb(BBSRendering.getChromaSkyColor());

            GL11.glClearColor(color.r, color.g, color.b, 1F);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            /* RenderSystem.setShaderFogColor(color.r, color.g, color.b, 1F); */

            info.cancel();
        }
    }

    /* TODO(1.21.11 render): WorldRenderer#renderLayer was removed by the FrameGraphBuilder/
     * OrderedRenderCommandQueue terrain rewrite (per-RenderLayer submission is now handled through
     * renderBlockLayers/SectionRenderState with no simple cancellation point). require = 0 keeps this
     * injector inert instead of crashing until the chroma-sky-terrain occlusion is re-ported. */
    @Inject(method = "renderLayer", at = @At("HEAD"), cancellable = true, require = 0)
    public void onRenderLayer(RenderLayer renderLayer, double cameraX, double cameraY, double cameraZ, Matrix4f positionMatrix, Matrix4f projectionMatrix, CallbackInfo info)
    {
        if (BBSRendering.isChromaSkyEnabled() && !BBSRendering.isChromaSkyTerrain())
        {

            info.cancel();
        }
    }

    @Inject(method = "setupFrustum", at = @At("HEAD"))
    public void onSetupFrustum(Matrix4f posMatrix, Matrix4f projMatrix, Vec3d pos, CallbackInfoReturnable<Frustum> info)
    {
        BBSRendering.camera.set(posMatrix);
    }

    @Inject(at = @At("RETURN"), method = "loadEntityOutlinePostProcessor")
    private void onLoadEntityOutlineShader(CallbackInfo info)
    {
        BBSRendering.resizeExtraFramebuffers();
    }

    @Inject(at = @At("RETURN"), method = "onResized")
    private void onResized(CallbackInfo info)
    {
        /*
        if (this.entityOutlinesFramebuffer == null)
        {
            return;
        }
        */

        BBSRendering.resizeExtraFramebuffers();
    }
}
