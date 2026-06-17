package mchorse.bbs_mod.cubic.render.vanilla;

import mchorse.bbs_mod.cubic.model.ArmorType;
import mchorse.bbs_mod.forms.entities.IEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EquipmentSlot;

import com.mojang.blaze3d.vertex.PoseStack;

public class ArmorRenderer
{
    public ArmorRenderer()
    {}

    public void renderArmorSlot(PoseStack matrices, MultiBufferSource vertexConsumers, IEntity entity, EquipmentSlot armorSlot, ArmorType type, int light)
    {
        // TODO 1.21.11: re-implement armor rendering with EquipmentModel + new trim/material APIs.
    }
}