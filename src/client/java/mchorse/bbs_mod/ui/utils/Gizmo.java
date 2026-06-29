package mchorse.bbs_mod.ui.utils;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.utils.StencilMap;
import mchorse.bbs_mod.utils.Axis;

import net.minecraft.client.gl.ShaderProgramKeys;
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

    public final Matrix4f lastGizmoMatrix = new Matrix4f();
    public boolean hasGizmoMatrix;

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

    public boolean isDragging()
    {
        return this.index != -1;
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
                else if (this.index == STENCIL_FREE)
                {
                    if (this.mode == Mode.TRANSLATE)
                    {
                        transform.enableFreeTranslation(this.mode.ordinal());
                    }
                    else if (this.mode == Mode.ROTATE)
                    {
                        transform.enableFreeRotation(this.mode.ordinal(), Axis.X);
                    }
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
        this.lastGizmoMatrix.set(stack.peek().getPositionMatrix());
        this.hasGizmoMatrix = true;

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
        this.drawBox(builder, stack, x1, y1, z1, x2, y2, z2, r, g, b, 1.0F);
    }

    private void drawBox(BufferBuilder builder, MatrixStack stack, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a)
    {
        Draw.fillBox(builder, stack, Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2), Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2), r, g, b, a);
    }

    private void drawAxes(MatrixStack stack, float axisSize, float axisOffset, float outlineSize, float outlineOffset)
    {
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        Matrix4f inv = new Matrix4f(stack.peek().getPositionMatrix()).invert();
        Vector4f camPos = new Vector4f(0, 0, 0, 1).mul(inv);
        double dist = Math.sqrt(camPos.x * camPos.x + camPos.y * camPos.y + camPos.z * camPos.z);
        float axesScale = BBSSettings.axesScale == null ? 1F : BBSSettings.axesScale.get();
        float scale = (float) (1.4F * Math.max(0.5D, dist * 0.12D) * axesScale);

        axisSize = 0.30F * scale;
        axisOffset = 0.012F * scale;
        float planeInner = 0.08F * scale;
        float planeOuter = 0.20F * scale;
        float offset = 0.001F * scale;

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

        if (this.mode == Mode.ROTATE)
        {
            float outlinePad = 0.015F * scale * (BBSSettings.axesThickness == null ? 1F : BBSSettings.axesThickness.get());
            float radius = 0.22F * scale;
            float thicknessRing = 0.025F * scale * (BBSSettings.axesThickness == null ? 1F : BBSSettings.axesThickness.get());

            if (activeZ)
            {
                float[] color = (this.index == STENCIL_Z) ? new float[] { 1.0F, 1.0F, 0.0F } : new float[] { 0.15F, 0.35F, 1.0F };
                Draw.arc3D(builder, stack, Axis.Z, radius, thicknessRing, color[0], color[1], color[2]);
            }

            if (activeX)
            {
                float[] color = (this.index == STENCIL_X) ? new float[] { 1.0F, 1.0F, 0.0F } : new float[] { 1.0F, 0.15F, 0.15F };
                Draw.arc3D(builder, stack, Axis.X, radius, thicknessRing, color[0], color[1], color[2]);
            }

            if (activeY)
            {
                float[] color = (this.index == STENCIL_Y) ? new float[] { 1.0F, 1.0F, 0.0F } : new float[] { 0.15F, 1.0F, 0.15F };
                Draw.arc3D(builder, stack, Axis.Y, radius, thicknessRing, color[0], color[1], color[2]);
            }

            if (activeFree)
            {
                float[] color = (this.index == STENCIL_FREE) ? new float[] { 1.0F, 1.0F, 0.0F } : new float[] { 1.0F, 1.0F, 1.0F };
                this.drawBox(builder, stack, -axisOffset, -axisOffset, -axisOffset, axisOffset, axisOffset, axisOffset, color[0], color[1], color[2], 1.0F);
            }
        }
        else if (this.mode == Mode.SCALE)
        {
            float[] xCol = (this.index == STENCIL_X) ? new float[] { 1.0F, 1.0F, 0.0F } : new float[] { 1.0F, 0.15F, 0.15F };
            float[] yCol = (this.index == STENCIL_Y) ? new float[] { 1.0F, 1.0F, 0.0F } : new float[] { 0.15F, 1.0F, 0.15F };
            float[] zCol = (this.index == STENCIL_Z) ? new float[] { 1.0F, 1.0F, 0.0F } : new float[] { 0.15F, 0.35F, 1.0F };
            float[] freeCol = (this.index == STENCIL_FREE) ? new float[] { 1.0F, 1.0F, 0.0F } : new float[] { 1.0F, 1.0F, 1.0F };

            if (activeX)
            {
                this.drawBox(builder, stack, 0, -axisOffset, -axisOffset, axisSize * sx, axisOffset, axisOffset, xCol[0], xCol[1], xCol[2], 1.0F);
            }
            if (activeY)
            {
                this.drawBox(builder, stack, -axisOffset, 0, -axisOffset, axisOffset, axisSize * sy, axisOffset, yCol[0], yCol[1], yCol[2], 1.0F);
            }
            if (activeZ)
            {
                this.drawBox(builder, stack, -axisOffset, -axisOffset, 0, axisOffset, axisOffset, axisSize * sz, zCol[0], zCol[1], zCol[2], 1.0F);
            }
            if (activeFree)
            {
                this.drawBox(builder, stack, -axisOffset, -axisOffset, -axisOffset, axisOffset, axisOffset, axisOffset, freeCol[0], freeCol[1], freeCol[2], 1.0F);
            }

            float half = axisOffset * 2.0F;
            if (activeX)
            {
                this.drawBox(builder, stack, axisSize * sx - half, -half, -half, axisSize * sx + half, half, half, xCol[0], xCol[1], xCol[2], 1.0F);
            }
            if (activeY)
            {
                this.drawBox(builder, stack, -half, axisSize * sy - half, -half, half, axisSize * sy + half, half, yCol[0], yCol[1], yCol[2], 1.0F);
            }
            if (activeZ)
            {
                this.drawBox(builder, stack, -half, -half, axisSize * sz - half, half, half, axisSize * sz + half, zCol[0], zCol[1], zCol[2], 1.0F);
            }
        }
        else // TRANSLATE
        {
            float[] xCol = (this.index == STENCIL_X) ? new float[] { 1.0F, 1.0F, 0.0F } : new float[] { 1.0F, 0.15F, 0.15F };
            float[] yCol = (this.index == STENCIL_Y) ? new float[] { 1.0F, 1.0F, 0.0F } : new float[] { 0.15F, 1.0F, 0.15F };
            float[] zCol = (this.index == STENCIL_Z) ? new float[] { 1.0F, 1.0F, 0.0F } : new float[] { 0.15F, 0.35F, 1.0F };

            float[] xzCol = (this.index == STENCIL_XZ) ? new float[] { 1.0F, 1.0F, 0.0F } : new float[] { 0.15F, 1.0F, 0.6F };
            float[] xyCol = (this.index == STENCIL_XY) ? new float[] { 1.0F, 1.0F, 0.0F } : new float[] { 0.6F, 0.15F, 1.0F };
            float[] zyCol = (this.index == STENCIL_ZY) ? new float[] { 1.0F, 1.0F, 0.0F } : new float[] { 1.0F, 0.4F, 0.15F };

            float[] freeCol = (this.index == STENCIL_FREE) ? new float[] { 1.0F, 1.0F, 0.0F } : new float[] { 1.0F, 1.0F, 1.0F };

            if (activeX)
            {
                this.drawBox(builder, stack, 0, -axisOffset, -axisOffset, axisSize * sx, axisOffset, axisOffset, xCol[0], xCol[1], xCol[2], 1.0F);
            }
            if (activeY)
            {
                this.drawBox(builder, stack, -axisOffset, 0, -axisOffset, axisOffset, axisSize * sy, axisOffset, yCol[0], yCol[1], yCol[2], 1.0F);
            }
            if (activeZ)
            {
                this.drawBox(builder, stack, -axisOffset, -axisOffset, 0, axisOffset, axisOffset, axisSize * sz, zCol[0], zCol[1], zCol[2], 1.0F);
            }

            if (activeXZ)
            {
                this.drawBox(builder, stack, planeInner * sx, -offset, planeInner * sz, planeOuter * sx, offset, planeOuter * sz, xzCol[0], xzCol[1], xzCol[2], 0.4F);
            }
            if (activeXY)
            {
                this.drawBox(builder, stack, planeInner * sx, planeInner * sy, -offset, planeOuter * sx, planeOuter * sy, offset, xyCol[0], xyCol[1], xyCol[2], 0.4F);
            }
            if (activeZY)
            {
                this.drawBox(builder, stack, -offset, planeInner * sy, planeInner * sz, offset, planeOuter * sy, planeOuter * sz, zyCol[0], zyCol[1], zyCol[2], 0.4F);
            }

            if (activeFree)
            {
                this.drawBox(builder, stack, -axisOffset, -axisOffset, -axisOffset, axisOffset, axisOffset, axisOffset, freeCol[0], freeCol[1], freeCol[2], 1.0F);
            }
        }

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.depthFunc(GL11.GL_ALWAYS);
        RenderSystem.depthMask(false);

        BufferRenderer.drawWithGlobalProgram(builder.end());

        RenderSystem.depthMask(true);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
    }

    public void renderStencil(MatrixStack stack, StencilMap map)
    {
        this.lastGizmoMatrix.set(stack.peek().getPositionMatrix());
        this.hasGizmoMatrix = true;

        if (BBSSettings.gizmos.get())
        {
            this.drawAxes(stack, map, 0.25F, 0.015F);
        }
    }

    private void drawAxes(MatrixStack stack, StencilMap map, float axisSize, float axisOffset)
    {
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        Matrix4f inv = new Matrix4f(stack.peek().getPositionMatrix()).invert();
        Vector4f camPos = new Vector4f(0, 0, 0, 1).mul(inv);
        double dist = Math.sqrt(camPos.x * camPos.x + camPos.y * camPos.y + camPos.z * camPos.z);
        float axesScale = BBSSettings.axesScale == null ? 1F : BBSSettings.axesScale.get();
        float scale = (float) (1.4F * Math.max(0.5D, dist * 0.12D) * axesScale);

        axisSize = 0.30F * scale;
        axisOffset = 0.012F * scale;
        float planeInner = 0.08F * scale;
        float planeOuter = 0.20F * scale;
        float offset = 0.001F * scale;

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

        if (this.mode == Mode.ROTATE)
        {
            float outlinePad = 0.015F * scale * (BBSSettings.axesThickness == null ? 1F : BBSSettings.axesThickness.get());
            float radius = 0.22F * scale;
            float thicknessRing = 0.025F * scale * (BBSSettings.axesThickness == null ? 1F : BBSSettings.axesThickness.get());

            Draw.arc3D(builder, stack, Axis.Z, radius, thicknessRing + outlinePad, STENCIL_Z / 255F, 0F, 0F);
            Draw.arc3D(builder, stack, Axis.X, radius, thicknessRing + outlinePad, STENCIL_X / 255F, 0F, 0F);
            Draw.arc3D(builder, stack, Axis.Y, radius, thicknessRing + outlinePad, STENCIL_Y / 255F, 0F, 0F);

            this.drawBox(builder, stack, -axisOffset, -axisOffset, -axisOffset, axisOffset, axisOffset, axisOffset, STENCIL_FREE / 255F, 0F, 0F, 1.0F);
        }
        else if (this.mode == Mode.SCALE)
        {
            this.drawBox(builder, stack, 0, -axisOffset, -axisOffset, axisSize * sx, axisOffset, axisOffset, STENCIL_X / 255F, 0F, 0F, 1.0F);
            this.drawBox(builder, stack, -axisOffset, 0, -axisOffset, axisOffset, axisSize * sy, axisOffset, STENCIL_Y / 255F, 0F, 0F, 1.0F);
            this.drawBox(builder, stack, -axisOffset, -axisOffset, 0, axisOffset, axisOffset, axisSize * sz, STENCIL_Z / 255F, 0F, 0F, 1.0F);
            this.drawBox(builder, stack, -axisOffset, -axisOffset, -axisOffset, axisOffset, axisOffset, axisOffset, STENCIL_FREE / 255F, 0F, 0F, 1.0F);

            float half = axisOffset * 2.0F;
            this.drawBox(builder, stack, axisSize * sx - half, -half, -half, axisSize * sx + half, half, half, STENCIL_X / 255F, 0F, 0F, 1.0F);
            this.drawBox(builder, stack, -half, axisSize * sy - half, -half, half, axisSize * sy + half, half, STENCIL_Y / 255F, 0F, 0F, 1.0F);
            this.drawBox(builder, stack, -half, -half, axisSize * sz - half, half, half, axisSize * sz + half, STENCIL_Z / 255F, 0F, 0F, 1.0F);
        }
        else // TRANSLATE
        {
            this.drawBox(builder, stack, 0, -axisOffset, -axisOffset, axisSize * sx, axisOffset, axisOffset, STENCIL_X / 255F, 0F, 0F, 1.0F);
            this.drawBox(builder, stack, -axisOffset, 0, -axisOffset, axisOffset, axisSize * sy, axisOffset, STENCIL_Y / 255F, 0F, 0F, 1.0F);
            this.drawBox(builder, stack, -axisOffset, -axisOffset, 0, axisOffset, axisOffset, axisSize * sz, STENCIL_Z / 255F, 0F, 0F, 1.0F);

            this.drawBox(builder, stack, planeInner * sx, -offset, planeInner * sz, planeOuter * sx, offset, planeOuter * sz, STENCIL_XZ / 255F, 0F, 0F, 1.0F);
            this.drawBox(builder, stack, planeInner * sx, planeInner * sy, -offset, planeOuter * sx, planeOuter * sy, offset, STENCIL_XY / 255F, 0F, 0F, 1.0F);
            this.drawBox(builder, stack, -offset, planeInner * sy, planeInner * sz, offset, planeOuter * sy, planeOuter * sz, STENCIL_ZY / 255F, 0F, 0F, 1.0F);

            this.drawBox(builder, stack, -axisOffset, -axisOffset, -axisOffset, axisOffset, axisOffset, axisOffset, STENCIL_FREE / 255F, 0F, 0F, 1.0F);
        }

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        BufferRenderer.drawWithGlobalProgram(builder.end());

        RenderSystem.depthMask(true);
    }

    public static enum Mode
    {
        TRANSLATE, SCALE, ROTATE;
    }
}
