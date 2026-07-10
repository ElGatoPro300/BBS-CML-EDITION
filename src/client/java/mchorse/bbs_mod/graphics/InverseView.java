package mchorse.bbs_mod.graphics;

import org.joml.Matrix3f;

/**
 * Holds the inverse of the current view rotation, mirroring the global that
 * {@code RenderSystem} maintained before 1.21.1 removed it. The world render
 * pass feeds it from the game camera, while UI model previews override it with
 * their own camera before rendering.
 */
public class InverseView
{
    private static final Matrix3f matrix = new Matrix3f();

    public static Matrix3f get()
    {
        return matrix;
    }

    public static void set(Matrix3f value)
    {
        matrix.set(value);
    }
}
