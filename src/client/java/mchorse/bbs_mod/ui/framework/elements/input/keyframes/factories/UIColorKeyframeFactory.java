package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditorUtils;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class UIColorKeyframeFactory extends UIKeyframeFactory<Color>
{
    private UIColor color;
    private UIToggle spectrum;
    private UIToggle noshadingOpacity;

    public UIColorKeyframeFactory(Keyframe<Color> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.color = new UIColor((c) -> this.setValue(Color.rgba(c)));
        this.color.setColor(keyframe.getValue().getARGBColor());
        this.color.withAlpha();

        this.spectrum = new UIToggle(UIKeys.GENERIC_KEYFRAMES_COLOR_SPECTRUM, (b) ->
            this.setSpectrum(b.getValue()));
        this.spectrum.tooltip(UIKeys.GENERIC_KEYFRAMES_COLOR_SPECTRUM_TOOLTIP);
        this.spectrum.setValue(keyframe.isSpectrum());

        this.noshadingOpacity = new UIToggle(UIKeys.FORMS_EDITORS_COLOR_NOSHADING_OPACITY, (b) ->
            this.setNoshadingOpacity(b.getValue()));
        this.noshadingOpacity.tooltip(UIKeys.FORMS_EDITORS_COLOR_NOSHADING_OPACITY_TOOLTIP);
        this.noshadingOpacity.setValue(keyframe.isNoshadingOpacity());

        this.scroll.add(this.color);
        this.scroll.add(this.spectrum);
        this.scroll.add(this.noshadingOpacity);
    }

    @Override
    public void update()
    {
        super.update();

        this.color.setColor(this.keyframe.getValue().getARGBColor());
        this.spectrum.setValue(this.keyframe.isSpectrum());
        this.noshadingOpacity.setValue(this.keyframe.isNoshadingOpacity());
    }

    private void setSpectrum(boolean value)
    {
        boolean[] applied = {false};

        UIReplaysEditorUtils.forEachSelectedKeyframe(this.editor, this.keyframe, (selected) ->
        {
            applied[0] = true;
            selected.setSpectrum(value);
        });

        if (!applied[0])
        {
            this.keyframe.setSpectrum(value);
        }
    }

    private void setNoshadingOpacity(boolean value)
    {
        boolean[] applied = {false};

        UIReplaysEditorUtils.forEachSelectedKeyframe(this.editor, this.keyframe, (selected) ->
        {
            applied[0] = true;
            selected.setNoshadingOpacity(value);
        });

        if (!applied[0])
        {
            this.keyframe.setNoshadingOpacity(value);
        }
    }
}
