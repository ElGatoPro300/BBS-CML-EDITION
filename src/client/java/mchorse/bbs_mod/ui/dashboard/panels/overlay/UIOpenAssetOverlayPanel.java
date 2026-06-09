package mchorse.bbs_mod.ui.dashboard.panels.overlay;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.ContentType;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.list.UIDataPathList;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.model.UIModelPreviewRenderer;
import mchorse.bbs_mod.ui.utility.audio.UIAudioEditorPanel;
import mchorse.bbs_mod.ui.utils.UIDataUtils;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.DataPath;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Colors;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class UIOpenAssetOverlayPanel extends UIOverlayPanel
{
    private UIDashboard dashboard;
    private UIElement sidebar;
    private UIElement mainArea;
    private UISearchList<DataPath> searchList;
    private UIMosaicGrid mosaicGrid;
    private UIIcon viewToggle;
    
    private ContentType currentType;
    private boolean isMosaic = true;
    private String filter = "";

    public UIOpenAssetOverlayPanel(IKey title, UIDashboard dashboard)
    {
        super(title);

        this.dashboard = dashboard;

        // Sidebar
        this.sidebar = new UIElement();
        this.sidebar.relative(this.content).w(32).h(1F).column(4).stretch().padding(4);

        // Main Area
        this.mainArea = new UIElement();
        this.mainArea.relative(this.content).x(32).w(1F, -32).h(1F);

        UIDataPathList pathList = new UIDataPathList((selections) -> this.openAsset(selections.get(0).toString()));
        this.searchList = new UISearchList<>(pathList);
        this.searchList.relative(this.mainArea).x(10).y(10).w(1F, -50).h(20);
        this.searchList.search.callback = (s) -> 
        {
            this.searchList.list.filter(s);
            this.filter = s;
            this.mosaicGrid.filter(s);
        };
        this.searchList.label(UIKeys.GENERAL_SEARCH);

        // List View (manual placement since searchList is now only 20px high)
        this.searchList.list.relative(this.mainArea).x(10).y(35).w(1F, -20).h(1F, -45);

        // Grid View
        this.mosaicGrid = new UIMosaicGrid(this, (id) -> this.openAsset(id));
        this.mosaicGrid.relative(this.mainArea).x(10).y(35).w(1F, -20).h(1F, -45);

        // View Toggle
        this.viewToggle = new UIIcon(Icons.LIST, (b) -> this.toggleView());
        this.viewToggle.relative(this.mainArea).x(1F, -30).y(10).wh(20, 20);

        this.viewToggle.tooltip(IKey.raw("Toggle Grid/List View"), Direction.LEFT);

        this.addSidebarButton(Icons.FILM, UIKeys.FILM_TITLE, () -> this.switchType(ContentType.FILMS));
        this.addSidebarButton(Icons.PARTICLE, UIKeys.PANELS_PARTICLES, () -> this.switchType(ContentType.PARTICLES));
        this.addSidebarButton(Icons.PLAYER, UIKeys.MODELS_TITLE, () -> this.switchType(ContentType.MODELS));
        this.addSidebarButton(Icons.SOUND, UIKeys.AUDIO_TITLE, () -> this.switchAudio());

        this.mainArea.add(this.searchList, this.searchList.list, this.mosaicGrid, this.viewToggle);
        this.content.add(this.sidebar, this.mainArea);

        this.updateView();
        this.switchType(ContentType.FILMS);
    }

    private void addSidebarButton(Icon icon, IKey tooltip, Runnable callback)
    {
        UIIcon button = new UIIcon(icon, (b) -> callback.run());
        button.tooltip(tooltip, Direction.RIGHT);
        this.sidebar.add(button);
    }

    private void toggleView()
    {
        this.isMosaic = !this.isMosaic;
        this.updateView();
    }

    private void updateView()
    {
        this.mosaicGrid.setVisible(this.isMosaic);
        this.searchList.list.setVisible(!this.isMosaic);
        this.viewToggle.both(this.isMosaic ? Icons.LIST : Icons.GALLERY);
    }

    private void openAsset(String id)
    {
        if (this.dashboard.documentTabsBar != null)
        {
            this.dashboard.documentTabsBar.addOrActivate(this.currentType, id);
        }
        else if (this.currentType != null)
        {
            this.dashboard.setPanel(this.currentType.get(this.dashboard));
            this.currentType.get(this.dashboard).pickData(id);
        }
        else
        {
            mchorse.bbs_mod.ui.utility.audio.UIAudioEditorPanel panel = this.dashboard.getPanel(mchorse.bbs_mod.ui.utility.audio.UIAudioEditorPanel.class);

            this.dashboard.setPanel(panel);
            panel.openAudioFile(id);
        }

        this.close();
    }

    private void switchType(ContentType type)
    {
        this.currentType = type;
        UIDataUtils.requestNames(type, (names) -> 
        {
            ((UIDataPathList)this.searchList.list).fill(names);
            this.mosaicGrid.fill(names, type);
            this.mosaicGrid.filter(this.filter);
        });
    }

    private void switchAudio()
    {
        this.currentType = null;
        File folder = new File(BBSMod.getAssetsFolder(), "audio");
        List<String> names = new ArrayList<>();

        if (folder.exists() && folder.isDirectory())
        {
            this.collectAudioFiles(folder, "", names);
        }

        ((UIDataPathList) this.searchList.list).fill(names);
        this.mosaicGrid.fill(names, null);
        this.mosaicGrid.filter(this.filter);
    }

    private void collectAudioFiles(File dir, String relativePath, List<String> result)
    {
        File[] files = dir.listFiles();

        if (files == null)
        {
            return;
        }

        for (File file : files)
        {
            String name = file.getName().toLowerCase();

            if (file.isDirectory())
            {
                this.collectAudioFiles(file, relativePath + file.getName() + "/", result);
            }
            else if (file.isFile() && (name.endsWith(".wav") || name.endsWith(".ogg")))
            {
                result.add("assets:audio/" + relativePath + file.getName());
            }
        }
    }

    public static class UIMosaicGrid extends UIScrollView
    {
        private UIOpenAssetOverlayPanel parent;
        private List<String> ids = new ArrayList<>();
        private List<String> filteredIds = new ArrayList<>();
        private String filter = "";
        private ContentType type;
        private Consumer<String> callback;
        private int lastCols = -1;


        public UIMosaicGrid(UIOpenAssetOverlayPanel parent, Consumer<String> callback)
        {
            this.parent = parent;
            this.callback = callback;
        }

        public void fill(Collection<String> names, ContentType type)
        {
            this.ids.clear();
            for (String name : names)
            {
                if (!name.endsWith("/")) this.ids.add(name);
            }
            this.type = type;
            this.filter(this.filter);
        }

        public void filter(String filter)
        {
            this.filter = filter;
            this.filteredIds.clear();
            for (String id : this.ids)
            {
                if (filter.isEmpty() || id.toLowerCase().contains(filter.toLowerCase()))
                {
                    this.filteredIds.add(id);
                }
            }
            this.lastCols = -1;
            this.rebuild();
        }

        private void rebuild()
        {
            this.removeAll();
            int w = this.area.w;
            if (w <= 0) return;

            int cardW = 80;
            int cardH = 90;
            int gap = 10;
            int cols = Math.max(1, (w - gap) / (cardW + gap));

            int i = 0;
            for (String id : this.filteredIds)
            {
                int col = i % cols;
                int row = i / cols;

                UIAssetCard card = new UIAssetCard(this.parent, id, this.type, (b) -> this.callback.accept(id));
                card.relative(this).x(gap + col * (cardW + gap)).y(gap + row * (cardH + gap)).w(cardW).h(cardH);
                this.add(card);
                i++;
            }
            
            int rows = (int) Math.ceil(this.filteredIds.size() / (double) cols);
            this.scroll.scrollSize = rows * (cardH + gap) + gap;
            super.resize();
        }


        @Override
        public void resize()
        {
            super.resize();
            int cols = Math.max(1, (this.area.w - 10) / 90);
            if (cols != this.lastCols)
            {
                this.lastCols = cols;
                this.rebuild();
            }
        }
    }

    public static class UIAssetCard extends UIElement
    {
        private UIOpenAssetOverlayPanel parent;
        private String id;
        private ContentType type;
        private Consumer<UIButton> callback;

        public UIAssetCard(UIOpenAssetOverlayPanel parent, String id, ContentType type, Consumer<UIButton> callback)
        {
            this.parent = parent;
            this.id = id;
            this.type = type;
            this.callback = callback;

            if (type == ContentType.MODELS)
            {
                UIModelPreviewRenderer renderer = new UIModelPreviewRenderer();
                renderer.relative(this).w(1F).h(60);
                renderer.setModel(id);
                this.add(renderer);
            }
        }

        @Override
        public boolean subMouseClicked(UIContext context)
        {
            if (this.area.isInside(context) && context.mouseButton == 0)
            {
                this.callback.accept(null);
                return true;
            }
            return super.subMouseClicked(context);
        }

        @Override
        public void render(UIContext context)
        {
            int color = this.area.isInside(context) ? Colors.A50 | Colors.HIGHLIGHT : Colors.A25;
            context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), color);
            context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.y + 60, Colors.A25);
            
            // Icon or Preview
            if (this.type == ContentType.FILMS)
            {
                UIFilmPanel filmPanel = this.parent.dashboard.getPanel(UIFilmPanel.class);
                Texture thumbnail = filmPanel.getThumbnail(this.id);

                if (thumbnail != null)
                {
                    int w = this.area.w - 4;
                    int h = (int) (w * (thumbnail.height / (float) thumbnail.width));
                    if (h > 56)
                    {
                        h = 56;
                        w = (int) (h * (thumbnail.width / (float) thumbnail.height));
                    }
                    int x = this.area.x + 2 + (this.area.w - 4 - w) / 2;
                    int y = this.area.y + 2 + (56 - h) / 2;

                    context.batcher.fullTexturedBox(thumbnail, x, y, w, h);
                }
                else
                {
                    context.batcher.icon(Icons.FILM, this.area.mx() - 8, this.area.y + 22);
                }
            }
            else if (this.type == ContentType.PARTICLES)
            {
                context.batcher.icon(Icons.PARTICLE, this.area.mx() - 8, this.area.y + 22);
            }
            else if (this.type == ContentType.MODELS)
            {
                // Renderer is added as child
            }
            else if (this.type == null) // Audio
            {
                context.batcher.icon(Icons.SOUND, this.area.mx() - 8, this.area.y + 22);
            }
            else
            {
                context.batcher.icon(Icons.FILE, this.area.mx() - 8, this.area.y + 22);
            }

            // Label
            String label = this.id;
            if (label.contains("/")) label = label.substring(label.lastIndexOf("/") + 1);
            int labelW = context.batcher.getFont().getWidth(label);
            if (labelW > this.area.w - 4) label = context.batcher.getFont().limitToWidth(label, this.area.w - 10) + "...";
            
            context.batcher.textShadow(label, this.area.mx(context.batcher.getFont().getWidth(label)), this.area.ey() - 14);
            
            super.render(context);
        }
    }
}
