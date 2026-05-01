 package mchorse.bbs_mod.camera.clips.misc;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;

import java.util.function.Predicate;

public class VideoClip extends CameraClip
{
    public static final Predicate<Clip> NO_VIDEO = (clip) -> !(clip instanceof VideoClip);

    public ValueString video = new ValueString("video", "");
    public ValueInt offset = new ValueInt("offset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
    /* Volumen del clip: 0–100 */
    public ValueInt volume = new ValueInt("volume", 100, 0, 100);
    public ValueInt x = new ValueInt("x", 0);
    public ValueInt y = new ValueInt("y", 0);
    public ValueInt width = new ValueInt("width", 0);
    public ValueInt height = new ValueInt("height", 0);
    public ValueFloat opacity = new ValueFloat("opacity", 1.0F, 0.0F, 1.0F);
    public ValueBoolean loops = new ValueBoolean("loops", true);
    public ValueBoolean global = new ValueBoolean("global", false);

    public VideoClip()
    {
        super();

        this.add(this.video);
        this.add(this.offset);
        this.add(this.volume);
        this.add(this.x);
        this.add(this.y);
        this.add(this.width);
        this.add(this.height);
        this.add(this.opacity);
        this.add(this.loops);
        this.add(this.global);
    }

    @Override
    public void shiftLeft(int tick)
    {
        super.shiftLeft(tick);

        this.offset.set(this.offset.get() - (this.tick.get() - tick));
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {}

    @Override
    protected Clip create()
    {
        return new VideoClip();
    }

    @Override
    protected void breakDownClip(Clip original, int offset)
    {
        super.breakDownClip(original, offset);

        this.offset.set(this.offset.get() + offset);
    }
}
