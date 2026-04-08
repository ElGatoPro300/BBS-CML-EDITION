package mchorse.bbs_mod.camera.clips.screen;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import java.util.ArrayList;
import java.util.List;

public class ColorClip extends CameraClip
{
    public ValueInt overlayColor = new ValueInt("overlayColor", Colors.A100);
    public final KeyframeChannel<Double> overlayAlpha = new KeyframeChannel<>("overlayAlpha", KeyframeFactories.DOUBLE);

    public ValueInt vignetteColor = new ValueInt("vignetteColor", Colors.A100);
    public final KeyframeChannel<Double> vignetteStrength = new KeyframeChannel<>("vignetteStrength", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> vignetteSmoothness = new KeyframeChannel<>("vignetteSmoothness", KeyframeFactories.DOUBLE);

    public final KeyframeChannel<Double>[] channels;

    private ColorEffect effect = new ColorEffect();

    public static List<ColorEffect> getEffects(ClipContext context)
    {
        return context.clipData.get("colorEffects", ArrayList::new);
    }

    public ColorClip()
    {
        this.channels = new KeyframeChannel[] {this.overlayAlpha, this.vignetteStrength, this.vignetteSmoothness};

        this.add(this.overlayColor);
        this.add(this.overlayAlpha);
        this.add(this.vignetteColor);
        this.add(this.vignetteStrength);
        this.add(this.vignetteSmoothness);
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        float t = context.relativeTick + context.transition;
        float factor = this.envelope.factorEnabled(this.duration.get(), t);

        this.effect.reset();

        float alpha = this.overlayAlpha.isEmpty() ? 0F : (float) (double) this.overlayAlpha.interpolate(t);

        if (alpha > 0F)
        {
            this.effect.hasOverlay = true;
            this.effect.overlayColor = Colors.setA(this.overlayColor.get(), alpha * factor);
        }

        float strength = this.vignetteStrength.isEmpty() ? 0F : (float) (double) this.vignetteStrength.interpolate(t);

        if (strength > 0F)
        {
            float smoothness = this.vignetteSmoothness.isEmpty() ? 0.5F : (float) (double) this.vignetteSmoothness.interpolate(t);

            this.effect.hasVignette = true;
            this.effect.vignetteColor = this.vignetteColor.get();
            this.effect.vignetteStrength = strength * factor;
            this.effect.vignetteSmoothness = smoothness;
        }

        if (this.effect.hasOverlay || this.effect.hasVignette)
        {
            getEffects(context).add(this.effect);
        }
    }

    @Override
    protected Clip create()
    {
        return new ColorClip();
    }
}
