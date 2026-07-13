package mchorse.bbs_mod.cubic.render;

import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class CubicRenderer
{
    public static final List<String> STENCIL_PICK_PRIORITY_BONES = Arrays.asList("low_body");

    /**
     * Process/render given model
     *
     * This method recursively goes through all groups in the model, and
     * applies given render processor. Processor may return true from its
     * sole method which means that iteration should be halted.
     */
    public static boolean processRenderModel(ICubicRenderer renderProcessor, BufferBuilder builder, MatrixStack stack, Model model)
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
     * Re-render specific groups after the main pass so overlapping stencil picks
     * prefer these bones over parents/siblings drawn earlier (e.g. low_body over torso).
     */
    public static void renderStencilPickPriority(ICubicRenderer renderProcessor, BufferBuilder builder, MatrixStack stack, Model model, Collection<String> boneIds)
    {
        RenderSystem.disableDepthTest();

        try
        {
            for (String boneId : boneIds)
            {
                ModelGroup group = model.getGroup(boneId);

                if (group != null)
                {
                    renderGroupBranch(renderProcessor, builder, stack, model, group);
                }
            }
        }
        finally
        {
            RenderSystem.enableDepthTest();
        }
    }

    private static void renderGroupBranch(ICubicRenderer renderProcessor, BufferBuilder builder, MatrixStack stack, Model model, ModelGroup target)
    {
        List<ModelGroup> path = new ArrayList<>();
        ModelGroup current = target;

        while (current != null)
        {
            path.add(0, current);
            current = current.parent;
        }

        for (ModelGroup group : path)
        {
            stack.push();
            renderProcessor.applyGroupTransformations(stack, group);
        }

        if (target.visible)
        {
            renderProcessor.renderGroup(builder, stack, target, model);
        }

        for (int i = 0, c = path.size(); i < c; i++)
        {
            stack.pop();
        }
    }

    /**
     * Apply the render processor, recursively
     */
    private static boolean processRenderRecursively(ICubicRenderer renderProcessor, BufferBuilder builder, MatrixStack stack, Model model, ModelGroup group)
    {
        stack.push();
        renderProcessor.applyGroupTransformations(stack, group);

        if (group.visible)
        {
            if (renderProcessor.renderGroup(builder, stack, group, model))
            {
                stack.pop();

                return true;
            }
        }

        for (ModelGroup childGroup : group.children)
        {
            if (processRenderRecursively(renderProcessor, builder, stack, model, childGroup))
            {
                stack.pop();

                return true;
            }
        }

        stack.pop();

        return false;
    }
}