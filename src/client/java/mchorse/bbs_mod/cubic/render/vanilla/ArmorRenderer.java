package mchorse.bbs_mod.cubic.render.vanilla;

import com.google.common.collect.Maps;
import mchorse.bbs_mod.forms.renderers.utils.RecolorVertexConsumer;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.cubic.model.ArmorType;
import mchorse.bbs_mod.forms.entities.IEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.trim.ArmorTrim;
import net.minecraft.item.equipment.EquipmentAsset;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

import java.util.Map;

public class ArmorRenderer
{
    private static final Map<String, Identifier> ARMOR_TEXTURE_CACHE = Maps.newHashMap();
    private final BipedEntityModel innerModel;
    private final BipedEntityModel outerModel;

    public ArmorRenderer(BipedEntityModel innerModel, BipedEntityModel outerModel, BakedModelManager bakery)
    {
        this.innerModel = innerModel;
        this.outerModel = outerModel;
    }

    public void renderArmorSlot(MatrixStack matrices, VertexConsumerProvider vertexConsumers, IEntity entity, EquipmentSlot armorSlot, ArmorType type, int light)
    {
        ItemStack itemStack = entity.getEquipmentStack(armorSlot);
        if (itemStack == null || itemStack.isEmpty()) return;
    }

    private ModelPart getPart(BipedEntityModel bipedModel, ArmorType type)
    {
        switch (type)
        {
            case HELMET -> {
                return bipedModel.head;
            }
            case CHEST, LEGGINGS -> {
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

        return bipedModel.head;
    }

    private void renderArmorParts(ModelPart part, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, ItemStack stack, boolean secondTextureLayer, float red, float green, float blue, String overlay)
    {
        VertexConsumer base = vertexConsumers.getBuffer(TexturedRenderLayers.getEntityCutout());
        VertexConsumer vertexConsumer = new RecolorVertexConsumer(base, new Color(red, green, blue, 1F));

        part.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);
    }

    private void renderTrim(ModelPart part, RegistryKey<EquipmentAsset> armorAssetKey, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, ArmorTrim trim, boolean leggings)
    {
        part.render(matrices, vertexConsumers.getBuffer(TexturedRenderLayers.getArmorTrims(trim.pattern().value().decal())), light, OverlayTexture.DEFAULT_UV);
    }

    private Identifier getTrimTexture(ArmorTrim trim, RegistryKey<EquipmentAsset> armorAssetKey, boolean leggings)
    {
        Identifier patternId = trim.pattern().value().assetId();
        String materialName = "iron";
        String suffix = leggings ? "_leggings" : "";

        return Identifier.of(patternId.getNamespace(), "trims/models/armor/" + patternId.getPath() + "_" + materialName + suffix);
    }

    private void renderGlint(ModelPart part, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light)
    {
        part.render(matrices, vertexConsumers.getBuffer(TexturedRenderLayers.getEntitySolid()), light, OverlayTexture.DEFAULT_UV);
    }

    private BipedEntityModel getModel(EquipmentSlot slot)
    {
        return this.usesInnerModel(slot) ? this.innerModel : this.outerModel;
    }

    private boolean usesInnerModel(EquipmentSlot slot)
    {
        return slot == EquipmentSlot.LEGS;
    }

    private Identifier getArmorTexture(ItemStack stack, boolean secondLayer, String overlay)
    {
        // Use default if not found
        String materialName = "unknown";
        
        // Try to get from components
        EquippableComponent equippable = stack.get(DataComponentTypes.EQUIPPABLE);
        if (equippable != null && equippable.assetId().isPresent())
        {
            materialName = equippable.assetId().get().getValue().getPath();
        }

        String id = "textures/entity/equipment/" + (secondLayer ? "humanoid_leggings" : "humanoid") + "/" + materialName + (overlay == null ? "" : "_" + overlay) + ".png";

        Identifier found = ARMOR_TEXTURE_CACHE.get(id);
        if (found == null)
        {
            found = Identifier.of("minecraft", id);
            ARMOR_TEXTURE_CACHE.put(id, found);
        }

        return found;
    }
}
