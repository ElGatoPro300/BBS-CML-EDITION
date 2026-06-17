package mchorse.bbs_mod.graphics.line;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.BufferBuilder;

public interface ILineRenderer <T>
{
    public void render(BufferBuilder builder, Matrix4f matrix, LinePoint<T> point);
}