package mchorse.bbs_mod.camera.clips.screen;

import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.ArrayList;
import java.util.List;

/** Holds all screen-node effect parameters evaluated from a ScreenNodeGraph. */
public class ScreenNodeEffect
{
    /* Color grade — from ColorGradeEffectNode */
    public float brightness;
    public float contrast;
    public float saturation;

    /* Vignette — from VignetteEffectNode */
    public float vignetteStrength;
    public float vignetteSmoothness = 0.5F;
    public int vignetteColor = Colors.A100;

    /* Grain — from GrainEffectNode */
    public float grainStrength;
    public float grainSize = 1F;

    /* Letterbox — from LetterboxEffectNode */
    public float letterboxSize;
    public int letterboxColor = Colors.A100;

    /* Overlay — from OverlayEffectNode */
    public int overlayColor = Colors.A100;
    public float overlayAlpha;

    /* Distortion — from DistortionEffectNode */
    public float distortX;
    public float distortY;

    public void reset()
    {
        this.brightness = 0F;
        this.contrast = 0F;
        this.saturation = 0F;
        this.vignetteStrength = 0F;
        this.vignetteSmoothness = 0.5F;
        this.vignetteColor = Colors.A100;
        this.grainStrength = 0F;
        this.grainSize = 1F;
        this.letterboxSize = 0F;
        this.letterboxColor = Colors.A100;
        this.overlayColor = Colors.A100;
        this.overlayAlpha = 0F;
        this.distortX = 0F;
        this.distortY = 0F;
    }

    public static List<ScreenNodeEffect> getEffects(ClipContext context)
    {
        return context.clipData.get("screenNodeEffects", ArrayList::new);
    }
}
