package mchorse.bbs_mod.ui.model;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.animation.ActionsConfig;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelCube;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.data.model.ModelQuad;
import mchorse.bbs_mod.cubic.data.model.ModelVertex;
import mchorse.bbs_mod.cubic.render.CubicCubeRenderer;
import mchorse.bbs_mod.cubic.render.ICubicRenderer;
import mchorse.bbs_mod.cubic.model.ModelConfig;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.ui.framework.elements.utils.UIModelRenderer;
import mchorse.bbs_mod.ui.utils.Gizmo;
import mchorse.bbs_mod.ui.utils.StencilFormFramebuffer;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCacheEntry;
import mchorse.bbs_mod.utils.MathUtils;

public class UIModelEditorRenderer extends UIModelRenderer
{
    private static final Logger LOGGER = LogUtils.getLogger();

    public UIPropTransform transform;

    private ModelForm form = new ModelForm();
    private ModelFormRenderer renderer;
    private ModelConfig config;
    private Consumer<String> callback;
    private String selectedBone;
    private ModelCube selectedCube;
    private boolean dirty = true;

    private StencilFormFramebuffer stencil = new StencilFormFramebuffer();
    private StencilMap stencilMap = new StencilMap();

    private ModelInstance previewModel;
    private String lastModelId;

    public UIModelEditorRenderer()
    {
        super();
        this.renderer = new ModelFormRenderer(this.form)
        {
            @Override
            public ModelInstance getModel()
            {
                return UIModelEditorRenderer.this.getModel();
            }
        };
    }

    public void setModel(String modelId)
    {
        this.form.model.set(modelId);
    }
    
    public void setConfig(ModelConfig config)
    {
        this.config = config;
    }

    public void setCallback(Consumer<String> callback)
    {
        this.callback = callback;
    }
    
    public void dirty()
    {
        this.dirty = true;
    }

    public void syncAnimationsAndResetAnimator()
    {
        this.syncAnimations();
    }

    public void syncAnimationsAndRefreshAnimator()
    {
        this.syncAnimations();

        if (this.previewModel != null)
        {
            this.renderer.ensureAnimator(0F);
            LOGGER.debug("Model editor animation sync: animator refreshed for model {}", this.previewModel.id);
        }
        else
        {
            LOGGER.debug("Model editor animation sync: preview model is null, animator refresh skipped");
        }

        this.dirty();
    }

    public ModelInstance getPreviewModelInstance()
    {
        return this.getModel();
    }

    public void invalidatePreviewModel()
    {
        this.deletePreview();
        this.dirty();
    }

    private void ensureFramebuffer()
    {
        this.stencil.setup(Link.bbs("stencil_form"));
        this.stencil.resizeGUI(this.area.w, this.area.h);
    }

    @Override
    public void resize()
    {
        super.resize();

        this.ensureFramebuffer();
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (this.stencil.hasPicked())
        {
            Pair<Form, String> picked = this.stencil.getPicked();

            if (picked != null)
            {
                if (picked.a == null)
                {
                    int index = this.stencil.getIndex();
                    
                    if (index >= Gizmo.STENCIL_X && index <= Gizmo.STENCIL_FREE)
                    {
                        Gizmo.INSTANCE.start(index, context.mouseX, context.mouseY, this.transform);
                        return true;
                    }
                }
                else if (this.callback != null)
                {
                    this.callback.accept(picked.b);
                    return true;
                }
            }
        }

        return super.subMouseClicked(context);
    }

    @Override
    public boolean subMouseReleased(UIContext context)
    {
        Gizmo.INSTANCE.stop();
        
        return super.subMouseReleased(context);
    }

    public void setSelectedBone(String bone)
    {
        this.selectedBone = bone;
    }

    public String getSelectedBone()
    {
        return this.selectedBone;
    }

    public void setSelectedCube(ModelCube cube)
    {
        this.selectedCube = cube;
    }

