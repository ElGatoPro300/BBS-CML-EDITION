package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.Axis;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.joml.Vectors;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.pose.PoseTransform;
import mchorse.bbs_mod.utils.pose.Transform;
import mchorse.bbs_mod.utils.colors.Color;
import org.joml.Vector3d;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class UITransformKeyframeFactory extends UIKeyframeFactory<Transform>
{
    public UIPropTransform transform;
    public UITrackpad fix;
    public UIColor color;
    public UIToggle lighting;

    public UITransformKeyframeFactory(Keyframe<Transform> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.transform = new UIPoseTransforms(this);
        this.transform.enableHotkeys();
        this.transform.setTransform(keyframe.getValue());

        UIKeyframeSheet sheet = editor.getGraph().getSheet(keyframe);

        if (isPoseLimbTrack(sheet))
        {
            this.fix = new UITrackpad((v) ->
            {
                UIPoseTransforms.applyPoseTransform(this.editor, this.keyframe, (poseT) -> poseT.fix = MathUtils.clamp(v.floatValue(), 0F, 1F));
                this.transform.setTransform(this.keyframe.getValue());
            });
            this.fix.limit(0D, 1D).increment(1D).values(0.1, 0.05D, 0.2D);
            this.fix.tooltip(UIKeys.POSE_CONTEXT_FIX_TOOLTIP);

            this.color = new UIColor((c) ->
            {
                UIPoseTransforms.applyPoseTransform(this.editor, this.keyframe, (poseT) -> poseT.color.set(c));
            });
            this.color.withAlpha();

            this.lighting = new UIToggle(UIKeys.FORMS_EDITORS_GENERAL_LIGHTING, (b) ->
            {
                UIPoseTransforms.applyPoseTransform(this.editor, this.keyframe, (poseT) -> poseT.lighting = b.getValue() ? 0F : 1F);
            });
            this.lighting.tooltip(UIKeys.FORMS_EDITORS_GENERAL_LIGHTING_TOOLTIP);

            PoseTransform poseTransform = this.getPoseTransform(keyframe);

            this.fix.setValue(poseTransform.fix);
            this.color.setColor(poseTransform.color.getARGBColor());
            this.lighting.setValue(poseTransform.lighting <= 0F);

            this.scroll.add(UI.label(UIKeys.POSE_CONTEXT_FIX));
            this.scroll.add(UI.row(this.fix, this.color, this.lighting));
        }

        this.scroll.add(this.transform);
    }

    private PoseTransform getPoseTransform(Keyframe<Transform> keyframe)
    {
        Transform value = keyframe.getValue();

        if (value instanceof PoseTransform poseTransform)
        {
            return poseTransform;
        }

        PoseTransform poseTransform = new PoseTransform();

        if (value != null)
        {
            poseTransform.copy(value);
        }

        keyframe.setValue(poseTransform, false);

        return poseTransform;
    }

    private static boolean isPoseLimbTrack(UIKeyframeSheet sheet)
    {
        if (sheet == null)
        {
            return false;
        }

        int colon = sheet.id.indexOf(':');

        if (colon == -1)
        {
            return false;
        }

        String propertyPath = sheet.id.substring(0, colon);
        String propertyId = StringUtils.fileName(propertyPath);

        return propertyId.equals("pose") || propertyId.startsWith("pose_overlay");
    }

    public UIPropTransform getTransform()
    {
        return this.transform;
    }

    public static class UIPoseTransforms extends UIPropTransform
    {
        private UITransformKeyframeFactory editor;

        public UIPoseTransforms(UITransformKeyframeFactory editor)
        {
            this.editor = editor;
        }

        private Transform getReferenceTransform()
        {
            Transform active = this.editor.keyframe.getValue();

            if (active != null && active != this.getTransform())
            {
                this.setTransform(active);
            }

            return this.getTransform();
        }

        private static void forEachEditableKeyframe(UIKeyframes editor, Keyframe keyframe, Consumer<Keyframe> consumer)
        {
            Set<Keyframe> processed = new HashSet<>();

            if (keyframe != null)
            {
                processed.add(keyframe);
                consumer.accept(keyframe);
            }

            for (UIKeyframeSheet sheet : editor.getGraph().getSheets())
            {
                if (sheet.channel.getFactory() != keyframe.getFactory())
                {
                    continue;
                }

                for (Keyframe kf : sheet.selection.getSelected())
                {
                    if (!processed.add(kf))
                    {
                        continue;
                    }

                    consumer.accept(kf);
                }
            }
        }

        public static void apply(UIKeyframes editor, Keyframe keyframe, Consumer<Transform> consumer)
        {
            forEachEditableKeyframe(editor, keyframe, (kf) ->
            {
                if (kf.getValue() instanceof Transform transform)
                {
                    Color c = kf.getColor();

                    if (c != null && c.a < 0.99F)
                    {
                        kf.setColor(Color.rgba((c.getRGBColor() | 0xFF000000)));
                    }

                    kf.preNotify();
                    consumer.accept(transform);
                    kf.postNotify();
                }
            });
        }

        public static void applyPoseTransform(UIKeyframes editor, Keyframe keyframe, Consumer<PoseTransform> consumer)
        {
            forEachEditableKeyframe(editor, keyframe, (kf) ->
            {
                Object value = kf.getValue();
                PoseTransform poseTransform;

                if (value instanceof PoseTransform p)
                {
                    poseTransform = p;
                }
                else if (value instanceof Transform transform)
                {
                    poseTransform = new PoseTransform();
                    poseTransform.copy(transform);
                    kf.setValue(poseTransform, false);
                }
                else
                {
                    return;
                }

                kf.preNotify();
                consumer.accept(poseTransform);
                kf.postNotify();
            });
        }

        @Override
        public void pasteTranslation(Vector3d translation)
        {
            apply(this.editor.editor, this.editor.keyframe, (poseT) -> poseT.translate.set(translation));
            this.refillTransform();
        }

        @Override
        public void pasteScale(Vector3d scale)
        {
            apply(this.editor.editor, this.editor.keyframe, (poseT) -> poseT.scale.set(scale));
            this.refillTransform();
        }

        @Override
        public void pasteRotation(Vector3d rotation)
        {
            apply(this.editor.editor, this.editor.keyframe, (poseT) -> poseT.rotate.set(Vectors.toRad(rotation)));
            this.refillTransform();
        }

        @Override
        public void pasteRotation2(Vector3d rotation)
        {
            apply(this.editor.editor, this.editor.keyframe, (poseT) -> poseT.rotate2.set(Vectors.toRad(rotation)));
            this.refillTransform();
        }

        @Override
        public void pastePivot(Vector3d pivot)
        {
            apply(this.editor.editor, this.editor.keyframe, (poseT) -> poseT.pivot.set((float) pivot.x, (float) pivot.y, (float) pivot.z));
            this.refillTransform();
        }

        @Override
        public void setT(Axis axis, double x, double y, double z)
        {
            Transform transform = this.getReferenceTransform();
            float dx = (float) (x - transform.translate.x);
            float dy = (float) (y - transform.translate.y);
            float dz = (float) (z - transform.translate.z);

            if (Math.abs(dx) < 0.0001F && Math.abs(dy) < 0.0001F && Math.abs(dz) < 0.0001F)
            {
                return;
            }

            apply(this.editor.editor, this.editor.keyframe, (poseT) ->
            {
                poseT.translate.x += dx;
                poseT.translate.y += dy;
                poseT.translate.z += dz;
            });
        }

        @Override
        public void setS(Axis axis, double x, double y, double z)
        {
            Transform transform = this.getReferenceTransform();
            float dx = (float) (x - transform.scale.x);
            float dy = (float) (y - transform.scale.y);
            float dz = (float) (z - transform.scale.z);

            if (Math.abs(dx) < 0.0001F && Math.abs(dy) < 0.0001F && Math.abs(dz) < 0.0001F)
            {
                return;
            }

            apply(this.editor.editor, this.editor.keyframe, (poseT) ->
            {
                poseT.scale.x += dx;
                poseT.scale.y += dy;
                poseT.scale.z += dz;
            });
        }

        @Override
        public void setR(Axis axis, double x, double y, double z)
        {
            Transform transform = this.getReferenceTransform();
            float dx = MathUtils.toRad((float) x) - transform.rotate.x;
            float dy = MathUtils.toRad((float) y) - transform.rotate.y;
            float dz = MathUtils.toRad((float) z) - transform.rotate.z;

            if (Math.abs(dx) < 0.0001F && Math.abs(dy) < 0.0001F && Math.abs(dz) < 0.0001F)
            {
                return;
            }

            apply(this.editor.editor, this.editor.keyframe, (poseT) ->
            {
                poseT.rotate.x += dx;
                poseT.rotate.y += dy;
                poseT.rotate.z += dz;
            });
        }

        @Override
        public void setR2(Axis axis, double x, double y, double z)
        {
            Transform transform = this.getReferenceTransform();
            float dx = MathUtils.toRad((float) x) - transform.rotate2.x;
            float dy = MathUtils.toRad((float) y) - transform.rotate2.y;
            float dz = MathUtils.toRad((float) z) - transform.rotate2.z;

            if (Math.abs(dx) < 0.0001F && Math.abs(dy) < 0.0001F && Math.abs(dz) < 0.0001F)
            {
                return;
            }

            apply(this.editor.editor, this.editor.keyframe, (poseT) ->
            {
                poseT.rotate2.x += dx;
                poseT.rotate2.y += dy;
                poseT.rotate2.z += dz;
            });
        }

        @Override
        public void setP(Axis axis, double x, double y, double z)
        {
            Transform transform = this.getReferenceTransform();
            float dx = (float) x - transform.pivot.x;
            float dy = (float) y - transform.pivot.y;
            float dz = (float) z - transform.pivot.z;

            if (Math.abs(dx) < 0.0001F && Math.abs(dy) < 0.0001F && Math.abs(dz) < 0.0001F)
            {
                return;
            }

            apply(this.editor.editor, this.editor.keyframe, (poseT) ->
            {
                poseT.pivot.x += dx;
                poseT.pivot.y += dy;
                poseT.pivot.z += dz;
            });
        }
    }
}