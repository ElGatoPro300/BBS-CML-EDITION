package mchorse.bbs_mod.ui.model;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelCube;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.data.model.ModelQuad;
import mchorse.bbs_mod.cubic.data.model.ModelVertex;
import mchorse.bbs_mod.cubic.model.IKChainConfig;
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
import mchorse.bbs_mod.ui.model.UIModelIKPanel;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import mchorse.bbs_mod.ui.utils.Gizmo;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCacheEntry;
import mchorse.bbs_mod.utils.MathUtils;

public class UIModelEditorRenderer extends UIModelRenderer
{
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
        this.renderIKVisualizer(context);
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
                gizmoMatrix = this.getSelectedBoneMatrix(this.selectedBone, matrixCache);
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

    private void renderIKVisualizer(UIContext context)
    {
        if (this.config == null || this.config.ikChains.getAllTyped().isEmpty())
        {
            return;
        }

        MatrixCache cache = this.renderer.collectMatrices(this.entity, context.getTransition());
        Matrix4f uiMatrix = context.batcher.getContext().getMatrices().peek().getPositionMatrix();
        BufferBuilder builder = Tessellator.getInstance().getBuffer();
        boolean hasSelected = this.hasSelectedIKChain();

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();

        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        for (IKChainConfig chain : this.config.ikChains.getAllTyped())
        {
            if (!chain.visualizer.get())
            {
                continue;
            }

            boolean selectedChain = this.isSelectedIKChain(chain);
            if (hasSelected && !selectedChain)
            {
                continue;
            }
            float jointR = selectedChain ? 1F : 0.85F;
            float jointG = selectedChain ? 0.95F : 0.9F;
            float jointB = selectedChain ? 0.2F : 0.95F;
            List<Vector3f> points = new ArrayList<>();
            Vector3f previous = null;

            for (String bone : chain.getBones())
            {
                Vector3f point = this.getBonePoint(cache, bone);

                if (point == null)
                {
                    continue;
                }

                if (previous != null)
                {
                    this.boneSolid(builder, uiMatrix, previous, point, jointR, jointG, jointB, selectedChain ? 0.9F : 0.45F, selectedChain ? 0.026F : 0.018F);
                }

                points.add(point);
                previous = point;
            }

            if (previous == null)
            {
                continue;
            }

            if (!chain.getBones().isEmpty())
            {
                String firstBone = chain.getBones().get(0);
                String parentBone = this.getParentBoneId(firstBone);

                if (parentBone != null && !parentBone.isEmpty())
                {
                    Vector3f parentPoint = this.getBonePoint(cache, parentBone);
                    Vector3f firstPoint = this.getBonePoint(cache, firstBone);

                    if (parentPoint != null && firstPoint != null)
                    {
                        this.boneSolid(builder, uiMatrix, parentPoint, firstPoint, jointR, jointG, jointB, selectedChain ? 0.8F : 0.3F, selectedChain ? 0.02F : 0.014F);
                    }
                }
            }

            for (Vector3f point : points)
            {
                this.joint(builder, uiMatrix, point, selectedChain ? 0.03F : 0.022F, jointR, jointG, jointB, selectedChain ? 1F : 0.7F);
            }

            Vector3f target = this.getTargetPoint(chain, cache);
            Vector3f pole = this.getPolePoint(chain, cache);

            if (target == null)
            {
                continue;
            }

            if (!hasSelected || selectedChain)
            {
                this.markerSolid(builder, uiMatrix, target, selectedChain ? 0.06F : 0.045F, 1F, 0.92F, 0.25F, selectedChain ? 1F : 0.5F);
            }

            if (selectedChain || !hasSelected)
            {
                this.boneSolid(builder, uiMatrix, previous, target, 1F, 0.92F, 0.25F, 0.9F, 0.014F);
            }

            if (pole != null)
            {
                Vector3f poleAnchor = chain.getBones().size() > 1 ? this.getBonePoint(cache, chain.getBones().get(1)) : previous;

                if (poleAnchor == null)
                {
                    poleAnchor = previous;
                }

                if (!hasSelected || selectedChain)
                {
                    this.markerSolid(builder, uiMatrix, pole, selectedChain ? 0.052F : 0.04F, 1F, 0.42F, 1F, selectedChain ? 1F : 0.5F);
                }

                if (selectedChain || !hasSelected)
                {
                    this.boneSolid(builder, uiMatrix, poleAnchor, pole, 1F, 0.42F, 1F, 0.9F, 0.012F);
                }
            }
        }

        BufferRenderer.drawWithGlobalProgram(builder.end());

        builder.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        for (IKChainConfig chain : this.config.ikChains.getAllTyped())
        {
            if (!chain.visualizer.get())
            {
                continue;
            }

            boolean selectedChain = this.isSelectedIKChain(chain);
            if (hasSelected && !selectedChain)
            {
                continue;
            }
            float alpha = selectedChain ? 1F : (hasSelected ? 0.2F : 0.35F);
            Vector3f previous = null;

            for (String bone : chain.getBones())
            {
                Vector3f point = this.getBonePoint(cache, bone);

                if (point == null)
                {
                    continue;
                }

                if (previous != null)
                {
                    this.line(builder, uiMatrix, previous, point, 1F, 1F, 1F, alpha);
                }

                previous = point;
            }

            if (!chain.getBones().isEmpty())
            {
                String firstBone = chain.getBones().get(0);
                String parentBone = this.getParentBoneId(firstBone);

                if (parentBone != null && !parentBone.isEmpty())
                {
                    Vector3f parentPoint = this.getBonePoint(cache, parentBone);
                    Vector3f firstPoint = this.getBonePoint(cache, firstBone);

                    if (parentPoint != null && firstPoint != null)
                    {
                        this.line(builder, uiMatrix, parentPoint, firstPoint, 1F, 1F, 1F, alpha);
                    }
                }
            }
        }

        BufferRenderer.drawWithGlobalProgram(builder.end());
        RenderSystem.enableDepthTest();
    }

