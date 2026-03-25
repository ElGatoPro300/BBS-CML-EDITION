package mchorse.bbs_mod.cubic.render.vao;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.opengl.GlStateManager;
import mchorse.bbs_mod.bobj.BOBJArmature;
import mchorse.bbs_mod.bobj.BOBJBone;
import mchorse.bbs_mod.bobj.BOBJLoader;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.utils.joml.Matrices;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntPredicate;

public class BOBJModelVAO
{
    public BOBJLoader.CompiledData data;
    public BOBJArmature armature;

    private int vao;
    private int count;

    /* GL buffers */
    public int vertexBuffer;
    public int normalBuffer;
    public int lightBuffer;
    public int texCoordBuffer;
    public int tangentBuffer;
    public int midTextureBuffer;

    private float[] tmpVertices;
    private float[] tmpNormals;
    private int[] tmpLight;
    private float[] tmpTangents;
    private int[] dominantBonePerTriangle;

    public BOBJModelVAO(BOBJLoader.CompiledData data, BOBJArmature armature)
    {
        this.data = data;
        this.armature = armature;

        this.initBuffers();
    }

    /**
     * Initiate buffers. This method is responsible for allocating 
     * buffers for the data to be passed to VBOs and also generating the 
     * VBOs themselves. 
     */
    private void initBuffers()
    {
        this.vao = GL30.glGenVertexArrays();

        GL30.glBindVertexArray(this.vao);

        this.vertexBuffer = GL30.glGenBuffers();
        this.normalBuffer = GL30.glGenBuffers();
        this.lightBuffer = GL30.glGenBuffers();
        this.texCoordBuffer = GL30.glGenBuffers();
        this.tangentBuffer = GL30.glGenBuffers();
        this.midTextureBuffer = GL30.glGenBuffers();

        this.count = this.data.normData.length / 3;
        this.tmpVertices = new float[this.data.posData.length];
        this.tmpNormals = new float[this.data.normData.length];
        this.tmpLight = new int[this.data.posData.length];
        this.tmpTangents = new float[this.count * 4];
        this.dominantBonePerTriangle = new int[this.count / 3];
        this.buildDominantBones();

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, this.vertexBuffer);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, this.data.posData, GL30.GL_DYNAMIC_DRAW);
        GL30.glVertexAttribPointer(Attributes.POSITION, 3, GL30.GL_FLOAT, false, 0, 0);

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, this.normalBuffer);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, this.data.normData, GL30.GL_DYNAMIC_DRAW);
        GL30.glVertexAttribPointer(Attributes.NORMAL, 3, GL30.GL_FLOAT, false, 0, 0);

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, this.lightBuffer);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, this.tmpLight, GL30.GL_DYNAMIC_DRAW);
        GL30.glVertexAttribIPointer(Attributes.LIGHTMAP_UV, 2, GL30.GL_INT, 0, 0);

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, this.texCoordBuffer);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, this.data.texData, GL30.GL_STATIC_DRAW);
        GL30.glVertexAttribPointer(Attributes.TEXTURE_UV, 2, GL30.GL_FLOAT, false, 0, 0);

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, this.tangentBuffer);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, this.tmpTangents, GL30.GL_STATIC_DRAW);
        GL30.glVertexAttribPointer(Attributes.TANGENTS, 4, GL30.GL_FLOAT, false, 0, 0);

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, this.texCoordBuffer);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, this.data.texData, GL30.GL_STATIC_DRAW);
        GL30.glVertexAttribPointer(Attributes.MID_TEXTURE_UV, 2, GL30.GL_FLOAT, false, 0, 0);

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    /**
     * Clean up resources which were used by this  
     */
    public void delete()
    {
        GL30.glDeleteVertexArrays(this.vao);

        GL15.glDeleteBuffers(this.vertexBuffer);
        GL15.glDeleteBuffers(this.normalBuffer);
        GL15.glDeleteBuffers(this.lightBuffer);
        GL15.glDeleteBuffers(this.texCoordBuffer);
        GL15.glDeleteBuffers(this.tangentBuffer);
        GL15.glDeleteBuffers(this.midTextureBuffer);
    }

    /**
     * Update this mesh. This method is responsible for applying 
     * matrix transformations to vertices and normals according to its 
     * bone owners and these bone influences.
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
                String boneName = this.armature.orderedBones.get(lightBone).name;
                allowBone = stencilMap.allowedBones.contains(boneName);
            }

            if (stencilMap != null)
            {
                this.tmpLight[i * 2] = Math.max(0, stencilMap.increment ? (allowBone ? lightBone : 0) : 0);
                this.tmpLight[i * 2 + 1] = 0;
            }
        }

        this.processData(newVertices, newNormals);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vertexBuffer);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, newVertices, GL15.GL_DYNAMIC_DRAW);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.normalBuffer);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, newNormals, GL15.GL_DYNAMIC_DRAW);

        if (BBSRendering.isIrisShadersEnabled())
        {
            BBSRendering.calculateTangents(this.tmpTangents, newVertices, newNormals, this.data.texData);

            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.tangentBuffer);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, this.tmpTangents, GL15.GL_DYNAMIC_DRAW);
        }

        if (stencilMap != null)
        {
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.lightBuffer);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, this.tmpLight, GL15.GL_DYNAMIC_DRAW);
        }

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
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

    private void drawTriangles(IntPredicate predicate)
    {
        int start = -1;

        for (int i = 0; i < this.dominantBonePerTriangle.length; i++)
        {
            boolean draw = predicate.test(this.dominantBonePerTriangle[i]);

            if (draw && start == -1)
            {
                start = i;
            }
            else if (!draw && start != -1)
            {
                GL30.glDrawArrays(GL30.GL_TRIANGLES, start * 3, (i - start) * 3);
                start = -1;
            }
        }

        if (start != -1)
        {
            GL30.glDrawArrays(GL30.GL_TRIANGLES, start * 3, (this.dominantBonePerTriangle.length - start) * 3);
        }
    }

    public void render(ShaderProgram shader, MatrixStack stack, float r, float g, float b, float a, StencilMap stencilMap, int light, int overlay, Link defaultTexture)
    {
        boolean hasShaders = BBSRendering.isIrisShadersEnabled();

        GL30.glVertexAttrib4f(Attributes.COLOR, r, g, b, a);
        GL30.glVertexAttribI2i(Attributes.OVERLAY_UV, overlay & '\uffff', overlay >> 16 & '\uffff');
        GL30.glVertexAttribI2i(Attributes.LIGHTMAP_UV, light & '\uffff', light >> 16 & '\uffff');

        int currentVAO = GL30.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int currentElementArrayBuffer = GL30.glGetInteger(GL30.GL_ELEMENT_ARRAY_BUFFER_BINDING);

        ModelVAORenderer.setupUniforms(stack, shader);

        GL30.glBindVertexArray(this.vao);

        GL30.glEnableVertexAttribArray(Attributes.POSITION);
        GL30.glEnableVertexAttribArray(Attributes.TEXTURE_UV);
        GL30.glEnableVertexAttribArray(Attributes.NORMAL);

        if (stencilMap != null) GL30.glEnableVertexAttribArray(Attributes.LIGHTMAP_UV);
        if (hasShaders) GL30.glEnableVertexAttribArray(Attributes.TANGENTS);
        if (hasShaders) GL30.glEnableVertexAttribArray(Attributes.MID_TEXTURE_UV);

        if (stencilMap == null)
        {
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
                GL30.glDrawArrays(GL30.GL_TRIANGLES, 0, this.count);
            }
            else
            {
                if (defaultTexture != null)
                {
                    BBSModClient.getTextures().bindTexture(defaultTexture);
                }

                this.drawTriangles((bone) -> bone < 0 || !overrides.containsKey(bone));

                for (Map.Entry<Integer, Link> entry : overrides.entrySet())
                {
                    BBSModClient.getTextures().bindTexture(entry.getValue());
                    this.drawTriangles((bone) -> bone == entry.getKey());
                }
            }
        }
        else
        {
            GL30.glDrawArrays(GL30.GL_TRIANGLES, 0, this.count);
        }

        GL30.glDisableVertexAttribArray(Attributes.POSITION);
        GL30.glDisableVertexAttribArray(Attributes.TEXTURE_UV);
        GL30.glDisableVertexAttribArray(Attributes.NORMAL);

        if (stencilMap != null) GL30.glDisableVertexAttribArray(Attributes.LIGHTMAP_UV);
        if (hasShaders) GL30.glDisableVertexAttribArray(Attributes.TANGENTS);
        if (hasShaders) GL30.glDisableVertexAttribArray(Attributes.MID_TEXTURE_UV);

        GL30.glBindVertexArray(currentVAO);
        GL30.glBindBuffer(GL30.GL_ELEMENT_ARRAY_BUFFER, currentElementArrayBuffer);
    }
}
