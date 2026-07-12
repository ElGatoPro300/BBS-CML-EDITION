package mchorse.bbs_mod.utils.pose;

import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.interps.Interpolation;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.interps.Lerps;
import mchorse.bbs_mod.utils.interps.easings.EasingArgs;
import mchorse.bbs_mod.utils.resources.LinkUtils;

public class PoseTransform extends Transform
{
    private static PoseTransform DEFAULT = new PoseTransform();

    public float fix;
    public final Color color = new Color().set(Colors.WHITE);
    public final Color colorSecondary = new Color().set(0F, 0F, 0F, 1F);
    public final Color paintColor = new Color().set(1F, 1F, 1F, 0F);
    public final Color glowingColor = new Color().set(1F, 1F, 1F, 1F);
    public float glowIntensity;
    public float glowRadius;
    public float lighting;
    public float shaderShadow = PaintSettings.SHADER_SHADOW_DEFAULT;
    public Link texture;
    public float textureBlend = 1F;

    @Override
    public void identity()
    {
        super.identity();

        this.fix = 0F;
        this.color.set(Colors.WHITE);
        this.colorSecondary.set(0F, 0F, 0F, 1F);
        this.paintColor.set(1F, 1F, 1F, 0F);
        this.glowingColor.set(1F, 1F, 1F, 1F);
        this.glowIntensity = 0F;
        this.glowRadius = 0F;
        this.lighting = 0F;
        this.shaderShadow = PaintSettings.SHADER_SHADOW_DEFAULT;
        this.texture = null;
        this.textureBlend = 1F;
    }

    @Override
    public void lerp(Transform transform, float a)
    {
        if (transform instanceof PoseTransform pose)
        {
            this.fix = Lerps.lerp(this.fix, pose.fix, a);

            this.color.r = Lerps.lerp(this.color.r, pose.color.r, a);
            this.color.g = Lerps.lerp(this.color.g, pose.color.g, a);
            this.color.b = Lerps.lerp(this.color.b, pose.color.b, a);
            this.color.a = Lerps.lerp(this.color.a, pose.color.a, a);

            this.paintColor.r = Lerps.lerp(this.paintColor.r, pose.paintColor.r, a);
            this.paintColor.g = Lerps.lerp(this.paintColor.g, pose.paintColor.g, a);
            this.paintColor.b = Lerps.lerp(this.paintColor.b, pose.paintColor.b, a);
            this.paintColor.a = Lerps.lerp(this.paintColor.a, pose.paintColor.a, a);

            this.glowingColor.r = Lerps.lerp(this.glowingColor.r, pose.glowingColor.r, a);
            this.glowingColor.g = Lerps.lerp(this.glowingColor.g, pose.glowingColor.g, a);
            this.glowingColor.b = Lerps.lerp(this.glowingColor.b, pose.glowingColor.b, a);
            this.glowingColor.a = 1F;

            this.glowIntensity = Lerps.lerp(this.glowIntensity, pose.glowIntensity, a);
            this.glowRadius = Lerps.lerp(this.glowRadius, pose.glowRadius, a);
            this.lighting = Lerps.lerp(this.lighting, pose.lighting, a);
            this.shaderShadow = Lerps.lerp(this.shaderShadow, pose.shaderShadow, a);
            this.textureBlend = Lerps.lerp(this.textureBlend, pose.textureBlend, a);
        }

        super.lerp(transform, a);
    }

