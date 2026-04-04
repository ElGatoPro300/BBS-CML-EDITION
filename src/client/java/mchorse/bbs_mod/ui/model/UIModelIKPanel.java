package mchorse.bbs_mod.ui.model;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.model.IKChainConfig;
import mchorse.bbs_mod.cubic.model.IKJointConstraint;
import mchorse.bbs_mod.cubic.model.ModelConfig;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.pose.Transform;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UIModelIKPanel extends UIElement
{
    public static final String IK_TARGET_PREFIX = "[IK_TARGET]::";
    public static final String IK_TARGET_PREFIX_ALT = "<IK_TARGET>:";
    public static final String IK_POLE_PREFIX = "[IK_POLE]::";
    public static final String IK_POLE_PREFIX_ALT = "<IK_POLE>:";

    private final UIModelPanel parent;

    private final UISearchList<String> ikSearch;
    private final UIScrollView leftScrollView;
    private final UIScrollView rightScrollView;
    private final UIStringList ikList;
    private final UIButton addIkButton;
    private final UIButton removeIkButton;
    private final UIButton editIkButton;
    private final UIToggle ikVisualizerToggle;

    private final UIButton addBoneButton;
    private final UIButton removeBoneButton;
    private final UIButton clearBonesButton;
    private final UISearchList<String> hierarchySearch;
    private final UIStringList hierarchyList;
    private final UIButton addHierarchyBoneButton;
    private final UIToggle useTargetBoneToggle;
    private final UIButton setTargetBoneButton;
    private final UIButton clearTargetBoneButton;
    private final UIButton selectTargetButton;
    private final UILabel targetBoneLabel;
    private final UIToggle usePoleBoneToggle;
    private final UIButton setPoleBoneButton;
    private final UIButton clearPoleBoneButton;
    private final UIButton selectPoleButton;
    private final UILabel poleBoneLabel;
    private final UIStringList bonesList;
    private final UISearchList<String> bonesSearch;
    private final UIPropTransform ikTransform;
    private final UITrackpad iterationsField;
    private final UITrackpad chainLengthField;
    private final UITrackpad toleranceField;
    private final UIToggle useCCDToggle;
    private final UITrackpad positionWeightField;
    private final UITrackpad rotationWeightField;
    private final UITrackpad effectorRotationWeightField;
    private final UITrackpad blendField;
    private final UIToggle stretchToggle;
    private final UITrackpad stretchLimitField;
    private final UITrackpad poleAngleOffsetField;
    private final UIButton presetBlenderButton;
    private final UIButton presetMineimatorButton;
    private final UIButton presetBlockbenchButton;
    private final UITextbox ikNameField;
    private final UILabel selectedBoneConstraintsLabel;
    private final UITrackpad minX;
    private final UITrackpad minY;
    private final UITrackpad minZ;
    private final UITrackpad maxX;
    private final UITrackpad maxY;
    private final UITrackpad maxZ;
    private final UITrackpad stiffnessX;
    private final UITrackpad stiffnessY;
    private final UITrackpad stiffnessZ;

    private ModelConfig config;
    private boolean filling;
    private int nextChainId;
    private boolean hierarchyMode;
    private final List<String> hierarchyBoneIds = new ArrayList<>();

    public UIModelIKPanel(UIModelPanel parent)
    {
        this.parent = parent;

        this.relative(parent.mainView).w(1F).h(1F);

        int sideMargin = 10;
        int leftWidth = 220;
        int rightWidth = 220;
        int rowGap = 6;

        this.leftScrollView = new UIScrollView();
        this.leftScrollView.relative(this).x(sideMargin).y(10).w(leftWidth).h(1F, -20);
        this.leftScrollView.scroll.cancelScrolling().opposite().scrollSpeed *= 3;

        this.rightScrollView = new UIScrollView();
        this.rightScrollView.relative(this).x(1F, -rightWidth - sideMargin).y(10).w(rightWidth).h(1F, -20);
        this.rightScrollView.scroll.cancelScrolling().opposite().scrollSpeed *= 3;

        UILabel leftTitle = UI.label(UIKeys.MODELS_IK_HIERARCHY).background();
        leftTitle.relative(this.leftScrollView).x(0).y(0).w(leftWidth).h(12);

        this.ikList = new UIStringList((l) -> this.fillSelectedChain());
        this.ikList.background();

        this.ikSearch = new UISearchList<>(this.ikList);
        this.ikSearch.label(UIKeys.GENERAL_SEARCH);
        this.ikSearch.relative(this.leftScrollView).x(0).y(16).w(leftWidth).h(200);

        this.addIkButton = new UIButton(UIKeys.GENERAL_ADD, (b) -> this.addChain());
        this.removeIkButton = new UIButton(UIKeys.GENERAL_REMOVE, (b) -> this.removeSelectedChain());

        this.addIkButton.w(0.5F, -4).h(20);
        this.removeIkButton.w(0.5F, -4).h(20);

        UIElement addRemoveBar = UI.row(8, this.addIkButton, this.removeIkButton);
        addRemoveBar.relative(this.ikSearch).y(1F, rowGap).w(1F).h(20);

        this.editIkButton = new UIButton(UIKeys.GENERAL_EDIT, (b) -> this.openHierarchyOverlay());
        this.editIkButton.relative(addRemoveBar).y(1F, rowGap).w(1F).h(20);

        UILabel visualizerLabel = UI.label(UIKeys.MODELS_IK_VISUALIZER);
        visualizerLabel.relative(this.editIkButton).y(1F, 10).w(leftWidth).h(12);

        this.ikVisualizerToggle = new UIToggle(UIKeys.MODELS_IK_SHOW, false, (b) -> this.updateVisualizer());
        this.ikVisualizerToggle.relative(visualizerLabel).y(1F, 4).w(120);

        this.addBoneButton = new UIButton(UIKeys.MODELS_IK_ADD_SELECTED, (b) -> this.addSelectedBone());
        this.addBoneButton.relative(this.rightScrollView).x(0).y(0).w(rightWidth).h(20);

        this.removeBoneButton = new UIButton(UIKeys.MODELS_IK_REMOVE_SELECTED, (b) -> this.removeSelectedBone());
        this.clearBonesButton = new UIButton(UIKeys.MODELS_IK_CLEAR_BONES, (b) -> this.clearBones());
        this.removeBoneButton.w(0.5F, -4).h(20);
        this.clearBonesButton.w(0.5F, -4).h(20);

        UIElement boneActions = UI.row(8, this.removeBoneButton, this.clearBonesButton);
        boneActions.relative(this.addBoneButton).y(1F, 6).w(1F).h(20);

        UILabel bonesLabel = UI.label(UIKeys.MODELS_IK_BONES);
        bonesLabel.relative(boneActions).y(1F, 6).w(1F).h(12);

        this.useTargetBoneToggle = new UIToggle(UIKeys.MODELS_IK_USE_TARGET_BONE, (b) -> this.updateUseTargetBone());
        this.useTargetBoneToggle.relative(this.ikVisualizerToggle).y(1F, 6).w(180);

        this.setTargetBoneButton = new UIButton(UIKeys.MODELS_IK_SET_TARGET_FROM_SELECTED, (b) -> this.setTargetBoneFromSelected());
        this.setTargetBoneButton.relative(this.useTargetBoneToggle).y(1F, 6).w(rightWidth).h(20);

        this.clearTargetBoneButton = new UIButton(UIKeys.MODELS_IK_CLEAR_TARGET_BONE, (b) -> this.clearTargetBone());
        this.clearTargetBoneButton.relative(this.setTargetBoneButton).y(1F, 6).w(rightWidth).h(20);

        this.selectTargetButton = new UIButton(UIKeys.MODELS_IK_SELECT_TARGET, (b) -> this.selectTargetVirtualBone());
        this.selectTargetButton.relative(this.clearTargetBoneButton).y(1F, 6).w(rightWidth).h(20);

        this.targetBoneLabel = UI.label(IKey.raw(""));
        this.targetBoneLabel.relative(this.selectTargetButton).y(1F, 6).w(rightWidth).h(12);

        this.usePoleBoneToggle = new UIToggle(UIKeys.MODELS_IK_USE_POLE_BONE, (b) -> this.updateUsePoleBone());
        this.usePoleBoneToggle.relative(this.targetBoneLabel).y(1F, 6).w(180);

        this.setPoleBoneButton = new UIButton(UIKeys.MODELS_IK_SET_POLE_FROM_SELECTED, (b) -> this.setPoleBoneFromSelected());
        this.setPoleBoneButton.relative(this.usePoleBoneToggle).y(1F, 6).w(rightWidth).h(20);

        this.clearPoleBoneButton = new UIButton(UIKeys.MODELS_IK_CLEAR_POLE_BONE, (b) -> this.clearPoleBone());
        this.clearPoleBoneButton.relative(this.setPoleBoneButton).y(1F, 6).w(rightWidth).h(20);

        this.selectPoleButton = new UIButton(UIKeys.MODELS_IK_SELECT_POLE, (b) -> this.selectPoleVirtualBone());
        this.selectPoleButton.relative(this.clearPoleBoneButton).y(1F, 6).w(rightWidth).h(20);

        this.poleBoneLabel = UI.label(IKey.raw(""));
        this.poleBoneLabel.relative(this.selectPoleButton).y(1F, 6).w(rightWidth).h(12);

        this.bonesList = new UIStringList((l) -> this.onBoneSelectionChanged())
        {
            @Override
            protected boolean sortElements()
            {
                return false;
            }

            @Override
            protected void handleSwap(int from, int to)
            {
                super.handleSwap(from, to);
                UIModelIKPanel.this.reorderChainBones(from, to);
            }
        };
        this.bonesList.sorting();
        this.bonesList.background();
        this.bonesSearch = new UISearchList<>(this.bonesList);
        this.bonesSearch.label(UIKeys.GENERAL_SEARCH);
        this.bonesSearch.relative(bonesLabel).y(1F, 4).w(1F).h(82);

        this.hierarchyList = new UIStringList((l) -> {});
        this.hierarchyList.background();
        this.hierarchySearch = new UISearchList<>(this.hierarchyList);
        this.hierarchySearch.label(UIKeys.GENERAL_SEARCH);
        this.hierarchySearch.relative(bonesLabel).y(1F, 4).w(1F).h(82);

        this.addHierarchyBoneButton = new UIButton(UIKeys.MODELS_IK_ADD_SELECTED, (b) -> this.addHierarchySelectionToChain());
        this.addHierarchyBoneButton.relative(this.hierarchySearch).y(1F, 4).w(1F).h(20);

        this.ikTransform = new UIPropTransform();
        this.ikTransform.relative(this.bonesSearch).y(1F, 8).w(1F).h(120);
        this.ikTransform.callbacks(this::markDirty, this::markDirty);

        this.iterationsField = new UITrackpad((v) -> this.updateSolverSettings()).integer().limit(1, 64);
        this.chainLengthField = new UITrackpad((v) -> this.updateSolverSettings()).integer().limit(0, 32);
        this.toleranceField = new UITrackpad((v) -> this.updateSolverSettings()).limit(0.001, 10).increment(0.01);
        this.useCCDToggle = new UIToggle(UIKeys.MODELS_IK_USE_CCD, false, (b) -> this.updateSolverSettings());
        this.positionWeightField = new UITrackpad((v) -> this.updateSolverSettings()).limit(0, 1).increment(0.01);
        this.rotationWeightField = new UITrackpad((v) -> this.updateSolverSettings()).limit(0, 1).increment(0.01);
        this.effectorRotationWeightField = new UITrackpad((v) -> this.updateSolverSettings()).limit(0, 1).increment(0.01);
        this.blendField = new UITrackpad((v) -> this.updateSolverSettings()).limit(0, 1).increment(0.01);
        this.stretchToggle = new UIToggle(UIKeys.MODELS_IK_STRETCH, false, (b) -> this.updateSolverSettings());
        this.stretchLimitField = new UITrackpad((v) -> this.updateSolverSettings()).limit(1, 5).increment(0.01);
        this.poleAngleOffsetField = new UITrackpad((v) -> this.updateSolverSettings()).limit(-180, 180).increment(1);
        this.iterationsField.tooltip(UIKeys.MODELS_IK_ITERATIONS);
        this.chainLengthField.tooltip(UIKeys.MODELS_IK_CHAIN_LENGTH);
        this.toleranceField.tooltip(UIKeys.MODELS_IK_TOLERANCE);
        this.positionWeightField.tooltip(UIKeys.MODELS_IK_POSITION_WEIGHT);
        this.rotationWeightField.tooltip(UIKeys.MODELS_IK_ROTATION_WEIGHT);
        this.effectorRotationWeightField.tooltip(UIKeys.MODELS_IK_EFFECTOR_ROTATION_WEIGHT);
        this.blendField.tooltip(UIKeys.MODELS_IK_BLEND);
        this.stretchLimitField.tooltip(UIKeys.MODELS_IK_STRETCH_LIMIT);
        this.poleAngleOffsetField.tooltip(UIKeys.MODELS_IK_POLE_ANGLE_OFFSET);

        UILabel solverLabel = UI.label(UIKeys.MODELS_IK_SOLVER);
        solverLabel.relative(this.ikTransform).y(1F, 6).w(1F).h(12);

        this.iterationsField.w(0.5F, -4).h(20);
        this.chainLengthField.w(0.5F, -4).h(20);
        UIElement row1 = UI.row(8, this.iterationsField, this.chainLengthField);
        row1.relative(solverLabel).y(1F, 2).w(1F).h(20);

        this.toleranceField.w(0.5F, -4).h(20);
        this.blendField.w(0.5F, -4).h(20);
        UIElement row2 = UI.row(8, this.toleranceField, this.blendField);
        row2.relative(row1).y(1F, 4).w(1F).h(20);

        this.positionWeightField.w(0.5F, -4).h(20);
        this.rotationWeightField.w(0.5F, -4).h(20);
        UIElement rowWeights = UI.row(8, this.positionWeightField, this.rotationWeightField);
        rowWeights.relative(row2).y(1F, 4).w(1F).h(20);

        this.useCCDToggle.w(1F).h(20);
        this.useCCDToggle.relative(rowWeights).y(1F, 4);

        this.effectorRotationWeightField.relative(this.useCCDToggle).y(1F, 4).w(1F).h(20);

        this.stretchToggle.w(0.45F, -4).h(20);
        this.stretchLimitField.w(0.55F, -4).h(20);
        UIElement row3 = UI.row(8, this.stretchToggle, this.stretchLimitField);
        row3.relative(this.effectorRotationWeightField).y(1F, 4).w(1F).h(20);

        UILabel poleOffsetLabel = UI.label(UIKeys.MODELS_IK_POLE_ANGLE_OFFSET);
        poleOffsetLabel.relative(row3).y(1F, 4).w(1F).h(12);

        this.poleAngleOffsetField.relative(poleOffsetLabel).y(1F, 2).w(1F).h(20);

        this.presetBlenderButton = new UIButton(UIKeys.MODELS_IK_PRESET_BLENDER, (b) -> this.applyPreset(0));
        this.presetMineimatorButton = new UIButton(UIKeys.MODELS_IK_PRESET_MINEIMATOR, (b) -> this.applyPreset(1));
        this.presetBlockbenchButton = new UIButton(UIKeys.MODELS_IK_PRESET_BLOCKBENCH, (b) -> this.applyPreset(2));
        this.presetBlenderButton.w(1F).h(20);
        this.presetMineimatorButton.w(1F).h(20);
        this.presetBlockbenchButton.w(1F).h(20);
        this.presetBlenderButton.relative(this.poleAngleOffsetField).y(1F, 6);
        this.presetMineimatorButton.relative(this.presetBlenderButton).y(1F, 4);
        this.presetBlockbenchButton.relative(this.presetMineimatorButton).y(1F, 4);

        UILabel nameLabel = UI.label(UIKeys.MODELS_IK_NAME);
        nameLabel.relative(this.presetBlockbenchButton).y(1F, 10).w(1F).h(12);

        this.ikNameField = new UITextbox(1000, this::updateChainName);
        this.ikNameField.relative(nameLabel).y(1F, 4).w(1F).h(20);

        UILabel constraintsLabel = UI.label(UIKeys.MODELS_IK_CONSTRAINTS);
        constraintsLabel.relative(this.ikNameField).y(1F, 10).w(1F).h(12);

        this.selectedBoneConstraintsLabel = UI.label(IKey.raw(""));
        this.selectedBoneConstraintsLabel.relative(constraintsLabel).y(1F, 2).w(1F).h(12);

        UILabel minLabel = UI.label(UIKeys.MODELS_IK_MIN);
        minLabel.relative(this.selectedBoneConstraintsLabel).y(1F, 4).w(1F).h(12);

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

        UILabel stiffnessLabel = UI.label(UIKeys.MODELS_IK_STIFFNESS);
        stiffnessLabel.relative(maxRow).y(1F, 4).w(1F).h(12);

        this.stiffnessX = new UITrackpad((v) -> this.updateStiffness(0, v.floatValue())).limit(0, 1).increment(0.01);
        this.stiffnessY = new UITrackpad((v) -> this.updateStiffness(1, v.floatValue())).limit(0, 1).increment(0.01);
        this.stiffnessZ = new UITrackpad((v) -> this.updateStiffness(2, v.floatValue())).limit(0, 1).increment(0.01);
        this.stiffnessX.w(0.333F, -6);
        this.stiffnessY.w(0.333F, -6);
        this.stiffnessZ.w(0.333F, -6);

        UIElement stiffnessRow = UI.row(6, this.stiffnessX, this.stiffnessY, this.stiffnessZ);
        stiffnessRow.relative(stiffnessLabel).y(1F, 2).w(1F).h(20);

        this.leftScrollView.add(leftTitle, this.ikSearch, addRemoveBar,
            this.editIkButton, visualizerLabel, this.ikVisualizerToggle,
            this.useTargetBoneToggle, this.setTargetBoneButton, this.clearTargetBoneButton, this.selectTargetButton, this.targetBoneLabel,
            this.usePoleBoneToggle, this.setPoleBoneButton, this.clearPoleBoneButton, this.selectPoleButton, this.poleBoneLabel);

        this.rightScrollView.add(this.addBoneButton, boneActions, bonesLabel, this.bonesSearch, this.hierarchySearch, this.addHierarchyBoneButton,
            this.ikTransform, solverLabel, row1, row2, rowWeights, this.useCCDToggle, this.effectorRotationWeightField, row3, poleOffsetLabel, this.poleAngleOffsetField,
            this.presetBlenderButton, this.presetMineimatorButton, this.presetBlockbenchButton, nameLabel, this.ikNameField,
            constraintsLabel, this.selectedBoneConstraintsLabel, minLabel, minRow, maxLabel, maxRow, stiffnessLabel, stiffnessRow);

        this.add(this.leftScrollView, this.rightScrollView);

        this.hierarchySearch.setVisible(false);
        this.addHierarchyBoneButton.setVisible(false);
        this.fillSelectedChain();
    }

    public void setConfig(ModelConfig config)
    {
        this.config = config;
        this.nextChainId = this.computeNextChainId();
        this.reloadHierarchy();

        if (this.config != null)
        {
            for (IKChainConfig chain : this.config.ikChains.getAllTyped())
            {
                this.initializeTargetFromLastBone(chain);
            }
        }

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

    public UIPropTransform getTargetTransformEditor()
    {
        return this.ikTransform;
    }

    public boolean isIKVirtualBone(String bone)
    {
        return isIKVirtualBoneName(bone);
    }

    public String getTargetVirtualBone(IKChainConfig chain)
    {
        return chain == null ? "" : IK_TARGET_PREFIX + chain.getId();
    }

    public String getPoleVirtualBone(IKChainConfig chain)
    {
        return chain == null ? "" : IK_POLE_PREFIX + chain.getId();
    }

    public IKChainConfig getChainByVirtualBone(String virtualBone)
    {
        if (this.config == null)
        {
            return null;
        }

        String id = extractIKVirtualId(virtualBone);

        if (id == null)
        {
            return null;
        }

        for (IKChainConfig chain : this.config.ikChains.getAllTyped())
        {
            if (chain.getId().equals(id))
            {
                return chain;
            }
        }

        return null;
    }

    public static boolean isIKVirtualBoneName(String bone)
    {
        return extractIKVirtualId(bone) != null;
    }

    public static boolean isIKPoleVirtualBoneName(String bone)
    {
        return extractIKPoleId(bone) != null;
    }

    public static String extractIKVirtualId(String bone)
    {
        String id = extractIKTargetId(bone);

        if (id != null)
        {
            return id;
        }

        return extractIKPoleId(bone);
    }

    public static String extractIKTargetId(String bone)
    {
        if (bone == null || bone.isEmpty())
        {
            return null;
        }

        if (bone.startsWith(IK_TARGET_PREFIX))
        {
            String id = bone.substring(IK_TARGET_PREFIX.length());

            return id.isEmpty() ? null : id;
        }

        if (bone.startsWith(IK_TARGET_PREFIX_ALT))
        {
            String id = bone.substring(IK_TARGET_PREFIX_ALT.length());

            return id.isEmpty() ? null : id;
        }

        if (bone.startsWith("IK_TARGET:"))
        {
            String id = bone.substring("IK_TARGET:".length());

            return id.isEmpty() ? null : id;
        }

        if (bone.startsWith("[IK_TARGET]:"))
        {
            String id = bone.substring("[IK_TARGET]:".length());

            return id.isEmpty() ? null : id;
        }

        return null;
    }

    public static String extractIKPoleId(String bone)
    {
        if (bone == null || bone.isEmpty())
        {
            return null;
        }

        if (bone.startsWith(IK_POLE_PREFIX))
        {
            String id = bone.substring(IK_POLE_PREFIX.length());

            return id.isEmpty() ? null : id;
        }

        if (bone.startsWith(IK_POLE_PREFIX_ALT))
        {
            String id = bone.substring(IK_POLE_PREFIX_ALT.length());

            return id.isEmpty() ? null : id;
        }

        if (bone.startsWith("IK_POLE:"))
        {
            String id = bone.substring("IK_POLE:".length());

            return id.isEmpty() ? null : id;
        }

        if (bone.startsWith("[IK_POLE]:"))
        {
            String id = bone.substring("[IK_POLE]:".length());

            return id.isEmpty() ? null : id;
        }

        return null;
    }

    public void selectVirtualBone(String virtualBone)
    {
        IKChainConfig chain = this.getChainByVirtualBone(virtualBone);

        if (chain == null || this.config == null)
        {
            return;
        }

        int index = this.config.ikChains.getAllTyped().indexOf(chain);

        if (index >= 0)
        {
            this.ikList.setIndex(index);
            this.fillSelectedChain();

            if (isIKPoleVirtualBoneName(virtualBone))
            {
                chain.usePoleBone.set(false);
                this.usePoleBoneToggle.setValue(false);
                this.ikTransform.setTransform(chain.pole);
            }
            else
            {
                chain.useTargetBone.set(false);
                this.useTargetBoneToggle.setValue(false);
                this.ikTransform.setTransform(chain.target);
            }

            this.markDirty();
        }
    }

    private void fillSelectedChain()
    {
        IKChainConfig chain = this.getSelectedChain();

        this.filling = true;

        if (chain == null)
        {
            this.ikNameField.setText("");
            this.ikVisualizerToggle.setValue(false);
            this.useTargetBoneToggle.setValue(false);
            this.usePoleBoneToggle.setValue(false);
            this.ikTransform.setTransform(new Transform());
            this.bonesList.clear();
            this.targetBoneLabel.label = IKey.raw(UIKeys.MODELS_IK_TARGET_BONE.get() + ": -");
            this.poleBoneLabel.label = IKey.raw(UIKeys.MODELS_IK_POLE_BONE.get() + ": -");
            this.iterationsField.setValue(8);
            this.useCCDToggle.setValue(false);
            this.chainLengthField.setValue(0);
            this.toleranceField.setValue(0.2);
            this.positionWeightField.setValue(1);
            this.rotationWeightField.setValue(1);
            this.effectorRotationWeightField.setValue(1);
            this.blendField.setValue(1);
            this.stretchToggle.setValue(false);
            this.stretchLimitField.setValue(1.25);
            this.poleAngleOffsetField.setValue(0);
            this.selectedBoneConstraintsLabel.label = IKey.raw(UIKeys.MODELS_IK_SELECTED_BONE_CONSTRAINTS.get() + ": -");
            this.minX.setValue(-180);
            this.minY.setValue(-180);
            this.minZ.setValue(-180);
            this.maxX.setValue(180);
            this.maxY.setValue(180);
            this.maxZ.setValue(180);
            this.stiffnessX.setValue(0);
            this.stiffnessY.setValue(0);
            this.stiffnessZ.setValue(0);
        }
        else
        {
            this.ensureTargetParentBone(chain);
            this.initializeTargetFromLastBone(chain);
            this.ikNameField.setText(chain.name.get());
            this.ikVisualizerToggle.setValue(chain.visualizer.get());
            this.useTargetBoneToggle.setValue(chain.useTargetBone.get());
            this.usePoleBoneToggle.setValue(chain.usePoleBone.get());
            this.ikTransform.setTransform(chain.target);
            this.iterationsField.setValue(chain.iterations.get());
            this.useCCDToggle.setValue(chain.useCCD.get());
            this.chainLengthField.setValue(chain.chainLength.get());
            this.toleranceField.setValue(chain.tolerance.get());
            this.positionWeightField.setValue(chain.positionWeight.get());
            this.rotationWeightField.setValue(chain.rotationWeight.get());
            this.effectorRotationWeightField.setValue(chain.effectorRotationWeight.get());
            this.blendField.setValue(chain.blend.get());
            this.stretchToggle.setValue(chain.stretch.get());
            this.stretchLimitField.setValue(chain.stretchLimit.get());
            this.poleAngleOffsetField.setValue(chain.poleAngleOffset.get());
            this.refreshBonesList(chain);
            this.updateTargetBoneLabel(chain);
            this.updatePoleBoneLabel(chain);
            this.applyConstraintFields(chain);
        }

        this.filling = false;
        this.updateScrollSize(this.leftScrollView);
        this.updateScrollSize(this.rightScrollView);
    }

    @Override
    public void resize()
    {
        super.resize();

        this.updateScrollSize(this.leftScrollView);
        this.updateScrollSize(this.rightScrollView);
    }

    private void updateScrollSize(UIScrollView scrollView)
    {
        if (scrollView == null)
        {
            return;
        }

        int maxY = scrollView.area.y;

        for (UIElement child : scrollView.getChildren(UIElement.class))
        {
            if (!child.isVisible())
            {
                continue;
            }

            maxY = Math.max(maxY, child.area.ey());
        }

        scrollView.scroll.scrollSize = Math.max(scrollView.area.h, maxY - scrollView.area.y + 10);
        scrollView.scroll.clamp();
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

    private void updateUseTargetBone()
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

        chain.useTargetBone.set(this.useTargetBoneToggle.getValue());
        this.markDirty();
    }

    private void setTargetBoneFromSelected()
    {
        IKChainConfig chain = this.getSelectedChain();
        String selectedBone = this.parent.renderer.getSelectedBone();

        if (chain == null || selectedBone == null || selectedBone.isEmpty() || this.isIKVirtualBone(selectedBone))
        {
            return;
        }

        chain.targetBone.set(selectedBone);
        chain.useTargetBone.set(true);
        this.useTargetBoneToggle.setValue(true);
        this.markDirty();
        this.updateTargetBoneLabel(chain);
    }

    private void selectTargetVirtualBone()
    {
        IKChainConfig chain = this.getSelectedChain();

        if (chain == null)
        {
            return;
        }

        if (chain.useTargetBone.get() && !chain.targetBone.get().isEmpty())
        {
            this.parent.selectBoneFromEditor(chain.targetBone.get());
            return;
        }

        List<String> bones = chain.getBones();

        if (!bones.isEmpty())
        {
            this.parent.selectBoneFromEditor(bones.get(bones.size() - 1));
        }
    }

    private void selectPoleVirtualBone()
    {
        IKChainConfig chain = this.getSelectedChain();

        if (chain == null)
        {
            return;
        }

        if (chain.usePoleBone.get() && !chain.poleBone.get().isEmpty())
        {
            this.parent.selectBoneFromEditor(chain.poleBone.get());
            return;
        }

        List<String> bones = chain.getBones();

        if (bones.size() > 1)
        {
            this.parent.selectBoneFromEditor(bones.get(1));
        }
    }

    private void clearTargetBone()
    {
        IKChainConfig chain = this.getSelectedChain();

        if (chain == null)
        {
            return;
        }

        chain.targetBone.set("");
        chain.useTargetBone.set(false);
        this.useTargetBoneToggle.setValue(false);
        this.markDirty();
        this.updateTargetBoneLabel(chain);
    }

    private void updateUsePoleBone()
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

        chain.usePoleBone.set(this.usePoleBoneToggle.getValue());
        this.markDirty();
    }

    private void setPoleBoneFromSelected()
    {
        IKChainConfig chain = this.getSelectedChain();
        String selectedBone = this.parent.renderer.getSelectedBone();

        if (chain == null || selectedBone == null || selectedBone.isEmpty() || this.isIKVirtualBone(selectedBone))
        {
            return;
        }

        chain.poleBone.set(selectedBone);
        chain.usePoleBone.set(true);
        this.usePoleBoneToggle.setValue(true);
        this.markDirty();
        this.updatePoleBoneLabel(chain);
    }

    private void clearPoleBone()
    {
        IKChainConfig chain = this.getSelectedChain();

        if (chain == null)
        {
            return;
        }

        chain.poleBone.set("");
        chain.usePoleBone.set(false);
        this.usePoleBoneToggle.setValue(false);
        this.markDirty();
        this.updatePoleBoneLabel(chain);
    }

    private void updateSolverSettings()
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

        chain.iterations.set((int) this.iterationsField.getValue());
        chain.useCCD.set(this.useCCDToggle.getValue());
        chain.chainLength.set((int) this.chainLengthField.getValue());
        chain.tolerance.set((float) this.toleranceField.getValue());
        chain.positionWeight.set((float) this.positionWeightField.getValue());
        chain.rotationWeight.set((float) this.rotationWeightField.getValue());
        chain.effectorRotationWeight.set((float) this.effectorRotationWeightField.getValue());
        chain.blend.set((float) this.blendField.getValue());
        chain.stretch.set(this.stretchToggle.getValue());
        chain.stretchLimit.set((float) this.stretchLimitField.getValue());
        chain.poleAngleOffset.set((float) this.poleAngleOffsetField.getValue());
        this.markDirty();
    }

    private void applyPreset(int preset)
    {
        IKChainConfig chain = this.getSelectedChain();

        if (chain == null)
        {
            return;
        }

        this.filling = true;

        if (preset == 0)
        {
            this.useCCDToggle.setValue(false);
            this.iterationsField.setValue(32);
            this.toleranceField.setValue(0.01);
            this.positionWeightField.setValue(1);
            this.rotationWeightField.setValue(1);
            this.effectorRotationWeightField.setValue(1);
            this.blendField.setValue(1);
            this.stretchToggle.setValue(true);
            this.stretchLimitField.setValue(1.25);
        }
        else if (preset == 1)
        {
            this.useCCDToggle.setValue(false);
            this.iterationsField.setValue(20);
            this.toleranceField.setValue(0.05);
            this.positionWeightField.setValue(1);
            this.rotationWeightField.setValue(0.9);
            this.effectorRotationWeightField.setValue(0.9);
            this.blendField.setValue(1);
            this.stretchToggle.setValue(false);
            this.stretchLimitField.setValue(1.1);
        }
        else
        {
            this.useCCDToggle.setValue(false);
            this.iterationsField.setValue(16);
            this.toleranceField.setValue(0.1);
            this.positionWeightField.setValue(1);
            this.rotationWeightField.setValue(0.8);
            this.effectorRotationWeightField.setValue(0.8);
            this.blendField.setValue(1);
            this.stretchToggle.setValue(false);
            this.stretchLimitField.setValue(1.0);
        }

        this.filling = false;
        this.updateSolverSettings();
    }

    private void addSelectedBone()
    {
        IKChainConfig chain = this.getSelectedChain();
        String selectedBone = this.parent.renderer.getSelectedBone();

        if (chain == null || selectedBone == null || selectedBone.isEmpty() || this.isIKVirtualBone(selectedBone))
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
        this.ensureTargetParentBone(chain);
        this.initializeTargetFromLastBone(chain);
        this.markDirty();
        this.refreshBonesList(chain);
        this.bonesList.setCurrent(selectedBone);
        this.onBoneSelectionChanged();
    }

    private void addHierarchySelectionToChain()
    {
        int visibleIndex = this.hierarchyList.getIndex();

        if (visibleIndex < 0 || visibleIndex >= this.hierarchyBoneIds.size())
        {
            return;
        }

        String bone = this.hierarchyBoneIds.get(visibleIndex);

        if (bone == null || bone.isEmpty())
        {
            return;
        }

        this.parent.renderer.setSelectedBone(bone);
        this.addSelectedBone();
    }

    private void removeSelectedBone()
    {
        IKChainConfig chain = this.getSelectedChain();
        int index = this.bonesList.getIndex();

        if (chain == null || index < 0 || index >= chain.bones.getAllTyped().size())
        {
            return;
        }

        String bone = chain.bones.getAllTyped().get(index).get();

        chain.bones.remove(index);
        chain.bones.sync();
        chain.removeJointConstraint(bone);
        if (bone.equals(chain.poleBone.get()))
        {
            chain.poleBone.set("");
            chain.usePoleBone.set(false);
            this.usePoleBoneToggle.setValue(false);
        }
        this.ensureTargetParentBone(chain);
        this.markDirty();
        this.refreshBonesList(chain);
        this.updatePoleBoneLabel(chain);
        this.onBoneSelectionChanged();
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

        while (!chain.jointConstraints.getAllTyped().isEmpty())
        {
            chain.jointConstraints.remove(0);
        }

        chain.bones.sync();
        chain.jointConstraints.sync();
        chain.poleBone.set("");
        chain.usePoleBone.set(false);
        this.usePoleBoneToggle.setValue(false);
        this.ensureTargetParentBone(chain);
        this.markDirty();
        this.refreshBonesList(chain);
        this.updatePoleBoneLabel(chain);
        this.onBoneSelectionChanged();
    }

    private void refreshBonesList(IKChainConfig chain)
    {
        String selected = this.bonesList.getCurrentFirst();

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

        if (selected != null)
        {
            this.bonesList.setCurrent(selected);
        }
    }

    private void reorderChainBones(int from, int to)
    {
        IKChainConfig chain = this.getSelectedChain();

        if (chain == null || from == to || from < 0 || to < 0 || from >= chain.bones.getAllTyped().size() || to >= chain.bones.getAllTyped().size())
        {
            return;
        }

        ValueString moved = chain.bones.getAllTyped().remove(from);
        chain.bones.getAllTyped().add(to, moved);
        chain.bones.sync();
        this.markDirty();
    }

    public void applyHierarchyBones(IKChainConfig chain, List<String> bones)
    {
        if (chain == null)
        {
            return;
        }

        while (!chain.bones.getAllTyped().isEmpty())
        {
            chain.bones.remove(0);
        }

        for (int i = 0; i < bones.size(); i++)
        {
            String bone = bones.get(i);

            if (bone != null && !bone.isEmpty())
            {
                chain.bones.add(new ValueString(String.valueOf(i), bone));
            }
        }

        chain.bones.sync();
        this.ensureTargetParentBone(chain);
        this.initializeTargetFromLastBone(chain);
        this.markDirty();
        this.refreshBonesList(chain);
        this.onBoneSelectionChanged();
    }

    public void previewBone(String bone)
    {
        if (bone != null && !bone.isEmpty())
        {
            this.parent.renderer.setSelectedBone(bone);
        }
    }

    private void onBoneSelectionChanged()
    {
        IKChainConfig chain = this.getSelectedChain();

        if (chain == null)
        {
            return;
        }

        this.applyConstraintFields(chain);
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

        String selectedBone = this.bonesList.getCurrentFirst();
        IKJointConstraint constraint = selectedBone == null ? null : chain.getOrCreateJointConstraint(selectedBone);

        if (axis == 0)
        {
            if (constraint != null)
            {
                if (min) constraint.minX.set(value);
                else constraint.maxX.set(value);
            }
            else
            {
                if (min) chain.minX.set(value);
                else chain.maxX.set(value);
            }
        }
        else if (axis == 1)
        {
            if (constraint != null)
            {
                if (min) constraint.minY.set(value);
                else constraint.maxY.set(value);
            }
            else
            {
                if (min) chain.minY.set(value);
                else chain.maxY.set(value);
            }
        }
        else
        {
            if (constraint != null)
            {
                if (min) constraint.minZ.set(value);
                else constraint.maxZ.set(value);
            }
            else
            {
                if (min) chain.minZ.set(value);
                else chain.maxZ.set(value);
            }
        }

        this.markDirty();
    }

    private void updateStiffness(int axis, float value)
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

        String selectedBone = this.bonesList.getCurrentFirst();
        IKJointConstraint constraint = selectedBone == null ? null : chain.getOrCreateJointConstraint(selectedBone);

        if (axis == 0)
        {
            if (constraint != null) constraint.stiffnessX.set(value);
            else chain.stiffnessX.set(value);
        }
        else if (axis == 1)
        {
            if (constraint != null) constraint.stiffnessY.set(value);
            else chain.stiffnessY.set(value);
        }
        else
        {
            if (constraint != null) constraint.stiffnessZ.set(value);
            else chain.stiffnessZ.set(value);
        }

        this.markDirty();
    }

    private void updateTargetBoneLabel(IKChainConfig chain)
    {
        String bone = chain == null || chain.targetBone.get().isEmpty() ? "-" : chain.targetBone.get();

        this.targetBoneLabel.label = IKey.raw(UIKeys.MODELS_IK_TARGET_BONE.get() + ": " + bone);
    }

    private void updatePoleBoneLabel(IKChainConfig chain)
    {
        String bone = chain == null || chain.poleBone.get().isEmpty() ? "-" : chain.poleBone.get();

        this.poleBoneLabel.label = IKey.raw(UIKeys.MODELS_IK_POLE_BONE.get() + ": " + bone);
    }

    private void applyConstraintFields(IKChainConfig chain)
    {
        String selectedBone = this.bonesList.getCurrentFirst();
        IKJointConstraint constraint = selectedBone == null ? null : chain.getJointConstraint(selectedBone);

        this.filling = true;

        if (constraint != null)
        {
            this.minX.setValue(constraint.minX.get());
            this.minY.setValue(constraint.minY.get());
            this.minZ.setValue(constraint.minZ.get());
            this.maxX.setValue(constraint.maxX.get());
            this.maxY.setValue(constraint.maxY.get());
            this.maxZ.setValue(constraint.maxZ.get());
            this.stiffnessX.setValue(constraint.stiffnessX.get());
            this.stiffnessY.setValue(constraint.stiffnessY.get());
            this.stiffnessZ.setValue(constraint.stiffnessZ.get());
            this.selectedBoneConstraintsLabel.label = IKey.raw(UIKeys.MODELS_IK_SELECTED_BONE_CONSTRAINTS.get() + ": " + selectedBone);
        }
        else
        {
            this.minX.setValue(chain.minX.get());
            this.minY.setValue(chain.minY.get());
            this.minZ.setValue(chain.minZ.get());
            this.maxX.setValue(chain.maxX.get());
            this.maxY.setValue(chain.maxY.get());
            this.maxZ.setValue(chain.maxZ.get());
            this.stiffnessX.setValue(chain.stiffnessX.get());
            this.stiffnessY.setValue(chain.stiffnessY.get());
            this.stiffnessZ.setValue(chain.stiffnessZ.get());
            this.selectedBoneConstraintsLabel.label = IKey.raw(UIKeys.MODELS_IK_SELECTED_BONE_CONSTRAINTS.get() + ": " + (selectedBone == null ? "-" : selectedBone));
        }

        this.filling = false;
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

    private void openHierarchyOverlay()
    {
        IKChainConfig chain = this.getSelectedChain();

        if (chain == null || this.getContext() == null || this.config == null)
        {
            return;
        }

        UIModelIKHierarchyOverlayPanel panel = new UIModelIKHierarchyOverlayPanel(this, chain, this.config.getId());

        UIOverlay.addOverlay(this.getContext(), panel, 520, 340);
    }

    private void toggleHierarchyMode()
    {
        this.hierarchyMode = !this.hierarchyMode;

        this.bonesSearch.setVisible(!this.hierarchyMode);
        this.addBoneButton.setVisible(!this.hierarchyMode);
        this.removeBoneButton.setVisible(!this.hierarchyMode);
        this.clearBonesButton.setVisible(!this.hierarchyMode);

        this.hierarchySearch.setVisible(this.hierarchyMode);
        this.addHierarchyBoneButton.setVisible(this.hierarchyMode);

        if (this.hierarchyMode)
        {
            this.reloadHierarchy();
        }

        this.resize();
    }

    private void reloadHierarchy()
    {
        this.hierarchyList.clear();
        this.hierarchyBoneIds.clear();

        if (this.config == null)
        {
            return;
        }

        ModelInstance instance = BBSModClient.getModels().getModel(this.config.getId());

        if (instance == null || !(instance.model instanceof Model model))
        {
            return;
        }

        for (ModelGroup group : model.topGroups)
        {
            this.collectHierarchy(group, 0);
        }
    }

    private void collectHierarchy(ModelGroup group, int depth)
    {
        StringBuilder prefix = new StringBuilder();

        for (int i = 0; i < depth; i++)
        {
            prefix.append("  ");
        }

        String label = prefix + "└ " + group.id;

        this.hierarchyList.add(label);
        this.hierarchyBoneIds.add(group.id);

        for (ModelGroup child : group.children)
        {
            this.collectHierarchy(child, depth + 1);
        }
    }

    private void initializeTargetFromLastBone(IKChainConfig chain)
    {
        if (chain == null || chain.useTargetBone.get() || !this.isTargetNearCenter(chain))
        {
            return;
        }

        List<String> bones = chain.getBones();

        if (bones.isEmpty() || this.config == null)
        {
            return;
        }

        String lastBone = bones.get(bones.size() - 1);
        ModelInstance instance = BBSModClient.getModels().getModel(this.config.getId());

        if (instance == null || !(instance.model instanceof Model model))
        {
            return;
        }

        ModelGroup group = model.getGroup(lastBone);

        if (group == null)
        {
            return;
        }

        Vector3f target = this.calculateDefaultTarget(model, group);

        chain.target.translate.set(target);
    }

    private void ensureTargetParentBone(IKChainConfig chain)
    {
        if (chain == null)
        {
            return;
        }

        List<String> bones = chain.getBones();
        String currentParent = chain.targetParentBone.get();

        if (bones.isEmpty())
        {
            chain.targetParentBone.set("");
            return;
        }

        if (currentParent == null || currentParent.isEmpty())
        {
            return;
        }

        if (bones.contains(currentParent))
        {
            chain.targetParentBone.set("");
        }
    }

    private Vector3f resolveTargetWorldForEditor(IKChainConfig chain)
    {
        return new Vector3f(chain.target.translate);
    }

    private void makeTargetLocalToParent(IKChainConfig chain)
    {
    }

    private boolean isTargetNearCenter(IKChainConfig chain)
    {
        return Math.abs(chain.target.translate.x) < 0.001F
            && Math.abs(chain.target.translate.y) < 0.001F
            && Math.abs(chain.target.translate.z) < 0.001F;
    }

    private Vector3f calculateDefaultTarget(Model model, ModelGroup group)
    {
        Map<ModelGroup, Matrix4f> matrices = new HashMap<>();

        for (ModelGroup top : model.topGroups)
        {
            this.collectInitialMatrices(top, new Matrix4f(), matrices);
        }

        Matrix4f matrix = matrices.get(group);

        if (matrix != null)
        {
            Vector3f translation = new Vector3f();

            matrix.getTranslation(translation);
            translation.mul(16F);

            if (!this.isNearZero(translation))
            {
                return translation;
            }
        }

        Vector3f pivot = group.initial.pivot;
        Vector3f origin = group.initial.translate;

        return origin.lengthSquared() > pivot.lengthSquared() ? new Vector3f(origin) : new Vector3f(pivot);
    }

    private void collectInitialMatrices(ModelGroup group, Matrix4f parent, Map<ModelGroup, Matrix4f> matrices)
    {
        Matrix4f matrix = new Matrix4f(parent);
        Vector3f translate = group.initial.translate;
        Vector3f pivot = group.initial.pivot;

        matrix.translate(
            -(translate.x - pivot.x) / 16F,
            (translate.y - pivot.y) / 16F,
            (translate.z - pivot.z) / 16F
        );
        matrix.translate(pivot.x / 16F, pivot.y / 16F, pivot.z / 16F);
        matrix.rotateZ((float) Math.toRadians(group.initial.rotate.z));
        matrix.rotateY((float) Math.toRadians(group.initial.rotate.y));
        matrix.rotateX((float) Math.toRadians(group.initial.rotate.x));
        matrix.rotateZ((float) Math.toRadians(group.initial.rotate2.z));
        matrix.rotateY((float) Math.toRadians(group.initial.rotate2.y));
        matrix.rotateX((float) Math.toRadians(group.initial.rotate2.x));
        matrix.scale(group.initial.scale.x, group.initial.scale.y, group.initial.scale.z);
        matrix.translate(-pivot.x / 16F, -pivot.y / 16F, -pivot.z / 16F);

        matrices.put(group, matrix);

        for (ModelGroup child : group.children)
        {
            this.collectInitialMatrices(child, matrix, matrices);
        }
    }

    private boolean isNearZero(Vector3f vector)
    {
        return Math.abs(vector.x) < 0.001F && Math.abs(vector.y) < 0.001F && Math.abs(vector.z) < 0.001F;
    }

    private void markDirty()
    {
        if (!this.filling)
        {
            this.parent.dirty();
        }
    }
}

