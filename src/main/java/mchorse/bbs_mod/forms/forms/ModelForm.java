package mchorse.bbs_mod.forms.forms;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.cubic.animation.ActionsConfig;
import mchorse.bbs_mod.forms.values.ValueActionsConfig;
import mchorse.bbs_mod.forms.values.ValueShapeKeys;
import mchorse.bbs_mod.obj.shapes.ShapeKeys;
import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.settings.values.core.ValueLink;
import mchorse.bbs_mod.settings.values.core.ValuePose;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.pose.Pose;

import java.util.ArrayList;
import java.util.List;

public class ModelForm extends Form
{
    public final ValueLink texture = new ValueLink("texture", null);
    public final ValueString model = new ValueString("model", "");
    public final ValueFloat pbrNormalIntensity = new ValueFloat("pbr_normal_intensity", 1F, 0F, 4F);
    public final ValueFloat pbrSpecularIntensity = new ValueFloat("pbr_specular_intensity", 1F, 0F, 4F);
    public final ValuePose pose = new ValuePose("pose", new Pose());
    public final ValuePose poseOverlay = new ValuePose("pose_overlay", new Pose());
    public final ValueActionsConfig actions = new ValueActionsConfig("actions", new ActionsConfig());
    public final ValueColor color = new ValueColor("color", Color.white());
    public final ValueShapeKeys shapeKeys = new ValueShapeKeys("shape_keys", new ShapeKeys());

    public final List<ValuePose> additionalOverlays = new ArrayList<>();

    public ModelForm()
    {
        super();

        this.add(this.texture);
        this.add(this.model);
        this.add(this.pbrNormalIntensity);
        this.add(this.pbrSpecularIntensity);
        this.add(this.pose);
        this.add(this.poseOverlay);

        for (int i = 0; i < BBSSettings.recordingPoseTransformOverlays.get(); i++)
        {
            ValuePose valuePose = new ValuePose("pose_overlay" + i, new Pose());

            this.additionalOverlays.add(valuePose);
            this.add(valuePose);
        }

        this.add(this.actions);
        this.add(this.color);
        this.add(this.shapeKeys);
    }

    @Override
    public String getDefaultDisplayName()
    {
        return this.model.get();
    }
}