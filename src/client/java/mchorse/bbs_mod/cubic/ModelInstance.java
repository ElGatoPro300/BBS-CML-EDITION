package mchorse.bbs_mod.cubic;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.bobj.BOBJBone;
import mchorse.bbs_mod.cubic.animation.ActionsConfig;
import mchorse.bbs_mod.cubic.data.animation.Animations;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.model.ArmorSlot;
import mchorse.bbs_mod.cubic.model.ArmorType;
import mchorse.bbs_mod.cubic.model.IKChainConfig;
import mchorse.bbs_mod.cubic.model.View;
import mchorse.bbs_mod.cubic.model.bobj.BOBJModel;
import mchorse.bbs_mod.cubic.physics.PhysBoneDefinition;
import mchorse.bbs_mod.cubic.render.CubicCubeRenderer;
import mchorse.bbs_mod.cubic.render.CubicMatrixRenderer;
import mchorse.bbs_mod.cubic.render.CubicRenderer;
import mchorse.bbs_mod.cubic.render.CubicVAOBuilderRenderer;
import mchorse.bbs_mod.cubic.render.CubicVAORenderer;
import mchorse.bbs_mod.cubic.render.vao.BOBJModelVAO;
import mchorse.bbs_mod.cubic.render.vao.ModelVAO;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.obj.shapes.ShapeKeys;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.pose.Pose;
import mchorse.bbs_mod.utils.resources.LinkUtils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;

