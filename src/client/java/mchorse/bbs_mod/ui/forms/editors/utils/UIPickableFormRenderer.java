package mchorse.bbs_mod.ui.forms.editors.utils;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.model.IKChainConfig;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.FormRenderType;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.forms.editors.UIFormEditor;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.ui.model.UIModelIKPanel;
import mchorse.bbs_mod.ui.utils.Gizmo;
import mchorse.bbs_mod.ui.utils.StencilFormFramebuffer;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.StringUtils;
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
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import java.util.function.Supplier;

public class UIPickableFormRenderer extends UIFormRenderer
{
    public UIFormEditor formEditor;

    private boolean update;

    private StencilFormFramebuffer stencil = new StencilFormFramebuffer();
    private StencilMap stencilMap = new StencilMap();

    private IEntity target;
    private Supplier<Boolean> renderForm;

    public UIPickableFormRenderer(UIFormEditor formEditor)
    {
        this.formEditor = formEditor;
    }

    public void updatable()
    {
        this.update = true;
    }

    public StencilFormFramebuffer getStencil()
    {
        return this.stencil;
    }

    public void setRenderForm(Supplier<Boolean> renderForm)
    {
        this.renderForm = renderForm;
    }

    public IEntity getTargetEntity()
    {
        return this.target == null ? this.entity : this.target;
    }

    public void setTarget(IEntity target)
    {
        this.target = target;
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
        if (this.formEditor.clickViewport(context, this.stencil))
        {
            return true;
        }

        return super.subMouseClicked(context);
    }

    @Override
    protected void renderUserModel(UIContext context)
    {
        if (this.form == null)
        {
            return;
        }

        this.formEditor.preFormRender(context, this.form);

        FormRenderingContext formContext = new FormRenderingContext()
            .set(FormRenderType.PREVIEW, this.target == null ? this.entity : this.target, context.batcher.getContext().getMatrices(), LightmapTextureManager.pack(15, 15), OverlayTexture.DEFAULT_UV, context.getTransition())
            .camera(this.camera)
            .modelRenderer()
            .equipment(false);

        if (this.renderForm == null || this.renderForm.get())
        {
            FormUtilsClient.render(this.form, formContext);
            this.renderIKTargets(context, false, null);

            if (this.form.hitbox.get())
            {
                this.renderFormHitbox(context);
            }
        }

        if (this.area.isInside(context))
        {
            GlStateManager._disableScissorTest();

            this.stencilMap.setup();
            this.stencil.apply();

            FormUtilsClient.render(this.form, formContext.stencilMap(this.stencilMap));
            this.renderIKTargets(context, true, this.stencilMap);

            Matrix4f matrix = this.formEditor.getOrigin(context.getTransition());
            MatrixStack stack = context.batcher.getContext().getMatrices();

            stack.push();

            if (matrix != null)
            {
                MatrixStackUtils.multiply(stack, matrix);
            }

            RenderSystem.disableCull();
            Gizmo.INSTANCE.renderStencil(stack, this.stencilMap);
            RenderSystem.enableCull();

            stack.pop();

            this.stencil.pickGUI(context, this.area);
            this.stencil.unbind(this.stencilMap);

            MinecraftClient.getInstance().getFramebuffer().beginWrite(true);

            GlStateManager._enableScissorTest();
        }
        else
        {
            this.stencil.clearPicking();
        }

        this.prepareGizmoRenderState();
        this.renderAxes(context);
    }

