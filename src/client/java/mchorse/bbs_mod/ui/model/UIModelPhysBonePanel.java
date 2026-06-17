package mchorse.bbs_mod.ui.model;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.model.ModelConfig;
import mchorse.bbs_mod.cubic.model.PhysBoneSlot;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.core.ValueList;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class UIModelPhysBonePanel extends UIElement
{
    private static final int SIDE_MARGIN = 10;
    private static final int LEFT_WIDTH = 220;
    private static final int RIGHT_WIDTH = 260;

    /* Left panel: list of bones */
    private final UIStringList boneList;
    private final UIButton     addBone;
    private final UIButton     removeBone;

    /* Right panel: configuration scroll view */
    private final UIScrollView detailScroll;
    private final UILabel      noSelectionLabel;
    private final UILabel      boneNameLabel;
    private final UIButton     boneButton;
    private final UIToggle     enabledToggle;
    private final UIToggle     pitchToggle;
    private final UIToggle     rollToggle;
    private final UITrackpad   stiffnessPad;
    private final UITrackpad   dampingPad;
    private final UITrackpad   gravityPad;
    private final UITrackpad   inertiaPad;
    private final UITrackpad   simSpeedPad;
    private final UITrackpad   maxAnglePad;
    private final UITrackpad   rollFactorPad;
    private final UIToggle     collisionToggle;

    /* State */
    private final UIModelPanel editor;
    private ModelConfig        config;
    private PhysBoneSlot       selected;
    private List<String>       boneNames = new ArrayList<>();

    public UIModelPhysBonePanel(UIModelPanel editor)
    {
        this.editor = editor;

        this.relative(editor.mainView).w(1F).h(1F);

        /* ----------------------------------------------------------------
         * LEFT SIDE: Bone definitions list
         * -------------------------------------------------------------- */
        UILabel listTitle = UI.label(UIKeys.MODELS_PHYS_BONES_EDITOR).background();
        listTitle.relative(this).x(SIDE_MARGIN).y(10).w(LEFT_WIDTH).h(12);

        this.boneList = new UIStringList((items) ->
        {
            if (items != null && !items.isEmpty())
            {
                /* Match the selected list item back to our slot */
                this.selectBone(items.get(0));
            }
        });
        this.boneList.relative(this)
                     .x(SIDE_MARGIN).y(26)
                     .w(LEFT_WIDTH).h(1F, -44);
        this.boneList.background();
        this.boneList.scroll.scrollItemSize = 18;

        this.addBone = new UIButton(UIKeys.MODELS_PHYS_BONES_ADD, (b) -> this.onAddBone());
        this.addBone.relative(this)
                    .x(SIDE_MARGIN).y(1F, -40)
                    .w(LEFT_WIDTH).h(18);
        this.addBone.tooltip(UIKeys.MODELS_PHYS_BONES_ADD_TOOLTIP);

        this.removeBone = new UIButton(UIKeys.MODELS_PHYS_BONES_REMOVE, (b) -> this.onRemoveBone());
        this.removeBone.relative(this)
                       .x(SIDE_MARGIN).y(1F, -20)
                       .w(LEFT_WIDTH).h(18);
        this.removeBone.tooltip(UIKeys.MODELS_PHYS_BONES_REMOVE_TOOLTIP);

        /* ----------------------------------------------------------------
         * RIGHT SIDE: PhysBone parameters
         * -------------------------------------------------------------- */
        UILabel editorTitle = UI.label(UIKeys.MODELS_PHYS_BONES_EDITOR).background();
        editorTitle.relative(this)
                   .x(1F, -RIGHT_WIDTH - SIDE_MARGIN).y(10)
                   .w(RIGHT_WIDTH).h(12);

        this.boneNameLabel = UI.label(IKey.raw("-"));
        this.boneNameLabel.relative(editorTitle).y(1F, 4).w(1F).h(12);

        this.noSelectionLabel = UI.label(UIKeys.MODELS_PHYS_BONES_NO_SELECTION);
        this.noSelectionLabel.relative(this)
                             .x(1F, -RIGHT_WIDTH - SIDE_MARGIN)
                             .y(38)
                             .w(RIGHT_WIDTH).h(20);

        this.detailScroll = UI.scrollView(20, 8);
        this.detailScroll.scroll.cancelScrolling().opposite().scrollSpeed *= 3;
        this.detailScroll.relative(this)
                         .x(1F, -RIGHT_WIDTH - SIDE_MARGIN).y(38)
                         .w(RIGHT_WIDTH).h(1F, -48);

        UIElement fields = new UIElement();
        fields.relative(this.detailScroll).w(1F);
        fields.column().stretch().vertical().height(20).padding(4);

        /* Bone selection button */
        this.boneButton = new UIButton(UIKeys.MODELS_PHYS_BONES_BONE, (b) ->
            this.openBonePicker((bone) ->
            {
                if (this.selected != null)
                {
                    this.selected.bone.set(bone);
                    this.updateBoneLabel();
                    this.refreshBoneList();
                    this.editor.dirty();
                }
            })
        );
        this.boneButton.tooltip(UIKeys.MODELS_PHYS_BONES_BONE_TOOLTIP);

        /* Config fields */
        this.enabledToggle = new UIToggle(UIKeys.MODELS_PHYS_BONES_ENABLED, (b) ->
        {
            if (this.selected != null)
            {
                this.selected.enabled.set(b.getValue());
                this.editor.dirty();
            }
        });
        this.enabledToggle.tooltip(UIKeys.MODELS_PHYS_BONES_ENABLED_TOOLTIP);

        this.pitchToggle = new UIToggle(UIKeys.MODELS_PHYS_BONES_PITCH, (b) ->
        {
            if (this.selected != null)
            {
                this.selected.pitch.set(b.getValue());
                this.editor.dirty();
            }
        });
        this.pitchToggle.tooltip(UIKeys.MODELS_PHYS_BONES_PITCH_TOOLTIP);

        this.rollToggle = new UIToggle(UIKeys.MODELS_PHYS_BONES_ROLL, (b) ->
        {
            if (this.selected != null)
            {
                this.selected.roll.set(b.getValue());
                this.editor.dirty();
            }
        });
        this.rollToggle.tooltip(UIKeys.MODELS_PHYS_BONES_ROLL_TOOLTIP);

        this.stiffnessPad = this.buildPad((v) ->
        {
            if (this.selected != null)
            {
                this.selected.stiffness.set(v.floatValue());
                this.editor.dirty();
            }
        }, 0D, 100D, 0.5D, 0.1D, 1.0D);
        this.stiffnessPad.tooltip(UIKeys.MODELS_PHYS_BONES_STIFFNESS_TOOLTIP);

        this.dampingPad = this.buildPad((v) ->
        {
            if (this.selected != null)
            {
                this.selected.damping.set(v.floatValue());
                this.editor.dirty();
            }
        }, 0D, 20D, 0.1D, 0.01D, 0.5D);
        this.dampingPad.tooltip(UIKeys.MODELS_PHYS_BONES_DAMPING_TOOLTIP);

        this.gravityPad = this.buildPad((v) ->
        {
            if (this.selected != null)
            {
                this.selected.gravity.set(v.floatValue());
                this.editor.dirty();
            }
        }, -5D, 5D, 0.05D, 0.01D, 0.25D);
        this.gravityPad.tooltip(UIKeys.MODELS_PHYS_BONES_GRAVITY_TOOLTIP);

        this.inertiaPad = this.buildPad((v) ->
        {
            if (this.selected != null)
            {
                this.selected.inertia.set(v.floatValue());
                this.editor.dirty();
            }
        }, 0D, 1D, 0.05D, 0.01D, 0.1D);
        this.inertiaPad.tooltip(UIKeys.MODELS_PHYS_BONES_INERTIA_TOOLTIP);

        this.simSpeedPad = this.buildPad((v) ->
        {
            if (this.selected != null)
            {
                this.selected.simSpeed.set(v.floatValue());
                this.editor.dirty();
            }
        }, 0D, 5D, 0.05D, 0.01D, 0.25D);
        this.simSpeedPad.tooltip(UIKeys.MODELS_PHYS_BONES_SIM_SPEED_TOOLTIP);

        this.maxAnglePad = this.buildPad((v) ->
        {
            if (this.selected != null)
            {
                this.selected.maxAngle.set(v.floatValue());
                this.editor.dirty();
            }
        }, 0D, 180D, 1D, 0.25D, 5D);
        this.maxAnglePad.tooltip(UIKeys.MODELS_PHYS_BONES_MAX_ANGLE_TOOLTIP);

        this.rollFactorPad = this.buildPad((v) ->
        {
            if (this.selected != null)
            {
                this.selected.rollFactor.set(v.floatValue());
                this.editor.dirty();
            }
        }, -2D, 2D, 0.05D, 0.01D, 0.25D);
        this.rollFactorPad.tooltip(UIKeys.MODELS_PHYS_BONES_ROLL_FACTOR_TOOLTIP);

        this.collisionToggle = new UIToggle(UIKeys.MODELS_PHYS_BONES_COLLISION, (b) ->
        {
            if (this.selected != null)
            {
                this.selected.collision.set(b.getValue());
                this.editor.dirty();
            }
        });
        this.collisionToggle.tooltip(UIKeys.MODELS_PHYS_BONES_COLLISION_TOOLTIP);

        fields.add(
            UI.label(UIKeys.MODELS_PHYS_BONES_BONE), this.boneButton,
            this.enabledToggle,
            this.pitchToggle,
            this.rollToggle,
            UI.label(UIKeys.MODELS_PHYS_BONES_STIFFNESS), this.stiffnessPad,
            UI.label(UIKeys.MODELS_PHYS_BONES_DAMPING), this.dampingPad,
            UI.label(UIKeys.MODELS_PHYS_BONES_GRAVITY), this.gravityPad,
            UI.label(UIKeys.MODELS_PHYS_BONES_INERTIA), this.inertiaPad,
            UI.label(UIKeys.MODELS_PHYS_BONES_SIM_SPEED), this.simSpeedPad,
            UI.label(UIKeys.MODELS_PHYS_BONES_MAX_ANGLE), this.maxAnglePad,
            UI.label(UIKeys.MODELS_PHYS_BONES_ROLL_FACTOR), this.rollFactorPad,
            this.collisionToggle
        );
        this.detailScroll.add(fields);

        this.add(listTitle, this.boneList, this.addBone, this.removeBone);
        this.add(editorTitle, this.boneNameLabel, this.noSelectionLabel, this.detailScroll);

        this.setDetailVisible(false);
    }

    private UITrackpad buildPad(Consumer<Double> callback,
                                double min, double max,
                                double step, double smallStep, double bigStep)
    {
        UITrackpad pad = new UITrackpad((v) -> callback.accept(v.doubleValue()));
        pad.limit(min, max).values(step, smallStep, bigStep);
        return pad;
    }

    private void setDetailVisible(boolean visible)
    {
        this.detailScroll.setEnabled(visible);
        this.detailScroll.setVisible(visible);
        this.noSelectionLabel.setVisible(!visible);
        this.boneNameLabel.setVisible(visible);
    }

    private void updateBoneLabel()
    {
        if (this.selected == null)
        {
            this.boneButton.label = UIKeys.MODELS_PHYS_BONES_BONE;
            return;
        }
        String name = this.selected.bone.get();
        this.boneButton.label = name.isEmpty() ? UIKeys.MODELS_PHYS_BONES_BONE : IKey.constant(name);
    }

    private void refreshDetailFields()
    {
        if (this.selected == null)
        {
            this.setDetailVisible(false);
            return;
        }

        this.setDetailVisible(true);

        String title = this.selected.bone.get();
        if (title.isEmpty())
        {
            title = "Slot " + this.selected.getId();
        }
        this.boneNameLabel.label = IKey.raw(title);

        this.updateBoneLabel();
        this.enabledToggle.setValue(this.selected.enabled.get());
        this.pitchToggle.setValue(this.selected.pitch.get());
        this.rollToggle.setValue(this.selected.roll.get());
        this.stiffnessPad.setValue(this.selected.stiffness.get());
        this.dampingPad.setValue(this.selected.damping.get());
        this.gravityPad.setValue(this.selected.gravity.get());
        this.inertiaPad.setValue(this.selected.inertia.get());
        this.simSpeedPad.setValue(this.selected.simSpeed.get());
        this.maxAnglePad.setValue(this.selected.maxAngle.get());
        this.rollFactorPad.setValue(this.selected.rollFactor.get());
        this.collisionToggle.setValue(this.selected.collision.get());
    }

    private void onAddBone()
    {
        if (this.config == null)
        {
            return;
        }

        ValueList<PhysBoneSlot> list = this.config.physBones;
        String newId = String.valueOf(list.getList().size());
        PhysBoneSlot slot = new PhysBoneSlot(newId);
        list.add(slot);

        this.refreshBoneList();
        this.boneList.setCurrent(this.getDisplayName(slot));
        this.selectBoneBySlot(slot);
        this.editor.dirty();
    }

    private void onRemoveBone()
    {
        if (this.config == null || this.selected == null)
        {
            return;
        }

        this.config.physBones.remove(this.selected);
        this.selected = null;
        this.refreshBoneList();
        this.setDetailVisible(false);
        this.editor.dirty();
    }

    private void selectBone(String displayName)
    {
        if (this.config == null)
        {
            return;
        }

        this.selected = null;

        for (PhysBoneSlot slot : this.config.physBones.getList())
        {
            if (this.getDisplayName(slot).equals(displayName))
            {
                this.selected = slot;
                break;
            }
        }

        this.refreshDetailFields();
    }

    private void selectBoneBySlot(PhysBoneSlot slot)
    {
        this.selected = slot;
        this.refreshDetailFields();
    }

    private String getDisplayName(PhysBoneSlot slot)
    {
        String name = slot.bone.get();
        if (name.isEmpty())
        {
            return "Slot " + slot.getId();
        }
        return name;
    }

    private void refreshBoneList()
    {
        List<String> names = new ArrayList<>();

        if (this.config != null)
        {
            for (PhysBoneSlot slot : this.config.physBones.getList())
            {
                names.add(this.getDisplayName(slot));
            }
        }

        this.boneList.setList(names);
        this.boneList.update();
    }

    private void openBonePicker(Consumer<String> callback)
    {
        if (this.boneNames.isEmpty())
        {
            return;
        }

        UIModelIKPanel.UIBonePickerContextMenu menu = new UIModelIKPanel.UIBonePickerContextMenu(this.boneNames, callback);
        this.getContext().replaceContextMenu(menu);
        menu.xy(this.area.x + LEFT_WIDTH + SIDE_MARGIN + 4, this.area.y + 40)
            .w(180).h(220).bounds(this.getContext().menu.overlay, 5);
    }

    public void setConfig(ModelConfig config)
    {
        this.config = config;
        this.selected = null;
        this.boneNames.clear();

        if (config != null)
        {
            ModelInstance instance = BBSModClient.getModels().getModel(config.getId());

            if (instance != null && instance.getModel() != null)
            {
                this.boneNames.addAll(instance.getModel().getAllGroupKeys());
                instance.getModel().getAllBOBJBones().forEach(b -> this.boneNames.add(b.name));
                Collections.sort(this.boneNames);
            }
        }

        this.refreshBoneList();
        this.setDetailVisible(false);
    }

    @Override
    public void render(UIContext context)
    {
        int x = this.area.x;
        int y = this.area.y;
        int ey = this.area.ey();

        /* Draw left side box */
        context.batcher.box(
            x + SIDE_MARGIN - 2, y + 6,
            x + SIDE_MARGIN + LEFT_WIDTH + 2, ey - 6,
            0xaa000000
        );

        /* Draw right side box */
        int rx = x + this.area.w - SIDE_MARGIN - RIGHT_WIDTH;
        context.batcher.box(
            rx - 2, y + 6,
            rx + RIGHT_WIDTH + 2, ey - 6,
            0xaa000000
        );

        super.render(context);
    }
}