    @Override
    public void lerp(Transform preA, Transform a, Transform b, Transform postB, IInterp interp, float x)
    {
        super.lerp(preA, a, b, postB, interp, x);

        if (preA instanceof PoseTransform || a instanceof PoseTransform || b instanceof PoseTransform || postB instanceof PoseTransform)
        {
            PoseTransform preA1 = preA instanceof PoseTransform ? (PoseTransform) preA : DEFAULT;
            PoseTransform a1 = a instanceof PoseTransform ? (PoseTransform) a : DEFAULT;
            PoseTransform b1 = b instanceof PoseTransform ? (PoseTransform) b : DEFAULT;
            PoseTransform postB1 = postB instanceof PoseTransform ? (PoseTransform) postB : DEFAULT;

            this.fix = (float) interp.interpolate(IInterp.context.set(preA1.fix, a1.fix, b1.fix, postB1.fix, x));

            this.color.set(
                (float) MathUtils.clamp(interp.interpolate(IInterp.context.set(preA1.color.r, a1.color.r, b1.color.r, postB1.color.r, x)), 0F, 1F),
                (float) MathUtils.clamp(interp.interpolate(IInterp.context.set(preA1.color.g, a1.color.g, b1.color.g, postB1.color.g, x)), 0F, 1F),
                (float) MathUtils.clamp(interp.interpolate(IInterp.context.set(preA1.color.b, a1.color.b, b1.color.b, postB1.color.b, x)), 0F, 1F),
                (float) MathUtils.clamp(interp.interpolate(IInterp.context.set(preA1.color.a, a1.color.a, b1.color.a, postB1.color.a, x)), 0F, 1F)
            );

            this.paintColor.set(
                (float) MathUtils.clamp(interp.interpolate(IInterp.context.set(preA1.paintColor.r, a1.paintColor.r, b1.paintColor.r, postB1.paintColor.r, x)), 0F, 1F),
                (float) MathUtils.clamp(interp.interpolate(IInterp.context.set(preA1.paintColor.g, a1.paintColor.g, b1.paintColor.g, postB1.paintColor.g, x)), 0F, 1F),
                (float) MathUtils.clamp(interp.interpolate(IInterp.context.set(preA1.paintColor.b, a1.paintColor.b, b1.paintColor.b, postB1.paintColor.b, x)), 0F, 1F),
                (float) interp.interpolate(IInterp.context.set(preA1.paintColor.a, a1.paintColor.a, b1.paintColor.a, postB1.paintColor.a, x))
            );

            this.glowingColor.set(
                (float) MathUtils.clamp(interp.interpolate(IInterp.context.set(preA1.glowingColor.r, a1.glowingColor.r, b1.glowingColor.r, postB1.glowingColor.r, x)), 0F, 1F),
                (float) MathUtils.clamp(interp.interpolate(IInterp.context.set(preA1.glowingColor.g, a1.glowingColor.g, b1.glowingColor.g, postB1.glowingColor.g, x)), 0F, 1F),
                (float) MathUtils.clamp(interp.interpolate(IInterp.context.set(preA1.glowingColor.b, a1.glowingColor.b, b1.glowingColor.b, postB1.glowingColor.b, x)), 0F, 1F),
                1F
            );

            this.glowIntensity = (float) interp.interpolate(IInterp.context.set(preA1.glowIntensity, a1.glowIntensity, b1.glowIntensity, postB1.glowIntensity, x));
            this.glowRadius = (float) interp.interpolate(IInterp.context.set(preA1.glowRadius, a1.glowRadius, b1.glowRadius, postB1.glowRadius, x));
            this.lighting = (float) interp.interpolate(IInterp.context.set(preA1.lighting, a1.lighting, b1.lighting, postB1.lighting, x));
            this.shaderShadow = (float) interp.interpolate(IInterp.context.set(preA1.shaderShadow, a1.shaderShadow, b1.shaderShadow, postB1.shaderShadow, x));
            this.textureBlend = (float) interp.interpolate(IInterp.context.set(preA1.textureBlend, a1.textureBlend, b1.textureBlend, postB1.textureBlend, x));
        }
    }

