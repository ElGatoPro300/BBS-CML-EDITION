package mchorse.bbs_mod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.controller.CameraController;
import mchorse.bbs_mod.camera.controller.ICameraController;
import mchorse.bbs_mod.camera.controller.PlayCameraController;
import mchorse.bbs_mod.client.BBSRendering;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin
{
    /**
     * This injection cancels bobbing when camera controller takes over
     */
    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    public void onBob(CameraRenderState cameraRenderState, PoseStack matrices, CallbackInfo ci)
    {
        if (BBSModClient.getCameraController().getCurrent() != null)
        {
            ci.cancel();
        }
    }

    /**
     * This injection replaces the camera roll when camera controller takes over
     */
    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    public void onTiltViewWhenHurt(CameraRenderState cameraRenderState, PoseStack matrices, CallbackInfo info)
    {
        CameraController controller = BBSModClient.getCameraController();

        if (controller.getCurrent() != null && !BBSRendering.isIrisShadowPass())
        {
            matrices.mulPose(Axis.ZP.rotationDegrees(controller.getRoll()));

            info.cancel();
        }
    }

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    public void onRenderHand(CameraRenderState cameraRenderState, float partialTick, Matrix4fc projection, CallbackInfo info)
    {
        ICameraController current = BBSModClient.getCameraController().getCurrent();

        if (current instanceof PlayCameraController)
        {
            info.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "renderLevel")
    private void onWorldRenderBegin(DeltaTracker deltaTracker, CallbackInfo callbackInfo)
    {
        BBSRendering.onWorldRenderBegin();
    }

    @Inject(at = @At("RETURN"), method = "renderLevel")
    private void onWorldRenderEnd(DeltaTracker deltaTracker, CallbackInfo callbackInfo)
    {
        BBSRendering.onWorldRenderEnd();
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void onBeforeHudRendering(DeltaTracker tickCounter, boolean tick, CallbackInfo info)
    {
        ICameraController current = BBSModClient.getCameraController().getCurrent();

        if (Minecraft.getInstance().options.hideGui && current == null)
        {
            BBSRendering.onRenderBeforeScreen();
        }
    }
}
