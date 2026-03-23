package mchorse.bbs_mod.cubic.model.fbx;

import mchorse.bbs_mod.bobj.BOBJArmature;
import mchorse.bbs_mod.bobj.BOBJBone;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FBXArmature
{
    private final String name;
    private final Map<String, BOBJBone> bones = new HashMap<>();
    private final List<BOBJBone> orderedBones = new ArrayList<>();

    public FBXArmature(String name)
    {
        this.name = name;
    }

    public boolean isEmpty()
    {
        return this.orderedBones.isEmpty();
    }

    public void addBone(String name, String parent, Matrix4f transform)
    {
        if (this.bones.containsKey(name))
        {
            return;
        }

        int index = this.orderedBones.size();
        BOBJBone bone = new BOBJBone(index, name, parent, transform);

        this.bones.put(name, bone);
        this.orderedBones.add(bone);
    }

    public BOBJArmature convert()
    {
        BOBJArmature armature = new BOBJArmature(this.name);

        for (BOBJBone bone : this.orderedBones)
        {
            armature.addBone(bone);
        }

        return armature;
    }
}
