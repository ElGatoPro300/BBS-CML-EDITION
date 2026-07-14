package mchorse.bbs_mod.ui.film.replays;

import mchorse.bbs_mod.camera.Camera;
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
import mchorse.bbs_mod.ui.utils.gizmo.GizmoRayFrame;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Film pose gizmo drag frame. Axis rings / translate / scale handles and the trackball sphere
 * all use view-space rays plus the captured pass matrix so drag math matches the drawn gizmo.
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
        boolean replayTransformGizmo = false;
        boolean anchorTrackGizmo = false;

        if (keyframeEditor != null && keyframeEditor.editor instanceof UITransformKeyframeFactory transformFactory)
        {
            UIKeyframeSheet sheet = keyframeEditor.getSheet(transformFactory.getKeyframe());

            poseLimbGizmo = UITransformKeyframeFactory.isPoseLimbTrack(sheet);
            replayTransformGizmo = !poseLimbGizmo;
        }
        else if (keyframeEditor != null && keyframeEditor.editor instanceof UIAnchorKeyframeFactory)
        {
            anchorTrackGizmo = true;
        }

        boolean filmTransformGizmo = replayTransformGizmo || anchorTrackGizmo;

        if (poseGizmo)
        {
            /* Pose bodies mirror X/Z relative to euler storage (see shouldInvertRotationRing).
             * View-space rays fix translate / Y ring feel; X/Z rings still need poseModelGizmoTuning
             * on the transform editor (same as UIModelPoseEditor). Trackball arcball clears euler
             * flips each drag via clearTrackballEulerInverts(). */
            transform.setInvertGizmoViewRing(true);
            transform.setInvertGizmoTrackball(false);
            transform.setInvertFilmPoseGizmoAxes(false);
            transform.clearTrackballEulerInverts();
            transform.invertFilmArcballDragY();
            transform.setFilmArcballTrackball(true);
            transform.setFilmMatchPoseTrackball(false);
        }
        else if (poseLimbGizmo)
        {
            transform.setInvertGizmoViewRing(false);
            transform.setInvertGizmoTrackball(false);
            transform.setInvertFilmPoseGizmoAxes(false);
            transform.setFilmArcballTrackball(false);
            transform.clearTrackballEulerInverts();
            transform.invertModelPoseTrackballXZ();
            transform.invertModelPoseTrackballDragY();
        }
        else if (replayTransformGizmo)
        {
            transform.setInvertGizmoViewRing(false);
            transform.setInvertGizmoTrackball(false);
            transform.setInvertFilmPoseGizmoAxes(false);
            transform.setFilmArcballTrackball(false);
            transform.clearTrackballEulerInverts();
            transform.invertModelPoseTrackballXYZ();
        }
        else
        {
            transform.setInvertFilmPoseGizmoAxes(false);
            transform.setFilmArcballTrackball(false);
            transform.clearTrackballEulerInverts();
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

                return FilmPoseGizmoDrag.fillHandleMouseRay(camera, vx, vy, area.w, area.h, mouseX, mouseY, rayOrigin, rayDirection);
            }

            @Override
            public boolean getGizmoMatrix(Matrix4f matrix)
            {
                return FilmPoseGizmoDrag.fillHandleGizmoMatrix(panel, matrix);
            }

            @Override
            public boolean projectDragPoint(UIContext context, double x, double y, double z, Vector2f screenOut)
            {
                if (area.w <= 0 || area.h <= 0)
                {
                    return false;
                }

                FilmPoseGizmoDrag.syncDragCamera(panel, camera);

                int vx = context.globalX(area.x);
                int vy = context.globalY(area.y);

                return GizmoRayFrame.projectViewPoint(camera.projection, vx, vy, area.w, area.h, x, y, z, screenOut);
            }
        });
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
