package mchorse.bbs_mod.forms.forms.utils;

import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.utils.colors.Color;

/**
 * Resolves wrap/overlay paint from form-level and per-limb settings.
 */
public final class FormWrapPaint
{
    private FormWrapPaint()
    {}

    public static boolean hasFormWrap(Form form)
    {
        return form != null && form.wrap_opacity.get() > 0.001F;
    }

    public static boolean hasGroupWrap(ModelGroup group)
    {
        return group != null && group.colorWrapOpacity > 0.001F;
    }

    public static boolean hasAnyWrap(Form form, Model model)
    {
        if (FormWrapPaint.hasFormWrap(form))
        {
            return true;
        }

        if (model == null)
        {
            return false;
        }

        for (ModelGroup group : model.getAllGroups())
        {
            if (FormWrapPaint.hasGroupWrap(group))
            {
                return true;
            }
        }

        return false;
    }

    public static void resolveFormWrap(Color out, Form form)
    {
        out.copy(form.wrap_color.get());
        out.a = form.wrap_opacity.get();
    }

    public static void resolveGroupWrap(Color out, ModelGroup group)
    {
        out.copy(group.colorWrap);
        out.a = group.colorWrapOpacity;
    }

    /**
     * Applies form wrap paint when no explicit paint intensity is set.
     */
    public static void mergeFormWrapIntoPaint(Color paintOut, Form form, PaintSettings paint, Color legacyPaint)
    {
        float paintStrength = paint.resolveIntensity(legacyPaint);

        paint.resolveColor(legacyPaint, paintOut);
        paintOut.a = paintStrength;

        if (paintStrength > 0.001F)
        {
            return;
        }

        if (!FormWrapPaint.hasFormWrap(form))
        {
            return;
        }

        FormWrapPaint.resolveFormWrap(paintOut, form);
    }

    public static void mergeWrapIntoResolvedPaint(Color paintOut, Form form, PaintSettings paint, Color legacyPaint)
    {
        FormWrapPaint.mergeFormWrapIntoPaint(paintOut, form, paint, legacyPaint);
    }

    public static void mergeWrapIntoConsumerPaint(Color paintOut, Form form)
    {
        paintOut.a = 0F;

        if (!FormWrapPaint.hasFormWrap(form))
        {
            return;
        }

        FormWrapPaint.resolveFormWrap(paintOut, form);
    }
}
