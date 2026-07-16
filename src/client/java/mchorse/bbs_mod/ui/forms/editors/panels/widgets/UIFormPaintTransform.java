package mchorse.bbs_mod.ui.forms.editors.panels.widgets;

import mchorse.bbs_mod.forms.forms.utils.EffectTransform;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.ui.framework.elements.input.UIEffectKeyframeTransform;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * {@link UIEffectKeyframeTransform} wired to a form's {@link PaintSettings#transform}.
 */
public class UIFormPaintTransform extends UIEffectKeyframeTransform
{
    private final Supplier<PaintSettings> settings;

    public UIFormPaintTransform(Supplier<PaintSettings> settings, Consumer<PaintSettings> setter)
    {
        super((apply) ->
        {
            PaintSettings copy = settings.get().copy();

            if (copy.transform == null)
            {
                copy.transform = new EffectTransform();
            }

            apply.accept(copy.transform);
            setter.accept(copy);
        });

        this.settings = settings;
    }

    public void syncFromForm()
    {
        PaintSettings paint = this.settings.get();

        if (paint == null || paint.transform == null)
        {
            this.setEffectTransform(new EffectTransform());
        }
        else
        {
            this.setEffectTransform(paint.transform);
        }
    }
}
