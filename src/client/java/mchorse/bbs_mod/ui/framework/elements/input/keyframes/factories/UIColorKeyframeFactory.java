package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.PaintMaskShape;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditorUtils;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcons;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UIEffectKeyframeTransform;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

import java.util.function.Consumer;

public class UIColorKeyframeFactory extends UIKeyframeFactory<Color>
{
    private UIIcons shapeIcons;
    private UIEffectKeyframeTransform transform;
    private UIColor color;
    private UIToggle spectrum;
    private UIToggle noshadingOpacity;

    public UIColorKeyframeFactory(Keyframe<Color> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.shapeIcons = new UIIcons((b) -> this.applyColorEdit((color) ->
            color.transform.shape = PaintMaskShape.fromId(b.getValue())));
        this.shapeIcons.add(Icons.SQUARE, UIKeys.FORMS_EDITORS_PAINT_SHAPE_BOX);
        this.shapeIcons.add(Icons.CIRCLE, UIKeys.FORMS_EDITORS_PAINT_SHAPE_CIRCLE);
        this.shapeIcons.add(Icons.TRIANGLE, UIKeys.FORMS_EDITORS_PAINT_SHAPE_TRIANGLE);
        this.shapeIcons.h(20);

        this.transform = new UIEffectKeyframeTransform((apply) -> this.applyColorEdit((color) -> apply.accept(color.transform)));
        this.transform.registerUndo(editor);

        this.color = new UIColor((c) -> this.applyColorEdit((color) ->
        {
            Color value = Color.rgba(c);

            color.set(value.r, value.g, value.b, value.a);
        }));
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

        this.scroll.add(UI.label(UIKeys.FORMS_EDITORS_PAINT_SHAPE), this.shapeIcons);
        this.scroll.add(this.transform);
        this.scroll.add(this.color.marginTop(8));
        this.scroll.add(this.spectrum);
        this.scroll.add(this.noshadingOpacity);

        this.update();
    }

    @Override
    public void update()
    {
        super.update();

        Color value = this.getOrCreateColor(this.keyframe.getValue());
        EffectTransform effect = value.transform;

        this.shapeIcons.setValue(effect.shape == null ? 0 : effect.shape.id);
        this.transform.setEffectTransform(effect);
        this.color.setColor(value.getARGBColor());
        this.spectrum.setValue(this.keyframe.isSpectrum());
        this.noshadingOpacity.setValue(this.keyframe.isNoshadingOpacity());
    }

    private Color getOrCreateColor(Color color)
    {
        if (color == null)
        {
            color = Color.white();
        }

        if (color.transform == null)
        {
            color.transform = new EffectTransform();
        }

        return color;
    }

    private void applyColorEdit(Consumer<Color> editor)
    {
        boolean[] applied = {false};

        UIReplaysEditorUtils.forEachSelectedKeyframe(this.editor, this.keyframe, (selected) ->
        {
            applied[0] = true;

            Color color = this.getOrCreateColor((Color) selected.getValue());

            selected.preNotify();
            editor.accept(color);
            selected.postNotify();
        });

        if (!applied[0])
        {
            Color color = this.getOrCreateColor(this.keyframe.getValue());

            this.keyframe.preNotify();
            editor.accept(color);
            this.keyframe.postNotify();
        }
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
