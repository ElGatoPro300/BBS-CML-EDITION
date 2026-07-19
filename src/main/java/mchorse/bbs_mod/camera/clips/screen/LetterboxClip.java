package mchorse.bbs_mod.camera.clips.screen;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LetterboxClip extends CameraClip
{
    private static final Color DEFAULT_COLOR = Color.rgba(Colors.A100);
    private static final double DEFAULT_HEIGHT = 0.4D;
    private static final double DEFAULT_WIDTH = 1D;
    private static final double DEFAULT_ZOOM = 1D;

    public final KeyframeChannel<Color> color = new KeyframeChannel<>("color", KeyframeFactories.COLOR);
    public final KeyframeChannel<Double> size = new KeyframeChannel<>("size", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> width = new KeyframeChannel<>("width", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> height = new KeyframeChannel<>("height", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> smoothness = new KeyframeChannel<>("smoothness", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> rotation = new KeyframeChannel<>("rotation", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> zoom = new KeyframeChannel<>("zoom", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> offsetX = new KeyframeChannel<>("offsetX", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> offsetY = new KeyframeChannel<>("offsetY", KeyframeFactories.DOUBLE);

    public final KeyframeChannel[] channels;

    private LetterboxEffect effect = new LetterboxEffect();

    public static List<LetterboxEffect> getEffects(ClipContext context)
    {
        return context.clipData.get("letterboxEffects", ArrayList::new);
    }

    public LetterboxClip()
    {
        this.channels = new KeyframeChannel[]
        {
            this.color,
            this.width,
            this.height,
            this.smoothness,
            this.rotation,
            this.zoom
        };

        this.add(this.color);
        this.add(this.size);
        this.add(this.width);
        this.add(this.height);
        this.add(this.smoothness);
        this.add(this.rotation);
        this.add(this.zoom);
        this.add(this.offsetX);
        this.add(this.offsetY);

        this.color.insert(0, DEFAULT_COLOR.copy());
        this.height.insert(0, DEFAULT_HEIGHT);
        this.width.insert(0, DEFAULT_WIDTH);
        this.zoom.insert(0, DEFAULT_ZOOM);
    }

    @Override
    public void fromData(BaseType data)
    {
        Map<String, BaseType> legacy = null;

        if (data != null && data.isMap())
        {
            legacy = this.collectLegacyScalars(data.asMap());
        }

        super.fromData(data);

        if (legacy != null)
        {
            this.migrateLegacy(legacy);
        }
    }

    private Map<String, BaseType> collectLegacyScalars(MapType map)
    {
        Map<String, BaseType> legacy = new HashMap<>();

        for (Map.Entry<String, BaseType> entry : map)
        {
            BaseType value = entry.getValue();

            if (value != null && !value.isList())
            {
                legacy.put(entry.getKey(), value);
            }
        }

        return legacy.isEmpty() ? null : legacy;
    }

    private void migrateLegacy(Map<String, BaseType> legacy)
    {
        this.migrateColor(legacy, "color", this.color, DEFAULT_COLOR);
    }

    private void migrateColor(Map<String, BaseType> legacy, String key, KeyframeChannel<Color> channel, Color fallback)
    {
        if (!channel.isEmpty())
        {
            return;
        }

        BaseType data = legacy.get(key);

        if (data != null && data.isNumeric())
        {
            channel.insert(0, Color.rgba(data.asNumeric().intValue()));
        }
        else
        {
            channel.insert(0, fallback.copy());
        }
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        float t = context.relativeTick + context.transition;
        float factor = this.envelope.factorEnabled(this.duration.get(), t);

        float thickness = this.height.isEmpty()
            ? (this.size.isEmpty() ? (float) DEFAULT_HEIGHT : (float) (double) this.size.interpolate(t))
            : (float) (double) this.height.interpolate(t);
        float sz = thickness * 0.25F;

        if (sz > 0F)
        {
            float barWidth = this.width.isEmpty() ? (float) DEFAULT_WIDTH : (float) (double) this.width.interpolate(t);
            float smooth = (this.smoothness.isEmpty() ? 0F : (float) (double) this.smoothness.interpolate(t)) * 0.25F;
            float rot = this.rotation.isEmpty() ? 0F : (float) (double) this.rotation.interpolate(t);
            float zm = this.zoom.isEmpty() ? (float) DEFAULT_ZOOM : (float) (double) this.zoom.interpolate(t);
            float offX = this.offsetX.isEmpty() ? 0F : (float) (double) this.offsetX.interpolate(t);
            float offY = this.offsetY.isEmpty() ? 0F : (float) (double) this.offsetY.interpolate(t);

            this.effect.size = sz * factor;
            this.effect.width = barWidth;
            this.effect.smoothness = smooth;
            this.effect.color = Colors.setA(this.interpColor(this.color, t, DEFAULT_COLOR).getARGBColor(), 1F);
            this.effect.rotation = rot;
            this.effect.zoom = Math.max(0.01F, zm);
            this.effect.offsetX = offX;
            this.effect.offsetY = offY;

            getEffects(context).add(this.effect);
        }
    }

    private Color interpColor(KeyframeChannel<Color> channel, float t, Color fallback)
    {
        if (channel.isEmpty())
        {
            return fallback;
        }

        return channel.interpolate(t, fallback);
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
