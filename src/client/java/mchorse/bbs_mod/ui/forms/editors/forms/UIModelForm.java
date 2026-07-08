package mchorse.bbs_mod.ui.forms.editors.forms;

import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCacheEntry;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.panels.UIActionsFormPanel;
import mchorse.bbs_mod.ui.forms.editors.panels.UIFormPanel;
import mchorse.bbs_mod.ui.forms.editors.panels.UIModelFormPanel;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.pose.UIPoseEditor;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.joml.Matrices;
import mchorse.bbs_mod.utils.pose.Transform;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class UIModelForm extends UIForm<ModelForm>
{
    public UIModelFormPanel modelPanel;

    public UIModelForm()
    {
        this.modelPanel = new UIModelFormPanel(this);
        this.defaultPanel = this.modelPanel;

        this.registerPanel(this.defaultPanel, UIKeys.FORMS_EDITORS_MODEL_POSE, Icons.POSE);
        this.registerPanel(new UIActionsFormPanel(this), UIKeys.FORMS_EDITORS_ACTIONS_TITLE, Icons.MORE);
        this.registerDefaultPanels();

        this.defaultPanel.keys().register(Keys.FORMS_PICK_TEXTURE, () ->
        {
            if (this.view != this.modelPanel)
            {
                this.setPanel(this.modelPanel);
            }

            this.modelPanel.pick.clickItself();
        });
    }

    @Override
    public UIPropTransform getEditableTransform()
    {
        return super.getEditableTransform();
    }

    @Override
    public void setPanel(UIFormPanel<ModelForm> panel)
    {
        super.setPanel(panel);

        if (panel == this.modelPanel && this.editor != null)
        {
            this.editor.disableFormTransformGizmo();
        }
    }

    public UIPropTransform getPoseGizmoTransform()
    {
        return this.modelPanel.poseEditor.transform;
    }

    public void showPosePanel()
    {
        this.setPanel(this.modelPanel);
    }

    @Override
    public Matrix4f getOrigin(float transition)
    {
        String path = FormUtils.getPath(this.form);
        UIPoseEditor poseEditor = this.modelPanel.poseEditor;

        return this.getOrigin(transition, StringUtils.combinePaths(path, poseEditor.groups.list.getCurrentFirst()), poseEditor.transform.isLocal());
    }

    /**
     * Pose gizmo basis matches {@link mchorse.bbs_mod.ui.model_blocks.UIModelBlockPanel}:
     * bone pivot in world space, plus the edited pose rotation in local mode only.
     */
    @Override
    public Matrix4f getOrigin(float transition, String path, boolean local)
    {
        if (path == null)
        {
            return Matrices.EMPTY_4F;
        }

        MatrixCache map = FormUtilsClient.getRenderer(FormUtils.getRoot(this.form)).collectMatrices(this.editor.renderer.getTargetEntity(), transition);
        boolean forceOrigin = path.endsWith("#origin");

        if (forceOrigin)
        {
            path = path.substring(0, path.length() - 7);
        }

        MatrixCacheEntry entry = map.get(path);

        if (entry == null)
        {
            return Matrices.EMPTY_4F;
        }

        Matrix4f originMatrix = entry.origin();

        if (originMatrix == null)
        {
            return Matrices.EMPTY_4F;
        }

        Vector3f pivot = originMatrix.getTranslation(new Vector3f());
        Matrix4f matrix = new Matrix4f().translation(pivot);

        if (!forceOrigin && local)
        {
            Transform poseTransform = this.modelPanel.poseEditor.transform.getTransform();

            matrix.mul(new Matrix4f(poseTransform.createRotationMatrix()));
        }

        return matrix;
    }

    public Matrix4f getOriginForPoseEditor(float transition, UIPoseEditor poseEditor)
    {
        if (poseEditor == null)
        {
            return this.getOrigin(transition);
        }

        String path = FormUtils.getPath(this.form);

        return this.getOrigin(transition, StringUtils.combinePaths(path, poseEditor.groups.list.getCurrentFirst()), poseEditor.transform.isLocal());
    }
}
