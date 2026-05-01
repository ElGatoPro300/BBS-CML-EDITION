package mchorse.bbs_mod.ui.utils;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.utils.Axis;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import com.mojang.blaze3d.systems.RenderSystem;

import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

public class Gizmo
{
    public interface IGizmoHandler
    {
        public void start(Gizmo gizmo, int index, int mouseX, int mouseY, UIPropTransform transform);
    }

    public final static int STENCIL_X = 1;
    public final static int STENCIL_Y = 2;
    public final static int STENCIL_Z = 3;
    public final static int STENCIL_XZ = 4;
    public final static int STENCIL_XY = 5;
    public final static int STENCIL_ZY = 6;
    public final static int STENCIL_FREE = 7;

    public final static Gizmo INSTANCE = new Gizmo();

    private Mode mode = Mode.TRANSLATE;

    private int index = -1;

    private UIPropTransform currentTransform;
    private Map<Integer, IGizmoHandler> handlers = new HashMap<>();

    private float lastSx = 1F;
    private float lastSy = 1F;
    private float lastSz = 1F;

    private Gizmo()
    {}

    public void register(int index, IGizmoHandler handler)
    {
        this.handlers.put(index, handler);
    }

    public Mode getMode()
    {
        return this.mode;
    }

    public boolean setMode(Mode mode)
    {
        if (!BBSSettings.gizmos.get())
        {
            return false;
        }

        boolean same = this.mode == mode;

        this.mode = mode;

        return !same;
    }

    public boolean start(int index, int mouseX, int mouseY, UIPropTransform transform)
    {
        if (!BBSSettings.gizmos.get())
        {
            return false;
        }

        if (this.handlers.containsKey(index))
        {
            this.handlers.get(index).start(this, index, mouseX, mouseY, transform);
            this.index = index;

            return true;
        }

        if (index >= STENCIL_X && index <= STENCIL_FREE)
        {
            this.index = index;

            this.currentTransform = transform;

            if (transform != null)
            {
                if (this.index == STENCIL_X)
                {
                    transform.enableMode(this.mode.ordinal(), Axis.X);
                }
                else if (this.index == STENCIL_Y)
                {
                    transform.enableMode(this.mode.ordinal(), Axis.Y);
                }
                else if (this.index == STENCIL_Z)
                {
                    transform.enableMode(this.mode.ordinal(), Axis.Z);
                }
                else if (this.index == STENCIL_XY)
                {
                    transform.enablePlaneMode(this.mode.ordinal(), Axis.X, Axis.Y);
                }
                else if (this.index == STENCIL_XZ)
                {
                    transform.enablePlaneMode(this.mode.ordinal(), Axis.X, Axis.Z);
                }
                else if (this.index == STENCIL_ZY)
                {
                    transform.enablePlaneMode(this.mode.ordinal(), Axis.Z, Axis.Y);
                }
                else if (this.index == STENCIL_FREE && this.mode == Mode.ROTATE)
                {
                    transform.enableFreeRotation(this.mode.ordinal(), Axis.X);
                }
            }

            return true;
        }

        return false;
    }

    public void stop()
    {
        this.index = -1;

        if (this.currentTransform != null)
        {
            this.currentTransform.acceptChanges();
        }

        this.currentTransform = null;
    }

    public void render(MatrixStack stack)
    {
        float thickness = BBSSettings.axesThickness == null ? 1F : BBSSettings.axesThickness.get();

        if (BBSSettings.gizmos.get())
        {
            this.drawAxes(stack, 0.25F, 0.015F * thickness, 0.26F, 0.025F * thickness);
        }
        else
        {
            Draw.coolerAxes(stack, 0.25F, 0.015F * thickness, 0.26F, 0.025F * thickness);
        }
    }