    private boolean isSelectedIKChain(IKChainConfig chain)
    {
        if (this.selectedBone == null || this.selectedBone.isEmpty() || chain == null)
        {
            return false;
        }

        String virtualId = UIModelIKPanel.extractIKVirtualId(this.selectedBone);

        if (virtualId != null)
        {
            return chain.getId().equals(virtualId);
        }

        for (String bone : chain.getBones())
        {
            if (this.selectedBone.equals(bone))
            {
                return true;
            }
        }

        return false;
    }

    private boolean hasSelectedIKChain()
    {
        if (this.config == null || this.config.ikChains.getAllTyped().isEmpty())
        {
            return false;
        }

        for (IKChainConfig chain : this.config.ikChains.getAllTyped())
        {
            if (this.isSelectedIKChain(chain))
            {
                return true;
            }
        }

        return false;
    }

    private String getParentBoneId(String bone)
    {
        ModelInstance instance = this.getPreviewModelInstance();

        if (instance == null || !(instance.model instanceof Model model))
        {
            return null;
        }

        ModelGroup group = model.getGroup(bone);

        if (group == null || group.parent == null)
        {
            return null;
        }

        return group.parent.id;
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

    private Vector3f getTargetPoint(IKChainConfig chain, MatrixCache cache)
    {
        if (chain.useTargetBone.get() && !chain.targetBone.get().isEmpty())
        {
            Vector3f target = this.getBonePoint(cache, chain.targetBone.get());

            if (target != null)
            {
                return target;
            }
        }

        String parent = chain.targetParentBone.get();

        if (parent != null && !parent.isEmpty() && chain.getBones().contains(parent))
        {
            parent = "";
        }

        return this.toWorldPoint(new Vector3f(chain.target.translate).mul(1F / 16F), parent, cache);
    }

    private Vector3f getPolePoint(IKChainConfig chain, MatrixCache cache)
    {
        if (chain.usePoleBone.get() && !chain.poleBone.get().isEmpty())
        {
            Vector3f pole = this.getBonePoint(cache, chain.poleBone.get());

            if (pole != null)
            {
                return pole;
            }
        }

        Vector3f point = this.toWorldPoint(new Vector3f(chain.pole.translate).mul(1F / 16F), "", cache);

        return point.lengthSquared() < 0.0000001F ? null : point;
    }

    private Vector3f toWorldPoint(Vector3f point, String parentBone, MatrixCache cache)
    {
        if (cache == null)
        {
            return point;
        }

        Matrix4f parentMatrix = null;

        if (parentBone != null && !parentBone.isEmpty())
        {
            MatrixCacheEntry parentEntry = cache.get(parentBone);

            if (parentEntry != null)
            {
                parentMatrix = parentEntry.origin() == null ? parentEntry.matrix() : parentEntry.origin();
            }
        }
        else
        {
            MatrixCacheEntry rootEntry = cache.get("");

            if (rootEntry != null)
            {
                parentMatrix = rootEntry.origin() == null ? rootEntry.matrix() : rootEntry.origin();
            }
        }

        if (parentMatrix == null)
        {
            return point;
        }

        Vector3f world = new Vector3f();
        parentMatrix.transformPosition(point, world);

        return world;
    }

    private Matrix4f getSelectedBoneMatrix(String selectedBone, MatrixCache cache)
    {
        if (selectedBone == null || selectedBone.isEmpty())
        {
            return null;
        }

        MatrixCacheEntry entry = cache.get(selectedBone);

        if (entry != null)
        {
            Matrix4f matrix = entry.origin();

            if (matrix == null)
            {
                matrix = entry.matrix();
            }

            return matrix;
        }

        if (this.config == null)
        {
            return null;
        }

        IKChainConfig chain = null;
        String id = UIModelIKPanel.extractIKVirtualId(selectedBone);

        if (id == null)
        {
            return null;
        }

        for (IKChainConfig candidate : this.config.ikChains.getAllTyped())
        {
            if (candidate.getId().equals(id))
            {
                chain = candidate;
                break;
            }
        }

        if (chain == null)
        {
            return null;
        }

        Vector3f position;

        if (UIModelIKPanel.isIKPoleVirtualBoneName(selectedBone))
        {
            position = this.getPolePoint(chain, cache);
        }
        else
        {
            position = this.getTargetPoint(chain, cache);
        }

        if (position == null)
        {
            return null;
        }

        return new Matrix4f().translate(position);
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

    private void joint(BufferBuilder builder, Matrix4f matrix, Vector3f p, float size, float r, float g, float b, float a)
    {
        this.line(builder, matrix, new Vector3f(p).add(-size, 0, 0), new Vector3f(p).add(size, 0, 0), r, g, b, a);
        this.line(builder, matrix, new Vector3f(p).add(0, -size, 0), new Vector3f(p).add(0, size, 0), r, g, b, a);
    }

    private void targetMarker(BufferBuilder builder, Matrix4f matrix, Vector3f p, float size, float r, float g, float b, float a)
    {
        Vector3f x1 = new Vector3f(p).add(size, 0, 0);
        Vector3f x2 = new Vector3f(p).add(-size, 0, 0);
        Vector3f z1 = new Vector3f(p).add(0, 0, size);
        Vector3f z2 = new Vector3f(p).add(0, 0, -size);
        Vector3f y1 = new Vector3f(p).add(0, size * 0.6F, 0);
        Vector3f y2 = new Vector3f(p).add(0, -size * 0.6F, 0);

        this.line(builder, matrix, x1, z1, r, g, b, a);
        this.line(builder, matrix, z1, x2, r, g, b, a);
        this.line(builder, matrix, x2, z2, r, g, b, a);
        this.line(builder, matrix, z2, x1, r, g, b, a);
        this.line(builder, matrix, y1, y2, r, g, b, a);
        this.line(builder, matrix, x1, x2, r, g, b, a);
    }

    private void boneSolid(BufferBuilder builder, Matrix4f matrix, Vector3f from, Vector3f to, float r, float g, float b, float a, float radius)
    {
        Vector3f dir = new Vector3f(to).sub(from);

        if (dir.lengthSquared() < 0.0000001F)
        {
            return;
        }

        dir.normalize();

        Vector3f up = Math.abs(dir.y) > 0.9F ? new Vector3f(1F, 0F, 0F) : new Vector3f(0F, 1F, 0F);
        Vector3f sideA = dir.cross(up, new Vector3f()).normalize().mul(radius);
        Vector3f sideB = dir.cross(sideA, new Vector3f()).normalize().mul(radius);
        Vector3f base = new Vector3f(from).lerp(to, 0.2F);

        Vector3f p1 = new Vector3f(base).add(sideA);
        Vector3f p2 = new Vector3f(base).add(sideB);
        Vector3f p3 = new Vector3f(base).sub(sideA);
        Vector3f p4 = new Vector3f(base).sub(sideB);
        Vector3f tip = new Vector3f(to);

        this.triangle(builder, matrix, p1, p2, tip, r, g, b, a);
        this.triangle(builder, matrix, p2, p3, tip, r, g, b, a);
        this.triangle(builder, matrix, p3, p4, tip, r, g, b, a);
        this.triangle(builder, matrix, p4, p1, tip, r, g, b, a);
        this.triangle(builder, matrix, p1, p2, p3, r, g, b, a * 0.7F);
        this.triangle(builder, matrix, p3, p4, p1, r, g, b, a * 0.7F);
    }

    private void markerSolid(BufferBuilder builder, Matrix4f matrix, Vector3f center, float size, float r, float g, float b, float a)
    {
        Vector3f top = new Vector3f(center).add(0, size, 0);
        Vector3f bottom = new Vector3f(center).add(0, -size, 0);
        Vector3f right = new Vector3f(center).add(size, 0, 0);
        Vector3f left = new Vector3f(center).add(-size, 0, 0);
        Vector3f front = new Vector3f(center).add(0, 0, size);
        Vector3f back = new Vector3f(center).add(0, 0, -size);

        this.triangle(builder, matrix, top, right, front, r, g, b, a);
        this.triangle(builder, matrix, top, front, left, r, g, b, a);
        this.triangle(builder, matrix, top, left, back, r, g, b, a);
        this.triangle(builder, matrix, top, back, right, r, g, b, a);
        this.triangle(builder, matrix, bottom, front, right, r, g, b, a);
        this.triangle(builder, matrix, bottom, left, front, r, g, b, a);
        this.triangle(builder, matrix, bottom, back, left, r, g, b, a);
        this.triangle(builder, matrix, bottom, right, back, r, g, b, a);
    }

    private void triangle(BufferBuilder builder, Matrix4f matrix, Vector3f a, Vector3f b, Vector3f c, float r, float g, float bl, float alpha)
    {
        builder.vertex(matrix, a.x, a.y, a.z).color(r, g, bl, alpha).next();
        builder.vertex(matrix, b.x, b.y, b.z).color(r, g, bl, alpha).next();
        builder.vertex(matrix, c.x, c.y, c.z).color(r, g, bl, alpha).next();
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
