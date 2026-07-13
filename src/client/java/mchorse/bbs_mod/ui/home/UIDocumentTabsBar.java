package mchorse.bbs_mod.ui.home;

import mchorse.bbs_mod.film.CrossWorldFilmEntry;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.ContentType;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.dashboard.utils.UIGraphPanel;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.UIWorldFilmsBrowserPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.navigation.UIControlBar;
import mchorse.bbs_mod.ui.framework.elements.navigation.UIIconTabButton;
import mchorse.bbs_mod.ui.model.UIModelPanel;
import mchorse.bbs_mod.ui.particles.UIParticleSchemePanel;
import mchorse.bbs_mod.ui.utility.audio.UIAudioEditorPanel;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.DataPath;
import mchorse.bbs_mod.utils.RecentAssetsTracker;
import mchorse.bbs_mod.utils.colors.Colors;

import net.minecraft.client.MinecraftClient;

import org.lwjgl.glfw.GLFW;

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

    private int dragIndex = -1;
    private long dragTime = 0L;
    private int pendingDragIndex = -1;
    private int pendingDragX;
    private int pendingDragY;

    private static final long DRAG_DELAY_MS = 150L;
    private static final int DRAG_MOVE_THRESHOLD = 5;

    public UIDocumentTabsBar(UIDashboard dashboard)
    {
        this.dashboard = dashboard;
        this.tabs = new UIElement();
        this.tabs.relative(this).x(8).y(0).w(1F, -16).h(HEIGHT).row(0).resize();
        this.add(this.tabs);

        this.documentTabs.add(DocumentTab.home());
        this.rebuild();
    }

    @Override
    public void render(UIContext context)
    {
        this.updateTabDragPoll(context);

        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xFF141418);

        super.render(context);

        if (this.isDragging() && this.dragIndex >= 0 && this.dragIndex < this.documentTabs.size())
        {
            this.renderDraggedTab(context);
        }
    }

    @Override
    public boolean subMouseReleased(UIContext context)
    {
        if (this.isDragging())
        {
            this.finishDrag(context);

            return true;
        }

        this.clearDrag();

        return super.subMouseReleased(context);
    }

    /* ------------------------------------------------------------------ */
    /* Public API                                                            */
    /* ------------------------------------------------------------------ */

    public void addOrActivate(ContentType type, String id)
    {
        ContentType recentType = (type == null) ? ContentType.SOUNDS : type;
        if (recentType != ContentType.GRAPH)
        {
            RecentAssetsTracker.add(recentType, id);
        }

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

    public boolean isOpen(ContentType type, String id)
    {
        return this.find(type, id) >= 0;
    }

    public boolean activateIfOpen(ContentType type, String id)
    {
        int existing = this.find(type, id);

        if (existing < 0)
        {
            return false;
        }

        this.activate(existing);

        return true;
    }

    public void closeTab(ContentType type, String id)
    {
        int index = this.find(type, id);

        if (index >= 0)
        {
            this.remove(index);
        }
    }

    public void closeCrossWorldFilmTabs(String filmId)
    {
        if (filmId == null || filmId.isEmpty())
        {
            return;
        }

        for (int i = this.documentTabs.size() - 1; i >= 0; i--)
        {
            DocumentTab tab = this.documentTabs.get(i);

            if (tab.isHome || tab.type != ContentType.FILMS || tab.id == null)
            {
                continue;
            }

            CrossWorldFilmEntry decoded = CrossWorldFilmEntry.decodeKey(tab.id);

            if (decoded != null && filmId.equals(decoded.filmId))
            {
                this.remove(i);
            }
        }
    }

    public void renameTab(ContentType type, String oldId, String newId)
    {
        int index = this.find(type, oldId);

        if (index >= 0)
        {
            DocumentTab tab = this.documentTabs.get(index);
            tab.id = newId;
            this.rebuild();
        }
    }

    public void switchToType(ContentType type)
    {
        if (type == null)
        {
            this.activateHome();
            return;
        }

        int index = -1;
        for (int i = 0; i < this.documentTabs.size(); i++)
        {
            DocumentTab tab = this.documentTabs.get(i);
            if (!tab.isHome && tab.type == type)
            {
                index = i;
                break;
            }
        }

        if (index >= 0)
        {
            this.activate(index);
        }
        else
        {
            /* If currently on Home, convert that tab in place instead of stacking */
            if (this.activeTab >= 0 && this.activeTab < this.documentTabs.size())
            {
                DocumentTab current = this.documentTabs.get(this.activeTab);
                if (current.isHome)
                {
                    current.isHome = false;
                    current.type = type;
                    current.id = null;
                    this.activate(this.activeTab);
                    this.rebuild();
                    return;
                }
            }

            this.documentTabs.add(new DocumentTab(type, null));
            this.rebuild();
            this.activate(this.documentTabs.size() - 1);
        }
    }

    public void switchHomeType(ContentType type)
    {
        if (this.activeTab >= 0 && this.activeTab < this.documentTabs.size())
        {
            DocumentTab current = this.documentTabs.get(this.activeTab);
            if (current.isHome)
            {
                current.homeType = type;
                this.activate(this.activeTab);
                this.rebuild();
                return;
            }
        }

        int homeIndex = -1;
        for (int i = 0; i < this.documentTabs.size(); i++)
        {
            if (this.documentTabs.get(i).isHome)
            {
                homeIndex = i;
                break;
            }
        }

        if (homeIndex >= 0)
        {
            DocumentTab homeTab = this.documentTabs.get(homeIndex);
            homeTab.homeType = type;
            this.activate(homeIndex);
        }
        else
        {
            DocumentTab homeTab = DocumentTab.home();
            homeTab.homeType = type;
            this.documentTabs.add(0, homeTab);
            this.rebuild();
            this.activate(0);
        }
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

    public void cycle(int direction)
    {
        if (this.documentTabs.isEmpty())
        {
            return;
        }

        int size = this.documentTabs.size();
        int newIndex = (this.activeTab + direction + size) % size;

        this.activate(newIndex);
    }

    public boolean matchesActiveAsset(ContentType type, String assetId)
    {
        if (type == null || assetId == null || assetId.isEmpty())
        {
            return false;
        }

        DocumentTab tab = this.getActiveDocumentTab();

        if (tab == null || tab.isHome || tab.type != type || tab.id == null)
        {
            return false;
        }

        if (assetId.equals(tab.id))
        {
            return true;
        }

        if (type == ContentType.FILMS)
        {
            CrossWorldFilmEntry decoded = CrossWorldFilmEntry.decodeKey(tab.id);

            return decoded != null && assetId.equals(decoded.filmId);
        }

        return false;
    }

    public DocumentTab getActiveDocumentTab()
    {
        if (this.activeTab < 0 || this.activeTab >= this.documentTabs.size())
        {
            return null;
        }

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
            DocumentTabButton button = new DocumentTabButton(index, this.titleOf(tab), this.iconOf(tab));

            button.active(this.activeTab == index);
            button.w(tab.isHome ? HOME_TAB_WIDTH : DOC_TAB_WIDTH).h(HEIGHT);

            if (!tab.isHome || this.documentTabs.size() > 1)
            {
                button.removable((b) -> this.remove(index));
            }

            this.tabs.add(button);
        }

        UIIconTabButton add = new UIIconTabButton(IKey.raw(""), Icons.ADD, (b) -> this.addHomeTab());

        add.background(false);
        add.w(ADD_TAB_WIDTH).h(HEIGHT);
        this.tabs.add(add);
        this.tabs.resize();
    }

    private void moveTab(int from, int to)
    {
        if (from < 0 || to < 0 || from >= this.documentTabs.size() || to >= this.documentTabs.size() || from == to)
        {
            return;
        }

        DocumentTab tab = this.documentTabs.remove(from);

        this.documentTabs.add(to, tab);

        if (this.activeTab == from)
        {
            this.activeTab = to;
        }
        else if (from < this.activeTab && to >= this.activeTab)
        {
            this.activeTab -= 1;
        }
        else if (from > this.activeTab && to <= this.activeTab)
        {
            this.activeTab += 1;
        }

        this.rebuild();
    }

    private void armTabDrag(int index, int mouseX, int mouseY)
    {
        if (!this.isMouseOverTabBar(mouseX, mouseY))
        {
            return;
        }

        this.pendingDragIndex = index;
        this.pendingDragX = mouseX;
        this.pendingDragY = mouseY;
        this.dragIndex = -1;
        this.dragTime = 0L;
    }

    private boolean isMouseOverTabBar(int mouseX, int mouseY)
    {
        return mouseX >= this.area.x && mouseX < this.area.ex()
            && mouseY >= this.area.y && mouseY < this.area.ey();
    }

    private void updateTabDragPoll(UIContext context)
    {
        if (this.pendingDragIndex < 0 && this.dragIndex < 0)
        {
            return;
        }

        if (!Window.isMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_LEFT))
        {
            if (this.isDragging())
            {
                this.finishDrag(context);
            }
            else
            {
                this.clearDrag();
            }

            return;
        }

        if (this.dragIndex < 0 && this.pendingDragIndex >= 0)
        {
            /* Only commit a reorder drag while the cursor is still over the tab strip.
             * Otherwise a tab click followed by viewport interaction would drag the tab
             * across the whole editor. */
            if (!this.isMouseOverTabBar(context.mouseX, context.mouseY))
            {
                this.clearDrag();

                return;
            }

            int dx = context.mouseX - this.pendingDragX;

            if (Math.abs(dx) > DRAG_MOVE_THRESHOLD)
            {
                this.dragIndex = this.pendingDragIndex;
                this.dragTime = System.currentTimeMillis() - DRAG_DELAY_MS - 1L;
                this.pendingDragIndex = -1;
            }
        }
    }

    private boolean isDragging()
    {
        return this.dragIndex >= 0 && System.currentTimeMillis() - this.dragTime > DRAG_DELAY_MS;
    }

    private void finishDrag(UIContext context)
    {
        if (!this.isDragging())
        {
            this.clearDrag();

            return;
        }

        int target = this.getDropIndex(context.mouseX);

        if (target >= 0 && target != this.dragIndex)
        {
            this.moveTab(this.dragIndex, target);
        }

        this.clearDrag();
    }

    private void clearDrag()
    {
        this.dragIndex = -1;
        this.dragTime = 0L;
        this.pendingDragIndex = -1;
    }

    private int getDropIndex(int mouseX)
    {
        List<IUIElement> children = this.tabs.getChildren();
        int count = this.documentTabs.size();

        for (int i = 0; i < count; i++)
        {
            if (i >= children.size())
            {
                break;
            }

            IUIElement child = children.get(i);

            if (child instanceof UIElement element && mouseX < element.area.mx())
            {
                return i;
            }
        }

        return Math.max(0, count - 1);
    }

    private void renderDraggedTab(UIContext context)
    {
        List<IUIElement> children = this.tabs.getChildren();

        if (this.dragIndex < 0 || this.dragIndex >= children.size())
        {
            return;
        }

        IUIElement child = children.get(this.dragIndex);

        if (!(child instanceof UIIconTabButton button))
        {
            return;
        }

        int w = button.area.w;
        int h = button.area.h;
        int x = context.mouseX - w / 2;
        int y = this.area.y;

        context.batcher.box(x, y, x + w, y + h, 0xCC2A2A30);
        context.batcher.outline(x, y, x + w, y + h, 0xFF15151A);

        DocumentTab tab = this.documentTabs.get(this.dragIndex);
        Icon icon = this.iconOf(tab);
        String label = this.titleOf(tab).get();

        if (icon != null)
        {
            context.batcher.icon(icon, Colors.WHITE, x + 8, y + (h - icon.h) / 2);
        }

        if (label != null && !label.isEmpty())
        {
            int textX = x + 8 + (icon == null ? 0 : icon.w + 5);
            int textY = y + (h - context.batcher.getFont().getHeight()) / 2;

            context.batcher.text(label, textX, textY, Colors.WHITE);
        }
    }

    private class DocumentTabButton extends UIIconTabButton
    {
        private static final int REMOVE_GUTTER = 22;

        private final int tabIndex;

        public DocumentTabButton(int tabIndex, IKey label, Icon icon)
        {
            super(label, icon, (b) -> UIDocumentTabsBar.this.activate(tabIndex));

            this.tabIndex = tabIndex;
        }

        @Override
        public boolean subMouseClicked(UIContext context)
        {
            boolean result = super.subMouseClicked(context);

            if (result && context.mouseButton == 0 && !this.isRemoveHit(context))
            {
                UIDocumentTabsBar.this.armTabDrag(this.tabIndex, context.mouseX, context.mouseY);
            }

            return result;
        }

        @Override
        public boolean subMouseReleased(UIContext context)
        {
            if (UIDocumentTabsBar.this.isDragging())
            {
                UIDocumentTabsBar.this.finishDrag(context);
                this.pressed = false;

                return true;
            }

            UIDocumentTabsBar.this.clearDrag();

            return super.subMouseReleased(context);
        }

        @Override
        protected void renderSkin(UIContext context)
        {
            if (UIDocumentTabsBar.this.isDragging() && UIDocumentTabsBar.this.dragIndex == this.tabIndex)
            {
                int x1 = this.area.x;
                int y1 = this.area.y;
                int x2 = this.area.ex();
                int y2 = this.area.ey();

                context.batcher.box(x1, y1, x2, y2, 0x6626262C);
                this.renderLockedArea(context);

                return;
            }

            super.renderSkin(context);
        }

        private boolean isRemoveHit(UIContext context)
        {
            return this.isRemovable() && this.area.isInside(context) && context.mouseX >= this.area.ex() - REMOVE_GUTTER;
        }
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

        if (this.activeTab >= 0 && this.activeTab < this.documentTabs.size() && this.activeTab != index)
        {
            DocumentTab leaving = this.documentTabs.get(this.activeTab);

            if (!leaving.isHome && leaving.type == ContentType.FILMS && leaving.id != null)
            {
                UIFilmPanel panel = this.dashboard.getPanel(UIFilmPanel.class);

                if (panel != null)
                {
                    panel.save();
                }
            }
        }

        this.activeTab = index;

        DocumentTab tab = this.documentTabs.get(index);

        if (tab.isHome && tab.homeType == null)
        {
            UIDashboardPanel current = this.dashboard.getPanels().panel;
            MinecraftClient client = MinecraftClient.getInstance();

            if (client.world == null || client.player == null)
            {
                if (current != null && !(current instanceof UIHomePanel) && !(current instanceof UIWorldFilmsBrowserPanel))
                {
                    tab.homeType = this.contentTypeOf(current);
                }
            }
        }

        UIDashboardPanel target = this.resolvePanel(tab);

        if (target != null && this.dashboard.getPanels().panel != target)
        {
            this.dashboard.setPanel(target);
        }

        if (!tab.isHome && tab.id != null)
        {
            ContentType recentType = (tab.type == null) ? ContentType.SOUNDS : tab.type;
            if (recentType != ContentType.GRAPH)
            {
                RecentAssetsTracker.add(recentType, tab.id);
            }

            this.loadAsset(tab);
        }
        else if (tab.isHome && target != null)
        {
            target.showHomeView();
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
        if (tab.isHome)
        {
            if (tab.homeType != null)
            {
                return this.panelForType(tab.homeType);
            }

            return this.dashboard.getPanel(UIHomePanel.class);
        }
        if (tab.type == ContentType.FILMS) return this.dashboard.getPanel(UIFilmPanel.class);
        if (tab.type == ContentType.MODELS) return this.dashboard.getPanel(UIModelPanel.class);
        if (tab.type == ContentType.PARTICLES) return this.dashboard.getPanel(UIParticleSchemePanel.class);
        if (tab.type == ContentType.GRAPH) return this.dashboard.getPanel(UIGraphPanel.class);

        return this.dashboard.getPanel(UIAudioEditorPanel.class);
    }

    private UIDashboardPanel panelForType(ContentType type)
    {
        if (type == ContentType.FILMS) return this.dashboard.getPanel(UIFilmPanel.class);
        if (type == ContentType.MODELS) return this.dashboard.getPanel(UIModelPanel.class);
        if (type == ContentType.PARTICLES) return this.dashboard.getPanel(UIParticleSchemePanel.class);
        if (type == ContentType.GRAPH) return this.dashboard.getPanel(UIGraphPanel.class);
        if (type == ContentType.SOUNDS) return this.dashboard.getPanel(UIAudioEditorPanel.class);

        return this.dashboard.getPanel(UIHomePanel.class);
    }

    private ContentType contentTypeOf(UIDashboardPanel panel)
    {
        if (panel instanceof UIFilmPanel) return ContentType.FILMS;
        if (panel instanceof UIModelPanel) return ContentType.MODELS;
        if (panel instanceof UIParticleSchemePanel) return ContentType.PARTICLES;
        if (panel instanceof UIGraphPanel) return ContentType.GRAPH;
        if (panel instanceof UIAudioEditorPanel) return ContentType.SOUNDS;

        return null;
    }

    private void loadAsset(DocumentTab tab)
    {
        if (tab.type == ContentType.FILMS)
        {
            UIFilmPanel panel = this.dashboard.getPanel(UIFilmPanel.class);

            if (panel != null && !panel.isFilmTabLoaded(tab.id))
            {
                panel.openFilmTab(tab.id);
            }
        }
        else if (tab.type == ContentType.MODELS)
        {
            UIModelPanel panel = this.dashboard.getPanel(UIModelPanel.class);

            if (panel != null && (panel.getData() == null || !tab.id.equals(panel.getData().getId())))
            {
                panel.pickData(tab.id);
            }
        }
        else if (tab.type == ContentType.PARTICLES)
        {
            UIParticleSchemePanel panel = this.dashboard.getPanel(UIParticleSchemePanel.class);

            if (panel != null && (panel.getData() == null || !tab.id.equals(panel.getData().getId())))
            {
                panel.pickData(tab.id);
            }
        }
        else if (tab.type == ContentType.GRAPH)
        {
            // Graph has no asset file to load
        }
        else
        {
            UIAudioEditorPanel panel = this.dashboard.getPanel(UIAudioEditorPanel.class);

            if (panel != null && (panel.audioEditor.getAudio() == null || !tab.id.equals(panel.audioEditor.getAudio().toString())))
            {
                panel.openAudioFile(tab.id);
            }
        }
    }

    private IKey titleOf(DocumentTab tab)
    {
        if (tab.isHome) return UIKeys.RAW_HOME;
        if (tab.type == ContentType.GRAPH) return UIKeys.GRAPH_TOOLTIP;

        if (tab.id != null)
        {
            CrossWorldFilmEntry crossWorld = CrossWorldFilmEntry.decodeKey(tab.id);

            if (crossWorld != null && tab.type == ContentType.FILMS)
            {
                UIFilmPanel panel = this.dashboard.getPanel(UIFilmPanel.class);
                CrossWorldFilmEntry resolved = panel != null ? panel.resolveCrossWorldEntryForTab(tab.id) : null;

                if (resolved != null)
                {
                    return IKey.raw(resolved.getDisplayLabel());
                }

                return IKey.raw(crossWorld.getDisplayLabel());
            }

            return IKey.raw(new DataPath(tab.id).getLast());
        }

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
        if (tab.type == ContentType.GRAPH) return Icons.GRAPH;

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
        public ContentType homeType;

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
