package mchorse.bbs_mod.camera.clips.screen;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

public class CinematicClip extends CameraClip
{
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

    public CinematicClip()
    {
        this.channels = new KeyframeChannel[] {
            this.aberration,
            this.vhs,
            this.lensDistortion,
            this.vintage,
            this.radialBlur,
            this.rain,
            this.dust,
            this.lightLeak,
        };

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

            ColorClip.getEffects(context).add(this.effect);
        }
    }

    @Override
    public boolean isPositionClip()
    {
        return false;
    }

    @Override
    protected Clip create()
    {
        return new CinematicClip();
    }
}
