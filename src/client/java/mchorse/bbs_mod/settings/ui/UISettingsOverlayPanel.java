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
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.interps.Interpolations;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class UISettingsOverlayPanel extends UIOverlayPanel
{
    private static final int SIDEBAR_WIDTH = 120;

    public UIElement sidebar;
    public UIElement panel;
    public UIScrollView options;
    public UITextbox search;

    private Settings settings;
    private UISettingsTab currentTab;

    public UISettingsOverlayPanel()
    {
        super(UIKeys.CONFIG_TITLE);
        this.resizable();

        this.sidebar = new UIElement();
        this.sidebar.relative(this.content).x(0).y(0).w(SIDEBAR_WIDTH).h(1F);
        this.sidebar.column(2).vertical().stretch().scroll().padding(6);

        this.panel = new UIElement();
        this.panel.relative(this.content).x(SIDEBAR_WIDTH).y(0).w(1F, -SIDEBAR_WIDTH).h(1F);

        this.options = new UIScrollView(ScrollDirection.VERTICAL);
        this.options.scroll.scrollSpeed = 51;
        this.options.relative(this.panel).w(1F).h(1F);
        this.options.column().scroll().vertical().stretch().padding(10).height(20);

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

        String first = null;

        for (Settings settings : BBSMod.getSettings().modules.values())
        {
            String id = settings.getId();
            IKey label = L10n.lang(UIValueFactory.getTitleKey(settings));
            UISettingsTab tab = new UISettingsTab(label, settings.icon, (t) -> this.selectConfig(id, t));

            tab.tooltip(label, Direction.RIGHT);
            this.sidebar.add(tab);

            if (first == null)
            {
                first = id;
            }
        }

        if (first != null)
        {
            UISettingsTab firstTab = this.sidebar.getChildren(UISettingsTab.class).get(0);

            this.selectConfig(first, firstTab);
        }
    }

    public void selectConfig(String mod, UISettingsTab tab)
    {
        this.settings = BBSMod.getSettings().modules.get(mod);

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

            String catTitleKey = UIValueFactory.getCategoryTitleKey(category);
            String catTooltipKey = UIValueFactory.getCategoryTooltipKey(category);
            boolean categoryMatches = query.isEmpty() || this.matchesQuery(query,
                L10n.lang(catTitleKey).get(),
                L10n.lang(catTooltipKey).get(),
                category.getId()
            );

            UILabel label = UI.label(L10n.lang(catTitleKey)).labelAnchor(0, 1).background(() -> BBSSettings.primaryColor(Colors.A50));
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
        super.renderBackground(context);

        this.sidebar.area.render(context.batcher, Colors.A50);
        context.batcher.box(this.sidebar.area.ex(), this.sidebar.area.y, this.sidebar.area.ex() + 1, this.sidebar.area.ey(), Colors.A50);
    }

    public static class UISettingsTab extends UIClickable<UISettingsTab>
    {
        public IKey label;
        public Icon icon;
        public boolean selected;

        public UISettingsTab(IKey label, Icon icon, Consumer<UISettingsTab> callback)
        {
            super(callback);

            this.label = label;
            this.icon = icon;
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
            if (this.selected)
            {
                this.area.render(context.batcher, BBSSettings.primaryColor(Colors.A100));
            }
            else if (this.hover)
            {
                this.area.render(context.batcher, Colors.A50);
            }

            int textX = this.area.x + 6;

            if (this.icon != null)
            {
                context.batcher.icon(this.icon, Colors.WHITE, this.area.x + 4, this.area.my() - 8);
                textX = this.area.x + 22;
            }

            int y = this.area.my(context.batcher.getFont().getHeight());

            context.batcher.textShadow(this.label.get(), textX, y, Colors.WHITE);
        }
    }
}
