package mchorse.bbs_mod.cubic.animation.legacy.config;

import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.MapType;

import java.util.Objects;

public class LegacyLimbAnimationConfig implements IMapSerializable
{
    public boolean swinging;
    public boolean swiping;
    public boolean lookX;
    public boolean lookY;
    public boolean idle;
    public boolean invert;
    public boolean wheel;
    public String wheelAxis = "x";
    public float wheelSpeed = 1F;
    public boolean wheelReverse;

    public boolean isEmpty()
    {
        return !this.swiping
            && !this.swinging
            && !this.lookX
            && !this.lookY
            && !this.idle
            && !this.invert
            && !this.wheel;
    }

    public LegacyLimbAnimationConfig copy()
    {
        LegacyLimbAnimationConfig config = new LegacyLimbAnimationConfig();

        config.swinging = this.swinging;
        config.swiping = this.swiping;
        config.lookX = this.lookX;
        config.lookY = this.lookY;
        config.idle = this.idle;
        config.invert = this.invert;
        config.wheel = this.wheel;
        config.wheelAxis = this.wheelAxis;
        config.wheelSpeed = this.wheelSpeed;
        config.wheelReverse = this.wheelReverse;

        return config;
    }

    @Override
    public void toData(MapType data)
    {
        data.putBool("swinging", this.swinging);
        data.putBool("swiping", this.swiping);
        data.putBool("look_x", this.lookX);
        data.putBool("look_y", this.lookY);
        data.putBool("idle", this.idle);
        data.putBool("invert", this.invert);
        data.putBool("wheel", this.wheel);
        data.putString("wheel_axis", this.wheelAxis);
        data.putFloat("wheel_speed", this.wheelSpeed);
        data.putBool("wheel_reverse", this.wheelReverse);
    }

    @Override
    public void fromData(MapType data)
    {
        this.swinging = data.getBool("swinging");
        this.swiping = data.getBool("swiping");
        this.lookX = data.getBool("look_x");
        this.lookY = data.getBool("look_y");
        this.idle = data.getBool("idle");
        this.invert = data.getBool("invert");
        this.wheel = data.getBool("wheel");
        this.wheelAxis = data.getString("wheel_axis", "x");
        this.wheelSpeed = data.getFloat("wheel_speed", 1F);
        this.wheelReverse = data.getBool("wheel_reverse");
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (!(obj instanceof LegacyLimbAnimationConfig config))
        {
            return false;
        }

        return this.swinging == config.swinging
            && this.swiping == config.swiping
            && this.lookX == config.lookX
            && this.lookY == config.lookY
            && this.idle == config.idle
            && this.invert == config.invert
            && this.wheel == config.wheel
            && this.wheelReverse == config.wheelReverse
            && Float.compare(this.wheelSpeed, config.wheelSpeed) == 0
            && Objects.equals(this.wheelAxis, config.wheelAxis);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.swinging, this.swiping, this.lookX, this.lookY, this.idle, this.invert, this.wheel, this.wheelAxis, this.wheelSpeed, this.wheelReverse);
    }
}
