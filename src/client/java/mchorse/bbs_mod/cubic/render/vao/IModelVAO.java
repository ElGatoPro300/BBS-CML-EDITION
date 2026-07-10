package mchorse.bbs_mod.cubic.render.vao;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Common contract for static triangle meshes drawn through the immediate model RenderLayer
 * (see {@link ModelVAORenderer}). Implementations bake the current stack position/normal matrices
 * into each vertex CPU-side and emit it into {@code builder} (format
 * POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL); the caller draws the resulting buffer.
 *
 * <p>Replaces the 1.21.1 raw-GL {@code render(VertexFormat, ...)} contract, which required a bound
 * VAO/VBO pair and a {@code ShaderProgram.bind()} — no longer expressible against the 1.21.11
 * RenderPipeline/RenderPass GPU abstraction.</p>
 */
public interface IModelVAO
{
    void writeImmediate(BufferBuilder builder, MatrixStack stack, float r, float g, float b, float a, int light, int overlay);

    void delete();
}
