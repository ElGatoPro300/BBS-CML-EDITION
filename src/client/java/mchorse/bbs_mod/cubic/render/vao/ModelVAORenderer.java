package mchorse.bbs_mod.cubic.render.vao;

import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

public class ModelVAORenderer
{
    public static void render(ShaderProgram shader, IModelVAO modelVAO, MatrixStack stack, Matrix4f matrix, float r, float g, float b, float a, int light, int overlay)
    {
        setupUniforms(stack, shader);
        modelVAO.render(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, stack, matrix, r, g, b, a, light, overlay);
    }

    public static void setupUniforms(MatrixStack stack, ShaderProgram shader)
    {
    }
}
