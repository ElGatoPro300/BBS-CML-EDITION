package mchorse.bbs_mod.ui.home;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.ContentType;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.navigation.UIControlBar;
import mchorse.bbs_mod.ui.framework.elements.navigation.UIIconTabButton;
import mchorse.bbs_mod.ui.model.UIModelPanel;
import mchorse.bbs_mod.ui.particles.UIParticleSchemePanel;
import mchorse.bbs_mod.ui.utility.audio.UIAudioEditorPanel;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.DataPath;

import java.util.ArrayList;
import java.util.List;

/**
 * Unified document tab bar that lives at the dashboard level.
 * Routes activations to the single registered editor instances rather than
 * embedding copies, which keeps state and parity intact across navigation paths.
 */
public class UIDocumentTabsBar extends UIControlBar
{
    public static final int HEIGHT = 20;

    private static final int HOME_TAB_WIDTH = 88;
    private static final int DOC_TAB_WIDTH = 122;
    private static final int ADD_TAB_WIDTH = 24;

    private final UIDashboard dashboard;
    private final UIElement tabs;
    private final List<DocumentTab> documentTabs = new ArrayList<>();
    private int activeTab = 0;

    public UIDocumentTabsBar(UIDashboard dashboard)
    {
        this.dashboard = dashboard;
        this.tabs = new UIElement();
        this.tabs.relative(this).x(8).y(0).w(1F, -16).h(HEIGHT).row(0).resize();
        this.add(this.tabs);

        this.documentTabs.add(DocumentTab.home());
        this.rebuild();
    }

    /* ------------------------------------------------------------------ */
    /* Public API                                                            */
    /* ------------------------------------------------------------------ */

    public void addOrActivate(ContentType type, String id)
    {
        int existing = this.find(type, id);

        if (existing >= 0)
        {
            this.activate(existing);

            return;
        }

        /* If currently on Home, convert that tab in place instead of stacking */
        if (this.activeTab >= 0 && this.activeTab < this.documentTabs.size())
        {
            DocumentTab current = this.documentTabs.get(this.activeTab);

            if (current.isHome)
            {
                current.isHome = false;
                current.type = type;
                current.id = id;
                this.activate(this.activeTab);
                this.rebuild();

                return;
            }
        }

        this.documentTabs.add(new DocumentTab(type, id));
        this.rebuild();
        this.activate(this.documentTabs.size() - 1);
    }

    public void activateHome()
    {
        for (int i = 0; i < this.documentTabs.size(); i++)
        {
            if (this.documentTabs.get(i).isHome)
            {
                this.activate(i);

                return;
            }
        }

        this.documentTabs.add(0, DocumentTab.home());
        this.rebuild();
        this.activate(0);
    }

    public DocumentTab getActiveDocumentTab()
    {
        if (this.activeTab < 0 || this.activeTab >= this.documentTabs.size()) return null;

        return this.documentTabs.get(this.activeTab);
    }

    /* ------------------------------------------------------------------ */
    /* Internals                                                             */
    /* ------------------------------------------------------------------ */

    private int find(ContentType type, String id)
    {
        for (int i = 0; i < this.documentTabs.size(); i++)
        {
            DocumentTab tab = this.documentTabs.get(i);

            if (tab.isHome) continue;
            if (tab.type != type) continue;
            if (id == null ? tab.id == null : id.equals(tab.id)) return i;
        }

        return -1;
    }

    private void rebuild()
    {
        this.tabs.removeAll();

        for (int i = 0; i < this.documentTabs.size(); i++)
        {
            int index = i;
            DocumentTab tab = this.documentTabs.get(i);
            UIIconTabButton button = new UIIconTabButton(this.titleOf(tab), this.iconOf(tab), (b) -> this.activate(index));

            button.color(this.activeTab == index ? BBSSettings.primaryColor.get() : 0x2d2d2d);
            button.w(tab.isHome ? HOME_TAB_WIDTH : DOC_TAB_WIDTH).h(HEIGHT);

            if (!tab.isHome || this.documentTabs.size() > 1)
            {
                button.removable((b) -> this.remove(index));
            }

            this.tabs.add(button);
        }

        UIIconTabButton add = new UIIconTabButton(IKey.raw(""), Icons.ADD, (b) -> this.addHomeTab());

        add.color(0x2d2d2d);
        add.background(false);
        add.w(ADD_TAB_WIDTH).h(HEIGHT);
        this.tabs.add(add);
        this.tabs.resize();
    }

