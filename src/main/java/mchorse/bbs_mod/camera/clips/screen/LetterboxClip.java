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
    public final KeyframeChannel<Double> width = new KeyframeChannel<>("width", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> height = new KeyframeChannel<>("height", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> smoothness = new KeyframeChannel<>("smoothness", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> rotation = new KeyframeChannel<>("rotation", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> zoom = new KeyframeChannel<>("zoom", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> offsetX = new KeyframeChannel<>("offsetX", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> offsetY = new KeyframeChannel<>("offsetY", KeyframeFactories.DOUBLE);

    public final KeyframeChannel<Double>[] channels;

    private LetterboxEffect effect = new LetterboxEffect();

    public static List<LetterboxEffect> getEffects(ClipContext context)
    {
        return context.clipData.get("letterboxEffects", ArrayList::new);
    }

    public LetterboxClip()
    {
        this.channels = new KeyframeChannel[] {this.width, this.height, this.smoothness, this.rotation, this.zoom};

        this.add(this.color);
        this.add(this.size);
        this.add(this.width);
        this.add(this.height);
        this.add(this.smoothness);
        this.add(this.rotation);
        this.add(this.zoom);
        this.add(this.offsetX);
        this.add(this.offsetY);
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        float t = context.relativeTick + context.transition;
        float factor = this.envelope.factorEnabled(this.duration.get(), t);

        float thickness = this.height.isEmpty()
            ? (this.size.isEmpty() ? 0F : (float) (double) this.size.interpolate(t))
            : (float) (double) this.height.interpolate(t);
        float sz = thickness * 0.25F;

        if (sz > 0F)
        {
            float barWidth = this.width.isEmpty() ? 1F : (float) (double) this.width.interpolate(t);
            float smooth = (this.smoothness.isEmpty() ? 0F : (float) (double) this.smoothness.interpolate(t)) * 0.25F;
            float rot = this.rotation.isEmpty() ? 0F : (float) (double) this.rotation.interpolate(t);
            float zm = this.zoom.isEmpty() ? 1F : (float) (double) this.zoom.interpolate(t);
            float offX = this.offsetX.isEmpty() ? 0F : (float) (double) this.offsetX.interpolate(t);
            float offY = this.offsetY.isEmpty() ? 0F : (float) (double) this.offsetY.interpolate(t);

            this.effect.size = sz * factor;
            this.effect.width = barWidth;
            this.effect.smoothness = smooth;
            this.effect.color = Colors.setA(this.color.get(), 1F);
            this.effect.rotation = rot;
            this.effect.zoom = Math.max(0.01F, zm);
            this.effect.offsetX = offX;
            this.effect.offsetY = offY;

            getEffects(context).add(this.effect);
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
        return new LetterboxClip();
    }
}
