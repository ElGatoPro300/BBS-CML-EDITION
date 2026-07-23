package mchorse.bbs_mod.cubic.render.vanilla;

import mchorse.bbs_mod.cubic.model.ArmorType;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.renderers.utils.RecolorVertexConsumer;
import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.DyeableArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.trim.ArmorTrim;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class ArmorRenderer
{
    private static final Map<String, Identifier> ARMOR_TEXTURE_CACHE = new HashMap<>();
    private final BipedEntityModel innerModel;
    private final BipedEntityModel outerModel;

    public ArmorRenderer(BipedEntityModel innerModel, BipedEntityModel outerModel, BakedModelManager bakery)
    {
        this.innerModel = innerModel;
        this.outerModel = outerModel;
    }

    public void renderArmorSlot(MatrixStack matrices, VertexConsumerProvider vertexConsumers, IEntity entity, EquipmentSlot slot, ArmorType type, int light)
    {
        ItemStack itemStack = entity.getEquipmentStack(slot);

        if (itemStack == null || itemStack.isEmpty())
        {
            return;
        }

        Item item = itemStack.getItem();

        if (item instanceof ArmorItem armorItem)
        {
            if (armorItem.getSlotType() == slot)
            {
                BipedEntityModel bipedModel = this.usesInnerModel(slot) ? this.innerModel : this.outerModel;
                ModelPart part = this.getPart(bipedModel, type);

                if (part == null)
                {
                    return;
                }

                boolean innerModel = this.usesInnerModel(slot);

                bipedModel.setVisible(true);

                part.pivotX = part.pivotY = part.pivotZ = 0F;
                part.pitch = part.yaw = part.roll = 0F;
                part.xScale = part.yScale = part.zScale = 1F;

                if (armorItem instanceof DyeableArmorItem dyeable && dyeable.hasColor(itemStack))
                {
                    int color = dyeable.getColor(itemStack);
                    float r = (float)(color >> 16 & 255) / 255.0F;
                    float g = (float)(color >> 8 & 255) / 255.0F;
                    float b = (float)(color & 255) / 255.0F;

                    this.renderArmorParts(part, matrices, vertexConsumers, light, armorItem, innerModel, r, g, b, null);
                    this.renderArmorParts(part, matrices, vertexConsumers, light, armorItem, innerModel, 1F, 1F, 1F, "overlay");
                }
                else
                {
                    this.renderArmorParts(part, matrices, vertexConsumers, light, armorItem, innerModel, 1F, 1F, 1F, null);
                }

                ArmorTrim trim = ArmorTrim.getTrim(MinecraftClient.getInstance().world.getRegistryManager(), itemStack, true).orElse(null);
                if (trim != null)
                {
                    this.renderTrim(part, armorItem.getMaterial(), matrices, vertexConsumers, light, trim, innerModel);
                }

                if (itemStack.hasGlint())
                {
                    this.renderGlint(part, matrices, vertexConsumers, light);
                }
            }
        }
    }

    private ModelPart getPart(BipedEntityModel bipedModel, ArmorType type)
    {
        switch (type)
        {
            case HELMET -> {
                return bipedModel.head;
            }
            case CHEST -> {
                return bipedModel.body;
            }
            case LEGGINGS -> {
                return bipedModel.body;
            }
            case LEFT_ARM -> {
                return bipedModel.leftArm;
            }
            case RIGHT_ARM -> {
                return bipedModel.rightArm;
            }
            case LEFT_LEG, LEFT_BOOT -> {
                return bipedModel.leftLeg;
            }
            case RIGHT_LEG, RIGHT_BOOT -> {
                return bipedModel.rightLeg;
            }
        }

        return null;
    }

    private void renderArmorParts(ModelPart part, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, ArmorItem armorItem, boolean secondLayer, float r, float g, float b, String overlay)
    {
        Identifier texture = this.getArmorTexture(armorItem, secondLayer, overlay);
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getArmorCutoutNoCull(texture));

        part.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV, r, g, b, 1F);
    }

    private void renderTrim(ModelPart part, ArmorMaterial armorMaterial, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, ArmorTrim trim, boolean innerModel)
    {
        BakedModelManager bakedModelManager = MinecraftClient.getInstance().getBakedModelManager();
        Sprite sprite = bakedModelManager.getAtlas(TexturedRenderLayers.ARMOR_TRIMS_ATLAS_TEXTURE).getSprite(innerModel ? trim.getLeggingsModelId(armorMaterial) : trim.getGenericModelId(armorMaterial));

        VertexConsumer vertexConsumer = sprite.getTextureSpecificVertexConsumer(vertexConsumers.getBuffer(TexturedRenderLayers.getArmorTrims(trim.getPattern().value().decal())));
        part.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV, 1F, 1F, 1F, 1F);
    }

    private void renderGlint(ModelPart part, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light)
    {
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getArmorEntityGlint());
        part.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV, 1F, 1F, 1F, 1F);
    }

    private boolean usesInnerModel(EquipmentSlot slot)
    {
        return slot == EquipmentSlot.LEGS;
    }

    private Identifier getArmorTexture(ArmorItem item, boolean secondLayer, String overlay)
    {
        String materialName = item.getMaterial().getName();
        String id = "textures/models/armor/" + materialName + "_layer_" + (secondLayer ? 2 : 1) + (overlay == null ? "" : "_" + overlay) + ".png";

        Identifier found = ARMOR_TEXTURE_CACHE.get(id);
        if (found == null)
        {
            found = new Identifier("minecraft", id);
            ARMOR_TEXTURE_CACHE.put(id, found);
        }

        return found;
    }
}