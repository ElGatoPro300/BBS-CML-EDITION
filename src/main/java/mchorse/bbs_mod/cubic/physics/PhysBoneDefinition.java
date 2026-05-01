package mchorse.bbs_mod.cubic.physics;

import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.MapType;

public class PhysBoneDefinition implements IMapSerializable
{
    public String bone = "";
    public boolean enabled = true;
    public boolean affectPitch = true;
    public float stiffness = 8F;
    public float damping = 1.75F;
    public float gravity = 0.2F;
    public float inertia = 1F;
    public float simSpeed = 1F;
    public float maxAngle = 45F;

    @Override
    public void toData(MapType data)
    {
        data.putString("bone", this.bone);
        data.putBool("enabled", this.enabled);
        data.putBool("pitch", this.affectPitch);
        data.putFloat("stiffness", this.stiffness);
        data.putFloat("damping", this.damping);
        data.putFloat("gravity", this.gravity);
        data.putFloat("inertia", this.inertia);
        data.putFloat("sim_speed", this.simSpeed);
        data.putFloat("max_angle", this.maxAngle);
    }

    @Override
    public void fromData(MapType data)
    {
        this.bone = data.getString("bone", this.bone);
        this.enabled = data.getBool("enabled", this.enabled);
        this.affectPitch = data.getBool("pitch", this.affectPitch);
        this.stiffness = data.getFloat("stiffness", this.stiffness);
        this.damping = data.getFloat("damping", this.damping);
        this.gravity = data.getFloat("gravity", this.gravity);
        this.inertia = data.getFloat("inertia", this.inertia);
        this.simSpeed = data.getFloat("sim_speed", this.simSpeed);
        this.maxAngle = data.getFloat("max_angle", this.maxAngle);
    }

    public PhysBoneDefinition copy()
    {
        PhysBoneDefinition copy = new PhysBoneDefinition();

        copy.bone = this.bone;
        copy.enabled = this.enabled;
        copy.affectPitch = this.affectPitch;
        copy.stiffness = this.stiffness;
        copy.damping = this.damping;
        copy.gravity = this.gravity;
        copy.inertia = this.inertia;
        copy.simSpeed = this.simSpeed;
        copy.maxAngle = this.maxAngle;

        return copy;
    }
}
