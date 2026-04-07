package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.model.IKChainConfig;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCacheEntry;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIModelPoseEditor;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIListOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.model.UIModelIKPanel;
import mchorse.bbs_mod.ui.utils.shapes.UIShapeKeys;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.pose.Transform;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

public class UIModelFormPanel extends UIFormPanel<ModelForm>
{
    public UIColor color;
    public UIModelPoseEditor poseEditor;
    public UIShapeKeys shapeKeys;
    public UIPropTransform ikTargetTransform;
    private String selectedIKTarget;

    public UIButton pickModel;
    public UIButton pick;

    public UIModelFormPanel(UIForm editor)
    {
        super(editor);

        this.pickModel = new UIButton(UIKeys.FORMS_EDITOR_MODEL_PICK_MODEL, (b) ->
        {
            UIListOverlayPanel list = new UIListOverlayPanel(UIKeys.FORMS_EDITOR_MODEL_MODELS, (l) ->
            {
                this.form.model.set(l);

                if (Window.isCtrlPressed())
                {
                    ModelInstance model = ModelFormRenderer.getModel(this.form);

                    if (model != null)
                    {
                        this.form.texture.set(model.texture);
                    }
                }

                this.editor.startEdit(this.form);
            });

            list.addValues(BBSModClient.getModels().getAvailableKeys());
            list.list.list.sort();
            list.setValue(this.form.model.get());

            UIOverlay.addOverlay(this.getContext(), list);
        });
        this.color = new UIColor((c) -> this.form.color.set(new Color().set(c))).withAlpha();
        this.color.direction(Direction.LEFT);
        this.poseEditor = new UIModelPoseEditor();
        this.poseEditor.pickCallback = this::pickGroup;
        this.poseEditor.setDefaultTextureSupplier(() ->
        {
            Link base = this.form.texture.get();
            if (base != null)
            {
                return base;
            }

            ModelInstance model = ModelFormRenderer.getModel(this.form);
            return model != null ? model.texture : null;
        });
        this.shapeKeys = new UIShapeKeys();
        this.ikTargetTransform = new UIPropTransform().callbacks(() -> this.form);
        this.pick = new UIButton(UIKeys.FORMS_EDITOR_MODEL_PICK_TEXTURE, (b) ->
        {
            Link link = this.form.texture.get();
            ModelInstance model = ModelFormRenderer.getModel(this.form);

            if (model != null && link == null)
            {
                link = model.texture;
            }

            UITexturePicker.open(this.getContext(), link, (l) -> this.form.texture.set(l));
        });

        this.options.add(this.pickModel);
        if (mchorse.bbs_mod.BBSSettings.pickLimbTexture.get())
        {
            this.options.add(this.pick);
        }
        this.options.add(this.color, this.poseEditor);
    }

    private void pickGroup(String group)
    {
        ModelInstance model = ModelFormRenderer.getModel(this.form);

        if (UIModelIKPanel.isIKVirtualBoneName(group))
        {
            IKChainConfig chain = this.getChainByVirtualBone(group, model);

            if (chain != null)
            {
                boolean pole = UIModelIKPanel.isIKPoleVirtualBoneName(group);
                String virtualBone = pole ? UIModelIKPanel.IK_POLE_PREFIX + chain.getId() : UIModelIKPanel.IK_TARGET_PREFIX + chain.getId();

                this.selectedIKTarget = virtualBone;
                if (pole)
                {
                    chain.usePoleBone.set(false);
                    this.ikTargetTransform.setTransform(chain.pole);
                }
                else
                {
                    chain.useTargetBone.set(false);
                    this.ikTargetTransform.setTransform(chain.target);
                }
                return;
            }
        }

        this.selectedIKTarget = null;
    }

