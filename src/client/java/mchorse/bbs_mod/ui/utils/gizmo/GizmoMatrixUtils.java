package mchorse.bbs_mod.ui.utils.gizmo;

import mchorse.bbs_mod.forms.renderers.utils.MatrixCacheEntry;
import mchorse.bbs_mod.utils.pose.Transform;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Shared gizmo-basis helpers used to keep drag math aligned with
 * {@link mchorse.bbs_mod.ui.model_blocks.UIModelBlockPanel}'s world-space gizmo frame.
 */
public final class GizmoMatrixUtils
{
    private GizmoMatrixUtils()
    {}

    /**
     * Bone gizmo basis for film pose keyframes ({@link mchorse.bbs_mod.film.BaseFilmController#renderAxes}).
     * Non-local mode uses the bone pivot matrix; local mode keeps the posed rotation with pivot translation.
     */
    public static Matrix4f resolveFilmPoseBoneMatrix(MatrixCacheEntry entry, boolean local)
    {
        if (entry == null)
        {
            return null;
        }

        if (local)
        {
            Matrix4f localMatrix = entry.matrix();
            Matrix4f originMatrix = entry.origin();

            if (localMatrix != null && originMatrix != null)
            {
                Matrix4f matrix = new Matrix4f(localMatrix);

                matrix.setTranslation(originMatrix.getTranslation(new Vector3f()));

                return matrix;
            }

            if (localMatrix != null)
            {
                return new Matrix4f(localMatrix);
            }

            if (originMatrix != null)
            {
                return new Matrix4f(originMatrix);
            }

            return null;
        }

        Matrix4f matrix = entry.origin();

        if (matrix == null)
        {
            matrix = entry.matrix();
        }

        return matrix == null ? null : new Matrix4f(matrix);
    }

    /**
     * Bakes the edited transform's rotation into a position-only gizmo matrix when local mode
     * is active, matching the model block gizmo's {@code gizmoWorldMatrix} composition.
     */
    public static Matrix4f withLocalRotation(Matrix4f matrix, Transform transform, boolean local)
    {
        if (matrix == null || !local || transform == null)
        {
            return matrix;
        }

        Matrix4f result = new Matrix4f(matrix);

        result.mul(new Matrix4f(transform.createRotationMatrix()));

        return result;
    }

    /** Converts a camera-relative gizmo matrix from the world render pass into world space. */
    public static Matrix4f cameraRelativeToWorld(Matrix4f cameraView, Matrix4f cameraRelative)
    {
        return new Matrix4f(cameraView).invert().mul(cameraRelative);
    }
}
