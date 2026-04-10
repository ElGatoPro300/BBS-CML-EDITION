package mchorse.bbs_mod.ui.triggers;

import mchorse.bbs_mod.blocks.entities.TriggerBlockEntity;
import mchorse.bbs_mod.network.ClientNetwork;
import mchorse.bbs_mod.settings.values.core.ValueList;
import mchorse.bbs_mod.triggers.Trigger;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.utils.colors.Colors;

public class UITriggerEditor extends UIElement
{
    public UIToggle collidable;
    public UIToggle region;
    public UIButton left;
    public UIButton right;
    public UIButton enter;
    public UIButton exit;
    public UIButton whileIn;
    public UITrackpad regionDelay;
    
    public UIElement hitboxLabel;
    public UIElement pos1Label;
    public UIElement pos2Label;
    public UIElement shapeLabel;
    public UIElement delayLabel;
    public UIElement offsetLabel;
    public UIElement sizeLabel;
    public UITrackpad x1, y1, z1;
    public UITrackpad x2, y2, z2;
    public UITrackpad ox, oy, oz;
    public UITrackpad sx, sy, sz;

    private TriggerBlockEntity entity;

    public UITriggerEditor()
    {
        this.collidable = new UIToggle(TriggerKeys.COLLIDABLE, false, (b) ->
        {
            if (this.entity != null)
            {
                this.entity.collidable.set(b.getValue());
                this.save();
            }
        });

        this.region = new UIToggle(TriggerKeys.REGION, false, (b) ->
        {
            if (this.entity != null)
            {
                this.entity.region.set(b.getValue());
                this.updateButtons();
                this.save();
            }
        });

        this.left = new UIButton(TriggerKeys.LEFT_CLICK, (b) -> this.openOverlay(this.entity.left));
        this.right = new UIButton(TriggerKeys.RIGHT_CLICK, (b) -> this.openOverlay(this.entity.right));
        this.enter = new UIButton(TriggerKeys.ON_ENTER, (b) -> this.openOverlay(this.entity.enter));
        this.exit = new UIButton(TriggerKeys.ON_EXIT, (b) -> this.openOverlay(this.entity.exit));
        this.whileIn = new UIButton(TriggerKeys.WHILE_IN, (b) -> this.openOverlay(this.entity.whileIn));
        this.regionDelay = new UITrackpad((v) -> { if (this.entity != null) { this.entity.regionDelay.set(v.intValue()); this.save(); } }).limit(0, 1000).integer();
        this.regionDelay.tooltip(TriggerKeys.REGION_DELAY);
        
        this.x1 = new UITrackpad((v) -> { if (this.entity != null) { this.entity.pos1.set(v.floatValue(), this.entity.pos1.get().y, this.entity.pos1.get().z); this.save(); } }).limit(0, 1).increment(0.1);
        this.y1 = new UITrackpad((v) -> { if (this.entity != null) { this.entity.pos1.set(this.entity.pos1.get().x, v.floatValue(), this.entity.pos1.get().z); this.save(); } }).limit(0, 1).increment(0.1);
        this.z1 = new UITrackpad((v) -> { if (this.entity != null) { this.entity.pos1.set(this.entity.pos1.get().x, this.entity.pos1.get().y, v.floatValue()); this.save(); } }).limit(0, 1).increment(0.1);
        
        this.x2 = new UITrackpad((v) -> { if (this.entity != null) { this.entity.pos2.set(v.floatValue(), this.entity.pos2.get().y, this.entity.pos2.get().z); this.save(); } }).limit(0, 1).increment(0.1);
        this.y2 = new UITrackpad((v) -> { if (this.entity != null) { this.entity.pos2.set(this.entity.pos2.get().x, v.floatValue(), this.entity.pos2.get().z); this.save(); } }).limit(0, 1).increment(0.1);
        this.z2 = new UITrackpad((v) -> { if (this.entity != null) { this.entity.pos2.set(this.entity.pos2.get().x, this.entity.pos2.get().y, v.floatValue()); this.save(); } }).limit(0, 1).increment(0.1);

        this.ox = new UITrackpad((v) -> { if (this.entity != null) { this.entity.regionOffset.set(v.floatValue(), this.entity.regionOffset.get().y, this.entity.regionOffset.get().z); this.save(); } }).increment(0.1);
        this.oy = new UITrackpad((v) -> { if (this.entity != null) { this.entity.regionOffset.set(this.entity.regionOffset.get().x, v.floatValue(), this.entity.regionOffset.get().z); this.save(); } }).increment(0.1);
        this.oz = new UITrackpad((v) -> { if (this.entity != null) { this.entity.regionOffset.set(this.entity.regionOffset.get().x, this.entity.regionOffset.get().y, v.floatValue()); this.save(); } }).increment(0.1);

        this.sx = new UITrackpad((v) -> { if (this.entity != null) { this.entity.regionSize.set(v.floatValue(), this.entity.regionSize.get().y, this.entity.regionSize.get().z); this.save(); } }).increment(0.1);
        this.sy = new UITrackpad((v) -> { if (this.entity != null) { this.entity.regionSize.set(this.entity.regionSize.get().x, v.floatValue(), this.entity.regionSize.get().z); this.save(); } }).increment(0.1);
        this.sz = new UITrackpad((v) -> { if (this.entity != null) { this.entity.regionSize.set(this.entity.regionSize.get().x, this.entity.regionSize.get().y, v.floatValue()); this.save(); } }).increment(0.1);

        this.x1.textbox.setColor(Colors.RED);
        this.y1.textbox.setColor(Colors.GREEN);
        this.z1.textbox.setColor(Colors.BLUE);
        this.x2.textbox.setColor(Colors.RED);
        this.y2.textbox.setColor(Colors.GREEN);
        this.z2.textbox.setColor(Colors.BLUE);
        this.ox.textbox.setColor(Colors.RED);
        this.oy.textbox.setColor(Colors.GREEN);
        this.oz.textbox.setColor(Colors.BLUE);
        this.sx.textbox.setColor(Colors.RED);
        this.sy.textbox.setColor(Colors.GREEN);
        this.sz.textbox.setColor(Colors.BLUE);

        this.hitboxLabel = UI.label(TriggerKeys.HITBOX).background(() -> 0x88000000).marginTop(10).marginBottom(2);
        this.pos1Label = UI.label(TriggerKeys.POS1);
        this.pos2Label = UI.label(TriggerKeys.POS2);
        this.shapeLabel = UI.label(TriggerKeys.SHAPE).background(() -> 0x88000000).marginTop(10).marginBottom(2);
        this.delayLabel = UI.label(TriggerKeys.REGION_DELAY);
        this.offsetLabel = UI.label(TriggerKeys.OFFSET);
        this.sizeLabel = UI.label(TriggerKeys.SIZE);

        this.add(UI.row(this.left, this.right));
        this.add(UI.row(this.enter, this.exit));
        this.add(this.whileIn);
        this.add(UI.row(this.collidable, this.region).marginTop(4));
        this.add(this.delayLabel, this.regionDelay);
        this.add(this.hitboxLabel);
        this.add(this.pos1Label, UI.row(this.x1, this.y1, this.z1));
        this.add(this.pos2Label, UI.row(this.x2, this.y2, this.z2));
        this.add(this.shapeLabel);
        this.add(this.offsetLabel, UI.row(this.ox, this.oy, this.oz));
        this.add(this.sizeLabel, UI.row(this.sx, this.sy, this.sz));
        
        this.column(5).vertical().stretch();
    }

