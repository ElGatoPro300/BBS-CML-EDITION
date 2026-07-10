package mchorse.bbs_mod.ui.film.controller;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.context.UIContextMenu;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UI;

/**
 * Small pop-up menu that exposes the viewport gizmo size (backed by {@link BBSSettings#axesScale}).
 *
 * The size is edited through a regular trackpad, so dragging changes the value; holding Shift while
 * dragging uses the "strong" step (5) and the +/- arrows step by 1. The value is persisted in the
 * config the moment it changes because {@link BBSSettings#axesScale} is a saved setting.
 */
public class UIGizmoSizeContextMenu extends UIContextMenu
{
    public UITrackpad size;

    private UIElement column;

    public UIGizmoSizeContextMenu()
    {
        this.size = new UITrackpad((v) -> BBSSettings.axesScale.set(v.floatValue()));
        this.size.limit(BBSSettings.axesScale).increment(1D).values(1D, 0.2D, 5D);
        this.size.setValue(BBSSettings.axesScale.get());

        this.column = UI.column(5, 10,
            UI.label(UIKeys.FILM_GIZMO_SIZE),
            this.size
        );
        this.column.relative(this).w(140);

        this.add(this.column);
        this.column.resize();
    }

    @Override
    public boolean isEmpty()
    {
        return false;
    }

    @Override
    public void setMouse(UIContext context)
    {
        this.xy(context.mouseX(), context.mouseY())
            .wh(this.column.area.w, this.column.area.h)
            .bounds(context.menu.overlay, 5);
    }
}
