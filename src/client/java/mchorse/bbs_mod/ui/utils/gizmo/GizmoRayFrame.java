package mchorse.bbs_mod.ui.utils.gizmo;

import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.utils.Area;

import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.function.Supplier;

/**
 * Builds the common "perspective camera + viewport area -> mouse ray" gizmo ray provider shared
 * by the Model and Form editors, replacing their two byte-for-byte identical anonymous
 * {@link UIPropTransform.IGizmoRayProvider} implementations.
 *
 * Not used by the Film/Replays or Animation State editors: their gizmo matrix lives in
 * camera-view-relative space (see {@code UIReplaysEditorUtils}), which needs a different ray
 * origin/matrix strategy and is intentionally left as its own provider rather than forced into
 * this shape.
 */
public final class GizmoRayFrame
{
    private GizmoRayFrame()
    {}

    public static UIPropTransform.IGizmoRayProvider fromCamera(Camera camera, Area area, Supplier<Matrix4f> gizmoMatrix)
    {
        return new UIPropTransform.IGizmoRayProvider()
        {
            @Override
            public boolean getMouseRay(UIContext context, int mouseX, int mouseY, Vector3d rayOrigin, Vector3f rayDirection)
            {
                if (area.w <= 0 || area.h <= 0)
                {
                    return false;
                }

                Vector3f direction = camera.getMouseDirection(
                    mouseX,
                    mouseY,
                    context.globalX(area.x),
                    context.globalY(area.y),
                    area.w,
                    area.h
                );

                if (direction.lengthSquared() <= 1.0E-12F)
                {
                    return false;
                }

                rayDirection.set(direction).normalize();
                rayOrigin.set(camera.position.x, camera.position.y, camera.position.z);

                return true;
            }

            @Override
            public boolean getGizmoMatrix(Matrix4f matrix)
            {
                Matrix4f source = gizmoMatrix.get();

                if (source == null)
                {
                    return false;
                }

                matrix.set(source);

                return true;
            }
        };
    }
}
