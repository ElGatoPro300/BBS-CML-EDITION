package mchorse.bbs_mod.camera.clips.screen;

public class ColorEffect
{
    public boolean hasOverlay;
    public int overlayColor;

    public boolean hasVignette;
    public int vignetteColor;
    public float vignetteStrength;
    public float vignetteSmoothness;

    public void reset()
    {
        this.hasOverlay = false;
        this.hasVignette = false;
    }
}
