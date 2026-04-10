package mchorse.bbs_mod.cubic.render.vao;

import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import org.lwjgl.opengl.GL30;

public class ModelVAORenderer
{
    public static void render(GlProgram shader, IModelVAO modelVAO, PoseStack stack, float r, float g, float b, float a, int light, int overlay)
    {
        int currentVAO = GL30.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int currentElementArrayBuffer = GL30.glGetInteger(GL30.GL_ELEMENT_ARRAY_BUFFER_BINDING);

        setupUniforms(stack, shader);

        if (shader != null)
        {
            /* shader binding handled by RenderLayer in 1.21.11 */
        }

        modelVAO.render(DefaultVertexFormat.ENTITY, r, g, b, a, light, overlay);

        GL30.glBindVertexArray(currentVAO);
        GL30.glBindBuffer(GL30.GL_ELEMENT_ARRAY_BUFFER, currentElementArrayBuffer);
    }

    public static void setupUniforms(PoseStack stack, GlProgram shader)
    {
        // 1.21.11 shader internals are no longer exposed; uniforms are handled by active pipeline.
    }
}