    @Override
    protected void renderUserModel(UIContext context)
    {
        this.updateModel();
        
        FormRenderingContext formContext = new FormRenderingContext()
            .set(FormRenderType.PREVIEW, this.entity, context.batcher.getContext().getMatrices(), LightmapTextureManager.pack(15, 15), OverlayTexture.DEFAULT_UV, context.getTransition())
            .camera(this.camera)
            .modelRenderer();

        this.renderer.render(formContext);
        MatrixCache matrixCache = this.renderer.collectMatrices(this.entity, context.getTransition());
        this.renderSelectedCubeVisualizer(context, matrixCache);

        /* Render Axes */
        Matrix4f gizmoMatrix = null;

        if (UIBaseMenu.renderAxes && this.selectedBone != null && !this.selectedBone.isEmpty())
        {
            if (this.selectedCube != null)
            {
                gizmoMatrix = this.getCubePivotMatrix(matrixCache);
            }
            else
            {
                MatrixCacheEntry entry = matrixCache.get(this.selectedBone);

                if (entry != null)
                {
                    Matrix4f matrix = entry.matrix();

                    if (matrix == null)
                    {
                        matrix = entry.origin();
                    }

                    gizmoMatrix = matrix;
                }
            }

            if (gizmoMatrix != null)
            {
                MatrixStack stack = context.batcher.getContext().getMatrices();

                stack.push();
                MatrixStackUtils.multiply(stack, gizmoMatrix);

                RenderSystem.disableDepthTest();
                Gizmo.INSTANCE.render(stack);
                RenderSystem.enableDepthTest();

                stack.pop();
            }
        }

        if (this.area.isInside(context))
        {
            if (this.stencil.getFramebuffer() == null)
            {
                this.ensureFramebuffer();
            }

            GlStateManager._disableScissorTest();

            this.stencilMap.setup();
            this.stencil.apply();

            this.renderer.render(formContext.stencilMap(this.stencilMap));

            if (gizmoMatrix != null)
            {
                MatrixStack stack = context.batcher.getContext().getMatrices();

                stack.push();
                MatrixStackUtils.multiply(stack, gizmoMatrix);

                RenderSystem.disableDepthTest();
                Gizmo.INSTANCE.renderStencil(stack, this.stencilMap);
                RenderSystem.enableDepthTest();

                stack.pop();
            }

            this.stencil.pickGUI(context, this.area);
            this.stencil.unbind(this.stencilMap);

            MinecraftClient.getInstance().getFramebuffer().beginWrite(true);

            GlStateManager._enableScissorTest();
        }
        else
        {
            this.stencil.clearPicking();
        }
    }

