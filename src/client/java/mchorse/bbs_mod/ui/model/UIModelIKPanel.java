package mchorse.bbs_mod.ui.model;

import mchorse.bbs_mod.cubic.model.IKChainConfig;
import mchorse.bbs_mod.cubic.model.ModelConfig;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.pose.Transform;

public class UIModelIKPanel extends UIElement
{
    private final UIModelPanel parent;

    private final UISearchList<String> ikSearch;
    private final UIStringList ikList;
    private final UIButton addIkButton;
    private final UIButton removeIkButton;
    private final UIButton editIkButton;
    private final UIToggle ikVisualizerToggle;

    private final UIButton addBoneButton;
    private final UIButton removeBoneButton;
    private final UIButton clearBonesButton;
    private final UIStringList bonesList;
    private final UISearchList<String> bonesSearch;
    private final UIPropTransform ikTransform;
    private final UITextbox ikNameField;
    private final UITrackpad minX;
    private final UITrackpad minY;
    private final UITrackpad minZ;
    private final UITrackpad maxX;
    private final UITrackpad maxY;
    private final UITrackpad maxZ;

    private ModelConfig config;
    private boolean filling;
    private int nextChainId;

    public UIModelIKPanel(UIModelPanel parent)
    {
        this.parent = parent;

        this.relative(parent.mainView).w(1F).h(1F);

        int sideMargin = 10;
        int leftWidth = 220;
        int rightWidth = 220;
        int rowGap = 6;

        UILabel leftTitle = UI.label(UIKeys.MODELS_IK_HIERARCHY).background();
        leftTitle.relative(this).x(sideMargin).y(10).w(leftWidth).h(12);

        this.ikList = new UIStringList((l) -> this.fillSelectedChain());
        this.ikList.background();

        this.ikSearch = new UISearchList<>(this.ikList);
        this.ikSearch.label(UIKeys.GENERAL_SEARCH);
        this.ikSearch.relative(this).x(sideMargin).y(26).w(leftWidth).h(1F, -210);

        this.addIkButton = new UIButton(UIKeys.GENERAL_ADD, (b) -> this.addChain());
        this.removeIkButton = new UIButton(UIKeys.GENERAL_REMOVE, (b) -> this.removeSelectedChain());

        this.addIkButton.w(0.5F, -4).h(20);
        this.removeIkButton.w(0.5F, -4).h(20);

        UIElement addRemoveBar = UI.row(8, this.addIkButton, this.removeIkButton);
        addRemoveBar.relative(this.ikSearch).y(1F, rowGap).w(1F).h(20);

        this.editIkButton = new UIButton(UIKeys.GENERAL_EDIT, (b) -> this.addSelectedBone());
        this.editIkButton.relative(addRemoveBar).y(1F, rowGap).w(1F).h(20);

        UILabel visualizerLabel = UI.label(UIKeys.MODELS_IK_VISUALIZER);
        visualizerLabel.relative(this.editIkButton).y(1F, 10).w(leftWidth).h(12);

        this.ikVisualizerToggle = new UIToggle(UIKeys.MODELS_IK_SHOW, false, (b) -> this.updateVisualizer());
        this.ikVisualizerToggle.relative(visualizerLabel).y(1F, 4).w(120);

        this.addBoneButton = new UIButton(UIKeys.MODELS_IK_ADD_SELECTED, (b) -> this.addSelectedBone());
        this.addBoneButton.relative(this).x(1F, -rightWidth - sideMargin).y(10).w(rightWidth).h(20);

        this.removeBoneButton = new UIButton(UIKeys.MODELS_IK_REMOVE_SELECTED, (b) -> this.removeSelectedBone());
        this.clearBonesButton = new UIButton(UIKeys.MODELS_IK_CLEAR_BONES, (b) -> this.clearBones());
        this.removeBoneButton.w(0.5F, -4).h(20);
        this.clearBonesButton.w(0.5F, -4).h(20);

        UIElement boneActions = UI.row(8, this.removeBoneButton, this.clearBonesButton);
        boneActions.relative(this.addBoneButton).y(1F, 6).w(1F).h(20);

        UILabel bonesLabel = UI.label(UIKeys.MODELS_IK_BONES);
        bonesLabel.relative(boneActions).y(1F, 6).w(1F).h(12);

        this.bonesList = new UIStringList(null);
        this.bonesList.background();
        this.bonesSearch = new UISearchList<>(this.bonesList);
        this.bonesSearch.label(UIKeys.GENERAL_SEARCH);
        this.bonesSearch.relative(bonesLabel).y(1F, 4).w(1F).h(90);

        this.ikTransform = new UIPropTransform();
        this.ikTransform.relative(this.bonesSearch).y(1F, 8).w(1F).h(120);
        this.ikTransform.callbacks(this::markDirty, this::markDirty);

        UILabel nameLabel = UI.label(UIKeys.MODELS_IK_NAME);
        nameLabel.relative(this.ikTransform).y(1F, 10).w(1F).h(12);

        this.ikNameField = new UITextbox(1000, this::updateChainName);
        this.ikNameField.relative(nameLabel).y(1F, 4).w(1F).h(20);

        UILabel constraintsLabel = UI.label(UIKeys.MODELS_IK_CONSTRAINTS);
        constraintsLabel.relative(this.ikNameField).y(1F, 10).w(1F).h(12);

        UILabel minLabel = UI.label(UIKeys.MODELS_IK_MIN);
        minLabel.relative(constraintsLabel).y(1F, 4).w(1F).h(12);

        this.minX = new UITrackpad((v) -> this.updateConstraint(true, 0, v.floatValue())).limit(-180, 180).increment(1);
        this.minY = new UITrackpad((v) -> this.updateConstraint(true, 1, v.floatValue())).limit(-180, 180).increment(1);
        this.minZ = new UITrackpad((v) -> this.updateConstraint(true, 2, v.floatValue())).limit(-180, 180).increment(1);
        this.minX.w(0.333F, -6);
        this.minY.w(0.333F, -6);
        this.minZ.w(0.333F, -6);

        UIElement minRow = UI.row(6, this.minX, this.minY, this.minZ);
        minRow.relative(minLabel).y(1F, 2).w(1F).h(20);

        UILabel maxLabel = UI.label(UIKeys.MODELS_IK_MAX);
        maxLabel.relative(minRow).y(1F, 4).w(1F).h(12);

        this.maxX = new UITrackpad((v) -> this.updateConstraint(false, 0, v.floatValue())).limit(-180, 180).increment(1);
        this.maxY = new UITrackpad((v) -> this.updateConstraint(false, 1, v.floatValue())).limit(-180, 180).increment(1);
        this.maxZ = new UITrackpad((v) -> this.updateConstraint(false, 2, v.floatValue())).limit(-180, 180).increment(1);
        this.maxX.w(0.333F, -6);
        this.maxY.w(0.333F, -6);
        this.maxZ.w(0.333F, -6);

        UIElement maxRow = UI.row(6, this.maxX, this.maxY, this.maxZ);
        maxRow.relative(maxLabel).y(1F, 2).w(1F).h(20);

        this.add(leftTitle, this.ikSearch, addRemoveBar,
            this.editIkButton, visualizerLabel, this.ikVisualizerToggle,
            this.addBoneButton, boneActions, bonesLabel, this.bonesSearch,
            this.ikTransform, nameLabel, this.ikNameField,
            constraintsLabel, minLabel, minRow, maxLabel, maxRow);

        this.fillSelectedChain();
    }

