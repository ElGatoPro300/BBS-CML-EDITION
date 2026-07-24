package mchorse.bbs_mod.cubic.render;

import mchorse.bbs_mod.bobj.BOBJBone;
import mchorse.bbs_mod.cubic.IModel;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.model.bobj.BOBJModel;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Map;
import java.util.Set;

/**
 * Collects world-space pivot frames for a set of bones (cubic or BOBJ).
 */
public final class BoneFrameCollector
{
    private BoneFrameCollector()
    {
    }

    public static void collect(IModel model, Set<String> wanted, Map<String, CubicRenderer.PivotFrame> out)
    {
        collect(model, wanted, out, null, false);
    }

    public static void collect(IModel model, Set<String> wanted, Map<String, CubicRenderer.PivotFrame> out, Matrix4f baseTransform)
    {
        collect(model, wanted, out, baseTransform, false);
    }

    /**
     * @param applyStretch fold each bone's IK stretch offset into the frames
     * when the model supports it (cubic {@code offset}). BOBJ stretch rides the
     * skinning matrix, so there is nothing to fold in for BOBJ.
     */
    public static void collect(IModel model, Set<String> wanted, Map<String, CubicRenderer.PivotFrame> out, Matrix4f baseTransform, boolean applyStretch)
    {
        if (model == null || wanted == null || wanted.isEmpty() || out == null)
        {
            return;
        }

        if (model instanceof Model cubic)
        {
            collectCubicPivotFrames(cubic, wanted, out, baseTransform, applyStretch);
            return;
        }

        if (model instanceof BOBJModel bobj)
        {
            collectBobjPivotFrames(bobj, wanted, out, baseTransform);
        }
    }

    /**
     * Collects pivot frames for cubic models (inline replacement for CubicRenderer.collectPivotFrames
     * which is not available in 1.21.11).
     */
    private static void collectCubicPivotFrames(Model model, Set<String> wanted, Map<String, CubicRenderer.PivotFrame> out, Matrix4f baseTransform, boolean applyStretch)
    {
        Vector3f baseTranslation = null;
        Quaternionf baseRotation = null;

        if (baseTransform != null)
        {
            baseTranslation = baseTransform.getTranslation(new Vector3f());
            baseRotation = baseTransform.getNormalizedRotation(new Quaternionf());
        }

        for (ModelGroup group : model.getAllGroups())
        {
            if (group == null || !wanted.contains(group.id))
            {
                continue;
            }

            Vector3f position = new Vector3f(group.current.translate.x / 16F, group.current.translate.y / 16F, group.current.translate.z / 16F);
            Quaternionf parentRotation = new Quaternionf();
            Quaternionf worldRotation = new Quaternionf();

            if (baseRotation != null && baseTranslation != null)
            {
                baseRotation.transform(position);
                position.add(baseTranslation);
                parentRotation = new Quaternionf(baseRotation);
                worldRotation = new Quaternionf(baseRotation);
            }

            out.put(group.id, new CubicRenderer.PivotFrame(position, parentRotation, worldRotation));
        }
    }

    private static void collectBobjPivotFrames(BOBJModel model, Set<String> wanted, Map<String, CubicRenderer.PivotFrame> out, Matrix4f baseTransform)
    {
        Vector3f baseTranslation = null;
        Quaternionf baseRotation = null;

        if (baseTransform != null)
        {
            baseTranslation = baseTransform.getTranslation(new Vector3f());
            baseRotation = baseTransform.getNormalizedRotation(new Quaternionf());
        }

        model.getArmature().setupMatrices();

        for (BOBJBone bone : model.getArmature().orderedBones)
        {
            if (bone == null || !wanted.contains(bone.name))
            {
                continue;
            }

            Vector3f position = bone.originMat.getTranslation(new Vector3f());
            Quaternionf parentRotation = bone.originMat.getNormalizedRotation(new Quaternionf());
            Quaternionf worldRotation = bone.mat.getNormalizedRotation(new Quaternionf());

            if (baseRotation != null && baseTranslation != null)
            {
                baseRotation.transform(position);
                position.add(baseTranslation);

                parentRotation = new Quaternionf(baseRotation).mul(parentRotation);
                worldRotation = new Quaternionf(baseRotation).mul(worldRotation);
            }

            out.put(bone.name, new CubicRenderer.PivotFrame(position, parentRotation, worldRotation));
        }
    }
}
