package mchorse.bbs_mod.utils.colors;

import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.interps.Lerps;

public class Colors
{
    public static final int RGB = 0xffffff;

    /* Alpha */
    public static final int A100 = 0xff000000;
    public static final int A90 = 0xee000000;
    public static final int A75 = 0xbb000000;
    public static final int A50 = 0x88000000;
    public static final int A25 = 0x44000000;
    public static final int A12 = 0x22000000;
    public static final int A6 = 0x11000000;

    public static final int WHITE = 0xffffffff;
    public static final int LIGHTEST_GRAY = 0xffcccccc;
    public static final int LIGHTER_GRAY = 0xffaaaaaa;
    public static final int GRAY = 0xff888888;
    public static final int DARKER_GRAY = 0xff444444;
    public static final int DARKEST_GRAY = 0xff222222;
    public static final int RED = 0xff3333;
    public static final int GREEN = 0x33ff33;
    public static final int BLUE = 0x3366ff;
    public static final int YELLOW = 0xffff33;
    public static final int CYAN = 0x33ffff;
    public static final int MAGENTA = 0xff66ff;
    public static final int DEEP_PINK = 0xff1493;
    public static final int PINK = 0xff9da1;
    public static final int ORANGE = 0xff8822;

    /* General purpose colors */
    public static final int CONTROL_BAR = 0xff141417;
    public static final int ACTIVE = 0x0088ff;
    public static final int POSITIVE = GREEN;
    public static final int NEGATIVE = RED;
    public static final int INACTIVE = 0xffbb00;
    public static final int HIGHLIGHT = 0xddddff;
    public static final int CURSOR = 0xff57f52a;

    public static final Color COLOR = new Color();

    public static int mulRGB(int color, float factor)
    {
        COLOR.set(color);
        COLOR.r *= factor;
        COLOR.g *= factor;
        COLOR.b *= factor;

        return COLOR.getARGBColor();
    }

    public static float getA(int color)
    {
        COLOR.set(color);

        return COLOR.a;
    }

    public static int setA(int color, float alpha)
    {
        COLOR.set(color);
        COLOR.a = alpha;

        return COLOR.getARGBColor();
    }

    public static int mulA(int color, float factor)
    {
        COLOR.set(color);
        COLOR.a *= factor;

        return COLOR.getARGBColor();
    }

    public static int a(float alpha)
    {
        return setA(0, alpha);
    }

    public static void interpolate(Color target, int a, int b, float x)
    {
        interpolate(target, a, b, x, true);
    }

    public static void interpolate(Color target, int a, int b, float x, boolean alpha)
    {
        target.set(a, alpha);
        COLOR.set(b, alpha);

        target.r = Lerps.lerp(target.r, COLOR.r, x);
        target.g = Lerps.lerp(target.g, COLOR.g, x);
        target.b = Lerps.lerp(target.b, COLOR.b, x);

        if (alpha)
        {
            target.a = Lerps.lerp(target.a, COLOR.a, x);
        }
    }

    public static int parse(String color)
    {
        return parse(color, 0);
    }

    public static int parse(String color, int orDefault)
    {
        try
        {
            return parseWithException(color);
        }
        catch (Exception e)
        {}

        return orDefault;
    }

    public static int parseWithException(String color) throws Exception
    {
        if (color.startsWith("#"))
        {
            color = color.substring(1);
        }

        if (color.length() == 6 || color.length() == 8)
        {
            return StringUtils.parseHex(color);
        }

        throw new Exception("Given color \"" + color + "\" can't be parsed!");
    }

    public static Color HSVtoRGB(float h, float s, float v)
    {
        return HSVtoRGB(new Color(), h, s, v);
    }

    /**
     * Convert HSV to RGB. All input values are expected to be 0..1.
     *
     * @link https://www.rapidtables.com/convert/color/hsv-to-rgb.html
     */
    public static Color HSVtoRGB(Color color, float h, float s, float v)
    {
        h *= 360;
        h %= 360;

        float c = v * s;
        float x = c * (1 - Math.abs((h / 60F) % 2 - 1));
        float m = v - c;

        if (h >= 0 && h < 60)
        {
            color.set(c, x, 0);
        }
        else if (h >= 60 && h < 120)
        {
            color.set(x, c, 0);
        }
        else if (h >= 120 && h < 180)
        {
            color.set(0, c, x);
        }
        else if (h >= 180 && h < 240)
        {
            color.set(0, x, c);
        }
        else if (h >= 240 && h < 300)
        {
            color.set(x, 0, c);
        }
        else
        {
            color.set(c, 0, x);
        }

        color.r += m;
        color.g += m;
        color.b += m;

        return color;
    }

    public static Color RGBtoHSV(float r, float g, float b)
    {
        return RGBtoHSV(new Color(), r, g, b);
    }

