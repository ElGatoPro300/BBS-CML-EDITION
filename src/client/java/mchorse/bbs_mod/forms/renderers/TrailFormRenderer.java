package mchorse.bbs_mod.forms.renderers;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.forms.ITickable;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.TrailForm;
import mchorse.bbs_mod.forms.forms.utils.GlowSettings;
import mchorse.bbs_mod.forms.forms.utils.PaintSettings;
import mchorse.bbs_mod.forms.renderers.utils.FlatGlowOverlayPass;
import mchorse.bbs_mod.forms.renderers.utils.FlatPaintOverlayPass;
import mchorse.bbs_mod.forms.renderers.utils.FormColorBlend;
import mchorse.bbs_mod.forms.renderers.utils.FormTextureBlendRenderer;
import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.colors.Color;

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
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TrailFormRenderer extends FormRenderer<TrailForm> implements ITickable
{
    private final Map<FormRenderType, ArrayDeque<Trail>> record = new HashMap<>();
    private int tick;

    public TrailFormRenderer(TrailForm form)
    {
        super(form);
    }

    @Override
    protected void renderInUI(UIContext context, int x1, int y1, int x2, int y2)
    {
        Texture texture = context.render.getTextures().getTexture(this.form.texture.get());
        float min = Math.min(texture.width, texture.height);
        int ow = (x2 - x1) - 4;
        int oh = (y2 - y1) - 4;
        int w = (int) ((texture.width / min) * ow);
        int h = (int) ((texture.height / min) * ow);
        int x = x1 + (ow - w) / 2 + 2;
        int y = y1 + (oh - h) / 2 + 2;

        context.batcher.fullTexturedBox(texture, x, y, w, h);
    }

    @Override
    protected void render3D(FormRenderingContext context)
    {
        super.render3D(context);

        if (BBSRendering.isIrisShadowPass() || context.type == FormRenderType.ITEM_INVENTORY)
        {
            return;
        }

        if (context.modelRenderer || context.ui)
        {
            MatrixStack stack = context.stack;
            float scale = BBSSettings.axesScale.get();
            float axisOffset = 0.01F * scale;
            float outlineSize = 1.01F;
            float outlineOffset = 0.02F * scale;

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder builder = tessellator.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

            Draw.fillBox(builder, stack, -outlineOffset, -outlineSize, -outlineOffset, outlineOffset, outlineSize, outlineOffset, 0, 0, 0);
            Draw.fillBox(builder, stack, -axisOffset, -1F, -axisOffset, axisOffset, 1F, axisOffset, 0, 1, 0);

            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            RenderSystem.disableDepthTest();
            BufferRenderer.drawWithGlobalProgram(builder.end());
            RenderSystem.enableDepthTest();

            return;
        }

        if (!BBSRendering.isRenderingWorld())
        {
            return;
        }

        MatrixStack stack = context.stack;
        Camera camera = context.camera;
        double baseX = camera.position.x;
        double baseY = camera.position.y;
        double baseZ = camera.position.z;
        float current = (float) this.tick + context.transition;
        ArrayDeque<Trail> trails = this.record.computeIfAbsent(context.type, (k) -> new ArrayDeque<>());

        if (!this.form.paused.get())
        {
            Matrix4f modelPosMatrix = new Matrix4f(stack.peek().getPositionMatrix());
            Vector4f topVec = new Vector4f(0F, 1F, 0F, 1F);
            Vector4f bottomVec = new Vector4f(0F, -1F, 0F, 1F);

            modelPosMatrix.transform(topVec);
            modelPosMatrix.transform(bottomVec);

            Trail record = new Trail();
            record.tick = current;
            record.top = new Vector3d(topVec.x + baseX, topVec.y + baseY, topVec.z + baseZ);
            record.bottom = new Vector3d(bottomVec.x + baseX, bottomVec.y + baseY, bottomVec.z + baseZ);
            record.stop = new Vector3f((float) (topVec.x - bottomVec.x), (float) (topVec.y - bottomVec.y), (float) (topVec.z - bottomVec.z)).lengthSquared() < 1.0E-4D;

            trails.addLast(record);
        }

        boolean loop = this.form.loop.get();
        float length = this.form.length.get();
        float end = current - length;
        Iterator<Trail> it = trails.iterator();
        boolean hasSomethingToRender = false;
        boolean lastStop = true;

        while (it.hasNext())
        {
            Trail trail = it.next();

            if (trail.tick < end)
            {
                it.remove();
            }
            else
            {
                hasSomethingToRender |= !trail.stop && !lastStop;
                lastStop = trail.stop;
            }
        }

        if (!hasSomethingToRender || trails.size() <= 1 || !(length > 0.001D))
        {
            return;
        }

        Link defaultTexture = this.form.texture.get();
        Color tint = new Color().set(context.color, true);

        FormTextureBlendRenderer.draw(this.form.textureBlend, defaultTexture, (link, alphaFactor) ->
        {
            this.renderTrailPass(stack, trails, loop, length, current, baseX, baseY, baseZ, link, tint, alphaFactor);
        });
    }

    private void renderTrailPass(MatrixStack stack, ArrayDeque<Trail> trails, boolean loop, float length, float current, double baseX, double baseY, double baseZ, Link textureLink, Color tint, float alphaFactor)
    {
        if (textureLink == null)
        {
            return;
        }

        BBSModClient.getTextures().bindTexture(textureLink);
        stack.push();

        PaintSettings paintSettings = this.form.paintSettings.get();
        Color legacyPaint = this.form.paintColor.get();
        float paintStrength = paintSettings.resolveIntensity(legacyPaint);
        boolean positivePaint = FormColorBlend.hasPositivePaint(paintSettings, legacyPaint);
        Color resolvedPaint = positivePaint ? FormColorBlend.resolvePaintColor(paintSettings, legacyPaint) : null;

        GlowSettings glowSettings = this.form.glowSettings.get();
        Color legacyGlow = this.form.glowingColor.get();
        float glowIntensity = glowSettings.resolveIntensity(legacyGlow);

        Color color = tint.copy();

        color.a *= alphaFactor;

        if (paintStrength < 0F)
        {
            FormColorBlend.applyPaintBlend(color, paintSettings, legacyPaint);
        }

        if (glowIntensity < 0F)
        {
            FormColorBlend.blendFormGlowBrighten(color, glowSettings, legacyGlow);
        }

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        Matrix4f identityMatrix = new Matrix4f();

        this.buildTrailQuads(builder, identityMatrix, trails, loop, length, current, baseX, baseY, baseZ, color);

        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        BufferRenderer.drawWithGlobalProgram(builder.end());

        if (positivePaint)
        {
            if (BBSRendering.isIrisWorldPaintDeferral())
            {
                this.submitDeferredTrailPaintOverlay(trails, loop, length, current, baseX, baseY, baseZ, textureLink, resolvedPaint, color.a);
            }
            else
            {
                this.renderPaintOverlay(trails, loop, length, current, baseX, baseY, baseZ, resolvedPaint, color.a);
            }
        }

        if (glowIntensity > 0F)
        {
            this.renderGlowOverlay(tessellator, identityMatrix, trails, loop, length, current, baseX, baseY, baseZ, glowSettings, legacyGlow, color.a, glowIntensity);
        }

        RenderSystem.enableDepthTest();
        stack.pop();
    }

    private void submitDeferredTrailPaintOverlay(ArrayDeque<Trail> trails, boolean loop, float length, float current, double baseX, double baseY, double baseZ, Link textureLink, Color resolvedPaint, float alpha)
    {
        ArrayDeque<Trail> trailSnapshot = this.copyTrails(trails);
        Color paintOverlay = new Color(resolvedPaint.r, resolvedPaint.g, resolvedPaint.b, resolvedPaint.a);
        Matrix4f paintMatrix = new Matrix4f(RenderSystem.getModelViewMatrix());

        paintOverlay.a *= alpha;

        ModelVAORenderer.submitPaintOverlay(false, () ->
        {
            BBSModClient.getTextures().bindTexture(textureLink);
            this.renderPaintOverlayPass(trailSnapshot, loop, length, current, baseX, baseY, baseZ, paintOverlay, paintMatrix);
        });
    }

    private ArrayDeque<Trail> copyTrails(ArrayDeque<Trail> trails)
    {
        ArrayDeque<Trail> copy = new ArrayDeque<>();

        for (Trail trail : trails)
        {
            Trail snapshot = new Trail();

            snapshot.tick = trail.tick;
            snapshot.stop = trail.stop;
            snapshot.top = new Vector3d(trail.top);
            snapshot.bottom = new Vector3d(trail.bottom);
            copy.addLast(snapshot);
        }

        return copy;
    }

    private void renderPaintOverlay(ArrayDeque<Trail> trails, boolean loop, float length, float current, double baseX, double baseY, double baseZ, Color resolvedPaint, float alpha)
    {
        Color paintOverlay = new Color(resolvedPaint.r, resolvedPaint.g, resolvedPaint.b, resolvedPaint.a);

        paintOverlay.a *= alpha;
        this.renderPaintOverlayPass(trails, loop, length, current, baseX, baseY, baseZ, paintOverlay, new Matrix4f());
    }

    private void renderPaintOverlayPass(ArrayDeque<Trail> trails, boolean loop, float length, float current, double baseX, double baseY, double baseZ, Color paintOverlay, Matrix4f vertexMatrix)
    {
        Tessellator tessellator = Tessellator.getInstance();

        FlatPaintOverlayPass.render(() ->
        {
            BufferBuilder paintBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
            int paintLight = LightmapTextureManager.MAX_LIGHT_COORDINATE;
            int overlay = OverlayTexture.DEFAULT_UV;

            this.buildTrailPaintQuads(paintBuilder, vertexMatrix, trails, loop, length, current, baseX, baseY, baseZ, paintOverlay, overlay, paintLight);
            BufferRenderer.drawWithGlobalProgram(paintBuilder.end());
        });
    }

    private void renderGlowOverlay(Tessellator tessellator, Matrix4f matrix, ArrayDeque<Trail> trails, boolean loop, float length, float current, double baseX, double baseY, double baseZ, GlowSettings glowSettings, Color legacyGlow, float alpha, float glowIntensity)
    {
        FlatGlowOverlayPass.render(glowSettings, legacyGlow, alpha, glowIntensity, (glowColor) ->
        {
            BufferBuilder glowBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
            this.buildTrailQuads(glowBuilder, matrix, trails, loop, length, current, baseX, baseY, baseZ, glowColor);
            BufferRenderer.drawWithGlobalProgram(glowBuilder.end());
        });
    }

    private void buildTrailQuads(BufferBuilder builder, Matrix4f matrix, ArrayDeque<Trail> trails, boolean loop, float length, float current, double baseX, double baseY, double baseZ, Color color)
    {
        Trail lastTrail = null;

        for (Trail trail : trails)
        {
            if (lastTrail != null && !lastTrail.stop && !trail.stop)
            {
                float u1 = loop ? trail.tick / length : (current - trail.tick) / length;
                float u2 = loop ? lastTrail.tick / length : (current - lastTrail.tick) / length;

                this.addTrailSegment(builder, matrix, trail, lastTrail, baseX, baseY, baseZ, u1, u2, color);
            }

            lastTrail = trail;
        }
    }

    private void buildTrailPaintQuads(BufferBuilder builder, Matrix4f matrix, ArrayDeque<Trail> trails, boolean loop, float length, float current, double baseX, double baseY, double baseZ, Color color, int overlay, int light)
    {
        Trail lastTrail = null;

        for (Trail trail : trails)
        {
            if (lastTrail != null && !lastTrail.stop && !trail.stop)
            {
                float u1 = loop ? trail.tick / length : (current - trail.tick) / length;
                float u2 = loop ? lastTrail.tick / length : (current - lastTrail.tick) / length;

                this.addTrailPaintSegment(builder, matrix, trail, lastTrail, baseX, baseY, baseZ, u1, u2, color, overlay, light);
            }

            lastTrail = trail;
        }
    }

    private void addTrailSegment(BufferBuilder builder, Matrix4f matrix, Trail trail, Trail lastTrail, double baseX, double baseY, double baseZ, float u1, float u2, Color color)
    {
        float x1 = (float) (trail.top.x - baseX);
        float x2 = (float) (trail.bottom.x - baseX);
        float x3 = (float) (lastTrail.bottom.x - baseX);
        float x4 = (float) (lastTrail.top.x - baseX);

        float y1 = (float) (trail.top.y - baseY);
        float y2 = (float) (trail.bottom.y - baseY);
        float y3 = (float) (lastTrail.bottom.y - baseY);
        float y4 = (float) (lastTrail.top.y - baseY);

        float z1 = (float) (trail.top.z - baseZ);
        float z2 = (float) (trail.bottom.z - baseZ);
        float z3 = (float) (lastTrail.bottom.z - baseZ);
        float z4 = (float) (lastTrail.top.z - baseZ);

        builder.vertex(matrix, x1, y1, z1).texture(u1, 0F).color(color.r, color.g, color.b, color.a);
        builder.vertex(matrix, x2, y2, z2).texture(u1, 1F).color(color.r, color.g, color.b, color.a);
        builder.vertex(matrix, x3, y3, z3).texture(u2, 1F).color(color.r, color.g, color.b, color.a);
        builder.vertex(matrix, x4, y4, z4).texture(u2, 0F).color(color.r, color.g, color.b, color.a);

        builder.vertex(matrix, x4, y4, z4).texture(u2, 0F).color(color.r, color.g, color.b, color.a);
        builder.vertex(matrix, x3, y3, z3).texture(u2, 1F).color(color.r, color.g, color.b, color.a);
        builder.vertex(matrix, x2, y2, z2).texture(u1, 1F).color(color.r, color.g, color.b, color.a);
        builder.vertex(matrix, x1, y1, z1).texture(u1, 0F).color(color.r, color.g, color.b, color.a);
    }

    private void addTrailPaintSegment(BufferBuilder builder, Matrix4f matrix, Trail trail, Trail lastTrail, double baseX, double baseY, double baseZ, float u1, float u2, Color color, int overlay, int light)
    {
        float x1 = (float) (trail.top.x - baseX);
        float x2 = (float) (trail.bottom.x - baseX);
        float x3 = (float) (lastTrail.bottom.x - baseX);
        float x4 = (float) (lastTrail.top.x - baseX);

        float y1 = (float) (trail.top.y - baseY);
        float y2 = (float) (trail.bottom.y - baseY);
        float y3 = (float) (lastTrail.bottom.y - baseY);
        float y4 = (float) (lastTrail.top.y - baseY);

        float z1 = (float) (trail.top.z - baseZ);
        float z2 = (float) (trail.bottom.z - baseZ);
        float z3 = (float) (lastTrail.bottom.z - baseZ);
        float z4 = (float) (lastTrail.top.z - baseZ);

        this.fillPaintVertex(builder, matrix, x1, y1, z1, u1, 0F, color, overlay, light);
        this.fillPaintVertex(builder, matrix, x2, y2, z2, u1, 1F, color, overlay, light);
        this.fillPaintVertex(builder, matrix, x3, y3, z3, u2, 1F, color, overlay, light);
        this.fillPaintVertex(builder, matrix, x4, y4, z4, u2, 0F, color, overlay, light);

        this.fillPaintVertex(builder, matrix, x4, y4, z4, u2, 0F, color, overlay, light);
        this.fillPaintVertex(builder, matrix, x3, y3, z3, u2, 1F, color, overlay, light);
        this.fillPaintVertex(builder, matrix, x2, y2, z2, u1, 1F, color, overlay, light);
        this.fillPaintVertex(builder, matrix, x1, y1, z1, u1, 0F, color, overlay, light);
    }

    private void fillPaintVertex(BufferBuilder builder, Matrix4f matrix, float x, float y, float z, float u, float v, Color color, int overlay, int light)
    {
        builder.vertex(matrix, x, y, z).color(color.r, color.g, color.b, color.a).texture(u, v).overlay(overlay).light(light).normal(0F, 0F, 1F);
    }

    @Override
    public void tick(IEntity entity)
    {
        this.tick += 1;
    }

    public static class Trail
    {
        public Vector3d top;
        public Vector3d bottom;
        public float tick;
        public boolean stop;
    }
}
