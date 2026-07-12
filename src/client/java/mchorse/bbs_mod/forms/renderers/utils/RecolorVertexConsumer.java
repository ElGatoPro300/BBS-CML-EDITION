package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.client.render.VertexConsumer;

import org.joml.Matrix4f;

public class RecolorVertexConsumer implements VertexConsumer
{
    public static Color newColor;

    protected VertexConsumer consumer;
    protected Color color;
    protected Color paintColor;

    public RecolorVertexConsumer(VertexConsumer consumer, Color color)
    {
        this(consumer, color, null);
    }

    public RecolorVertexConsumer(VertexConsumer consumer, Color color, Color paintColor)
    {
        this.consumer = consumer;
        this.color = color;
        this.paintColor = paintColor;
    }

    public RecolorVertexConsumer(VertexConsumer consumer, Color color, Color paintColor, boolean ignoredLegacyParam)
    {
        this(consumer, color, paintColor);
    }

    @Override
    public VertexConsumer vertex(float x, float y, float z)
    {
        return this.consumer.vertex(x, y, z);
    }

    @Override
    public VertexConsumer vertex(Matrix4f matrix, float x, float y, float z)
    {
        return this.consumer.vertex(matrix, x, y, z);
    }

    @Override
    public VertexConsumer color(int red, int green, int blue, int alpha)
    {
        red = (int) (this.color.r * red);
        green = (int) (this.color.g * green);
        blue = (int) (this.color.b * blue);
        alpha = (int) (this.color.a * alpha);

        /* Paint overlay stage: blend the recolored vertex color toward the paint color by its strength */
        if (this.paintColor != null && Math.abs(this.paintColor.a) > 0F)
        {
            float pa = this.paintColor.a;

            if (pa >= 1F)
            {
                red = (int) (this.paintColor.r * 255F);
                green = (int) (this.paintColor.g * 255F);
                blue = (int) (this.paintColor.b * 255F);
            }
            else if (pa > 0F)
            {
                red = (int) (red + (this.paintColor.r * 255F - red) * pa);
                green = (int) (green + (this.paintColor.g * 255F - green) * pa);
                blue = (int) (blue + (this.paintColor.b * 255F - blue) * pa);
            }
            else
            {
                float factor = Math.max(0F, 1F + pa);

                red = (int) (red * factor);
                green = (int) (green * factor);
                blue = (int) (blue * factor);
            }
        }

        return this.consumer.color(red, green, blue, alpha);
    }

    @Override
    public VertexConsumer texture(float u, float v)
    {
        return this.consumer.texture(u, v);
    }

    @Override
    public VertexConsumer overlay(int u, int v)
    {
        return this.consumer.overlay(u, v);
    }

    @Override
    public VertexConsumer light(int u, int v)
    {
        return this.consumer.light(u, v);
    }

    @Override
    public VertexConsumer normal(float x, float y, float z)
    {
        return this.consumer.normal(x, y, z);
    }

}
