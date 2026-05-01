package mchorse.bbs_mod.camera.clips.screen;

import mchorse.bbs_mod.utils.colors.Colors;

/** Holds all screen-node effect parameters evaluated from a ScreenNodeGraph. */
public class ScreenNodeEffect
{
    /* Color grade — from ScreenOutputNode */
    public float brightness;
    public float contrast;
    public float saturation;

    /* Vignette — from VignetteEffectNode */
    public float vignetteStrength;
    public float vignetteSmoothness;
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

    public static java.util.List<ScreenNodeEffect> getEffects(mchorse.bbs_mod.utils.clips.ClipContext context)
    {
        return context.clipData.get("screenNodeEffects", java.util.ArrayList::new);
    }
}