    private void renderSelectedCubeVisualizer(UIContext context, MatrixCache cache)
    {
        if (this.selectedCube == null || this.selectedBone == null || this.selectedBone.isEmpty())
        {
            return;
        }

        Matrix4f cubeMatrix = this.getCubePivotMatrix(cache);
        Matrix4f uiMatrix = context.batcher.getContext().getMatrices().peek().getPositionMatrix();

        if (cubeMatrix == null)
        {
            return;
        }

        MatrixStack cubeStack = new MatrixStack();

        MatrixStackUtils.multiply(cubeStack, cubeMatrix);
        CubicCubeRenderer.rotate(cubeStack, this.selectedCube.rotate);
        CubicCubeRenderer.moveBackFromPivot(cubeStack, this.selectedCube.pivot);

        cubeMatrix = new Matrix4f(cubeStack.peek().getPositionMatrix());

        if (this.selectedCube.quads.isEmpty())
        {
            return;
        }

        BufferBuilder builder = Tessellator.getInstance().getBuffer();

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        builder.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        for (ModelQuad quad : this.selectedCube.quads)
        {
            if (quad.vertices.size() != 4)
            {
                continue;
            }

            for (int i = 0; i < 4; i++)
            {
                ModelVertex va = quad.vertices.get(i);
                ModelVertex vb = quad.vertices.get((i + 1) % 4);
                Vector3f a = new Vector3f(va.vertex);
                Vector3f b = new Vector3f(vb.vertex);

                cubeMatrix.transformPosition(a);
                cubeMatrix.transformPosition(b);

                this.line(builder, uiMatrix, a, b, 1F, 0.6F, 0F, 1F);
            }
        }

        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    private Matrix4f getCubePivotMatrix(MatrixCache cache)
    {
        if (this.selectedCube == null || this.selectedBone == null || this.selectedBone.isEmpty())
        {
            return null;
        }

        ModelInstance instance = this.getPreviewModelInstance();

        if (instance == null || !(instance.model instanceof Model model))
        {
            return null;
        }

        ModelGroup group = model.getGroup(this.selectedBone);

        if (group == null)
        {
            return null;
        }

        MatrixStack cubeStack = new MatrixStack();
        MatrixCacheEntry rootEntry = cache.get("");
        Matrix4f rootMatrix = rootEntry == null ? null : rootEntry.matrix();

        if (rootMatrix != null)
        {
            MatrixStackUtils.multiply(cubeStack, rootMatrix);
        }

        cubeStack.multiply(RotationAxis.POSITIVE_Y.rotation(MathUtils.PI));

        List<ModelGroup> chain = new ArrayList<>();

        for (ModelGroup cursor = group; cursor != null; cursor = cursor.parent)
        {
            chain.add(0, cursor);
        }

        for (ModelGroup element : chain)
        {
            ICubicRenderer.translateGroup(cubeStack, element);
            ICubicRenderer.moveToGroupPivot(cubeStack, element);
            ICubicRenderer.rotateGroup(cubeStack, element);
            ICubicRenderer.scaleGroup(cubeStack, element);
            ICubicRenderer.moveBackFromGroupPivot(cubeStack, element);
        }

        CubicCubeRenderer.moveToPivot(cubeStack, this.selectedCube.pivot);

        return new Matrix4f(cubeStack.peek().getPositionMatrix());
    }

    private Vector3f getBonePoint(MatrixCache cache, String bone)
    {
        MatrixCacheEntry entry = cache.get(bone);

        if (entry == null)
        {
            return null;
        }

        Matrix4f matrix = entry.origin() == null ? entry.matrix() : entry.origin();

        if (matrix == null)
        {
            return null;
        }

        return this.translation(matrix);
    }

    private Vector3f translation(Matrix4f matrix)
    {
        Vector3f vector = new Vector3f();

        matrix.getTranslation(vector);

        return vector;
    }

    private void line(BufferBuilder builder, Matrix4f matrix, Vector3f a, Vector3f b, float r, float g, float bl, float alpha)
    {
        builder.vertex(matrix, a.x, a.y, a.z).color(r, g, bl, alpha).next();
        builder.vertex(matrix, b.x, b.y, b.z).color(r, g, bl, alpha).next();
    }

    private void cross(BufferBuilder builder, Matrix4f matrix, Vector3f p, float size, float r, float g, float b, float a)
    {
        this.line(builder, matrix, new Vector3f(p).add(-size, 0, 0), new Vector3f(p).add(size, 0, 0), r, g, b, a);
        this.line(builder, matrix, new Vector3f(p).add(0, -size, 0), new Vector3f(p).add(0, size, 0), r, g, b, a);
        this.line(builder, matrix, new Vector3f(p).add(0, 0, -size), new Vector3f(p).add(0, 0, size), r, g, b, a);
    }

    @Override
    public void render(UIContext context)
    {
        super.render(context);

        if (!this.stencil.hasPicked())
        {
            return;
        }

        Texture texture = this.stencil.getFramebuffer().getMainTexture();
        int index = this.stencil.getIndex();
        int w = texture.width;
        int h = texture.height;

        ShaderProgram previewProgram = BBSShaders.getPickerPreviewProgram();
        GlUniform target = previewProgram.getUniform("Target");

        if (target != null)
        {
            target.set(index);
        }

        RenderSystem.enableBlend();
        context.batcher.texturedBox(BBSShaders::getPickerPreviewProgram, texture.id, Colors.WHITE, this.area.x, this.area.y, this.area.w, this.area.h, 0, h, w, 0, w, h);

        Pair<Form, String> pair = this.stencil.getPicked();

        if (pair != null && pair.a != null && !pair.b.isEmpty())
        {
            String label = pair.a.getFormIdOrName() + " - " + pair.b;

            context.batcher.textCard(label, context.mouseX + 12, context.mouseY + 8);
        }
    }
    
    private void updateModel()
    {
        if (this.config == null)
        {
            return;
        }

        this.syncAnimations();
        this.form.color.get().set(this.config.color.get());

        if (!this.dirty)
        {
            return;
        }

        this.dirty = false;

        try
        {
            ModelInstance model = this.getModel();

            if (model != null)
            {
                boolean wasProcedural = model.procedural;

                model.applyConfig((MapType) this.config.toData());
                model.texture = this.config.texture.get();
                model.color = this.config.color.get();

                if (wasProcedural != model.procedural)
                {
                    this.renderer.resetAnimator();
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    private ModelInstance getModel()
    {
        String modelId = this.form.model.get();

        if (modelId.isEmpty())
        {
            this.deletePreview();
            return null;
        }

        if (!modelId.equals(this.lastModelId) || this.previewModel == null)
        {
            ModelInstance globalModel = BBSModClient.getModels().getModel(modelId);

            if (globalModel != null)
            {
                this.deletePreview();

                this.previewModel = new ModelInstance(globalModel.id, globalModel.model, globalModel.animations, globalModel.texture);
                this.previewModel.setup();

                if (this.config != null)
                {
                    try
                    {
                        this.syncAnimations();
                        this.previewModel.applyConfig((MapType) this.config.toData());
                        this.previewModel.texture = this.config.texture.get();
                        this.previewModel.color = this.config.color.get();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }

                this.lastModelId = modelId;
            }
        }

        return this.previewModel;
    }

    private void syncAnimations()
    {
        if (this.config == null)
        {
            LOGGER.debug("Model editor animation sync skipped: config is null");
            return;
        }

        ActionsConfig source = this.config.animations.get();
        ActionsConfig target = this.form.actions.get();

        if (!Objects.equals(target.geckoAnimations, source.geckoAnimations))
        {
            target.geckoAnimations.copy(source.geckoAnimations);
            LOGGER.debug(
                "Model editor animation sync applied: enabled={} limbs={}",
                target.geckoAnimations.enabled,
                target.geckoAnimations.limbs.size()
            );
        }
        else
        {
            LOGGER.debug(
                "Model editor animation sync skipped: no changes (enabled={} limbs={})",
                target.geckoAnimations.enabled,
                target.geckoAnimations.limbs.size()
            );
        }
    }

    private void deletePreview()
    {
        if (this.previewModel != null)
        {
            this.previewModel.delete();
            this.previewModel = null;
        }

        this.lastModelId = null;
    }
}
