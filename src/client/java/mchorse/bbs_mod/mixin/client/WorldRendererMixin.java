package mchorse.bbs_mod.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.utils.colors.Color;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
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

    @Inject(method = "renderSky(Lnet/minecraft/client/util/math/MatrixStack;Lorg/joml/Matrix4f;FLnet/minecraft/client/render/Camera;ZLjava/lang/Runnable;)V", at = @At("HEAD"), cancellable = true, require = 0)
    public void onRenderSky(CallbackInfo info)
    {
        if (BBSSettings.chromaSkyEnabled.get())
        {
            Color color = Color.rgb(BBSSettings.chromaSkyColor.get());

            GL11.glClearColor(color.r, color.g, color.b, 1F);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            /* RenderSystem.setShaderFogColor(color.r, color.g, color.b, 1F); */

            info.cancel();
        }
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
