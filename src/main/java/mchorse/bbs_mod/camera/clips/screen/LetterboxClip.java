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

public class LetterboxClip extends CameraClip
{
    public ValueInt color = new ValueInt("color", Colors.A100);
    public final KeyframeChannel<Double> size = new KeyframeChannel<>("size", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> smoothness = new KeyframeChannel<>("smoothness", KeyframeFactories.DOUBLE);

    public final KeyframeChannel<Double>[] channels;

    private LetterboxEffect effect = new LetterboxEffect();

    public static List<LetterboxEffect> getEffects(ClipContext context)
    {
        return context.clipData.get("letterboxEffects", ArrayList::new);
    }

    public LetterboxClip()
    {
        this.channels = new KeyframeChannel[] {this.size, this.smoothness};

        this.add(this.color);
        this.add(this.size);
        this.add(this.smoothness);
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        float t = context.relativeTick + context.transition;
        float factor = this.envelope.factorEnabled(this.duration.get(), t);

        float sz = (this.size.isEmpty() ? 0F : (float) (double) this.size.interpolate(t)) * 0.25F;

        if (sz > 0F)
        {
            float smooth = (this.smoothness.isEmpty() ? 0F : (float) (double) this.smoothness.interpolate(t)) * 0.25F;

            this.effect.size = sz * factor;
            this.effect.smoothness = smooth;
            this.effect.color = this.color.get();

            getEffects(context).add(this.effect);
        }
    }

    @Override
    protected Clip create()
    {
        return new LetterboxClip();
    }
}
