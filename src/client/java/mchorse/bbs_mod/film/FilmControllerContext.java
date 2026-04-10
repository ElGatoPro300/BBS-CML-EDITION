package mchorse.bbs_mod.film;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import io.netty.util.collection.IntObjectMap;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;

public class FilmControllerContext
{
    public final static FilmControllerContext instance = new FilmControllerContext();

    public IntObjectMap<IEntity> entities;
    public IEntity entity;
    public Replay replay;
    public Camera camera;
    public PoseStack stack;
    public MultiBufferSource consumers;
    public StencilMap map;

    public float transition;
    public int color;
    public float shadowRadius;
    public float shadowOpacity;

    public String bone;
    public boolean local;

    public String bone2;
    public boolean local2;

    public String nameTag = "";
    public boolean relative;
    public boolean isShadowPass;
    public Matrix4f localGroupTransform;
    public Matrix4f viewMatrix;

    private FilmControllerContext()
    {}

    private void reset()
    {
        this.map = null;
        this.shadowRadius = 0F;
        this.shadowOpacity = 1F;
        this.color = Colors.WHITE;
        this.bone = null;
        this.local = false;
        this.nameTag = "";
        this.relative = false;
        this.isShadowPass = false;
        this.localGroupTransform = null;
        this.viewMatrix = null;
    }

    public FilmControllerContext setup(IntObjectMap<IEntity> entities, IEntity entity, Replay replay, LevelRenderContext context)
    {
        this.reset();

        this.entities = entities;
        this.entity = entity;
        this.replay = replay;
        this.camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        this.stack = context.poseStack();
        if (this.stack == null)
        {
            this.stack = new PoseStack();
            MatrixStackUtils.multiply(this.stack, RenderSystem.getModelViewMatrix());
        }
        this.consumers = context.bufferSource();
        this.transition = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);

        return this;
    }

    public FilmControllerContext setup(IntObjectMap<IEntity> entities, IEntity entity, Replay replay, Camera camera, PoseStack stack, MultiBufferSource consumers, float transition)
    {
        this.reset();

        this.entities = entities;
        this.entity = entity;
        this.replay = replay;
        this.camera = camera;
        this.stack = stack;
        this.consumers = consumers;
        this.transition = transition;

        return this;
    }

    public FilmControllerContext transition(float transition)
    {
        this.transition = transition;

        return this;
    }

    public FilmControllerContext stencil(StencilMap map)
    {
        this.map = map;

        return this;
    }

    public FilmControllerContext viewMatrix(Matrix4f viewMatrix)
    {
        this.viewMatrix = viewMatrix;

        return this;
    }

    public FilmControllerContext shadow(boolean shadow, float shadowRadius)
    {
        this.shadowRadius = shadow ? shadowRadius : 0F;
        this.shadowOpacity = 1F;

        return this;
    }

    public FilmControllerContext shadow(boolean shadow, float shadowRadius, float shadowOpacity)
    {
        this.shadowRadius = shadow ? shadowRadius : 0F;
        this.shadowOpacity = shadow ? shadowOpacity : 0F;

        return this;
    }

    public FilmControllerContext shadow(float shadowRadius)
    {
        this.shadowRadius = shadowRadius;
        this.shadowOpacity = 1F;

        return this;
    }

    public FilmControllerContext color(int overlayColor)
    {
        this.color = overlayColor;

        return this;
    }

    public FilmControllerContext bone(String bone, boolean local)
    {
        this.bone = bone;
        this.local = local;

        return this;
    }

    public FilmControllerContext bone2(String bone, boolean local)
    {
        this.bone2 = bone;
        this.local2 = local;

        return this;
    }

    public FilmControllerContext nameTag(String nameTag)
    {
        this.nameTag = nameTag;

        return this;
    }

    public FilmControllerContext relative(boolean relative)
    {
        this.relative = relative;

        return this;
    }

    public FilmControllerContext isShadowPass(boolean isShadowPass)
    {
        this.isShadowPass = isShadowPass;

        return this;
    }
}