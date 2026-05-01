package mchorse.bbs_mod.forms.renderers;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.opengl.GlStateManager;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.cubic.render.vao.ModelVAO;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.forms.forms.ExtrudedForm;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.joml.Vectors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;

import java.util.function.Supplier;

public class ExtrudedFormRenderer extends FormRenderer<ExtrudedForm>
{
    public ExtrudedFormRenderer(ExtrudedForm form)
    {
        super(form);
    }

    @Override
    public void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        MatrixStack stack = new MatrixStack();

        stack.push();

        Matrix4f uiMatrix = ModelFormRenderer.getUIMatrix(context, x1, y1, x2, y2);

        this.applyTransforms(uiMatrix, context.getTransition());
        MatrixStackUtils.multiply(stack, uiMatrix);
        stack.translate(0F, 1F, 0F);
        stack.scale(1.5F, 1.5F, 4F);
        stack.scale(this.form.uiScale.get(), this.form.uiScale.get(), this.form.uiScale.get());

        /* Shading fix */
        stack.peek().getNormalMatrix().getScale(Vectors.EMPTY_3F);
        stack.peek().getNormalMatrix().scale(1F / Vectors.EMPTY_3F.x, -1F / Vectors.EMPTY_3F.y, 1F / Vectors.EMPTY_3F.z);

        GlStateManager._depthFunc(GL11.GL_LEQUAL);
        this.renderModel(BBSShaders::getModel,
            stack,
            OverlayTexture.DEFAULT_UV, LightmapTextureManager.MAX_LIGHT_COORDINATE, Colors.WHITE,
            context.getTransition(),
            null,
            true,
            false
        );
        GlStateManager._depthFunc(GL11.GL_ALWAYS);

        stack.pop();
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
        boolean shading = this.form.shading.get();

        if (BBSRendering.isIrisShadersEnabled())
        {
            shading = true;
        }

        VertexFormat format = shading ? VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL : VertexFormats.POSITION_TEXTURE_COLOR;
        Supplier<ShaderProgram> shader = this.getShader(
            context,
            shading
                ? () ->
                {
                    // RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_ENTITY_TRANSLUCENT);
                    /* shader binding handled by RenderLayer in 1.21.11 */
                    return null;
                }
                : () ->
                {
                    // RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
                    /* shader binding handled by RenderLayer in 1.21.11 */
                    return null;
                },
            shading ? BBSShaders::getPickerBillboardProgram : BBSShaders::getPickerBillboardNoShadingProgram
        );

        this.renderModel(shader, context.stack, context.overlay, context.light, context.color, context.getTransition(), context.camera, false, context.modelRenderer || context.isPicking());
    }

    private void renderModel(Supplier<ShaderProgram> shader, MatrixStack matrices, int overlay, int light, int overlayColor, float transition, Camera camera, boolean invertY, boolean modelRenderer)
    {
        Link texture = this.form.texture.get();
        ModelVAO data = BBSModClient.getTextures().getExtruder().get(texture);

        if (data != null)
        {
            if (this.form.billboard.get())
            {
                Matrix4f modelMatrix = matrices.peek().getPositionMatrix();
                Vector3f scale = new Vector3f();

                modelMatrix.getScale(scale);

                if (invertY)
                {
                    scale.y = -scale.y;
                }

                modelMatrix.m00(1).m01(0).m02(0);
                modelMatrix.m10(0).m11(1).m12(0);
                modelMatrix.m20(0).m21(0).m22(1);

                if (camera != null && !modelRenderer)
                {
                    modelMatrix.mul(camera.view);
                }

                modelMatrix.scale(scale);

                matrices.peek().getNormalMatrix().identity();
                matrices.peek().getNormalMatrix().scale(1F / scale.x, 1F / scale.y, 1F / scale.z);
            }

            Color color = Colors.COLOR.set(overlayColor, true);
            Color formColor = this.form.color.get();

            BBSModClient.getTextures().bindTexture(texture);

            GlStateManager._enableBlend();
            GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

            ModelVAORenderer.render(shader.get(), data, matrices, color.r * formColor.r, color.g * formColor.g, color.b * formColor.b, color.a * formColor.a, light, overlay);

            GlStateManager._disableBlend();

        }
    }
}