    @Override
    public void lerp(Transform preA, Transform a, Transform b, Transform postB, IInterp interp, float x, double w0, double w1, double w2, double w3)
    {
        super.lerp(preA, a, b, postB, interp, x, w0, w1, w2, w3);

        if (preA instanceof PoseTransform || a instanceof PoseTransform || b instanceof PoseTransform || postB instanceof PoseTransform)
        {
            PoseTransform preA1 = preA instanceof PoseTransform ? (PoseTransform) preA : DEFAULT;
            PoseTransform a1 = a instanceof PoseTransform ? (PoseTransform) a : DEFAULT;
            PoseTransform b1 = b instanceof PoseTransform ? (PoseTransform) b : DEFAULT;
            PoseTransform postB1 = postB instanceof PoseTransform ? (PoseTransform) postB : DEFAULT;

            EasingArgs args = null;

            if (interp instanceof Interpolation)
            {
                args = ((Interpolation) interp).getArgs();
            }

            this.fix = this.interpolate(preA1.fix, a1.fix, b1.fix, postB1.fix, x, interp, args, preA == a, postB == b, w0, w1, w2, w3);

            this.color.set(
                MathUtils.clamp(this.interpolate(preA1.color.r, a1.color.r, b1.color.r, postB1.color.r, x, interp, args, preA == a, postB == b, w0, w1, w2, w3), 0F, 1F),
                MathUtils.clamp(this.interpolate(preA1.color.g, a1.color.g, b1.color.g, postB1.color.g, x, interp, args, preA == a, postB == b, w0, w1, w2, w3), 0F, 1F),
                MathUtils.clamp(this.interpolate(preA1.color.b, a1.color.b, b1.color.b, postB1.color.b, x, interp, args, preA == a, postB == b, w0, w1, w2, w3), 0F, 1F),
                MathUtils.clamp(this.interpolate(preA1.color.a, a1.color.a, b1.color.a, postB1.color.a, x, interp, args, preA == a, postB == b, w0, w1, w2, w3), 0F, 1F)
            );

            this.paintColor.set(
                MathUtils.clamp(this.interpolate(preA1.paintColor.r, a1.paintColor.r, b1.paintColor.r, postB1.paintColor.r, x, interp, args, preA == a, postB == b, w0, w1, w2, w3), 0F, 1F),
                MathUtils.clamp(this.interpolate(preA1.paintColor.g, a1.paintColor.g, b1.paintColor.g, postB1.paintColor.g, x, interp, args, preA == a, postB == b, w0, w1, w2, w3), 0F, 1F),
                MathUtils.clamp(this.interpolate(preA1.paintColor.b, a1.paintColor.b, b1.paintColor.b, postB1.paintColor.b, x, interp, args, preA == a, postB == b, w0, w1, w2, w3), 0F, 1F),
                this.interpolate(preA1.paintColor.a, a1.paintColor.a, b1.paintColor.a, postB1.paintColor.a, x, interp, args, preA == a, postB == b, w0, w1, w2, w3)
            );

            this.glowingColor.set(
                MathUtils.clamp(this.interpolate(preA1.glowingColor.r, a1.glowingColor.r, b1.glowingColor.r, postB1.glowingColor.r, x, interp, args, preA == a, postB == b, w0, w1, w2, w3), 0F, 1F),
                MathUtils.clamp(this.interpolate(preA1.glowingColor.g, a1.glowingColor.g, b1.glowingColor.g, postB1.glowingColor.g, x, interp, args, preA == a, postB == b, w0, w1, w2, w3), 0F, 1F),
                MathUtils.clamp(this.interpolate(preA1.glowingColor.b, a1.glowingColor.b, b1.glowingColor.b, postB1.glowingColor.b, x, interp, args, preA == a, postB == b, w0, w1, w2, w3), 0F, 1F),
                1F
            );

            this.glowIntensity = this.interpolate(preA1.glowIntensity, a1.glowIntensity, b1.glowIntensity, postB1.glowIntensity, x, interp, args, preA == a, postB == b, w0, w1, w2, w3);
            this.glowRadius = this.interpolate(preA1.glowRadius, a1.glowRadius, b1.glowRadius, postB1.glowRadius, x, interp, args, preA == a, postB == b, w0, w1, w2, w3);
            this.lighting = this.interpolate(preA1.lighting, a1.lighting, b1.lighting, postB1.lighting, x, interp, args, preA == a, postB == b, w0, w1, w2, w3);
            this.shaderShadow = this.interpolate(preA1.shaderShadow, a1.shaderShadow, b1.shaderShadow, postB1.shaderShadow, x, interp, args, preA == a, postB == b, w0, w1, w2, w3);
            this.textureBlend = this.interpolate(preA1.textureBlend, a1.textureBlend, b1.textureBlend, postB1.textureBlend, x, interp, args, preA == a, postB == b, w0, w1, w2, w3);
        }
    }

    private float interpolate(double preA, double a, double b, double postB, float x, IInterp interp, EasingArgs args, boolean boundaryStart, boolean boundaryEnd, double w0, double w1, double w2, double w3)
    {
        IInterp.context.set(preA, a, b, postB, x);
        IInterp.context.setBoundary(boundaryStart, boundaryEnd);

        if (args != null)
        {
            IInterp.context.extra(args);
        }

        if (interp == Interpolations.NURBS)
        {
            IInterp.context.weights(w0, w1, w2, w3);
        }

        return (float) interp.interpolate(IInterp.context);
    }

