package mchorse.bbs_mod.items;

import net.minecraft.item.ItemStack;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.ToolMaterials;

public class StructurePickerItem extends ShovelItem
{
    public StructurePickerItem(Settings settings)
    {
        super(ToolMaterials.WOOD, settings);
    }

    @Override
    public boolean hasGlint(ItemStack stack)
    {
        return true;
    }
}
