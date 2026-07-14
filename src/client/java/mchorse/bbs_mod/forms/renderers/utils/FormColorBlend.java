package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Color;

public class FormColorBlend
{
    public static final float EMISSION_STRENGTH = 8F;

    public enum BlendMode
    {
        MULTIPLY,
        BRIGHTEN
    }

    public static void blend(Color base, Color overlay, boolean additive)
    {
        blend(base, overlay, additive ? BlendMode.BRIGHTEN : BlendMode.MULTIPLY);
    }

    public static void blendFormGlowBrighten(Color base, GlowSettings glow, Color fallback)
    {
        if (base == null || glow == null)
        {
            return;
        }

        float intensity = glow.resolveIntensity(fallback);

        if (intensity == 0F)
        {
            return;
        }

        Color resolved = new Color();

        glow.resolveColor(fallback, resolved);
        blendEmission(base, resolved, intensity);
    }

    public static void blendBrighten(Color base, Color glowColor, float intensity)
    {
        blendEmission(base, glowColor, intensity);
    }

    public static void blendEmission(Color base, Color glowColor, float intensity)
    {
        if (base == null || glowColor == null || intensity == 0F)
        {
            return;
        }

        float r = MathUtils.clamp(glowColor.r, 0F, 1F);
        float g = MathUtils.clamp(glowColor.g, 0F, 1F);
        float b = MathUtils.clamp(glowColor.b, 0F, 1F);

        if (intensity > 0F)
        {
            base.r += r * intensity * EMISSION_STRENGTH;
            base.g += g * intensity * EMISSION_STRENGTH;
            base.b += b * intensity * EMISSION_STRENGTH;
        }
        else
        {
            /* Linear darken: 0 = unchanged, -1 = fully black (smooth for keyframe animation). */
            float factor = Math.max(0F, 1F + intensity);

            base.r *= factor;
            base.g *= factor;
            base.b *= factor;
        }
    }

    public static void blend(Color base, Color overlay, BlendMode mode)
    {
        if (base == null || overlay == null)
        {
            return;
        }

        float a = MathUtils.clamp(overlay.a, 0F, 1F);
        float r = MathUtils.clamp(overlay.r, 0F, 1F);
        float g = MathUtils.clamp(overlay.g, 0F, 1F);
        float b = MathUtils.clamp(overlay.b, 0F, 1F);

        if (mode == BlendMode.BRIGHTEN)
        {
            blendBrighten(base, overlay, a);
        }
        else
        {
            base.r *= r;
            base.g *= g;
            base.b *= b;
            base.a *= a;
        }
    }
}