    @Override
    public boolean equals(Object obj)
    {
        boolean result = super.equals(obj);

        if (obj instanceof PoseTransform poseTransform)
        {
            result = result && this.fix == poseTransform.fix;
            result = result && this.color.equals(poseTransform.color);
            result = result && this.paintColor.equals(poseTransform.paintColor);
            result = result && this.glowingColor.equals(poseTransform.glowingColor);
            result = result && this.glowIntensity == poseTransform.glowIntensity;
            result = result && this.glowRadius == poseTransform.glowRadius;
            result = result && this.lighting == poseTransform.lighting;
            result = result && this.shaderShadow == poseTransform.shaderShadow;
            result = result && this.textureBlend == poseTransform.textureBlend;
            result = result && ((this.texture == null && poseTransform.texture == null) || (this.texture != null && this.texture.equals(poseTransform.texture)));
        }

        return result;
    }

    @Override
    public Transform copy()
    {
        PoseTransform transform = new PoseTransform();

        transform.copy(this);

        return transform;
    }

    @Override
    public void copy(Transform transform)
    {
        if (transform instanceof PoseTransform poseTransform)
        {
            this.fix = poseTransform.fix;
            this.color.copy(poseTransform.color);
            this.colorSecondary.copy(poseTransform.colorSecondary);
            this.paintColor.copy(poseTransform.paintColor);
            this.glowingColor.copy(poseTransform.glowingColor);
            this.glowingColor.a = 1F;
            this.glowIntensity = poseTransform.glowIntensity;
            this.glowRadius = poseTransform.glowRadius;
            this.lighting = poseTransform.lighting;
            this.shaderShadow = poseTransform.shaderShadow;
            this.texture = LinkUtils.copy(poseTransform.texture);
            this.textureBlend = poseTransform.textureBlend;
        }

        super.copy(transform);
    }

    @Override
    public void toData(MapType data)
    {
        super.toData(data);

        data.putFloat("fix", this.fix);
        data.putInt("color", this.color.getARGBColor());

        if (!Colors.isPickerDefaultSecondary(this.colorSecondary))
        {
            data.putInt("color_secondary", this.colorSecondary.getRGBColor());
        }

        data.putInt("paint_color", this.paintColor.getARGBColor());
        data.putInt("glowing_color", this.glowingColor.getRGBColor());
        data.putFloat("glow_intensity", this.glowIntensity);
        data.putFloat("glow_radius", this.glowRadius);
        data.putFloat("lighting", this.lighting);
        if (this.shaderShadow != PaintSettings.SHADER_SHADOW_DEFAULT)
        {
            data.putFloat("shader_shadow", this.shaderShadow);
        }
        if (this.texture != null)
        {
            data.put("texture", LinkUtils.toData(this.texture));
        }
        if (this.textureBlend != 1F)
        {
            data.putFloat("texture_blend", this.textureBlend);
        }
    }

    @Override
    public void fromData(MapType data)
    {
        super.fromData(data);

        this.fix = data.getFloat("fix");
        this.color.set(data.getInt("color", Colors.WHITE));
        this.colorSecondary.set(0F, 0F, 0F, 1F);

        if (data.has("color_secondary"))
        {
            this.colorSecondary.set(data.getInt("color_secondary"));
            this.colorSecondary.a = 1F;
        }

        this.paintColor.set(data.getInt("paint_color", 0x00FFFFFF));
        int glowArgb = data.getInt("glowing_color", 0xFFFFFF);

        this.glowingColor.set(glowArgb);
        this.glowingColor.a = 1F;

        if (data.has("glow_intensity"))
        {
            this.glowIntensity = data.getFloat("glow_intensity");
        }
        else
        {
            Color legacy = new Color().set(glowArgb);

            this.glowIntensity = legacy.a;
        }

        this.glowRadius = data.getFloat("glow_radius");

        this.lighting = data.getFloat("lighting");
        if (data.has("shader_shadow"))
        {
            this.shaderShadow = data.getFloat("shader_shadow", PaintSettings.SHADER_SHADOW_DEFAULT);
        }
        else
        {
            this.shaderShadow = PaintSettings.SHADER_SHADOW_DEFAULT;
        }
        if (data.has("texture"))
        {
            this.texture = LinkUtils.create(data.get("texture"));
        }
        if (data.has("texture_blend"))
        {
            this.textureBlend = data.getFloat("texture_blend");
        }
        else
        {
            this.textureBlend = 1F;
        }
    }

    @Override
    public boolean isDefault()
    {
        return this.equals(DEFAULT);
    }
}