package mchorse.bbs_mod.ui.model;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.UIModelRenderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;

/**
 * A lightweight, non-interactive model preview renderer for mosaic grid cards.
 *
 * <p>Uses orthographic projection, hides the grid, and blocks all mouse
 * interaction so the user cannot rotate or zoom the preview.</p>
 */
public class UIModelPreviewRenderer extends UIModelRenderer
{
    private ModelForm form = new ModelForm();
    private ModelFormRenderer renderer;
    private ModelInstance previewModel;
    private String lastModelId;

    public UIModelPreviewRenderer()
    {
        super();

        this.grid = false;

        this.renderer = new ModelFormRenderer(this.form)
        {
            @Override
            public ModelInstance getModel()
            {
                return UIModelPreviewRenderer.this.getModelInstance();
            }
        };

        /* Nice default angle for thumbnails */
        this.setRotation(-25, 20);
        this.setDistance(20);
        this.setPosition(0, 1.1F, 0);
    }

    public void setModel(String modelId)
    {
        this.form.model.set(modelId);
    }

    private ModelInstance getModelInstance()
    {
        String modelId = this.form.model.get();

        if (modelId.isEmpty())
        {
            this.previewModel = null;
            return null;
        }

        if (!modelId.equals(this.lastModelId) || this.previewModel == null)
        {
            ModelInstance globalModel = BBSModClient.getModels().getModel(modelId);

            if (globalModel != null)
            {
                this.previewModel = new ModelInstance(globalModel.id, globalModel.model, globalModel.animations, globalModel.texture);
                this.previewModel.setup();
                this.lastModelId = modelId;
            }
            else
            {
                this.previewModel = null;
            }
        }

        return this.previewModel;
    }

    /* ---- Disable all mouse interaction ---- */

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        return false;
    }

    @Override
    public boolean subMouseScrolled(UIContext context)
    {
        return false;
    }

    @Override
    public boolean subMouseReleased(UIContext context)
    {
        return false;
    }

    /* ---- Fix scroll MatrixStack translation for 3D ---- */

    @Override
    public void render(UIContext context)
    {
        /*
         * The UI MatrixStack contains a translation from the UIScrollView.
         * Since we explicitly set the glViewport to the absolute screen position,
         * applying the scroll shift again to the 3D world matrix causes the model 
         * to shift twice and fly off-screen. We temporarily undo it here.
         */
        int sx = -context.globalX(0);
        int sy = -context.globalY(0);

        context.batcher.getContext().getMatrices().pushMatrix();
        context.batcher.getContext().getMatrices().translate(sx, sy);

        super.render(context);

        context.batcher.getContext().getMatrices().popMatrix();
    }

    /* ---- Orthographic viewport ---- */

    @Override
    protected void setupViewport(UIContext context)
    {
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

        MinecraftClient mc = MinecraftClient.getInstance();

        float rx = (float) Math.round(mc.getWindow().getWidth() / (double) context.menu.width);
        float ry = (float) Math.round(mc.getWindow().getHeight() / (double) context.menu.height);
        float size = BBSModClient.getOriginalFramebufferScale();

        /* Account for scroll/shift to fix disappearing models using global UI coordinates */
        int ax = context.globalX(this.area.x);
        int ay = context.globalY(this.area.y);

        int vx = (int) (ax * rx);
        int vy = (int) (mc.getWindow().getHeight() - (ay + this.area.h) * ry);
        int vw = (int) (this.area.w * rx);
        int vh = (int) (this.area.h * ry);

        com.mojang.blaze3d.opengl.GlStateManager._viewport((int) (vx * size), (int) (vy * size), (int) (vw * size), (int) (vh * size));

        /* Orthographic projection scaled so the model fits nicely (zoomed out) */
        float orthoScale = (float) this.distance.getValue() * 0.3F;
        float aspect = vw / (float) Math.max(1, vh);
        this.camera.projection.identity().ortho(
            -orthoScale * aspect, orthoScale * aspect,
            -orthoScale, orthoScale,
            this.camera.near, this.camera.far
        );
        this.camera.updateView();
    }

    /* ---- Render the model ---- */

    @Override
    protected void renderUserModel(UIContext context)
    {
        FormRenderingContext formContext = new FormRenderingContext()
            .set(FormRenderType.PREVIEW, this.entity, new net.minecraft.client.util.math.MatrixStack(), LightmapTextureManager.pack(15, 15), OverlayTexture.DEFAULT_UV, context.getTransition())
            .camera(this.camera)
            .modelRenderer();

        this.renderer.render(formContext);
    }
}
