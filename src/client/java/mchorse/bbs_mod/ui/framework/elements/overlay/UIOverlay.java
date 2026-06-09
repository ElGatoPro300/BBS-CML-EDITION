package mchorse.bbs_mod.ui.framework.elements.overlay;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.EventPropagation;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.resizers.Flex;
import mchorse.bbs_mod.utils.colors.Colors;

import org.joml.Vector2i;

import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class UIOverlay extends UIElement
{
    private static final Map<String, Vector2i> offsets = new HashMap<>();
    private static final Map<String, Vector2i> sizes = new HashMap<>();

    private int background = Colors.A50;
    private float openTransition = 0.0F;
    private boolean closing = false;

    public float getOpenTransition()
    {
        return this.openTransition;
    }

    public static UIOverlay addOverlay(UIContext context, UIOverlayPanel panel)
    {
        UIOverlay overlay = new UIOverlay();

        panel.relative(overlay).xy(0.5F, 0.5F).wh(0.5F, 0.5F).anchor(0.5F).bounds(overlay, 0);
        setupPanel(context, overlay, panel);

        return overlay;
    }

    public static UIOverlay addOverlay(UIContext context, UIOverlayPanel panel, float w, float h)
    {
        UIOverlay overlay = new UIOverlay();

        panel.relative(overlay).xy(0.5F, 0.5F).wh(w, h).anchor(0.5F).bounds(overlay, 0);
        setupPanel(context, overlay, panel);

        return overlay;
    }

    public static UIOverlay addOverlay(UIContext context, UIOverlayPanel panel, int w, int h)
    {
        UIOverlay overlay = new UIOverlay();

        panel.relative(overlay).xy(0.5F, 0.5F).wh(w, h).anchor(0.5F).bounds(overlay, 0);
        setupPanel(context, overlay, panel);

        return overlay;
    }

    public static UIOverlay addOverlay(UIContext context, UIOverlayPanel panel, int w, float h)
    {
        UIOverlay overlay = new UIOverlay();

        panel.relative(overlay).xy(0.5F, 0.5F).w(w).h(h).anchor(0.5F).bounds(overlay, 0);
        setupPanel(context, overlay, panel);

        return overlay;
    }

    public static UIOverlay addOverlayLeft(UIContext context, UIOverlayPanel panel, int w)
    {
        return addOverlayLeft(context, panel, w, 10);
    }

    public static UIOverlay addOverlayLeft(UIContext context, UIOverlayPanel panel, int w, int padding)
    {
        UIOverlay overlay = new UIOverlay();

        panel.relative(overlay).x(padding).y(padding).w(w).h(1F, -padding * 2).anchor(0F, 0F).bounds(overlay, 0);
        setupPanel(context, overlay, panel);

        return overlay;
    }

    public static UIOverlay addOverlayRight(UIContext context, UIOverlayPanel panel, int w)
    {
        return addOverlayRight(context, panel, w, 10);
    }

    public static UIOverlay addOverlayRight(UIContext context, UIOverlayPanel panel, int w, int padding)
    {
        UIOverlay overlay = new UIOverlay();

        panel.relative(overlay).x(1F, -padding).y(padding).w(w).h(1F, -padding * 2).anchor(1F, 0F).bounds(overlay, 0);
        setupPanel(context, overlay, panel);

        return overlay;
    }

    public static void setupPanel(UIContext context, UIOverlay overlay, UIOverlayPanel panel)
    {
        if (panel.hasParent())
        {
            return;
        }

        Flex flex = panel.getFlex();
        String key = panel.getClass().getSimpleName();
        Vector2i offset = offsets.get(panel.getClass().getSimpleName());
        Vector2i size = sizes.get(key);

        panel.setInitialOffset(flex.x.offset, flex.y.offset);
        panel.setInitialSizeOffset(flex.w.offset, flex.h.offset);

        if (offset != null)
        {
            flex.x.offset = offset.x;
            flex.y.offset = offset.y;
        }

        if (size != null)
        {
            flex.w.offset = size.x;
            flex.h.offset = size.y;
        }

        overlay.full(context.menu.overlay);
        context.menu.overlay.add(overlay);
        overlay.add(panel);
        context.menu.overlay.resize();
    }

    public static boolean has(UIContext context)
    {
        return !context.menu.getRoot().getChildren(UIOverlayPanel.class).isEmpty();
    }

    public UIOverlay()
    {
        this.eventPropagataion(EventPropagation.BLOCK).markContainer();
    }

    public UIOverlay background(int background)
    {
        this.background = background;

        return this;
    }

    public UIOverlay noBackground()
    {
        return this.background(0);
    }

    public void closeItself()
    {
        if (this.closing)
        {
            return;
        }

        UIUtils.playClick();

        if (BBSSettings.editorSimplifyAnimations.get())
        {
            this.openTransition = 0.0F;
            this.closing = true;
            this.performClose();
        }
        else
        {
            this.closing = true;
        }
    }

    private void performClose()
    {
        this.removeFromParent();

        for (UIOverlayPanel element : this.getChildren(UIOverlayPanel.class))
        {
            element.removeFromParent();
            element.onClose();

            /* Save offset */
            Vector2i offset = new Vector2i(element.getFlex().x.offset, element.getFlex().y.offset);
            Vector2i size = new Vector2i(element.getFlex().w.offset, element.getFlex().h.offset);
            String key = element.getClass().getSimpleName();

            offsets.put(key, offset);
            sizes.put(key, size);
        }
    }

    /* Don't pass user input down the line... */

    @Override
    protected boolean subMouseClicked(UIContext context)
    {
        this.closeItself();

        return super.subMouseClicked(context);
    }

    @Override
    public void render(UIContext context)
    {
        float target = this.closing ? 0.0F : 1.0F;

        if (BBSSettings.editorSimplifyAnimations.get())
        {
            this.openTransition = target;
        }
        else
        {
            if (this.openTransition < 0.001F && !this.closing)
            {
                /* Fast start on first frame */
                this.openTransition = 0.01F;
            }
            this.openTransition += (target - this.openTransition) * 0.2F;
        }

        if (this.closing && this.openTransition <= 0.01F)
        {
            this.performClose();
            return;
        }

        if (Colors.getA(this.background) > 0F)
        {
            int alpha = (int) (Colors.getA(this.background) * this.openTransition * 255);
            int finalBgColor = Colors.setA(this.background, alpha / 255.0F);
            this.area.render(context.batcher, finalBgColor);
        }

        super.render(context);
    }
}
