package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIPromptOverlayPanel;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.ArrayList;
import java.util.List;

public class UIRenderQueueOverlayPanel extends UIOverlayPanel
{
    public UIRenderQueueList queueList;
    public UIIcon addEntry;
    public UIIcon removeEntry;
    public UIElement previewArea;
    public UIButton renderButton;

    public UIRenderQueueOverlayPanel()
    {
        super(UIKeys.RENDER_QUEUE_TITLE);

        /* Queue list */
        this.queueList = new UIRenderQueueList((list) ->
        {});
        this.queueList.background();
        this.queueList.relative(this.content).xy(0, 20).w(180).h(1F, -20);

        /* Add / remove icon buttons above the list */
        this.addEntry = new UIIcon(Icons.ADD, (b) -> this.openAddEntry());
        this.addEntry.tooltip(UIKeys.RENDER_QUEUE_ADD, Direction.TOP);
        this.addEntry.relative(this.content).xy(0, 0).wh(20, 20);

        this.removeEntry = new UIIcon(Icons.REMOVE, (b) -> this.removeSelectedEntry());
        this.removeEntry.tooltip(UIKeys.RENDER_QUEUE_REMOVE, Direction.TOP);
        this.removeEntry.relative(this.content).xy(20, 0).wh(20, 20);

        /* Preview area to the right of the list */
        this.previewArea = new UIElement();
        this.previewArea.relative(this.content).x(186).y(0).w(1F, -186).h(1F, -26);

        /* Render button bottom-right */
        this.renderButton = new UIButton(UIKeys.RENDER_QUEUE_RENDER, (b) -> this.startRender());
        this.renderButton.relative(this.content).x(1F, -86).y(1F, -22).wh(82, 20);

        this.content.add(this.addEntry, this.removeEntry, this.queueList, this.previewArea, this.renderButton);
    }

    private void openAddEntry()
    {
        UIPromptOverlayPanel panel = new UIPromptOverlayPanel(
            UIKeys.RENDER_QUEUE_ADD_TITLE,
            UIKeys.RENDER_QUEUE_ADD_MESSAGE,
            (name) ->
            {
                if (name != null && !name.trim().isEmpty())
                {
                    this.addToQueue(name.trim());
                }
            }
        );

        UIOverlay.addOverlay(this.getContext(), panel, 240, 0.5F);
    }

    private void addToQueue(String name)
    {
        List<String> list = this.queueList.getList();

        if (!list.contains(name))
        {
            this.queueList.add(name);
        }
    }

    private void removeSelectedEntry()
    {
        String selected = this.queueList.getCurrentFirst();

        if (selected != null)
        {
            this.queueList.remove(selected);
        }
    }

    private void startRender()
    {
        /* TODO: trigger rendering of queued films in order */
    }

    @Override
    protected void renderBackground(UIContext context)
    {
        super.renderBackground(context);

        /* Preview area background (darker box) */
        context.batcher.box(
            this.previewArea.area.x,
            this.previewArea.area.y,
            this.previewArea.area.ex(),
            this.previewArea.area.ey(),
            Colors.A50
        );

        String selected = this.queueList.getCurrentFirst();

        if (selected != null && !selected.isEmpty())
        {
            int tw = context.batcher.getDefaultTextRenderer().getWidth(selected);
            int th = context.batcher.getDefaultTextRenderer().getHeight();

            context.batcher.text(
                selected,
                this.previewArea.area.mx() - tw / 2,
                this.previewArea.area.my() - th / 2,
                Colors.WHITE
            );
        }
    }
}
