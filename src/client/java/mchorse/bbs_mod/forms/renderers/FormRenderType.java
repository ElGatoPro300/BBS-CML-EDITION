package mchorse.bbs_mod.forms.renderers;

import net.minecraft.item.ItemDisplayContext;
import mchorse.bbs_mod.forms.values.ModelTransformMode;

public enum FormRenderType
{
    MODEL_BLOCK, ENTITY, ITEM_FP, ITEM_TP, ITEM_INVENTORY, ITEM, PREVIEW;

    public static FormRenderType fromModelMode(ItemDisplayContext mode)
    {
        if (mode.isFirstPerson())
        {
            return ITEM_FP;
        }
        else if (mode == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || mode == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
        {
            return ITEM_TP;
        }
        else if (mode == ItemDisplayContext.GROUND)
        {
            return ITEM;
        }
        else if (mode == ItemDisplayContext.GUI)
        {
            return ITEM_INVENTORY;
        }

        return ENTITY;
    }

    public static FormRenderType fromModelMode(ModelTransformMode mode)
    {
        if (mode == ModelTransformMode.FIRST_PERSON_LEFT_HAND || mode == ModelTransformMode.FIRST_PERSON_RIGHT_HAND)
        {
            return ITEM_FP;
        }
        else if (mode == ModelTransformMode.THIRD_PERSON_LEFT_HAND || mode == ModelTransformMode.THIRD_PERSON_RIGHT_HAND)
        {
            return ITEM_TP;
        }
        else if (mode == ModelTransformMode.GROUND)
        {
            return ITEM;
        }
        else if (mode == ModelTransformMode.GUI)
        {
            return ITEM_INVENTORY;
        }

        return ENTITY;
    }
}
