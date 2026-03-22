package mchorse.bbs_mod.forms.renderers.utils;

import mchorse.bbs_mod.utils.colors.Color;
import net.minecraft.client.render.VertexConsumer;
import org.joml.Matrix4f;

public class RecolorVertexConsumer implements VertexConsumer
{
    public static Color newColor;

    protected VertexConsumer consumer;
    protected Color color;

    public RecolorVertexConsumer(VertexConsumer consumer, Color color)
    {
        this.consumer = consumer;
        this.color = color;
    }

    public VertexConsumer vertex(float x, float y, float z)
    {
        return this.consumer.vertex(x, y, z);
    }

    public VertexConsumer vertex(Matrix4f matrix, float x, float y, float z)
    {
        return this.consumer.vertex(matrix, x, y, z);
    }

    public VertexConsumer color(int red, int green, int blue, int alpha)
    {
        red = (int) (this.color.r * red);
        green = (int) (this.color.g * green);
        blue = (int) (this.color.b * blue);
        alpha = (int) (this.color.a * alpha);

        return this.consumer.color(red, green, blue, alpha);
    }

    public VertexConsumer color(int argb)
    {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        return this.color(r, g, b, a);
    }

    public VertexConsumer texture(float u, float v)
    {
        return this.consumer.texture(u, v);
    }

    public VertexConsumer overlay(int u, int v)
    {
        return this.consumer.overlay(u, v);
    }

    public VertexConsumer light(int u, int v)
    {
        return this.consumer.light(u, v);
    }

    public VertexConsumer normal(float x, float y, float z)
    {
        return this.consumer.normal(x, y, z);
    }

    public VertexConsumer lineWidth(float width)
    {
        if (this.consumer != null)
        {
            return this.consumer.lineWidth(width);
        }
        return this;
    }

}
