package mchorse.bbs_mod.items;

import net.minecraft.item.ItemStack;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.ToolMaterials;

public class StructurePickerItem extends ShovelItem
{
    public StructurePickerItem(Settings settings)
    {
        super(ToolMaterials.WOOD, 1.5F, -3.0F, settings);
    }

    @Override
    public boolean hasGlint(ItemStack stack)
    {
        return true;
    }
}
