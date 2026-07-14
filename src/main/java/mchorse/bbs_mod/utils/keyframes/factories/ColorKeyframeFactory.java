package mchorse.bbs_mod.utils.keyframes.factories;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.IntType;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

public class ColorKeyframeFactory implements IKeyframeFactory<Color>
{
    private Color i = new Color();

    @Override
    public Color fromData(BaseType data)
    {
        if (!data.isNumeric())
        {
            return new Color();
        }

        return Color.rgba(data.asNumeric().intValue());
    }

    @Override
    public BaseType toData(Color value)
    {
        return new IntType(value.getARGBColor());
    }

    @Override
    public Color createEmpty()
    {
        return new Color().set(Colors.WHITE);
    }

    @Override
    public Color copy(Color value)
    {
        return value.copy();
    }

    @Override
    public Color interpolate(Keyframe<Color> preA, Keyframe<Color> a, Keyframe<Color> b, Keyframe<Color> postB, IInterp interpolation, float x)
    {
        if (a.isSpectrum())
        {
            Colors.interpolateKeyframeColorHSV(this.i, preA.getValue(), a.getValue(), b.getValue(), postB.getValue(), interpolation, x);
        }
        else
        {
            Colors.interpolateKeyframeColorRGB(this.i, preA.getValue(), a.getValue(), b.getValue(), postB.getValue(), interpolation, x);
        }

        return this.i;
    }

    @Override
    public Color interpolate(Color preA, Color a, Color b, Color postB, IInterp interpolation, float x)
    {
        Colors.interpolateKeyframeColorRGB(this.i, preA, a, b, postB, interpolation, x);

        return this.i;
    }
}
