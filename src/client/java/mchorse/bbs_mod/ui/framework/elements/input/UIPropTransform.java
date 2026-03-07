package mchorse.bbs_mod.ui.framework.elements.input;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.IValueNotifier;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;
import mchorse.bbs_mod.ui.utils.Gizmo;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Axis;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.Timer;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.pose.Transform;
import net.minecraft.client.MinecraftClient;
import org.joml.Intersectiond;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class UIPropTransform extends UITransform
{
    public static final List<BiConsumer<UIPropTransform, ContextMenuManager>> contextMenuExtensions = new ArrayList<>();

    private static final double[] CURSOR_X = new double[1];
    private static final double[] CURSOR_Y = new double[1];

    private Transform transform;
    private Runnable preCallback;
    private Runnable postCallback;

    private boolean editing;
    private int mode;
    private Axis axis = Axis.X;
    private Axis secondaryAxis;
    private int lastX;
    private int lastY;
    private Transform cache = new Transform();
    private Timer checker = new Timer(30);

    private boolean model;
    private boolean local;
    private boolean freeRotation;

    private UITransformHandler handler;

    private boolean gizmoDrag;
    private final Matrix4f gizmoMvp = new Matrix4f();
    private final Matrix4f gizmoInvMvp = new Matrix4f();
    private int gizmoViewportX;
    private int gizmoViewportY;
    private int gizmoViewportW;
    private int gizmoViewportH;
    private final Vector2f gizmoOrigin2D = new Vector2f();
    private final Vector2f gizmoAxisX = new Vector2f();
    private final Vector2f gizmoAxisY = new Vector2f();
    private final Vector2f gizmoAxisZ = new Vector2f();
    private float gizmoAxisXLenSq;
    private float gizmoAxisYLenSq;
    private float gizmoAxisZLenSq;
    private boolean gizmoInvReady;
    private final Vector2f gizmoTmp2D = new Vector2f();
    private final Vector4f gizmoTmp4D = new Vector4f();
    private final Vector4f gizmoNear4D = new Vector4f();
    private final Vector4f gizmoFar4D = new Vector4f();
    private final Vector3d gizmoRayStart = new Vector3d();
    private final Vector3d gizmoRayEnd = new Vector3d();
    private final Vector3d gizmoP0 = new Vector3d();
    private final Vector3d gizmoP1 = new Vector3d();
    private final Vector3d gizmoCross = new Vector3d();
    private final Vector3d gizmoTangent = new Vector3d();
    private final Vector3d gizmoAxisNormal = new Vector3d();
    private final Vector2f gizmoP2D = new Vector2f();
    private final Vector2f gizmoP2DNext = new Vector2f();

    public UIPropTransform()
    {
        this.handler = new UITransformHandler(this);

        this.context((menu) ->
        {
            menu.action(
                this.local ? Icons.FULLSCREEN : Icons.MINIMIZE,
                this.local ? UIKeys.TRANSFORMS_CONTEXT_SWITCH_GLOBAL : UIKeys.TRANSFORMS_CONTEXT_SWITCH_LOCAL,
                this::toggleLocal
            );

            menu.actions.add(0, menu.actions.remove(menu.actions.size() - 1));

            for (BiConsumer<UIPropTransform, ContextMenuManager> consumer : contextMenuExtensions)
            {
                consumer.accept(this, menu);
            }
        });

        this.iconT.callback = (b) -> this.toggleLocal();
        this.iconT.hoverColor = Colors.LIGHTEST_GRAY;
        this.iconT.setEnabled(true);
        this.iconT.tooltip(this.local ? UIKeys.TRANSFORMS_CONTEXT_SWITCH_GLOBAL : UIKeys.TRANSFORMS_CONTEXT_SWITCH_LOCAL);

        this.noCulling();
    }

    public UIPropTransform callbacks(Supplier<IValueNotifier> notifier)
    {
        return this.callbacks(
            () -> notifier.get().preNotify(),
            () -> notifier.get().postNotify()
        );
    }

    public UIPropTransform callbacks(Runnable pre, Runnable post)
    {
        this.preCallback = pre;
        this.postCallback = post;

        return this;
    }

    public void preCallback()
    {
        if (this.preCallback != null) this.preCallback.run();
    }

    public void postCallback()
    {
        if (this.postCallback != null) this.postCallback.run();
    }

    public void setModel()
    {
        this.model = true;
    }

    public boolean isLocal()
    {
        return this.local;
    }

    private void toggleLocal()
    {
        this.local = !this.local;

        if (!this.local)
        {
            this.fillT(this.transform.translate.x, this.transform.translate.y, this.transform.translate.z);
        }

        this.tx.forcedLabel(this.local ? UIKeys.GENERAL_X : null);
        this.ty.forcedLabel(this.local ? UIKeys.GENERAL_Y : null);
        this.tz.forcedLabel(this.local ? UIKeys.GENERAL_Z : null);
        this.tx.relative(this.local);
        this.ty.relative(this.local);
        this.tz.relative(this.local);
        this.iconT.tooltip(this.local ? UIKeys.TRANSFORMS_CONTEXT_SWITCH_GLOBAL : UIKeys.TRANSFORMS_CONTEXT_SWITCH_LOCAL);
    }

    private Vector3f calculateLocalVector(double factor, Axis axis)
    {
        Vector3f vector3f = new Vector3f(
            (float) (axis == Axis.X ? factor : 0D),
            (float) (axis == Axis.Y ? factor : 0D),
            (float) (axis == Axis.Z ? factor : 0D)
        );
        /* I have no fucking idea why I have to rotate it 180 degrees by X axis... but it works! */
        Matrix3f matrix = new Matrix3f()
            .rotateX(this.model ? MathUtils.PI : 0F)
            .mul(this.transform.createRotationMatrix());

        matrix.transform(vector3f);

        return vector3f;
    }

    public UIPropTransform enableHotkeys()
    {
        IKey category = UIKeys.TRANSFORMS_KEYS_CATEGORY;
        Supplier<Boolean> active = () -> this.editing;

        this.keys().register(Keys.TRANSFORMATIONS_TRANSLATE, () -> this.enableMode(0)).category(category);
        this.keys().register(Keys.TRANSFORMATIONS_SCALE, () -> this.enableMode(1)).category(category);
        this.keys().register(Keys.TRANSFORMATIONS_ROTATE, () -> this.enableMode(2)).category(category);
        this.keys().register(Keys.TRANSFORMATIONS_X, () -> this.axis = Axis.X).active(active).category(category);
        this.keys().register(Keys.TRANSFORMATIONS_Y, () -> this.axis = Axis.Y).active(active).category(category);
        this.keys().register(Keys.TRANSFORMATIONS_Z, () -> this.axis = Axis.Z).active(active).category(category);
        this.keys().register(Keys.TRANSFORMATIONS_TOGGLE_LOCAL, () ->
        {
            this.toggleLocal();
            UIUtils.playClick();
        }).category(category);

        return this;
    }

    public Transform getTransform()
    {
        return this.transform;
    }

    public void refillTransform()
    {
        this.setTransform(this.getTransform());
    }

    public void setTransform(Transform transform)
    {
        if (transform == null)
        {
            return;
        }

        this.transform = transform;

        float minScale = Math.min(transform.scale.x, Math.min(transform.scale.y, transform.scale.z));
        float maxScale = Math.max(transform.scale.x, Math.max(transform.scale.y, transform.scale.z));

        if (BBSSettings.uniformScale.get())
        {
            if (
                (minScale == maxScale && !this.isUniformScale()) ||
                (minScale != maxScale && this.isUniformScale())
            ) {
                this.toggleUniformScale();
            }
        }

        this.fillT(transform.translate.x, transform.translate.y, transform.translate.z);
        this.fillS(transform.scale.x, transform.scale.y, transform.scale.z);
        this.fillR(MathUtils.toDeg(transform.rotate.x), MathUtils.toDeg(transform.rotate.y), MathUtils.toDeg(transform.rotate.z));
        this.fillR2(MathUtils.toDeg(transform.rotate2.x), MathUtils.toDeg(transform.rotate2.y), MathUtils.toDeg(transform.rotate2.z));
        this.fillP(transform.pivot.x, transform.pivot.y, transform.pivot.z);
    }

    public void enableMode(int mode)
    {
        this.enableMode(mode, null);
    }

    public void enableMode(int mode, Axis axis)
    {
        this.clearGizmoDrag();

        if (Gizmo.INSTANCE.setMode(Gizmo.Mode.values()[mode]) && axis == null)
        {
            return;
        }

        UIContext context = this.getContext();

        if (this.editing)
        {
            Axis[] values = Axis.values();

            this.axis = values[MathUtils.cycler(this.axis.ordinal() + 1, 0, values.length - 1)];
            this.secondaryAxis = null;
            this.freeRotation = false;

            this.restore(true);
        }
        else
        {
            this.axis = axis == null ? Axis.X : axis;
            this.secondaryAxis = null;
            this.freeRotation = false;
            this.lastX = context.mouseX;
            this.lastY = context.mouseY;
        }

        this.editing = true;
        this.mode = mode;

        this.cache.copy(this.transform);

        if (!this.handler.hasParent())
        {
            context.menu.overlay.add(this.handler);
        }
    }

    public void enablePlaneMode(int mode, Axis primary, Axis secondary)
    {
        this.clearGizmoDrag();

        if (Gizmo.INSTANCE.setMode(Gizmo.Mode.values()[mode]) && primary == null)
        {
            return;
        }

        UIContext context = this.getContext();

        this.axis = primary == null ? Axis.X : primary;
        this.secondaryAxis = secondary;
        this.freeRotation = false;
        this.lastX = context.mouseX;
        this.lastY = context.mouseY;

        this.editing = true;
        this.mode = mode;

        this.cache.copy(this.transform);

        if (!this.handler.hasParent())
        {
            context.menu.overlay.add(this.handler);
        }
    }

    public void enableFreeRotation(int mode, Axis marker)
    {
        this.clearGizmoDrag();

        if (Gizmo.INSTANCE.setMode(Gizmo.Mode.values()[mode]) && marker == null)
        {
            return;
        }

        UIContext context = this.getContext();

        if (this.editing)
        {
            this.freeRotation = true;
            this.secondaryAxis = null;

            this.restore(true);
        }
        else
        {
            this.axis = Axis.X;
            this.secondaryAxis = null;
            this.lastX = context.mouseX;
            this.lastY = context.mouseY;
            this.freeRotation = true;
        }

        this.editing = true;
        this.mode = mode;

        this.cache.copy(this.transform);

        if (!this.handler.hasParent())
        {
            context.menu.overlay.add(this.handler);
        }
    }

    private Vector3f getValue()
    {
        if (this.mode == 1)
        {
            return this.transform.scale;
        }
        else if (this.mode == 2)
        {
            return this.local && BBSSettings.gizmos.get() ? this.transform.rotate2 : this.transform.rotate;
        }

        return this.transform.translate;
    }

    private void restore(boolean fully)
    {
        if (this.mode == 0 || fully) this.setT(null, this.cache.translate.x, this.cache.translate.y, this.cache.translate.z);
        if (this.mode == 1 || fully) this.setS(null, this.cache.scale.x, this.cache.scale.y, this.cache.scale.z);
        if (this.mode == 2 || fully)
        {
            this.setR(null, MathUtils.toDeg(this.cache.rotate.x), MathUtils.toDeg(this.cache.rotate.y), MathUtils.toDeg(this.cache.rotate.z));
            this.setR2(null, MathUtils.toDeg(this.cache.rotate2.x), MathUtils.toDeg(this.cache.rotate2.y), MathUtils.toDeg(this.cache.rotate2.z));
        }
    }

    private void disable()
    {
        this.editing = false;
        this.freeRotation = false;
        this.clearGizmoDrag();

        if (this.handler.hasParent())
        {
            this.handler.removeFromParent();
        }
    }

    public void acceptChanges()
    {
        this.disable();
        this.setTransform(this.transform);
    }

    public void rejectChanges()
    {
        this.disable();
        this.restore(true);
        this.setTransform(this.transform);
    }

    @Override
    protected void internalSetT(double x, Axis axis)
    {
        if (this.local)
        {
            try
            {
                Vector3f vector3f = this.calculateLocalVector(x, axis);

                this.setT(null,
                    this.transform.translate.x + vector3f.x,
                    this.transform.translate.y + vector3f.y,
                    this.transform.translate.z + vector3f.z
                );
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            super.internalSetT(x, axis);
        }
    }

    @Override
    public void setT(Axis axis, double x, double y, double z)
    {
        this.preCallback();
        this.transform.translate.set((float) x, (float) y, (float) z);
        this.postCallback();
    }

    @Override
    public void setS(Axis axis, double x, double y, double z)
    {
        this.preCallback();
        this.transform.scale.set((float) x, (float) y, (float) z);
        this.postCallback();
    }

    @Override
    public void setR(Axis axis, double x, double y, double z)
    {
        this.preCallback();
        this.transform.rotate.set(MathUtils.toRad((float) x), MathUtils.toRad((float) y), MathUtils.toRad((float) z));
        this.postCallback();
    }

    @Override
    public void setR2(Axis axis, double x, double y, double z)
    {
        this.preCallback();
        this.transform.rotate2.set(MathUtils.toRad((float) x), MathUtils.toRad((float) y), MathUtils.toRad((float) z));
        this.postCallback();
    }

    @Override
    public void setP(Axis axis, double x, double y, double z)
    {
        this.preCallback();
        this.transform.pivot.set((float) x, (float) y, (float) z);
        this.postCallback();
    }

    @Override
    protected boolean subKeyPressed(UIContext context)
    {
        if (this.editing)
        {
            if (context.isPressed(GLFW.GLFW_KEY_ENTER))
            {
                this.acceptChanges();

                return true;
            }
            else if (context.isPressed(GLFW.GLFW_KEY_ESCAPE))
            {
                this.rejectChanges();

                return true;
            }
        }

        return super.subKeyPressed(context);
    }

    @Override
    public void render(UIContext context)
    {
        if (this.editing && this.checker.isTime())
        {
            /* UIContext.mouseX can't be used because when cursor is outside of window
             * its position stops being updated. That's why it has to be queried manually
             * through GLFW...
             *
             * It gets updated outside the window only when one of mouse buttons is
             * being held! */
            GLFW.glfwGetCursorPos(Window.getWindow(), CURSOR_X, CURSOR_Y);

            MinecraftClient mc = MinecraftClient.getInstance();
            int w = mc.getWindow().getWidth();
            int h = mc.getWindow().getHeight();

            double rawX = CURSOR_X[0];
            double rawY = CURSOR_Y[0];
            double fx = Math.ceil(w / (double) context.menu.width);
            double fy = Math.ceil(h / (double) context.menu.height);
            int border = 5;
            int borderPadding = border + 1;

            if (rawX <= border)
            {
                Window.moveCursor(w - borderPadding, (int) mc.mouse.getY());

                this.lastX = context.menu.width - (int) (borderPadding / fx);
                this.checker.mark();
            }
            else if (rawX >= w - border)
            {
                Window.moveCursor(borderPadding, (int) mc.mouse.getY());

                this.lastX = (int) (borderPadding / fx);
                this.checker.mark();
            }
            else if (rawY <= border)
            {
                Window.moveCursor((int) mc.mouse.getX(), h - borderPadding);

                this.lastY = context.menu.height - (int) (borderPadding / fy);
                this.checker.mark();
            }
            else if (rawY >= h - border)
            {
                Window.moveCursor((int) mc.mouse.getX(), borderPadding);

                this.lastY = (int) (borderPadding / fy);
                this.checker.mark();
            }
            else
            {
                int dx = context.mouseX - this.lastX;
                int dy = context.mouseY - this.lastY;
                Vector3f vector = this.getValue();
                boolean all = this.mode == 1 && Window.isCtrlPressed();
                UITrackpad reference = this.mode == 0 ? this.tx : (this.mode == 1 ? this.sx : this.rx);
                float factor = (float) reference.getValueModifier();

                if (this.mode == 0 && this.applyGizmoTranslate(dx, dy, factor))
                {
                    this.setTransform(this.transform);
                }
                else if (this.mode == 1 && this.applyGizmoScale(dx, dy, factor, all))
                {
                    this.setTransform(this.transform);
                }
                else if (this.mode == 2 && !this.freeRotation && this.applyGizmoRotate(context.mouseX, context.mouseY, this.lastX, this.lastY, factor))
                {
                    this.setTransform(this.transform);
                }
                else if (this.local && this.mode == 0)
                {
                    Vector3f local = new Vector3f();

                    if (this.secondaryAxis == null)
                    {
                        double delta = this.axis == Axis.Y ? factor * dy : factor * dx;

                        local.add(this.calculateLocalVector(delta, this.axis));
                    }
                    else
                    {
                        double primaryDelta = factor * dx;
                        double secondaryDelta = factor * dy;

                        local.add(this.calculateLocalVector(primaryDelta, this.axis));
                        local.add(this.calculateLocalVector(secondaryDelta, this.secondaryAxis));
                    }

                    this.setT(null, vector.x + local.x, vector.y + local.y, vector.z + local.z);
                }
                else
                {
                    Vector3f vector3f = new Vector3f(vector);

                    if (this.mode == 2)
                    {
                        vector3f.mul(180F / MathUtils.PI);
                    }

                    if (this.mode == 2 && this.freeRotation)
                    {
                        vector3f.x -= factor * dy;
                        vector3f.y += factor * dx;
                    }
                    else if (this.mode == 0 && this.secondaryAxis != null)
                    {
                        if (this.axis == Axis.X)
                        {
                            vector3f.x += factor * dx;
                        }
                        else if (this.axis == Axis.Y)
                        {
                            vector3f.y += factor * dx;
                        }
                        else if (this.axis == Axis.Z)
                        {
                            vector3f.z += factor * dx;
                        }

                        float secondaryDelta = factor * dy;

                        if (this.secondaryAxis == Axis.X)
                        {
                            vector3f.x += secondaryDelta;
                        }
                        else if (this.secondaryAxis == Axis.Y)
                        {
                            vector3f.y -= secondaryDelta;
                        }
                        else if (this.secondaryAxis == Axis.Z)
                        {
                            vector3f.z -= secondaryDelta;
                        }
                    }
                    else
                    {
                        if (this.mode == 0 && !this.local && this.secondaryAxis == null && !all)
                        {
                            if (this.axis == Axis.X)
                            {
                                vector3f.x += factor * dx;
                            }
                            else if (this.axis == Axis.Y)
                            {
                                vector3f.y -= factor * dy;
                            }
                            else if (this.axis == Axis.Z)
                            {
                                vector3f.z += factor * dx;
                            }
                        }
                        else
                        {
                            if (this.axis == Axis.X || all) vector3f.x += factor * dx;
                            if (this.axis == Axis.Y || all) vector3f.y += factor * dx;
                            if (this.axis == Axis.Z || all) vector3f.z += factor * dx;
                        }
                    }

                    if (this.mode == 0) this.setT(null, vector3f.x, vector3f.y, vector3f.z);
                    if (this.mode == 1) this.setS(null, vector3f.x, vector3f.y, vector3f.z);
                    if (this.mode == 2)
                    {
                        if (this.local && BBSSettings.gizmos.get()) this.setR2(null, vector3f.x, vector3f.y, vector3f.z);
                        else this.setR(null, vector3f.x, vector3f.y, vector3f.z);
                    }
                }

                this.setTransform(this.transform);

                this.lastX = context.mouseX;
                this.lastY = context.mouseY;
            }
        }

        super.render(context);

        if (this.editing)
        {
            String label = UIKeys.TRANSFORMS_EDITING.get();
            FontRenderer font = context.batcher.getFont();
            int x = this.area.mx(font.getWidth(label));
            int y = this.area.my(font.getHeight());

            context.batcher.textCard(label, x, y, Colors.WHITE, BBSSettings.primaryColor(Colors.A50));
        }
    }

    public static class UITransformHandler extends UIElement
    {
        private UIPropTransform transform;

        public UITransformHandler(UIPropTransform transform)
        {
            this.transform = transform;
        }

        @Override
        protected boolean subMouseClicked(UIContext context)
        {
            if (this.transform.editing)
            {
                if (context.mouseButton == 0)
                {
                    this.transform.acceptChanges();

                    return true;
                }
                else if (context.mouseButton == 1)
                {
                    this.transform.rejectChanges();

                    return true;
                }
            }
            
            return super.subMouseClicked(context);
        }

        @Override
        protected boolean subMouseScrolled(UIContext context)
        {
            UITrackpad.updateAmplifier(context);

            return true;
        }
    }

    public void beginGizmoDrag(Gizmo.DragContext context)
    {
        this.gizmoDrag = false;
        this.gizmoInvReady = false;

        if (context == null || !context.ready)
        {
            return;
        }

        this.gizmoViewportX = context.viewportX;
        this.gizmoViewportY = context.viewportY;
        this.gizmoViewportW = context.viewportW;
        this.gizmoViewportH = context.viewportH;

        this.gizmoMvp.set(context.projection).mul(context.modelView);
        this.gizmoMvp.invert(this.gizmoInvMvp);
        this.gizmoInvReady = true;

        if (!this.projectGizmoPoint(0F, 0F, 0F, this.gizmoOrigin2D))
        {
            return;
        }

        if (this.projectGizmoPoint(1F, 0F, 0F, this.gizmoTmp2D))
        {
            this.gizmoAxisX.set(this.gizmoTmp2D).sub(this.gizmoOrigin2D);
            this.gizmoAxisXLenSq = this.gizmoAxisX.lengthSquared();
        }
        else
        {
            this.gizmoAxisXLenSq = 0F;
        }

        if (this.projectGizmoPoint(0F, 1F, 0F, this.gizmoTmp2D))
        {
            this.gizmoAxisY.set(this.gizmoTmp2D).sub(this.gizmoOrigin2D);
            this.gizmoAxisYLenSq = this.gizmoAxisY.lengthSquared();
        }
        else
        {
            this.gizmoAxisYLenSq = 0F;
        }

        if (this.projectGizmoPoint(0F, 0F, 1F, this.gizmoTmp2D))
        {
            this.gizmoAxisZ.set(this.gizmoTmp2D).sub(this.gizmoOrigin2D);
            this.gizmoAxisZLenSq = this.gizmoAxisZ.lengthSquared();
        }
        else
        {
            this.gizmoAxisZLenSq = 0F;
        }

        this.gizmoDrag = true;
    }

    private void clearGizmoDrag()
    {
        this.gizmoDrag = false;
        this.gizmoInvReady = false;
    }

    private boolean applyGizmoTranslate(int dx, int dy, float factor)
    {
        if (!this.gizmoDrag || this.mode != 0)
        {
            return false;
        }

        if (dx == 0 && dy == 0)
        {
            return true;
        }

        Vector3f vector3f = new Vector3f(this.transform.translate);

        if (this.secondaryAxis == null)
        {
            Vector2f axis = this.getGizmoAxis(this.axis);
            float lenSq = this.getGizmoAxisLenSq(this.axis);
            float len = (float) Math.sqrt(lenSq);

            if (len < 1e-3F)
            {
                return false;
            }

            float units = (dx * axis.x + dy * axis.y) / len;
            units *= factor;

            if (this.axis == Axis.X) vector3f.x += units;
            else if (this.axis == Axis.Y) vector3f.y += units;
            else if (this.axis == Axis.Z) vector3f.z -= units;
        }
        else
        {
            Vector2f axisA = this.getGizmoAxis(this.axis);
            Vector2f axisB = this.getGizmoAxis(this.secondaryAxis);
            float lenA = (float) Math.sqrt(this.getGizmoAxisLenSq(this.axis));
            float lenB = (float) Math.sqrt(this.getGizmoAxisLenSq(this.secondaryAxis));

            if (lenA < 1e-3F || lenB < 1e-3F)
            {
                return false;
            }

            float ax = axisA.x / lenA;
            float ay = axisA.y / lenA;
            float bx = axisB.x / lenB;
            float by = axisB.y / lenB;
            float det = ax * by - ay * bx;

            if (Math.abs(det) < 1e-4F)
            {
                return false;
            }

            float da = (dx * by - dy * bx) / det;
            float db = (dy * ax - dx * ay) / det;

            da *= factor;
            db *= factor;

            if (this.axis == Axis.X) vector3f.x += da;
            else if (this.axis == Axis.Y) vector3f.y += da;
            else if (this.axis == Axis.Z) vector3f.z -= da;

            if (this.secondaryAxis == Axis.X) vector3f.x += db;
            else if (this.secondaryAxis == Axis.Y) vector3f.y += db;
            else if (this.secondaryAxis == Axis.Z) vector3f.z -= db;
        }

        this.setT(null, vector3f.x, vector3f.y, vector3f.z);

        return true;
    }

    private boolean applyGizmoRotate(int mouseX, int mouseY, int lastX, int lastY, float factor)
    {
        if (!this.gizmoDrag || !this.gizmoInvReady || this.mode != 2 || this.freeRotation)
        {
            return false;
        }

        if (mouseX == lastX && mouseY == lastY)
        {
            return true;
        }

        Axis axis = this.axis;

        if (!this.intersectGizmoPlane(lastX, lastY, axis, this.gizmoP0) || !this.intersectGizmoPlane(mouseX, mouseY, axis, this.gizmoP1))
        {
            return false;
        }

        if (this.gizmoP0.lengthSquared() < 1e-6 || this.gizmoP1.lengthSquared() < 1e-6)
        {
            return false;
        }

        this.gizmoCross.set(this.gizmoP0).cross(this.gizmoP1);

        double dot = this.gizmoP0.dot(this.gizmoP1);
        double axisDot = this.getGizmoAxisNormal(axis).dot(this.gizmoCross);
        double angleRad = Math.atan2(axisDot, dot);
        float dragSign = this.getAxisDragDirectionSign(axis, this.gizmoP1, mouseX, mouseY, lastX, lastY);

        if (dragSign == 0F)
        {
            return true;
        }

        float deltaDeg = MathUtils.toDeg((float) Math.abs(angleRad)) * factor * dragSign;

        Vector3f current = this.local && BBSSettings.gizmos.get() ? this.transform.rotate2 : this.transform.rotate;
        Vector3f rotDeg = new Vector3f(
            MathUtils.toDeg(current.x),
            MathUtils.toDeg(current.y),
            MathUtils.toDeg(current.z)
        );

        if (axis == Axis.X) rotDeg.x += deltaDeg;
        else if (axis == Axis.Y) rotDeg.y += deltaDeg;
        else if (axis == Axis.Z) rotDeg.z += deltaDeg;

        if (this.local && BBSSettings.gizmos.get()) this.setR2(null, rotDeg.x, rotDeg.y, rotDeg.z);
        else this.setR(null, rotDeg.x, rotDeg.y, rotDeg.z);

        return true;
    }

    private boolean applyGizmoScale(int dx, int dy, float factor, boolean all)
    {
        if (!this.gizmoDrag || this.mode != 1)
        {
            return false;
        }

        if (dx == 0 && dy == 0)
        {
            return true;
        }

        Vector2f axis = this.getGizmoAxis(this.axis);
        float lenSq = this.getGizmoAxisLenSq(this.axis);
        float len = (float) Math.sqrt(lenSq);

        if (len < 1e-3F)
        {
            return false;
        }

        float delta = (dx * axis.x + dy * axis.y) / len;
        delta *= factor;

        Vector3f vector3f = new Vector3f(this.transform.scale);

        if (all)
        {
            vector3f.x += delta;
            vector3f.y += delta;
            vector3f.z += delta;
        }
        else
        {
            if (this.axis == Axis.X) vector3f.x += delta;
            else if (this.axis == Axis.Y) vector3f.y += delta;
            else if (this.axis == Axis.Z) vector3f.z += delta;
        }

        this.setS(null, vector3f.x, vector3f.y, vector3f.z);

        return true;
    }

    private Vector2f getGizmoAxis(Axis axis)
    {
        if (axis == Axis.X) return this.gizmoAxisX;
        if (axis == Axis.Y) return this.gizmoAxisY;
        return this.gizmoAxisZ;
    }

    private float getGizmoAxisLenSq(Axis axis)
    {
        if (axis == Axis.X) return this.gizmoAxisXLenSq;
        if (axis == Axis.Y) return this.gizmoAxisYLenSq;
        return this.gizmoAxisZLenSq;
    }

    private Vector3d getGizmoAxisNormal(Axis axis)
    {
        if (axis == Axis.X) return this.gizmoAxisNormal.set(1, 0, 0);
        if (axis == Axis.Y) return this.gizmoAxisNormal.set(0, 1, 0);
        return this.gizmoAxisNormal.set(0, 0, 1);
    }

    private float getAxisDragDirectionSign(Axis axis, Vector3d pointOnRing, int mouseX, int mouseY, int lastX, int lastY)
    {
        this.gizmoTangent.set(this.getGizmoAxisNormal(axis)).cross(pointOnRing);

        if (this.gizmoTangent.lengthSquared() < 1e-8)
        {
            return 0F;
        }

        this.gizmoTangent.normalize().mul(0.25D);

        if (!this.projectGizmoPoint((float) pointOnRing.x, (float) pointOnRing.y, (float) pointOnRing.z, this.gizmoP2D))
        {
            return 0F;
        }

        if (!this.projectGizmoPoint(
            (float) (pointOnRing.x + this.gizmoTangent.x),
            (float) (pointOnRing.y + this.gizmoTangent.y),
            (float) (pointOnRing.z + this.gizmoTangent.z),
            this.gizmoP2DNext
        ))
        {
            return 0F;
        }

        float tangentX = this.gizmoP2DNext.x - this.gizmoP2D.x;
        float tangentY = this.gizmoP2D.y - this.gizmoP2DNext.y;
        float dragX = mouseX - lastX;
        float dragY = lastY - mouseY;
        float alignment = dragX * tangentX + dragY * tangentY;

        if (Math.abs(alignment) < 1e-6F)
        {
            return 0F;
        }

        float sign = Math.signum(alignment);

        /* This editor view applies an extra base yaw, so X/Z ring drag appears mirrored
         * compared to mathematical axis direction. Correct that here to keep ring drag
         * intuitive (CCW drag -> CCW visual rotation) on red/blue rings. */
        if (axis == Axis.X || axis == Axis.Z)
        {
            sign = -sign;
        }

        return sign;
    }

    private boolean intersectGizmoPlane(int mouseX, int mouseY, Axis axis, Vector3d out)
    {
        if (this.gizmoViewportW <= 0 || this.gizmoViewportH <= 0)
        {
            return false;
        }

        float ndcX = (mouseX - this.gizmoViewportX) / (float) this.gizmoViewportW * 2F - 1F;
        float ndcY = 1F - (mouseY - this.gizmoViewportY) / (float) this.gizmoViewportH * 2F;

        this.gizmoNear4D.set(ndcX, ndcY, -1F, 1F);
        this.gizmoFar4D.set(ndcX, ndcY, 1F, 1F);

        this.gizmoInvMvp.transform(this.gizmoNear4D);
        this.gizmoInvMvp.transform(this.gizmoFar4D);

        if (this.gizmoNear4D.w == 0F || this.gizmoFar4D.w == 0F)
        {
            return false;
        }

        this.gizmoNear4D.div(this.gizmoNear4D.w);
        this.gizmoFar4D.div(this.gizmoFar4D.w);

        this.gizmoRayStart.set(this.gizmoNear4D.x, this.gizmoNear4D.y, this.gizmoNear4D.z);
        this.gizmoRayEnd.set(this.gizmoFar4D.x, this.gizmoFar4D.y, this.gizmoFar4D.z);

        double a = 0;
        double b = 0;
        double c = 0;

        if (axis == Axis.X) a = 1;
        else if (axis == Axis.Y) b = 1;
        else if (axis == Axis.Z) c = 1;

        return Intersectiond.intersectLineSegmentPlane(
            this.gizmoRayStart.x, this.gizmoRayStart.y, this.gizmoRayStart.z,
            this.gizmoRayEnd.x, this.gizmoRayEnd.y, this.gizmoRayEnd.z,
            a, b, c, 0, out
        );
    }

    private boolean projectGizmoPoint(float x, float y, float z, Vector2f out)
    {
        if (this.gizmoViewportW <= 0 || this.gizmoViewportH <= 0)
        {
            return false;
        }

        this.gizmoTmp4D.set(x, y, z, 1F);
        this.gizmoMvp.transform(this.gizmoTmp4D);

        if (this.gizmoTmp4D.w == 0F)
        {
            return false;
        }

        float ndcX = this.gizmoTmp4D.x / this.gizmoTmp4D.w;
        float ndcY = this.gizmoTmp4D.y / this.gizmoTmp4D.w;

        out.x = this.gizmoViewportX + (ndcX * 0.5F + 0.5F) * this.gizmoViewportW;
        out.y = this.gizmoViewportY + (1F - (ndcY * 0.5F + 0.5F)) * this.gizmoViewportH;

        return true;
    }
}
