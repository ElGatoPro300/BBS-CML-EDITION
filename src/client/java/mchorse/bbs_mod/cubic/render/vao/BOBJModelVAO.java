package mchorse.bbs_mod.cubic.render.vao;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.bobj.BOBJArmature;
import mchorse.bbs_mod.bobj.BOBJBone;
import mchorse.bbs_mod.bobj.BOBJLoader;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.client.render.picker.BBSPickerRenderer;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.utils.joml.Matrices;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.IntPredicate;

/**
 * Skinned BOBJ mesh. Bone skinning stays on the CPU (unchanged by the render migration); the 1.21.11
 * GPU pipeline rewrite removed the raw-GL VAO/VBO + ShaderProgram bind this class previously drew
 * with, so the skinned result (tmpVertices/tmpNormals/tmpLight) is instead emitted into one or more
 * BufferBuilders per draw and submitted through {@link BBSShaders#getModelLayer()} (per-bone texture
 * overrides still split into separate draws/buffers, one per bound texture, matching the previous
 * {@code glDrawArrays} range-splitting behaviour).
 */
public class BOBJModelVAO
{
    public BOBJLoader.CompiledData data;
    public BOBJArmature armature;

    private final int count;

    private final float[] tmpVertices;
    private final float[] tmpNormals;
    private final int[] tmpLight;
    private final int[] dominantBonePerTriangle;

    public BOBJModelVAO(BOBJLoader.CompiledData data, BOBJArmature armature)
    {
        this.data = data;
        this.armature = armature;

        this.count = this.data.normData.length / 3;
        this.tmpVertices = new float[this.data.posData.length];
        this.tmpNormals = new float[this.data.normData.length];
        this.tmpLight = new int[this.count * 2];
        this.dominantBonePerTriangle = new int[this.count / 3];

        this.buildDominantBones();
    }

    /**
     * Previously freed the raw-GL VAO/VBOs. The skinned mesh now draws through the immediate
     * BufferBuilder path (see {@link #render}), so there is nothing GPU-side to free here anymore.
     */
    public void delete()
    {}

    /**
     * Update this mesh. This method is responsible for applying
     * matrix transformations to vertices and normals according to its
     * bone owners and these bone influences. The skinned result is kept on the CPU
     * (tmpVertices/tmpNormals/tmpLight) and emitted into a BufferBuilder in {@link #render}.
     */
    public void updateMesh(StencilMap stencilMap)
    {
        Vector4f sum = new Vector4f();
        Vector4f result = new Vector4f(0F, 0F, 0F, 0F);
        Vector3f sumNormal = new Vector3f();
        Vector3f resultNormal = new Vector3f();

        float[] oldVertices = this.data.posData;
        float[] newVertices = this.tmpVertices;
        float[] oldNormals = this.data.normData;
        float[] newNormals = this.tmpNormals;

        Matrix4f[] matrices = this.armature.matrices;

        for (int i = 0, c = this.count; i < c; i++)
        {
            int count = 0;
            float maxWeight = -1;
            int lightBone = -1;

            for (int w = 0; w < 4; w++)
            {
                float weight = this.data.weightData[i * 4 + w];

                if (weight > 0)
                {
                    int index = this.data.boneIndexData[i * 4 + w];

                    sum.set(oldVertices[i * 3], oldVertices[i * 3 + 1], oldVertices[i * 3 + 2], 1F);
                    matrices[index].transform(sum);
                    result.add(sum.mul(weight));

                    sumNormal.set(oldNormals[i * 3], oldNormals[i * 3 + 1], oldNormals[i * 3 + 2]);
                    Matrices.TEMP_3F.set(matrices[index]).transform(sumNormal);
                    resultNormal.add(sumNormal.mul(weight));

                    count++;

                    if (weight > maxWeight)
                    {
                        lightBone = index;
                        maxWeight = weight;
                    }
                }
            }

            if (count == 0)
            {
                result.set(oldVertices[i * 3], oldVertices[i * 3 + 1], oldVertices[i * 3 + 2], 1F);
                resultNormal.set(oldNormals[i * 3], oldNormals[i * 3 + 1], oldNormals[i * 3 + 2]);
            }

            result.x /= result.w;
            result.y /= result.w;
            result.z /= result.w;

            newVertices[i * 3] = result.x;
            newVertices[i * 3 + 1] = result.y;
            newVertices[i * 3 + 2] = result.z;

            newNormals[i * 3] = resultNormal.x;
            newNormals[i * 3 + 1] = resultNormal.y;
            newNormals[i * 3 + 2] = resultNormal.z;

            result.set(0F, 0F, 0F, 0F);
            resultNormal.set(0F, 0F, 0F);

            boolean allowBone = true;
            if (stencilMap != null && stencilMap.allowedBones != null && lightBone >= 0)
            {
                BOBJBone bone = this.getBoneByIndex(lightBone);
                allowBone = bone != null && stencilMap.allowedBones.contains(bone.name);
            }

            if (stencilMap != null)
            {
                this.tmpLight[i * 2] = Math.max(0, stencilMap.increment ? (allowBone ? lightBone : 0) : 0);
                this.tmpLight[i * 2 + 1] = 0;
            }
        }

        this.processData(newVertices, newNormals);
    }

