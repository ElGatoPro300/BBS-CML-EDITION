package mchorse.bbs_mod.camera.clips.screen;

public class ColorEffect
{
    public boolean hasOverlay;
    public int overlayColor;

    public boolean hasVignette;
    public int vignetteColor;
    public float vignetteStrength;
    public float vignetteSmoothness;

    public boolean hasDistort;
    public float distortX;
    public float distortY;

    public boolean hasGrade;
    public float brightness;
    public float contrast;
    public float saturation;
    public float hue;
    public float liftR, liftG, liftB;
    public float gammaR, gammaG, gammaB;
    public float gainR, gainG, gainB;

    public void reset()
    {
        this.hasOverlay = false;
        this.hasVignette = false;
        this.hasGrade = false;
        this.hasDistort = false;
    }
}
