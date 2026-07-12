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
        this.bindSecondaryFromEditor();

        this.spectrum = new UIToggle(UIKeys.GENERIC_KEYFRAMES_COLOR_SPECTRUM, (b) ->
        {
            this.keyframe.setSpectrum(b.getValue());
        });
        this.spectrum.tooltip(UIKeys.GENERIC_KEYFRAMES_COLOR_SPECTRUM_TOOLTIP);
        this.spectrum.setValue(keyframe.isSpectrum());

        this.scroll.add(this.color);
        this.scroll.add(this.spectrum);
    }

    private void bindSecondaryFromEditor()
    {
        for (UIKeyframeSheet sheet : this.editor.getGraph().getSheets())
        {
            if (sheet.channel == this.keyframe.getParent() && sheet.property != null)
            {
                Form form = FormUtils.getForm(sheet.property);

                if (form != null)
                {
                    this.color.bindSecondary(form.colorSecondary);
                }

                break;
            }
        }
    }

    @Override
    public void update()
    {
        super.update();

        this.color.setColor(this.keyframe.getValue().getARGBColor());
        this.syncSecondaryFromEditor();
        this.spectrum.setValue(this.keyframe.isSpectrum());
    }

    private void syncSecondaryFromEditor()
    {
        for (UIKeyframeSheet sheet : this.editor.getGraph().getSheets())
        {
            if (sheet.channel == this.keyframe.getParent() && sheet.property != null)
            {
                Form form = FormUtils.getForm(sheet.property);

                if (form != null)
                {
                    this.color.syncSecondary(form.colorSecondary);
                }

                break;
            }
        }
    }
}