    private void updateButtons()
    {
        boolean region = this.region.getValue();

        this.enter.setEnabled(region);
        this.exit.setEnabled(region);
        this.whileIn.setEnabled(region);
        this.delayLabel.setEnabled(region);
        this.regionDelay.setEnabled(region);
        this.shapeLabel.setEnabled(region);
        this.ox.setEnabled(region);
        this.oy.setEnabled(region);
        this.oz.setEnabled(region);
        this.sx.setEnabled(region);
        this.sy.setEnabled(region);
        this.sz.setEnabled(region);
        this.offsetLabel.setEnabled(region);
        this.sizeLabel.setEnabled(region);
    }

    public void setEntity(TriggerBlockEntity entity)
    {
        this.entity = entity;

        if (entity != null)
        {
            this.collidable.setValue(entity.collidable.get());
            this.region.setValue(entity.region.get());
            
            this.x1.setValue(entity.pos1.get().x);
            this.y1.setValue(entity.pos1.get().y);
            this.z1.setValue(entity.pos1.get().z);
            
            this.x2.setValue(entity.pos2.get().x);
            this.y2.setValue(entity.pos2.get().y);
            this.z2.setValue(entity.pos2.get().z);

            this.ox.setValue(entity.regionOffset.get().x);
            this.oy.setValue(entity.regionOffset.get().y);
            this.oz.setValue(entity.regionOffset.get().z);

            this.sx.setValue(entity.regionSize.get().x);
            this.sy.setValue(entity.regionSize.get().y);
            this.sz.setValue(entity.regionSize.get().z);
            this.regionDelay.setValue(entity.regionDelay.get());
            
            this.updateButtons();
        }
    }

    private void save()
    {
        if (this.entity != null)
        {
            ClientNetwork.sendTriggerBlockUpdate(this.entity.getBlockPos(), this.entity);
        }
    }

    private void openOverlay(ValueList<Trigger> list)
    {
        if (this.entity == null) return;

        UIOverlay.addOverlay(this.getContext(), new UITriggerOverlayPanel(list, this::save), 400, 250);
    }
}
