package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.EventPropagation;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.colors.Colors;

import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Popup element rendered on top of the global UI overlay showing the list of
 * items belonging to a toolbar section (or a nested submenu). Positions itself
 * in absolute overlay coordinates and coordinates open/close of its own
 * child submenus.
 *
 * <p>Phase 1 scope: this element renders and reacts to hover / click, but the
 * bound {@link ToolbarItem#runnable} references are typically {@code null},
 * so clicking a leaf is a no-op that just closes the chain.</p>
 */
public class ToolbarMenu extends UIElement
{
    /* Chain references */

    /**
     * Owner toolbar; the root of the popup chain. Used to coordinate open state
     * with the section buttons on the bar.
     */
    public final TimelineToolbar toolbar;

    /**
     * Parent popup in the chain, or {@code null} if this popup is directly
     * attached to a section button on the toolbar.
     */
    public final ToolbarMenu parentMenu;

    /* Content */

    public final List<ToolbarItem> items;

    /* Layout & interaction state */

    /**
     * Column at which shortcut text starts inside a row, in local pixels
     * relative to the row's left edge. Computed once in {@link #computeLayout}
     * so all shortcuts line up right-aligned to a common column.
     */
    private int shortcutColumn;

    /**
     * X coordinate at which the trailing arrow column starts (for submenu rows).
     */
    private int arrowColumn;

    /**
     * When {@code true}, the popup opened towards the left of its anchor (the
     * parent row) instead of the default right side. Used so the parent row's
     * trailing arrow flips to {@code <}.
     */
    private boolean openedToLeft;

    /**
     * Index of the item whose submenu is currently opened (in {@link #items}),
     * or {@code -1} if no submenu is open.
     */
    private int openChildIndex = -1;

    /**
     * Currently open child submenu, or {@code null}.
     */
    private ToolbarMenu openChild;

    /* Constructor */

    public ToolbarMenu(TimelineToolbar toolbar, ToolbarMenu parentMenu, List<ToolbarItem> items)
    {
        super();

        this.toolbar = toolbar;
        this.parentMenu = parentMenu;
        this.items = items;

        this.eventPropagataion(EventPropagation.BLOCK_INSIDE);
    }

    /* Public API */

    /**
     * Positions this popup so that its top-right anchors just above the given
     * anchor rectangle (used for root popups opening upward from the toolbar).
     * Falls back to opening downward if there is not enough space above.
     */
    public void openAbove(UIContext context, Area anchor)
    {
        this.computeLayout(context.batcher.getFont());

        int screenW = context.menu.width;
        int screenH = context.menu.height;

        int w = this.area.w;
        int h = this.area.h;

        int x = anchor.x;
        int preferredY = anchor.y - TimelineToolbarSettings.MENU_GAP - h;
        int y;

        if (preferredY >= 0)
        {
            y = preferredY;
        }
        else
        {
            /* Not enough space above: fall back to below the toolbar. */
            y = anchor.ey() + TimelineToolbarSettings.MENU_GAP;
        }

        if (x + w > screenW) x = screenW - w;
        if (x < 0) x = 0;
        if (y + h > screenH) y = screenH - h;
        if (y < 0) y = 0;

        this.area.setPos(x, y);
        this.area.setSize(w, h);
    }

    /**
     * Positions this popup as a child submenu of another popup, defaulting to
     * the right side of the parent row and flipping left when needed.
     */
    public void openBesides(UIContext context, Area rowRect)
    {
        this.computeLayout(context.batcher.getFont());

        int screenW = context.menu.width;
        int screenH = context.menu.height;

        int w = this.area.w;
        int h = this.area.h;

        int preferredX = rowRect.ex() + TimelineToolbarSettings.MENU_GAP;
        int x;

        if (preferredX + w <= screenW)
        {
            x = preferredX;
            this.openedToLeft = false;
        }
        else
        {
            int leftX = rowRect.x - TimelineToolbarSettings.MENU_GAP - w;

            if (leftX >= 0)
            {
                x = leftX;
                this.openedToLeft = true;
            }
            else
            {
                /* Neither side fits: clamp to the right edge of the screen. */
                x = Math.max(0, screenW - w);
                this.openedToLeft = false;
            }
        }

        int y = rowRect.y;

        if (y + h > screenH) y = screenH - h;
        if (y < 0) y = 0;

        this.area.setPos(x, y);
        this.area.setSize(w, h);
    }

    public boolean isOpenedToLeft()
    {
        return this.openedToLeft;
    }

    public void closeChain()
    {
        this.closeChild();

        ToolbarMenu p = this.parentMenu;

        while (p != null)
        {
            p.closeChild();
            p = p.parentMenu;
        }

        this.removeFromParent();
        this.toolbar.notifyChainClosed();
    }

    public void closeChild()
    {
        if (this.openChild != null)
        {
            this.openChild.closeChild();
            this.openChild.removeFromParent();
            this.openChild = null;
        }

        this.openChildIndex = -1;
    }

    /* Layout */

    private void computeLayout(FontRenderer font)
    {
        int labelMaxWidth = 0;
        int shortcutMaxWidth = 0;
        boolean anyHasSubmenu = false;

        for (ToolbarItem item : this.items)
        {
            if (item.separator || item.label == null)
            {
                continue;
            }

            int labelW = font.getWidth(item.label.get());

            if (labelW > labelMaxWidth) labelMaxWidth = labelW;

            if (item.keyCombo != null)
            {
                int scW = font.getWidth(item.keyCombo.getKeyCombo());

                if (scW > shortcutMaxWidth) shortcutMaxWidth = scW;
            }

            if (item.hasChildren())
            {
                anyHasSubmenu = true;
            }
        }

        int contentLeft = TimelineToolbarSettings.MENU_ITEM_PADDING_LEFT
            + TimelineToolbarSettings.MENU_ITEM_ICON_SLOT
            + TimelineToolbarSettings.MENU_ITEM_ICON_LABEL_GAP;

        this.shortcutColumn = contentLeft + labelMaxWidth + TimelineToolbarSettings.MENU_LABEL_SHORTCUT_GAP;

        int rowEnd = this.shortcutColumn + shortcutMaxWidth;

        if (anyHasSubmenu)
        {
            rowEnd += TimelineToolbarSettings.MENU_SHORTCUT_ARROW_GAP + TimelineToolbarSettings.MENU_ARROW_WIDTH;
        }

        this.arrowColumn = rowEnd - TimelineToolbarSettings.MENU_ARROW_WIDTH;

        int totalWidth = Math.max(TimelineToolbarSettings.MENU_MIN_WIDTH,
            rowEnd + TimelineToolbarSettings.MENU_ITEM_PADDING_RIGHT + TimelineToolbarSettings.MENU_WIDTH_PADDING);

        int totalHeight = 4;

        for (ToolbarItem item : this.items)
        {
            totalHeight += this.getItemHeight(item);
        }

        totalHeight += 4;

        this.area.setSize(totalWidth, totalHeight);
    }

    private int getItemHeight(ToolbarItem item)
    {
        if (item.separator)
        {
            return TimelineToolbarSettings.MENU_SEPARATOR_HEIGHT;
        }

        return TimelineToolbarSettings.MENU_ITEM_HEIGHT;
    }

    private int getRowIndexAt(int mouseY)
    {
        int y = this.area.y + 4;

        for (int i = 0; i < this.items.size(); i++)
        {
            ToolbarItem item = this.items.get(i);
            int h = this.getItemHeight(item);

            if (mouseY >= y && mouseY < y + h)
            {
                return i;
            }

            y += h;
        }

        return -1;
    }

    private Area getRowArea(int index)
    {
        int y = this.area.y + 4;

        for (int i = 0; i < this.items.size(); i++)
        {
            ToolbarItem item = this.items.get(i);
            int h = this.getItemHeight(item);

            if (i == index)
            {
                Area a = new Area();
                a.setPos(this.area.x, y);
                a.setSize(this.area.w, h);
                return a;
            }

            y += h;
        }

        return null;
    }

    /* Rendering */

    @Override
    public void render(UIContext context)
    {
        this.renderBackground(context);
        this.updateHover(context);
        this.renderItems(context);

        super.render(context);

        this.checkDismissByDistance(context);
    }

    private void renderBackground(UIContext context)
    {
        context.batcher.dropShadow(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 8,
            Colors.A25, Colors.A100);
        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(),
            TimelineToolbarSettings.MENU_BACKGROUND);
        context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(),
            TimelineToolbarSettings.MENU_BORDER);
    }

    private void updateHover(UIContext context)
    {
        if (!this.area.isInside(context))
        {
            return;
        }

        int hoveredIndex = this.getRowIndexAt(context.mouseY);

        if (hoveredIndex < 0)
        {
            return;
        }

        ToolbarItem item = this.items.get(hoveredIndex);

        if (item.separator)
        {
            return;
        }

        if (item.hasChildren() && item.isEnabled())
        {
            if (this.openChildIndex != hoveredIndex)
            {
                this.openSubmenu(context, hoveredIndex, item);
            }
        }
        else if (this.openChildIndex != -1)
        {
            /* Hovering a leaf row while a submenu was open: close the submenu. */
            this.closeChild();
        }
    }

    private void openSubmenu(UIContext context, int index, ToolbarItem item)
    {
        this.closeChild();

        ToolbarMenu child = new ToolbarMenu(this.toolbar, this, item.children);
        context.menu.overlay.add(child);
        child.openBesides(context, this.getRowArea(index));

        this.openChildIndex = index;
        this.openChild = child;
    }

    private void renderItems(UIContext context)
    {
        FontRenderer font = context.batcher.getFont();
        int y = this.area.y + 4;

        for (int i = 0; i < this.items.size(); i++)
        {
            ToolbarItem item = this.items.get(i);
            int h = this.getItemHeight(item);

            if (item.separator)
            {
                this.renderSeparator(context, y, h);
            }
            else
            {
                boolean hover = this.area.x <= context.mouseX && context.mouseX < this.area.ex()
                    && context.mouseY >= y && context.mouseY < y + h;
                boolean submenuAnchor = this.openChildIndex == i;

                this.renderRow(context, font, item, y, h, hover, submenuAnchor);
            }

            y += h;
        }
    }

    private void renderSeparator(UIContext context, int y, int h)
    {
        int midY = y + h / 2;
        int x1 = this.area.x + 6;
        int x2 = this.area.ex() - 6;

        context.batcher.box(x1, midY, x2, midY + 1, TimelineToolbarSettings.MENU_BORDER);
    }

    private void renderRow(UIContext context, FontRenderer font, ToolbarItem item, int y, int h,
        boolean hover, boolean submenuAnchor)
    {
        boolean enabled = item.isEnabled();
        int labelColor = enabled ? TimelineToolbarSettings.MENU_ITEM_FG
            : TimelineToolbarSettings.MENU_ITEM_DISABLED_FG;
        int shortcutColor = enabled ? TimelineToolbarSettings.MENU_ITEM_SHORTCUT_FG
            : TimelineToolbarSettings.MENU_ITEM_DISABLED_FG;
        int iconColor = enabled ? TimelineToolbarSettings.MENU_ITEM_FG
            : TimelineToolbarSettings.MENU_ITEM_DISABLED_FG;

        int rowX1 = this.area.x;
        int rowX2 = this.area.ex();

        if ((hover || submenuAnchor) && enabled)
        {
            context.batcher.box(rowX1, y, rowX2, y + h, TimelineToolbarSettings.MENU_ITEM_HOVER);
        }

        if (item.destructive)
        {
            /* Vertical red bar on the left, same visual as UIIcon for REMOVE/TRASH. */
            context.batcher.box(rowX1, y, rowX1 + 2, y + h, Colors.A100 | Colors.RED);
            context.batcher.gradientHBox(rowX1 + 2, y, rowX1 + 20, y + h, Colors.A25 | Colors.RED, 0);
        }

        int iconSlotX = rowX1 + TimelineToolbarSettings.MENU_ITEM_PADDING_LEFT;
        int iconCenterY = y + h / 2;

        if (item.icon != null)
        {
            context.batcher.icon(item.icon, iconColor,
                iconSlotX + TimelineToolbarSettings.MENU_ITEM_ICON_SLOT / 2, iconCenterY, 0.5F, 0.5F);
        }

        int labelX = iconSlotX + TimelineToolbarSettings.MENU_ITEM_ICON_SLOT
            + TimelineToolbarSettings.MENU_ITEM_ICON_LABEL_GAP;
        int labelY = y + (h - font.getHeight()) / 2 + 1;

        if (item.label != null)
        {
            context.batcher.text(item.label.get(), labelX, labelY, labelColor, false);
        }

        if (item.keyCombo != null)
        {
            String shortcut = item.keyCombo.getKeyCombo();

            context.batcher.text(shortcut, rowX1 + this.shortcutColumn, labelY, shortcutColor, false);
        }

        if (item.hasChildren())
        {
            String arrow = submenuAnchor && this.openChild != null && this.openChild.isOpenedToLeft()
                ? "<"
                : ">";
            int arrowX = rowX1 + this.arrowColumn;
            int arrowY = labelY;

            context.batcher.text(arrow, arrowX, arrowY,
                enabled ? TimelineToolbarSettings.MENU_ARROW_FG
                    : TimelineToolbarSettings.MENU_ITEM_DISABLED_FG, false);
        }

        if (!enabled && hover)
        {
            IKey reason = item.getDisabledReason();

            if (reason != null)
            {
                String txt = reason.get();
                int tx = context.mouseX + 8;
                int ty = context.mouseY + 12;

                context.batcher.textCard(txt, tx, ty,
                    TimelineToolbarSettings.MENU_ITEM_DISABLED_REASON_FG, Colors.A75);
            }
        }
    }

    private void checkDismissByDistance(UIContext context)
    {
        /* Only the root popup runs the check to avoid duplicated work. */
        if (this.parentMenu != null)
        {
            return;
        }

        int mx = context.mouseX;
        int my = context.mouseY;
        int threshold = TimelineToolbarSettings.TOOLBAR_MENU_DISMISS_DISTANCE_PX;

        if (this.chainContainsPoint(mx, my))
        {
            return;
        }

        if (this.toolbar.area.isInside(mx, my))
        {
            return;
        }

        int minDist = this.distanceToRect(this.toolbar.area, mx, my);
        ToolbarMenu current = this;

        while (current != null)
        {
            int d = this.distanceToRect(current.area, mx, my);

            if (d < minDist) minDist = d;

            current = current.openChild;
        }

        if (minDist > threshold)
        {
            this.closeChain();
        }
    }

    private boolean chainContainsPoint(int x, int y)
    {
        ToolbarMenu current = this;

        while (current != null)
        {
            if (current.area.isInside(x, y))
            {
                return true;
            }

            current = current.openChild;
        }

        return false;
    }

    private int distanceToRect(Area a, int x, int y)
    {
        int dx = 0;
        int dy = 0;

        if (x < a.x) dx = a.x - x;
        else if (x > a.ex()) dx = x - a.ex();

        if (y < a.y) dy = a.y - y;
        else if (y > a.ey()) dy = y - a.ey();

        if (dx == 0 && dy == 0) return 0;

        return (int) Math.sqrt(dx * dx + dy * dy);
    }

    /* Input */

    @Override
    protected boolean subMouseClicked(UIContext context)
    {
        if (!this.area.isInside(context))
        {
            /* Click outside chain: close everything and let it propagate to
             * whatever is below (so the existing right-click context menu can
             * still open normally). */
            this.closeChain();

            return false;
        }

        int index = this.getRowIndexAt(context.mouseY);

        if (index < 0)
        {
            return true;
        }

        ToolbarItem item = this.items.get(index);

        if (item.separator || !item.isEnabled())
        {
            return true;
        }

        if (context.mouseButton == 0)
        {
            if (item.hasChildren())
            {
                if (this.openChildIndex == index)
                {
                    this.closeChild();
                }
                else
                {
                    this.openSubmenu(context, index, item);
                }
            }
            else if (item.runnable != null)
            {
                item.runnable.run();
                this.closeChain();
            }
            else
            {
                /* Phase 1: no handler wired yet; close the chain so the user
                 * gets feedback that the click was received. */
                this.closeChain();
            }
        }
        else if (context.mouseButton == 1)
        {
            /* Right-click inside the popup just dismisses without opening the
             * underlying context menu. */
            this.closeChain();
        }

        return true;
    }

    @Override
    protected boolean subKeyPressed(UIContext context)
    {
        if (context.isPressed(GLFW.GLFW_KEY_ESCAPE))
        {
            this.closeChain();

            return true;
        }

        return super.subKeyPressed(context);
    }
}
