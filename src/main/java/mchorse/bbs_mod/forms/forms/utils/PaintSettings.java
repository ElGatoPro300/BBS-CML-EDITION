package mchorse.bbs_mod.forms.forms.utils;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.colors.Color;

import java.util.Objects;

/**
 * Paint color and intensity settings. Intensity is unbounded and may be negative
 * (negative values darken the surface). Legacy paint_color used alpha as opacity.
 */
public class PaintSettings
{
    public static final float SHADER_SHADOW_DEFAULT = 1F;
    public static final float SHADER_SHADOW_FIX_BUG = 0.005F;
    public static final float SHADER_SHADOW_FIX_BUG_THRESHOLD = 0.01F;

    public float r = 1F;
    public float g = 1F;
    public float b = 1F;
    public float intensity;
    public boolean sync = false;
    public float shaderShadow = SHADER_SHADOW_DEFAULT;

    public PaintSettings()
    {}

    public PaintSettings copy()
    {
        PaintSettings copy = new PaintSettings();

        copy.r = this.r;
        copy.g = this.g;
        copy.b = this.b;
        copy.intensity = this.intensity;
        copy.sync = this.sync;
        copy.shaderShadow = this.shaderShadow;

        return copy;
    }

    public static boolean isFixBugShaderShadow(float value)
    {
        return value <= SHADER_SHADOW_FIX_BUG_THRESHOLD;
    }

    public static float fixBugShaderShadow(boolean enabled)
    {
        return enabled ? SHADER_SHADOW_FIX_BUG : SHADER_SHADOW_DEFAULT;
    }

    public static float resolveAutoShaderShadow(float intensity)
    {
        return intensity != 0F ? SHADER_SHADOW_FIX_BUG : SHADER_SHADOW_DEFAULT;
    }

    public static float resolveAutoShaderShadowForPoseAlpha(float paintAlpha)
    {
        return paintAlpha != 0F ? SHADER_SHADOW_FIX_BUG : SHADER_SHADOW_DEFAULT;
    }

    public float effectiveShaderShadow(Color legacy)
    {
        return resolveAutoShaderShadow(this.resolveIntensity(legacy));
    }

    public void applyAutoShaderShadow()
    {
        this.shaderShadow = resolveAutoShaderShadow(this.intensity);
    }

    public void resolveColor(Color fallback, Color out)
    {
        out.set(this.r, this.g, this.b, 1F);

        if (this.intensity != 0F)
        {
            return;
        }

        if (this.r == 1F && this.g == 1F && this.b == 1F && fallback != null)
        {
            if (fallback.r != 1F || fallback.g != 1F || fallback.b != 1F)
            {
                out.set(fallback.r, fallback.g, fallback.b, 1F);
            }
        }
    }

    /**
     * Returns paint intensity, or a default when only a legacy paint_color tint is set.
     */
    public float resolveIntensity(Color legacy)
    {
        if (this.intensity != 0F)
        {
            return this.intensity;
        }

        if (legacy != null && legacy.a != 0F)
        {
            return legacy.a;
        }

        if (legacy != null && (legacy.r != 1F || legacy.g != 1F || legacy.b != 1F))
        {
            return 1F;
        }

        return 0F;
    }

    public boolean resolveSync()
    {
        return this.sync;
    }

    public void fromData(BaseType data)
    {
        if (data instanceof MapType map)
        {
            this.r = map.has("r") ? map.getFloat("r") : 1F;
            this.g = map.has("g") ? map.getFloat("g") : 1F;
            this.b = map.has("b") ? map.getFloat("b") : 1F;
            this.intensity = map.getFloat("intensity");
            this.sync = map.getBool("sync", false);
            if (map.has("shaderShadow"))
            {
                if (map.get("shaderShadow").isNumeric())
                {
                    this.shaderShadow = map.getFloat("shaderShadow", 1F);
                }
                else
                {
                    this.shaderShadow = map.getBool("shaderShadow", true) ? 1F : 0F;
                }
            }
            else
            {
                this.shaderShadow = SHADER_SHADOW_DEFAULT;
            }
        }
    }

    public BaseType toData()
    {
        MapType map = new MapType();

        map.putFloat("r", this.r);
        map.putFloat("g", this.g);
        map.putFloat("b", this.b);
        map.putFloat("intensity", this.intensity);
        map.putBool("sync", this.sync);
        map.putFloat("shaderShadow", this.shaderShadow);

        return map;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (!(o instanceof PaintSettings that))
        {
            return false;
        }

        return Float.compare(this.r, that.r) == 0
            && Float.compare(this.g, that.g) == 0
            && Float.compare(this.b, that.b) == 0
            && Float.compare(this.intensity, that.intensity) == 0
            && this.sync == that.sync
            && Float.compare(this.shaderShadow, that.shaderShadow) == 0;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.r, this.g, this.b, this.intensity, this.sync, this.shaderShadow);
    }
}
