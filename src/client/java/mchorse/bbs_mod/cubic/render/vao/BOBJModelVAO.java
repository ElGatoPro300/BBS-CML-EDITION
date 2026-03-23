package mchorse.bbs_mod.cubic.render.vao;

import mchorse.bbs_mod.bobj.BOBJArmature;
import mchorse.bbs_mod.bobj.BOBJBone;
import mchorse.bbs_mod.bobj.BOBJLoader;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.utils.joml.Matrices;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

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
    public int colorBuffer;

    /* Index buffers for proper opaque/translucent splitting */
    private int opaqueIndexBuffer;
    private int translucentIndexBuffer;
    private int opaqueIndexCount;
    private int translucentIndexCount;
    private int[] cachedOpaqueIdx;
    private int[] cachedTransIdx;

    private boolean hasSemiTransparency;

    private float[] tmpVertices;
    private float[] tmpNormals;
    private int[] tmpLight;
    private float[] tmpTangents;
    private float[] tmpColors;
    private int[] tmpBones;

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
        this.colorBuffer = GL30.glGenBuffers();
        this.opaqueIndexBuffer = GL30.glGenBuffers();
        this.translucentIndexBuffer = GL30.glGenBuffers();

        this.count = this.data.normData.length / 3;
        this.tmpVertices = new float[this.data.posData.length];
        this.tmpNormals = new float[this.data.normData.length];
        this.tmpLight = new int[this.data.posData.length];
        this.tmpTangents = new float[this.count * 4];
        this.tmpColors = new float[this.count * 4];
        this.tmpBones = new int[this.count];

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, this.vertexBuffer);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, this.data.posData, GL30.GL_DYNAMIC_DRAW);
        GL30.glVertexAttribPointer(Attributes.POSITION, 3, GL30.GL_FLOAT, false, 0, 0);

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, this.normalBuffer);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, this.data.normData, GL30.GL_DYNAMIC_DRAW);
        GL30.glVertexAttribPointer(Attributes.NORMAL, 3, GL30.GL_FLOAT, false, 0, 0);

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, this.lightBuffer);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, this.tmpLight, GL30.GL_DYNAMIC_DRAW);
        GL30.glVertexAttribIPointer(Attributes.LIGHTMAP_UV, 2, GL30.GL_INT, 0, 0);

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, this.colorBuffer);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, this.tmpColors, GL30.GL_DYNAMIC_DRAW);
        GL30.glVertexAttribPointer(Attributes.COLOR, 4, GL30.GL_FLOAT, false, 0, 0);

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, this.texCoordBuffer);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, this.data.texData, GL30.GL_STATIC_DRAW);
        GL30.glVertexAttribPointer(Attributes.TEXTURE_UV, 2, GL30.GL_FLOAT, false, 0, 0);

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, this.tangentBuffer);
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, this.tmpTangents, GL30.GL_STATIC_DRAW);
        GL30.glVertexAttribPointer(Attributes.TANGENTS, 4, GL30.GL_FLOAT, false, 0, 0);

        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, this.midTextureBuffer);
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
        GL15.glDeleteBuffers(this.colorBuffer);
        GL15.glDeleteBuffers(this.opaqueIndexBuffer);
        GL15.glDeleteBuffers(this.translucentIndexBuffer);
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
            /* Normalize once per vertex to keep correct lighting */
            if (resultNormal.lengthSquared() > 0F) resultNormal.normalize();
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

            if (lightBone != -1)
            {
                mchorse.bbs_mod.utils.colors.Color color = this.armature.orderedBones.get(lightBone).color;

                this.tmpColors[i * 4] = color.r;
                this.tmpColors[i * 4 + 1] = color.g;
                this.tmpColors[i * 4 + 2] = color.b;
                this.tmpColors[i * 4 + 3] = color.a;
            }
            else
            {
                this.tmpColors[i * 4] = 1F;
                this.tmpColors[i * 4 + 1] = 1F;
                this.tmpColors[i * 4 + 2] = 1F;
                this.tmpColors[i * 4 + 3] = 1F;
            }

            if (!allowBone)
            {
                this.tmpColors[i * 4 + 3] = 0F;
            }

            this.tmpBones[i] = lightBone;
        }

        this.processData(newVertices, newNormals);

        this.hasSemiTransparency = false;

        for (int i = 0; i < this.count; i++)
        {
            if (this.tmpColors[i * 4 + 3] < 1.0F)
            {
                this.hasSemiTransparency = true;
                break;
            }
        }

        /* Build per-triangle index lists for opaque/translucent */
        int triCount = this.count / 3;
        int[] opaqueIdx = new int[this.count];
        int[] transIdx = new int[this.count];
        int oi = 0;
        int ti = 0;

        for (int t = 0; t < triCount; t++)
        {
            int v0 = t * 3;
            int v1 = v0 + 1;
            int v2 = v0 + 2;

            float a0 = this.tmpColors[v0 * 4 + 3];
            float a1 = this.tmpColors[v1 * 4 + 3];
            float a2 = this.tmpColors[v2 * 4 + 3];

            boolean triTranslucent = (a0 < 0.999f) || (a1 < 0.999f) || (a2 < 0.999f);

            if (triTranslucent)
            {
                transIdx[ti++] = v0;
                transIdx[ti++] = v1;
                transIdx[ti++] = v2;
            }
            else
            {
                opaqueIdx[oi++] = v0;
                opaqueIdx[oi++] = v1;
                opaqueIdx[oi++] = v2;
            }
        }

        this.opaqueIndexCount = oi;
        this.translucentIndexCount = ti;
        this.cachedOpaqueIdx = java.util.Arrays.copyOf(opaqueIdx, oi);
        this.cachedTransIdx = java.util.Arrays.copyOf(transIdx, ti);

        int prevVAO = GL30.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        GL30.glBindVertexArray(this.vao);
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.opaqueIndexBuffer);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, this.cachedOpaqueIdx, GL15.GL_DYNAMIC_DRAW);

        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.translucentIndexBuffer);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, this.cachedTransIdx, GL15.GL_DYNAMIC_DRAW);
        GL30.glBindVertexArray(prevVAO);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.vertexBuffer);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, newVertices, GL15.GL_DYNAMIC_DRAW);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.normalBuffer);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, newNormals, GL15.GL_DYNAMIC_DRAW);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, this.colorBuffer);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, this.tmpColors, GL15.GL_DYNAMIC_DRAW);

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

    public void render(ShaderProgram shader, MatrixStack stack, float r, float g, float b, float a, StencilMap stencilMap, int light, int overlay)
    {
        if (a <= 0F)
        {
            return;
        }

        boolean hasShaders = BBSRendering.isIrisShadersEnabled();

        int currentVAO = GL30.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        boolean prevCullEnabled = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_CULL_FACE);
        int prevCullFace = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_CULL_FACE_MODE);
        boolean prevDepthMask = org.lwjgl.opengl.GL11.glGetBoolean(org.lwjgl.opengl.GL11.GL_DEPTH_WRITEMASK);

        ModelVAORenderer.setupUniforms(stack, shader, r, g, b, a);

        shader.bind();

        GL30.glBindVertexArray(this.vao);

        GL30.glEnableVertexAttribArray(Attributes.COLOR);
        GL30.glDisableVertexAttribArray(Attributes.LIGHTMAP_UV);
        GL30.glDisableVertexAttribArray(Attributes.OVERLAY_UV);

        GL30.glVertexAttribI2i(Attributes.OVERLAY_UV, overlay & '\uffff', overlay >> 16 & '\uffff');
        GL30.glVertexAttribI2i(Attributes.LIGHTMAP_UV, light & '\uffff', light >> 16 & '\uffff');

        GL30.glEnableVertexAttribArray(Attributes.POSITION);
        GL30.glEnableVertexAttribArray(Attributes.TEXTURE_UV);
        GL30.glEnableVertexAttribArray(Attributes.NORMAL);

        if (stencilMap != null) GL30.glEnableVertexAttribArray(Attributes.LIGHTMAP_UV);
        if (hasShaders) GL30.glEnableVertexAttribArray(Attributes.TANGENTS);
        if (hasShaders) GL30.glEnableVertexAttribArray(Attributes.MID_TEXTURE_UV);

        boolean globalTranslucent = a < 1.0F;
        boolean semiTransparent = globalTranslucent || this.hasSemiTransparency;

        com.mojang.blaze3d.systems.RenderSystem.enableCull();

        /* Opaque pass first (only if global alpha is 1) */
        if (!globalTranslucent && this.opaqueIndexCount > 0)
        {
            GL11.glCullFace(GL11.GL_BACK);

            boolean hasBoneTextures = false;

            for (BOBJBone bone : this.armature.orderedBones)
            {
                if (bone.texture != null)
                {
                    hasBoneTextures = true;
                    break;
                }
            }

            if (!hasBoneTextures)
            {
                GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.opaqueIndexBuffer);
                GL30.glDrawElements(GL30.GL_TRIANGLES, this.opaqueIndexCount, GL30.GL_UNSIGNED_INT, 0L);
            }
            else
            {
                int triCount = this.opaqueIndexCount / 3;
                int[] baseIdx = this.cachedOpaqueIdx;
                int[] indices = new int[this.opaqueIndexCount];
                int prevTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

                for (BOBJBone bone : this.armature.orderedBones)
                {
                    if (bone.texture == null)
                    {
                        continue;
                    }

                    int boneIndex = bone.index;
                    int countIdx = 0;

                    for (int t = 0; t < triCount; t++)
                    {
                        int i0 = baseIdx[t * 3];
                        int i1 = baseIdx[t * 3 + 1];
                        int i2 = baseIdx[t * 3 + 2];
                        int triBone = this.getTriangleBone(i0, i1, i2);

                        if (triBone == boneIndex)
                        {
                            indices[countIdx++] = i0;
                            indices[countIdx++] = i1;
                            indices[countIdx++] = i2;
                        }
                    }

                    if (countIdx > 0)
                    {
                        BBSModClient.getTextures().bindTexture(bone.texture);
                        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.opaqueIndexBuffer);
                        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, java.util.Arrays.copyOf(indices, countIdx), GL15.GL_DYNAMIC_DRAW);
                        GL30.glDrawElements(GL30.GL_TRIANGLES, countIdx, GL30.GL_UNSIGNED_INT, 0L);
                    }
                }

                int defaultCount = 0;

                for (int t = 0; t < triCount; t++)
                {
                    int i0 = baseIdx[t * 3];
                    int i1 = baseIdx[t * 3 + 1];
                    int i2 = baseIdx[t * 3 + 2];
                    int triBone = this.getTriangleBone(i0, i1, i2);
                    BOBJBone bone = triBone >= 0 && triBone < this.armature.orderedBones.size() ? this.armature.orderedBones.get(triBone) : null;

                    if (bone == null || bone.texture == null)
                    {
                        indices[defaultCount++] = i0;
                        indices[defaultCount++] = i1;
                        indices[defaultCount++] = i2;
                    }
                }

                if (defaultCount > 0)
                {
                    GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTexture);
                    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.opaqueIndexBuffer);
                    GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, java.util.Arrays.copyOf(indices, defaultCount), GL15.GL_DYNAMIC_DRAW);
                    GL30.glDrawElements(GL30.GL_TRIANGLES, defaultCount, GL30.GL_UNSIGNED_INT, 0L);
                }

                GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevTexture);
            }
        }

        /* Translucent pass */
        if (semiTransparent && this.translucentIndexCount > 0)
        {
            /* Sort translucent triangles back-to-front using current model-view */
            org.joml.Matrix4f mv = new org.joml.Matrix4f(com.mojang.blaze3d.systems.RenderSystem.getModelViewMatrix()).mul(stack.peek().getPositionMatrix());
            int triCount = this.translucentIndexCount / 3;
            Integer[] order = new Integer[triCount];
            for (int i = 0; i < triCount; i++) order[i] = i;

            org.joml.Vector4f vtx = new org.joml.Vector4f();
            java.util.Arrays.sort(order, (aIdx, bIdx) -> {
                int a0 = aIdx * 3;
                int a1 = a0 + 1;
                int a2 = a0 + 2;
                int ia0 = this.getTransIndex(a0);
                int ia1 = this.getTransIndex(a1);
                int ia2 = this.getTransIndex(a2);
                float az = centroidZ(ia0, ia1, ia2, this.tmpVertices, mv, vtx);

                int b0 = bIdx * 3;
                int b1 = b0 + 1;
                int b2 = b0 + 2;
                int ib0 = this.getTransIndex(b0);
                int ib1 = this.getTransIndex(b1);
                int ib2 = this.getTransIndex(b2);
                float bz = centroidZ(ib0, ib1, ib2, this.tmpVertices, mv, vtx);

                return Float.compare(az, bz); /* back-to-front (Z more positive is farther) */
            });

            int[] sortedTrans = new int[this.translucentIndexCount];
            int p = 0;
            for (int i = 0; i < triCount; i++)
            {
                int base = order[i] * 3;
                sortedTrans[p++] = this.getTransIndex(base);
                sortedTrans[p++] = this.getTransIndex(base + 1);
                sortedTrans[p++] = this.getTransIndex(base + 2);
            }

            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.translucentIndexBuffer);
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, sortedTrans, GL15.GL_DYNAMIC_DRAW);

            com.mojang.blaze3d.systems.RenderSystem.depthMask(false);

            GL11.glCullFace(GL11.GL_FRONT);
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, this.translucentIndexBuffer);
            GL30.glDrawElements(GL30.GL_TRIANGLES, this.translucentIndexCount, GL30.GL_UNSIGNED_INT, 0L);

            GL11.glCullFace(GL11.GL_BACK);
            GL30.glDrawElements(GL30.GL_TRIANGLES, this.translucentIndexCount, GL30.GL_UNSIGNED_INT, 0L);

            com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
        }

        GL30.glDisableVertexAttribArray(Attributes.COLOR);
        GL30.glEnableVertexAttribArray(Attributes.LIGHTMAP_UV);
        GL30.glEnableVertexAttribArray(Attributes.OVERLAY_UV);

        GL30.glDisableVertexAttribArray(Attributes.POSITION);
        GL30.glDisableVertexAttribArray(Attributes.TEXTURE_UV);
        GL30.glDisableVertexAttribArray(Attributes.NORMAL);

        if (stencilMap != null) GL30.glDisableVertexAttribArray(Attributes.LIGHTMAP_UV);
        if (hasShaders) GL30.glDisableVertexAttribArray(Attributes.TANGENTS);
        if (hasShaders) GL30.glDisableVertexAttribArray(Attributes.MID_TEXTURE_UV);

        shader.unbind();

        /* Restore GL state */
        if (prevCullEnabled) org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_CULL_FACE);
        else org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_CULL_FACE);
        org.lwjgl.opengl.GL11.glCullFace(prevCullFace);
        com.mojang.blaze3d.systems.RenderSystem.depthMask(prevDepthMask);

        /* Restore only the previous VAO; its own EBO binding is part of that VAO's state */
        GL30.glBindVertexArray(currentVAO);
    }

    private int getTransIndex(int i)
    {
        return (this.cachedTransIdx != null && i >= 0 && i < this.cachedTransIdx.length) ? this.cachedTransIdx[i] : i;
    }

    private static float centroidZ(int i0, int i1, int i2, float[] verts, org.joml.Matrix4f mv, org.joml.Vector4f tmp)
    {
        float z = 0f;
        tmp.set(verts[i0 * 3], verts[i0 * 3 + 1], verts[i0 * 3 + 2], 1f).mul(mv);
        z += tmp.z;
        tmp.set(verts[i1 * 3], verts[i1 * 3 + 1], verts[i1 * 3 + 2], 1f).mul(mv);
        z += tmp.z;
        tmp.set(verts[i2 * 3], verts[i2 * 3 + 1], verts[i2 * 3 + 2], 1f).mul(mv);
        z += tmp.z;
        return z / 3f;
    }

    private int getTriangleBone(int v0, int v1, int v2)
    {
        int b0 = v0 >= 0 && v0 < this.tmpBones.length ? this.tmpBones[v0] : -1;
        int b1 = v1 >= 0 && v1 < this.tmpBones.length ? this.tmpBones[v1] : -1;
        int b2 = v2 >= 0 && v2 < this.tmpBones.length ? this.tmpBones[v2] : -1;

        if (b0 == b1 || b0 == b2)
        {
            return b0;
        }

        if (b1 == b2)
        {
            return b1;
        }

        if (b0 != -1)
        {
            return b0;
        }

        if (b1 != -1)
        {
            return b1;
        }

        return b2;
    }
}
