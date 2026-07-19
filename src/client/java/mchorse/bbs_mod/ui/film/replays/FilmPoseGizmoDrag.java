package mchorse.bbs_mod.ui.film.replays;

import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.film.replays.Replay;
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
        boolean bobjModel = FilmPoseGizmoDrag.isBobjReplay(panel);

        if (poseGizmo)
        {
            transform.configurePoseRingTuning(bobjModel);

            /* Cubic pose bodies mirror X/Z relative to euler storage (post-multiplied bone-local
             * Ry(180°), see shouldInvertRotationRing) — the view ring inherits that sign. BOBJ
             * bones pre-multiply the flip globally, so their view ring keeps the natural sense.
             * Trackball arcball clears euler flips each drag via clearTrackballEulerInverts(). */
            transform.setInvertGizmoViewRing(!bobjModel);
            transform.setInvertGizmoTrackball(false);
            transform.setInvertFilmPoseGizmoAxes(false);
            transform.clearTrackballEulerInverts();
            transform.invertFilmArcballDragY();
            transform.setFilmArcballTrackball(true);
            transform.setFilmMatchPoseTrackball(false);

            /* Pose editor may be built before the model is known (scale defaults to 16). */
            transform.translationScale(bobjModel ? 1F : 16F);
            transform.setAxisProjectedTranslation(bobjModel);
        }
        else if (poseLimbGizmo)
        {
            transform.configurePoseLimbRingTuning(bobjModel);

            transform.setInvertGizmoViewRing(false);
            transform.setInvertGizmoTrackball(false);
            transform.setInvertFilmPoseGizmoAxes(false);
            transform.setFilmArcballTrackball(false);
            transform.clearTrackballEulerInverts();

            /* The vertical drag flip belongs to the limb context (both formats); the X/Z
             * euler flips compensate the cubic bone-local Ry(180°) that BOBJ doesn't have. */
            transform.invertModelPoseTrackballDragY();

            if (!bobjModel)
            {
                transform.invertModelPoseTrackballXZ();
            }

            /* Limb sheets have null formProperty, so the factory often kept cubic's /16 scale
             * on BOBJ — force block units + axis-projected rays so drag tracks the cursor. */
            transform.translationScale(bobjModel ? 1F : 16F);
            transform.setAxisProjectedTranslation(bobjModel);
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

        /* Same depth-based composition as the visual pass, so drags grab the
         * handle exactly where it is drawn regardless of the shader path. */
        Gizmo.composeVisualMatrix(panel.lastGizmoMatrix, BBSRendering.camera, panel.lastProjection, matrix);

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

    public static void syncDragCamera(UIFilmPanel panel, Camera camera)
    {
        camera.copy(panel.getWorldCamera());
        camera.view.set(panel.lastView);
        camera.projection.set(panel.lastProjection);
    }

    private static boolean isBobjReplay(UIFilmPanel panel)
    {
        if (panel == null || panel.replayEditor == null)
        {
            return false;
        }

        Replay replay = panel.replayEditor.getReplay();

        if (replay == null)
        {
            return false;
        }

        Form form = FormUtils.getRoot(replay.form.get());

        return form instanceof ModelForm modelForm && ModelFormRenderer.isBobjModel(modelForm);
    }
}
