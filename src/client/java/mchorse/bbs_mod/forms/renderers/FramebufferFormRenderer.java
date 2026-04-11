package mchorse.bbs_mod.forms.renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.forms.forms.FramebufferForm;
import mchorse.bbs_mod.graphics.Framebuffer;
import mchorse.bbs_mod.graphics.Renderbuffer;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.graphics.texture.TextureFormat;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.Quad;
import mchorse.bbs_mod.utils.colors.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;
import java.util.function.Supplier;

public class FramebufferFormRenderer extends FormRenderer<FramebufferForm>
{
    private static final Quad quad = new Quad();
    private static final Quad uvQuad = new Quad();
    private final Link framebufferKey;

    public FramebufferFormRenderer(FramebufferForm form)
    {
        super(form);

        this.framebufferKey = Link.bbs("framebuffer_form_" + Integer.toHexString(System.identityHashCode(form)));
    }

    @Override
    protected void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        MatrixStack stack = context.batcher.getContext().getMatrices();

        stack.push();

        Matrix4f uiMatrix = ModelFormRenderer.getUIMatrix(context, x1, y1, x2, y2);

        this.applyTransforms(uiMatrix, context.getTransition());
        stack.translate(0F, 1F, 0F);
        MatrixStackUtils.multiply(stack, uiMatrix);
        stack.scale(this.form.uiScale.get(), this.form.uiScale.get(), this.form.uiScale.get());

        this.renderModel(
            this.getFramebufferTexture(),
            VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
            GameRenderer::getRenderTypeEntityTranslucentProgram,
            stack,
            0,
            0xf000f0,
            0xffffffff,
            context.getTransition(),
            false
        );

