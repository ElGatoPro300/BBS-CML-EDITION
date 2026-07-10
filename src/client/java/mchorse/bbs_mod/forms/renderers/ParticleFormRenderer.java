package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.forms.ITickable;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.ParticleForm;
import mchorse.bbs_mod.graphics.texture.AdoptedTexture;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.particles.ParticleScheme;
import mchorse.bbs_mod.particles.emitter.ParticleEmitter;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.joml.Vectors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;

public class ParticleFormRenderer extends FormRenderer<ParticleForm> implements ITickable
{
    public static long lastUpdate = 0L;

    private ParticleEmitter emitter;
    private boolean checked;
    private boolean restart;
    private long lastParticleUpdate = lastUpdate;

    public ParticleFormRenderer(ParticleForm form)
    {
        super(form);
    }

    public ParticleEmitter getEmitter()
    {
        return this.emitter;
    }

    public void ensureEmitter(World world, float transition)
    {
        if (this.lastParticleUpdate < lastUpdate)
        {
            this.lastParticleUpdate = lastUpdate;
            this.checked = false;
        }

        if (!this.checked)
        {
            ParticleScheme scheme = BBSModClient.getParticles().load(this.form.effect.get());

            if (scheme != null)
            {
                this.emitter = new ParticleEmitter();
                this.emitter.setScheme(scheme);
                this.emitter.setWorld(world);
            }

            this.checked = true;
        }

        if (this.emitter != null && !BBSRendering.isIrisShadowPass())
        {
            boolean lastPaused = this.emitter.paused;

            this.emitter.paused = this.form.paused.get();

            if (lastPaused != this.emitter.paused && !this.emitter.paused && this.emitter.age > 0 && !this.restart)
            {
                this.restart = true;
            }
        }
    }

    @Override
    public void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        this.ensureEmitter(MinecraftClient.getInstance().world, context.getTransition());

        ParticleEmitter emitter = this.emitter;

        if (emitter != null)
        {
            MatrixStack stack = new MatrixStack();
            int scale = (y2 - y1) / 2;

            stack.push();
            stack.translate((x2 + x1) / 2, (y2 + y1) / 2, 40);
            MatrixStackUtils.scaleStack(stack, scale, scale, scale);

            this.updateTexture(context.getTransition());
            emitter.lastGlobal.set(new Vector3f(0, 0, 0));
            emitter.rotation.identity();
            emitter.renderUI(stack, context.getTransition());

            stack.pop();
        }
    }

    @Override
    public void render3D(FormRenderingContext context)
    {
        this.ensureEmitter(MinecraftClient.getInstance().world, context.transition);

        ParticleEmitter emitter = this.emitter;

        if (emitter != null)
        {
            emitter.setUserVariables(
                this.form.user1.get(),
                this.form.user2.get(),
                this.form.user3.get(),
                this.form.user4.get(),
                this.form.user5.get(),
                this.form.user6.get()
            );

            this.updateTexture(context.getTransition());

            Matrix4f stackMatrix = new Matrix4f(context.stack.peek().getPositionMatrix());

            if (BBSRendering.isIrisShadersEnabled() && !context.modelRenderer && context.entity != null)
            {
                stackMatrix = BBSRendering.stripTerrainPositionMatrix(stackMatrix);
            }

            Matrix4f matrix = new Matrix4f(MatrixStackUtils.getInverseViewRotationMatrix());

            matrix.mul(stackMatrix);

            Vector3d translation = new Vector3d(matrix.getTranslation(Vectors.TEMP_3F));
            translation.add(context.camera.position.x, context.camera.position.y, context.camera.position.z);

            context.stack.push();
            context.stack.loadIdentity();
            context.stack.multiplyPositionMatrix(MatrixStackUtils.getViewRotationMatrix());

            emitter.lastGlobal.set(translation);
            emitter.rotation.set(matrix);
            emitter.modelRenderer = context.modelRenderer;

            if (!BBSRendering.isIrisShadowPass())
            {
                boolean billboard = BBSRendering.isIrisShadersEnabled();
                VertexFormat format = billboard ? VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL : VertexFormats.POSITION_TEXTURE_COLOR_LIGHT;
                RenderLayer layer = billboard ? RenderLayers.entityTranslucent(this.getParticleTextureId()) : BBSShaders.getParticlesLayer();

                if (context.isPicking())
                {
                    /* 1.21.11 render: ParticleEmitter#render only exposes a RenderLayer sink (see
                     * .port_1.21.11_notes.md #5/#6) with no way to intercept its BuiltBuffer and hand it to
                     * BBSPickerRenderer, so per-particle picking loses pixel accuracy the same way block/label
                     * picking does elsewhere in this pass — setupTarget still records the index for whatever
                     * else consults it, but the particles still draw through their normal RenderLayer. */
                    this.setupTarget(context, null);
                }

                emitter.render(format, layer, context.stack, context.overlay, context.getTransition());
            }

            context.stack.pop();
        }
    }

    private Identifier getParticleTextureId()
    {
        Link textureLink = this.emitter.texture != null ? this.emitter.texture : this.emitter.scheme.texture;
        Texture texture = BBSModClient.getTextures().getTexture(textureLink);

        return AdoptedTexture.identifier(texture);
    }

    private void updateTexture(float transition)
    {
        if (this.emitter != null)
        {
            this.emitter.texture = this.form.texture.get();
        }
    }

    @Override
    public void tick(IEntity entity)
    {
        this.ensureEmitter(entity.getWorld(), 0F);

        if (this.emitter != null)
        {
            /* Rewind the emitter if it was paused and resumed in order to make
             * particle effects with once emitter */
            if (this.restart)
            {
                this.emitter.stop();
                this.emitter.start();

                this.restart = false;
            }

            this.emitter.update();
        }
    }
}
