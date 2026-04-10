package mchorse.bbs_mod.forms.renderers;

import com.mojang.blaze3d.opengl.GlProgram;
import com.mojang.blaze3d.opengl.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.BodyPart;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.settings.values.core.ValueTransform;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.keys.KeyCodes;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.interps.Lerps;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.InteractionHand;
import org.joml.Matrix4f;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public abstract class FormRenderer <T extends Form>
{
    private static boolean suppressFormDisplayName;

    public static void setSuppressFormDisplayName(boolean suppress)
    {
        suppressFormDisplayName = suppress;
    }

    protected T form;

    public FormRenderer(T form)
    {
        this.form = form;
    }

    public T getForm()
    {
        return this.form;
    }

    public List<String> getBones()
    {
        return Collections.emptyList();
    }

    public final void renderUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        this.renderInUI(context, x1, y1, x2, y2);

        FontRenderer font = context.batcher.getFont();
        String name = this.form.name.get();

        if (!suppressFormDisplayName && !name.isEmpty())
        {
            name = font.limitToWidth(name, x2 - x1 - 3);

            int w = font.getWidth(name);

            context.batcher.textCard(name, (x2 + x1 - w) / 2, y1 + 6, Colors.WHITE, Colors.ACTIVE | Colors.A50);
        }

        int keybind = this.form.hotkey.get();

        if (keybind > 0)
        {
            name = KeyCodes.getName(keybind);
            name = font.limitToWidth(name, x2 - x1 - 3);

            int w = font.getWidth(name);

            context.batcher.textCard(name, (x2 + x1 - w) / 2, y2 - 6 - font.getHeight(), Colors.WHITE, Colors.A50);
        }
    }

    protected abstract void renderInUI(UIContext context, int x1, int y1, int x2, int y2);

    public boolean renderArm(PoseStack matrices, int light, AbstractClientPlayer player, InteractionHand hand)
    {
        return false;
    }

    public final void render(FormRenderingContext context)
    {
        if (!this.form.shaderShadow.get() && BBSRendering.isIrisShadowPass())
        {
            return;
        }

        this.form.applyStates(context.transition);

        int light = context.light;
        boolean visible = this.form.visible.get();

        if (!visible)
        {
            return;
        }

        boolean isPicking = context.stencilMap != null;

        context.stack.pushPose();
        this.applyTransforms(context.stack, false, context.getTransition());

        float lf = 1F - MathUtils.clamp(this.form.lighting.get(), 0F, 1F);
        int u = context.light & '\uffff';
        int v = context.light >> 16 & '\uffff';

        u = (int) Lerps.lerp(u, LightTexture.FULL_BLOCK, lf);
        context.light = u | v << 16;

        this.render3D(context);

        if (isPicking)
        {
            this.updateStencilMap(context);
        }

        this.renderBodyParts(context);

        context.stack.popPose();

        context.light = light;

        this.form.unapplyStates();
    }

    protected void applyTransforms(PoseStack stack, boolean origin, float transition)
    {
        Transform transform = this.createTransform();

        if (origin)
        {
            stack.translate(transform.translate.x, transform.translate.y, transform.translate.z);
        }
        else
        {
            MatrixStackUtils.applyTransform(stack, transform);
        }
    }

    protected void applyTransforms(Matrix4f matrix, float transition)
    {
        matrix.mul(this.createTransform().createMatrix());
    }

    protected Transform createTransform()
    {
        Transform transform = new Transform();

        transform.copy(this.form.transform.get());
        this.applyTransform(transform, this.form.transformOverlay.get());

        for (ValueTransform t : this.form.additionalTransforms)
        {
            this.applyTransform(transform, t.get());
        }

        return transform;
    }

    private void applyTransform(Transform transform, Transform overlay)
    {
        transform.translate.add(overlay.translate);
        transform.scale.add(overlay.scale).sub(1, 1, 1);
        transform.rotate.add(overlay.rotate);
        transform.rotate2.add(overlay.rotate2);
        transform.pivot.add(overlay.pivot);
    }

    protected Supplier<GlProgram> getShader(FormRenderingContext context, Supplier<GlProgram> normal, Supplier<GlProgram> picking)
    {
        if (context.isPicking())
        {
            GlProgram program = picking.get();

            if (program == null)
            {
                return normal;
            }

            this.setupTarget(context, program);

            return () -> program;
        }

        return normal;
    }

    protected void setupTarget(FormRenderingContext context, GlProgram program)
    {
        if (program == null)
        {
            return;
        }

        bindShaderProgram(program);

        Uniform target = program.getUniform("Target");

        if (target != null)
        {
            int pickingIndex = context.getPickingIndex();

            /* no-op uniform */ // target.set(pickingIndex);
        }
    }

    private static void bindShaderProgram(GlProgram program)
    {
        try
        {
            Method setShader = RenderSystem.class.getMethod("setShader", Supplier.class);
            setShader.invoke(null, (Supplier<GlProgram>) () -> program);

            return;
        }
        catch (Exception ignored)
        {}

        try
        {
            Method setShaderProgram = RenderSystem.class.getMethod("setShaderProgram", GlProgram.class);
            setShaderProgram.invoke(null, program);
        }
        catch (Exception ignored)
        {}
    }

    protected void updateStencilMap(FormRenderingContext context)
    {
        context.stencilMap.addPicking(this.form);
    }

    protected void render3D(FormRenderingContext context)
    {}

    public void renderBodyParts(FormRenderingContext context)
    {
        for (BodyPart part : this.form.parts.getAllTyped())
        {
            this.renderBodyPart(part, context);
        }
    }

    protected void renderBodyPart(BodyPart part, FormRenderingContext context)
    {
        IEntity oldEntity = context.entity;

        context.entity = part.useTarget.get() ? oldEntity : part.getEntity();

        if (part.getForm() != null)
        {
            context.stack.pushPose();
            MatrixStackUtils.applyTransform(context.stack, part.transform.get());

            FormUtilsClient.render(part.getForm(), context);

            context.stack.popPose();
        }

        context.entity = oldEntity;
    }

    public MatrixCache collectMatrices(IEntity entity, float transition)
    {
        MatrixCache map = new MatrixCache();
        PoseStack stack = new PoseStack();

        this.collectMatrices(entity, stack, map, "", transition);

        return map;
    }

    public void collectMatrices(IEntity entity, PoseStack stack, MatrixCache matrices, String prefix, float transition)
    {
        Matrix4f mm = new Matrix4f();
        Matrix4f oo = new Matrix4f();

        stack.pushPose();
        this.applyTransforms(stack, true, transition);
        oo.set(new Matrix4f());
        stack.popPose();

        stack.pushPose();
        this.applyTransforms(stack, false, transition);
        mm.set(new Matrix4f());

        matrices.put(prefix, mm, oo);

        int i = 0;

        for (BodyPart part : this.form.parts.getAllTyped())
        {
            Form form = part.getForm();

            if (form != null)
            {
                stack.pushPose();
                MatrixStackUtils.applyTransform(stack, part.transform.get());

                FormUtilsClient.getRenderer(form).collectMatrices(entity, stack, matrices, StringUtils.combinePaths(prefix, String.valueOf(i)), transition);

                stack.popPose();
            }

            i += 1;
        }

        stack.popPose();
    }
}