    /**
     * Convert RGB to HSV. All input values are expected to be 0..1.
     * The given color will be populated with HSV to red, green and blue
     * respectively in 0..1 value range.
     *
     * @link https://www.rapidtables.com/convert/color/rgb-to-hsv.html
     */
    public static Color RGBtoHSV(Color color, float r, float g, float b)
    {
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        /* Hue */
        if (delta == 0)
        {
            color.r = 0;
        }
        else if (max == r)
        {
            color.r = 60F * (((g - b) / delta) % 6F);
        }
        else if (max == g)
        {
            color.r = 60F * (((b - r) / delta) + 2F);
        }
        else if (max == b)
        {
            color.r = 60F * (((r - g) / delta) + 4F);
        }

        color.r /= 360F;

        if (color.r < 0)
        {
            color.r += 1F;
        }

        /* Saturation */
        color.g = max == 0 ? 0 : delta / max;

        /* Value */
        color.b = max;

        return color;
    }

    private static final Color HSV_SCRATCH_A = new Color();
    private static final Color HSV_SCRATCH_B = new Color();
    private static final Color HSV_SCRATCH_C = new Color();
    private static final Color HSV_SCRATCH_D = new Color();
    private static final Color HSV_RGB_OUT = new Color();

    /* Below this saturation the picker treats the color as achromatic (no hue bleed). */
    private static final float PICKER_SATURATION_SNAP = 0.004F;

    /**
     * Returns the hue endpoint for long-path interpolation around the spectrum
     * (red → orange → yellow → green → cyan → blue → magenta → red).
     */
    public static float longPathHueTarget(float hueFrom, float hueTo)
    {
        float delta = hueTo - hueFrom;

        if (delta > 0.5F)
        {
            delta -= 1F;
        }
        else if (delta < -0.5F)
        {
            delta += 1F;
        }

        float longDelta;

        if (delta > 0F)
        {
            longDelta = delta - 1F;
        }
        else if (delta < 0F)
        {
            longDelta = delta + 1F;
        }
        else
        {
            longDelta = 1F;
        }

        return hueFrom + longDelta;
    }

    private static float wrapHue(float hue)
    {
        hue %= 1F;

        if (hue < 0F)
        {
            hue += 1F;
        }

        return hue;
    }

    /**
     * Smoother easing for spectrum hue travel (smootherstep).
     */
    public static float smootherstep(float x)
    {
        x = MathUtils.clamp(x, 0F, 1F);

        return x * x * x * (x * (x * 6F - 15F) + 10F);
    }

    /**
     * Default RGB interpolation between color keyframes.
     */
    public static Color interpolateKeyframeColorRGB(Color out, Color preA, Color a, Color b, Color postB, IInterp interpolation, float x)
    {
        out.r = MathUtils.clamp((float) interpolation.interpolate(IInterp.context.set(preA.r, a.r, b.r, postB.r, x)), 0F, 1F);
        out.g = MathUtils.clamp((float) interpolation.interpolate(IInterp.context.set(preA.g, a.g, b.g, postB.g, x)), 0F, 1F);
        out.b = MathUtils.clamp((float) interpolation.interpolate(IInterp.context.set(preA.b, a.b, b.b, postB.b, x)), 0F, 1F);
        out.a = MathUtils.clamp((float) interpolation.interpolate(IInterp.context.set(preA.a, a.a, b.a, postB.a, x)), 0F, 1F);

        return out;
    }

    /**
     * Interpolate between color keyframes through the long hue path on the spectrum bar.
     */
    public static Color interpolateKeyframeColorHSV(Color out, Color preA, Color a, Color b, Color postB, IInterp interpolation, float x)
    {
        Colors.RGBtoHSV(Colors.HSV_SCRATCH_A, preA.r, preA.g, preA.b);
        Colors.RGBtoHSV(Colors.HSV_SCRATCH_B, a.r, a.g, a.b);
        Colors.RGBtoHSV(Colors.HSV_SCRATCH_C, b.r, b.g, b.b);
        Colors.RGBtoHSV(Colors.HSV_SCRATCH_D, postB.r, postB.g, postB.b);

        float hueB = Colors.longPathHueTarget(Colors.HSV_SCRATCH_B.r, Colors.HSV_SCRATCH_C.r);
        float huePostB = Colors.longPathHueTarget(Colors.HSV_SCRATCH_C.r, Colors.HSV_SCRATCH_D.r);
        float hueX = Colors.smootherstep(x);
        float satX = Colors.smootherstep(x);
        float valX = Colors.smootherstep(x);

        float hue = Colors.wrapHue((float) interpolation.interpolate(IInterp.context.set(
            Colors.HSV_SCRATCH_A.r, Colors.HSV_SCRATCH_B.r, hueB, huePostB, hueX)));

        float sat = (float) interpolation.interpolate(IInterp.context.set(
            Colors.HSV_SCRATCH_A.g, Colors.HSV_SCRATCH_B.g, Colors.HSV_SCRATCH_C.g, Colors.HSV_SCRATCH_D.g, satX));

        float val = (float) interpolation.interpolate(IInterp.context.set(
            Colors.HSV_SCRATCH_A.b, Colors.HSV_SCRATCH_B.b, Colors.HSV_SCRATCH_C.b, Colors.HSV_SCRATCH_D.b, valX));

        Colors.HSVtoRGB(Colors.HSV_RGB_OUT, hue, sat, val);

        out.r = Colors.HSV_RGB_OUT.r;
        out.g = Colors.HSV_RGB_OUT.g;
        out.b = Colors.HSV_RGB_OUT.b;
        out.a = MathUtils.clamp((float) interpolation.interpolate(IInterp.context.set(preA.a, a.a, b.a, postB.a, x)), 0F, 1F);

        return out;
    }

