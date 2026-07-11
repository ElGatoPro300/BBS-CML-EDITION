package mchorse.bbs_mod.ui.forms.editors.panels.widgets;

import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.settings.values.IValueListener;
import mchorse.bbs_mod.settings.values.core.ValuePose;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.utils.pose.UIPoseEditor;
import mchorse.bbs_mod.utils.pose.PoseTransform;

public class UIModelPoseEditor extends UIPoseEditor
{
    private ValuePose valuePose;

    @Override
    protected boolean useModelGizmoDrag()
    {
        /* General transform uses a plain UIPropTransform; match that for pose trackball drag. */
        return false;
    }

    @Override
    protected float getGizmoTranslationScale()
    {
        return 2.5F;
    }

    public void setValuePose(ValuePose valuePose)
    {
        this.valuePose = valuePose;
    }

    @Override
    protected UIPropTransform createTransformEditor()
    {
        /* Pose gizmo: flip X/Z rings and Z translate; trackball euler matches General panel. */
        return super.createTransformEditor()
            .callbacks(() -> this.valuePose)
            .poseModelGizmoTuning()
            .invertModelPoseTrackballXZ()
            .invertModelPoseTrackballDragY();
    }

    @Override
    protected void pastePose(MapType data)
    {
        this.valuePose.preNotify(IValueListener.FLAG_UNMERGEABLE);
        super.pastePose(data);
        this.valuePose.postNotify(IValueListener.FLAG_UNMERGEABLE);
    }

    @Override
    protected void flipPose()
    {
        this.valuePose.preNotify(IValueListener.FLAG_UNMERGEABLE);
        super.flipPose();
        this.valuePose.postNotify(IValueListener.FLAG_UNMERGEABLE);
    }

    @Override
    protected void setFix(PoseTransform transform, float value)
    {
        this.valuePose.preNotify(IValueListener.FLAG_UNMERGEABLE);
        super.setFix(transform, value);
        this.valuePose.postNotify(IValueListener.FLAG_UNMERGEABLE);
    }

    @Override
    protected void setColor(PoseTransform transform, int value)
    {
        this.valuePose.preNotify(IValueListener.FLAG_UNMERGEABLE);
        super.setColor(transform, value);
        this.valuePose.postNotify(IValueListener.FLAG_UNMERGEABLE);
    }

    @Override
    protected void setPaintColor(PoseTransform transform, int value)
    {
        this.valuePose.preNotify(IValueListener.FLAG_UNMERGEABLE);
        super.setPaintColor(transform, value);
        this.valuePose.postNotify(IValueListener.FLAG_UNMERGEABLE);
    }

    @Override
    protected void setLighting(PoseTransform transform, boolean value)
    {
        this.valuePose.preNotify(IValueListener.FLAG_UNMERGEABLE);
        super.setLighting(transform, value);
        this.valuePose.postNotify(IValueListener.FLAG_UNMERGEABLE);
    }
}