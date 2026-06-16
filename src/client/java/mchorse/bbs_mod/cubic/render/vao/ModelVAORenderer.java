package mchorse.bbs_mod.cubic.render.vao;

import mchorse.bbs_mod.client.BBSRendering;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL30;

public class ModelVAORenderer
{
    public static void render(ShaderProgram shader, IModelVAO modelVAO, MatrixStack stack, float r, float g, float b, float a, int light, int overlay)
    {
        int currentVAO = GL30.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int currentElementArrayBuffer = GL30.glGetInteger(GL30.GL_ELEMENT_ARRAY_BUFFER_BINDING);

        setupUniforms(stack, shader);

        shader.bind();

        int textureID = RenderSystem.getShaderTexture(0);
        GlStateManager._activeTexture(GL30.GL_TEXTURE0);
        GlStateManager._bindTexture(textureID);

        modelVAO.render(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, r, g, b, a, light, overlay);
        shader.unbind();

        GL30.glBindVertexArray(currentVAO);
        GL30.glBindBuffer(GL30.GL_ELEMENT_ARRAY_BUFFER, currentElementArrayBuffer);
    }

    public static void setupUniforms(MatrixStack stack, ShaderProgram shader)
    {
        Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(stack.peek().getPositionMatrix());

        for (int i = 0; i < 12; i++)
        {
            shader.addSamplerTexture("Sampler" + i, RenderSystem.getShaderTexture(i));
        }

        if (shader.projectionMat != null)
        {
            shader.projectionMat.set(RenderSystem.getProjectionMatrix());
        }

        if (shader.modelViewMat != null)
        {
            shader.modelViewMat.set(modelView);
        }

        /* NormalMat is present by default in Iris' shaders, but when there is no Iris,
         * the BBS mod's model.json shader is being used instead that provides NormalMat
         * uniform.
         */
        GlUniform normalUniform = shader.getUniform("NormalMat");

        if (normalUniform != null)
        {
            if (BBSRendering.isIrisShadersEnabled())
            {
                normalUniform.set(modelView.normal(new Matrix3f()));
            }
            else
            {
                normalUniform.set(stack.peek().getNormalMatrix());
            }
        }

        Fog fog = RenderSystem.getShaderFog();

        if (shader.fogStart != null)
        {
            shader.fogStart.set(fog.start());
        }

        if (shader.fogEnd != null)
        {
            shader.fogEnd.set(fog.end());
        }

        if (shader.fogColor != null)
        {
            shader.fogColor.set(fog.red(), fog.green(), fog.blue(), fog.alpha());
        }

        if (shader.fogShape != null)
        {
            shader.fogShape.set(fog.shape().getId());
        }

        if (shader.colorModulator != null)
        {
            shader.colorModulator.set(1F, 1F, 1F, 1F);
        }

        if (shader.gameTime != null)
        {
            shader.gameTime.set(RenderSystem.getShaderGameTime());
        }

        if (shader.textureMat != null)
        {
            shader.textureMat.set(RenderSystem.getTextureMatrix());
        }

        RenderSystem.setupShaderLights(shader);
    }
}
