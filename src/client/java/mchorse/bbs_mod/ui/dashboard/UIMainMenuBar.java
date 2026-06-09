package mchorse.bbs_mod.ui.dashboard;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.ui.ContentType;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.dashboard.panels.UIDataDashboardPanel;
import mchorse.bbs_mod.ui.dashboard.panels.overlay.UIAboutOverlayPanel;
import mchorse.bbs_mod.ui.dashboard.panels.overlay.UIOpenAssetOverlayPanel;
import mchorse.bbs_mod.ui.dashboard.utils.UIGraphPanel;
import mchorse.bbs_mod.ui.selectors.UISelectorsOverlayPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIPromptOverlayPanel;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.keys.KeyCombo;
import mchorse.bbs_mod.ui.utils.keys.Keybind;
import mchorse.bbs_mod.utils.RecentAssetsTracker;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.repos.IRepository;

import java.util.function.Consumer;

public class UIMainMenuBar extends UIElement
{
    private UIDashboard dashboard;
    UIMenuButton activeButton = null;

    public UIMainMenuBar(UIDashboard dashboard)
    {
        this.dashboard = dashboard;

        this.h(20);

        UIElement brand = new UIElement()
        {
            @Override
            public void render(UIContext context)
            {
                String title = "BBS";
                int y = this.area.my(context.batcher.getFont().getHeight());

                context.batcher.textShadow(title, this.area.x, y, 0xFFCCCCCC);
                super.render(context);
            }
        };
        brand.w(25).marginLeft(6);

        this.add(brand);
        this.add(new UIMenuButton(IKey.raw("File"), this, this::buildFileMenu).w(28));
        this.add(new UIMenuButton(IKey.raw("Edit"), this, this::buildEditMenu).w(28));
        this.add(new UIMenuButton(IKey.raw("Tools"), this, this::buildToolsMenu).w(32));
        this.add(new UIMenuButton(IKey.raw("Help"), this, this::buildHelpMenu).w(28));

        this.row(2).preferred(999);
    }

    @Override
    public void render(UIContext context)
    {
        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xFF111111);
        context.batcher.box(this.area.x, this.area.ey() - 1, this.area.ex(), this.area.ey(), 0xFF222222);

