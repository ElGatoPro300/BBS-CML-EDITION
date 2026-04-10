package mchorse.bbs_mod.cubic.render.vanilla;

import com.mojang.blaze3d.vertex.PoseStack;
import mchorse.bbs_mod.cubic.model.ArmorType;
import mchorse.bbs_mod.forms.entities.IEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EquipmentSlot;

public class ArmorRenderer
{
    public ArmorRenderer()
    {}

    public void renderArmorSlot(PoseStack matrices, MultiBufferSource vertexConsumers, IEntity entity, EquipmentSlot armorSlot, ArmorType type, int light)
    {
        // TODO 1.21.11: re-implement armor rendering with EquipmentModel + new trim/material APIs.
    }
}