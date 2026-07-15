package mchorse.bbs_mod.client;

import net.minecraft.util.math.RotationAxis;

import org.joml.Matrix4f;

/**
 * Rotates the celestial dome around the vertical axis (Mine-imator sun path rotation).
 * The sky pass shares its model-view matrix with later world rendering, so the rotation
 * must be restored after {@code WorldRenderer.renderSky} finishes.
 */
public final class SunPathRotation
{
    private static Matrix4f savedMatrix;

    private SunPathRotation()
    {
    }

    public static void begin(Matrix4f matrix)
    {
        Float degrees = BBSRendering.getSunPathRotationDegrees();

        if (degrees == null || degrees == 0F)
        {
            savedMatrix = null;

            return;
        }

        savedMatrix = new Matrix4f(matrix);
        matrix.rotate(RotationAxis.POSITIVE_Y.rotationDegrees(degrees));
    }

    public static void end(Matrix4f matrix)
    {
        if (savedMatrix != null)
        {
            matrix.set(savedMatrix);
            savedMatrix = null;
        }
    }
}
