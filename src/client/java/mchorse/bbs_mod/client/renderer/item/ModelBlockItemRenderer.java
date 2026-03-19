package mchorse.bbs_mod.client.renderer.item;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.serialization.MapCodec;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.blocks.entities.ModelProperties;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.values.ModelTransformMode;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3fc;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

public class ModelBlockItemRenderer implements SpecialModelRenderer<ItemStack>
{
    private Map<ItemStack, Item> map = new HashMap<>();

    public void update()
    {
        Iterator<Item> it = this.map.values().iterator();

        while (it.hasNext())
        {
            Item item = it.next();

            if (item.expiration <= 0)
            {
                it.remove();
            }

            item.expiration -= 1;
            item.entity.getProperties().update(item.formEntity);
            item.formEntity.update();
        }
    }

    @Override
    public ItemStack getData(ItemStack stack)
    {
        return stack;
    }

    @Override
    public void render(ItemStack stack, ItemDisplayContext mode, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, int overlay, boolean hasGlint, int seed)
    {
        Item item = this.get(stack);

        if (item != null)
        {
            ModelProperties properties = item.entity.getProperties();
            ModelTransformMode transformMode = this.toModelMode(mode);
            Form form = properties.getForm(transformMode);
            Transform transform = properties.getTransform(transformMode);

            if (form != null)
            {
                item.expiration = 20;

                matrices.push();
                matrices.translate(0.5F, 0F, 0.5F);
                MatrixStackUtils.applyTransform(matrices, transform);

                GlStateManager._enableDepthTest();

                FormUtilsClient.render(form, new FormRenderingContext()
                    .set(FormRenderType.fromModelMode(mode), item.formEntity, matrices, light, overlay, 1F)
                    .camera(MinecraftClient.getInstance().gameRenderer.getCamera()));

                GlStateManager._disableDepthTest();

                matrices.pop();
            }
        }
    }

    @Override
    public void collectVertices(Consumer<Vector3fc> consumer)
    {}

    private ModelTransformMode toModelMode(ItemDisplayContext mode)
    {
        if (mode == ItemDisplayContext.GUI) return ModelTransformMode.GUI;
        if (mode == ItemDisplayContext.THIRD_PERSON_LEFT_HAND) return ModelTransformMode.THIRD_PERSON_LEFT_HAND;
        if (mode == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) return ModelTransformMode.THIRD_PERSON_RIGHT_HAND;
        if (mode == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) return ModelTransformMode.FIRST_PERSON_LEFT_HAND;
        if (mode == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) return ModelTransformMode.FIRST_PERSON_RIGHT_HAND;
        if (mode == ItemDisplayContext.GROUND) return ModelTransformMode.GROUND;
        return ModelTransformMode.NONE;
    }

    public Item get(ItemStack stack)
    {
        if (stack == null || stack.getItem() != BBSMod.MODEL_BLOCK_ITEM)
        {
            return null;
        }

        if (this.map.containsKey(stack))
        {
            return this.map.get(stack);
        }

        ModelBlockEntity entity = new ModelBlockEntity(BlockPos.ORIGIN, BBSMod.MODEL_BLOCK.getDefaultState());
        Item item = new Item(entity);

        this.map.put(stack, item);

        NbtComponent nbtComponent = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (nbtComponent == null)
        {
            return item;
        }

        NbtCompound nbt = nbtComponent.copyNbt();
        var world = MinecraftClient.getInstance().world;
        if (world != null)
        {
            entity.readNbt(nbt);
        }

        return item;
    }

    public static class Unbaked implements SpecialModelRenderer.Unbaked
    {
        public static final com.mojang.serialization.MapCodec<Unbaked> CODEC = com.mojang.serialization.MapCodec.unit(new Unbaked());

        @Override
        public MapCodec<Unbaked> getCodec()
        {
            return CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(net.minecraft.client.render.item.model.special.SpecialModelRenderer.BakeContext context)
        {
            return new ModelBlockItemRenderer();
        }
    }

    public static class Item
    {
        public ModelBlockEntity entity;
        public IEntity formEntity;
        public int expiration = 20;

        public Item(ModelBlockEntity entity)
        {
            this.entity = entity;
            this.formEntity = new StubEntity(MinecraftClient.getInstance().world);
        }
    }
}
