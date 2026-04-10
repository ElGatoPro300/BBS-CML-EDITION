package mchorse.bbs_mod.cubic.render;

import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.render.vao.ModelVAO;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.obj.shapes.ShapeKeys;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.interps.Lerps;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;

public class CubicVAORenderer extends CubicCubeRenderer
{
    private GlProgram program;
    private ModelInstance model;
    private Link defaultTexture;

    public CubicVAORenderer(GlProgram program, ModelInstance model, int light, int overlay, StencilMap stencilMap, ShapeKeys shapeKeys, Link defaultTexture)
    {
        super(light, overlay, stencilMap, shapeKeys);

        this.program = program;
        this.model = model;
        this.defaultTexture = defaultTexture;
    }

    @Override
    public boolean renderGroup(BufferBuilder builder, PoseStack stack, ModelGroup group, Model model)
    {
        if (this.stencilMap != null && !this.stencilMap.isBoneAllowed(group.id))
        {
            return false;
        }

        ModelVAO modelVAO = this.model.getVaos().get(group);

        if (modelVAO != null && group.visible)
        {
            if (group.textureOverride != null)
            {
                BBSModClient.getTextures().bindTexture(group.textureOverride);
            }
            else if (this.defaultTexture != null)
            {
                BBSModClient.getTextures().bindTexture(this.defaultTexture);
            }
            else
            {
                BBSModClient.getTextures().bindTexture(this.model.texture);
            }

            float r = this.r * group.color.r;
            float g = this.g * group.color.g;
            float b = this.b * group.color.b;
            float a = this.a * group.color.a;
            int light = this.light;

            if (this.stencilMap != null)
            {
                light = this.stencilMap.increment ? group.index : 0;
            }
            else
            {
                int u = (int) Lerps.lerp(light & '\uffff', LightTexture.FULL_BLOCK, MathUtils.clamp(group.lighting, 0F, 1F));
                int v = light >> 16 & '\uffff';

                light = u | v << 16;
            }

            ModelVAORenderer.render(this.program, modelVAO, stack, r, g, b, a, light, this.overlay);
        }

        return false;
    }
}