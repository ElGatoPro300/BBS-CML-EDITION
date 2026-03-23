package mchorse.bbs_mod.cubic.model.loaders;

import mchorse.bbs_mod.cubic.data.animation.Animation;
import mchorse.bbs_mod.cubic.data.animation.AnimationPart;
import mchorse.bbs_mod.bobj.BOBJKeyframe;
import mchorse.bbs_mod.bobj.BOBJBone;
import mchorse.bbs_mod.bobj.BOBJGroup;
import mchorse.bbs_mod.bobj.BOBJChannel;
import mchorse.bbs_mod.bobj.BOBJLoader;
import mchorse.bbs_mod.bobj.BOBJAction;
import mchorse.bbs_mod.bobj.BOBJArmature;
import mchorse.bbs_mod.bobj.BOBJLoader.BOBJData;
import mchorse.bbs_mod.bobj.BOBJLoader.BOBJMesh;
import mchorse.bbs_mod.bobj.BOBJLoader.CompiledData;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.data.animation.Animations;
import mchorse.bbs_mod.cubic.model.ModelManager;
import mchorse.bbs_mod.cubic.model.bobj.BOBJModel;
import mchorse.bbs_mod.cubic.model.fbx.FBXConverter;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.math.Constant;
import mchorse.bbs_mod.math.molang.expressions.MolangExpression;
import mchorse.bbs_mod.math.molang.expressions.MolangValue;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import org.joml.Vector2d;
import org.joml.Vector3f;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.Assimp;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FBXModelLoader implements IModelLoader
{
    @Override
    public ModelInstance load(String id, ModelManager models, Link model, Collection<Link> links, MapType config)
    {
        Link fbxLink = null;

        for (Link link : links)
        {
            if (link.path.endsWith(".fbx"))
            {
                fbxLink = link;
                break;
            }
        }

        if (fbxLink == null)
        {
            return null;
        }

        try
        {
            InputStream stream = models.provider.getAsset(fbxLink);

            if (stream == null)
            {
                return null;
            }

            byte[] bytes = stream.readAllBytes();
            stream.close();

            java.nio.ByteBuffer buffer = org.lwjgl.BufferUtils.createByteBuffer(bytes.length);
            buffer.put(bytes);
            buffer.flip();

            org.lwjgl.assimp.AIPropertyStore store = Assimp.aiCreatePropertyStore();
            Assimp.aiSetImportPropertyInteger(store, Assimp.AI_CONFIG_IMPORT_FBX_PRESERVE_PIVOTS, 0);
            Assimp.aiSetImportPropertyFloat(store, Assimp.AI_CONFIG_GLOBAL_SCALE_FACTOR_KEY, 1.0f);

            AIScene scene = Assimp.aiImportFileFromMemoryWithProperties(buffer,
                    Assimp.aiProcess_Triangulate |
                            Assimp.aiProcess_FlipUVs |
                            Assimp.aiProcess_LimitBoneWeights |
                            Assimp.aiProcess_JoinIdenticalVertices |
                            Assimp.aiProcess_GenSmoothNormals |
                            Assimp.aiProcess_OptimizeGraph |
                            Assimp.aiProcess_PopulateArmatureData,
                    (java.nio.ByteBuffer) null,
                    store);

            if (scene == null)
            {
                System.err.println("Error loading FBX model: " + Assimp.aiGetErrorString());
                return null;
            }

            BOBJData data;
            try
            {
                data = FBXConverter.convert(scene);
            } finally
            {
                Assimp.aiReleaseImport(scene);
            }

            data.initiateArmatures();

            CompiledData compiledData = this.compile(data);

            BOBJArmature armature = null;
            if (!data.armatures.isEmpty())
            {
                armature = data.armatures.values().iterator().next();
            }

            if (armature == null)
            {
                armature = new BOBJArmature("Armature");
                armature.initArmature();
            }

            BOBJModel bobjModel = new BOBJModel(armature, compiledData, false);

            Animations animations = new Animations(models.parser);

            for (BOBJAction action : data.actions.values())
            {
                Animation animation = new Animation(action.name, models.parser);
                animation.setLength(action.getDuration() / 20.0);

                for (BOBJGroup group : action.groups.values())
                {
                    AnimationPart part = new AnimationPart(models.parser);

                    for (BOBJChannel channel : group.channels)
                    {
                        KeyframeChannel<MolangExpression> targetChannel = null;

                        switch (channel.path)
                        {
                            case "location.x": targetChannel = part.x; break;
                            case "location.y": targetChannel = part.y; break;
                            case "location.z": targetChannel = part.z; break;
                            case "rotation.x": targetChannel = part.rx; break;
                            case "rotation.y": targetChannel = part.ry; break;
                            case "rotation.z": targetChannel = part.rz; break;
                            case "scale.x": targetChannel = part.sx; break;
                            case "scale.y": targetChannel = part.sy; break;
                            case "scale.z": targetChannel = part.sz; break;
                        }

                        if (targetChannel != null)
                        {
                            for (BOBJKeyframe kf : channel.keyframes)
                            {
                                targetChannel.insert(kf.frame, new MolangValue(models.parser, new Constant(kf.value)));
                            }
                        }
                    }

                    animation.parts.put(group.name, part);
                }

                animations.add(animation);
            }

            Link textureLink = null;

            /*
             Try to find texture from mesh data first
             */
            if (!data.meshes.isEmpty() && data.meshes.get(0) instanceof FBXConverter.FBXMesh)
            {
                FBXConverter.FBXMesh mesh = (FBXConverter.FBXMesh) data.meshes.get(0);

                if (mesh.texture != null && !mesh.texture.isEmpty())
                {
                    Link specificLink = model.combine(mesh.texture);
                    if (links.contains(specificLink))
                    {
                        textureLink = specificLink;
                    } else
                    {
                        for (Link l : links)
                        {
                            if (l.path.endsWith(mesh.texture))
                            {
                                textureLink = l;
                                break;
                            }
                        }
                    }
                }
            }

            if (textureLink == null)
            {
                for (Link l : links)
                {
                String path = l.path.toLowerCase();

                if (path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg"))
                    {
                        textureLink = l;
                        break;
                    }
                }
            }

            ModelInstance modelInstance = new ModelInstance(id, bobjModel, animations, textureLink);
            modelInstance.applyConfig(config);
            return modelInstance;

        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    private CompiledData compile(BOBJData data)
    {
        int totalVertices = 0;
        for (BOBJMesh mesh : data.meshes) {
            for (BOBJLoader.Face face : mesh.faces)
            {
                totalVertices += face.idxGroups.length;
            }
        }

        float[] pos = new float[totalVertices * 3];
        float[] tex = new float[totalVertices * 2];
        float[] norm = new float[totalVertices * 3];
        float[] weights = new float[totalVertices * 4];
        int[] bones = new int[totalVertices * 4];
        int[] indices = new int[totalVertices];


        int vIndex = 0;  /** >> Vertex index */
        int wIndex = 0;  /** >> Weight/Bone index (x4) */
        int pIndex = 0;  /** >> Position/Normal index (x3) */
        int tIndex = 0;  /** >> Texture index (x2) */

        for (BOBJMesh mesh : data.meshes)
        {
            for (BOBJLoader.Face face : mesh.faces)
            {
                for (BOBJLoader.IndexGroup group : face.idxGroups)
                {
                    BOBJLoader.Vertex v = data.vertices.get(group.idxPos);
                    Vector2d t = data.textures.get(group.idxTextCoord);
                    Vector3f n = data.normals.get(group.idxVecNormal);

                    pos[pIndex] = v.x; pos[pIndex+1] = v.y; pos[pIndex+2] = v.z;
                    norm[pIndex] = n.x; norm[pIndex+1] = n.y; norm[pIndex+2] = n.z;
                    pIndex += 3;

                    tex[tIndex] = (float) t.x; tex[tIndex+1] = (float) t.y;
                    tIndex += 2;

                    if (v.weights.isEmpty())
                    {
                        weights[wIndex] = 1.0f; bones[wIndex] = 0;
                        weights[wIndex+1] = 0.0f; bones[wIndex+1] = -1;
                        weights[wIndex+2] = 0.0f; bones[wIndex+2] = -1;
                        weights[wIndex+3] = 0.0f; bones[wIndex+3] = -1;
                    } else
                    {
                        for (int i = 0; i < 4; i++)
                        {
                            if (i < v.weights.size())
                            {
                                BOBJLoader.Weight w = v.weights.get(i);
                                weights[wIndex+i] = w.factor;
                                BOBJBone bone = mesh.armature != null ? mesh.armature.bones.get(w.name) : null;
                                bones[wIndex+i] = (bone == null ? 0 : bone.index);
                            } else
                            {
                                weights[wIndex+i] = 0f;
                                bones[wIndex+i] = -1;
                            }
                        }
                    }
                    wIndex += 4;
                    indices[vIndex] = vIndex;
                    vIndex++;
                }
            }
        }

        return new CompiledData(
                pos, tex, norm, weights, bones, indices,
                null
        );
    }
}
