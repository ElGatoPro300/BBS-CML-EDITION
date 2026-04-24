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

public class UITriggerEditor extends UIElement
{
    public UIToggle collidable;
    public UIButton left;
    public UIButton right;
    
    public UITrackpad x1, y1, z1;
    public UITrackpad x2, y2, z2;

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

        this.left = new UIButton(TriggerKeys.LEFT_CLICK, (b) -> this.openOverlay(false));
        this.right = new UIButton(TriggerKeys.RIGHT_CLICK, (b) -> this.openOverlay(true));
        
        this.x1 = new UITrackpad((v) -> { if (this.entity != null) { this.entity.pos1.set(v.floatValue(), this.entity.pos1.get().y, this.entity.pos1.get().z); this.save(); } }).limit(0, 1).increment(0.1);
        this.y1 = new UITrackpad((v) -> { if (this.entity != null) { this.entity.pos1.set(this.entity.pos1.get().x, v.floatValue(), this.entity.pos1.get().z); this.save(); } }).limit(0, 1).increment(0.1);
        this.z1 = new UITrackpad((v) -> { if (this.entity != null) { this.entity.pos1.set(this.entity.pos1.get().x, this.entity.pos1.get().y, v.floatValue()); this.save(); } }).limit(0, 1).increment(0.1);
        
        this.x2 = new UITrackpad((v) -> { if (this.entity != null) { this.entity.pos2.set(v.floatValue(), this.entity.pos2.get().y, this.entity.pos2.get().z); this.save(); } }).limit(0, 1).increment(0.1);
        this.y2 = new UITrackpad((v) -> { if (this.entity != null) { this.entity.pos2.set(this.entity.pos2.get().x, v.floatValue(), this.entity.pos2.get().z); this.save(); } }).limit(0, 1).increment(0.1);
        this.z2 = new UITrackpad((v) -> { if (this.entity != null) { this.entity.pos2.set(this.entity.pos2.get().x, this.entity.pos2.get().y, v.floatValue()); this.save(); } }).limit(0, 1).increment(0.1);

        this.add(UI.row(this.left, this.right), this.collidable);
        this.add(UI.label(TriggerKeys.POS1), UI.row(this.x1, this.y1, this.z1));
        this.add(UI.label(TriggerKeys.POS2), UI.row(this.x2, this.y2, this.z2));
        
        this.column(5).vertical().stretch();
    }

    public void setEntity(TriggerBlockEntity entity)
    {
        this.entity = entity;

        if (entity != null)
        {
            this.collidable.setValue(entity.collidable.get());
            
            this.x1.setValue(entity.pos1.get().x);
            this.y1.setValue(entity.pos1.get().y);
            this.z1.setValue(entity.pos1.get().z);
            
            this.x2.setValue(entity.pos2.get().x);
            this.y2.setValue(entity.pos2.get().y);
            this.z2.setValue(entity.pos2.get().z);
        }
    }

    private void openOverlay(boolean rightClick)
    {
        if (this.entity == null) return;

        ValueList<Trigger> list = rightClick ? this.entity.right : this.entity.left;
        UIOverlay.addOverlay(this.getContext(), new UITriggerOverlayPanel(list, this::save), 400, 250);
    }

    private void save()
    {
        if (this.entity != null)
        {
            ClientNetwork.sendTriggerBlockUpdate(this.entity.getPos(), this.entity);
        }
    }
}
