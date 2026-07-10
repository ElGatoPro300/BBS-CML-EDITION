package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.Camera;
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
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.util.math.MatrixStack;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.opengl.GlStateManager;

import org.lwjgl.opengl.GL11;

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

        MinecraftClient.getInstance().gameRenderer.getDiffuseLighting().setShaderLights(DiffuseLighting.Type.ENTITY_IN_UI);

        GlStateManager._depthFunc(GL11.GL_LEQUAL);
        this.renderModel(false,
            stack,
            OverlayTexture.DEFAULT_UV, LightmapTextureManager.MAX_LIGHT_COORDINATE, Colors.WHITE,
            context.getTransition(),
            null,
            true,
            false,
            false
        );
        GlStateManager._depthFunc(GL11.GL_ALWAYS);

        MinecraftClient.getInstance().gameRenderer.getDiffuseLighting().setShaderLights(DiffuseLighting.Type.LEVEL);

        stack.pop();
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
        /* The "shading" toggle used to pick between a lit (POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL) and a
         * flat (POSITION_TEXTURE_COLOR) vertex format/shader. ModelVAORenderer.render/renderPicking now always
         * bake the lit model format internally (see .port_1.21.11_notes.md #7), so that distinction is no
         * longer expressible here; the form keeps the property, but this renderer always draws through the
         * shaded model pipeline. */
        boolean picking = context.isPicking();

        if (picking)
        {
            this.setupTarget(context, null);
        }

        this.renderModel(picking, context.stack, context.overlay, context.light, context.color, context.getTransition(), context.camera, false, context.modelRenderer || context.isPicking());
    }

    private void renderModel(boolean picking, MatrixStack matrices, int overlay, int light, int overlayColor, float transition, Camera camera, boolean invertY, boolean modelRenderer)
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

            color.mul(formColor);

            BBSModClient.getTextures().bindTexture(texture);

            GlStateManager._enableBlend();

            float r = color.r * formColor.r;
            float g = color.g * formColor.g;
            float b = color.b * formColor.b;
            float a = color.a * formColor.a;

            if (picking)
            {
                ModelVAORenderer.renderPicking(data, matrices, r, g, b, a, light, overlay);
            }
            else
            {
                ModelVAORenderer.render(data, matrices, r, g, b, a, light, overlay);
            }

            GlStateManager._disableBlend();
        }
    }
}
