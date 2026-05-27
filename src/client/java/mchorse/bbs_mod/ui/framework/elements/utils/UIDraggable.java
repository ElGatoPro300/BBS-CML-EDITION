package mchorse.bbs_mod.ui.framework.elements.utils;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.utils.Scroll;
import mchorse.bbs_mod.utils.colors.Colors;

import org.joml.Vector2i;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class UIDraggable extends UIElement
{
    private Consumer<UIContext> callback;
    private Consumer<UIContext> render;
    private Supplier<Vector2i> reference;
    private Runnable dragEndCallback;
    private boolean dragging;
    private boolean hover;

    private int mouseX;
    private int mouseY;
    private Vector2i referenceMouse;
    private boolean dragUpdated;

    private int threshold;
    private boolean thresholdMet;

    public UIDraggable(Consumer<UIContext> callback)
    {
        this.callback = callback;
    }

    public UIDraggable hoverOnly()
    {
        this.hover = true;

        return this;
    }

    public UIDraggable rendering(Consumer<UIContext> render)
    {
        this.render = render;

        return this;
    }

    public UIDraggable reference(Supplier<Vector2i> reference)
    {
        this.reference = reference;

        return this;
    }

    public UIDraggable dragEnd(Runnable callback)
    {
        this.dragEndCallback = callback;

        return this;
    }

    /* Movement threshold (in pixels) before the drag callback starts firing.
       Lets short clicks on the handle be treated as no-op instead of triggering
       a single-frame drag (e.g. unintentionally undocking a panel). */
    public UIDraggable threshold(int threshold)
    {
        this.threshold = threshold;

        return this;
    }

    public boolean isDragging()
    {
        return this.dragging;
    }

    /* True only once the mouse has moved past the configured threshold (or
       immediately if no threshold was set). Useful for distinguishing a real
       drag from a stationary click on the handle. */
    public boolean isActivelyDragging()
    {
        return this.dragging && this.thresholdMet;
    }

    public void updateDrag(UIContext context)
    {
        if (this.dragging && this.callback != null)
        {
            if (!this.thresholdMet)
            {
                int dx = context.mouseX - this.mouseX;
                int dy = context.mouseY - this.mouseY;

                if (Math.abs(dx) < this.threshold && Math.abs(dy) < this.threshold)
                {
                    this.dragUpdated = true;
                    return;
                }

                this.thresholdMet = true;
            }

            int mouseX = context.mouseX;
            int mouseY = context.mouseY;

            if (this.referenceMouse != null)
            {
                context.mouseX = this.referenceMouse.x + (mouseX - this.mouseX);
                context.mouseY = this.referenceMouse.y + (mouseY - this.mouseY);
            }

            this.callback.accept(context);

            context.mouseX = mouseX;
            context.mouseY = mouseY;
            this.dragUpdated = true;
        }
    }

    @Override
    protected boolean subMouseClicked(UIContext context)
    {
        if (this.area.isInside(context) && context.mouseButton == 0)
        {
            this.mouseX = context.mouseX;
            this.mouseY = context.mouseY;
            this.dragging = true;
            this.thresholdMet = this.threshold <= 0;

            if (this.reference != null)
            {
                this.referenceMouse = this.reference.get();
            }

            return true;
        }

        return super.subMouseClicked(context);
    }

    @Override
    protected boolean subMouseReleased(UIContext context)
    {
        boolean wasDragging = this.dragging;
        boolean fireEnd = wasDragging && this.thresholdMet;

        this.dragging = false;
        this.thresholdMet = false;

        if (fireEnd && this.dragEndCallback != null)
        {
            this.dragEndCallback.run();
        }

        return super.subMouseReleased(context);
    }

    @Override
    public void render(UIContext context)
    {
        super.render(context);

        if (!this.hover || this.area.isInside(context) || this.dragging)
        {
            if (this.render != null)
            {
                this.render.accept(context);
            }
            else
            {
                Scroll.bar(context.batcher, this.area.x, this.area.y, this.area.ex(), this.area.ey(), Colors.A50);
            }
        }

        if (this.dragging && this.callback != null && !this.dragUpdated)
        {
            this.updateDrag(context);
        }
        this.dragUpdated = false;
    }
}