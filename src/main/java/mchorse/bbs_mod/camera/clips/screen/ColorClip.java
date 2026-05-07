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

    /* Color grade channels — keyframe value range ~0–4, effective range 0–1 (×0.25 scale) */
    public final KeyframeChannel<Double> saturation = new KeyframeChannel<>("saturation", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> hue = new KeyframeChannel<>("hue", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> brightness = new KeyframeChannel<>("brightness", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> contrast = new KeyframeChannel<>("contrast", KeyframeFactories.DOUBLE);

    /* Lift (shadows) */
    public final KeyframeChannel<Double> liftR = new KeyframeChannel<>("liftR", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> liftG = new KeyframeChannel<>("liftG", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> liftB = new KeyframeChannel<>("liftB", KeyframeFactories.DOUBLE);

    /* Gamma (midtones) */
    public final KeyframeChannel<Double> gammaR = new KeyframeChannel<>("gammaR", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> gammaG = new KeyframeChannel<>("gammaG", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> gammaB = new KeyframeChannel<>("gammaB", KeyframeFactories.DOUBLE);

    /* Gain (highlights) */
    public final KeyframeChannel<Double> gainR = new KeyframeChannel<>("gainR", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> gainG = new KeyframeChannel<>("gainG", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> gainB = new KeyframeChannel<>("gainB", KeyframeFactories.DOUBLE);

    /* Cinematic effects */
    public final KeyframeChannel<Double> aberration = new KeyframeChannel<>("aberration", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> vhs = new KeyframeChannel<>("vhs", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> lensDistortion = new KeyframeChannel<>("lensDistortion", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> vintage = new KeyframeChannel<>("vintage", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> radialBlur = new KeyframeChannel<>("radialBlur", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> rain = new KeyframeChannel<>("rain", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> dust = new KeyframeChannel<>("dust", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> lightLeak = new KeyframeChannel<>("lightLeak", KeyframeFactories.DOUBLE);

    public final KeyframeChannel<Double>[] channels;

    private ColorEffect effect = new ColorEffect();

    public static List<ColorEffect> getEffects(ClipContext context)
    {
        return context.clipData.get("colorEffects", ArrayList::new);
    }

    public ColorClip()
    {
        this.channels = new KeyframeChannel[] {
            this.overlayAlpha,
            this.saturation,
            this.hue,
            this.brightness,
            this.contrast,
            this.liftR,
            this.liftG,
            this.liftB,
            this.gammaR,
            this.gammaG,
            this.gammaB,
            this.gainR,
            this.gainG,
            this.gainB,
            this.aberration,
            this.vhs,
            this.lensDistortion,
            this.vintage,
            this.radialBlur,
            this.rain,
            this.dust,
            this.lightLeak,
        };

        this.add(this.overlayColor);
        this.add(this.overlayAlpha);
        this.add(this.saturation);
        this.add(this.hue);
        this.add(this.brightness);
        this.add(this.contrast);
        this.add(this.liftR);
        this.add(this.liftG);
        this.add(this.liftB);
        this.add(this.gammaR);
        this.add(this.gammaG);
        this.add(this.gammaB);
        this.add(this.gainR);
        this.add(this.gainG);
        this.add(this.gainB);
        this.add(this.aberration);
        this.add(this.vhs);
        this.add(this.lensDistortion);
        this.add(this.vintage);
        this.add(this.radialBlur);
        this.add(this.rain);
        this.add(this.dust);
        this.add(this.lightLeak);
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        float t = context.relativeTick + context.transition;
        float factor = this.envelope.factorEnabled(this.duration.get(), t);

        this.effect.reset();

        /* Overlay: keyframe values 0–4, effective alpha 0–1 */
        float alpha = (this.overlayAlpha.isEmpty() ? 0F : (float) (double) this.overlayAlpha.interpolate(t)) * 0.25F;

        if (alpha > 0F)
        {
            this.effect.hasOverlay = true;
            this.effect.overlayColor = Colors.setA(this.overlayColor.get(), alpha * factor);
        }

        /* Color grade: keyframe values in ~±4 range, effective in ~±1 range (×0.25) */
        float sat = (this.saturation.isEmpty() ? 0F : (float) (double) this.saturation.interpolate(t)) * 0.25F;
        float hueRot = this.hue.isEmpty() ? 0F : (float) (double) this.hue.interpolate(t);
        float bright = (this.brightness.isEmpty() ? 0F : (float) (double) this.brightness.interpolate(t)) * 0.25F;
        float cont = (this.contrast.isEmpty() ? 0F : (float) (double) this.contrast.interpolate(t)) * 0.25F;
        float lR = (this.liftR.isEmpty() ? 0F : (float) (double) this.liftR.interpolate(t)) * 0.25F;
        float lG = (this.liftG.isEmpty() ? 0F : (float) (double) this.liftG.interpolate(t)) * 0.25F;
        float lB = (this.liftB.isEmpty() ? 0F : (float) (double) this.liftB.interpolate(t)) * 0.25F;
        float gR = (this.gammaR.isEmpty() ? 0F : (float) (double) this.gammaR.interpolate(t)) * 0.25F;
        float gG = (this.gammaG.isEmpty() ? 0F : (float) (double) this.gammaG.interpolate(t)) * 0.25F;
        float gB = (this.gammaB.isEmpty() ? 0F : (float) (double) this.gammaB.interpolate(t)) * 0.25F;
        float gnR = (this.gainR.isEmpty() ? 0F : (float) (double) this.gainR.interpolate(t)) * 0.25F;
        float gnG = (this.gainG.isEmpty() ? 0F : (float) (double) this.gainG.interpolate(t)) * 0.25F;
        float gnB = (this.gainB.isEmpty() ? 0F : (float) (double) this.gainB.interpolate(t)) * 0.25F;

        boolean hasGrade = sat != 0F || hueRot != 0F || bright != 0F || cont != 0F
            || lR != 0F || lG != 0F || lB != 0F
            || gR != 0F || gG != 0F || gB != 0F
            || gnR != 0F || gnG != 0F || gnB != 0F;

        if (hasGrade)
        {
            this.effect.hasGrade = true;
            this.effect.saturation = sat * factor;
            this.effect.hue = hueRot * factor;
            this.effect.brightness = bright * factor;
            this.effect.contrast = cont * factor;
            this.effect.liftR = lR * factor;
            this.effect.liftG = lG * factor;
            this.effect.liftB = lB * factor;
            this.effect.gammaR = gR * factor;
            this.effect.gammaG = gG * factor;
            this.effect.gammaB = gB * factor;
            this.effect.gainR = gnR * factor;
            this.effect.gainG = gnG * factor;
            this.effect.gainB = gnB * factor;
        }

        /* Cinematic effects */
        float ab = (this.aberration.isEmpty() ? 0F : (float) (double) this.aberration.interpolate(t)) * 0.25F;
        float vh = (this.vhs.isEmpty() ? 0F : (float) (double) this.vhs.interpolate(t)) * 0.25F;
        float ld = (this.lensDistortion.isEmpty() ? 0F : (float) (double) this.lensDistortion.interpolate(t)) * 0.25F;
        float vt = (this.vintage.isEmpty() ? 0F : (float) (double) this.vintage.interpolate(t)) * 0.25F;
        float rb = (this.radialBlur.isEmpty() ? 0F : (float) (double) this.radialBlur.interpolate(t)) * 0.25F;
        float rn = (this.rain.isEmpty() ? 0F : (float) (double) this.rain.interpolate(t)) * 0.25F;
        float ds = (this.dust.isEmpty() ? 0F : (float) (double) this.dust.interpolate(t)) * 0.25F;
        float ll = (this.lightLeak.isEmpty() ? 0F : (float) (double) this.lightLeak.interpolate(t)) * 0.25F;

        if (ab != 0F || vh != 0F || ld != 0F || vt != 0F || rb != 0F || rn != 0F || ds != 0F || ll != 0F)
        {
            this.effect.hasCinematic = true;
            this.effect.aberration = ab * factor;
            this.effect.vhs = vh * factor;
            this.effect.lensDistortion = ld * factor;
            this.effect.vintage = vt * factor;
            this.effect.radialBlur = rb * factor;
            this.effect.rain = rn * factor;
            this.effect.dust = ds * factor;
            this.effect.lightLeak = ll * factor;
            this.effect.time = t / 20.0F; // Convert timeline ticks to seconds
        }

        if (this.effect.hasOverlay || this.effect.hasGrade || this.effect.hasCinematic)
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
