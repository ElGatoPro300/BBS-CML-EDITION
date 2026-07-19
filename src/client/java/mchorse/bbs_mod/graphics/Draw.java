package mchorse.bbs_mod.graphics;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.camera.data.Angle;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.utils.Axis;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.MatrixStackUtils;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;

public class Draw
{
    public static void renderBox(MatrixStack stack, double x, double y, double z, double w, double h, double d)
    {
        renderBox(stack, x, y, z, w, h, d, 1, 1, 1);
    }

    public static void renderBox(MatrixStack stack, double x, double y, double z, double w, double h, double d, float r, float g, float b)
    {
        renderBox(stack, x, y, z, w, h, d, r, g, b, 1F);
    }

    public static void renderBox(MatrixStack stack, double x, double y, double z, double w, double h, double d, float r, float g, float b, float a)
    {
        /* Iris TAA turns lines/alpha into stipple during the world pass. Queue solid edges for LAST. */
        if (BBSRendering.isIrisShadersEnabled() && BBSRendering.isRenderingWorld())
        {
            enqueueIrisBox(stack, x, y, z, w, h, d, r, g, b);

            return;
        }

        stack.push();
        stack.translate(x, y, z);
        float fw = (float) w;
        float fh = (float) h;
        float fd = (float) d;
        float t = 1 / 96F + (float) (Math.sqrt(w * w + h + h + d + d) / 2000);

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        /* Pillars: fillBox(builder, -t, -t, -t, t, t, t, r, g, b, a); */
        fillBox(builder, stack, -t, -t, -t, t, t + fh, t, r, g, b, a);
        fillBox(builder, stack, -t + fw, -t, -t, t + fw, t + fh, t, r, g, b, a);
        fillBox(builder, stack, -t, -t, -t + fd, t, t + fh, t + fd, r, g, b, a);
        fillBox(builder, stack, -t + fw, -t, -t + fd, t + fw, t + fh, t + fd, r, g, b, a);

        /* Top */
        fillBox(builder, stack, -t, -t + fh, -t, t + fw, t + fh, t, r, g, b, a);
        fillBox(builder, stack, -t, -t + fh, -t + fd, t + fw, t + fh, t + fd, r, g, b, a);
        fillBox(builder, stack, -t, -t + fh, -t, t, t + fh, t + fd, r, g, b, a);
        fillBox(builder, stack, -t + fw, -t + fh, -t, t + fw, t + fh, t + fd, r, g, b, a);

        /* Bottom */
        fillBox(builder, stack, -t, -t, -t, t + fw, t, t, r, g, b, a);
        fillBox(builder, stack, -t, -t, -t + fd, t + fw, t, t + fd, r, g, b, a);
        fillBox(builder, stack, -t, -t, -t, t, t, t + fd, r, g, b, a);
        fillBox(builder, stack, -t + fw, -t, -t, t + fw, t, t + fd, r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(builder.end());

        stack.pop();
    }


    private static final java.util.List<IrisBox> irisBoxQueue = new java.util.ArrayList<>();

    private static final class IrisBox
    {
        private final Matrix4f matrix;
        private final float w;
        private final float h;
        private final float d;
        private final float r;
        private final float g;
        private final float b;

        private IrisBox(Matrix4f matrix, float w, float h, float d, float r, float g, float b)
        {
            this.matrix = matrix;
            this.w = w;
            this.h = h;
            this.d = d;
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }

    private static void enqueueIrisBox(MatrixStack stack, double x, double y, double z, double w, double h, double d, float r, float g, float b)
    {
        Matrix4f matrix = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(stack.peek().getPositionMatrix());

        matrix.translate((float) x, (float) y, (float) z);
        irisBoxQueue.add(new IrisBox(matrix, (float) w, (float) h, (float) d, r, g, b));
    }

    /** Flush hitboxes queued during the Iris world pass (call from WorldRenderEvents.LAST). */
    public static void flushIrisBoxes()
    {
        if (irisBoxQueue.isEmpty())
        {
            return;
        }

        boolean savedBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean savedDepth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        MatrixStack stack = new MatrixStack();

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        MatrixStackUtils.pushIdentityModelView();

        try
        {
            for (IrisBox box : irisBoxQueue)
            {
                stack.push();
                stack.peek().getPositionMatrix().set(box.matrix);
                renderBoxSolidEdges(stack, box.w, box.h, box.d, box.r, box.g, box.b);
                stack.pop();
            }
        }
        finally
        {
            MatrixStackUtils.popModelView();
            irisBoxQueue.clear();

            if (savedDepth)
            {
                RenderSystem.enableDepthTest();
            }

            if (savedBlend)
            {
                RenderSystem.enableBlend();
            }
        }
    }

    private static void renderBoxSolidEdges(MatrixStack stack, float fw, float fh, float fd, float r, float g, float b)
    {
        float t = 1 / 96F + (float) (Math.sqrt(fw * fw + fh + fh + fd + fd) / 2000);
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        fillBox(builder, stack, -t, -t, -t, t, t + fh, t, r, g, b, 1F);
        fillBox(builder, stack, -t + fw, -t, -t, t + fw, t + fh, t, r, g, b, 1F);
        fillBox(builder, stack, -t, -t, -t + fd, t, t + fh, t + fd, r, g, b, 1F);
        fillBox(builder, stack, -t + fw, -t, -t + fd, t + fw, t + fh, t + fd, r, g, b, 1F);

        fillBox(builder, stack, -t, -t + fh, -t, t + fw, t + fh, t, r, g, b, 1F);
        fillBox(builder, stack, -t, -t + fh, -t + fd, t + fw, t + fh, t + fd, r, g, b, 1F);
        fillBox(builder, stack, -t, -t + fh, -t, t, t + fh, t + fd, r, g, b, 1F);
        fillBox(builder, stack, -t + fw, -t + fh, -t, t + fw, t + fh, t + fd, r, g, b, 1F);

        fillBox(builder, stack, -t, -t, -t, t + fw, t, t, r, g, b, 1F);
        fillBox(builder, stack, -t, -t, -t + fd, t + fw, t, t + fd, r, g, b, 1F);
        fillBox(builder, stack, -t, -t, -t, t, t, t + fd, r, g, b, 1F);
        fillBox(builder, stack, -t + fw, -t, -t, t + fw, t, t + fd, r, g, b, 1F);

        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    private static void renderBoxWireframe(MatrixStack stack, double x, double y, double z, double w, double h, double d, float r, float g, float b, float a)
    {
        stack.push();
        stack.translate(x, y, z);

        Matrix4f matrix = stack.peek().getPositionMatrix();
        float x1 = 0F;
        float y1 = 0F;
        float z1 = 0F;
        float x2 = (float) w;
        float y2 = (float) h;
        float z2 = (float) d;
        boolean savedBlend = GL11.glIsEnabled(GL11.GL_BLEND);

        RenderSystem.disableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.lineWidth(2F);

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        wireLine(builder, matrix, x1, y1, z1, x2, y1, z1, r, g, b, a);
        wireLine(builder, matrix, x2, y1, z1, x2, y1, z2, r, g, b, a);
        wireLine(builder, matrix, x2, y1, z2, x1, y1, z2, r, g, b, a);
        wireLine(builder, matrix, x1, y1, z2, x1, y1, z1, r, g, b, a);

        wireLine(builder, matrix, x1, y2, z1, x2, y2, z1, r, g, b, a);
        wireLine(builder, matrix, x2, y2, z1, x2, y2, z2, r, g, b, a);
        wireLine(builder, matrix, x2, y2, z2, x1, y2, z2, r, g, b, a);
        wireLine(builder, matrix, x1, y2, z2, x1, y2, z1, r, g, b, a);

        wireLine(builder, matrix, x1, y1, z1, x1, y2, z1, r, g, b, a);
        wireLine(builder, matrix, x2, y1, z1, x2, y2, z1, r, g, b, a);
        wireLine(builder, matrix, x2, y1, z2, x2, y2, z2, r, g, b, a);
        wireLine(builder, matrix, x1, y1, z2, x1, y2, z2, r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(builder.end());

        RenderSystem.lineWidth(1F);

        if (savedBlend)
        {
            RenderSystem.enableBlend();
        }

        stack.pop();
    }

    private static void wireLine(BufferBuilder builder, Matrix4f matrix, float x0, float y0, float z0, float x1, float y1, float z1, float r, float g, float b, float a)
    {
        builder.vertex(matrix, x0, y0, z0).color(r, g, b, a);
        builder.vertex(matrix, x1, y1, z1).color(r, g, b, a);
    }
    /**
     * Fill a quad for {@link VertexFormats#POSITION_TEXTURE_COLOR_NORMAL}. Points should
     * be supplied in this order:
     *
     *     3 -------> 4
     *     ^
     *     |
     *     |
     *     2 <------- 1
     *
     * I.e. bottom left, bottom right, top left, top right, where left is -X and right is +X,
     * in case of a quad on fixed on Z axis.
     */
    public static void fillTexturedNormalQuad(BufferBuilder builder, MatrixStack stack, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float u1, float v1, float u2, float v2, float r, float g, float b, float a, float nx, float ny, float nz)
    {
        Matrix4f matrix4f = stack.peek().getPositionMatrix();

        /* 1 - BL, 2 - BR, 3 - TR, 4 - TL */
        builder.vertex(matrix4f, x2, y2, z2).texture(u1, v2).color(r, g, b, a).normal(nx, ny, nz);
        builder.vertex(matrix4f, x1, y1, z1).texture(u2, v2).color(r, g, b, a).normal(nx, ny, nz);
        builder.vertex(matrix4f, x4, y4, z4).texture(u2, v1).color(r, g, b, a).normal(nx, ny, nz);

        builder.vertex(matrix4f, x2, y2, z2).texture(u1, v2).color(r, g, b, a).normal(nx, ny, nz);
        builder.vertex(matrix4f, x4, y4, z4).texture(u2, v1).color(r, g, b, a).normal(nx, ny, nz);
        builder.vertex(matrix4f, x3, y3, z3).texture(u1, v1).color(r, g, b, a).normal(nx, ny, nz);
    }

    public static void fillQuad(BufferBuilder builder, MatrixStack stack, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float b, float a)
    {
        Matrix4f matrix4f = stack.peek().getPositionMatrix();

        /* 1 - BR, 2 - BL, 3 - TL, 4 - TR */
        builder.vertex(matrix4f, x1, y1, z1).color(r, g, b, a);
        builder.vertex(matrix4f, x2, y2, z2).color(r, g, b, a);
        builder.vertex(matrix4f, x3, y3, z3).color(r, g, b, a);
        builder.vertex(matrix4f, x1, y1, z1).color(r, g, b, a);
        builder.vertex(matrix4f, x3, y3, z3).color(r, g, b, a);
        builder.vertex(matrix4f, x4, y4, z4).color(r, g, b, a);
    }

    public static void fillBoxTo(BufferBuilder builder, MatrixStack stack, float x1, float y1, float z1, float x2, float y2, float z2, float thickness, float r, float g, float b, float a)
    {
        if (stack == null)
        {
            stack = new MatrixStack();
            MatrixStackUtils.multiply(stack, RenderSystem.getModelViewMatrix());
        }

        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        Angle angle = Angle.angle(dx, dy, dz);

        stack.push();

        stack.translate(x1, y1, z1);
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle.yaw));
        stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(angle.pitch));

        fillBox(builder, stack, -thickness / 2, -thickness / 2, 0, thickness / 2, thickness / 2, (float) distance, r, g, b, a);

        stack.pop();
    }

    public static void fillBox(BufferBuilder builder, MatrixStack stack, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b)
    {
        fillBox(builder, stack, x1, y1, z1, x2, y2, z2, r, g, b, 1F);
    }

    public static void fillBox(BufferBuilder builder, MatrixStack stack, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a)
    {
        /* X */
        fillQuad(builder, stack, x1, y1, z2, x1, y2, z2, x1, y2, z1, x1, y1, z1, r, g, b, a);
        fillQuad(builder, stack, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, r, g, b, a);

        /* Y */
        fillQuad(builder, stack, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, r, g, b, a);
        fillQuad(builder, stack, x2, y2, z1, x1, y2, z1, x1, y2, z2, x2, y2, z2, r, g, b, a);

        /* Z */
        fillQuad(builder, stack, x2, y1, z1, x1, y1, z1, x1, y2, z1, x2, y2, z1, r, g, b, a);
        fillQuad(builder, stack, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, r, g, b, a);
    }

    public static void coolerAxes(MatrixStack stack, float axisSize, float axisOffset, float outlineSize, float outlineOffset)
    {
        float scale = BBSSettings.axesScale.get();

        axisSize *= scale;
        axisOffset *= scale;
        outlineSize *= scale;
        outlineOffset *= scale;

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        fillBox(builder, stack, 0, -outlineOffset, -outlineOffset, outlineSize, outlineOffset, outlineOffset, 0, 0, 0);
        fillBox(builder, stack, -outlineOffset, 0, -outlineOffset, outlineOffset, outlineSize, outlineOffset, 0, 0, 0);
        fillBox(builder, stack, -outlineOffset, -outlineOffset, 0, outlineOffset, outlineOffset, outlineSize, 0, 0, 0);
        fillBox(builder, stack, -outlineOffset, -outlineOffset, -outlineOffset, outlineOffset, outlineOffset, outlineOffset, 0, 0, 0);

        fillBox(builder, stack, 0, -axisOffset, -axisOffset, axisSize, axisOffset, axisOffset, 1, 0, 0);
        fillBox(builder, stack, -axisOffset, 0, -axisOffset, axisOffset, axisSize, axisOffset, 0, 1, 0);
        fillBox(builder, stack, -axisOffset, -axisOffset, 0, axisOffset, axisOffset, axisSize, 0, 0, 1);
        fillBox(builder, stack, -axisOffset, -axisOffset, -axisOffset, axisOffset, axisOffset, axisOffset, 1, 1, 1);

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.disableDepthTest();

        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    public static void arc3D(BufferBuilder builder, MatrixStack stack, Axis axis, float radius, float thickness, float r, float g, float b)
    {
        arc3D(builder, stack, axis, radius, thickness, r, g, b, 0F, 360F);
    }

    /**
     * Draws a solid cone (with a capped base) between two points, used for the tapered
     * arrow tips on gizmo translate handles. The base circle is perpendicular to the
     * apex-to-base direction, so it works for any axis without extra stack rotation.
     */
    public static void cone(BufferBuilder builder, MatrixStack stack, float apexX, float apexY, float apexZ, float baseX, float baseY, float baseZ, float radius, int segments, float r, float g, float b, float a)
    {
        Matrix4f mat = stack.peek().getPositionMatrix();

        float dx = baseX - apexX;
        float dy = baseY - apexY;
        float dz = baseZ - apexZ;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (len < 1.0E-6F)
        {
            return;
        }

        dx /= len;
        dy /= len;
        dz /= len;

        float upx = 0F;
        float upy = 1F;
        float upz = 0F;

        if (Math.abs(dy) > 0.99F)
        {
            upx = 1F;
            upy = 0F;
            upz = 0F;
        }

        float rx = dy * upz - dz * upy;
        float ry = dz * upx - dx * upz;
        float rz = dx * upy - dy * upx;
        float rl = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);

        rx /= rl;
        ry /= rl;
        rz /= rl;

        float ux = ry * dz - rz * dy;
        float uy = rz * dx - rx * dz;
        float uz = rx * dy - ry * dx;

        for (int i = 0; i < segments; i++)
        {
            double a1 = Math.PI * 2D * i / segments;
            double a2 = Math.PI * 2D * (i + 1) / segments;

            float x1 = baseX + (float) (rx * Math.cos(a1) + ux * Math.sin(a1)) * radius;
            float y1 = baseY + (float) (ry * Math.cos(a1) + uy * Math.sin(a1)) * radius;
            float z1 = baseZ + (float) (rz * Math.cos(a1) + uz * Math.sin(a1)) * radius;

            float x2 = baseX + (float) (rx * Math.cos(a2) + ux * Math.sin(a2)) * radius;
            float y2 = baseY + (float) (ry * Math.cos(a2) + uy * Math.sin(a2)) * radius;
            float z2 = baseZ + (float) (rz * Math.cos(a2) + uz * Math.sin(a2)) * radius;

            builder.vertex(mat, apexX, apexY, apexZ).color(r, g, b, a);
            builder.vertex(mat, x1, y1, z1).color(r, g, b, a);
            builder.vertex(mat, x2, y2, z2).color(r, g, b, a);

            builder.vertex(mat, x1, y1, z1).color(r, g, b, a);
            builder.vertex(mat, baseX, baseY, baseZ).color(r, g, b, a);
            builder.vertex(mat, x2, y2, z2).color(r, g, b, a);
        }
    }

    /**
     * Draws a standard UV sphere centered at the local origin, used for the invisible
     * free-rotate trackball hit volume (and its stencil id encoding).
     */
    public static void sphere(BufferBuilder builder, MatrixStack stack, float radius, int rings, int sectors, float r, float g, float b, float a)
    {
        Matrix4f mat = stack.peek().getPositionMatrix();

        for (int i = 0; i < rings; i++)
        {
            double v1 = Math.PI * i / rings;
            double v2 = Math.PI * (i + 1) / rings;

            for (int j = 0; j < sectors; j++)
            {
                double u1 = Math.PI * 2D * j / sectors;
                double u2 = Math.PI * 2D * (j + 1) / sectors;

                float x11 = (float) (Math.sin(v1) * Math.cos(u1) * radius);
                float y11 = (float) (Math.cos(v1) * radius);
                float z11 = (float) (Math.sin(v1) * Math.sin(u1) * radius);

                float x12 = (float) (Math.sin(v2) * Math.cos(u1) * radius);
                float y12 = (float) (Math.cos(v2) * radius);
                float z12 = (float) (Math.sin(v2) * Math.sin(u1) * radius);

                float x21 = (float) (Math.sin(v1) * Math.cos(u2) * radius);
                float y21 = y11;
                float z21 = (float) (Math.sin(v1) * Math.sin(u2) * radius);

                float x22 = (float) (Math.sin(v2) * Math.cos(u2) * radius);
                float y22 = y12;
                float z22 = (float) (Math.sin(v2) * Math.sin(u2) * radius);

                builder.vertex(mat, x11, y11, z11).color(r, g, b, a);
                builder.vertex(mat, x12, y12, z12).color(r, g, b, a);
                builder.vertex(mat, x22, y22, z22).color(r, g, b, a);

                builder.vertex(mat, x11, y11, z11).color(r, g, b, a);
                builder.vertex(mat, x22, y22, z22).color(r, g, b, a);
                builder.vertex(mat, x21, y21, z21).color(r, g, b, a);
            }
        }
    }

    /**
     * Based on ElGatoPro300's code from BBS mod CML edition
     */
    public static void arc3D(BufferBuilder builder, MatrixStack stack, Axis axis, float radius, float thickness, float r, float g, float b, float startDeg, float sweepDeg)
    {
        int segU = 96;
        int segV = 24;
        double u0 = Math.toRadians(startDeg);
        double uStep = Math.toRadians(sweepDeg / (double) segU);
        double vStep = Math.PI * 2D / (double) segV;

        stack.push();

        if (axis == Axis.X) stack.multiply(RotationAxis.POSITIVE_Z.rotation(MathUtils.PI / 2F));
        if (axis == Axis.Z) stack.multiply(RotationAxis.POSITIVE_X.rotation(MathUtils.PI / 2F));

        float tubeR = thickness * 0.5F;
        Matrix4f mat = stack.peek().getPositionMatrix();

        for (int iu = 0; iu < segU; iu++)
        {
            double u1 = u0 + uStep * iu;
            double u2 = u0 + uStep * (iu + 1);

            for (int iv = 0; iv < segV; iv++)
            {
                double v1 = vStep * iv;
                double v2 = vStep * (iv + 1);
                double cos1 = radius + tubeR * Math.cos(v1);
                double cos2 = radius + tubeR * Math.cos(v2);

                float x11 = (float) (cos1 * Math.cos(u1));
                float z11 = (float) (cos1 * Math.sin(u1));
                float y11 = (float) (tubeR * Math.sin(v1));

                float x12 = (float) (cos2 * Math.cos(u1));
                float z12 = (float) (cos2 * Math.sin(u1));
                float y12 = (float) (tubeR * Math.sin(v2));

                float x21 = (float) (cos1 * Math.cos(u2));
                float z21 = (float) (cos1 * Math.sin(u2));
                float y21 = (float) (tubeR * Math.sin(v1));

                float x22 = (float) (cos2 * Math.cos(u2));
                float z22 = (float) (cos2 * Math.sin(u2));
                float y22 = (float) (tubeR * Math.sin(v2));

                builder.vertex(mat, x11, y11, z11).color(r, g, b, 1F);
                builder.vertex(mat, x12, y12, z12).color(r, g, b, 1F);
                builder.vertex(mat, x22, y22, z22).color(r, g, b, 1F);

                builder.vertex(mat, x11, y11, z11).color(r, g, b, 1F);
                builder.vertex(mat, x22, y22, z22).color(r, g, b, 1F);
                builder.vertex(mat, x21, y21, z21).color(r, g, b, 1F);
            }
        }

        stack.pop();
    }
}