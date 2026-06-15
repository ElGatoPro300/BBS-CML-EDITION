package mchorse.bbs_mod.client;

public interface IWindowDimensionsOverride
{
    public void bbs$overrideDimensions(int videoWidth, int videoHeight, double scaleFactor, float framebufferScale);

    public void bbs$restoreDimensions();
}
