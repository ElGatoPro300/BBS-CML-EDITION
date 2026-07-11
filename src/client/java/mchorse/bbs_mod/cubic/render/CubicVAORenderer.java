package mchorse.bbs_mod.cubic.render;

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

import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;

import com.mojang.blaze3d.systems.RenderSystem;

public class CubicVAORenderer extends CubicCubeRenderer
{
    private ShaderProgram program;
    private ModelInstance model;
    private Link defaultTexture;

    public CubicVAORenderer(ShaderProgram program, ModelInstance model, int light, int overlay, StencilMap stencilMap, ShapeKeys shapeKeys, Link defaultTexture)
    {
        super(light, overlay, stencilMap, shapeKeys);

        this.program = program;
        this.model = model;
        this.defaultTexture = defaultTexture;
    }

    @Override
    public boolean renderGroup(BufferBuilder builder, MatrixStack stack, ModelGroup group, Model model)
    {
        if (this.stencilMap != null && !this.stencilMap.isBoneAllowed(group.id))
        {
            return false;
        }

        ModelVAO modelVAO = this.model.getVaos().get(group);

        if (modelVAO != null && group.visible)
        {
            float r;
            float g;
            float b;
            float a;

            if (ModelVAORenderer.isPaintPass())
            {
                float pr;
                float pg;
                float pb;
                float pa;

                if (group.paintColor.a > 0F)
                {
                    pr = group.paintColor.r;
                    pg = group.paintColor.g;
                    pb = group.paintColor.b;
                    pa = group.paintColor.a;
                }
                else
                {
                    pr = ModelVAORenderer.getBasePaintR();
                    pg = ModelVAORenderer.getBasePaintG();
                    pb = ModelVAORenderer.getBasePaintB();
                    pa = ModelVAORenderer.getBasePaintStrength();
                }

                if (pa <= 0F)
                {
                    return false;
                }

                ModelVAORenderer.setGroupPaint(group.paintColor.r, group.paintColor.g, group.paintColor.b, group.paintColor.a);

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

                r = pr;
                g = pg;
                b = pb;
                a = ModelVAORenderer.isPaintOverlayPass() ? this.a : this.a * pa;
            }
            else
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

                r = this.r * group.color.r;
                g = this.g * group.color.g;
                b = this.b * group.color.b;
                a = this.a * group.color.a;

                ModelVAORenderer.setGroupPaint(group.paintColor.r, group.paintColor.g, group.paintColor.b, group.paintColor.a);
            }

            int light = this.light;

            if (this.stencilMap != null)
            {
                light = this.stencilMap.increment ? group.index : 0;
            }
            else
            {
                int u = (int) Lerps.lerp(light & '\uffff', LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE, MathUtils.clamp(group.lighting, 0F, 1F));
                int v = light >> 16 & '\uffff';

                light = u | v << 16;
            }

            ModelVAORenderer.render(this.program, modelVAO, stack, r, g, b, a, light, this.overlay);
        }

        return false;
    }
}