package mchorse.bbs_mod.graphics;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.opengl.GlStateManager;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.camera.data.Angle;
import mchorse.bbs_mod.utils.Axis;
import mchorse.bbs_mod.utils.MathUtils;
import org.joml.Matrix4f;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import net.minecraft.client.renderer.rendertype.RenderTypes;

public class Draw
{
    public static void renderBox(PoseStack stack, double x, double y, double z, double w, double h, double d)
    {
        renderBox(stack, x, y, z, w, h, d, 1, 1, 1);
    }

    public static void renderBox(PoseStack stack, double x, double y, double z, double w, double h, double d, float r, float g, float b)
    {
        renderBox(stack, x, y, z, w, h, d, r, g, b, 1F);
    }

    public static void renderBox(PoseStack stack, double x, double y, double z, double w, double h, double d, float r, float g, float b, float a)
    {
        stack.pushPose();
        stack.translate(x, y, z);
        float fw = (float) w;
        float fh = (float) h;
        float fd = (float) d;
        float t = 1 / 96F + (float) (Math.sqrt(w * w + h + h + d + d) / 2000);

        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        // RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

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

        RenderTypes.debugFilledBox().draw(builder.buildOrThrow());

        stack.popPose();
    }

    /**
     * Fill a quad for {@link com.mojang.blaze3d.vertex.DefaultVertexFormat#POSITION_TEX_COLOR_NORMAL}. Points should
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
    public static void fillTexturedNormalQuad(BufferBuilder builder, PoseStack stack, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float u1, float v1, float u2, float v2, float r, float g, float b, float a, float nx, float ny, float nz)
    {
        Matrix4f matrix4f = new Matrix4f();

        /* 1 - BL, 2 - BR, 3 - TR, 4 - TL */
        builder.addVertex(matrix4f, x2, y2, z2).setUv(u1, v2).setColor(r, g, b, a).setNormal(nx, ny, nz);
        builder.addVertex(matrix4f, x1, y1, z1).setUv(u2, v2).setColor(r, g, b, a).setNormal(nx, ny, nz);
        builder.addVertex(matrix4f, x4, y4, z4).setUv(u2, v1).setColor(r, g, b, a).setNormal(nx, ny, nz);

