package mchorse.bbs_mod.ui.framework.elements.context;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.ui.utils.context.ContextAction;
import mchorse.bbs_mod.ui.utils.context.ContextSeparatorAction;

import java.util.List;
import java.util.function.Consumer;

public class UIActionList extends UIList<ContextAction>
{
    private static final int SEPARATOR_HEIGHT = 8;

    public UIActionList(Consumer<List<ContextAction>> callback)
    {
        super(callback);
    }

    private int getItemHeight(ContextAction action)
    {
        return action instanceof ContextSeparatorAction ? SEPARATOR_HEIGHT : this.scroll.scrollItemSize;
    }

    private int getIndexAt(int mouseY)
    {
        int y = this.area.y - (int) this.scroll.getScroll();

        for (int i = 0; i < this.list.size(); i++)
        {
            ContextAction action = this.list.get(i);
            int h = this.getItemHeight(action);

            if (mouseY >= y && mouseY < y + h)
            {
                return i;
            }

            y += h;
        }

        return -1;
    }

    @Override
    public void update()
    {
        int totalHeight = 0;

        for (ContextAction action : this.list)
        {
            totalHeight += this.getItemHeight(action);
        }

        this.scroll.scrollSize = totalHeight;
        this.scroll.clamp();
    }

    @Override
    public void renderList(UIContext context)
    {
        int y = this.area.y - (int) this.scroll.getScroll();
        int low = this.area.y;
        int high = this.area.ey();
        int mouseX = context.mouseX;
        int mouseY = context.mouseY;

        for (int i = 0; i < this.list.size(); i++)
        {
            ContextAction action = this.list.get(i);
            int h = this.getItemHeight(action);

            if (y + h < low)
            {
                y += h;
                continue;
            }

            if (y >= high)
            {
                break;
            }

            boolean hover = mouseX >= this.area.x && mouseY >= y && mouseX < this.area.ex() && mouseY < y + h;
            boolean selected = this.current.contains(i);

            action.render(context, context.batcher.getFont(), this.area.x, y, this.area.w, h, hover, selected);

            y += h;
        }
    }

    @Override
    public void renderListElement(UIContext context, ContextAction element, int i, int x, int y, boolean hover, boolean selected)
    {
        int h = this.getItemHeight(element);

        element.render(context, context.batcher.getFont(), x, y, this.area.w, h, hover, selected);
    }

    @Override
    public boolean subMouseClicked(UIContext context)
    {
        if (this.scroll.mouseClicked(context))
        {
            return true;
        }

        if (this.area.isInside(context) && context.mouseButton == 0)
        {
            int index = this.getIndexAt(context.mouseY);

            if (this.exists(index))
            {
                ContextAction action = this.list.get(index);

                if (action instanceof ContextSeparatorAction)
                {
                    return true;
                }

                this.setIndex(index);

                if (this.callback != null)
                {
                    this.callback.accept(this.getCurrent());

                    return true;
                }
            }

            return true;
        }

        return super.subMouseClicked(context);
    }
}
