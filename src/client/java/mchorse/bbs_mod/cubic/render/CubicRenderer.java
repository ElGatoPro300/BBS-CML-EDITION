package mchorse.bbs_mod.cubic.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;

public class CubicRenderer
{
    /**
     * Process/render given model
     *
     * This method recursively goes through all groups in the model, and
     * applies given render processor. Processor may return true from its
     * sole method which means that iteration should be halted.
     */
    public static boolean processRenderModel(ICubicRenderer renderProcessor, BufferBuilder builder, PoseStack stack, Model model)
    {
        for (ModelGroup group : model.topGroups)
        {
            if (processRenderRecursively(renderProcessor, builder, stack, model, group))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Apply the render processor, recursively
     */
    private static boolean processRenderRecursively(ICubicRenderer renderProcessor, BufferBuilder builder, PoseStack stack, Model model, ModelGroup group)
    {
        stack.pushPose();
        renderProcessor.applyGroupTransformations(stack, group);

        if (group.visible)
        {
            if (renderProcessor.renderGroup(builder, stack, group, model))
            {
                stack.popPose();

                return true;
            }
        }

        for (ModelGroup childGroup : group.children)
        {
            if (processRenderRecursively(renderProcessor, builder, stack, model, childGroup))
            {
                stack.popPose();

                return true;
            }
        }

        stack.popPose();

        return false;
    }
}