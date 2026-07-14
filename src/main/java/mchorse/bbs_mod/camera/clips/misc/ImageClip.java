package mchorse.bbs_mod.camera.clips.misc;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.forms.forms.utils.TextureBlend;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.settings.values.core.ValueLink;
import mchorse.bbs_mod.settings.values.misc.ValueVector4f;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.KeyframeSegment;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import mchorse.bbs_mod.utils.resources.LinkUtils;

import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

public class ImageClip extends CameraClip
{
    public ValueLink texture = new ValueLink("texture", null);
    public ValueBoolean linear = new ValueBoolean("linear", false);
    public ValueBoolean mipmap = new ValueBoolean("mipmap", false);
    public ValueVector4f crop = new ValueVector4f("crop", new Vector4f(0, 0, 0, 0));
    public ValueBoolean resizeCrop = new ValueBoolean("resizeCrop", false);
    public ValueColor color = new ValueColor("color", Color.white());
    public ValueLink blendFrom = new ValueLink("blend_from", null);
    public ValueLink blendTo = new ValueLink("blend_to", null);
    public ValueBoolean uniformSize = new ValueBoolean("uniform_size", true);

    public final KeyframeChannel<Link> textureTrack = new KeyframeChannel<>("texture_track", KeyframeFactories.LINK);
    public final KeyframeChannel<Double> offsetX = new KeyframeChannel<>("offsetX", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> offsetY = new KeyframeChannel<>("offsetY", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> rotation = new KeyframeChannel<>("rotation", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> blend = new KeyframeChannel<>("blend", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> x = new KeyframeChannel<>("x", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> y = new KeyframeChannel<>("y", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> width = new KeyframeChannel<>("width", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> height = new KeyframeChannel<>("height", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> anchorX = new KeyframeChannel<>("anchorX", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> anchorY = new KeyframeChannel<>("anchorY", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> windowX = new KeyframeChannel<>("windowX", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> windowY = new KeyframeChannel<>("windowY", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> opacity = new KeyframeChannel<>("opacity", KeyframeFactories.DOUBLE);

    public final KeyframeChannel[] channels;

    private ImageOverlay overlay = new ImageOverlay();

    public static List<ImageOverlay> getImages(ClipContext context)
    {
        return context.clipData.get("images", ArrayList::new);
    }

    public ImageClip()
    {
        this.channels = new KeyframeChannel[]
        {
            this.textureTrack, this.offsetX, this.offsetY, this.rotation,
            this.x, this.y, this.width, this.height,
            this.anchorX, this.anchorY, this.windowX, this.windowY, this.opacity
        };

        this.add(this.texture);
        this.add(this.linear);
        this.add(this.mipmap);
        this.add(this.crop);
        this.add(this.resizeCrop);
        this.add(this.color);
        this.add(this.blendFrom);
        this.add(this.blendTo);
        this.add(this.uniformSize);
        this.add(this.textureTrack);
        this.add(this.offsetX);
        this.add(this.offsetY);
        this.add(this.rotation);
        this.add(this.blend);
        this.add(this.x);
        this.add(this.y);
        this.add(this.width);
        this.add(this.height);
        this.add(this.anchorX);
        this.add(this.anchorY);
        this.add(this.windowX);
        this.add(this.windowY);
        this.add(this.opacity);
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        Link link = this.getTextureAt(context.relativeTick + context.transition);

        if (link == null)
        {
            return;
        }

        List<ImageOverlay> images = getImages(context);
        float t = context.relativeTick + context.transition;
        float factor = this.envelope.factorEnabled(this.duration.get(), t);
        float alpha = factor * (float) this.interp(this.opacity, t, 1D);

        if (alpha <= 0F)
        {
            return;
        }

        Color tinted = this.color.get().copy();
        tinted.a *= alpha;

        TextureBlend textureBlend = this.getTextureBlend(t);

        this.overlay.updateTexture(
            link,
            this.linear.get(),
            this.mipmap.get(),
            this.resizeCrop.get(),
            this.crop.get(),
            tinted,
            (float) this.interp(this.offsetX, t, 0D),
            (float) this.interp(this.offsetY, t, 0D),
            (float) this.interp(this.rotation, t, 0D),
            textureBlend
        );
        this.overlay.updateLayout(
            (int) Math.round(this.interp(this.x, t, 0D)),
            (int) Math.round(this.interp(this.y, t, 0D)),
            (int) Math.round(this.interp(this.width, t, 100D)),
            (int) Math.round(this.interp(this.height, t, 100D)),
            (float) this.interp(this.anchorX, t, 0.5D),
            (float) this.interp(this.anchorY, t, 0.5D),
            (float) this.interp(this.windowX, t, 0.5D),
            (float) this.interp(this.windowY, t, 0.5D),
            alpha
        );
        this.overlay.renderOrder = context.count;
        images.add(this.overlay);
    }

    private Link getTextureAt(float t)
    {
        if (!this.textureTrack.isEmpty())
        {
            return this.textureTrack.interpolate(t);
        }

        return this.texture.get();
    }

    private TextureBlend getTextureBlend(float t)
    {
        float blendValue = (float) this.interp(this.blend, t, 0D);

        if (blendValue > 0F)
        {
            return new TextureBlend(this.blendFrom.get(), this.blendTo.get(), blendValue);
        }

        KeyframeSegment<Link> segment = this.textureTrack.find(t);

        if (segment != null && segment.a.isBend() && !segment.isSame())
        {
            float blendFactor = (float) segment.a.getInterpolation().interpolate(0D, 1D, segment.x);
            Link from = LinkUtils.copy(segment.a.getValue());
            Link to = LinkUtils.copy(segment.b.getValue());

            return new TextureBlend(from, to, blendFactor);
        }

        return null;
    }

    private double interp(KeyframeChannel<Double> channel, float t, double fallback)
    {
        if (channel.isEmpty())
        {
            return fallback;
        }

        return channel.interpolate(t);
    }

    @Override
    public boolean isPositionClip()
    {
        return false;
    }

    @Override
    protected Clip create()
    {
        return new ImageClip();
    }
}
