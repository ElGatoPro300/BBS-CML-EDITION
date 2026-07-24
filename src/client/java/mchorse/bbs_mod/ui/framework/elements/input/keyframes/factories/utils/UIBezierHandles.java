package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.utils;

import mchorse.bbs_mod.camera.utils.TimeUtils;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIConstants;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class UIBezierHandles
{
    private UITrackpad lx;
    private UITrackpad ly;
    private UITrackpad rx;
    private UITrackpad ry;

    private Keyframe<?> keyframe;
    private int index = -1;

    public UIBezierHandles(Keyframe<?> keyframe)
    {
        this(keyframe, -1);
    }

    public UIBezierHandles(Keyframe<?> keyframe, int index)
    {
        this.keyframe = keyframe;
        this.index = index;

        /* TODO 1.21.11: Keyframe.setLx/setLy/setRx/setRy removed */
        this.lx = new UITrackpad((v) -> {});
        this.ly = new UITrackpad((v) -> {});
        this.rx = new UITrackpad((v) -> {});
        this.ry = new UITrackpad((v) -> {});
        
        this.update();
    }

    public UIElement createColumn()
    {
        UIIcon leftIcon = new UIIcon(Icons.LEFT_HANDLE, null);
        leftIcon.tooltip(UIKeys.KEYFRAMES_LEFT_HANDLE);
        leftIcon.wh(UIConstants.CONTROL_HEIGHT, UIConstants.CONTROL_HEIGHT);
        UIIcon rightIcon = new UIIcon(Icons.RIGHT_HANDLE, null);
        rightIcon.tooltip(UIKeys.KEYFRAMES_RIGHT_HANDLE);
        rightIcon.wh(UIConstants.CONTROL_HEIGHT, UIConstants.CONTROL_HEIGHT);
        int rowMargin = 4;
        return UI.column(UIConstants.MARGIN,
            UI.row(rowMargin, 0, UIConstants.CONTROL_HEIGHT, leftIcon, this.lx, this.ly),
            UI.row(rowMargin, 0, UIConstants.CONTROL_HEIGHT, rightIcon, this.rx, this.ry)
        );
    }

    public void setKeyframe(Keyframe<?> keyframe)
    {
        this.keyframe = keyframe;
    }

    public void update()
    {
        /* TODO 1.21.11: Keyframe.getLx/getLy/getRx/getRy removed */
        this.lx.setValue(0F);
        this.ly.setValue(0F);
        this.rx.setValue(0F);
        this.ry.setValue(0F);
    }
}
