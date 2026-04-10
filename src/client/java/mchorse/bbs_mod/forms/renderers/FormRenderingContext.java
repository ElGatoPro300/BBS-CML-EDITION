package mchorse.bbs_mod.forms.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.utils.MathUtils;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;

public class FormRenderingContext
{
    public FormRenderType type;
    public IEntity entity;
    public PoseStack stack;
    public int light;
    public int overlay;
    public float transition;
    public final Camera camera = new Camera();
    public StencilMap stencilMap;
    public boolean ui;
    public int color;
    public boolean modelRenderer;
    public boolean relative;
    public boolean isShadowPass;
    public Matrix4f viewMatrix;

    public FormRenderingContext()
    {}

    public FormRenderingContext set(FormRenderType type, IEntity entity, PoseStack stack, int light, int overlay, float transition)
    {
        this.type = type == null ? FormRenderType.ENTITY : type;
        this.entity = entity;
        this.stack = stack;
        this.light = light;
        this.overlay = overlay;
        this.transition = transition;
        this.stencilMap = null;
        this.ui = false;
        this.color = 0xffffffff;
        this.relative = false;
        this.isShadowPass = false;
        this.viewMatrix = null;

        return this;
    }

    public FormRenderingContext camera(Camera camera)
    {
        this.camera.copy(camera);
        this.camera.updateView();

        return this;
    }

    public FormRenderingContext camera(net.minecraft.client.Camera camera)
    {
        this.camera.position.set(camera.position().x, camera.position().y, camera.position().z);
        this.camera.rotation.set(MathUtils.toRad(-camera.xRot()), MathUtils.toRad(camera.yRot()), 0F);
        this.camera.fov = MathUtils.toRad(Minecraft.getInstance().options.fov().get());
        this.camera.view.identity().rotate(camera.rotation());

        return this;
    }

    public FormRenderingContext stencilMap(StencilMap stencilMap)
    {
        this.stencilMap = stencilMap;

        return this;
    }

    public FormRenderingContext inUI()
    {
        this.ui = true;

        return this;
    }

    public FormRenderingContext color(int color)
    {
        this.color = color;

        return this;
    }

    public FormRenderingContext modelRenderer()
    {
        this.modelRenderer = true;

        return this;
    }

    public float getTransition()
    {
        return this.transition;
    }

    public boolean isPicking()
    {
        return this.stencilMap != null;
    }

    public int getPickingIndex()
    {
        return this.stencilMap == null ? -1 : this.stencilMap.objectIndex;
    }
}