        stack.pop();
    }

    private Texture getFramebufferTexture()
    {
        Framebuffer framebuffer = BBSModClient.getFramebuffers().getFramebuffer(this.framebufferKey, (f) ->
        {
            Texture texture = new Texture();

            texture.setFormat(TextureFormat.RGB_U8);
            texture.setSize(2, 2);
            texture.setFilter(GL11.GL_NEAREST);
            texture.setWrap(GL13.GL_CLAMP_TO_EDGE);

            Renderbuffer renderbuffer = new Renderbuffer();

            renderbuffer.resize(2, 2);

            f.deleteTextures().attach(texture, GL30.GL_COLOR_ATTACHMENT0);
            f.attach(renderbuffer);
            f.unbind();
        });

        return framebuffer.getMainTexture();
    }

    @Override
    public void renderBodyParts(FormRenderingContext context)
    {
        boolean copyWorld = this.form.parts.getAllTyped().isEmpty();
        boolean useScreenTexture = BBSRendering.isIrisShadersEnabled() && copyWorld;
        boolean updateCapture = !BBSRendering.isIrisShadowPass() && !useScreenTexture;

        Framebuffer framebuffer = BBSModClient.getFramebuffers().getFramebuffer(this.framebufferKey, (f) ->
        {
            Texture texture = new Texture();

            texture.setFormat(TextureFormat.RGB_U8);
            texture.setSize(2, 2);
            texture.setFilter(GL11.GL_NEAREST);
            texture.setWrap(GL13.GL_CLAMP_TO_EDGE);

            Renderbuffer renderbuffer = new Renderbuffer();

            renderbuffer.resize(2, 2);

            f.deleteTextures().attach(texture, GL30.GL_COLOR_ATTACHMENT0);
            f.attach(renderbuffer);
            f.unbind();
        });

        Texture mainTexture = framebuffer.getMainTexture();

        if (mainTexture.getFormat() != TextureFormat.RGB_U8)
        {
            int tw = Math.max(mainTexture.width, 2);
            int th = Math.max(mainTexture.height, 2);

            mainTexture.bind();
            mainTexture.setFormat(TextureFormat.RGB_U8);
            mainTexture.setSize(tw, th);
            mainTexture.unbind();
        }

        int[] viewport = new int[4];

        try (MemoryStack stack = MemoryStack.stackPush())
        {
            IntBuffer viewportBuffer = stack.mallocInt(4);

            GL30.glGetIntegerv(GL30.GL_VIEWPORT, viewportBuffer);

            for (int i = 0; i < 4; i++)
            {
                viewport[i] = viewportBuffer.get(i);
            }
        }

        float scale = MathUtils.clamp(this.form.scale.get(), 0.01F, 4F);
        int w = MathUtils.clamp(Math.round(this.form.width.get() * scale), 2, 4096);
        int h = MathUtils.clamp(Math.round(this.form.height.get() * scale), 2, 4096);
        int prevDraw = GL30.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int prevRead = GL30.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int prevCullFaceMode = GL30.glGetInteger(GL11.GL_CULL_FACE_MODE);
        boolean hadCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        Vector3f light0 = RenderSystem.shaderLightDirections[0];
        Vector3f light1 = RenderSystem.shaderLightDirections[1];
        Matrix4f projectionMatrix = new Matrix4f(RenderSystem.getProjectionMatrix());

        try
        {
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL30.glCullFace(GL30.GL_FRONT);
            RenderSystem.setShaderLights(new Vector3f(0F, 0F, 1F), new Vector3f(0F, 0F, 1F));
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(-1F, 1F, 1F, -1F, -500F, 500F), VertexSorter.BY_Z);
            RenderSystem.getModelViewStack().push();
            RenderSystem.getModelViewStack().peek().getPositionMatrix().identity();
            RenderSystem.getModelViewStack().peek().getNormalMatrix().identity();
            RenderSystem.applyModelViewMatrix();

            framebuffer.apply();

            if (w != mainTexture.width || h != mainTexture.height)
            {
                framebuffer.resize(w, h);
            }

            if (updateCapture && GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) == GL30.GL_FRAMEBUFFER_COMPLETE)
            {
                if (copyWorld)
                {
                    int sourceRead = prevRead != 0 ? prevRead : prevDraw;

                    if (sourceRead == 0)
                    {
                        MinecraftClient.getInstance().getFramebuffer().beginRead();
                        sourceRead = GL30.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
                    }

                    int glError;

                    while ((glError = GL11.glGetError()) != GL11.GL_NO_ERROR)
                    {}

                    GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, sourceRead);
                    GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, framebuffer.id);
                    GL30.glBlitFramebuffer(
                        viewport[0], viewport[1], viewport[0] + viewport[2], viewport[1] + viewport[3],
                        0, h, w, 0,
                        GL11.GL_COLOR_BUFFER_BIT,
                        GL11.GL_LINEAR
                    );

                    if ((glError = GL11.glGetError()) != GL11.GL_NO_ERROR)
                    {
                        Texture capture = framebuffer.getMainTexture();
                        int copyW = Math.max(1, Math.min(w, viewport[2]));
                        int copyH = Math.max(1, Math.min(h, viewport[3]));

                        capture.bind();
                        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, viewport[0], viewport[1], copyW, copyH);
                        capture.unbind();
                    }
                }
                else
                {
                    framebuffer.clear();
                    context.stack.push();
                    context.stack.peek().getPositionMatrix().identity();
                    context.stack.peek().getNormalMatrix().identity();

                    super.renderBodyParts(context);

                    context.stack.pop();
                }
            }
        }
        finally
        {
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, prevDraw);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, prevRead);
            GL30.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);

            RenderSystem.setShaderLights(light0, light1);
            RenderSystem.getModelViewStack().pop();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(projectionMatrix, VertexSorter.BY_Z);
            GL30.glCullFace(prevCullFaceMode);

            if (!hadCull)
            {
                GL11.glDisable(GL11.GL_CULL_FACE);
            }
        }

        boolean shading = !context.isPicking();
        VertexFormat format = shading ? VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL : VertexFormats.POSITION_TEXTURE_LIGHT_COLOR;
        Supplier<ShaderProgram> shader = shading ? GameRenderer::getRenderTypeEntityTranslucentProgram : GameRenderer::getPositionTexLightmapColorProgram;
        Texture finalTexture = framebuffer.getMainTexture();

        if (useScreenTexture)
        {
            Texture screenTexture = BBSRendering.getTexture();

            if (screenTexture.width > 1 && screenTexture.height > 1)
            {
                finalTexture = screenTexture;
            }
        }

        this.renderModel(finalTexture, format, shader, context.stack, context.overlay, context.light, context.color, context.getTransition(), useScreenTexture);
    }

    private void renderModel(Texture texture, VertexFormat format, Supplier<ShaderProgram> shader, MatrixStack matrices, int overlay, int light, int overlayColor, float transition, boolean flipY)
    {
        float w = texture.width;
        float h = texture.height;

        /* TL = top left, BR = bottom right*/
        Vector4f crop = new Vector4f(0, 0, 0, 0);
        float baseTLx = crop.x / w;
        float baseTLy = crop.y / h;
        float baseBRx = 1 - crop.z / w;
        float baseBRy = 1 - crop.w / h;
        float uvTLx = baseTLx;
        float uvTLy = flipY ? 1F - baseTLy : baseTLy;
        float uvBRx = baseBRx;
        float uvBRy = flipY ? 1F - baseBRy : baseBRy;

        uvQuad.p1.set(uvTLx, uvTLy, 0);
        uvQuad.p2.set(uvBRx, uvTLy, 0);
        uvQuad.p3.set(uvTLx, uvBRy, 0);
        uvQuad.p4.set(uvBRx, uvBRy, 0);

        /* Calculate quad's size (vertices, not UV) */
        float ratioX = w > h ? h / w : 1F;
        float ratioY = h > w ? w / h : 1F;
        float TLx = (baseTLx - 0.5F) * ratioY;
        float TLy = -(baseTLy - 0.5F) * ratioX;
        float BRx = (baseBRx - 0.5F) * ratioY;
        float BRy = -(baseBRy - 0.5F) * ratioX;

        quad.p1.set(TLx, TLy, 0);
        quad.p2.set(BRx, TLy, 0);
        quad.p3.set(TLx, BRy, 0);
        quad.p4.set(BRx, BRy, 0);

        this.renderQuad(format, texture, shader, matrices, overlay, light, overlayColor, transition);
    }

    private void renderQuad(VertexFormat format, Texture texture, Supplier<ShaderProgram> shader, MatrixStack matrices, int overlay, int light, int overlayColor, float transition)
    {
        BufferBuilder builder = Tessellator.getInstance().getBuffer();
        Color color = Color.white();
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        Matrix3f normal = matrices.peek().getNormalMatrix();

        color.mul(overlayColor);

        GameRenderer gameRenderer = MinecraftClient.getInstance().gameRenderer;

        gameRenderer.getLightmapTextureManager().enable();
        gameRenderer.getOverlayTexture().setupOverlayColor();

        BBSModClient.getTextures().bindTexture(texture);
        RenderSystem.setShader(shader);

        texture.bind();
        texture.setFilterMipmap(false, false);
        builder.begin(VertexFormat.DrawMode.TRIANGLES, format);

        /* Front */
        this.fill(format, builder, matrix, quad.p3.x, quad.p3.y, color, uvQuad.p3.x, uvQuad.p3.y, overlay, light, normal, 1F).next();
        this.fill(format, builder, matrix, quad.p2.x, quad.p2.y, color, uvQuad.p2.x, uvQuad.p2.y, overlay, light, normal, 1F).next();
        this.fill(format, builder, matrix, quad.p1.x, quad.p1.y, color, uvQuad.p1.x, uvQuad.p1.y, overlay, light, normal, 1F).next();

        this.fill(format, builder, matrix, quad.p3.x, quad.p3.y, color, uvQuad.p3.x, uvQuad.p3.y, overlay, light, normal, 1F).next();
        this.fill(format, builder, matrix, quad.p4.x, quad.p4.y, color, uvQuad.p4.x, uvQuad.p4.y, overlay, light, normal, 1F).next();
        this.fill(format, builder, matrix, quad.p2.x, quad.p2.y, color, uvQuad.p2.x, uvQuad.p2.y, overlay, light, normal, 1F).next();

        /* Back */
        this.fill(format, builder, matrix, quad.p1.x, quad.p1.y, color, uvQuad.p1.x, uvQuad.p1.y, overlay, light, normal, -1F).next();
        this.fill(format, builder, matrix, quad.p2.x, quad.p2.y, color, uvQuad.p2.x, uvQuad.p2.y, overlay, light, normal, -1F).next();
        this.fill(format, builder, matrix, quad.p3.x, quad.p3.y, color, uvQuad.p3.x, uvQuad.p3.y, overlay, light, normal, -1F).next();

        this.fill(format, builder, matrix, quad.p2.x, quad.p2.y, color, uvQuad.p2.x, uvQuad.p2.y, overlay, light, normal, -1F).next();
        this.fill(format, builder, matrix, quad.p4.x, quad.p4.y, color, uvQuad.p4.x, uvQuad.p4.y, overlay, light, normal, -1F).next();
        this.fill(format, builder, matrix, quad.p3.x, quad.p3.y, color, uvQuad.p3.x, uvQuad.p3.y, overlay, light, normal, -1F).next();

        RenderSystem.defaultBlendFunc();
        RenderSystem.enableBlend();
        BufferRenderer.drawWithGlobalProgram(builder.end());

        gameRenderer.getLightmapTextureManager().disable();
        gameRenderer.getOverlayTexture().teardownOverlayColor();
    }

    private VertexConsumer fill(VertexFormat format, VertexConsumer consumer, Matrix4f matrix, float x, float y, Color color, float u, float v, int overlay, int light, Matrix3f normal, float nz)
    {
        if (format == VertexFormats.POSITION_TEXTURE_LIGHT_COLOR)
        {
            return consumer.vertex(matrix, x, y, 0F).texture(u, v).light(light).color(color.r, color.g, color.b, color.a);
        }

        return consumer.vertex(matrix, x, y, 0F).color(color.r, color.g, color.b, color.a).texture(u, v).overlay(overlay).light(light).normal(normal, 0F, 0F, nz);
    }
}