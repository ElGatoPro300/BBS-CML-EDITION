package mchorse.bbs_mod.ui.aprilfools;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.UIText;
import mchorse.bbs_mod.utils.colors.Colors;

public class UIFixBBSOverlayPanel extends UIOverlayPanel
{
    private UIButton noButton;
    private boolean noFalling = false;
    private long noFallStart = 0L;
    private Runnable onAccept;

    public UIFixBBSOverlayPanel(Runnable onAccept)
    {
        super(IKey.raw("FIX YOUR BBS NOW"));

        this.onAccept = onAccept;

        /* No escape — hide close button */
        this.icons.setVisible(false);

        UIText message = new UIText(IKey.raw("Your BBS requires immediate attention.\nThis is not optional."));
        message.color(Colors.WHITE, true).lineHeight(12);
        message.relative(this.content).x(6).y(6).w(1F, -12).h(1F, -44);

        UIButton accept = new UIButton(IKey.raw("ACCEPT"), (b) -> this.handleAccept());
        this.noButton = new UIButton(IKey.raw("NO"), (b) -> this.handleNo());

        accept.relative(this.content).x(6).y(1F, -28).w(0.5F, -10).h(20);
        this.noButton.relative(this.content).x(0.5F, 4).y(1F, -28).w(0.5F, -10).h(20);

        this.content.add(message, accept, this.noButton);
    }

    private void handleAccept()
    {
        /* Remove blocking container from the overlay */
        if (this.getParent() != null)
        {
            this.getParent().removeFromParent();
        }

        if (this.onAccept != null)
        {
            this.onAccept.run();
        }
    }

    private void handleNo()
    {
        if (!this.noFalling)
        {
            this.noFalling = true;
            this.noFallStart = System.currentTimeMillis();
        }
    }

    @Override
    protected void renderBackground(UIContext context)
    {
        /* Dim the entire screen behind the panel */
        UIElement parent = this.getParent();

        if (parent != null)
        {
            parent.area.render(context.batcher, Colors.A75);
        }

        super.renderBackground(context);
    }

    @Override
    public void render(UIContext context)
    {
        if (this.noFalling)
        {
            float t = (System.currentTimeMillis() - this.noFallStart) / 1000F;
            /* gravity: 800 px/s² */
            this.noButton.getFlex().y.offset = (int) (400F * t * t);
            this.resize();
        }

        super.render(context);
    }
}
