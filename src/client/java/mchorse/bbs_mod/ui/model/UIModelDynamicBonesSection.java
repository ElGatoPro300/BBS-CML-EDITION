package mchorse.bbs_mod.ui.model;

import mchorse.bbs_mod.cubic.model.ModelConfig;
import mchorse.bbs_mod.cubic.model.PhysBoneSlot;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;

import java.util.ArrayList;
import java.util.List;

public class UIModelDynamicBonesSection extends UIModelSection
{
    private final UIStringList list;
    private final UIIcon add;
    private final UIIcon remove;
    private final UIButton assignBone;
    private final UIToggle enabled;
    private final UIToggle pitch;
    private final UITrackpad stiffness;
    private final UITrackpad damping;
    private final UITrackpad gravity;
    private final UITrackpad inertia;
    private final UITrackpad simSpeed;
    private final UITrackpad maxAngle;

    private String selectedBone;
    private CopiedSettings copiedSettings;

    public UIModelDynamicBonesSection(UIModelPanel editor)
    {
        super(editor);

        this.title.label = UIKeys.MODELS_DYNAMIC_BONES;
        this.list = new UIStringList(this::onListPicked);
        this.list.background();
        this.list.context((menu) ->
        {
            UIContext context = this.getContext();
            int index = this.list.getIndex();

            if (context != null)
            {
                int hovered = this.list.getHoveredIndex(context);

                if (hovered >= 0)
                {
                    index = hovered;
                }
            }

            PhysBoneSlot target = this.getSlotAt(index);

            if (target == null)
            {
                return;
            }

            this.pick(index);
            menu.action(Icons.COPY, UIKeys.MODELS_DYNAMIC_BONES_COPY_SETTINGS, () -> this.copySettings(target));

            if (this.copiedSettings != null)
            {
                menu.action(Icons.PASTE, UIKeys.MODELS_DYNAMIC_BONES_PASTE_SETTINGS, () -> this.pasteSettings(target));
            }
        });
        this.add = new UIIcon(Icons.ADD, (b) -> this.addSlot());
        this.remove = new UIIcon(Icons.REMOVE, (b) -> this.removeSlot());
        this.assignBone = new UIButton(UIKeys.MODELS_DYNAMIC_BONES_ASSIGN_SELECTED_BONE, (b) -> this.assignSelectedBone());
        this.enabled = new UIToggle(UIKeys.MODELS_DYNAMIC_BONES_ENABLED, (b) ->
        {
            PhysBoneSlot slot = this.getCurrentSlot();

            if (slot != null)
            {
                slot.enabled.set(b.getValue());
                this.markDirty();
            }
        });
        this.pitch = new UIToggle(UIKeys.MODELS_DYNAMIC_BONES_AFFECT_PITCH, (b) ->
        {
            PhysBoneSlot slot = this.getCurrentSlot();

            if (slot != null)
            {
                slot.pitch.set(b.getValue());
                this.markDirty();
            }
        });
        this.stiffness = new UITrackpad((v) -> this.editFloat(v.floatValue(), this::setStiffness)).limit(0, 50);
        this.damping = new UITrackpad((v) -> this.editFloat(v.floatValue(), this::setDamping)).limit(0, 50);
        this.gravity = new UITrackpad((v) -> this.editFloat(v.floatValue(), this::setGravity)).limit(-10, 10);
        this.inertia = new UITrackpad((v) -> this.editFloat(v.floatValue(), this::setInertia)).limit(0, 3);
        this.simSpeed = new UITrackpad((v) -> this.editFloat(v.floatValue(), this::setSimSpeed)).limit(0.01, 5);
        this.maxAngle = new UITrackpad((v) -> this.editFloat(v.floatValue(), this::setMaxAngle)).limit(0, 180);
        this.add.tooltip(UIKeys.MODELS_DYNAMIC_BONES_ADD_TOOLTIP);
        this.remove.tooltip(UIKeys.MODELS_DYNAMIC_BONES_REMOVE_TOOLTIP);
        this.assignBone.tooltip(UIKeys.MODELS_DYNAMIC_BONES_ASSIGN_SELECTED_BONE_TOOLTIP);
        this.enabled.tooltip(UIKeys.MODELS_DYNAMIC_BONES_ENABLED_TOOLTIP);
        this.pitch.tooltip(UIKeys.MODELS_DYNAMIC_BONES_AFFECT_PITCH_TOOLTIP);
        this.stiffness.tooltip(UIKeys.MODELS_DYNAMIC_BONES_STIFFNESS_TOOLTIP);
        this.damping.tooltip(UIKeys.MODELS_DYNAMIC_BONES_DAMPING_TOOLTIP);
        this.gravity.tooltip(UIKeys.MODELS_DYNAMIC_BONES_GRAVITY_TOOLTIP);
        this.inertia.tooltip(UIKeys.MODELS_DYNAMIC_BONES_INERTIA_TOOLTIP);
        this.simSpeed.tooltip(UIKeys.MODELS_DYNAMIC_BONES_SIM_SPEED_TOOLTIP);
        this.maxAngle.tooltip(UIKeys.MODELS_DYNAMIC_BONES_MAX_ANGLE_TOOLTIP);

        this.stiffness.values(0.1, 0.05, 0.25);
        this.damping.values(0.1, 0.05, 0.25);
        this.gravity.values(0.1, 0.05, 0.25);
        this.inertia.values(0.1, 0.05, 0.25);
        this.simSpeed.values(0.1, 0.05, 0.25);
        this.maxAngle.values(1, 0.5, 2);

        UIElement controls = UI.row(this.add, this.remove);

        this.list.h(160);
        this.fields.add(controls);
        this.fields.add(this.list);
        this.fields.add(this.assignBone);
        this.fields.add(this.enabled);
        this.fields.add(this.pitch);
        this.fields.add(UI.label(UIKeys.MODELS_DYNAMIC_BONES_STIFFNESS), this.stiffness);
        this.fields.add(UI.label(UIKeys.MODELS_DYNAMIC_BONES_DAMPING), this.damping);
        this.fields.add(UI.label(UIKeys.MODELS_DYNAMIC_BONES_GRAVITY), this.gravity);
        this.fields.add(UI.label(UIKeys.MODELS_DYNAMIC_BONES_INERTIA), this.inertia);
        this.fields.add(UI.label(UIKeys.MODELS_DYNAMIC_BONES_SIM_SPEED), this.simSpeed);
        this.fields.add(UI.label(UIKeys.MODELS_DYNAMIC_BONES_MAX_ANGLE), this.maxAngle);

        this.pick(-1);
    }

