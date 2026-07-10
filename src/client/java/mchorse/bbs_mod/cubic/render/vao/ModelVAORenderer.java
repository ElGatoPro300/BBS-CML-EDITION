package mchorse.bbs_mod.cubic.render.vao;

import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.client.render.picker.BBSPickerRenderer;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;

public class ModelVAORenderer
{
    /**
     * Draw an {@link IModelVAO} through the immediate model RenderLayer. The 1.21.11 rewrite removed
     * ShaderProgram.bind()/unbind() and the imperative uniform/sampler/fog/light setup; the built-in
     * uniforms now live in the std140 UBOs (DynamicTransforms/Projection/Fog/Lighting) that
     * {@link BBSShaders#getModelLayer()} uploads per draw. The geometry is baked CPU-side into a
     * BufferBuilder (matching the cubic immediate path) and submitted through that layer.
     */
    public static void render(IModelVAO modelVAO, MatrixStack stack, float r, float g, float b, float a, int light, int overlay)
    {
        BuiltBuffer built = write(modelVAO, stack, r, g, b, a, light, overlay);

        if (built != null)
        {
            BBSShaders.getModelLayer().draw(built);
        }
    }

    /**
     * Draw an {@link IModelVAO} through the picker_models pipeline into the active picking target,
     * for stencil/picking passes. Replaces the old {@code ModelVAORenderer.render(pickerShader, ...)}
     * overload; the picker uniform (Target index) is uploaded by {@link BBSPickerRenderer}.
     */
    public static void renderPicking(IModelVAO modelVAO, MatrixStack stack, float r, float g, float b, float a, int light, int overlay)
    {
        BuiltBuffer built = write(modelVAO, stack, r, g, b, a, light, overlay);

        if (built != null)
        {
            BBSPickerRenderer.draw(BBSShaders.getPickerModelsProgram(), built, RenderSystem.getModelViewMatrix());
        }
    }

    private static BuiltBuffer write(IModelVAO modelVAO, MatrixStack stack, float r, float g, float b, float a, int light, int overlay)
    {
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);

        modelVAO.writeImmediate(builder, stack, r, g, b, a, light, overlay);

        return builder.endNullable();
    }
}
