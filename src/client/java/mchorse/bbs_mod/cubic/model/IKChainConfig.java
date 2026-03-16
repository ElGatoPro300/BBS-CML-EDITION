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
    public final ValueInt iterations = new ValueInt("iterations", 8);
    public final ValueFloat tolerance = new ValueFloat("tolerance", 0.2F);
    public final ValueFloat minX = new ValueFloat("min_x", -180F);
    public final ValueFloat maxX = new ValueFloat("max_x", 180F);
    public final ValueFloat minY = new ValueFloat("min_y", -180F);
    public final ValueFloat maxY = new ValueFloat("max_y", 180F);
    public final ValueFloat minZ = new ValueFloat("min_z", -180F);
    public final ValueFloat maxZ = new ValueFloat("max_z", 180F);
    public final Transform target = new Transform();
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
        this.add(this.iterations);
        this.add(this.tolerance);
        this.add(this.minX);
        this.add(this.maxX);
        this.add(this.minY);
        this.add(this.maxY);
        this.add(this.minZ);
        this.add(this.maxZ);
        this.add(this.bones);
    }

    @Override
    public BaseType toData()
    {
        MapType data = (MapType) super.toData();
        Transform transform = this.target.copy();

        transform.toDeg();
        data.put("target_transform", transform.toData());

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
}