    private void addHomeTab()
    {
        int insertAt = this.activeTab + 1;

        this.documentTabs.add(insertAt, DocumentTab.home());
        this.rebuild();
        this.activate(insertAt);
    }

    private void activate(int index)
    {
        if (index < 0 || index >= this.documentTabs.size()) return;

        this.activeTab = index;

        DocumentTab tab = this.documentTabs.get(index);
        UIDashboardPanel target = this.resolvePanel(tab);

        if (target != null && this.dashboard.getPanels().panel != target)
        {
            this.dashboard.setPanel(target);
        }

        if (!tab.isHome && tab.id != null)
        {
            this.loadAsset(tab);
        }

        this.rebuild();
    }

    private void remove(int index)
    {
        if (index < 0 || index >= this.documentTabs.size()) return;

        DocumentTab removed = this.documentTabs.get(index);

        /* Single-tab case: convert to Home rather than disappearing */
        if (this.documentTabs.size() == 1 && !removed.isHome)
        {
            removed.isHome = true;
            removed.type = null;
            removed.id = null;
            this.activate(0);

            return;
        }

        this.documentTabs.remove(index);

        if (this.documentTabs.isEmpty())
        {
            this.documentTabs.add(DocumentTab.home());
        }

        int newActive = Math.max(0, Math.min(this.activeTab, this.documentTabs.size() - 1));

        this.activeTab = -1;
        this.activate(newActive);
    }

    private UIDashboardPanel resolvePanel(DocumentTab tab)
    {
        if (tab.isHome) return this.dashboard.getPanel(UIHomePanel.class);
        if (tab.type == ContentType.FILMS) return this.dashboard.getPanel(UIFilmPanel.class);
        if (tab.type == ContentType.MODELS) return this.dashboard.getPanel(UIModelPanel.class);
        if (tab.type == ContentType.PARTICLES) return this.dashboard.getPanel(UIParticleSchemePanel.class);

        return this.dashboard.getPanel(UIAudioEditorPanel.class);
    }

    private void loadAsset(DocumentTab tab)
    {
        if (tab.type == ContentType.FILMS)
        {
            UIFilmPanel panel = this.dashboard.getPanel(UIFilmPanel.class);

            if (panel != null) panel.pickData(tab.id);
        }
        else if (tab.type == ContentType.MODELS)
        {
            UIModelPanel panel = this.dashboard.getPanel(UIModelPanel.class);

            if (panel != null) panel.pickData(tab.id);
        }
        else if (tab.type == ContentType.PARTICLES)
        {
            UIParticleSchemePanel panel = this.dashboard.getPanel(UIParticleSchemePanel.class);

            if (panel != null) panel.pickData(tab.id);
        }
        else
        {
            UIAudioEditorPanel panel = this.dashboard.getPanel(UIAudioEditorPanel.class);

            if (panel != null) panel.openAudioFile(tab.id);
        }
    }

    private IKey titleOf(DocumentTab tab)
    {
        if (tab.isHome) return IKey.raw("Home");
        if (tab.id != null) return IKey.raw(new DataPath(tab.id).getLast());
        if (tab.type == ContentType.FILMS) return UIKeys.FILM_TITLE;
        if (tab.type == ContentType.MODELS) return UIKeys.MODELS_TITLE;
        if (tab.type == ContentType.PARTICLES) return UIKeys.PANELS_PARTICLES;

        return UIKeys.AUDIO_TITLE;
    }

    private Icon iconOf(DocumentTab tab)
    {
        if (tab.isHome) return Icons.FOLDER;
        if (tab.type == ContentType.FILMS) return Icons.FILM;
        if (tab.type == ContentType.MODELS) return Icons.PLAYER;
        if (tab.type == ContentType.PARTICLES) return Icons.PARTICLE;

        return Icons.SOUND;
    }

    /* ------------------------------------------------------------------ */
    /* Tab record                                                            */
    /* ------------------------------------------------------------------ */

    public static class DocumentTab
    {
        public boolean isHome;
        public ContentType type;
        public String id;

        private DocumentTab(ContentType type, String id)
        {
            this.isHome = false;
            this.type = type;
            this.id = id;
        }

        private static DocumentTab home()
        {
            DocumentTab tab = new DocumentTab(null, null);

            tab.isHome = true;

            return tab;
        }
    }
}
