package mchorse.bbs_mod.ui.model;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.bobj.BOBJBone;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.animation.ActionsConfig;
import mchorse.bbs_mod.cubic.animation.IAnimator;
import mchorse.bbs_mod.cubic.animation.ProceduralAnimator;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelCube;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.data.model.ModelQuad;
import mchorse.bbs_mod.cubic.data.model.ModelVertex;
import mchorse.bbs_mod.cubic.model.IKChainConfig;
import mchorse.bbs_mod.cubic.model.ModelConfig;
import mchorse.bbs_mod.cubic.render.CubicCubeRenderer;
import mchorse.bbs_mod.cubic.render.ICubicRenderer;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCacheEntry;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.ui.framework.elements.utils.UIModelRenderer;
import mchorse.bbs_mod.ui.utils.Gizmo;
import mchorse.bbs_mod.ui.utils.StencilFormFramebuffer;
import mchorse.bbs_mod.utils.MathUtils;
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
import org.joml.Vector3d;
import org.joml.Vector3f;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;

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

    /* ---- IK gizmo state ---- */
    /** The currently active IK chain config — set by UIModelIKPanel. null = no IK gizmo. */
    private IKChainConfig activeIKChain;

    private StencilFormFramebuffer stencil = new StencilFormFramebuffer();
    private StencilMap stencilMap = new StencilMap();

    private ModelInstance previewModel;
    private String lastModelId;
    private final Matrix4f lastGizmoMatrix = new Matrix4f();
    private boolean hasGizmoMatrix;


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

    /**
     * Sets the IK chain whose target gizmo should be drawn in the 3D viewport.
     * Pass null to hide the IK gizmo.
     */
    public void setActiveIKChain(IKChainConfig chain)
    {
        this.activeIKChain = chain;
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
                        this.prepareGizmoDrag(this.transform);
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


        this.renderIKGizmo(context, matrixCache);

        /* Render Axes */
        Matrix4f gizmoMatrix = null;
        this.hasGizmoMatrix = false;

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
                this.lastGizmoMatrix.set(gizmoMatrix);
                this.hasGizmoMatrix = true;
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

    private void prepareGizmoDrag(UIPropTransform transform)
    {
        if (transform == null)
        {
            return;
        }

        transform.setGizmoRayProvider(new UIPropTransform.IGizmoRayProvider()
        {
            @Override
            public boolean getMouseRay(UIContext context, int mouseX, int mouseY, Vector3d rayOrigin, Vector3f rayDirection)
            {
                if (UIModelEditorRenderer.this.area.w <= 0 || UIModelEditorRenderer.this.area.h <= 0)
                {
                    return false;
                }

                Vector3f direction = UIModelEditorRenderer.this.camera.getMouseDirection(
                    mouseX,
                    mouseY,
                    UIModelEditorRenderer.this.area.x,
                    UIModelEditorRenderer.this.area.y,
                    UIModelEditorRenderer.this.area.w,
                    UIModelEditorRenderer.this.area.h
                );

                if (direction.lengthSquared() <= 1.0E-12F)
                {
                    return false;
                }

                rayDirection.set(direction).normalize();
                rayOrigin.set(
                    UIModelEditorRenderer.this.camera.position.x - UIModelEditorRenderer.this.pos.x,
                    UIModelEditorRenderer.this.camera.position.y - UIModelEditorRenderer.this.pos.y,
                    UIModelEditorRenderer.this.camera.position.z - UIModelEditorRenderer.this.pos.z
                );

                return true;
            }

            @Override
            public boolean getGizmoMatrix(Matrix4f matrix)
            {
                if (!UIModelEditorRenderer.this.hasGizmoMatrix)
                {
                    return false;
                }

                matrix.set(UIModelEditorRenderer.this.lastGizmoMatrix);

                return true;
            }
        });
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

    /**
     * Renders the IK gizmo:
     *  - A magenta 3D crosshair at the IK target position.
     *  - Cyan lines connecting the bones of the active chain (tip→root).
     *
     * Positions are in model-local space (1 unit = 1/16 block for Cubic models).
     * The renderer uses the same MatrixCache used for the bone gizmo / cube outline.
     */
    private void renderIKGizmo(UIContext context, MatrixCache matrixCache)
    {
        if (this.activeIKChain == null || !this.activeIKChain.enabled.get())
        {
            return;
        }

        Matrix4f uiMatrix = context.batcher.getContext().getMatrices().peek().getPositionMatrix();

        /* ---- target crosshair ---- */
        Vector3f targetWorld = new Vector3f(this.activeIKChain.target.get());

        /* Convert from IK metres to render units:
           In Cubic models, pivots are in 1/16 blocks; FABRIK works in metres
           (we divided by 16 in IKChain). For the gizmo we use the same metres.
           Multiply by 16 to match the model-local render coordinate system. */
        targetWorld.mul(16F);

        /* Apply model root matrix (same transform the bones live under) */
        MatrixCacheEntry rootEntry = matrixCache.get("");
        Matrix4f rootMat = rootEntry != null ? rootEntry.matrix() : null;
        Matrix4f gizmoMat = new Matrix4f(uiMatrix);

        if (rootMat != null)
        {
            /* Combine: UI projection × root bone matrix */
            gizmoMat.mul(rootMat);
        }

        /* Flip Z to match the renderer's Y-rotation PI applied in getCubePivotMatrix */
        gizmoMat.rotateY(MathUtils.PI);

        BufferBuilder builder = Tessellator.getInstance().getBuffer();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();

        builder.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        /* --- magenta crosshair at target --- */
        float cs = 0.12F * 16F;   /* crosshair arm length in render units */
        float tr = 1.0F, tg = 0.2F, tb = 1.0F, ta = 0.9F;

        this.cross(builder, gizmoMat, targetWorld, cs, tr, tg, tb, ta);

        /* --- diamond outline around target (XZ plane) --- */
        float ds = cs * 0.7F;
        Vector3f px = new Vector3f(targetWorld).add(ds, 0, 0);
        Vector3f nx = new Vector3f(targetWorld).add(-ds, 0, 0);
        Vector3f pz = new Vector3f(targetWorld).add(0, 0, ds);
        Vector3f nz = new Vector3f(targetWorld).add(0, 0, -ds);
        Vector3f py = new Vector3f(targetWorld).add(0, ds, 0);
        Vector3f ny = new Vector3f(targetWorld).add(0, -ds, 0);

        /* XZ diamond */
        this.line(builder, gizmoMat, px, pz, tr, tg, tb, ta);
        this.line(builder, gizmoMat, pz, nx, tr, tg, tb, ta);
        this.line(builder, gizmoMat, nx, nz, tr, tg, tb, ta);
        this.line(builder, gizmoMat, nz, px, tr, tg, tb, ta);

        /* Vertical diamond */
        this.line(builder, gizmoMat, px, py, tr, tg, tb, ta);
        this.line(builder, gizmoMat, py, nx, tr, tg, tb, ta);
        this.line(builder, gizmoMat, nx, ny, tr, tg, tb, ta);
        this.line(builder, gizmoMat, ny, px, tr, tg, tb, ta);

        /* --- cyan lines connecting IK chain bones (tip → root) --- */
        String tipName  = this.activeIKChain.tipBone.get();
        String rootName = this.activeIKChain.rootBone.get();

        if (!tipName.isEmpty() && !rootName.isEmpty())
        {
            ModelInstance inst = this.getPreviewModelInstance();

            if (inst != null && inst.model instanceof Model model)
            {
                /* Walk from tip to root collecting bone points */
                List<Vector3f> bonePoints = new ArrayList<>();
                ModelGroup cursor = model.getGroup(tipName);

                while (cursor != null)
                {
                    Vector3f pt = this.getBonePoint(matrixCache, cursor.id);

                    if (pt != null)
                    {
                        bonePoints.add(pt);
                    }

                    if (cursor.id.equals(rootName))
                    {
                        break;
                    }

                    cursor = cursor.parent;
                }

                /* Draw lines between consecutive bone points */
                for (int i = 0; i < bonePoints.size() - 1; i++)
                {
                    this.line(builder, uiMatrix,
                              bonePoints.get(i), bonePoints.get(i + 1),
                              0.2F, 0.9F, 1.0F, 0.85F);
                }

                /* Small crosshair at each joint */
                for (Vector3f pt : bonePoints)
                {
                    this.cross(builder, uiMatrix, pt, 0.025F, 0.2F, 0.9F, 1.0F, 0.8F);
                }
            }
        }

        BufferRenderer.drawWithGlobalProgram(builder.end());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }


    private int getBoneStencilId(String bone)
    {
        for (Map.Entry<Integer, Pair<Form, String>> entry : this.stencilMap.indexMap.entrySet())
        {
            if (entry.getValue().a == this.form && entry.getValue().b.equals(bone))
            {
                return entry.getKey();
            }
        }
        return 0;
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

                /* Live IK preview: push chain configs into the preview animator */
                this.applyIKChainsToPreview(model);
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

    /**
     * Pushes the IK chain configurations from the active ModelConfig into the
     * preview model's ProceduralAnimator so the FABRIK solver runs every frame
     * in the editor preview viewport.
     */
    private void applyIKChainsToPreview(ModelInstance model)
    {
        if (this.config == null || this.config.ikChains.getList().isEmpty())
        {
            return;
        }

        /* ModelFormRenderer.getAnimator() returns the live IAnimator.
           If the model is procedural, it will be a ProceduralAnimator. */
        IAnimator animator = this.renderer.getAnimator();

        if (animator instanceof ProceduralAnimator pa)
        {
            pa.setIKChains(new ArrayList<>(this.config.ikChains.getList()));
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