        builder.addVertex(matrix4f, x2, y2, z2).setUv(u1, v2).setColor(r, g, b, a).setNormal(nx, ny, nz);
        builder.addVertex(matrix4f, x4, y4, z4).setUv(u2, v1).setColor(r, g, b, a).setNormal(nx, ny, nz);
        builder.addVertex(matrix4f, x3, y3, z3).setUv(u1, v1).setColor(r, g, b, a).setNormal(nx, ny, nz);
    }

    public static void fillQuad(BufferBuilder builder, PoseStack stack, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float b, float a)
    {
        Matrix4f matrix4f = new Matrix4f();

        /* 1 - BR, 2 - BL, 3 - TL, 4 - TR */
        builder.addVertex(matrix4f, x1, y1, z1).setColor(r, g, b, a);
        builder.addVertex(matrix4f, x2, y2, z2).setColor(r, g, b, a);
        builder.addVertex(matrix4f, x3, y3, z3).setColor(r, g, b, a);
        builder.addVertex(matrix4f, x1, y1, z1).setColor(r, g, b, a);
        builder.addVertex(matrix4f, x3, y3, z3).setColor(r, g, b, a);
        builder.addVertex(matrix4f, x4, y4, z4).setColor(r, g, b, a);
    }

    public static void fillBoxTo(BufferBuilder builder, PoseStack stack, float x1, float y1, float z1, float x2, float y2, float z2, float thickness, float r, float g, float b, float a)
    {
        if (stack == null)
        {
            stack = new PoseStack();
            MatrixStackUtils.multiply(stack, RenderSystem.getModelViewMatrix());
        }

        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        Angle angle = Angle.angle(dx, dy, dz);

        stack.pushPose();

        stack.translate(x1, y1, z1);
        stack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(angle.yaw));
        stack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(angle.pitch));

        fillBox(builder, stack, -thickness / 2, -thickness / 2, 0, thickness / 2, thickness / 2, (float) distance, r, g, b, a);

        stack.popPose();
    }

    public static void fillBox(BufferBuilder builder, PoseStack stack, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b)
    {
        fillBox(builder, stack, x1, y1, z1, x2, y2, z2, r, g, b, 1F);
    }

    public static void fillBox(BufferBuilder builder, PoseStack stack, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a)
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

    public static void coolerAxes(PoseStack stack, float axisSize, float axisOffset, float outlineSize, float outlineOffset)
    {
        float scale = BBSSettings.axesScale.get();

        axisSize *= scale;
        axisOffset *= scale;
        outlineSize *= scale;
        outlineOffset *= scale;

        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        fillBox(builder, stack, 0, -outlineOffset, -outlineOffset, outlineSize, outlineOffset, outlineOffset, 0, 0, 0);
        fillBox(builder, stack, -outlineOffset, 0, -outlineOffset, outlineOffset, outlineSize, outlineOffset, 0, 0, 0);
        fillBox(builder, stack, -outlineOffset, -outlineOffset, 0, outlineOffset, outlineOffset, outlineSize, 0, 0, 0);
        fillBox(builder, stack, -outlineOffset, -outlineOffset, -outlineOffset, outlineOffset, outlineOffset, outlineOffset, 0, 0, 0);

        fillBox(builder, stack, 0, -axisOffset, -axisOffset, axisSize, axisOffset, axisOffset, 1, 0, 0);
        fillBox(builder, stack, -axisOffset, 0, -axisOffset, axisOffset, axisSize, axisOffset, 0, 1, 0);
        fillBox(builder, stack, -axisOffset, -axisOffset, 0, axisOffset, axisOffset, axisSize, 0, 0, 1);
        fillBox(builder, stack, -axisOffset, -axisOffset, -axisOffset, axisOffset, axisOffset, axisOffset, 1, 1, 1);

        // RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        /* shader binding handled by RenderLayer in 1.21.11 */
        GlStateManager._disableDepthTest();

        RenderTypes.debugFilledBox().draw(builder.buildOrThrow());
    }

    public static void arc3D(BufferBuilder builder, PoseStack stack, Axis axis, float radius, float thickness, float r, float g, float b)
    {
        arc3D(builder, stack, axis, radius, thickness, r, g, b, 0F, 360F);
    }

    /**
     * Based on ElGatoPro300's code from BBS mod CML edition
     */
    public static void arc3D(BufferBuilder builder, PoseStack stack, Axis axis, float radius, float thickness, float r, float g, float b, float startDeg, float sweepDeg)
    {
        int segU = 96;
        int segV = 24;
        double u0 = Math.toRadians(startDeg);
        double uStep = Math.toRadians(sweepDeg / (double) segU);
        double vStep = Math.PI * 2D / (double) segV;

        stack.pushPose();

        if (axis == Axis.X) stack.mulPose(com.mojang.math.Axis.ZP.rotation(MathUtils.PI / 2F));
        if (axis == Axis.Z) stack.mulPose(com.mojang.math.Axis.XP.rotation(MathUtils.PI / 2F));

        float tubeR = thickness * 0.5F;
        Matrix4f mat = new Matrix4f();

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

                builder.addVertex(mat, x11, y11, z11).setColor(r, g, b, 1F);
                builder.addVertex(mat, x12, y12, z12).setColor(r, g, b, 1F);
                builder.addVertex(mat, x22, y22, z22).setColor(r, g, b, 1F);

                builder.addVertex(mat, x11, y11, z11).setColor(r, g, b, 1F);
                builder.addVertex(mat, x22, y22, z22).setColor(r, g, b, 1F);
                builder.addVertex(mat, x21, y21, z21).setColor(r, g, b, 1F);
            }
        }

        stack.popPose();
    }
}
