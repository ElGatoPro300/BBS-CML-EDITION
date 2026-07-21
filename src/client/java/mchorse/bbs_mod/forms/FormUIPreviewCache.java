package mchorse.bbs_mod.forms;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.graphics.Framebuffer;
import mchorse.bbs_mod.graphics.Renderbuffer;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Colors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;

import org.joml.Matrix4f;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Caches form UI previews (color + depth) keyed by form id/revision and orbit angle.
 * List thumbnails always use this cache (budgeted fills). Selected forms can still
 * live-render via {@link FormUtilsClient#renderUI}.
 */
public final class FormUIPreviewCache
{
    private static final int MAX_ENTRIES = 512;
    private static final int ANGLE_BUCKETS = 48;
    private static final int MAX_FILLS_PER_FRAME = 16;
    private static final long FRAME_BUDGET_NS = 12_000_000L;
    /* Render thumbnails larger than the on-screen cell, then downscale with linear filter. */
    private static final int SUPERSAMPLE = 3;

    private static final Map<Long, CacheEntry> CACHE = new LinkedHashMap<>()
    {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, CacheEntry> eldest)
        {
            if (this.size() > MAX_ENTRIES)
            {
                eldest.getValue().delete();

                return true;
            }

            return false;
        }
    };

    private static Framebuffer scratchFramebuffer;
    private static Texture scratchTexture;
    private static Renderbuffer scratchDepth;
    private static int scratchWidth;
    private static int scratchHeight;
    private static long budgetFrameNs;
    private static int fillsThisFrame;

    private FormUIPreviewCache()
    {}

    public static void clear()
    {
        for (CacheEntry entry : CACHE.values())
        {
            entry.delete();
        }

        CACHE.clear();
    }

    public static void render(Form form, UIContext context, int x1, int y1, int x2, int y2)
    {
        if (form == null)
        {
            return;
        }

        int width = x2 - x1;
        int height = y2 - y1;

        if (width <= 0 || height <= 0)
        {
            return;
        }

        /* List thumbs stay on the budgeted cache. Callers that need live motion
         * (selected cell) use FormUtilsClient.renderUI directly. */
        int revision = form.getEditRevision();
        int angleBucket = getAngleBucket(context, x1, x2);
        long key = buildKey(form, width, height, angleBucket);
        CacheEntry entry = CACHE.get(key);

        if (entry != null && entry.revision == revision && entry.texture != null && entry.texture.isValid())
        {
            thisBlit(context, entry.texture, x1, y1, width, height);

            return;
        }

        if (!thisBeginFill())
        {
            CacheEntry fallback = thisFindFallback(form, width, height, revision);

            if (fallback != null)
            {
                thisBlit(context, fallback.texture, x1, y1, width, height);
            }
            else
            {
                /* Always show something: live static pose when the cache is busy. */
                FormUtilsClient.renderUI(form, context, x1, y1, x2, y2, false);
            }

            return;
        }

        if (entry == null)
        {
            entry = new CacheEntry();
            CACHE.put(key, entry);
        }

        thisRenderToEntry(form, context, entry, width, height, revision, angleBucket);
        thisBlit(context, entry.texture, x1, y1, width, height);
    }

    private static void thisBlit(UIContext context, Texture texture, int x1, int y1, int width, int height)
    {
        context.batcher.flush();
        /* glCopyTexSubImage2D keeps OpenGL's bottom-left origin; GUI quads are top-left. */
        context.batcher.texturedBox(
            texture,
            Colors.WHITE,
            x1,
            y1,
            width,
            height,
            0,
            texture.height,
            texture.width,
            0,
            texture.width,
            texture.height
        );
        context.batcher.flush();
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
    }

    private static boolean thisBeginFill()
    {
        long now = System.nanoTime();

        if (now - budgetFrameNs > FRAME_BUDGET_NS)
        {
            budgetFrameNs = now;
            fillsThisFrame = 0;
        }

        if (fillsThisFrame >= MAX_FILLS_PER_FRAME)
        {
            return false;
        }

        fillsThisFrame += 1;

        return true;
    }

    private static CacheEntry thisFindFallback(Form form, int width, int height, int revision)
    {
        int formKey = formCacheKey(form);

        for (CacheEntry entry : CACHE.values())
        {
            if (entry != null && entry.formKey == formKey && entry.width == width && entry.height == height
                && entry.revision == revision && entry.texture != null && entry.texture.isValid())
            {
                return entry;
            }
        }

        return null;
    }

    private static void thisRenderToEntry(Form form, UIContext context, CacheEntry entry, int width, int height, int revision, int angleBucket)
    {
        int renderW = Math.max(1, width * SUPERSAMPLE);
        int renderH = Math.max(1, height * SUPERSAMPLE);

        ensureScratchFramebuffer(renderW, renderH);

        MinecraftClient client = MinecraftClient.getInstance();
        int[] viewport = new int[4];
        boolean scissorWasEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        Matrix4f previousProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        MatrixStack matrices = context.batcher.getContext().getMatrices();

        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);

        context.batcher.flush();

        /* Morph list cells clip with screen-space scissors. Those scissors do not
         * overlap the scratch FBO viewport (0,0,w,h), so leaving them on caches black. */
        if (scissorWasEnabled)
        {
            GlStateManager._disableScissorTest();
        }

        /* Preview fill uses cell-local coords. Match GUI Y-down ortho to the supersampled
         * target so getUIMatrix scale fills the thumbnail instead of a screen speck. */
        RenderSystem.setProjectionMatrix(
            new Matrix4f().ortho(0F, renderW, renderH, 0F, -1000F, 3000F),
            VertexSorter.BY_Z
        );
        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().identity();
        RenderSystem.applyModelViewMatrix();
        matrices.push();
        matrices.peek().getPositionMatrix().identity();
        matrices.peek().getNormalMatrix().identity();

        scratchFramebuffer.bind();
        scratchFramebuffer.applyClear();
        FormRenderer.setSuppressFormDisplayName(true);

        try
        {
            FormUtilsClient.renderUI(form, context, 0, 0, renderW, renderH);
            context.batcher.flush();
        }
        finally
        {
            FormRenderer.setSuppressFormDisplayName(false);
        }

        entry.ensure(renderW, renderH);
        entry.texture.bind();
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, renderW, renderH);
        entry.texture.unbind();

        scratchFramebuffer.unbind();

        matrices.pop();
        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(previousProjection, VertexSorter.BY_Z);

        if (client != null && client.getFramebuffer() != null)
        {
            /* Do not clear — world may already be in the main FB (immersive morph UI). */
            client.getFramebuffer().beginWrite(false);
        }

        GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);

        if (scissorWasEnabled)
        {
            GlStateManager._enableScissorTest();
        }

        entry.revision = revision;
        entry.angleBucket = angleBucket;
        entry.width = width;
        entry.height = height;
        entry.formKey = formCacheKey(form);
    }

    private static void ensureScratchFramebuffer(int width, int height)
    {
        if (scratchFramebuffer != null && scratchWidth == width && scratchHeight == height)
        {
            return;
        }

        if (scratchFramebuffer == null)
        {
            scratchFramebuffer = BBSModClient.getFramebuffers().getFramebuffer(Link.bbs("form_ui_preview_scratch"), (framebuffer) ->
            {
                scratchTexture = new Texture();

                scratchTexture.setSize(width, height);
                scratchTexture.setFilter(GL11.GL_LINEAR);
                scratchTexture.setWrap(GL13.GL_CLAMP_TO_EDGE);

                scratchDepth = new Renderbuffer();

                scratchDepth.resize(width, height);

                framebuffer.deleteTextures().attach(scratchTexture, GL30.GL_COLOR_ATTACHMENT0);
                framebuffer.attach(scratchDepth);
                framebuffer.unbind();
            });
        }
        else
        {
            scratchFramebuffer.resize(width, height);
        }

        scratchWidth = width;
        scratchHeight = height;
    }

    private static int getAngleBucket(UIContext context, int x1, int x2)
    {
        float angle = MathUtils.toRad(context.mouseX - (x1 + x2) / 2) + MathUtils.PI;

        if (BBSSettings.freezeModels.get())
        {
            angle = -MathUtils.PI + MathUtils.PI / 8F;
        }

        int bucket = (int) (angle * ANGLE_BUCKETS / (MathUtils.PI * 2F));

        return Math.floorMod(bucket, ANGLE_BUCKETS);
    }

    private static int formCacheKey(Form form)
    {
        String formId = form.getFormId();

        return formId != null && !formId.isEmpty()
            ? formId.hashCode()
            : System.identityHashCode(form);
    }

    private static long buildKey(Form form, int width, int height, int angleBucket)
    {
        /* Prefer stable form id so recreating the morph palette still hits warm cache.
         * Fall back to identity for anonymous / duplicated instances without an id. */
        int formKey = formCacheKey(form);

        return ((long) formKey << 32)
            ^ ((long) width << 16)
            ^ (long) height
            ^ ((long) angleBucket << 48);
    }

    private static final class CacheEntry
    {
        public Texture texture;
        public int revision = -1;
        public int angleBucket = Integer.MIN_VALUE;
        public int width;
        public int height;
        public int formKey;

        public void ensure(int w, int h)
        {
            if (this.texture == null)
            {
                this.texture = new Texture();
                this.texture.setFilter(GL11.GL_LINEAR);
                this.texture.setWrap(GL13.GL_CLAMP_TO_EDGE);
            }

            if (this.texture.width != w || this.texture.height != h)
            {
                this.texture.setSize(w, h);
            }
        }

        public void delete()
        {
            if (this.texture != null)
            {
                this.texture.delete();
                this.texture = null;
            }
        }
    }
}
