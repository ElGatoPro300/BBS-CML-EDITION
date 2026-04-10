package mchorse.bbs_mod.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import mchorse.bbs_mod.utils.joml.Vectors;
import mchorse.bbs_mod.utils.pose.Transform;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

public class MatrixStackUtils
{
    private static Matrix3f normal = new Matrix3f();

    private static Matrix4f oldProjection = new Matrix4f();
    private static Matrix4f oldMV = new Matrix4f();
    private static Matrix3f oldInverse = new Matrix3f();

    public static void scaleStack(PoseStack stack, float x, float y, float z)
    {
        new Matrix4f().scale(x, y, z);
        stack.last().normal().scale(x < 0F ? -1F : 1F, y < 0F ? -1F : 1F, z < 0F ? -1F : 1F);
    }

    public static void cacheMatrices()
    {
        /* Cache the global stuff */
        oldProjection.set(RenderSystem.getModelViewMatrix());
        oldMV.set(RenderSystem.getModelViewMatrix());
        oldInverse.set(new Matrix3f(RenderSystem.getModelViewMatrix()));

        Matrix4fStack mvStack = RenderSystem.getModelViewStack();
        mvStack.identity();
        applyModelViewMatrix();
    }

    public static void restoreMatrices()
    {
        /* Return back to orthographic projection */
        /* projection matrix state managed by 1.21.11 renderer */

        Matrix4fStack mvStack = RenderSystem.getModelViewStack();
        mvStack.set(oldMV);
        applyModelViewMatrix();
    }

    public static void applyModelViewMatrix()
    {
        // 1.21.11 no longer exposes direct shader uniform mutation for this path.
    }

    public static void applyTransform(PoseStack stack, Transform transform)
    {
        stack.translate(transform.translate.x, transform.translate.y, transform.translate.z);

        if (transform.pivot.x != 0F || transform.pivot.y != 0F || transform.pivot.z != 0F)
        {
            stack.translate(transform.pivot.x, transform.pivot.y, transform.pivot.z);
        }

        stack.mulPose(Axis.ZP.rotation(transform.rotate.z));
        stack.mulPose(Axis.YP.rotation(transform.rotate.y));
        stack.mulPose(Axis.XP.rotation(transform.rotate.x));
        stack.mulPose(Axis.ZP.rotation(transform.rotate2.z));
        stack.mulPose(Axis.YP.rotation(transform.rotate2.y));
        stack.mulPose(Axis.XP.rotation(transform.rotate2.x));
        scaleStack(stack, transform.scale.x, transform.scale.y, transform.scale.z);

        if (transform.pivot.x != 0F || transform.pivot.y != 0F || transform.pivot.z != 0F)
        {
            stack.translate(-transform.pivot.x, -transform.pivot.y, -transform.pivot.z);
        }
    }

    public static void multiply(PoseStack stack, Matrix4f matrix)
    {
        normal.set(matrix);
        normal.getScale(Vectors.TEMP_3F);

        Vectors.TEMP_3F.x = Vectors.TEMP_3F.x == 0F ? 0F : 1F / Vectors.TEMP_3F.x;
        Vectors.TEMP_3F.y = Vectors.TEMP_3F.y == 0F ? 0F : 1F / Vectors.TEMP_3F.y;
        Vectors.TEMP_3F.z = Vectors.TEMP_3F.z == 0F ? 0F : 1F / Vectors.TEMP_3F.z;

        normal.scale(Vectors.TEMP_3F);

        new Matrix4f().mul(matrix);
        stack.last().normal().mul(normal);
    }

    public static void scaleBack(PoseStack matrices)
    {
        Matrix4f position = new Matrix4f();

        float scaleX = (float) Math.sqrt(position.m00() * position.m00() + position.m10() * position.m10() + position.m20() * position.m20());
        float scaleY = (float) Math.sqrt(position.m01() * position.m01() + position.m11() * position.m11() + position.m21() * position.m21());
        float scaleZ = (float) Math.sqrt(position.m02() * position.m02() + position.m12() * position.m12() + position.m22() * position.m22());

        float max = Math.max(scaleX, Math.max(scaleY, scaleZ));

        position.m00(position.m00() / max);
        position.m10(position.m10() / max);
        position.m20(position.m20() / max);

        position.m01(position.m01() / max);
        position.m11(position.m11() / max);
        position.m21(position.m21() / max);

        position.m02(position.m02() / max);
        position.m12(position.m12() / max);
        position.m22(position.m22() / max);
    }

    public static Matrix4f stripScale(Matrix4f matrix)
    {
        Matrix4f m = new Matrix4f(matrix);

        float sx = (float) Math.sqrt(m.m00() * m.m00() + m.m01() * m.m01() + m.m02() * m.m02());
        float sy = (float) Math.sqrt(m.m10() * m.m10() + m.m11() * m.m11() + m.m12() * m.m12());
        float sz = (float) Math.sqrt(m.m20() * m.m20() + m.m21() * m.m21() + m.m22() * m.m22());

        if (sx != 0F)
        {
            m.m00(m.m00() / sx);
            m.m01(m.m01() / sx);
            m.m02(m.m02() / sx);
        }

        if (sy != 0F)
        {
            m.m10(m.m10() / sy);
            m.m11(m.m11() / sy);
            m.m12(m.m12() / sy);
        }

        if (sz != 0F)
        {
            m.m20(m.m20() / sz);
            m.m21(m.m21() / sz);
            m.m22(m.m22() / sz);
        }

        return m;
    }
}