    public void setConfig(ModelConfig config)
    {
        this.config = config;
        this.nextChainId = this.computeNextChainId();
        this.refreshChainList();
        this.fillSelectedChain();
    }

    private void addChain()
    {
        if (this.config == null)
        {
            return;
        }

        IKChainConfig chain = new IKChainConfig(String.valueOf(this.nextChainId));
        String name = "IK-" + (this.config.ikChains.getAllTyped().size() + 1);

        this.nextChainId += 1;
        chain.name.set(name);
        this.config.ikChains.add(chain);
        this.config.ikChains.sync();
        this.markDirty();

        this.refreshChainList();
        this.ikList.setIndex(this.config.ikChains.getAllTyped().size() - 1);
        this.fillSelectedChain();
    }

    private void removeSelectedChain()
    {
        if (this.config == null)
        {
            return;
        }

        int index = this.ikList.getIndex();

        if (index >= 0 && index < this.config.ikChains.getAllTyped().size())
        {
            this.config.ikChains.remove(index);
            this.config.ikChains.sync();
            this.markDirty();
            this.refreshChainList();
            this.ikList.setIndex(Math.min(index, this.config.ikChains.getAllTyped().size() - 1));
            this.fillSelectedChain();
        }
    }

    private void refreshChainList()
    {
        this.ikList.clear();

        if (this.config == null)
        {
            return;
        }

        for (IKChainConfig chain : this.config.ikChains.getAllTyped())
        {
            this.ikList.add(chain.name.get().isEmpty() ? chain.getId() : chain.name.get());
        }
    }

    private IKChainConfig getSelectedChain()
    {
        if (this.config == null)
        {
            return null;
        }

        int index = this.ikList.getIndex();

        if (index < 0 || index >= this.config.ikChains.getAllTyped().size())
        {
            return null;
        }

        return this.config.ikChains.getAllTyped().get(index);
    }

