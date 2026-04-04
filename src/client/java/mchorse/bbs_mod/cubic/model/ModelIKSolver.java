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
        List<ModelGroup> groups = resolveGroups(model, chain);

        if (groups.size() < 2)
        {
            return;
        }

        int iterations = Math.max(1, Math.min(chain.iterations.get(), 64));
        float tolerance = Math.max(0.001F, chain.tolerance.get());
        float blend = MathUtils.clamp(chain.blend.get(), 0F, 1F);
        float positionWeight = MathUtils.clamp(chain.positionWeight.get(), 0F, 1F);
        float rotationWeight = MathUtils.clamp(chain.rotationWeight.get(), 0F, 1F);
        float effectorRotationWeight = MathUtils.clamp(chain.effectorRotationWeight.get(), 0F, 1F);
        boolean useCCD = chain.useCCD.get();
        boolean allowStretch = chain.stretch.get();
        float stretchLimit = Math.max(1F, chain.stretchLimit.get());
        Map<ModelGroup, Matrix4f> matrices = collectMatrices(model);
        List<Vector3f> current = collectPositions(groups, matrices);

        if (current.size() < 2)
        {
            return;
        }

        Vector3f target = resolveTarget(model, chain, matrices, groups);

        if (target == null)
        {
            return;
        }

        Vector3f effectorStart = new Vector3f(current.get(current.size() - 1));
        target.lerp(effectorStart, 1F - blend * positionWeight);

        float[] lengths = collectLengths(current);
        float totalLength = totalLength(lengths);

        if (totalLength < 0.00001F)
        {
            return;
        }

        List<Vector3f> solved = clonePositions(current);
        Vector3f root = new Vector3f(solved.get(0));
        Vector3f rootToTarget = new Vector3f(target).sub(root);
        float distance = rootToTarget.length();

        if (distance > totalLength)
        {
            if (allowStretch)
            {
                float desired = Math.min(distance, totalLength * stretchLimit);
                float factor = desired / totalLength;

                for (int i = 0; i < lengths.length; i++)
                {
                    lengths[i] *= factor;
                }
            }
            else if (distance > 0.00001F)
            {
                target = rootToTarget.normalize().mul(totalLength).add(root);
            }
        }

        if (useCCD)
        {
            ccdSolve(solved, target, iterations, tolerance);
        }
        else
        {
            fabrikSolve(solved, lengths, target, root, iterations, tolerance);
        }

        Vector3f pole = resolvePole(model, chain, matrices);

        if (pole != null && groups.size() > 2)
        {
            applyPole(solved, pole, chain.poleAngleOffset.get());
        }

        for (int i = 0; i < groups.size() - 1; i++)
        {
            ModelGroup joint = groups.get(i);
            ModelGroup child = groups.get(i + 1);
            matrices = collectMatrices(model);
            Matrix4f jointMatrix = matrices.get(joint);
            Matrix4f childMatrix = matrices.get(child);

            if (jointMatrix == null || childMatrix == null)
            {
                continue;
            }

            Vector3f jointPosition = matrixPosition(jointMatrix);
            Vector3f childPosition = matrixPosition(childMatrix);
            Vector3f currentDirection = childPosition.sub(jointPosition, new Vector3f());
            Vector3f desiredDirection = new Vector3f(solved.get(i + 1)).sub(solved.get(i));

            if (currentDirection.lengthSquared() < 0.000001F || desiredDirection.lengthSquared() < 0.000001F)
            {
                continue;
            }

            currentDirection.normalize();
            desiredDirection.normalize();

            Quaternionf worldDelta = new Quaternionf().rotateTo(currentDirection, desiredDirection);
            Quaternionf parentRotation = new Quaternionf();

            if (joint.parent != null && matrices.get(joint.parent) != null)
            {
                matrices.get(joint.parent).getNormalizedRotation(parentRotation);
            }

            Quaternionf localDelta = new Quaternionf(parentRotation).invert().mul(worldDelta).mul(parentRotation);
            IKJointConstraint constraint = chain.getJointConstraint(joint.id);
            float jointRotationWeight = i == groups.size() - 2 ? effectorRotationWeight : rotationWeight;
            localDelta = dampLocalDelta(localDelta, chain, constraint, jointRotationWeight);
            Quaternionf currentRotation = toQuaternion(joint);
            Quaternionf result = localDelta.mul(currentRotation, new Quaternionf());

            applyQuaternion(joint, result, chain, joint.id);
        }
    }

    private static List<ModelGroup> resolveGroups(Model model, IKChainConfig chain)
    {
        List<String> bones = chain.getBones();
        List<ModelGroup> groups = new ArrayList<>();

        for (String bone : bones)
        {
            ModelGroup group = model.getGroup(bone);

            if (group != null)
            {
                groups.add(group);
            }
        }

        int chainLength = Math.max(0, chain.chainLength.get());

        if (chainLength > 0 && chainLength < groups.size())
        {
            groups = new ArrayList<>(groups.subList(groups.size() - chainLength, groups.size()));
        }

        return groups;
    }

    private static Vector3f resolveTarget(Model model, IKChainConfig chain, Map<ModelGroup, Matrix4f> matrices, List<ModelGroup> groups)
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

        Vector3f target = new Vector3f(chain.target.translate);

        if (!chain.targetParentBone.get().isEmpty() && !isDrivenBone(chain.targetParentBone.get(), groups))
        {
            ModelGroup parent = model.getGroup(chain.targetParentBone.get());

            if (parent != null)
            {
                Matrix4f matrix = matrices.get(parent);

                if (matrix != null)
                {
                    Vector3f world = new Vector3f();
                    matrix.transformPosition(target.mul(1F / 16F), world);

                    return world.mul(16F);
                }
            }
        }

        return target;
    }

    private static boolean isDrivenBone(String bone, List<ModelGroup> groups)
    {
        if (bone == null || bone.isEmpty() || groups == null)
        {
            return false;
        }

        for (ModelGroup group : groups)
        {
            if (group != null && bone.equals(group.id))
            {
                return true;
            }
        }

        return false;
    }

    private static Vector3f resolvePole(Model model, IKChainConfig chain, Map<ModelGroup, Matrix4f> matrices)
    {
        if (chain.usePoleBone.get() && !chain.poleBone.get().isEmpty())
        {
            ModelGroup group = model.getGroup(chain.poleBone.get());

            if (group != null)
            {
                Matrix4f matrix = matrices.get(group);

                if (matrix != null)
                {
                    return matrixPosition(matrix);
                }
            }
        }

        Vector3f pole = new Vector3f(chain.pole.translate);

        return pole.lengthSquared() < 0.0000001F ? null : pole;
    }

    private static List<Vector3f> collectPositions(List<ModelGroup> groups, Map<ModelGroup, Matrix4f> matrices)
    {
        List<Vector3f> points = new ArrayList<>();

        for (ModelGroup group : groups)
        {
            Matrix4f matrix = matrices.get(group);

            if (matrix != null)
            {
                points.add(matrixPosition(matrix));
            }
        }

        return points;
    }

    private static List<Vector3f> clonePositions(List<Vector3f> points)
    {
        List<Vector3f> clone = new ArrayList<>();

        for (Vector3f point : points)
        {
            clone.add(new Vector3f(point));
        }

        return clone;
    }

    private static float[] collectLengths(List<Vector3f> points)
    {
        float[] lengths = new float[Math.max(0, points.size() - 1)];

        for (int i = 0; i < lengths.length; i++)
        {
            lengths[i] = points.get(i).distance(points.get(i + 1));
        }

        return lengths;
    }

    private static float totalLength(float[] lengths)
    {
        float total = 0F;

        for (float length : lengths)
        {
            total += length;
        }

        return total;
    }

    private static void fabrikSolve(List<Vector3f> points, float[] lengths, Vector3f target, Vector3f root, int iterations, float tolerance)
    {
        int last = points.size() - 1;

        for (int iteration = 0; iteration < iterations; iteration++)
        {
            points.get(last).set(target);

            for (int i = last - 1; i >= 0; i--)
            {
                Vector3f current = points.get(i);
                Vector3f next = points.get(i + 1);
                Vector3f direction = current.sub(next, new Vector3f());

                if (direction.lengthSquared() < 0.0000001F)
                {
                    continue;
                }

                direction.normalize().mul(lengths[i]);
                current.set(next).add(direction);
            }

            points.get(0).set(root);

            for (int i = 1; i <= last; i++)
            {
                Vector3f previous = points.get(i - 1);
                Vector3f current = points.get(i);
                Vector3f direction = current.sub(previous, new Vector3f());

                if (direction.lengthSquared() < 0.0000001F)
                {
                    continue;
                }

                direction.normalize().mul(lengths[i - 1]);
                current.set(previous).add(direction);
            }

            if (points.get(last).distance(target) <= tolerance)
            {
                break;
            }
        }
    }

    private static void ccdSolve(List<Vector3f> points, Vector3f target, int iterations, float tolerance)
    {
        int last = points.size() - 1;

        for (int iteration = 0; iteration < iterations; iteration++)
        {
            Vector3f effector = points.get(last);

            if (effector.distance(target) <= tolerance)
            {
                break;
            }

            for (int i = last - 1; i >= 0; i--)
            {
                Vector3f joint = points.get(i);
                Vector3f toEffector = new Vector3f(effector).sub(joint);
                Vector3f toTarget = new Vector3f(target).sub(joint);

                if (toEffector.lengthSquared() < 0.0000001F || toTarget.lengthSquared() < 0.0000001F)
                {
                    continue;
                }

                toEffector.normalize();
                toTarget.normalize();

                Quaternionf rotation = new Quaternionf().rotateTo(toEffector, toTarget);

                for (int j = i + 1; j <= last; j++)
                {
                    Vector3f point = points.get(j);
                    Vector3f offset = point.sub(joint, new Vector3f());

                    rotation.transform(offset);
                    point.set(joint).add(offset);
                }

                effector = points.get(last);

                if (effector.distance(target) <= tolerance)
                {
                    break;
                }
            }
        }
    }

    private static void applyPole(List<Vector3f> points, Vector3f pole, float angleOffset)
    {
        for (int i = 1; i < points.size() - 1; i++)
        {
            Vector3f previous = points.get(i - 1);
            Vector3f current = points.get(i);
            Vector3f next = points.get(i + 1);
            Vector3f axis = new Vector3f(next).sub(previous);

            if (axis.lengthSquared() < 0.0000001F)
            {
                continue;
            }

            axis.normalize();

            Vector3f projectedCurrent = projectOnPlane(current, previous, axis);
            Vector3f projectedPole = projectOnPlane(pole, previous, axis);
            Vector3f from = projectedCurrent.sub(previous, new Vector3f());
            Vector3f to = projectedPole.sub(previous, new Vector3f());

            if (from.lengthSquared() < 0.0000001F || to.lengthSquared() < 0.0000001F)
            {
                continue;
            }

            from.normalize();
            to.normalize();

            float angle = from.angleSigned(to, axis) + MathUtils.toRad(angleOffset);
            Quaternionf rotation = new Quaternionf().fromAxisAngleRad(axis, angle);
            Vector3f offset = current.sub(previous, new Vector3f());

            rotation.transform(offset);
            current.set(previous).add(offset);
        }
    }

    private static Vector3f projectOnPlane(Vector3f point, Vector3f planePoint, Vector3f planeNormal)
    {
        float distance = new Vector3f(point).sub(planePoint).dot(planeNormal);

        return new Vector3f(point).sub(new Vector3f(planeNormal).mul(distance));
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

    private static Quaternionf dampLocalDelta(Quaternionf localDelta, IKChainConfig chain, IKJointConstraint constraint, float rotationWeight)
    {
        float sx = constraint != null ? constraint.stiffnessX.get() : chain.stiffnessX.get();
        float sy = constraint != null ? constraint.stiffnessY.get() : chain.stiffnessY.get();
        float sz = constraint != null ? constraint.stiffnessZ.get() : chain.stiffnessZ.get();
        sx = MathUtils.clamp(sx, 0F, 1F);
        sy = MathUtils.clamp(sy, 0F, 1F);
        sz = MathUtils.clamp(sz, 0F, 1F);
        rotationWeight = MathUtils.clamp(rotationWeight, 0F, 1F);
        Vector3f euler = localDelta.getEulerAnglesZYX(new Vector3f());

        return new Quaternionf()
            .rotateZ(euler.z * (1F - sz) * rotationWeight)
            .rotateY(euler.y * (1F - sy) * rotationWeight)
            .rotateX(euler.x * (1F - sx) * rotationWeight);
    }

    private static void applyQuaternion(ModelGroup group, Quaternionf quaternion, IKChainConfig chain, String bone)
    {
        IKJointConstraint constraint = chain.getJointConstraint(bone);
        Vector3f euler = quaternion.getEulerAnglesZYX(new Vector3f());
        float minX = constraint != null ? constraint.minX.get() : chain.minX.get();
        float maxX = constraint != null ? constraint.maxX.get() : chain.maxX.get();
        float minY = constraint != null ? constraint.minY.get() : chain.minY.get();
        float maxY = constraint != null ? constraint.maxY.get() : chain.maxY.get();
        float minZ = constraint != null ? constraint.minZ.get() : chain.minZ.get();
        float maxZ = constraint != null ? constraint.maxZ.get() : chain.maxZ.get();
        float rx = clampDeg(MathUtils.toDeg(euler.x), minX, maxX);
        float ry = clampDeg(MathUtils.toDeg(euler.y), minY, maxY);
        float rz = clampDeg(MathUtils.toDeg(euler.z), minZ, maxZ);

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