    @Override
    public void startEdit(ModelForm form)
    {
        super.startEdit(form);
        this.selectedIKTarget = null;
        this.ikTargetTransform.setTransform(new Transform());

        ModelInstance model = ModelFormRenderer.getModel(this.form);
        String poseGroup = model == null ? this.form.model.get() : model.poseGroup;
        if (poseGroup == null || poseGroup.isEmpty())
        {
            poseGroup = model == null ? this.form.model.get() : model.id;
        }

        this.poseEditor.setValuePose(form.pose);
        this.poseEditor.setPose(form.pose.get(), poseGroup);
        this.poseEditor.fillGroups(model == null ? null : model.model, model == null ? null : model.flippedParts, true);
        this.injectIKVirtualBones(model);
        this.color.setColor(form.color.get().getARGBColor());

        this.shapeKeys.removeFromParent();

        if (model != null)
        {
            Set<String> modelShapeKeys = model.model.getShapeKeys();

            if (!modelShapeKeys.isEmpty())
            {
                this.options.add(this.shapeKeys);
                this.shapeKeys.setShapeKeys(poseGroup, modelShapeKeys, this.form.shapeKeys.get());
            }
        }

        this.options.resize();
    }

    public UIPropTransform getEditableTransform()
    {
        return this.selectedIKTarget == null ? this.poseEditor.transform : this.ikTargetTransform;
    }

    public Matrix4f getIKTargetOriginMatrix(float transition)
    {
        IKChainConfig chain = this.getChainByVirtualBone(this.selectedIKTarget, ModelFormRenderer.getModel(this.form));
        Transform transform = this.ikTargetTransform.getTransform();
        boolean pole = UIModelIKPanel.isIKPoleVirtualBoneName(this.selectedIKTarget);

        if (transform == null)
        {
            transform = chain != null ? (pole ? chain.pole : chain.target) : new Transform();
            this.ikTargetTransform.setTransform(transform);
        }

        Vector3f point = new Vector3f(transform.translate).mul(1F / 16F);

        if (chain != null && !pole && !chain.targetParentBone.get().isEmpty())
        {
            String path = StringUtils.combinePaths(FormUtils.getPath(this.form), chain.targetParentBone.get());
            MatrixCache map = FormUtilsClient.getRenderer(FormUtils.getRoot(this.form)).collectMatrices(this.editor.editor.renderer.getTargetEntity(), transition);
            MatrixCacheEntry entry = map.get(path);

            if (entry != null)
            {
                Matrix4f matrix = entry.origin() == null ? entry.matrix() : entry.origin();

                if (matrix != null)
                {
                    Vector3f world = new Vector3f();

                    matrix.transformPosition(point, world);

                    return new Matrix4f().translate(world);
                }
            }
        }

        return new Matrix4f().translate(point);
    }

    private void injectIKVirtualBones(ModelInstance model)
    {
        if (model == null || model.ikChains.isEmpty())
        {
            return;
        }

        Collection<String> existing = this.poseEditor.groups.list.getList();
        Collection<String> add = new ArrayList<>();

        for (IKChainConfig chain : model.ikChains)
        {
            String virtualBone = UIModelIKPanel.IK_TARGET_PREFIX + chain.getId();
            String poleVirtualBone = UIModelIKPanel.IK_POLE_PREFIX + chain.getId();

            if (!existing.contains(virtualBone))
            {
                add.add(virtualBone);
            }

            if (!existing.contains(poleVirtualBone))
            {
                add.add(poleVirtualBone);
            }
        }

        if (!add.isEmpty())
        {
            this.poseEditor.groups.list.add(add);
            this.poseEditor.groups.list.sort();
        }
    }

    private IKChainConfig getChainByVirtualBone(String virtualBone, ModelInstance model)
    {
        if (model == null)
        {
            return null;
        }

        String id = UIModelIKPanel.extractIKVirtualId(virtualBone);

        if (id == null)
        {
            return null;
        }

        for (IKChainConfig chain : model.ikChains)
        {
            if (chain.getId().equals(id))
            {
                return chain;
            }
        }

        return null;
    }

    @Override
    public void pickBone(String bone)
    {
        if (bone == null || bone.isEmpty())
        {
            return;
        }

        this.poseEditor.selectBone(bone);
        this.pickGroup(bone);
    }
}
