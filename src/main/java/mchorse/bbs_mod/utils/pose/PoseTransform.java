package mchorse.bbs_mod.utils.pose;

import mchorse.bbs_mod.data.types.MapType;
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
    public float lighting;
    public Link texture;

    @Override
    public void identity()
    {
        super.identity();

        this.fix = 0F;
        this.color.set(Colors.WHITE);
        this.lighting = 0F;
        this.texture = null;
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

            this.lighting = Lerps.lerp(this.lighting, pose.lighting, a);
        }

        super.lerp(transform, a);
    }

    @Override
    public void lerp(Transform preA, Transform a, Transform b, Transform postB, IInterp interp, float x)
    {
        super.lerp(preA, a, b, postB, interp, x);

        if (preA instanceof PoseTransform preA1)
        {
            PoseTransform a1 = (PoseTransform) a;
            PoseTransform b1 = (PoseTransform) b;
            PoseTransform postB1 = (PoseTransform) postB;

            this.fix = (float) interp.interpolate(IInterp.context.set(preA1.fix, a1.fix, b1.fix, postB1.fix, x));

            this.color.set(
                (float) MathUtils.clamp(interp.interpolate(IInterp.context.set(preA1.color.r, a1.color.r, b1.color.r, postB1.color.r, x)), 0F, 1F),
                (float) MathUtils.clamp(interp.interpolate(IInterp.context.set(preA1.color.g, a1.color.g, b1.color.g, postB1.color.g, x)), 0F, 1F),
                (float) MathUtils.clamp(interp.interpolate(IInterp.context.set(preA1.color.b, a1.color.b, b1.color.b, postB1.color.b, x)), 0F, 1F),
                (float) MathUtils.clamp(interp.interpolate(IInterp.context.set(preA1.color.a, a1.color.a, b1.color.a, postB1.color.a, x)), 0F, 1F)
            );

            this.lighting = (float) interp.interpolate(IInterp.context.set(preA1.lighting, a1.lighting, b1.lighting, postB1.lighting, x));
        }
    }

    @Override
    public void lerp(Transform preA, Transform a, Transform b, Transform postB, IInterp interp, float x, double w0, double w1, double w2, double w3)
    {
        super.lerp(preA, a, b, postB, interp, x, w0, w1, w2, w3);

        if (preA instanceof PoseTransform preA1)
        {
            PoseTransform a1 = (PoseTransform) a;
            PoseTransform b1 = (PoseTransform) b;
            PoseTransform postB1 = (PoseTransform) postB;

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

            this.lighting = this.interpolate(preA1.lighting, a1.lighting, b1.lighting, postB1.lighting, x, interp, args, preA == a, postB == b, w0, w1, w2, w3);
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
            result = result && this.lighting == poseTransform.lighting;
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
            this.lighting = poseTransform.lighting;
            this.texture = LinkUtils.copy(poseTransform.texture);
        }

        super.copy(transform);
    }

    @Override
    public void toData(MapType data)
    {
        super.toData(data);

        data.putFloat("fix", this.fix);
        data.putInt("color", this.color.getARGBColor());
        data.putFloat("lighting", this.lighting);
        if (this.texture != null)
        {
            data.put("texture", LinkUtils.toData(this.texture));
        }
    }

    @Override
    public void fromData(MapType data)
    {
        super.fromData(data);

        this.fix = data.getFloat("fix");
        this.color.set(data.getInt("color", Colors.WHITE));
        this.lighting = data.getFloat("lighting");
        if (data.has("texture"))
        {
            this.texture = LinkUtils.create(data.get("texture"));
        }
    }

    @Override
    public boolean isDefault()
    {
        return this.equals(DEFAULT);
    }
}