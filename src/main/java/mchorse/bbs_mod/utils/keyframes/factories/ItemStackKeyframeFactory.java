package mchorse.bbs_mod.utils.keyframes.factories;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.interps.IInterp;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;

import java.util.Optional;

public class ItemStackKeyframeFactory implements IKeyframeFactory<ItemStack>
{
    @Override
    public ItemStack fromData(BaseType data)
    {
        net.minecraft.registry.RegistryWrapper.WrapperLookup registries = mchorse.bbs_mod.BBSMod.getRegistryManager();
        com.mojang.serialization.DynamicOps<NbtElement> ops = registries != null ? net.minecraft.registry.RegistryOps.of(NbtOps.INSTANCE, registries) : NbtOps.INSTANCE;
        DataResult<Pair<ItemStack, NbtElement>> decode = ItemStack.CODEC.decode(ops, DataStorageUtils.toNbt(data));
        Optional<Pair<ItemStack, NbtElement>> result = decode.result();

        return result.map(Pair::getFirst).orElse(ItemStack.EMPTY);
    }

    @Override
    public BaseType toData(ItemStack value)
    {
        net.minecraft.registry.RegistryWrapper.WrapperLookup registries = mchorse.bbs_mod.BBSMod.getRegistryManager();
        com.mojang.serialization.DynamicOps<NbtElement> ops = registries != null ? net.minecraft.registry.RegistryOps.of(NbtOps.INSTANCE, registries) : NbtOps.INSTANCE;
        Optional<NbtElement> result = ItemStack.CODEC.encodeStart(ops, value).result();

        return result.map(DataStorageUtils::fromNbt).orElse(new MapType());
    }

    @Override
    public ItemStack createEmpty()
    {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean compare(Object a, Object b)
    {
        if (a instanceof ItemStack itemA && b instanceof ItemStack itemB)
        {
            return ItemStack.areEqual(itemA, itemB);
        }

        return false;
    }

    @Override
    public ItemStack copy(ItemStack value)
    {
        return value.copy();
    }

    @Override
    public ItemStack interpolate(ItemStack preA, ItemStack a, ItemStack b, ItemStack postB, IInterp interpolation, float x)
    {
        return a;
    }
}