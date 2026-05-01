package mchorse.bbs_mod.cubic.model;

import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;

public class PhysBoneSlot extends ValueGroup
{
    public final ValueString bone = new ValueString("bone", "");
    public final ValueBoolean enabled = new ValueBoolean("enabled", true);
    public final ValueBoolean pitch = new ValueBoolean("pitch", true);
    public final ValueFloat stiffness = new ValueFloat("stiffness", 8F);
    public final ValueFloat damping = new ValueFloat("damping", 1.75F);
    public final ValueFloat gravity = new ValueFloat("gravity", 0.2F);
    public final ValueFloat inertia = new ValueFloat("inertia", 1F);
    public final ValueFloat simSpeed = new ValueFloat("sim_speed", 1F);
    public final ValueFloat maxAngle = new ValueFloat("max_angle", 45F);

    public PhysBoneSlot(String id)
    {
        super(id);

        this.add(this.bone);
        this.add(this.enabled);
        this.add(this.pitch);
        this.add(this.stiffness);
        this.add(this.damping);
        this.add(this.gravity);
        this.add(this.inertia);
        this.add(this.simSpeed);
        this.add(this.maxAngle);
    }
}
