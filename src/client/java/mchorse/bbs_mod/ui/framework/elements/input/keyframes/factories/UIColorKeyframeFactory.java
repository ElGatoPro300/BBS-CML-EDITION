package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class UIColorKeyframeFactory extends UIKeyframeFactory<Color>
{
    private UIColor color;
    private UIToggle spectrum;

    public UIColorKeyframeFactory(Keyframe<Color> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.color = new UIColor((c) -> this.setValue(Color.rgba(c)));
        this.color.setColor(keyframe.getValue().getARGBColor());
        this.color.withAlpha();
        this.bindWrapFromEditor();

        this.spectrum = new UIToggle(UIKeys.GENERIC_KEYFRAMES_COLOR_SPECTRUM, (b) ->
        {
            this.keyframe.setSpectrum(b.getValue());
        });
        this.spectrum.tooltip(UIKeys.GENERIC_KEYFRAMES_COLOR_SPECTRUM_TOOLTIP);
        this.spectrum.setValue(keyframe.isSpectrum());

        this.scroll.add(this.color);
        this.scroll.add(this.spectrum);
    }

    private void bindWrapFromEditor()
    {
        UIKeyframeSheet sheet = this.editor.getGraph().getSheet(this.keyframe);

        if (sheet == null || sheet.property == null)
        {
            return;
        }

        Form form = FormUtils.getForm(sheet.property);

        if (form == null)
        {
            return;
        }

        this.color.bindWrap(form.wrap_color, form.wrap_opacity);
        this.color.syncWrap(form.wrap_color.get().getRGBColor(), form.wrap_opacity.get());
    }

    @Override
    public void update()
    {
        super.update();

        this.color.setColor(this.keyframe.getValue().getARGBColor());
        this.bindWrapFromEditor();
        this.spectrum.setValue(this.keyframe.isSpectrum());
    }
}
