package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.EventPropagation;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.ArrayList;
import java.util.List;

/**
 * The toolbar bar itself. Displays a horizontal row of section buttons
 * anchored to the bottom of a timeline panel. Each section shows an icon and,
 * when space allows, its label to the right; otherwise the label collapses
 * into a hover tooltip (rightmost sections first). Clicking a section opens its
 * popup ({@link ToolbarMenu}) on the global UI overlay.
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
     * Whether each section currently shows its label inline ({@code true}) or
     * is collapsed to icon-only ({@code false}).
     */
    private boolean[] sectionShowLabel = new boolean[0];

    /**
     * Index of the currently open section, or {@code -1} if no popup is open.
     */
    private int openIndex = -1;

    /**
     * Currently open root popup, or {@code null}.
     */
    private ToolbarMenu openMenu;

    /**
     * Invoked when the toolbar hierarchy changes or a section is opened,
     * so an active timeline interaction mode can be cancelled.
     */
    private Runnable interactionCancelListener;

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
        this.notifyInteractionCancel();
        this.closeOpenMenu();
        this.sections.clear();
        this.sections.addAll(newSections);
        this.sectionAreas.clear();
        this.sectionShowLabel = new boolean[newSections.size()];

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

    /**
     * Whether {@code menu} is still the root popup tracked by this toolbar.
     * Used when deferred closes run so replacing a menu via hover-switch does
     * not clear the newly opened section.
     */
    public boolean isActiveRootMenu(ToolbarMenu menu)
    {
        return this.openMenu == menu;
    }

    public void closeOpenMenu()
    {
        if (this.openMenu != null)
        {
            this.openMenu.closeChain();
        }
    }

    public void setInteractionCancelListener(Runnable listener)
    {
        this.interactionCancelListener = listener;
    }

    private void notifyInteractionCancel()
    {
        if (this.interactionCancelListener != null)
        {
            this.interactionCancelListener.run();
        }
    }

    /* Layout */

    private void layoutSections(FontRenderer font)
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

        if (this.sectionShowLabel.length != size)
        {
            this.sectionShowLabel = new boolean[size];
        }

        for (int i = 0; i < size; i++)
        {
            this.sectionShowLabel[i] = true;
        }

        int available = this.area.w - TimelineToolbarSettings.TOOLBAR_PADDING * 2;
        int total = this.computeSectionsWidth(font, this.sectionShowLabel);

        for (int i = size - 1; i >= 0 && total > available; i--)
        {
            if (this.sectionShowLabel[i])
            {
                this.sectionShowLabel[i] = false;
                total = this.computeSectionsWidth(font, this.sectionShowLabel);
            }
        }

        int x = this.area.x + TimelineToolbarSettings.TOOLBAR_PADDING;
        int y = this.area.y;
        int h = this.area.h;
        int maxX = this.area.ex() - TimelineToolbarSettings.TOOLBAR_PADDING;

        for (int i = 0; i < size; i++)
        {
            int w = this.getSectionWidth(font, i, this.sectionShowLabel[i]);
            Area a = this.sectionAreas.get(i);

            if (x >= maxX)
            {
                a.setPos(x, y);
                a.setSize(0, h);

                continue;
            }

            if (x + w > maxX)
            {
                w = maxX - x;
            }

            a.setPos(x, y);
            a.setSize(w, h);

            x += w;

            if (i < size - 1)
            {
                x += TimelineToolbarSettings.TOOLBAR_SECTION_SPACING;
            }
        }
    }

    private int computeSectionsWidth(FontRenderer font, boolean[] showLabel)
    {
        int size = this.sections.size();
        int total = 0;

        for (int i = 0; i < size; i++)
        {
            total += this.getSectionWidth(font, i, showLabel[i]);

            if (i < size - 1)
            {
                total += TimelineToolbarSettings.TOOLBAR_SECTION_SPACING;
            }
        }

        return total;
    }

    private int getSectionWidth(FontRenderer font, int index, boolean showLabel)
    {
        if (!showLabel)
        {
            return TimelineToolbarSettings.SECTION_ICON_SIZE;
        }

        String text = this.sections.get(index).label.get();

        return TimelineToolbarSettings.SECTION_ICON_SIZE
            + TimelineToolbarSettings.SECTION_LABEL_PADDING
            + font.getWidth(text)
            + TimelineToolbarSettings.SECTION_LABEL_TRAILING_PADDING;
    }

    /* Rendering */

    @Override
    public void render(UIContext context)
    {
        FontRenderer font = context.batcher.getFont();

        this.layoutSections(font);
        this.renderBar(context);

        context.batcher.clip(this.area, context);

        int hovered = this.getSectionIndexAt(context.mouseX, context.mouseY);
        boolean suppressSectionHover = this.openMenu != null && this.isPointerOverOpenMenu(context);

        this.renderSections(context, font, hovered, suppressSectionHover);

        context.batcher.unclip(context);

        this.updateHoverSwitch(context);

        super.render(context);

        /* Queue after super.render(): UIElement.render() calls resetTooltip()
         * when the mouse is inside this bar (BLOCK_INSIDE), which would wipe a
         * card queued earlier in the same pass. Only show the hover card when
         * the section label is collapsed (icon-only mode). */
        if (hovered >= 0 && !this.sectionShowLabel[hovered] && !suppressSectionHover)
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

    private void renderSections(UIContext context, FontRenderer font, int hovered, boolean suppressHover)
    {
        for (int i = 0; i < this.sections.size(); i++)
        {
            Area a = this.sectionAreas.get(i);

            if (a.w <= 0)
            {
                continue;
            }

            ToolbarSection section = this.sections.get(i);
            boolean isOpen = this.openIndex == i;
            boolean isHover = hovered == i && !suppressHover;
            boolean showLabel = this.sectionShowLabel[i];

            this.renderSection(context, font, section, a, isHover, isOpen, showLabel);
        }
    }

    private void renderSection(UIContext context, FontRenderer font, ToolbarSection section, Area a,
        boolean hover, boolean isOpen, boolean showLabel)
    {
        if (isOpen)
        {
            context.batcher.box(a.x, a.y, a.ex(), a.ey(), TimelineToolbarSettings.SECTION_OPEN_COLOR);
        }
        else if (hover)
        {
            context.batcher.box(a.x, a.y, a.ex(), a.ey(), TimelineToolbarSettings.SECTION_HOVER_COLOR);
        }

        int iconSize = TimelineToolbarSettings.SECTION_ICON_SIZE;
        int iconY = a.y + (a.h - iconSize) / 2;
        Icon icon = section.icon;

        if (icon != null)
        {
            context.batcher.icon(icon, Colors.WHITE, a.x + iconSize / 2F, iconY + iconSize / 2F, 0.5F, 0.5F);
        }

        if (showLabel)
        {
            String text = section.label.get();
            int labelX = a.x + iconSize + TimelineToolbarSettings.SECTION_LABEL_PADDING;
            int labelY = a.y + (a.h - font.getHeight()) / 2 + 1;
            int labelMaxX = a.ex() - TimelineToolbarSettings.SECTION_LABEL_TRAILING_PADDING;

            if (labelX < labelMaxX)
            {
                context.batcher.text(text, labelX, labelY, Colors.WHITE, false);
            }
        }
    }

    private boolean isPointerOverOpenMenu(UIContext context)
    {
        if (this.openMenu == null)
        {
            return false;
        }

        return this.openMenu.isChainAt(context.mouseX, context.mouseY);
    }

    /**
     * While a popup chain is open, section switching on hover is only allowed
     * when the cursor is on the toolbar bar itself — not when it moves over an
     * open menu/submenu (including overlap in small windows).
     */
    private boolean shouldAllowSectionHoverSwitch(UIContext context)
    {
        if (this.openMenu == null)
        {
            return false;
        }

        if (this.isPointerOverOpenMenu(context))
        {
            return false;
        }

        return this.area.isInside(context.mouseX, context.mouseY);
    }

    private void updateHoverSwitch(UIContext context)
    {
        if (!this.shouldAllowSectionHoverSwitch(context))
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
        this.notifyInteractionCancel();

        if (this.openMenu != null)
        {
            this.openMenu.closeChain();
        }
        else
        {
            this.purgeOrphanMenus(context);
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

    /**
     * Removes stray popup elements that lost sync with {@link #openMenu}
     * (e.g. after a pinned submenu click before hover-only behaviour).
     */
    private void purgeOrphanMenus(UIContext context)
    {
        for (ToolbarMenu menu : context.menu.overlay.getChildren(ToolbarMenu.class, new ArrayList<>(), true))
        {
            if (menu.toolbar == this)
            {
                menu.removeFromParent();
            }
        }

        this.notifyChainClosed();
    }

    /* Input */

    @Override
    protected boolean subMouseClicked(UIContext context)
    {
        if (!this.area.isInside(context))
        {
            return false;
        }

        this.layoutSections(context.batcher.getFont());

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

            context.setTimelineToolbarConsumePointer(true);
        }

        return true;
    }
}
