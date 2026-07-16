package mchorse.bbs_mod.forms.forms.utils;

import mchorse.bbs_mod.utils.MathUtils;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Builds inverse transform matrices and evaluates soft box masks for paint.
 */
public class EffectTransformMath
{
    public static final float BILLBOARD_MASK_HALF = 0.45F;
    /** Half-height at scale 1.0; full vertical span is 2x this value (feet to head for humanoids). */
    public static final float MODEL_MASK_HALF_BASE = 1F;
    public static final float MODEL_MASK_Y_BIAS = 1F;

    private static final Matrix4f MATRIX = new Matrix4f();
    private static final Vector3f LOCAL = new Vector3f();

    private EffectTransformMath()
    {}

    public static boolean isTransformActive(EffectTransform transform)
    {
        return transform != null && transform.isActive();
    }

    public static void resolveModelMaskHalfExtents(EffectTransform transform, Vector3f dest)
    {
        resolveMaskHalfExtents(transform, dest, MODEL_MASK_HALF_BASE, MODEL_MASK_Y_BIAS);
    }

    public static void resolveBillboardMaskHalfExtents(EffectTransform transform, Vector3f dest)
    {
        resolveMaskHalfExtents(transform, dest, BILLBOARD_MASK_HALF, 1F);
    }

    public static void resolveBlockMaskHalfExtents(EffectTransform transform, Vector3f dest)
    {
        resolveMaskHalfExtents(transform, dest, 0.5F, 1F);
    }

    private static void resolveMaskHalfExtents(EffectTransform transform, Vector3f dest, float baseHalf, float yBias)
    {
        if (transform == null)
        {
            dest.set(baseHalf, baseHalf * yBias, baseHalf);

            return;
        }

        float scaleX = transform.scaleX == 0F ? 0.001F : transform.scaleX;
        float scaleY = transform.scaleY == 0F ? 0.001F : transform.scaleY;
        float scaleZ = transform.scaleZ == 0F ? 0.001F : transform.scaleZ;

        dest.set(baseHalf * scaleX, baseHalf * yBias * scaleY, baseHalf * scaleZ);
    }

    /**
     * Inverse of translate + rotate only. Scale is applied via mask half-extents.
     */
    public static void buildInverseMatrix(EffectTransform transform, Matrix4f dest)
    {
        if (transform == null)
        {
            dest.identity();

            return;
        }

        MATRIX.identity()
            .translate(transform.offsetX, transform.offsetY, transform.offsetZ)
            .rotateXYZ(MathUtils.toRad(transform.rotateX), MathUtils.toRad(transform.rotateY), MathUtils.toRad(transform.rotateZ));

        dest.set(MATRIX);
        dest.invert();
    }

    /**
     * Soft unit-box mask in effect-local space. Full strength inside the oriented box,
     * smooth falloff near edges.
     */
    public static float mask3DModel(float x, float y, float z, EffectTransform transform, Vector3f halfExtents)
    {
        return mask3DModel(x, y, z, transform, halfExtents, true);
    }

    /**
     * Soft unit-box mask in effect-local space. When {@code bottomAnchoredY} is true the box spans
     * y in [0, 2*halfY] so scale Y maps to model height from the feet upward.
     */
    public static float mask3DModel(float x, float y, float z, EffectTransform transform, Vector3f halfExtents, boolean bottomAnchoredY)
    {
        if (!isTransformActive(transform))
        {
            return 1F;
        }

        buildInverseMatrix(transform, MATRIX);
        LOCAL.set(x, y, z);
        MATRIX.transformPosition(LOCAL);

        if (bottomAnchoredY)
        {
            LOCAL.y -= halfExtents.y;
        }

        float edgeX = Math.abs(LOCAL.x) - halfExtents.x;
        float edgeY = Math.abs(LOCAL.y) - halfExtents.y;
        float edgeZ = Math.abs(LOCAL.z) - halfExtents.z;
        float outsideX = Math.max(edgeX, 0F);
        float outsideY = Math.max(edgeY, 0F);
        float outsideZ = Math.max(edgeZ, 0F);
        float dist = (float) Math.sqrt(outsideX * outsideX + outsideY * outsideY + outsideZ * outsideZ);
        float maxInside = Math.max(edgeX, Math.max(edgeY, edgeZ));

        dist += Math.min(Math.max(maxInside, 0F), 0F);

        if (dist <= 0F)
        {
            return 1F;
        }

        float maxHalf = Math.max(halfExtents.x, Math.max(halfExtents.y, halfExtents.z));
        float falloff = Math.max(maxHalf * 0.15F, 0.1F);

        if (dist >= falloff)
        {
            return 0F;
        }

        return 1F - dist / falloff;
    }

    public static float maskBillboard(float x, float y, float z, EffectTransform transform)
    {
        Vector3f half = new Vector3f();

        resolveBillboardMaskHalfExtents(transform, half);

        return mask3DModel(x, y, z, transform, half, false);
    }

    public static float maskBlock(float x, float y, float z, EffectTransform transform)
    {
        Vector3f half = new Vector3f();

        resolveBlockMaskHalfExtents(transform, half);

        return mask3DModel(x - 0.5F, y, z - 0.5F, transform, half);
    }
}
