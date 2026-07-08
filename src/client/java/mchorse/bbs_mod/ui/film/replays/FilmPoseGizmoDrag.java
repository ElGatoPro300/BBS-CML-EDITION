package mchorse.bbs_mod.ui.film.replays;

import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.camera.CameraUtils;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIAnchorKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIPoseKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UITransformKeyframeFactory;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.Gizmo;

import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Film pose gizmo drag frame. Axis rings / translate / scale handles use view-space rays plus
 * the captured pass matrix. The trackball sphere uses the same world-space mouse ray as the
 * model editor ({@link mchorse.bbs_mod.ui.utils.gizmo.GizmoRayFrame}) for screen-basis rotation.
 */
public final class FilmPoseGizmoDrag
{
    private FilmPoseGizmoDrag()
    {}

    public static void prepare(UIFilmPanel panel, Area area, UIPropTransform transform)
    {
        if (panel == null || area == null || transform == null)
        {
            return;
        }

        final Camera camera = panel.getCamera();

        if (camera == null)
        {
            return;
        }

        UIKeyframeEditor keyframeEditor = panel.replayEditor.keyframeEditor;
        boolean poseGizmo = keyframeEditor != null && keyframeEditor.editor instanceof UIPoseKeyframeFactory;
        boolean poseLimbGizmo = false;
        boolean filmTransformGizmo = false;

        if (keyframeEditor != null && keyframeEditor.editor instanceof UITransformKeyframeFactory transformFactory)
        {
            UIKeyframeSheet sheet = keyframeEditor.getSheet(transformFactory.getKeyframe());

            poseLimbGizmo = UITransformKeyframeFactory.isPoseLimbTrack(sheet);
            filmTransformGizmo = !poseLimbGizmo;
        }
        /* Anchor track uses the exact same handle-ray/ring rotation behavior as the plain
         * (non pose-limb) film Transform track: same ray provider, same non-inverted rings,
         * same trackball vertical direction. */
        else if (keyframeEditor != null && keyframeEditor.editor instanceof UIAnchorKeyframeFactory)
        {
            filmTransformGizmo = true;
        }

        if (poseGizmo)
        {
            transform.setInvertGizmoViewRing(false);
            transform.setInvertGizmoTrackball(true);
            transform.setInvertFilmPoseGizmoAxes(true);
        }
        else if (poseLimbGizmo)
        {
            /* Pose-to-limb: axis rings use poseLimbGizmoTuning(); trackball matches main pose. */
            transform.setInvertGizmoViewRing(false);
            transform.setInvertGizmoTrackball(false);
            transform.setInvertFilmPoseGizmoAxes(false);
        }
        else
        {
            transform.setInvertFilmPoseGizmoAxes(false);
        }

        transform.setFilmMatchPoseTrackball(filmTransformGizmo);

        transform.setGizmoRayProvider(new UIPropTransform.IGizmoRayProvider()
        {
            @Override
            public boolean getMouseRay(UIContext context, int mouseX, int mouseY, Vector3d rayOrigin, Vector3f rayDirection)
            {
                if (area.w <= 0 || area.h <= 0)
                {
                    return false;
                }

                FilmPoseGizmoDrag.syncDragCamera(panel, camera);

                int vx = context.globalX(area.x);
                int vy = context.globalY(area.y);

                if (FilmPoseGizmoDrag.isTrackballDrag())
                {
                    return FilmPoseGizmoDrag.fillTrackballMouseRay(camera, vx, vy, area.w, area.h, mouseX, mouseY, rayOrigin, rayDirection);
                }

                return FilmPoseGizmoDrag.fillHandleMouseRay(camera, vx, vy, area.w, area.h, mouseX, mouseY, rayOrigin, rayDirection);
            }

            @Override
            public boolean getGizmoMatrix(Matrix4f matrix)
            {
                return FilmPoseGizmoDrag.fillHandleGizmoMatrix(panel, matrix);
            }
        });
    }

    private static boolean isTrackballDrag()
    {
        return Gizmo.INSTANCE.isDragging() && Gizmo.INSTANCE.getActiveHandle() == Gizmo.STENCIL_TRACKBALL;
    }

    private static boolean fillTrackballMouseRay(Camera camera, int vx, int vy, int vw, int vh, int mouseX, int mouseY, Vector3d rayOrigin, Vector3f rayDirection)
    {
        Vector3f direction = CameraUtils.getMouseDirection(
            camera.projection,
            camera.view,
            mouseX,
            mouseY,
            vx,
            vy,
            vw,
            vh
        );

        if (direction.lengthSquared() <= 1.0E-12F)
        {
            return false;
        }

        rayDirection.set(direction).normalize();
        rayOrigin.set(camera.position.x, camera.position.y, camera.position.z);

        return true;
    }

    private static boolean fillHandleMouseRay(Camera camera, int vx, int vy, int vw, int vh, int mouseX, int mouseY, Vector3d rayOrigin, Vector3f rayDirection)
    {
        Vector3f direction = getViewSpaceMouseDirection(
            camera.projection,
            mouseX,
            mouseY,
            vx,
            vy,
            vw,
            vh
        );

        if (direction.lengthSquared() <= 1.0E-12F)
        {
            return false;
        }

        rayDirection.set(direction).normalize();
        rayOrigin.set(0D, 0D, 0D);

        return true;
    }

    private static boolean fillHandleGizmoMatrix(UIFilmPanel panel, Matrix4f matrix)
    {
        if (panel == null || !panel.hasLastGizmoMatrix)
        {
            return false;
        }

        if (BBSRendering.isIrisShadersEnabled())
        {
            matrix.set(panel.lastGizmoMatrix);
        }
        else
        {
            matrix.set(BBSRendering.camera);
            matrix.mul(panel.lastGizmoMatrix);
        }

        return true;
    }

    private static Vector3f getViewSpaceMouseDirection(Matrix4f projection, int mx, int my, int vx, int vy, int vw, int vh)
    {
        mx -= vx;
        my -= vy;

        float w2 = vw / 2F;
        float h2 = vh / 2F;
        float x = (mx - w2) / w2;
        float y = (-my + h2) / h2;

        Matrix4f inverseProjection = new Matrix4f(projection).invert();
        Vector4f forward = new Vector4f(x, y, 0F, 1F).mul(inverseProjection);

        if (Math.abs(forward.w) > 1.0E-6F)
        {
            float invW = 1F / forward.w;

            forward.x *= invW;
            forward.y *= invW;
            forward.z *= invW;
        }

        Vector3f direction = new Vector3f(forward.x, forward.y, forward.z);

        direction.normalize();

        return direction;
    }

    static void syncDragCamera(UIFilmPanel panel, Camera camera)
    {
        camera.copy(panel.getWorldCamera());
        camera.view.set(panel.lastView);
        camera.projection.set(panel.lastProjection);
    }
}
