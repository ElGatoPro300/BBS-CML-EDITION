package mchorse.bbs_mod.forms.renderers.utils;

import com.mojang.blaze3d.vertex.VertexConsumer;
import mchorse.bbs_mod.utils.colors.Color;
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

    public VertexConsumer addVertex(float x, float y, float z)
    {
        return this.consumer.addVertex(x, y, z);
    }

    public VertexConsumer vertex(Matrix4f matrix, float x, float y, float z)
    {
        return this.consumer.addVertex(matrix, x, y, z);
    }

    public VertexConsumer setColor(int red, int green, int blue, int alpha)
    {
        red = (int) (this.color.r * red);
        green = (int) (this.color.g * green);
        blue = (int) (this.color.b * blue);
        alpha = (int) (this.color.a * alpha);

        return this.consumer.setColor(red, green, blue, alpha);
    }

    @Override
    public VertexConsumer setColor(int argb)
    {
        return this.consumer.setColor(argb);
    }

    public VertexConsumer setUv(float u, float v)
    {
        return this.consumer.setUv(u, v);
    }

    public VertexConsumer setUv1(int u, int v)
    {
        return this.consumer.setUv1(u, v);
    }

    public VertexConsumer setUv2(int u, int v)
    {
        return this.consumer.setUv2(u, v);
    }

    public VertexConsumer setNormal(float x, float y, float z)
    {
        return this.consumer.setNormal(x, y, z);
    }

    @Override
    public VertexConsumer setLineWidth(float width)
    {
        return this.consumer.setLineWidth(width);
    }

}
