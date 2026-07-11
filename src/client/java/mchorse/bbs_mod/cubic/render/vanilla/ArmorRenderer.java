package mchorse.bbs_mod.cubic.render.vanilla;

import mchorse.bbs_mod.cubic.model.ArmorType;
import mchorse.bbs_mod.forms.entities.IEntity;

import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;

/**
 * Renders vanilla armor models over an {@link IEntity}'s biped model parts.
 *
 * <p>1.21.11 moved all armor rendering off ModelPart/BipedEntityModel entirely: it now goes through the new
 * equipment-render pipeline (package net.minecraft.client.render.entity.equipment), driven by
 * OrderedRenderCommandQueue. ModelPart also lost its
 * mutable pose fields ({@code pivotX/Y/Z}, {@code pitch/yaw/roll}, {@code xScale/yScale/zScale}) and its
 * {@code render(MatrixStack, VertexConsumer, int, int)} method (only {@code forEachCuboid(MatrixStack,
 * CuboidConsumer)} remains) — there is no direct replacement for the old "grab a part, zero its pose, render
 * it with a recolored VertexConsumer" approach used here. {@code BakedModelManager} also lost
 * {@code getAtlas(Identifier)} (the armor-trims sprite atlas lookup this class used to do in its constructor).
 *
 * <p>Faithfully reproducing the new equipment-model pipeline for a detached (non-LivingEntity) biped model is
 * a large, separate undertaking. Until that is done, this renderer intentionally draws nothing (see
 * {@link #renderArmorSlot}) so forms with equipped armor simply show no armor layer instead of crashing/
 * failing to compile. RenderLayers#armorCutoutNoCull(Identifier) /
 * #armorEntityGlint() and TexturedRenderLayers#getArmorTrims(boolean) are confirmed direct replacements
 * for the old RenderLayer statics, for whenever this gets revisited.</p>
 */
public class ArmorRenderer
{
    private final BipedEntityModel<?> innerModel;
    private final BipedEntityModel<?> outerModel;

    public ArmorRenderer(BipedEntityModel<?> innerModel, BipedEntityModel<?> outerModel, BakedModelManager bakery)
    {
        this.innerModel = innerModel;
        this.outerModel = outerModel;
    }

    public void renderArmorSlot(MatrixStack matrices, VertexConsumerProvider vertexConsumers, IEntity entity, EquipmentSlot armorSlot, ArmorType type, int light)
    {
        /* See class comment: armor rendering is stubbed out pending a port to the 1.21.11
         * EquipmentRenderer/OrderedRenderCommandQueue pipeline. */
    }
}
