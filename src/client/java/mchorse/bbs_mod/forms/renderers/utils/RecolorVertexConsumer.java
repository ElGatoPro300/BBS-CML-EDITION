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
            float lum = (red * 0.2126F + green * 0.7152F + blue * 0.0722F) / 255F;
            int paintR = (int) (this.paintColor.r * lum * 255F);
            int paintG = (int) (this.paintColor.g * lum * 255F);
            int paintB = (int) (this.paintColor.b * lum * 255F);

            if (pa >= 1F)
            {
                red = paintR;
                green = paintG;
                blue = paintB;
            }
            else if (pa > 0F)
            {
                red = (int) (red + (paintR - red) * pa);
                green = (int) (green + (paintG - green) * pa);
                blue = (int) (blue + (paintB - blue) * pa);
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