    private void fillSelectedChain()
    {
        IKChainConfig chain = this.getSelectedChain();

        this.filling = true;

        if (chain == null)
        {
            this.ikNameField.setText("");
            this.ikVisualizerToggle.setValue(false);
            this.ikTransform.setTransform(new Transform());
            this.bonesList.clear();
            this.minX.setValue(-180);
            this.minY.setValue(-180);
            this.minZ.setValue(-180);
            this.maxX.setValue(180);
            this.maxY.setValue(180);
            this.maxZ.setValue(180);
        }
        else
        {
            this.ikNameField.setText(chain.name.get());
            this.ikVisualizerToggle.setValue(chain.visualizer.get());
            this.ikTransform.setTransform(chain.target);
            this.refreshBonesList(chain);
            this.minX.setValue(chain.minX.get());
            this.minY.setValue(chain.minY.get());
            this.minZ.setValue(chain.minZ.get());
            this.maxX.setValue(chain.maxX.get());
            this.maxY.setValue(chain.maxY.get());
            this.maxZ.setValue(chain.maxZ.get());
        }

        this.filling = false;
    }

    private void updateChainName(String value)
    {
        if (this.filling)
        {
            return;
        }

        IKChainConfig chain = this.getSelectedChain();

        if (chain == null)
        {
            return;
        }

        chain.name.set(value);
        this.markDirty();

        int index = this.ikList.getIndex();

        this.refreshChainList();
        this.ikList.setIndex(index);
    }

    private void updateVisualizer()
    {
        if (this.filling)
        {
            return;
        }

        IKChainConfig chain = this.getSelectedChain();

        if (chain == null)
        {
            return;
        }

        chain.visualizer.set(this.ikVisualizerToggle.getValue());
        this.markDirty();
    }

    private void assignSelectedBoneToChain()
    {
        IKChainConfig chain = this.getSelectedChain();
        String selectedBone = this.parent.renderer.getSelectedBone();

        if (chain == null || selectedBone == null || selectedBone.isEmpty())
        {
            return;
        }

        while (!chain.bones.getAllTyped().isEmpty())
        {
            chain.bones.remove(0);
        }

        chain.bones.add(new ValueString("0", selectedBone));
        chain.bones.sync();
        this.markDirty();
        this.refreshBonesList(chain);
    }

    private void addSelectedBone()
    {
        IKChainConfig chain = this.getSelectedChain();
        String selectedBone = this.parent.renderer.getSelectedBone();

        if (chain == null || selectedBone == null || selectedBone.isEmpty())
        {
            return;
        }

        for (ValueString value : chain.bones.getAllTyped())
        {
            if (selectedBone.equals(value.get()))
            {
                return;
            }
        }

        chain.bones.add(new ValueString(String.valueOf(chain.bones.getAllTyped().size()), selectedBone));
        chain.bones.sync();
        this.markDirty();
        this.refreshBonesList(chain);
    }

    private void removeSelectedBone()
    {
        IKChainConfig chain = this.getSelectedChain();
        int index = this.bonesList.getIndex();

        if (chain == null || index < 0 || index >= chain.bones.getAllTyped().size())
        {
            return;
        }

        chain.bones.remove(index);
        chain.bones.sync();
        this.markDirty();
        this.refreshBonesList(chain);
    }

    private void clearBones()
    {
        IKChainConfig chain = this.getSelectedChain();

        if (chain == null)
        {
            return;
        }

        while (!chain.bones.getAllTyped().isEmpty())
        {
            chain.bones.remove(0);
        }

        chain.bones.sync();
        this.markDirty();
        this.refreshBonesList(chain);
    }

    private void refreshBonesList(IKChainConfig chain)
    {
        this.bonesList.clear();

        if (chain == null)
        {
            return;
        }

        for (ValueString value : chain.bones.getAllTyped())
        {
            if (!value.get().isEmpty())
            {
                this.bonesList.add(value.get());
            }
        }
    }

    private void updateConstraint(boolean min, int axis, float value)
    {
        if (this.filling)
        {
            return;
        }

        IKChainConfig chain = this.getSelectedChain();

        if (chain == null)
        {
            return;
        }

        if (axis == 0)
        {
            if (min) chain.minX.set(value);
            else chain.maxX.set(value);
        }
        else if (axis == 1)
        {
            if (min) chain.minY.set(value);
            else chain.maxY.set(value);
        }
        else
        {
            if (min) chain.minZ.set(value);
            else chain.maxZ.set(value);
        }

        this.markDirty();
    }

    private int computeNextChainId()
    {
        if (this.config == null)
        {
            return 0;
        }

        int max = -1;

        for (IKChainConfig chain : this.config.ikChains.getAllTyped())
        {
            try
            {
                max = Math.max(max, Integer.parseInt(chain.getId()));
            }
            catch (Exception ignored)
            {}
        }

        return max + 1;
    }

    private void markDirty()
    {
        if (!this.filling)
        {
            this.parent.dirty();
        }
    }
}