import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.Tessellator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ModelInstance implements IModelInstance
{
    public final String id;
    public IModel model;
    public Animations animations;
    public Link texture;
    public int color = Colors.WHITE;

    /* Model's additional properties */
    public String poseGroup;
    public boolean procedural;
    public boolean culling = true;
    public boolean onCpu;
    public String anchorGroup = "";

    public View view;

    public Vector3f scale = new Vector3f(1F);
    public float uiScale = 1F;
    public Pose sneakingPose = new Pose();
    public Pose parts = new Pose();

    public List<ArmorSlot> itemsMain = new ArrayList<>();
    public List<ArmorSlot> itemsOff = new ArrayList<>();
    public List<PhysBoneDefinition> physBones = new ArrayList<>();
    public List<IKChainConfig> ikChains = new ArrayList<>();
    public Map<String, String> flippedParts = new HashMap<>();
    public Map<ArmorType, ArmorSlot> armorSlots = new HashMap<>();

    public ArmorSlot fpMain;
    public ArmorSlot fpOffhand;

    public ArmorSlot itemsMainTransform = new ArmorSlot("items_main_transform");
    public ArmorSlot itemsOffTransform = new ArmorSlot("items_off_transform");
    public ActionsConfig actions = new ActionsConfig();

    private Map<ModelGroup, ModelVAO> vaos = new HashMap<>();

    public ModelInstance(String id, IModel model, Animations animations, Link texture)
    {
        this.id = id;
        this.model = model;
        this.animations = animations;
        this.texture = texture;

        this.poseGroup = id;
    }

    @Override
    public IModel getModel()
    {
        return this.model;
    }

    @Override
    public Pose getSneakingPose()
    {
        return this.sneakingPose;
    }

    @Override
    public Animations getAnimations()
    {
        return this.animations;
    }

    @Override
    public String getHeadBone()
    {
        return this.view == null ? "head" : this.view.headBone;
    }

    @Override
    public List<PhysBoneDefinition> getPhysBones()
    {
        return this.physBones;
    }

    public Map<ModelGroup, ModelVAO> getVaos()
    {
        return this.vaos;
    }

    public String getAnchor()
    {
        String anchor = this.model.getAnchor();

        if (this.anchorGroup.isEmpty() && !anchor.isEmpty())
        {
            return anchor;
        }

        return this.anchorGroup;
    }

    public void applyConfig(MapType config)
    {
        if (config == null)
        {
            return;
        }

        this.procedural = config.getBool("procedural", this.procedural);
        this.culling = config.getBool("culling", this.culling);
        this.onCpu = config.getBool("on_cpu", this.onCpu);
        this.poseGroup = config.getString("pose_group", this.poseGroup);
        if (this.poseGroup == null || this.poseGroup.isEmpty())
        {
            this.poseGroup = this.id;
        }

        if (config.has("texture"))
        {
            this.texture = LinkUtils.create(config.get("texture"));
        }
        if (config.has("color")) this.color = config.getInt("color");
        if (config.has("items_main"))
        {
            this.itemsMain.clear();

            ListType list = config.get("items_main").asList();

            for (BaseType type : list)
            {
                ArmorSlot slot = new ArmorSlot(String.valueOf(this.itemsMain.size()));

                slot.fromData(type);
                this.itemsMain.add(slot);
            }
        }
        if (config.has("phys_bones", BaseType.TYPE_LIST))
        {
            this.physBones.clear();

            ListType list = config.get("phys_bones").asList();

            for (BaseType type : list)
            {
                if (!type.isMap())
                {
                    continue;
                }

                PhysBoneDefinition definition = new PhysBoneDefinition();

                definition.fromData(type.asMap());
                this.physBones.add(definition);
            }
        }
        if (config.has("items_off"))
        {
            this.itemsOff.clear();

            ListType list = config.get("items_off").asList();

            for (BaseType type : list)
            {
                ArmorSlot slot = new ArmorSlot(String.valueOf(this.itemsOff.size()));

                slot.fromData(type);
                this.itemsOff.add(slot);
            }
        }
        if (config.has("ui_scale")) this.uiScale = config.getFloat("ui_scale");
        if (config.has("scale")) this.scale = DataStorageUtils.vector3fFromData(config.getList("scale"), new Vector3f(1F));
        if (config.has("sneaking_pose", BaseType.TYPE_MAP))
        {
            this.sneakingPose = new Pose();
            this.sneakingPose.fromData(config.getMap("sneaking_pose"));
        }
        if (config.has("parts", BaseType.TYPE_MAP))
        {
            this.parts = new Pose();
            this.parts.fromData(config.getMap("parts"));
        }
        if (config.has("anchor")) this.anchorGroup = config.getString("anchor");
        if (config.has("flipped_parts"))
        {
            MapType map = config.getMap("flipped_parts");

            for (String key : map.keys())
            {
                String string = map.getString(key);

                if (!string.trim().isEmpty())
                {
                    this.flippedParts.put(key, string);
                }
            }
        }
        if (config.has("armor_slots"))
        {
            MapType map = config.getMap("armor_slots");

            for (String key : map.keys())
            {
                try
                {
                    ArmorType type = ArmorType.valueOf(key.toUpperCase());
                    ArmorSlot slot = new ArmorSlot(key);

                    slot.fromData(map.getMap(key));
                    this.armorSlots.put(type, slot);
                }
                catch (Exception e)
                {}
            }
        }
        if (config.has("fp_main"))
        {
            this.fpMain = new ArmorSlot("fp_main");
            this.fpMain.fromData(config.get("fp_main"));
        }
        if (config.has("fp_offhand"))
        {
            this.fpOffhand = new ArmorSlot("fp_offhand");
            this.fpOffhand.fromData(config.get("fp_offhand"));
        }
        if (config.has("items_main_transform"))
        {
            this.itemsMainTransform = new ArmorSlot("items_main_transform");
            this.itemsMainTransform.fromData(config.get("items_main_transform"));
        }
        if (config.has("items_off_transform"))
        {
            this.itemsOffTransform = new ArmorSlot("items_off_transform");
            this.itemsOffTransform.fromData(config.get("items_off_transform"));
        }

        if (config.has("animations", BaseType.TYPE_MAP))
        {
            this.actions = new ActionsConfig();
            this.actions.fromData(config.getMap("animations"));
        }
        else
        {
            this.actions = new ActionsConfig();
        }

        /* Optional look-at configuration */
        if (config.has("look_at", BaseType.TYPE_MAP))
        {
            this.view = new View();

            this.view.fromData(config.getMap("look_at"));
        }

        /* IK chains */
        if (config.has("ik_chains", BaseType.TYPE_LIST))
        {
            this.ikChains.clear();

            ListType list = config.get("ik_chains").asList();

            for (int i = 0; i < list.size(); i++)
            {
                BaseType type = list.get(i);

                if (!type.isMap())
                {
                    continue;
                }

                IKChainConfig chain = new IKChainConfig(String.valueOf(i));

                chain.fromData(type);
                this.ikChains.add(chain);
            }
        }
    }

    public MapType toConfig()
    {
        MapType config = new MapType();

        if (this.procedural) config.putBool("procedural", true);
        if (!this.culling) config.putBool("culling", false);
        if (this.onCpu) config.putBool("on_cpu", true);
        if (!this.poseGroup.equals(this.id)) config.putString("pose_group", this.poseGroup);
        if (!this.anchorGroup.isEmpty()) config.putString("anchor", this.anchorGroup);

        if (this.texture != null) config.put("texture", LinkUtils.toData(this.texture));
        if (this.color != Colors.WHITE) config.putInt("color", this.color);

        if (!this.itemsMain.isEmpty())
        {
            ListType list = new ListType();

            for (ArmorSlot slot : this.itemsMain)
            {
                BaseType data = slot.toData();

                if (data != null)
                {
                    list.add(data);
                }
            }

            if (!list.isEmpty())
            {
                config.put("items_main", list);
            }
        }

        if (!this.itemsOff.isEmpty())
        {
            ListType list = new ListType();

            for (ArmorSlot slot : this.itemsOff)
            {
                BaseType data = slot.toData();

                if (data != null)
                {
                    list.add(data);
                }
            }

            if (!list.isEmpty())
            {
                config.put("items_off", list);
            }
        }

        if (this.uiScale != 1F) config.putFloat("ui_scale", this.uiScale);
        if (this.scale.x != 1F || this.scale.y != 1F || this.scale.z != 1F)
        {
            config.put("scale", DataStorageUtils.vector3fToData(this.scale));
        }

        if (this.sneakingPose != null && !this.sneakingPose.transforms.isEmpty())
        {
            config.put("sneaking_pose", this.sneakingPose.toData());
        }

        if (this.parts != null && !this.parts.transforms.isEmpty())
        {
            config.put("parts", this.parts.toData());
        }

        if (!this.flippedParts.isEmpty())
        {
            MapType map = new MapType();

            for (Map.Entry<String, String> entry : this.flippedParts.entrySet())
            {
                map.putString(entry.getKey(), entry.getValue());
            }

            config.put("flipped_parts", map);
        }

        if (!this.armorSlots.isEmpty())
        {
            MapType map = new MapType();

            for (Map.Entry<ArmorType, ArmorSlot> entry : this.armorSlots.entrySet())
            {
                map.put(entry.getKey().name().toLowerCase(), entry.getValue().toData());
            }

            config.put("armor_slots", map);
        }

        if (this.fpMain != null) config.put("fp_main", this.fpMain.toData());
        if (this.fpOffhand != null) config.put("fp_offhand", this.fpOffhand.toData());
        if (this.itemsMainTransform != null) config.put("items_main_transform", this.itemsMainTransform.toData());
        if (this.itemsOffTransform != null) config.put("items_off_transform", this.itemsOffTransform.toData());

        if (this.view != null)
        {
            MapType lookAt = new MapType();

            this.view.toData(lookAt);
            config.put("look_at", lookAt);
        }

        if (!this.physBones.isEmpty())
        {
            ListType list = new ListType();

            for (PhysBoneDefinition definition : this.physBones)
            {
                MapType map = new MapType();

                definition.toData(map);
                list.add(map);
            }

            config.put("phys_bones", list);
        }

        if (this.actions != null && !this.actions.geckoAnimations.isDefault())
        {
            config.put("animations", this.actions.toData());
        }

        /* IK chains */
        if (!this.ikChains.isEmpty())
        {
            ListType ikList = new ListType();

            for (IKChainConfig chain : this.ikChains)
            {
                ikList.add(chain.toData());
            }

            config.put("ik_chains", ikList);
        }

        return config;
    }
    public ModelInstance copy()
    {
        ModelInstance copy = new ModelInstance(this.id, this.model.copy(), this.animations, this.texture);

        copy.poseGroup = this.poseGroup;
        copy.procedural = this.procedural;
        copy.culling = this.culling;
        copy.onCpu = this.onCpu;
        copy.anchorGroup = this.anchorGroup;
        if (this.view != null)
        {
            MapType lookAt = new MapType();

            this.view.toData(lookAt);
            copy.view = new View();
            copy.view.fromData(lookAt);
        }

        copy.scale.set(this.scale);
        copy.uiScale = this.uiScale;
        copy.sneakingPose = this.sneakingPose.copy();
        copy.parts = this.parts.copy();
        copy.color = this.color;

        for (ArmorSlot slot : this.itemsMain) copy.itemsMain.add(slot.copy());
        for (ArmorSlot slot : this.itemsOff) copy.itemsOff.add(slot.copy());
        for (PhysBoneDefinition definition : this.physBones) copy.physBones.add(definition.copy());
        copy.flippedParts.putAll(this.flippedParts);

        if (this.fpMain != null) copy.fpMain = this.fpMain.copy();
        if (this.fpOffhand != null) copy.fpOffhand = this.fpOffhand.copy();
        if (this.itemsMainTransform != null) copy.itemsMainTransform = this.itemsMainTransform.copy();
        if (this.itemsOffTransform != null) copy.itemsOffTransform = this.itemsOffTransform.copy();

        for (Map.Entry<ArmorType, ArmorSlot> entry : this.armorSlots.entrySet())
        {
            copy.armorSlots.put(entry.getKey(), entry.getValue().copy());
        }

        copy.actions.copy(this.actions);

        return copy;
    }

    public void setup()
    {
        if (this.model instanceof BOBJModel model)
        {
            MinecraftClient.getInstance().execute(model::setup);
        }

        /* VAOs should be only generated if there are no shape keys */
        if (!this.model.getShapeKeys().isEmpty())
        {
            return;
        }

        if (this.model instanceof Model model && !this.onCpu)
        {
            MinecraftClient.getInstance().execute(() ->
            {
                CubicRenderer.processRenderModel(new CubicVAOBuilderRenderer(this.vaos), null, new MatrixStack(), model);
            });
        }
    }

    public boolean isVAORendered()
    {
        return !this.vaos.isEmpty() || this.model instanceof BOBJModel;
    }

    public void delete()
    {
        for (ModelVAO value : this.vaos.values())
        {
            value.delete();
        }

        this.vaos.clear();
    }

    /* Rendering */

    public void fillStencilMap(StencilMap stencilMap, ModelForm form)
    {
        if (this.model instanceof Model model)
        {
            for (ModelGroup group : model.getOrderedGroups())
            {
                stencilMap.addPicking(form, group.id);
            }
        }
        else if (this.model instanceof BOBJModel model)
        {
            for (BOBJBone orderedBone : model.getArmature().orderedBones)
            {
                stencilMap.addPicking(form, orderedBone.name);
            }
        }
    }

    public void captureMatrices(MatrixCache bones)
    {
        if (this.model instanceof Model model)
        {
            MatrixStack stack = new MatrixStack();
            CubicMatrixRenderer renderer = new CubicMatrixRenderer(model);

            CubicRenderer.processRenderModel(renderer, null, stack, model);

            for (ModelGroup group : model.getAllGroups())
            {
                Matrix4f matrix = new Matrix4f(renderer.matrices.get(group.index));
                Matrix4f origin = new Matrix4f(renderer.origins.get(group.index));

                matrix.translate(
                    group.initial.translate.x / 16,
                    group.initial.translate.y / 16,
                    group.initial.translate.z / 16
                );
                matrix.rotateY(MathUtils.PI);
                origin.translate(
                    group.initial.translate.x / 8192,
                    group.initial.translate.y / 8192,
                    group.initial.translate.z / 8192
                );
                origin.rotateY(MathUtils.PI);
                
                bones.put(group.id, matrix, origin);
            }
        }
        else if (this.model instanceof BOBJModel model)
        {
            model.getArmature().setupMatrices();

            for (BOBJBone orderedBone : model.getArmature().orderedBones)
            {
                Matrix4f matrix = new Matrix4f();
                Matrix4f origin = new Matrix4f();

                matrix.rotateY(MathUtils.PI).mul(orderedBone.mat);
                origin.rotateY(MathUtils.PI).mul(orderedBone.originMat);
                bones.put(orderedBone.name, matrix, origin);
            }
        }
    }

    public void render(MatrixStack stack, Supplier<ShaderProgram> program, Color color, int light, int overlay, StencilMap stencilMap, ShapeKeys keys, Link defaultTexture)
    {
        if (this.model instanceof Model model)
        {
            boolean isVao = this.isVAORendered();
            CubicCubeRenderer renderProcessor = isVao
                ? new CubicVAORenderer(program.get(), this, light, overlay, stencilMap, keys, defaultTexture)
                : new CubicCubeRenderer(light, overlay, stencilMap, keys);

            Color c = new Color().set(this.color);

            renderProcessor.setColor(color.r * c.r, color.g * c.g, color.b * c.b, color.a * c.a);

            if (isVao)
            {
                CubicRenderer.processRenderModel(renderProcessor, null, stack, model);
            }
            else
            {
                BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);

                CubicRenderer.processRenderModel(renderProcessor, builder, stack, model);

                try
                {
                    RenderLayers.solid().draw(builder.end());
                }
                catch (IllegalStateException e)
                {
                    
                }
            }
        }
        else if (this.model instanceof BOBJModel model)
        {
            BOBJModelVAO vao = model.getVao();

            if (vao != null)
            {
                stack.push();
                stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180F));

                vao.armature.setupMatrices();
                vao.updateMesh(stencilMap);
                vao.render(program.get(), stack, color.r, color.g, color.b, color.a, stencilMap, light, overlay, this.texture);

                stack.pop();
            }
        }
    }
}

