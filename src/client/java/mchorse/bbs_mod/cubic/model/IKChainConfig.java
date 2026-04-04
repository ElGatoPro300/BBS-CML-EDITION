package mchorse.bbs_mod.cubic.model;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueList;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.pose.Transform;

import java.util.ArrayList;
import java.util.List;

public class IKChainConfig extends ValueGroup
{
    public final ValueString name = new ValueString("name", "");
    public final ValueBoolean enabled = new ValueBoolean("enabled", true);
    public final ValueBoolean visualizer = new ValueBoolean("visualizer");
    public final ValueBoolean useTargetBone = new ValueBoolean("use_target_bone");
    public final ValueString targetBone = new ValueString("target_bone", "");
    public final ValueString targetParentBone = new ValueString("target_parent_bone", "");
    public final ValueBoolean usePoleBone = new ValueBoolean("use_pole_bone");
    public final ValueString poleBone = new ValueString("pole_bone", "");
    public final ValueInt iterations = new ValueInt("iterations", 8);
    public final ValueBoolean useCCD = new ValueBoolean("use_ccd");
    public final ValueInt chainLength = new ValueInt("chain_length", 0);
    public final ValueFloat tolerance = new ValueFloat("tolerance", 0.2F);
    public final ValueFloat positionWeight = new ValueFloat("position_weight", 1F);
    public final ValueFloat rotationWeight = new ValueFloat("rotation_weight", 1F);
    public final ValueFloat effectorRotationWeight = new ValueFloat("effector_rotation_weight", 1F);
    public final ValueFloat blend = new ValueFloat("blend", 1F);
    public final ValueFloat poleAngleOffset = new ValueFloat("pole_angle_offset", 0F);
    public final ValueBoolean stretch = new ValueBoolean("stretch");
    public final ValueFloat stretchLimit = new ValueFloat("stretch_limit", 1.25F);
    public final ValueFloat minX = new ValueFloat("min_x", -180F);
    public final ValueFloat maxX = new ValueFloat("max_x", 180F);
    public final ValueFloat minY = new ValueFloat("min_y", -180F);
    public final ValueFloat maxY = new ValueFloat("max_y", 180F);
    public final ValueFloat minZ = new ValueFloat("min_z", -180F);
    public final ValueFloat maxZ = new ValueFloat("max_z", 180F);
    public final ValueFloat stiffnessX = new ValueFloat("stiffness_x", 0F);
    public final ValueFloat stiffnessY = new ValueFloat("stiffness_y", 0F);
    public final ValueFloat stiffnessZ = new ValueFloat("stiffness_z", 0F);
    public final ValueList<IKJointConstraint> jointConstraints = new ValueList<IKJointConstraint>("joint_constraints")
    {
        @Override
        protected IKJointConstraint create(String id)
        {
            return new IKJointConstraint(id);
        }
    };
    public final Transform target = new Transform();
    public final Transform pole = new Transform();
    public final ValueList<ValueString> bones = new ValueList<ValueString>("bones")
    {
        @Override
        protected ValueString create(String id)
        {
            return new ValueString(id, "");
        }
    };

    public IKChainConfig(String id)
    {
        super(id);

        this.add(this.name);
        this.add(this.enabled);
        this.add(this.visualizer);
        this.add(this.useTargetBone);
        this.add(this.targetBone);
        this.add(this.targetParentBone);
        this.add(this.usePoleBone);
        this.add(this.poleBone);
        this.add(this.iterations);
        this.add(this.useCCD);
        this.add(this.chainLength);
        this.add(this.tolerance);
        this.add(this.positionWeight);
        this.add(this.rotationWeight);
        this.add(this.effectorRotationWeight);
        this.add(this.blend);
        this.add(this.poleAngleOffset);
        this.add(this.stretch);
        this.add(this.stretchLimit);
        this.add(this.minX);
        this.add(this.maxX);
        this.add(this.minY);
        this.add(this.maxY);
        this.add(this.minZ);
        this.add(this.maxZ);
        this.add(this.stiffnessX);
        this.add(this.stiffnessY);
        this.add(this.stiffnessZ);
        this.add(this.jointConstraints);
        this.add(this.bones);
    }

    @Override
    public BaseType toData()
    {
        MapType data = (MapType) super.toData();
        Transform transform = this.target.copy();
        Transform poleTransform = this.pole.copy();

        transform.toDeg();
        poleTransform.toDeg();
        data.put("target_transform", transform.toData());
        data.put("pole_transform", poleTransform.toData());

        return data;
    }

    @Override
    public void fromData(BaseType data)
    {
        super.fromData(data);

        if (!data.isMap())
        {
            return;
        }

        MapType map = data.asMap();

        if (map.has("target_transform", BaseType.TYPE_MAP))
        {
            this.target.fromData(map.getMap("target_transform"));
            this.target.toRad();
        }

        if (map.has("pole_transform", BaseType.TYPE_MAP))
        {
            this.pole.fromData(map.getMap("pole_transform"));
            this.pole.toRad();
        }
    }

    public List<String> getBones()
    {
        List<String> list = new ArrayList<>();

        for (ValueString value : this.bones.getAllTyped())
        {
            String bone = value.get().trim();

            if (!bone.isEmpty())
            {
                list.add(bone);
            }
        }

        return list;
    }

    public IKJointConstraint getJointConstraint(String bone)
    {
        if (bone == null || bone.isEmpty())
        {
            return null;
        }

        for (IKJointConstraint constraint : this.jointConstraints.getAllTyped())
        {
            if (bone.equals(constraint.bone.get()))
            {
                return constraint;
            }
        }

        return null;
    }

    public IKJointConstraint getOrCreateJointConstraint(String bone)
    {
        IKJointConstraint constraint = this.getJointConstraint(bone);

        if (constraint != null)
        {
            return constraint;
        }

        constraint = new IKJointConstraint(String.valueOf(this.jointConstraints.getAllTyped().size()));
        constraint.bone.set(bone);
        this.jointConstraints.add(constraint);
        this.jointConstraints.sync();

        return constraint;
    }

    public void removeJointConstraint(String bone)
    {
        IKJointConstraint constraint = this.getJointConstraint(bone);

        if (constraint == null)
        {
            return;
        }

        this.jointConstraints.remove(constraint);
        this.jointConstraints.sync();
    }
}