        super.render(context);
    }

    /* ------------------------------------------------------------------ */
    /* Menu open/close                                                       */
    /* ------------------------------------------------------------------ */

    void toggleMenu(UIMenuButton button, Consumer<ContextMenuManager> consumer)
    {
        UIContext context = this.getContext();

        context.closeContextMenu();
        this.activeButton = null;

        /* Use wasActiveLastFrame (captured in render, before events fire) so that
           the context menu closing itself first doesn't confuse the toggle check. */
        if (!button.wasActiveLastFrame)
        {
            this.openMenuBelow(button, consumer);
        }
    }

    void openMenuBelow(UIMenuButton button, Consumer<ContextMenuManager> consumer)
    {
        UIContext context = this.getContext();

        context.replaceContextMenu((menu) ->
        {
            consumer.accept(menu);
            menu.onClose((e) -> this.activeButton = null);
        });

        if (context.contextMenu != null)
        {
            context.contextMenu.getFlex().x.set(0, button.area.x);
            context.contextMenu.getFlex().y.set(0, button.area.ey());
            context.contextMenu.bounds(context.menu.overlay, 5);
            context.contextMenu.resize();
        }

        this.activeButton = button;
    }

    /* ------------------------------------------------------------------ */
    /* Menu builders                                                         */
    /* ------------------------------------------------------------------ */

    private void buildFileMenu(ContextMenuManager menu)
    {
        menu.action(Icons.ADD, IKey.raw("New"), () -> this.openNewSubmenu());
        menu.action(Icons.FOLDER, IKey.raw("Open"), () -> this.openOpenPopup());
        menu.action(Icons.TIME, IKey.raw("Recent"), () -> this.openRecentSubmenu());
        menu.action(Icons.SETTINGS, UIKeys.CONFIG_TITLE, () -> UIOverlay.addOverlay(this.getContext(), this.dashboard.settingsPanel, 520, 320));
        menu.action(Icons.JOYSTICK, UIKeys.ADDONS_TITLE, () -> UIOverlay.addOverlay(this.getContext(), this.dashboard.addonsPanel, 520, 320));
    }

    private void buildEditMenu(ContextMenuManager menu)
    {
        menu.action(Icons.UNDO, UIKeys.CAMERA_EDITOR_KEYS_EDITOR_UNDO, () -> this.triggerKey(Keys.UNDO));
        menu.action(Icons.REDO, UIKeys.CAMERA_EDITOR_KEYS_EDITOR_REDO, () -> this.triggerKey(Keys.REDO));
    }

    private void buildToolsMenu(ContextMenuManager menu)
    {
        menu.action(Icons.PROPERTIES, UIKeys.SELECTORS_TITLE, () ->
            UIOverlay.addOverlayRight(this.getContext(), new UISelectorsOverlayPanel(), 240));
        menu.action(Icons.GRAPH, UIKeys.GRAPH_TOOLTIP, () ->
            this.dashboard.setPanel(this.dashboard.getPanel(UIGraphPanel.class)));
    }

    private void buildHelpMenu(ContextMenuManager menu)
    {
        menu.action(Icons.HELP, IKey.raw("About"), () -> UIOverlay.addOverlay(this.getContext(), new UIAboutOverlayPanel(IKey.raw("About"), this.dashboard), 640, 520));
    }

    /* ------------------------------------------------------------------ */
    /* Submenu actions                                                       */
    /* ------------------------------------------------------------------ */

    private void openNewSubmenu()
    {
        this.getContext().replaceContextMenu((menu) ->
        {
            menu.action(Icons.FILM, UIKeys.FILM_TITLE, () -> this.createNewAsset(ContentType.FILMS));
            menu.action(Icons.PARTICLE, UIKeys.PANELS_PARTICLES, () -> this.createNewAsset(ContentType.PARTICLES));
            menu.action(Icons.PLAYER, UIKeys.MODELS_TITLE, () -> this.createNewAsset(ContentType.MODELS));
        });
    }

    private void openRecentSubmenu()
    {
        this.getContext().replaceContextMenu((menu) ->
        {
            if (RecentAssetsTracker.RECENT.isEmpty())
            {
                menu.action(Icons.NONE, IKey.raw("No recent assets"), () -> {});
                return;
            }

            for (RecentAssetsTracker.Entry entry : RecentAssetsTracker.RECENT)
            {
                menu.action(this.iconFor(entry.type), IKey.raw(entry.id), () ->
                {
                    UIDataDashboardPanel panel = entry.type != null ? entry.type.get(this.dashboard) : null;

                    if (panel != null)
                    {
                        this.dashboard.setPanel(panel);
                        panel.pickData(entry.id);
                    }
                });
            }
        });
    }

    private void createNewAsset(ContentType type)
    {
        UIPromptOverlayPanel panel = new UIPromptOverlayPanel(
            UIKeys.GENERAL_ADD,
            UIKeys.PANELS_MODALS_ADD,
            (name) ->
            {
                if (name.trim().isEmpty())
                {
                    return;
                }

                IRepository repository = type.getRepository();
                ValueGroup created = (ValueGroup) repository.create(name);

                if (created != null)
                {
                    repository.save(name, created.toData().asMap());
                }

                UIDataDashboardPanel dashboardPanel = type.get(this.dashboard);

                if (dashboardPanel != null)
                {
                    this.dashboard.setPanel(dashboardPanel);
                    dashboardPanel.pickData(name);
                }
            }
        );
        panel.text.filename();
        UIOverlay.addOverlay(this.getContext(), panel);
    }

    private void openOpenPopup()
    {
        UIOverlay.addOverlay(this.getContext(), new UIOpenAssetOverlayPanel(IKey.raw("Open Asset"), this.dashboard), 520, 320);
    }

    private void triggerKey(KeyCombo combo)
    {
        if (this.dashboard.panels.panel == null)
        {
            return;
        }

        for (Keybind keybind : this.dashboard.panels.panel.keys().keybinds)
        {
            if (combo.equals(keybind.getCombo()) && keybind.isActive())
            {
                keybind.callback.run();
                return;
            }
        }
    }

    private Icon iconFor(ContentType type)
    {
        if (type == ContentType.FILMS) return Icons.FILM;
        if (type == ContentType.PARTICLES) return Icons.PARTICLE;
        if (type == ContentType.MODELS) return Icons.PLAYER;
        if (type == ContentType.SOUNDS) return Icons.SOUND;

        return Icons.NONE;
    }

    /* ------------------------------------------------------------------ */
    /* Menu button                                                           */
    /* ------------------------------------------------------------------ */

    public static class UIMenuButton extends UIButton
    {
        final UIMainMenuBar bar;
        final Consumer<ContextMenuManager> menuConsumer;
        private boolean prevHover = false;

        /* Captured during render (before events fire) — used by toggleMenu to
           determine whether this button's menu was open when the click started. */
        boolean wasActiveLastFrame = false;

        public UIMenuButton(IKey label, UIMainMenuBar bar, Consumer<ContextMenuManager> menuConsumer)
        {
            super(label, null);

            this.bar = bar;
            this.menuConsumer = menuConsumer;
            this.callback = (b) -> this.bar.toggleMenu(this, this.menuConsumer);
        }

        @Override
        public void render(UIContext context)
        {
            this.wasActiveLastFrame = this.bar.activeButton == this;

            boolean nowHovered = this.area.isInside(context);

            /* Switch menus on hover when another menu is already open */
            if (nowHovered && !this.prevHover
                && this.bar.activeButton != null
                && this.bar.activeButton != this)
            {
                this.bar.openMenuBelow(this, this.menuConsumer);
            }

            this.prevHover = nowHovered;

            super.render(context);
        }

        @Override
        protected void renderSkin(UIContext context)
        {
            boolean active = this.bar.activeButton == this;
            boolean hovered = this.area.isInside(context);

            if (active)
            {
                context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(),
                    Colors.setA(BBSSettings.primaryColor.get(), 0.55F));
            }
            else if (hovered)
            {
                context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), Colors.A25);
            }

            int x = this.area.mx(context.batcher.getFont().getWidth(this.label.get()));
            int y = this.area.my(context.batcher.getFont().getHeight());

            context.batcher.textShadow(this.label.get(), x, y, Colors.WHITE);
        }
    }
}