    public static boolean isPickerDefaultSecondary(Color secondary)
    {
        return secondary.r <= 0.001F && secondary.g <= 0.001F && secondary.b <= 0.001F;
    }

    /**
     * Returns how strongly the primary color should be treated as a "solid"
     * secondary color (range 0..1). 0 = no solid effect (vertex multiply only),
     * 1 = fully solid secondary color applied via lit paint.
     *
     * Uses the picker's vertical value axis (bottom = 1) so the transition
     * matches what the user sees in the color square.
     */
    public static float computeSolidStrength(Color primary, Color secondary)
    {
        if (Colors.isPickerDefaultSecondary(secondary))
        {
            return 0F;
        }

        Colors.pickerHSVFromRGB(Colors.HSV_SCRATCH_A, primary, secondary);

        return MathUtils.clamp(1F - Colors.HSV_SCRATCH_A.b, 0F, 1F);
    }

    public static void pickerColorFromHSV(Color out, Color secondary, float h, float s, float v)
    {
        /* Left edge / near-neutral: achromatic multiply only; secondary solid is applied via paint. */
        if (s <= Colors.PICKER_SATURATION_SNAP)
        {
            out.r = v;
            out.g = v;
            out.b = v;

            return;
        }

        if (Colors.isPickerDefaultSecondary(secondary))
        {
            Colors.HSVtoRGB(out, h, s, v);

            return;
        }

        Colors.HSVtoRGB(Colors.HSV_SCRATCH_A, h, 1F, 1F);

        float mixR = Lerps.lerp(1F, Colors.HSV_SCRATCH_A.r, s);
        float mixG = Lerps.lerp(1F, Colors.HSV_SCRATCH_A.g, s);
        float mixB = Lerps.lerp(1F, Colors.HSV_SCRATCH_A.b, s);

        out.r = Lerps.lerp(secondary.r, mixR, v);
        out.g = Lerps.lerp(secondary.g, mixG, v);
        out.b = Lerps.lerp(secondary.b, mixB, v);
    }

    public static void pickerHSVFromRGB(Color hsv, Color rgb, Color secondary)
    {
        Colors.RGBtoHSV(hsv, rgb.r, rgb.g, rgb.b);

        if (hsv.g <= Colors.PICKER_SATURATION_SNAP)
        {
            hsv.r = 0F;
            hsv.g = 0F;

            return;
        }

        if (Colors.isPickerDefaultSecondary(secondary))
        {
            return;
        }

        float hue = hsv.r;
        float bestSaturation = hsv.g;
        float bestValue = hsv.b;
        float bestError = Float.MAX_VALUE;

        Colors.HSVtoRGB(Colors.HSV_SCRATCH_A, hue, 1F, 1F);

        for (int si = 0; si <= 50; si++)
        {
            float saturation = si / 50F;
            float mixR = Lerps.lerp(1F, Colors.HSV_SCRATCH_A.r, saturation);
            float mixG = Lerps.lerp(1F, Colors.HSV_SCRATCH_A.g, saturation);
            float mixB = Lerps.lerp(1F, Colors.HSV_SCRATCH_A.b, saturation);

            for (int vi = 0; vi <= 50; vi++)
            {
                float value = vi / 50F;
                float r = Lerps.lerp(secondary.r, mixR, value);
                float g = Lerps.lerp(secondary.g, mixG, value);
                float b = Lerps.lerp(secondary.b, mixB, value);
                float dr = r - rgb.r;
                float dg = g - rgb.g;
                float db = b - rgb.b;
                float error = dr * dr + dg * dg + db * db;

                if (error < bestError)
                {
                    bestError = error;
                    bestSaturation = saturation;
                    bestValue = value;
                }
            }
        }

        hsv.g = bestSaturation;
        hsv.b = bestValue;
    }
}