package mchorse.bbs_mod.cubic.model;

import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.utils.MathUtils;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelIKSolver
{
    public static void apply(ModelInstance model)
    {
        if (!(model.model instanceof Model cubicModel))
        {
            return;
        }

        if (model.ikChains.isEmpty())
        {
            return;
        }

        for (IKChainConfig chain : model.ikChains)
        {
            if (!chain.enabled.get())
            {
                continue;
            }

            applyChain(cubicModel, chain);
        }
    }

    private static void applyChain(Model model, IKChainConfig chain)
    {
        List<ModelGroup> groups = resolveGroups(model, chain.getBones());

        if (groups.isEmpty())
        {
            return;
        }

        int iterations = Math.max(1, Math.min(chain.iterations.get(), 32));
        float tolerance = Math.max(0.001F, chain.tolerance.get());

        for (int iteration = 0; iteration < iterations; iteration++)
        {
            Map<ModelGroup, Matrix4f> matrices = collectMatrices(model);
            Vector3f target = resolveTarget(model, chain, matrices);

            if (target == null)
            {
                return;
            }

            Vector3f effectorPosition = matrixPosition(matrices.get(groups.get(groups.size() - 1)));

            if (effectorPosition.distance(target) <= tolerance)
            {
                return;
            }

            for (int i = groups.size() - 1; i >= 0; i--)
            {
                ModelGroup joint = groups.get(i);
                Matrix4f jointMatrix = matrices.get(joint);

                if (jointMatrix == null)
                {
                    continue;
                }

                effectorPosition = matrixPosition(matrices.get(groups.get(groups.size() - 1)));

                if (effectorPosition.distance(target) <= tolerance)
                {
                    return;
                }

                Vector3f jointPosition = matrixPosition(jointMatrix);
                Vector3f toEffector = effectorPosition.sub(jointPosition, new Vector3f());
                Vector3f toTarget = target.sub(jointPosition, new Vector3f());

                if (toEffector.lengthSquared() < 0.00001F || toTarget.lengthSquared() < 0.00001F)
                {
                    continue;
                }

                toEffector.normalize();
                toTarget.normalize();

                Quaternionf worldDelta = new Quaternionf().rotateTo(toEffector, toTarget);
                Quaternionf parentRotation = new Quaternionf();

                if (joint.parent != null && matrices.get(joint.parent) != null)
                {
                    matrices.get(joint.parent).getNormalizedRotation(parentRotation);
                }

                Quaternionf localDelta = new Quaternionf(parentRotation)
                    .invert()
                    .mul(worldDelta)
                    .mul(parentRotation);

                Quaternionf current = toQuaternion(joint);
                Quaternionf result = localDelta.mul(current, new Quaternionf());

                applyQuaternion(joint, result, chain);
                matrices = collectMatrices(model);
            }
        }
    }

    private static List<ModelGroup> resolveGroups(Model model, List<String> bones)
    {
        List<ModelGroup> groups = new ArrayList<>();

        for (String bone : bones)
        {
            ModelGroup group = model.getGroup(bone);

            if (group != null)
            {
                groups.add(group);
            }
        }

        return groups;
    }

    private static Vector3f resolveTarget(Model model, IKChainConfig chain, Map<ModelGroup, Matrix4f> matrices)
    {
        if (chain.useTargetBone.get() && !chain.targetBone.get().isEmpty())
        {
            ModelGroup group = model.getGroup(chain.targetBone.get());

            if (group != null)
            {
                Matrix4f matrix = matrices.get(group);

                if (matrix != null)
                {
                    return matrixPosition(matrix);
                }
            }
        }

        return new Vector3f(chain.target.translate);
    }

    private static Map<ModelGroup, Matrix4f> collectMatrices(Model model)
    {
        Map<ModelGroup, Matrix4f> matrices = new HashMap<>();
        Matrix4f identity = new Matrix4f();

        for (ModelGroup group : model.topGroups)
        {
            collectMatrices(group, identity, matrices);
        }

        return matrices;
    }

    private static void collectMatrices(ModelGroup group, Matrix4f parent, Map<ModelGroup, Matrix4f> matrices)
    {
        Matrix4f matrix = new Matrix4f(parent);
        Vector3f translate = group.current.translate;
        Vector3f pivot = group.current.pivot;

        matrix.translate(
            -(translate.x - pivot.x) / 16F,
            (translate.y - pivot.y) / 16F,
            (translate.z - pivot.z) / 16F
        );
        matrix.translate(pivot.x / 16F, pivot.y / 16F, pivot.z / 16F);
        matrix.rotateZ(MathUtils.toRad(group.current.rotate.z));
        matrix.rotateY(MathUtils.toRad(group.current.rotate.y));
        matrix.rotateX(MathUtils.toRad(group.current.rotate.x));
        matrix.rotateZ(MathUtils.toRad(group.current.rotate2.z));
        matrix.rotateY(MathUtils.toRad(group.current.rotate2.y));
        matrix.rotateX(MathUtils.toRad(group.current.rotate2.x));
        matrix.scale(group.current.scale.x, group.current.scale.y, group.current.scale.z);
        matrix.translate(-pivot.x / 16F, -pivot.y / 16F, -pivot.z / 16F);

        matrices.put(group, matrix);

        for (ModelGroup child : group.children)
        {
            collectMatrices(child, matrix, matrices);
        }
    }

    private static Vector3f matrixPosition(Matrix4f matrix)
    {
        if (matrix == null)
        {
            return new Vector3f();
        }

        Vector3f vector = new Vector3f();

        matrix.getTranslation(vector);

        return vector.mul(16F);
    }

    private static Quaternionf toQuaternion(ModelGroup group)
    {
        return new Quaternionf()
            .rotateZ(MathUtils.toRad(group.current.rotate.z))
            .rotateY(MathUtils.toRad(group.current.rotate.y))
            .rotateX(MathUtils.toRad(group.current.rotate.x))
            .rotateZ(MathUtils.toRad(group.current.rotate2.z))
            .rotateY(MathUtils.toRad(group.current.rotate2.y))
            .rotateX(MathUtils.toRad(group.current.rotate2.x));
    }

    private static void applyQuaternion(ModelGroup group, Quaternionf quaternion, IKChainConfig chain)
    {
        Vector3f euler = quaternion.getEulerAnglesZYX(new Vector3f());
        float rx = clampDeg(MathUtils.toDeg(euler.x), chain.minX.get(), chain.maxX.get());
        float ry = clampDeg(MathUtils.toDeg(euler.y), chain.minY.get(), chain.maxY.get());
        float rz = clampDeg(MathUtils.toDeg(euler.z), chain.minZ.get(), chain.maxZ.get());

        group.current.rotate.x = rx;
        group.current.rotate.y = ry;
        group.current.rotate.z = rz;
        group.current.rotate2.set(0F, 0F, 0F);
    }

    private static float clampDeg(float value, float min, float max)
    {
        float low = Math.min(min, max);
        float high = Math.max(min, max);

        return Math.max(low, Math.min(high, value));
    }
}
