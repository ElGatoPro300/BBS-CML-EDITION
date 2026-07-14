package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditorUtils;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

import java.util.function.Consumer;

public class UIPaintSettingsKeyframeFactory extends UIKeyframeFactory<PaintSettings>
{
    private UIColor paintColor;
    private UITrackpad intensity;
    private UIToggle spectrum;

    public UIPaintSettingsKeyframeFactory(Keyframe<PaintSettings> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.paintColor = new UIColor((c) -> this.setColor(c));
        this.paintColor.tooltip(UIKeys.FORMS_EDITORS_PAINT_COLOR);

        this.intensity = new UITrackpad((value) -> this.setIntensity(value.floatValue()));
        this.intensity.increment(0.05D).values(0.1D, 0.05D, 0.2D);
        this.intensity.tooltip(UIKeys.FORMS_EDITORS_PAINT_INTENSITY);

        this.spectrum = new UIToggle(UIKeys.GENERIC_KEYFRAMES_COLOR_SPECTRUM, (b) -> this.setSpectrum(b.getValue()));
        this.spectrum.tooltip(UIKeys.GENERIC_KEYFRAMES_COLOR_SPECTRUM_TOOLTIP);
        this.spectrum.setValue(keyframe.isSpectrum());

        this.scroll.add(UI.row(UI.label(UIKeys.FORMS_EDITORS_PAINT_COLOR), this.paintColor));
        this.scroll.add(UI.label(UIKeys.FORMS_EDITORS_PAINT_INTENSITY), this.intensity);
        this.scroll.add(this.spectrum);

        this.update();
    }

    @Override
    public void update()
    {
        super.update();

        PaintSettings value = this.getOrCreateSettings(this.keyframe.getValue());

        this.paintColor.setColor(new Color().set(value.r, value.g, value.b, 1F).getRGBColor());
        this.intensity.setValue(value.intensity);
        this.spectrum.setValue(this.keyframe.isSpectrum());
    }

    private PaintSettings getOrCreateSettings(PaintSettings settings)
    {
        if (settings == null)
        {
            return new PaintSettings();
        }

        return settings;
    }

    private void applyToSelected(Consumer<PaintSettings> consumer)
    {
        boolean[] applied = {false};

        UIReplaysEditorUtils.forEachSelectedKeyframe(this.editor, this.keyframe, (selected) ->
        {
            applied[0] = true;

            PaintSettings settings = this.getOrCreateSettings((PaintSettings) selected.getValue()).copy();

            consumer.accept(settings);
            selected.setValue(settings, true);
        });

        if (!applied[0])
        {
            PaintSettings settings = this.getOrCreateSettings(this.keyframe.getValue()).copy();

            consumer.accept(settings);
            this.keyframe.setValue(settings, true);
        }
    }

    private void setColor(int c)
    {
        Color color = new Color().set(c);

        this.applyToSelected((settings) ->
        {
            settings.r = color.r;
            settings.g = color.g;
            settings.b = color.b;
            settings.applyAutoShaderShadow();
        });
    }

    private void setIntensity(float value)
    {
        this.applyToSelected((settings) ->
        {
            settings.intensity = value;
            settings.applyAutoShaderShadow();
        });
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
}