    private void prepareGizmoRenderState()
    {
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.disableBlend();
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    private void renderAxes(UIContext context)
    {
        Matrix4f matrix = this.formEditor.getOrigin(context.getTransition());
        MatrixStack stack = context.batcher.getContext().getMatrices();

        stack.push();

        if (matrix != null)
        {
            MatrixStackUtils.multiply(stack, matrix);
        }

        /* Draw axes */
        if (UIBaseMenu.renderAxes)
        {
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            Gizmo.INSTANCE.render(stack);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
        }

        stack.pop();
    }

    private void renderIKTargets(UIContext context, boolean stencil, StencilMap map)
    {
        if (!(this.form instanceof ModelForm modelForm))
        {
            return;
        }

        ModelFormRenderer renderer = (ModelFormRenderer) FormUtilsClient.getRenderer(modelForm);
        ModelInstance instance = renderer.getModel();

        if (instance == null || instance.ikChains.isEmpty())
        {
            return;
        }

        MatrixCache cache = renderer.collectMatrices(this.getTargetEntity(), context.getTransition());
        MatrixStack stack = context.batcher.getContext().getMatrices();
        BufferBuilder builder = Tessellator.getInstance().getBuffer();

        if (!stencil)
        {
            RenderSystem.disableDepthTest();
            builder.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

            for (IKChainConfig chain : instance.ikChains)
            {
                if (!chain.visualizer.get())
                {
                    continue;
                }

                Vector3f previous = null;

                for (String bone : chain.getBones())
                {
                    Matrix4f matrix = this.getBoneMatrix(cache, bone);

                    if (matrix == null)
                    {
                        continue;
                    }

                    Vector3f point = new Vector3f();
                    matrix.getTranslation(point);

                    if (previous != null)
                    {
                        this.line(builder, stack, previous, point, 0F, 1F, 1F, 1F);
                    }

                    previous = point;
                }

                Vector3f target = this.getTargetPoint(chain, cache);
                Vector3f pole = this.getPolePoint(chain, cache);

                if (previous != null && target != null)
                {
                    this.line(builder, stack, previous, target, 1F, 1F, 0F, 1F);
                }

                if (pole != null)
                {
                    Vector3f poleAnchor = chain.getBones().size() > 1 ? this.getBonePoint(cache, chain.getBones().get(1)) : previous;

                    if (poleAnchor == null)
                    {
                        poleAnchor = previous;
                    }

                    if (poleAnchor != null)
                    {
                        this.line(builder, stack, poleAnchor, pole, 1F, 0.4F, 1F, 1F);
                    }
                }
            }

            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            BufferRenderer.drawWithGlobalProgram(builder.end());
            RenderSystem.enableDepthTest();
        }

        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        for (IKChainConfig chain : instance.ikChains)
        {
            if (!chain.visualizer.get())
            {
                continue;
            }

            Vector3f target = this.getTargetPoint(chain, cache);

            if (target == null)
            {
                continue;
            }

            float r = 1F;
            float g = 0.9F;
            float b = 0.2F;
            String virtualBone = UIModelIKPanel.IK_TARGET_PREFIX + chain.getId();

            if (stencil && map != null)
            {
                int index = map.objectIndex++;

                map.indexMap.put(index, new Pair<>(this.form, virtualBone));
                r = (index & 255) / 255F;
                g = ((index >> 8) & 255) / 255F;
                b = ((index >> 16) & 255) / 255F;
            }

            float s = 0.04F;
            float y = target.y;

            Draw.fillQuad(builder, stack,
                target.x - s, y, target.z - s,
                target.x + s, y, target.z - s,
                target.x + s, y, target.z + s,
                target.x - s, y, target.z + s,
                r, g, b, 1F
            );
            Draw.fillQuad(builder, stack,
                target.x - s, y, target.z + s,
                target.x + s, y, target.z + s,
                target.x + s, y, target.z - s,
                target.x - s, y, target.z - s,
                r, g, b, 1F
            );

            Vector3f pole = this.getPolePoint(chain, cache);

            if (pole != null)
            {
                float pr = 1F;
                float pg = 0.4F;
                float pb = 1F;
                String poleVirtualBone = UIModelIKPanel.IK_POLE_PREFIX + chain.getId();

                if (stencil && map != null)
                {
                    int poleIndex = map.objectIndex++;

                    map.indexMap.put(poleIndex, new Pair<>(this.form, poleVirtualBone));
                    pr = (poleIndex & 255) / 255F;
                    pg = ((poleIndex >> 8) & 255) / 255F;
                    pb = ((poleIndex >> 16) & 255) / 255F;
                }

                float ps = 0.035F;
                float py = pole.y;

                Draw.fillQuad(builder, stack,
                    pole.x - ps, py, pole.z - ps,
                    pole.x + ps, py, pole.z - ps,
                    pole.x + ps, py, pole.z + ps,
                    pole.x - ps, py, pole.z + ps,
                    pr, pg, pb, 1F
                );
                Draw.fillQuad(builder, stack,
                    pole.x - ps, py, pole.z + ps,
                    pole.x + ps, py, pole.z + ps,
                    pole.x + ps, py, pole.z - ps,
                    pole.x - ps, py, pole.z - ps,
                    pr, pg, pb, 1F
                );
            }
        }

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    private void line(BufferBuilder builder, MatrixStack stack, Vector3f a, Vector3f b, float r, float g, float bl, float alpha)
    {
        Matrix4f matrix = stack.peek().getPositionMatrix();

        builder.vertex(matrix, a.x, a.y, a.z).color(r, g, bl, alpha).next();
        builder.vertex(matrix, b.x, b.y, b.z).color(r, g, bl, alpha).next();
    }

    private Vector3f getTargetPoint(IKChainConfig chain, MatrixCache cache)
    {
        if (chain.useTargetBone.get() && !chain.targetBone.get().isEmpty())
        {
            Matrix4f matrix = this.getBoneMatrix(cache, chain.targetBone.get());

            if (matrix != null)
            {
                Vector3f vector = new Vector3f();
                matrix.getTranslation(vector);

                return vector;
            }
        }

        if (!chain.targetParentBone.get().isEmpty())
        {
            Matrix4f matrix = this.getBoneMatrix(cache, chain.targetParentBone.get());

            if (matrix != null)
            {
                Vector3f world = new Vector3f();

                matrix.transformPosition(new Vector3f(chain.target.translate).mul(1F / 16F), world);

                return world;
            }
        }

        return new Vector3f(chain.target.translate).mul(1F / 16F);
    }

    private Vector3f getPolePoint(IKChainConfig chain, MatrixCache cache)
    {
        if (chain.usePoleBone.get() && !chain.poleBone.get().isEmpty())
        {
            Matrix4f matrix = this.getBoneMatrix(cache, chain.poleBone.get());

            if (matrix != null)
            {
                Vector3f vector = new Vector3f();
                matrix.getTranslation(vector);

                return vector;
            }
        }

        Vector3f point = new Vector3f(chain.pole.translate).mul(1F / 16F);
        Matrix4f root = this.getBoneMatrix(cache, "");

        if (root != null)
        {
            Vector3f world = new Vector3f();
            root.transformPosition(point, world);
            point = world;
        }

        return point.lengthSquared() < 0.0000001F ? null : point;
    }

    private Vector3f getBonePoint(MatrixCache cache, String bone)
    {
        Matrix4f matrix = this.getBoneMatrix(cache, bone);

        if (matrix == null)
        {
            return null;
        }

        Vector3f point = new Vector3f();
        matrix.getTranslation(point);

        return point;
    }

    private Matrix4f getBoneMatrix(MatrixCache cache, String bone)
    {
        mchorse.bbs_mod.forms.renderers.utils.MatrixCacheEntry entry = cache.get(bone);

        if (entry == null && this.form != null && bone != null && !bone.isEmpty())
        {
            String path = StringUtils.combinePaths(FormUtils.getPath(this.form), bone);
            entry = cache.get(path);
        }

        if (entry == null)
        {
            return null;
        }

        return entry.origin() == null ? entry.matrix() : entry.origin();
    }

    private void renderFormHitbox(UIContext context)
    {
        float hitboxW = this.form.hitboxWidth.get();
        float hitboxH = this.form.hitboxHeight.get();
        float eyeHeight = hitboxH * this.form.hitboxEyeHeight.get();

        /* Draw look vector */
        final float thickness = 0.01F;
        Draw.renderBox(context.batcher.getContext().getMatrices(), -thickness, -thickness + eyeHeight, -thickness, thickness, thickness, 2F, 1F, 0F, 0F);

        /* Draw hitbox */
        Draw.renderBox(context.batcher.getContext().getMatrices(), -hitboxW / 2, 0, -hitboxW / 2, hitboxW, hitboxH, hitboxW);
    }

    @Override
    protected void update()
    {
        super.update();

        if (this.update && this.target != null)
        {
            this.form.update(this.entity);
        }
    }

    @Override
    public void render(UIContext context)
    {
        super.render(context);

        if (!this.stencil.hasPicked())
        {
            return;
        }

        int index = this.stencil.getIndex();
        Texture texture = this.stencil.getFramebuffer().getMainTexture();
        Pair<Form, String> pair = this.stencil.getPicked();
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

        if (pair != null && pair.a != null)
        {
            String label = pair.a.getFormIdOrName();

            if (!pair.b.isEmpty())
            {
                label += " - " + pair.b;
            }

            context.batcher.textCard(label, context.mouseX + 12, context.mouseY + 8);
        }
    }

    @Override
    protected void renderGrid(UIContext context)
    {
        if (this.renderForm == null || this.renderForm.get())
        {
            super.renderGrid(context);
        }
    }
}