    private void drawBox(BufferBuilder builder, MatrixStack stack, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b)
    {
        Draw.fillBox(builder, stack, Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2), Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2), r, g, b);
    }

    private void drawAxes(MatrixStack stack, float axisSize, float axisOffset, float outlineSize, float outlineOffset)
    {
        float scale = BBSSettings.axesScale.get();

        axisSize *= scale;
        axisOffset *= scale;
        outlineSize *= scale;
        outlineOffset *= scale;

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        Matrix4f inv = new Matrix4f(stack.peek().getPositionMatrix()).invert();
        Vector4f camPos = new Vector4f(0, 0, 0, 1).mul(inv);
        float sx = camPos.x >= 0 ? 1F : -1F;
        float sy = camPos.y >= 0 ? 1F : -1F;
        float sz = camPos.z >= 0 ? 1F : -1F;

        if (this.index == -1)
        {
            this.lastSx = sx;
            this.lastSy = sy;
            this.lastSz = sz;
        }
        else
        {
            sx = this.lastSx;
            sy = this.lastSy;
            sz = this.lastSz;
        }

        boolean activeX = this.index == -1 || this.index == STENCIL_X;
        boolean activeY = this.index == -1 || this.index == STENCIL_Y;
        boolean activeZ = this.index == -1 || this.index == STENCIL_Z;
        boolean activeXZ = this.index == -1 || this.index == STENCIL_XZ;
        boolean activeXY = this.index == -1 || this.index == STENCIL_XY;
        boolean activeZY = this.index == -1 || this.index == STENCIL_ZY;
        boolean activeFree = this.index == -1 || this.index == STENCIL_FREE;

        BufferBuilder builder = Tessellator.getInstance().getBuffer();

        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        if (this.mode == Mode.ROTATE)
        {
            float outlinePad = 0.015F * scale * (BBSSettings.axesThickness == null ? 1F : BBSSettings.axesThickness.get());
            float radius = 0.22F * scale;
            float thicknessRing = 0.025F * scale * (BBSSettings.axesThickness == null ? 1F : BBSSettings.axesThickness.get());

            if (activeZ)
            {
                Draw.arc3D(builder, stack, Axis.Z, radius, thicknessRing + outlinePad, 0F, 0F, 0F);
                Draw.arc3D(builder, stack, Axis.Z, radius, thicknessRing, 0F, 0F, 1F);
            }

            if (activeX)
            {
                Draw.arc3D(builder, stack, Axis.X, radius, thicknessRing + outlinePad, 0F, 0F, 0F);
                Draw.arc3D(builder, stack, Axis.X, radius, thicknessRing, 1F, 0F, 0F);
            }

            if (activeY)
            {
                Draw.arc3D(builder, stack, Axis.Y, radius, thicknessRing + outlinePad, 0F, 0F, 0F);
                Draw.arc3D(builder, stack, Axis.Y, radius, thicknessRing, 0F, 1F, 0F);
            }

            if (activeFree)
            {
                this.drawBox(builder, stack, -outlineOffset, -outlineOffset, -outlineOffset, outlineOffset, outlineOffset, outlineOffset, 0F, 0F, 0F);
                this.drawBox(builder, stack, -axisOffset, -axisOffset, -axisOffset, axisOffset, axisOffset, axisOffset, 1F, 1F, 1F);
            }
        }
        else
        {
            if (activeX) this.drawBox(builder, stack, 0, -outlineOffset, -outlineOffset, outlineSize * sx, outlineOffset, outlineOffset, 0F, 0F, 0F);
            if (activeY) this.drawBox(builder, stack, -outlineOffset, 0, -outlineOffset, outlineOffset, outlineSize * sy, outlineOffset, 0F, 0F, 0F);
            if (activeZ) this.drawBox(builder, stack, -outlineOffset, -outlineOffset, 0, outlineOffset, outlineOffset, outlineSize * sz, 0F, 0F, 0F);
            if (this.index == -1) this.drawBox(builder, stack, -outlineOffset, -outlineOffset, -outlineOffset, outlineOffset, outlineOffset, outlineOffset, 0F, 0F, 0F);

            if (this.mode == Mode.SCALE)
            {
                float scaleStart = axisSize + axisOffset / 2F - outlineOffset / 2F;
                float scaleEnd = axisSize + axisOffset / 2F + outlineOffset / 2F;
                float offset = axisOffset * 2.75F;

                if (activeX) this.drawBox(builder, stack, scaleStart * sx, -offset, -offset, scaleEnd * sx, offset, offset, 0F, 0F, 0F);
                if (activeY) this.drawBox(builder, stack, -offset, scaleStart * sy, -offset, offset, scaleEnd * sy, offset, 0F, 0F, 0F);
                if (activeZ) this.drawBox(builder, stack, -offset, -offset, scaleStart * sz, offset, offset, scaleEnd * sz, 0F, 0F, 0F);
            }

            if (activeX) this.drawBox(builder, stack, 0, -axisOffset, -axisOffset, axisSize * sx, axisOffset, axisOffset, 1F, 0F, 0F);
            if (activeY) this.drawBox(builder, stack, -axisOffset, 0, -axisOffset, axisOffset, axisSize * sy, axisOffset, 0F, 1F, 0F);
            if (activeZ) this.drawBox(builder, stack, -axisOffset, -axisOffset, 0, axisOffset, axisOffset, axisSize * sz, 0F, 0F, 1F);
            if (this.index == -1) this.drawBox(builder, stack, -axisOffset, -axisOffset, -axisOffset, axisOffset, axisOffset, axisOffset, 1F, 1F, 1F);

            if (this.mode == Mode.TRANSLATE)
            {
                float planeInner = axisSize * 0.25F;
                float planeOuter = axisSize * 0.65F;
                float offset = 0.001F;

                this.drawTranslatePlanes(builder, stack, planeInner, planeOuter, offset, sx, sy, sz, activeXZ, activeXY, activeZY);
            }

            if (this.mode == Mode.SCALE)
            {
                float scaleEnd = axisSize + axisOffset;

                if (activeX) this.drawBox(builder, stack, axisSize * sx, -axisOffset * 2F, -axisOffset * 2F, scaleEnd * sx, axisOffset * 2F, axisOffset * 2F, 1F, 0F, 0F);
                if (activeY) this.drawBox(builder, stack, -axisOffset * 2F, axisSize * sy, -axisOffset * 2F, axisOffset * 2F, scaleEnd * sy, axisOffset * 2F, 0F, 1F, 0F);
                if (activeZ) this.drawBox(builder, stack, -axisOffset * 2F, -axisOffset * 2F, axisSize * sz, axisOffset * 2F, axisOffset * 2F, scaleEnd * sz, 0F, 0F, 1F);
            }
        }

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.depthFunc(GL11.GL_ALWAYS);

        BufferRenderer.drawWithGlobalProgram(builder.end());

        RenderSystem.depthFunc(GL11.GL_LEQUAL);
    }

    public void renderStencil(MatrixStack stack, StencilMap map)
    {
        if (BBSSettings.gizmos.get())
        {
            this.drawAxes(stack, map, 0.25F, 0.015F);
        }
    }

    private void drawAxes(MatrixStack stack, StencilMap map, float axisSize, float axisOffset)
    {
        float scale = BBSSettings.axesScale.get();

        axisSize *= scale;
        axisOffset *= scale;

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        Matrix4f inv = new Matrix4f(stack.peek().getPositionMatrix()).invert();
        Vector4f camPos = new Vector4f(0, 0, 0, 1).mul(inv);
        float sx = camPos.x >= 0 ? 1F : -1F;
        float sy = camPos.y >= 0 ? 1F : -1F;
        float sz = camPos.z >= 0 ? 1F : -1F;

        if (this.index == -1)
        {
            this.lastSx = sx;
            this.lastSy = sy;
            this.lastSz = sz;
        }
        else
        {
            sx = this.lastSx;
            sy = this.lastSy;
            sz = this.lastSz;
        }

        BufferBuilder builder = Tessellator.getInstance().getBuffer();

        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        if (this.mode == Mode.ROTATE)
        {
            float outlinePad = 0.015F * scale * (BBSSettings.axesThickness == null ? 1F : BBSSettings.axesThickness.get());
            float radius = 0.22F * scale;
            float thicknessRing = 0.025F * scale * (BBSSettings.axesThickness == null ? 1F : BBSSettings.axesThickness.get());

            Draw.arc3D(builder, stack, Axis.Z, radius, thicknessRing + outlinePad, STENCIL_Z / 255F, 0F, 0F);
            Draw.arc3D(builder, stack, Axis.X, radius, thicknessRing + outlinePad, STENCIL_X / 255F, 0F, 0F);
            Draw.arc3D(builder, stack, Axis.Y, radius, thicknessRing + outlinePad, STENCIL_Y / 255F, 0F, 0F);

            this.drawBox(builder, stack, -axisOffset, -axisOffset, -axisOffset, axisOffset, axisOffset, axisOffset, STENCIL_FREE / 255F, 0F, 0F);
        }
        else
        {
            this.drawBox(builder, stack, 0, -axisOffset, -axisOffset, axisSize * sx, axisOffset, axisOffset, STENCIL_X / 255F, 0F, 0F);
            this.drawBox(builder, stack, -axisOffset, 0, -axisOffset, axisOffset, axisSize * sy, axisOffset, STENCIL_Y / 255F, 0F, 0F);
            this.drawBox(builder, stack, -axisOffset, -axisOffset, 0, axisOffset, axisOffset, axisSize * sz, STENCIL_Z / 255F, 0F, 0F);
            this.drawBox(builder, stack, -axisOffset, -axisOffset, -axisOffset, axisOffset, axisOffset, axisOffset, 0F, 0F, 0F);

            if (this.mode == Mode.SCALE)
            {
                float scaleEnd = axisSize + axisOffset;

                this.drawBox(builder, stack, axisSize * sx, -axisOffset * 2F, -axisOffset * 2F, scaleEnd * sx, axisOffset * 2F, axisOffset * 2F, STENCIL_X / 255F, 0F, 0F);
                this.drawBox(builder, stack, -axisOffset * 2F, axisSize * sy, -axisOffset * 2F, axisOffset * 2F, scaleEnd * sy, axisOffset * 2F, STENCIL_Y / 255F, 0F, 0F);
                this.drawBox(builder, stack, -axisOffset * 2F, -axisOffset * 2F, axisSize * sz, axisOffset * 2F, axisOffset * 2F, scaleEnd * sz, STENCIL_Z / 255F, 0F, 0F);
            }

            if (this.mode == Mode.TRANSLATE)
            {
                float planeInner = axisSize * 0.25F;
                float offset = 0.001F;
                float planeOuter = axisSize * 0.65F;

                this.drawTranslateStencilPlanes(builder, stack, planeInner, planeOuter, offset, sx, sy, sz);
            }
        }

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.disableDepthTest();

        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    private void drawTranslatePlanes(BufferBuilder builder, MatrixStack stack, float planeInner, float planeOuter, float offset, float sx, float sy, float sz, boolean activeXZ, boolean activeXY, boolean activeZY)
    {
        if (activeXZ) this.drawBox(builder, stack, planeInner * sx, -offset, planeInner * sz, planeOuter * sx, offset, planeOuter * sz, 0F, 1F, 0F);
        if (activeXY) this.drawBox(builder, stack, planeInner * sx, planeInner * sy, -offset, planeOuter * sx, planeOuter * sy, offset, 0F, 0F, 1F);
        if (activeZY) this.drawBox(builder, stack, -offset, planeInner * sy, planeInner * sz, offset, planeOuter * sy, planeOuter * sz, 1F, 0F, 0F);
    }

    private void drawTranslateStencilPlanes(BufferBuilder builder, MatrixStack stack, float planeInner, float planeOuter, float offset, float sx, float sy, float sz)
    {
        float xz = STENCIL_XZ / 255F;
        float xy = STENCIL_XY / 255F;
        float zy = STENCIL_ZY / 255F;

        this.drawBox(builder, stack, planeInner * sx, -offset, planeInner * sz, planeOuter * sx, offset, planeOuter * sz, xz, 0F, 0F);
        this.drawBox(builder, stack, planeInner * sx, planeInner * sy, -offset, planeOuter * sx, planeOuter * sy, offset, xy, 0F, 0F);
        this.drawBox(builder, stack, -offset, planeInner * sy, planeInner * sz, offset, planeOuter * sy, planeOuter * sz, zy, 0F, 0F);
    }

    public static enum Mode
    {
        TRANSLATE, SCALE, ROTATE;
    }
}
