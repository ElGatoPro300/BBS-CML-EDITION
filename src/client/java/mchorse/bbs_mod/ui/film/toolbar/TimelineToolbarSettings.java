package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.utils.colors.Colors;

/**
 * Tweakable constants for the timeline toolbar system. Kept in a dedicated
 * class so future adjustments (density, dismiss distance, colors) can be
 * changed in one place without touching UI logic.
 */
public class TimelineToolbarSettings
{
    /* Layout */

    /**
     * Height in pixels of the toolbar itself (anchored to the bottom of a
     * timeline panel).
     */
    public static final int TOOLBAR_HEIGHT = 32;

    /**
     * Horizontal margin between two root sections in the toolbar.
     */
    public static final int TOOLBAR_SECTION_SPACING = 2;

    /**
     * Width of a root section button (icon-only, square with the toolbar
     * height).
     */
    public static final int TOOLBAR_SECTION_WIDTH = 28;

    /**
     * Horizontal padding inside the toolbar bar itself (left/right insets).
     */
    public static final int TOOLBAR_PADDING = 4;

    /* Menu popup layout */

    /**
     * Height in pixels of a single menu row (item).
     */
    public static final int MENU_ITEM_HEIGHT = 18;

    /**
     * Height in pixels of a separator row.
     */
    public static final int MENU_SEPARATOR_HEIGHT = 6;

    /**
     * Left padding inside a menu row before the icon slot.
     */
    public static final int MENU_ITEM_PADDING_LEFT = 6;

    /**
     * Right padding inside a menu row after the shortcut / arrow.
     */
    public static final int MENU_ITEM_PADDING_RIGHT = 6;

    /**
     * Fixed width reserved for the leading icon slot on every row. Rows with
     * no icon leave this slot empty so labels line up across items.
     */
    public static final int MENU_ITEM_ICON_SLOT = 18;

    /**
     * Gap between the icon slot and the label.
     */
    public static final int MENU_ITEM_ICON_LABEL_GAP = 2;

    /**
     * Minimum gap between the label and the shortcut column.
     */
    public static final int MENU_LABEL_SHORTCUT_GAP = 20;

    /**
     * Gap between the shortcut column and the trailing arrow (>) for
     * submenus.
     */
    public static final int MENU_SHORTCUT_ARROW_GAP = 6;

    /**
     * Width reserved for the trailing arrow indicating a submenu.
     */
    public static final int MENU_ARROW_WIDTH = 6;

    /**
     * Minimum overall width of a popup menu.
     */
    public static final int MENU_MIN_WIDTH = 100;

    /**
     * Extra padding added to a popup's width to keep things breathable.
     */
    public static final int MENU_WIDTH_PADDING = 8;

    /**
     * Vertical gap between a section button and its popup (or between a
     * parent submenu row and its child popup on overlap).
     */
    public static final int MENU_GAP = 1;

    /* Interaction */

    /**
     * How far (in pixels) the mouse must travel away from every open menu
     * rectangle (root section + open popups) before the entire chain closes.
     * Blender-like dismiss distance.
     */
    public static final int TOOLBAR_MENU_DISMISS_DISTANCE_PX = 40;

    /**
     * Delay in ms before a hovered submenu row expands its child popup.
     * Kept small to feel responsive; larger values reduce accidental opens
     * when the mouse crosses a submenu row on its way somewhere else.
     */
    public static final long SUBMENU_HOVER_OPEN_DELAY_MS = 120L;

    /* Colors */

    /**
     * Background color of the toolbar bar.
     */
    public static final int TOOLBAR_BACKGROUND = 0xff1a1a1e;

    /**
     * Top separator line color for the toolbar.
     */
    public static final int TOOLBAR_BORDER = 0xff3a3a42;

    /**
     * Hover tint for a root section button.
     */
    public static final int SECTION_HOVER_COLOR = Colors.A50 | 0x4488ff;

    /**
     * Highlight color when a section popup is currently open.
     */
    public static final int SECTION_OPEN_COLOR = Colors.A75 | 0x4488ff;

    /**
     * Background color of a popup menu.
     */
    public static final int MENU_BACKGROUND = 0xf01e1e22;

    /**
     * Border color of a popup menu.
     */
    public static final int MENU_BORDER = 0xff3a3a42;

    /**
     * Hover tint applied to a menu row.
     */
    public static final int MENU_ITEM_HOVER = Colors.A50 | 0x4488ff;

    /**
     * Color of a menu item label / icon when enabled.
     */
    public static final int MENU_ITEM_FG = Colors.WHITE;

    /**
     * Color of the shortcut column when enabled.
     */
    public static final int MENU_ITEM_SHORTCUT_FG = 0xff9aa0a6;

    /**
     * Color of a menu item label / icon when disabled (any cause).
     */
    public static final int MENU_ITEM_DISABLED_FG = 0xff707078;

    /**
     * Text color for the "reason" tooltip shown when a red-disabled item is
     * hovered (e.g. viewport hidden). Only the tooltip text is red; the row
     * itself stays with the normal disabled color to avoid noise.
     */
    public static final int MENU_ITEM_DISABLED_REASON_FG = Colors.A100 | Colors.RED;

    /**
     * Color of the trailing arrow for submenus (enabled state).
     */
    public static final int MENU_ARROW_FG = 0xffcccccc;

    /* Constructor */

    private TimelineToolbarSettings()
    {}
}