    protected void processData(float[] newVertices, float[] newNormals)
    {}

    private void buildDominantBones()
    {
        for (int triangle = 0, triCount = this.dominantBonePerTriangle.length; triangle < triCount; triangle++)
        {
            int base = triangle * 3;
            int a = this.getDominantBoneForVertex(base);
            int b = this.getDominantBoneForVertex(base + 1);
            int c = this.getDominantBoneForVertex(base + 2);

            if (a == b || a == c)
            {
                this.dominantBonePerTriangle[triangle] = a;
            }
            else if (b == c)
            {
                this.dominantBonePerTriangle[triangle] = b;
            }
            else
            {
                this.dominantBonePerTriangle[triangle] = a;
            }
        }
    }

    private int getDominantBoneForVertex(int vertex)
    {
        int base = vertex * 4;
        float max = -1F;
        int bone = -1;

        for (int i = 0; i < 4; i++)
        {
            float weight = this.data.weightData[base + i];
            int boneIndex = this.data.boneIndexData[base + i];

            if (boneIndex >= 0 && weight > max)
            {
                max = weight;
                bone = boneIndex;
            }
        }

        return bone;
    }

    private BOBJBone getBoneByIndex(int index)
    {
        for (BOBJBone bone : this.armature.orderedBones)
        {
            if (bone.index == index)
            {
                return bone;
            }
        }

        return null;
    }

    public void render(MatrixStack stack, float r, float g, float b, float a, StencilMap stencilMap, int light, int overlay, Link defaultTexture)
    {
        if (stencilMap != null)
        {
            BuiltBuffer built = this.writeBuffer(stack, r, g, b, a, stencilMap, light, overlay, null);

            if (built != null)
            {
                BBSPickerRenderer.draw(BBSShaders.getPickerModelsProgram(), built, RenderSystem.getModelViewMatrix());
            }

            return;
        }

        Map<Integer, Link> overrides = new HashMap<>();

        for (BOBJBone bone : this.armature.orderedBones)
        {
            if (bone.texture != null)
            {
                overrides.put(bone.index, bone.texture);
            }
        }

        if (overrides.isEmpty())
        {
            if (defaultTexture != null)
            {
                BBSModClient.getTextures().bindTexture(defaultTexture);
            }

            this.drawGroup(stack, r, g, b, a, light, overlay, null);

            return;
        }

        if (defaultTexture != null)
        {
            BBSModClient.getTextures().bindTexture(defaultTexture);
        }

        this.drawGroup(stack, r, g, b, a, light, overlay, (bone) -> bone < 0 || !overrides.containsKey(bone));

        for (Map.Entry<Integer, Link> entry : overrides.entrySet())
        {
            BBSModClient.getTextures().bindTexture(entry.getValue());
            this.drawGroup(stack, r, g, b, a, light, overlay, (bone) -> bone == entry.getKey());
        }
    }

    private void drawGroup(MatrixStack stack, float r, float g, float b, float a, int light, int overlay, IntPredicate predicate)
    {
        BuiltBuffer built = this.writeBuffer(stack, r, g, b, a, null, light, overlay, predicate);

        if (built != null)
        {
            BBSShaders.getModelLayer().draw(built);
        }
    }

    private BuiltBuffer writeBuffer(MatrixStack stack, float r, float g, float b, float a, StencilMap stencilMap, int light, int overlay, IntPredicate predicate)
    {
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);

        Matrix4f position = stack.peek().getPositionMatrix();
        Matrix3f normalMatrix = stack.peek().getNormalMatrix();

        float[] vertices = this.tmpVertices;
        float[] normals = this.tmpNormals;
        float[] texData = this.data.texData;

        Vector4f vertex = new Vector4f();
        Vector3f normal = new Vector3f();

        int lu = light & 0xffff;
        int lv = light >> 16 & 0xffff;

        for (int triangle = 0, triCount = this.dominantBonePerTriangle.length; triangle < triCount; triangle++)
        {
            if (predicate != null && !predicate.test(this.dominantBonePerTriangle[triangle]))
            {
                continue;
            }

            for (int k = 0; k < 3; k++)
            {
                int i = triangle * 3 + k;

                vertex.set(vertices[i * 3], vertices[i * 3 + 1], vertices[i * 3 + 2], 1F);
                position.transform(vertex);

                normal.set(normals[i * 3], normals[i * 3 + 1], normals[i * 3 + 2]);
                normalMatrix.transform(normal);

                int u = lu;
                int v = lv;

                if (stencilMap != null)
                {
                    u = this.tmpLight[i * 2];
                    v = this.tmpLight[i * 2 + 1];
                }

                builder.vertex(vertex.x, vertex.y, vertex.z)
                    .color(r, g, b, a)
                    .texture(texData[i * 2], texData[i * 2 + 1])
                    .overlay(overlay)
                    .light(u, v)
                    .normal(normal.x, normal.y, normal.z);
            }
        }

        return builder.endNullable();
    }
}
