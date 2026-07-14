package mchorse.bbs_mod.camera.clips.misc;

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

public class SubtitleClip extends CameraClip
{
    public final KeyframeChannel<String> text = new KeyframeChannel<>("text", KeyframeFactories.STRING);
    public final KeyframeChannel<Double> x = new KeyframeChannel<>("x", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> y = new KeyframeChannel<>("y", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> size = new KeyframeChannel<>("size", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> anchorX = new KeyframeChannel<>("anchorX", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> anchorY = new KeyframeChannel<>("anchorY", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Color> color = new KeyframeChannel<>("color", KeyframeFactories.COLOR);
    public final KeyframeChannel<Boolean> textShadow = new KeyframeChannel<>("textShadow", KeyframeFactories.BOOLEAN);
    public final KeyframeChannel<Double> windowX = new KeyframeChannel<>("windowX", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> windowY = new KeyframeChannel<>("windowY", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Color> background = new KeyframeChannel<>("background", KeyframeFactories.COLOR);
    public final KeyframeChannel<Double> backgroundOffset = new KeyframeChannel<>("backgroundOffset", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> shadow = new KeyframeChannel<>("shadow", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Boolean> shadowOpaque = new KeyframeChannel<>("shadowOpaque", KeyframeFactories.BOOLEAN);
    public final KeyframeChannel<Integer> lineHeight = new KeyframeChannel<>("lineHeight", KeyframeFactories.INTEGER);
    public final KeyframeChannel<Integer> maxWidth = new KeyframeChannel<>("maxWidth", KeyframeFactories.INTEGER);

    public final KeyframeChannel[] channels;

    private static final Color DEFAULT_COLOR = Color.white();
    private static final Color DEFAULT_BACKGROUND = new Color().set(0);

    private Subtitle subtitle = new Subtitle();

    public static List<Subtitle> getSubtitles(ClipContext context)
    {
        return context.clipData.get("subtitles", ArrayList::new);
    }

    public SubtitleClip()
    {
        this.channels = new KeyframeChannel[]
        {
            this.text,
            this.x,
            this.y,
            this.size,
            this.anchorX,
            this.anchorY,
            this.color,
            this.textShadow,
            this.windowX,
            this.windowY,
            this.background,
            this.backgroundOffset,
            this.shadow,
            this.shadowOpaque,
            this.lineHeight,
            this.maxWidth
        };

        for (KeyframeChannel channel : this.channels)
        {
            this.add(channel);
        }
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

        if (this.text.isEmpty() && !this.title.get().isEmpty())
        {
            this.text.insert(0, this.title.get());
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
        this.migrateString(legacy, "text", this.text, this.title.get());
        this.migrateDouble(legacy, "x", this.x, 0D);
        this.migrateDouble(legacy, "y", this.y, 0D);
        this.migrateDouble(legacy, "size", this.size, 10D);
        this.migrateDouble(legacy, "anchorX", this.anchorX, 0.5D);
        this.migrateDouble(legacy, "anchorY", this.anchorY, 0.5D);
        this.migrateColor(legacy, "color", this.color, DEFAULT_COLOR);
        this.migrateBoolean(legacy, "textShadow", this.textShadow, true);
        this.migrateDouble(legacy, "windowX", this.windowX, 0.5D);
        this.migrateDouble(legacy, "windowY", this.windowY, 0.5D);
        this.migrateColor(legacy, "background", this.background, DEFAULT_BACKGROUND);
        this.migrateDouble(legacy, "backgroundOffset", this.backgroundOffset, 2D);
        this.migrateDouble(legacy, "shadow", this.shadow, 0D);
        this.migrateBoolean(legacy, "shadowOpaque", this.shadowOpaque, false);
        this.migrateInteger(legacy, "lineHeight", this.lineHeight, 12);
        this.migrateInteger(legacy, "maxWidth", this.maxWidth, 0);
    }

    private void migrateString(Map<String, BaseType> legacy, String key, KeyframeChannel<String> channel, String fallback)
    {
        if (!channel.isEmpty())
        {
            return;
        }

        BaseType data = legacy.get(key);

        if (data != null && data.isString())
        {
            channel.insert(0, data.asString());
        }
        else if (!fallback.isEmpty())
        {
            channel.insert(0, fallback);
        }
    }

    private void migrateDouble(Map<String, BaseType> legacy, String key, KeyframeChannel<Double> channel, double fallback)
    {
        if (!channel.isEmpty())
        {
            return;
        }

        BaseType data = legacy.get(key);

        if (data != null && data.isNumeric())
        {
            channel.insert(0, data.asNumeric().doubleValue());
        }
        else
        {
            channel.insert(0, fallback);
        }
    }

    private void migrateInteger(Map<String, BaseType> legacy, String key, KeyframeChannel<Integer> channel, int fallback)
    {
        if (!channel.isEmpty())
        {
            return;
        }

        BaseType data = legacy.get(key);

        if (data != null && data.isNumeric())
        {
            channel.insert(0, data.asNumeric().intValue());
        }
        else
        {
            channel.insert(0, fallback);
        }
    }

    private void migrateBoolean(Map<String, BaseType> legacy, String key, KeyframeChannel<Boolean> channel, boolean fallback)
    {
        if (!channel.isEmpty())
        {
            return;
        }

        BaseType data = legacy.get(key);

        if (data != null && data.isNumeric())
        {
            channel.insert(0, data.asNumeric().boolValue());
        }
        else
        {
            channel.insert(0, fallback);
        }
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
        List<Subtitle> subtitles = getSubtitles(context);
        float t = context.relativeTick + context.transition;
        float factor = this.envelope.factorEnabled(this.duration.get(), t);
        Color tinted = this.interpColor(this.color, t, DEFAULT_COLOR).copy();
        int color = Colors.setA(tinted.getARGBColor(), factor * tinted.a);

        this.subtitle.update(
            this.interpString(this.text, t, this.title.get()),
            (int) Math.round(this.interpDouble(this.x, t, 0D)),
            (int) Math.round(this.interpDouble(this.y, t, 0D)),
            (float) this.interpDouble(this.size, t, 10D),
            (float) this.interpDouble(this.anchorX, t, 0.5D),
            (float) this.interpDouble(this.anchorY, t, 0.5D),
            color,
            this.interpBoolean(this.textShadow, t, true)
        );
        this.subtitle.updateWindow(
            (float) this.interpDouble(this.windowX, t, 0.5D),
            (float) this.interpDouble(this.windowY, t, 0.5D)
        );
        this.subtitle.updateBackground(
            this.interpColor(this.background, t, DEFAULT_BACKGROUND).getARGBColor(),
            (float) this.interpDouble(this.backgroundOffset, t, 2D),
            (float) this.interpDouble(this.shadow, t, 0D),
            this.interpBoolean(this.shadowOpaque, t, false)
        );
        this.subtitle.updateConstraints(
            this.interpInteger(this.lineHeight, t, 12),
            this.interpInteger(this.maxWidth, t, 0)
        );
        this.subtitle.renderOrder = context.count;
        subtitles.add(this.subtitle);
    }

    private String interpString(KeyframeChannel<String> channel, float t, String fallback)
    {
        if (channel.isEmpty())
        {
            return fallback;
        }

        return channel.interpolate(t, fallback);
    }

    private double interpDouble(KeyframeChannel<Double> channel, float t, double fallback)
    {
        if (channel.isEmpty())
        {
            return fallback;
        }

        return channel.interpolate(t);
    }

    private int interpInteger(KeyframeChannel<Integer> channel, float t, int fallback)
    {
        if (channel.isEmpty())
        {
            return fallback;
        }

        return channel.interpolate(t, fallback);
    }

    private boolean interpBoolean(KeyframeChannel<Boolean> channel, float t, boolean fallback)
    {
        if (channel.isEmpty())
        {
            return fallback;
        }

        return channel.interpolate(t, fallback);
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
        return new SubtitleClip();
    }
}
