package mchorse.bbs_mod.cubic.render.vao;

import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public interface IModelVAO
{
    public void render(VertexFormat format, MatrixStack stack, Matrix4f projectionMatrix, float r, float g, float b, float a, int light, int overlay);
}
