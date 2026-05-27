package mchorse.bbs_mod.settings.ui;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.Settings;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIClickable;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.ScrollDirection;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.interps.Interpolations;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class UISettingsOverlayPanel extends UIOverlayPanel
{
    private static final int SIDEBAR_WIDTH = 180;

    public UIScrollView sidebar;
    public UIElement panel;
    public UIScrollView options;
    public UITextbox search;

    private Settings settings;
    private String selectedCategoryId;
    private boolean isKeybindsSelected;
    private UISettingsTab currentTab;

    public UISettingsOverlayPanel()
    {
        super(UIKeys.CONFIG_TITLE);
        this.title.color(Colors.WHITE);
        this.resizable();
        this.content.w(1F);

        this.sidebar = new UIScrollView(ScrollDirection.VERTICAL);
        this.sidebar.relative(this.content).x(0).y(0).w(SIDEBAR_WIDTH).h(1F);
        this.sidebar.column(2).vertical().stretch().scroll().padding(6);

        this.panel = new UIElement();
        this.panel.relative(this.content).x(SIDEBAR_WIDTH).y(0).w(1F, -SIDEBAR_WIDTH).h(1F);

        this.options = new UIScrollView(ScrollDirection.VERTICAL);
        this.options.scroll.scrollSpeed = 51;
        this.options.relative(this.panel).x(6).y(6).w(1F, -12).h(1F, -12);
        this.options.column(8).scroll().vertical().stretch().padding(10).height(20);

        this.search = new UITextbox(100, (str) -> this.refresh());
        this.search.placeholder(UIKeys.GENERAL_SEARCH);
        this.search.h(20);

        this.panel.add(this.options);
        this.content.add(this.sidebar, this.panel);

        this.rebuildTabs();
        this.markContainer();
    }

    private void rebuildTabs()
    {
        this.sidebar.removeAll();
        this.currentTab = null;

        UISettingsTab firstTab = null;

        Settings bbsSettings = BBSMod.getSettings().modules.get("bbs");
        if (bbsSettings != null)
        {
            for (ValueGroup category : bbsSettings.categories.values())
            {
                if (!category.isVisible())
                {
                    continue;
                }
                String categoryId = category.getId();
                IKey label = L10n.lang(UIValueFactory.getCategoryTitleKey(category));
                Icon icon = this.getCategoryIcon(categoryId);
                UISettingsTab tab = new UISettingsTab(label, icon, categoryId, false, (t) -> this.selectCategory(categoryId, t));
                tab.tooltip(label, Direction.RIGHT);
                this.sidebar.add(tab);

                if (firstTab == null)
                {
                    firstTab = tab;
                }
            }
        }

        Settings keybindsSettings = BBSMod.getSettings().modules.get("keybinds");
        if (keybindsSettings != null)
        {
            IKey label = L10n.lang(UIValueFactory.getTitleKey(keybindsSettings));
            UISettingsTab tab = new UISettingsTab(label, Icons.KEY_CAP, null, true, (t) -> this.selectKeybinds(t));
            tab.tooltip(label, Direction.RIGHT);
            this.sidebar.add(tab);

            if (firstTab == null)
            {
                firstTab = tab;
            }
        }

        if (firstTab != null)
        {
            if (firstTab.isKeybinds)
            {
                this.selectKeybinds(firstTab);
            }
            else
            {
                this.selectCategory(firstTab.categoryId, firstTab);
            }
        }
    }

    private Icon getCategoryIcon(String categoryId)
    {
        switch (categoryId)
        {
            case "appearance": return Icons.PROCESSOR;
            case "axes": return Icons.GRAPH;
            case "tutorials": return Icons.HELP;
            case "background": return Icons.IMAGE;
            case "chroma_sky": return Icons.GLOBE;
            case "scrollbars": return Icons.LIST;
            case "multiskin": return Icons.PLAYER;
            case "video": return Icons.VIDEO_CAMERA;
            case "editor": return Icons.CAMERA;
            case "recording": return Icons.PROPERTIES;
            case "model_blocks": return Icons.BLOCK;
            case "entity_selectors": return Icons.VISIBLE;
            case "dc": return Icons.SKULL;
            case "shader_curves": return Icons.CURVES;
            case "fluid_simulation": return Icons.FILTER;
            case "audio": return Icons.SOUND;
            case "cdn": return Icons.USER;
            default: return Icons.SETTINGS;
        }
    }

    public void selectCategory(String categoryId, UISettingsTab tab)
    {
        this.settings = BBSMod.getSettings().modules.get("bbs");
        this.selectedCategoryId = categoryId;
        this.isKeybindsSelected = false;

        if (this.currentTab != null)
        {
            this.currentTab.selected = false;
        }

        this.currentTab = tab;

        if (this.currentTab != null)
        {
            this.currentTab.selected = true;
        }

        this.refresh();
    }

    public void selectKeybinds(UISettingsTab tab)
    {
        this.settings = BBSMod.getSettings().modules.get("keybinds");
        this.selectedCategoryId = null;
        this.isKeybindsSelected = true;

        if (this.currentTab != null)
        {
            this.currentTab.selected = false;
        }

        this.currentTab = tab;

        if (this.currentTab != null)
        {
            this.currentTab.selected = true;
        }

        this.refresh();
    }

    public void refresh()
    {
        if (this.settings == null)
        {
            return;
        }

        this.options.removeAll();
        this.options.add(this.search.marginBottom(10));

        boolean first = true;
        String query = this.search.getText().trim().toLowerCase();

        for (ValueGroup category : this.settings.categories.values())
        {
            if (!category.isVisible())
            {
                continue;
            }

            if (this.selectedCategoryId != null && !category.getId().equals(this.selectedCategoryId))
            {
                continue;
            }

            String catTitleKey = UIValueFactory.getCategoryTitleKey(category);
            String catTooltipKey = UIValueFactory.getCategoryTooltipKey(category);
            boolean categoryMatches = query.isEmpty() || this.matchesQuery(query,
                L10n.lang(catTitleKey).get(),
                L10n.lang(catTooltipKey).get(),
                category.getId()
            );

            UILabel label = UI.label(L10n.lang(catTitleKey)).labelAnchor(0, 1).color(0xff000000 | BBSSettings.primaryColor.get()).background(() -> 0xFF1A1A22);
            List<UIElement> options = new ArrayList<>();

            label.tooltip(L10n.lang(catTooltipKey), Direction.BOTTOM);

            for (BaseValue value : category.getAll())
            {
                if (!value.isVisible())
                {
                    continue;
                }
                boolean valueMatches = categoryMatches || query.isEmpty() || this.matchesQuery(query,
                    L10n.lang(UIValueFactory.getValueLabelKey(value)).get(),
                    L10n.lang(UIValueFactory.getValueCommentKey(value)).get(),
                    value.getId()
                );

                if (!valueMatches)
                {
                    continue;
                }

                /* Populate interpolation labels for default interpolation settings on client side */
                if (value == BBSSettings.defaultInterpolation || value == BBSSettings.defaultPathInterpolation)
                {
                    try
                    {
                        List<IKey> interpKeys = new ArrayList<>();

                        for (String k : Interpolations.MAP.keySet())
                        {
                            interpKeys.add(UIKeys.C_INTERPOLATION.get(k));
                        }

                        if (value instanceof ValueInt)
                        {
                            ((ValueInt) value).modes(interpKeys.toArray(new IKey[0]));
                        }
                    }
                    catch (Throwable ignored) {}
                }

                if (value == BBSSettings.editorReplayHudPosition)
                {
                    if (value instanceof ValueInt)
                    {
                        String key = UIValueFactory.getValueLabelKey(value);

                        ((ValueInt) value).modes(
                            L10n.lang(key + ".top_left"),
                            L10n.lang(key + ".top_right"),
                            L10n.lang(key + ".bottom_left"),
                            L10n.lang(key + ".bottom_right")
                        );
                    }
                }

                List<UIElement> elements = UIValueMap.create(value, this);

                for (UIElement element : elements)
                {
                    options.add(element);
                }
            }

            if (options.isEmpty())
            {
                continue;
            }

            UIElement firstContainer = UI.column(5, 0, 20, label, options.remove(0)).marginTop(first ? 0 : 24);

            this.options.add(firstContainer);

            for (UIElement element : options)
            {
                this.options.add(element);
            }

            first = false;
        }

        this.resize();
    }

    private boolean matchesQuery(String query, String... values)
    {
        if (query.isEmpty())
        {
            return true;
        }

        for (String value : values)
        {
            if (value != null && value.toLowerCase().contains(query))
            {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void renderBackground(UIContext context)
    {
        // Main background
        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xFF141418);
        context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xFF2A2A35, 1);

        // Header Row
        int headerH = 20;
        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.y + headerH, 0xFF1A1A22);
        context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.y + headerH, 0xFF2A2A35, 1);

        // Left sidebar
        context.batcher.box(this.sidebar.area.x, this.sidebar.area.y, this.sidebar.area.ex(), this.sidebar.area.ey(), 0xFF111115);
        context.batcher.outline(this.sidebar.area.x, this.sidebar.area.y, this.sidebar.area.ex(), this.sidebar.area.ey(), 0xFF22222A, 1);

        // Resize handles
        int resizeColor = Colors.GRAY;
        int right = this.area.ex();
        int bottom = this.area.ey();
        context.batcher.box(right - 9, bottom - 1, right - 1, bottom, resizeColor);
        context.batcher.box(right - 1, bottom - 9, right, bottom - 1, resizeColor);
    }

    public static class UISettingsTab extends UIClickable<UISettingsTab>
    {
        public IKey label;
        public Icon icon;
        public boolean selected;
        public String categoryId;
        public boolean isKeybinds;

        public UISettingsTab(IKey label, Icon icon, String categoryId, boolean isKeybinds, Consumer<UISettingsTab> callback)
        {
            super(callback);

            this.label = label;
            this.icon = icon;
            this.categoryId = categoryId;
            this.isKeybinds = isKeybinds;
            this.h(20);
        }

        @Override
        protected UISettingsTab get()
        {
            return this;
        }

        @Override
        protected void renderSkin(UIContext context)
        {
            int bg = this.selected ? 0xFF1D1D26 : (this.hover ? 0xFF181820 : 0xFF111115);
            context.batcher.box(this.area.x + 2, this.area.y, this.area.ex() - 2, this.area.ey(), bg);

            if (this.selected)
            {
                context.batcher.box(this.area.x + 2, this.area.y, this.area.x + 5, this.area.ey(), 0xFF1976D2);
            }

            int textX = this.area.x + 8;
            int textColor = this.selected ? (0xff000000 | BBSSettings.primaryColor.get()) : 0xFFCCCCCC;

            if (this.icon != null)
            {
                context.batcher.icon(this.icon, textColor, this.area.x + 6, this.area.my() - 8);
                textX = this.area.x + 24;
            }

            int y = this.area.my(context.batcher.getFont().getHeight());

            context.batcher.clip(this.area.x, this.area.y, this.area.w - 6, this.area.h, context);
            context.batcher.textShadow(this.label.get(), textX, y, textColor);
            context.batcher.unclip(context);
        }
    }
}
