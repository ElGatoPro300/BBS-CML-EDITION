package mchorse.bbs_mod.cubic.render.vao;

import mchorse.bbs_mod.client.BBSShaders;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Static (non-skinned) triangle mesh used by cubic Models (VAO path) and extruded forms.
 *
 * <p>The 1.21.11 GPU pipeline rewrite removed the raw-GL VAO + ShaderProgram.bind()/unbind() draw this
 * class used to hold (there is no supported way to bind a raw GL vertex array against the new
 * RenderPipeline/RenderPass abstraction). The geometry is now kept on the CPU and emitted into a
 * BufferBuilder per draw ({@link #writeImmediate}), exactly like the cubic immediate path, then drawn
 * through {@link BBSShaders#getModelLayer()} by {@link ModelVAORenderer}.</p>
 */
public class ModelVAO implements IModelVAO
{
    private final ModelVAOData data;

    public ModelVAO(ModelVAOData data)
    {
        this.data = data;
    }

    /**
     * Previously freed the raw-GL VAOs. Geometry now lives on the CPU, so there is nothing to free.
     */
    public void delete()
    {}

    /**
     * Bake the stack position/normal matrices into each vertex CPU-side and write the triangles into
     * {@code builder} (format POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL). Color/overlay/light are
     * constant per draw (matching the old {@code glVertexAttrib4f}/{@code glVertexAttribI2i} defaults).
     */
    public void writeImmediate(BufferBuilder builder, MatrixStack stack, float r, float g, float b, float a, int light, int overlay)
    {
        Matrix4f position = stack.peek().getPositionMatrix();
        Matrix3f normalMatrix = stack.peek().getNormalMatrix();

        float[] vertices = this.data.vertices();
        float[] normals = this.data.normals();
        float[] texCoords = this.data.texCoords();

        Vector4f vertex = new Vector4f();
        Vector3f normal = new Vector3f();

        int lu = light & 0xffff;
        int lv = light >> 16 & 0xffff;

        int count = vertices.length / 3;

        for (int i = 0; i < count; i++)
        {
            vertex.set(vertices[i * 3], vertices[i * 3 + 1], vertices[i * 3 + 2], 1F);
            position.transform(vertex);

            normal.set(normals[i * 3], normals[i * 3 + 1], normals[i * 3 + 2]);
            normalMatrix.transform(normal);

            builder.vertex(vertex.x, vertex.y, vertex.z)
                .color(r, g, b, a)
                .texture(texCoords[i * 2], texCoords[i * 2 + 1])
                .overlay(overlay)
                .light(lu, lv)
                .normal(normal.x, normal.y, normal.z);
        }
    }
}