    private void onListPicked(List<String> values)
    {
        if (values.isEmpty())
        {
            this.pick(-1);
            return;
        }

        this.pick(this.list.getIndex());
    }

    @Override
    public IKey getTitle()
    {
        return UIKeys.MODELS_DYNAMIC_BONES;
    }

    @Override
    public void onBoneSelected(String bone)
    {
        this.selectedBone = bone;
    }

    @Override
    public void setConfig(ModelConfig config)
    {
        super.setConfig(config);
        this.refreshList();
    }

    private void addSlot()
    {
        if (this.config == null)
        {
            return;
        }

        PhysBoneSlot slot = new PhysBoneSlot(String.valueOf(this.config.physBones.getAllTyped().size()));

        if (this.selectedBone != null)
        {
            slot.bone.set(this.selectedBone);
        }

        this.config.physBones.add(slot);
        this.config.physBones.sync();
        this.markDirty();
        this.refreshList();
        this.pick(this.config.physBones.getAllTyped().size() - 1);
    }

    private void removeSlot()
    {
        if (this.config == null)
        {
            return;
        }

        int index = this.list.getIndex();

        if (index < 0 || index >= this.config.physBones.getAllTyped().size())
        {
            return;
        }

        this.config.physBones.remove(index);
        this.config.physBones.sync();
        this.markDirty();
        this.refreshList();
        this.pick(Math.min(index, this.config.physBones.getAllTyped().size() - 1));
    }

    private void assignSelectedBone()
    {
        PhysBoneSlot slot = this.getCurrentSlot();

        if (slot == null || this.selectedBone == null)
        {
            return;
        }

        slot.bone.set(this.selectedBone);
        this.markDirty();
        this.refreshList();
    }

    private void editFloat(float value, FloatEditor editor)
    {
        PhysBoneSlot slot = this.getCurrentSlot();

        if (slot == null)
        {
            return;
        }

        editor.apply(slot, value);
        this.markDirty();
    }

    private void setStiffness(PhysBoneSlot slot, float value)
    {
        slot.stiffness.set(value);
    }

    private void setDamping(PhysBoneSlot slot, float value)
    {
        slot.damping.set(value);
    }

    private void setGravity(PhysBoneSlot slot, float value)
    {
        slot.gravity.set(value);
    }

    private void setInertia(PhysBoneSlot slot, float value)
    {
        slot.inertia.set(value);
    }

