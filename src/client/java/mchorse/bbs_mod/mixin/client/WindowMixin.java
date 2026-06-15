package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.ui.framework.UIScreen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Window.class)
public class WindowMixin implements mchorse.bbs_mod.client.IWindowDimensionsOverride
{
    /**
     * Apply BBS's UI scale as a fractional value (e.g. 1.6). Minecraft's GUI scale is integer-only,
     * so when a non-whole BBS scale is configured and a BBS screen is open, override the window's
     * scale factor with the exact value. Whole-number and "auto" scales keep Minecraft's normal
     * (clamped) behaviour.
     */
    @ModifyVariable(method = "setScaleFactor", at = @At("HEAD"), argsOnly = true)
    private double bbs_overrideUIScaleFactor(double scaleFactor)
    {
        double uiScale = BBSModClient.getUIScaleFactor();

        if (uiScale > 0D && uiScale != Math.floor(uiScale) && MinecraftClient.getInstance().currentScreen instanceof UIScreen)
        {
            return uiScale;
        }

        return scaleFactor;
    }

    @Shadow
    private int width;

    @Shadow
    private int height;

    @Shadow
    private int framebufferWidth;

    @Shadow
    private int framebufferHeight;

    @Shadow
    private int scaledWidth;

    @Shadow
    private int scaledHeight;

    @Shadow
    private double scaleFactor;

    private int bbs$savedWidth;
    private int bbs$savedHeight;
    private int bbs$savedFramebufferWidth;
    private int bbs$savedFramebufferHeight;
    private int bbs$savedScaledWidth;
    private int bbs$savedScaledHeight;
    private double bbs$savedScaleFactor;
    private boolean bbs$isOverridden;

    @Override
    public void bbs$overrideDimensions(int videoWidth, int videoHeight, double scaleFactor, float framebufferScale)
    {
        if (this.bbs$isOverridden)
        {
            return;
        }

        this.bbs$savedWidth = this.width;
        this.bbs$savedHeight = this.height;
        this.bbs$savedFramebufferWidth = this.framebufferWidth;
        this.bbs$savedFramebufferHeight = this.framebufferHeight;
        this.bbs$savedScaledWidth = this.scaledWidth;
        this.bbs$savedScaledHeight = this.scaledHeight;
        this.bbs$savedScaleFactor = this.scaleFactor;

        this.width = videoWidth;
        this.height = videoHeight;
        this.framebufferWidth = (int) (videoWidth * framebufferScale);
        this.framebufferHeight = (int) (videoHeight * framebufferScale);
        this.scaleFactor = scaleFactor;
        this.scaledWidth = (int) Math.ceil((double) this.framebufferWidth / scaleFactor);
        this.scaledHeight = (int) Math.ceil((double) this.framebufferHeight / scaleFactor);

        this.bbs$isOverridden = true;
    }

    @Override
    public void bbs$restoreDimensions()
    {
        if (!this.bbs$isOverridden)
        {
            return;
        }

        this.width = this.bbs$savedWidth;
        this.height = this.bbs$savedHeight;
        this.framebufferWidth = this.bbs$savedFramebufferWidth;
        this.framebufferHeight = this.bbs$savedFramebufferHeight;
        this.scaledWidth = this.bbs$savedScaledWidth;
        this.scaledHeight = this.bbs$savedScaledHeight;
        this.scaleFactor = this.bbs$savedScaleFactor;

        this.bbs$isOverridden = false;
    }
}