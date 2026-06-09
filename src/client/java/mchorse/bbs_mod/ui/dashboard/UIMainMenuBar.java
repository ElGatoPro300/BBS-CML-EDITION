package mchorse.bbs_mod.ui.dashboard;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.ui.ContentType;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.addons.UIAddonsPanel;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.dashboard.panels.UIDataDashboardPanel;
import mchorse.bbs_mod.ui.dashboard.panels.overlay.UIAboutOverlayPanel;
import mchorse.bbs_mod.ui.dashboard.panels.overlay.UIOpenAssetOverlayPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIPromptOverlayPanel;
import mchorse.bbs_mod.ui.supporters.UISupportersPanel;
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

    public UIMainMenuBar(UIDashboard dashboard)
    {
        this.dashboard = dashboard;

        this.h(16);

        UIElement brand = new UIElement()
        {
            @Override
            public void render(UIContext context)
            {
                String title = "BBS";
                int y = this.area.my(context.batcher.getFont().getHeight());

                context.batcher.textShadow(title, this.area.x, y, Colors.WHITE);
                super.render(context);
            }
        };
        brand.w(25).marginLeft(6);

        this.add(brand);
        this.add(new UIMenuButton(IKey.raw("File"), (b) -> this.openFileMenu()).w(28));
        this.add(new UIMenuButton(IKey.raw("Edit"), (b) -> this.openEditMenu()).w(28));
        this.add(new UIMenuButton(IKey.raw("Help"), (b) -> this.openHelpMenu()).w(28));

        this.row(2).preferred(999);
    }

    @Override
    public void render(UIContext context)
    {
        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xFF111111);
        context.batcher.box(this.area.x, this.area.ey() - 1, this.area.ex(), this.area.ey(), 0xFF222222);

        super.render(context);
    }

    private void openFileMenu()
    {
        this.getContext().replaceContextMenu((menu) ->
        {
            menu.action(Icons.ADD, IKey.raw("New"), () -> this.openNewMenu());
            menu.action(Icons.FOLDER, IKey.raw("Open"), () -> this.openOpenPopup());
            menu.action(Icons.TIME, IKey.raw("Recent"), () -> this.openRecentMenu());
            menu.action(Icons.SETTINGS, UIKeys.CONFIG_TITLE, () -> UIOverlay.addOverlayRight(this.getContext(), this.dashboard.settingsPanel, 240));
            menu.action(Icons.JOYSTICK, IKey.raw("Addons"), () -> this.dashboard.setPanel(this.dashboard.getPanel(UIAddonsPanel.class)));
        });
    }

    private void openNewMenu()
    {
        this.getContext().replaceContextMenu((menu) ->
        {
            menu.action(Icons.FILM, UIKeys.FILM_TITLE, () -> this.createNewAsset(ContentType.FILMS));
            menu.action(Icons.PARTICLE, UIKeys.PANELS_PARTICLES, () -> this.createNewAsset(ContentType.PARTICLES));
            menu.action(Icons.PLAYER, UIKeys.MODELS_TITLE, () -> this.createNewAsset(ContentType.MODELS));
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
        UIOverlay.addOverlay(this.getContext(), new UIOpenAssetOverlayPanel(IKey.raw("Open Asset"), this.dashboard), 400, 300);
    }

    private void openRecentMenu()
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
                menu.action(this.getIcon(entry.type), IKey.raw(entry.id), () ->
                {
                    UIDataDashboardPanel panel = entry.type.get(this.dashboard);

                    if (panel != null)
                    {
                        this.dashboard.setPanel(panel);
                        panel.pickData(entry.id);
                    }
                });
            }
        });
    }

    private Icon getIcon(ContentType type)
    {
        if (type == ContentType.FILMS) return Icons.FILM;
        if (type == ContentType.PARTICLES) return Icons.PARTICLE;
        if (type == ContentType.MODELS) return Icons.PLAYER;

        return Icons.NONE;
    }

    private void openEditMenu()
    {
        this.getContext().replaceContextMenu((menu) ->
        {
            menu.action(Icons.UNDO, UIKeys.CAMERA_EDITOR_KEYS_EDITOR_UNDO, () -> this.triggerKey(Keys.UNDO));
            menu.action(Icons.REDO, UIKeys.CAMERA_EDITOR_KEYS_EDITOR_REDO, () -> this.triggerKey(Keys.REDO));

        });
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

    private void openHelpMenu()
    {
        this.getContext().replaceContextMenu((menu) ->
        {
            menu.action(Icons.HELP, IKey.raw("About"), () -> UIOverlay.addOverlay(this.getContext(), new UIAboutOverlayPanel(IKey.raw("About"), this.dashboard), 200, 150));
            menu.action(Icons.HEART, IKey.raw("Credits"), () -> this.dashboard.setPanel(this.dashboard.getPanel(UISupportersPanel.class)));
        });
    }

    public static class UIMenuButton extends UIButton
    {
        private Icon icon;

        public UIMenuButton(IKey label, Consumer<UIButton> callback)
        {
            super(label, callback);
        }

        public UIMenuButton(Icon icon, Consumer<UIButton> callback)
        {
            super(IKey.EMPTY, callback);
            this.icon = icon;
        }

        @Override
        protected void renderSkin(UIContext context)
        {
            if (this.area.isInside(context))
            {
                context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), Colors.A25 | BBSSettings.primaryColor.get());
            }

            if (this.icon != null)
            {
                context.batcher.icon(this.icon, Colors.WHITE, this.area.mx() - 8, this.area.my() - 8);
            }
            else
            {
                int x = this.area.mx(context.batcher.getFont().getWidth(this.label.get()));
                int y = this.area.my(context.batcher.getFont().getHeight());

                context.batcher.textShadow(this.label.get(), x, y, Colors.WHITE);
            }
        }
    }
}