    private void setSimSpeed(PhysBoneSlot slot, float value)
    {
        slot.simSpeed.set(value);
    }

    private void setMaxAngle(PhysBoneSlot slot, float value)
    {
        slot.maxAngle.set(value);
    }

    private void copySettings(PhysBoneSlot slot)
    {
        this.copiedSettings = new CopiedSettings(
            slot.enabled.get(),
            slot.pitch.get(),
            slot.stiffness.get(),
            slot.damping.get(),
            slot.gravity.get(),
            slot.inertia.get(),
            slot.simSpeed.get(),
            slot.maxAngle.get()
        );
    }

    private void pasteSettings(PhysBoneSlot slot)
    {
        if (this.copiedSettings == null)
        {
            return;
        }

        slot.enabled.set(this.copiedSettings.enabled);
        slot.pitch.set(this.copiedSettings.pitch);
        slot.stiffness.set(this.copiedSettings.stiffness);
        slot.damping.set(this.copiedSettings.damping);
        slot.gravity.set(this.copiedSettings.gravity);
        slot.inertia.set(this.copiedSettings.inertia);
        slot.simSpeed.set(this.copiedSettings.simSpeed);
        slot.maxAngle.set(this.copiedSettings.maxAngle);
        this.config.physBones.sync();
        this.markDirty();
        this.applySlot(slot);
    }

    private void refreshList()
    {
        int previous = this.list.getIndex();
        List<String> labels = new ArrayList<>();

        if (this.config != null)
        {
            for (PhysBoneSlot slot : this.config.physBones.getAllTyped())
            {
                String bone = slot.bone.get();

                labels.add(bone.isEmpty() ? UIKeys.GENERAL_NONE.get() : bone);
            }
        }

        this.list.setList(labels);
        this.list.setIndex(Math.min(previous, labels.size() - 1));
        this.pick(this.list.getIndex());
    }

    private void pick(int index)
    {
        if (this.config == null || index < 0 || index >= this.config.physBones.getAllTyped().size())
        {
            this.list.setIndex(-1);
            this.applySlot(null);

            return;
        }

        this.list.setIndex(index);
        this.applySlot(this.config.physBones.getAllTyped().get(index));
    }

    private void applySlot(PhysBoneSlot slot)
    {
        boolean hasSlot = slot != null;

        this.remove.setEnabled(hasSlot);
        this.assignBone.setEnabled(hasSlot && this.selectedBone != null);
        this.enabled.setEnabled(hasSlot);
        this.pitch.setEnabled(hasSlot);
        this.stiffness.setEnabled(hasSlot);
        this.damping.setEnabled(hasSlot);
        this.gravity.setEnabled(hasSlot);
        this.inertia.setEnabled(hasSlot);
        this.simSpeed.setEnabled(hasSlot);
        this.maxAngle.setEnabled(hasSlot);

        if (!hasSlot)
        {
            this.enabled.setValue(false);
            this.pitch.setValue(false);
            this.stiffness.setValue(0);
            this.damping.setValue(0);
            this.gravity.setValue(0);
            this.inertia.setValue(0);
            this.simSpeed.setValue(0);
            this.maxAngle.setValue(0);

            return;
        }

        this.enabled.setValue(slot.enabled.get());
        this.pitch.setValue(slot.pitch.get());
        this.stiffness.setValue(slot.stiffness.get());
        this.damping.setValue(slot.damping.get());
        this.gravity.setValue(slot.gravity.get());
        this.inertia.setValue(slot.inertia.get());
        this.simSpeed.setValue(slot.simSpeed.get());
        this.maxAngle.setValue(slot.maxAngle.get());
    }

    private PhysBoneSlot getCurrentSlot()
    {
        if (this.config == null)
        {
            return null;
        }

        int index = this.list.getIndex();

        if (index < 0 || index >= this.config.physBones.getAllTyped().size())
        {
            return null;
        }

        return this.config.physBones.getAllTyped().get(index);
    }

    private PhysBoneSlot getSlotAt(int index)
    {
        if (this.config == null || index < 0 || index >= this.config.physBones.getAllTyped().size())
        {
            return null;
        }

        return this.config.physBones.getAllTyped().get(index);
    }

    private void markDirty()
    {
        this.editor.dirty();
        this.editor.renderer.dirty();
    }

    private interface FloatEditor
    {
        void apply(PhysBoneSlot slot, float value);
    }

    private record CopiedSettings(
        boolean enabled,
        boolean pitch,
        float stiffness,
        float damping,
        float gravity,
        float inertia,
        float simSpeed,
        float maxAngle
    ) {}
}
