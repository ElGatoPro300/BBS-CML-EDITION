package mchorse.bbs_mod.ui.framework.elements.input;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.IValueNotifier;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.Gizmo;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;
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
import org.joml.Vector3d;
import org.joml.Vector3f;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class UIPropTransform extends UITransform
{
    public interface IGizmoRayProvider
    {
        public boolean getMouseRay(UIContext context, int mouseX, int mouseY, Vector3d rayOrigin, Vector3f rayDirection);
        public boolean getGizmoMatrix(Matrix4f matrix);
    }

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
    private boolean freeTranslation;
    private boolean rayDragInitialized;

    private IGizmoRayProvider gizmoRayProvider;
    private final Vector3d rayOrigin = new Vector3d();
    private final Vector3f rayDirection = new Vector3f();
    private final Matrix4f rayGizmoMatrix = new Matrix4f();
    private final Vector3f rayPrimaryAxis = new Vector3f();
    private final Vector3f raySecondaryAxis = new Vector3f();
    private final Vector3f rayPlaneNormal = new Vector3f();
    private final Vector3d rayLastPoint = new Vector3d();
    private final Vector3d rayCurrentPoint = new Vector3d();
    private final Vector3f rayGizmoOrigin = new Vector3f();
    private double rayLastAxisValue;
    private final Vector3d planeOrigin = new Vector3d();
    private final Vector3d planeNormal = new Vector3d();
    private final Vector3d rayDirectionD = new Vector3d();

    private UITransformHandler handler;
    private boolean wasWrapped;
    private float translationScale = 1F;

    public UIPropTransform()
    {
        this.handler = new UITransformHandler(this);

        this.iconT.callback = (b) -> this.toggleLocal();
        this.iconT.hoverColor = Colors.LIGHTEST_GRAY;
        this.iconT.setEnabled(true);
        this.iconT.tooltip(this.local ? UIKeys.TRANSFORMS_CONTEXT_SWITCH_GLOBAL : UIKeys.TRANSFORMS_CONTEXT_SWITCH_LOCAL);

        this.noCulling();
    }

    @Override
    protected void addGeneralTabActions(ContextMenuManager menu, ListType transforms)
    {
        menu.action(
            this.local ? Icons.FULLSCREEN : Icons.MINIMIZE,
            this.local ? UIKeys.TRANSFORMS_CONTEXT_SWITCH_GLOBAL : UIKeys.TRANSFORMS_CONTEXT_SWITCH_LOCAL,
            this::toggleLocal
        );

        for (BiConsumer<UIPropTransform, ContextMenuManager> consumer : contextMenuExtensions)
        {
            consumer.accept(this, menu);
        }
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
        if (pre != null)
        {
            Runnable existing = this.preCallback;
            this.preCallback = existing == null ? pre : () ->
            {
                existing.run();
                pre.run();
            };
        }

        if (post != null)
        {
            Runnable existing = this.postCallback;
            this.postCallback = existing == null ? post : () ->
            {
                existing.run();
                post.run();
            };
        }

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

    public void setGizmoRayProvider(IGizmoRayProvider provider)
    {
        this.gizmoRayProvider = provider;
    }

    public UIPropTransform translationScale(float translationScale)
    {
        this.translationScale = translationScale;
        return this;
    }

    public void enableMode(int mode)
    {
        this.enableMode(mode, null);
    }

    public void enableMode(int mode, Axis axis)
    {
        if (Gizmo.INSTANCE.setMode(Gizmo.Mode.values()[mode]) && axis == null)
        {
            return;
        }

        UIContext context = this.getContext();

        if (context == null)
        {
            return;
        }

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
        this.rayDragInitialized = false;

        this.cache.copy(this.transform);

        if (!this.handler.hasParent())
        {
            context.menu.overlay.add(this.handler);
        }

        this.initializeRayDrag(context);
    }

    public void enablePlaneMode(int mode, Axis primary, Axis secondary)
    {
        if (Gizmo.INSTANCE.setMode(Gizmo.Mode.values()[mode]) && primary == null)
        {
            return;
        }

        UIContext context = this.getContext();

        if (context == null)
        {
            return;
        }

        this.axis = primary == null ? Axis.X : primary;
        this.secondaryAxis = secondary;
        this.freeRotation = false;
        this.lastX = context.mouseX;
        this.lastY = context.mouseY;

        this.editing = true;
        this.mode = mode;
        this.rayDragInitialized = false;

        this.cache.copy(this.transform);

        if (!this.handler.hasParent())
        {
            context.menu.overlay.add(this.handler);
        }

        this.initializeRayDrag(context);
    }

    public void enableFreeRotation(int mode, Axis marker)
    {
        if (Gizmo.INSTANCE.setMode(Gizmo.Mode.values()[mode]) && marker == null)
        {
            return;
        }

        UIContext context = this.getContext();

        if (context == null)
        {
            return;
        }

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
        this.rayDragInitialized = false;

        this.cache.copy(this.transform);

        if (!this.handler.hasParent())
        {
            context.menu.overlay.add(this.handler);
        }

        this.initializeRayDrag(context);
    }

    public void enableFreeTranslation(int mode)
    {
        UIContext context = this.getContext();

        if (context == null)
        {
            return;
        }

        this.axis = Axis.X;
        this.secondaryAxis = null;
        this.freeRotation = false;
        this.freeTranslation = true;
        this.lastX = context.mouseX;
        this.lastY = context.mouseY;

        this.editing = true;
        this.mode = mode;
        this.rayDragInitialized = false;

        this.cache.copy(this.transform);

        if (!this.handler.hasParent())
        {
            context.menu.overlay.add(this.handler);
        }

        this.initializeRayDrag(context);
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
        this.freeTranslation = false;
        this.rayDragInitialized = false;

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

            boolean shouldWrap = this.mode == 0;
            boolean didWrap = false;

            if (shouldWrap && rawX <= border)
            {
                Window.moveCursor(w - borderPadding, (int) mc.mouse.getY());

                this.lastX = context.menu.width - (int) (borderPadding / fx);
                this.checker.mark();
                didWrap = true;
            }
            else if (shouldWrap && rawX >= w - border)
            {
                Window.moveCursor(borderPadding, (int) mc.mouse.getY());

                this.lastX = (int) (borderPadding / fx);
                this.checker.mark();
                didWrap = true;
            }
            else if (shouldWrap && rawY <= border)
            {
                Window.moveCursor((int) mc.mouse.getX(), h - borderPadding);

                this.lastY = context.menu.height - (int) (borderPadding / fy);
                this.checker.mark();
                didWrap = true;
            }
            else if (shouldWrap && rawY >= h - border)
            {
                Window.moveCursor((int) mc.mouse.getX(), borderPadding);

                this.lastY = (int) (borderPadding / fy);
                this.checker.mark();
                didWrap = true;
            }

            if (didWrap)
            {
                this.wasWrapped = true;
            }

            boolean handledByRayDrag = this.applyRayDrag(context);

                if (!handledByRayDrag)
                {
                    int dx = context.mouseX - this.lastX;
                    int dy = context.mouseY - this.lastY;
                    Vector3f vector = this.getValue();
                    boolean all = this.mode == 1 && Window.isCtrlPressed();
                    UITrackpad reference = this.mode == 0 ? this.tx : (this.mode == 1 ? this.sx : this.rx);
                    float factor = (float) reference.getValueModifier();

                    if (this.local && this.mode == 0)
                    {
                        Vector3f local = new Vector3f();

                        if (this.secondaryAxis == null)
                        {
                            double delta;

                            if (this.axis == Axis.Y)
                            {
                                if (!Gizmo.INSTANCE.isDragging())
                                {
                                    delta = factor * dx;
                                }
                                else
                                {
                                    delta = factor * dy;
                                }
                            }
                            else
                            {
                                delta = factor * dx;
                            }

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
                                    if (!Gizmo.INSTANCE.isDragging())
                                    {
                                        vector3f.y += factor * dx;
                                    }
                                    else
                                    {
                                        vector3f.y -= factor * dy;
                                    }
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
                }

                this.setTransform(this.transform);

                if (!didWrap)
                {
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

    private boolean initializeRayDrag(UIContext context)
    {
        if (!Gizmo.INSTANCE.isDragging())
        {
            this.rayDragInitialized = false;
            return false;
        }

        if (this.gizmoRayProvider == null || context == null)
        {
            this.rayDragInitialized = false;
            return false;
        }

        if (this.mode == 1 || (this.mode == 2 && this.freeRotation))
        {
            this.rayDragInitialized = false;
            return false;
        }

        if (!this.gizmoRayProvider.getGizmoMatrix(this.rayGizmoMatrix))
        {
            this.rayDragInitialized = false;
            return false;
        }

        if (!this.gizmoRayProvider.getMouseRay(context, context.mouseX, context.mouseY, this.rayOrigin, this.rayDirection))
        {
            this.rayDragInitialized = false;
            return false;
        }

        this.rayGizmoMatrix.getTranslation(this.rayGizmoOrigin);
        this.extractAxisWorld(this.axis, this.rayPrimaryAxis);

        if (this.mode == 0 && this.freeTranslation)
        {
            this.rayPlaneNormal.set(this.rayDirection);

            if (!this.normalizeSafe(this.rayPlaneNormal))
            {
                this.rayDragInitialized = false;
                return false;
            }
        }
        else if (this.mode == 0 && this.secondaryAxis != null)
        {
            this.extractAxisWorld(this.secondaryAxis, this.raySecondaryAxis);
            this.rayPlaneNormal.set(this.rayPrimaryAxis).cross(this.raySecondaryAxis);

            if (!this.normalizeSafe(this.rayPlaneNormal))
            {
                this.rayDragInitialized = false;
                return false;
            }
        }
        else if (this.mode == 0)
        {
            double axisValue = this.computeAxisValue(this.rayOrigin, this.rayDirection, this.rayPrimaryAxis);

            if (!Double.isFinite(axisValue))
            {
                this.rayDragInitialized = false;
                return false;
            }

            this.rayLastAxisValue = axisValue;
        }
        else if (this.mode == 2)
        {
            this.rayPlaneNormal.set(this.rayPrimaryAxis);

            if (!this.normalizeSafe(this.rayPlaneNormal))
            {
                this.rayDragInitialized = false;
                return false;
            }
        }
        else
        {
            this.rayDragInitialized = false;
            return false;
        }

        if ((this.mode != 0 || this.secondaryAxis != null || this.freeTranslation) && !this.intersectCurrentRay(this.rayLastPoint))
        {
            this.rayDragInitialized = false;
            return false;
        }

        this.rayDragInitialized = true;

        return true;
    }

    private boolean applyRayDrag(UIContext context)
    {
        if (!Gizmo.INSTANCE.isDragging())
        {
            return false;
        }

        if (this.gizmoRayProvider == null || context == null)
        {
            return false;
        }

        if (!this.rayDragInitialized && !this.initializeRayDrag(context))
        {
            return false;
        }

        if (!this.gizmoRayProvider.getMouseRay(context, context.mouseX, context.mouseY, this.rayOrigin, this.rayDirection))
        {
            return false;
        }

        if (this.wasWrapped)
        {
            if (this.mode == 0)
            {
                if (this.freeTranslation || this.secondaryAxis != null)
                {
                    if (this.intersectCurrentRay(this.rayLastPoint))
                    {
                        this.wasWrapped = false;
                    }
                }
                else
                {
                    double axisValue = this.computeAxisValue(this.rayOrigin, this.rayDirection, this.rayPrimaryAxis);
                    if (Double.isFinite(axisValue))
                    {
                        this.rayLastAxisValue = axisValue;
                        this.wasWrapped = false;
                    }
                }
            }
        }

        if (this.mode == 0)
        {
            if (this.freeTranslation)
            {
                if (!this.intersectCurrentRay(this.rayCurrentPoint))
                {
                    return false;
                }

                Vector3d delta = new Vector3d(this.rayCurrentPoint).sub(this.rayLastPoint);

                if (delta.lengthSquared() <= 1.0E-12D)
                {
                    return true;
                }

                Vector3f value = this.getValue();

                Vector3f worldX = new Vector3f();
                Vector3f worldY = new Vector3f();
                Vector3f worldZ = new Vector3f();
                this.extractAxisWorld(Axis.X, worldX);
                this.extractAxisWorld(Axis.Y, worldY);
                this.extractAxisWorld(Axis.Z, worldZ);

                float dx_world = (float) delta.dot(worldX.x, worldX.y, worldX.z) * this.translationScale;
                float dy_world = (float) delta.dot(worldY.x, worldY.y, worldY.z) * this.translationScale;
                float dz_world = (float) delta.dot(worldZ.x, worldZ.y, worldZ.z) * this.translationScale;

                if (this.local)
                {
                    Vector3f result = new Vector3f();

                    result.add(this.calculateLocalVector(dx_world, Axis.X));
                    result.add(this.calculateLocalVector(dy_world, Axis.Y));
                    result.add(this.calculateLocalVector(dz_world, Axis.Z));

                    this.setT(null, value.x + result.x, value.y + result.y, value.z + result.z);
                }
                else
                {
                    Vector3f result = new Vector3f(value);

                    this.addAxisDelta(result, Axis.X, dx_world);
                    this.addAxisDelta(result, Axis.Y, dy_world);
                    this.addAxisDelta(result, Axis.Z, dz_world);

                    this.setT(null, result.x, result.y, result.z);
                }

                this.rayLastPoint.set(this.rayCurrentPoint);
            }
            else if (this.secondaryAxis == null)
            {
                double axisValue = this.computeAxisValue(this.rayOrigin, this.rayDirection, this.rayPrimaryAxis);

                if (!Double.isFinite(axisValue))
                {
                    return false;
                }

                float primaryDelta = (float) (axisValue - this.rayLastAxisValue) * this.translationScale;

                if (Math.abs(primaryDelta) <= 1.0E-8F)
                {
                    return true;
                }

                Vector3f value = this.getValue();

                if (this.local)
                {
                    Vector3f result = this.calculateLocalVector(primaryDelta, this.axis);

                    this.setT(null, value.x + result.x, value.y + result.y, value.z + result.z);
                }
                else
                {
                    Vector3f result = new Vector3f(value);

                    this.addAxisDelta(result, this.axis, primaryDelta);
                    this.setT(null, result.x, result.y, result.z);
                }

                this.rayLastAxisValue = axisValue;
            }
            else
            {
                if (!this.intersectCurrentRay(this.rayCurrentPoint))
                {
                    return false;
                }

                Vector3d delta = new Vector3d(this.rayCurrentPoint).sub(this.rayLastPoint);

                if (delta.lengthSquared() <= 1.0E-12D)
                {
                    return true;
                }

                float primaryDelta = (float) delta.dot(this.rayPrimaryAxis.x, this.rayPrimaryAxis.y, this.rayPrimaryAxis.z) * this.translationScale;
                Vector3f value = this.getValue();

                if (this.local)
                {
                    Vector3f result = new Vector3f();

                    result.add(this.calculateLocalVector(primaryDelta, this.axis));

                    if (this.secondaryAxis != null)
                    {
                        float secondaryDelta = (float) delta.dot(this.raySecondaryAxis.x, this.raySecondaryAxis.y, this.raySecondaryAxis.z) * this.translationScale;

                        result.add(this.calculateLocalVector(secondaryDelta, this.secondaryAxis));
                    }

                    this.setT(null, value.x + result.x, value.y + result.y, value.z + result.z);
                }
                else
                {
                    Vector3f result = new Vector3f(value);

                    this.addAxisDelta(result, this.axis, primaryDelta);

                    float secondaryDelta = (float) delta.dot(this.raySecondaryAxis.x, this.raySecondaryAxis.y, this.raySecondaryAxis.z) * this.translationScale;
                    this.addAxisDelta(result, this.secondaryAxis, secondaryDelta);

                    this.setT(null, result.x, result.y, result.z);
                }

                this.rayLastPoint.set(this.rayCurrentPoint);
            }
        }
        else if (this.mode == 2 && !this.freeRotation)
        {
            if (!this.intersectCurrentRay(this.rayCurrentPoint))
            {
                return false;
            }

            Vector3f from = new Vector3f(
                (float) (this.rayLastPoint.x - this.rayGizmoOrigin.x),
                (float) (this.rayLastPoint.y - this.rayGizmoOrigin.y),
                (float) (this.rayLastPoint.z - this.rayGizmoOrigin.z)
            );
            Vector3f to = new Vector3f(
                (float) (this.rayCurrentPoint.x - this.rayGizmoOrigin.x),
                (float) (this.rayCurrentPoint.y - this.rayGizmoOrigin.y),
                (float) (this.rayCurrentPoint.z - this.rayGizmoOrigin.z)
            );

            if (!this.normalizeSafe(from) || !this.normalizeSafe(to))
            {
                return false;
            }

            Vector3f cross = new Vector3f(from).cross(to);
            float sin = this.rayPlaneNormal.dot(cross);
            float cos = from.dot(to);
            float angle = (float) Math.toDegrees(Math.atan2(sin, cos));

            Vector3f value = new Vector3f(this.getValue()).mul(180F / MathUtils.PI);
            this.addAxisDelta(value, this.axis, angle);

            if (this.local && BBSSettings.gizmos.get())
            {
                this.setR2(null, value.x, value.y, value.z);
            }
            else
            {
                this.setR(null, value.x, value.y, value.z);
            }

            this.rayLastPoint.set(this.rayCurrentPoint);
        }
        else
        {
            return false;
        }

        return true;
    }

    private boolean intersectCurrentRay(Vector3d out)
    {
        this.planeOrigin.set(this.rayGizmoOrigin.x, this.rayGizmoOrigin.y, this.rayGizmoOrigin.z);
        this.planeNormal.set(this.rayPlaneNormal.x, this.rayPlaneNormal.y, this.rayPlaneNormal.z);
        this.rayDirectionD.set(this.rayDirection.x, this.rayDirection.y, this.rayDirection.z);

        if (this.planeNormal.dot(this.rayDirectionD) > 0)
        {
            this.planeNormal.negate();
        }

        double distance = Intersectiond.intersectRayPlane(this.rayOrigin, this.rayDirectionD, this.planeOrigin, this.planeNormal, 1.0E-6D);

        if (!Double.isFinite(distance) || distance < 0D)
        {
            return false;
        }

        out.set(this.rayOrigin).fma(distance, this.rayDirectionD);

        return true;
    }

    private void extractAxisWorld(Axis axis, Vector3f out)
    {
        if (axis == Axis.X)
        {
            out.set(1F, 0F, 0F);
        }
        else if (axis == Axis.Y)
        {
            out.set(0F, 1F, 0F);
        }
        else
        {
            out.set(0F, 0F, 1F);
        }

        this.rayGizmoMatrix.transformDirection(out);
        this.normalizeSafe(out);
    }

    private boolean normalizeSafe(Vector3f vector)
    {
        float lengthSquared = vector.lengthSquared();

        if (lengthSquared <= 1.0E-12F)
        {
            return false;
        }

        vector.mul((float) (1D / Math.sqrt(lengthSquared)));

        return true;
    }

    private void addAxisDelta(Vector3f vector, Axis axis, float delta)
    {
        if (axis == Axis.X)
        {
            vector.x += delta;
        }
        else if (axis == Axis.Y)
        {
            vector.y += delta;
        }
        else
        {
            vector.z += delta;
        }
    }

    private double computeAxisValue(Vector3d origin, Vector3f direction, Vector3f axisDirection)
    {
        double ux = direction.x;
        double uy = direction.y;
        double uz = direction.z;
        double vx = axisDirection.x;
        double vy = axisDirection.y;
        double vz = axisDirection.z;

        double wx = origin.x - this.rayGizmoOrigin.x;
        double wy = origin.y - this.rayGizmoOrigin.y;
        double wz = origin.z - this.rayGizmoOrigin.z;

        double b = ux * vx + uy * vy + uz * vz;
        double d = ux * wx + uy * wy + uz * wz;
        double e = vx * wx + vy * wy + vz * wz;
        double denom = 1D - b * b;

        if (Math.abs(denom) <= 1.0E-8D)
        {
            return e;
        }

        return (e - b * d) / denom;
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
}
