package mchorse.bbs_mod.cubic.model;

import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;

public class IKJointConstraint extends ValueGroup
{
    public final ValueString bone = new ValueString("bone", "");
    public final ValueFloat minX = new ValueFloat("min_x", -180F);
    public final ValueFloat maxX = new ValueFloat("max_x", 180F);
    public final ValueFloat minY = new ValueFloat("min_y", -180F);
    public final ValueFloat maxY = new ValueFloat("max_y", 180F);
    public final ValueFloat minZ = new ValueFloat("min_z", -180F);
    public final ValueFloat maxZ = new ValueFloat("max_z", 180F);

    public IKJointConstraint(String id)
    {
        super(id);

        this.add(this.bone);
        this.add(this.minX);
        this.add(this.maxX);
        this.add(this.minY);
        this.add(this.maxY);
        this.add(this.minZ);
        this.add(this.maxZ);
    }
}
