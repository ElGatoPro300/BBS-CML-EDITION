package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.EventPropagation;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.ArrayList;
import java.util.List;

/**
 * The toolbar bar itself. Displays a horizontal row of icon-only section
 * buttons anchored to the bottom of a timeline panel. Clicking a section
 * opens its popup (a {@link ToolbarMenu}) attached to the global UI overlay.
 *
 * <p>Also coordinates open/close state so hovering across sections while one
 * is open switches to the hovered section without needing an extra click
 * (Blender-like behaviour).</p>
 */
public class TimelineToolbar extends UIElement
{
    private final List<ToolbarSection> sections = new ArrayList<>();

    /**
     * Cached areas of each section button after every render pass. Kept
     * separate from the child element list so opening a popup does not need
     * to walk the child tree.
     */
    private final List<Area> sectionAreas = new ArrayList<>();

    /**
     * Index of the currently open section, or {@code -1} if no popup is open.
     */
    private int openIndex = -1;

    /**
     * Currently open root popup, or {@code null}.
     */
    private ToolbarMenu openMenu;

    /* Constructor */

    public TimelineToolbar()
    {
        super();

        this.h(TimelineToolbarSettings.TOOLBAR_HEIGHT);
        /* Block mouse events inside the bar (clicks should not pass through to
         * the timeline) but let keyboard events propagate so Escape can still
         * close the editor when the cursor is over the toolbar. */
        this.mouseEventPropagataion(EventPropagation.BLOCK_INSIDE);
    }

    /* API */

    public TimelineToolbar setSections(List<ToolbarSection> newSections)
    {
        this.closeOpenMenu();
        this.sections.clear();
        this.sections.addAll(newSections);
        this.sectionAreas.clear();

        for (int i = 0; i < newSections.size(); i++)
        {
            this.sectionAreas.add(new Area());
        }

        return this;
    }

    public List<ToolbarSection> getSections()
    {
        return this.sections;
    }

    public boolean isMenuOpen()
    {
        return this.openMenu != null;
    }

    public int getOpenIndex()
    {
        return this.openIndex;
    }

    /**
     * Returns the section index under the given screen coordinates, or
     * {@code -1} if the point is not over a section button.
     */
    public int getSectionIndexAt(int mouseX, int mouseY)
    {
        this.layoutSections();

        for (int i = 0; i < this.sectionAreas.size(); i++)
        {
            if (this.sectionAreas.get(i).isInside(mouseX, mouseY))
            {
                return i;
            }
        }

        return -1;
    }

    public boolean isToolbarSectionAt(int mouseX, int mouseY)
    {
        return this.getSectionIndexAt(mouseX, mouseY) >= 0;
    }

    public void notifyChainClosed()
    {
        this.openMenu = null;
        this.openIndex = -1;
    }

    public void closeOpenMenu()
    {
        if (this.openMenu != null)
        {
            this.openMenu.closeChain();
        }
    }

    /* Layout: compute section rects each frame based on our own area */

    private void layoutSections()
    {
        int size = this.sections.size();

        if (this.sectionAreas.size() != size)
        {
            this.sectionAreas.clear();

            for (int i = 0; i < size; i++)
            {
                this.sectionAreas.add(new Area());
            }
        }

        int x = this.area.x + TimelineToolbarSettings.TOOLBAR_PADDING;
        int y = this.area.y + (this.area.h - TimelineToolbarSettings.TOOLBAR_SECTION_WIDTH) / 2;
        int w = TimelineToolbarSettings.TOOLBAR_SECTION_WIDTH;
        int h = TimelineToolbarSettings.TOOLBAR_SECTION_WIDTH;

        for (int i = 0; i < size; i++)
        {
            Area a = this.sectionAreas.get(i);
            a.setPos(x, y);
            a.setSize(w, h);

            x += w + TimelineToolbarSettings.TOOLBAR_SECTION_SPACING;
        }
    }

    /* Rendering */

    @Override
    public void render(UIContext context)
    {
        this.layoutSections();
        this.renderBar(context);

        int hovered = this.getSectionIndexAt(context.mouseX, context.mouseY);

        this.renderSections(context, hovered);
        this.updateHoverSwitch(context);

        super.render(context);

        /* Queue after super.render(): UIElement.render() calls resetTooltip()
         * when the mouse is inside this bar (BLOCK_INSIDE), which would wipe a
         * card queued earlier in the same pass. */
        if (hovered >= 0)
        {
            ToolbarSection section = this.sections.get(hovered);
            String text = section.label.get();

            context.drawForegroundTextCard(text, context.mouseX + 8, context.mouseY + 12,
                Colors.WHITE, Colors.A75);
        }
    }

    private void renderBar(UIContext context)
    {
        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(),
            TimelineToolbarSettings.TOOLBAR_BACKGROUND);
        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.y + 1,
            TimelineToolbarSettings.TOOLBAR_BORDER);
    }

    private void renderSections(UIContext context, int hovered)
    {
        for (int i = 0; i < this.sections.size(); i++)
        {
            ToolbarSection section = this.sections.get(i);
            Area a = this.sectionAreas.get(i);
            boolean isOpen = this.openIndex == i;
            boolean isHover = hovered == i;

            this.renderSection(context, section, a, isHover, isOpen);
        }
    }

    private void renderSection(UIContext context, ToolbarSection section, Area a, boolean hover,
        boolean isOpen)
    {
        if (isOpen)
        {
            context.batcher.box(a.x, a.y, a.ex(), a.ey(), TimelineToolbarSettings.SECTION_OPEN_COLOR);
        }
        else if (hover)
        {
            context.batcher.box(a.x, a.y, a.ex(), a.ey(), TimelineToolbarSettings.SECTION_HOVER_COLOR);
        }

        Icon icon = section.icon;

        if (icon != null)
        {
            context.batcher.icon(icon, Colors.WHITE, a.mx(), a.my(), 0.5F, 0.5F);
        }
    }

    private void updateHoverSwitch(UIContext context)
    {
        if (this.openMenu == null)
        {
            return;
        }

        int hovered = this.getSectionIndexAt(context.mouseX, context.mouseY);

        if (hovered >= 0 && hovered != this.openIndex)
        {
            this.openSection(context, hovered);
        }
    }

    /* Open logic */

    private void openSection(UIContext context, int index)
    {
        if (this.openMenu != null)
        {
            this.openMenu.closeChain();
        }

        ToolbarSection section = this.sections.get(index);

        if (section.items.isEmpty())
        {
            return;
        }

        ToolbarMenu menu = new ToolbarMenu(this, null, section.items);
        context.menu.overlay.add(menu);
        menu.openAbove(context, this.sectionAreas.get(index));

        this.openIndex = index;
        this.openMenu = menu;
    }

    /* Input */

    @Override
    protected boolean subMouseClicked(UIContext context)
    {
        if (!this.area.isInside(context))
        {
            return false;
        }

        int index = this.getSectionIndexAt(context.mouseX, context.mouseY);

        if (index < 0)
        {
            return true;
        }

        if (context.mouseButton == 0)
        {
            if (this.openMenu != null && this.openIndex == index)
            {
                this.closeOpenMenu();
            }
            else
            {
                this.openSection(context, index);
            }
        }

        return true;
    }
}
