package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.BBS;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.client.CrossWorldFilmLoader;
import mchorse.bbs_mod.client.CrossWorldFilmScanner;
import mchorse.bbs_mod.client.FilmLaunchHelper;
import mchorse.bbs_mod.client.WorldLaunchHelper;
import mchorse.bbs_mod.film.CrossWorldFilmEntry;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.list.UIDataPathList;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.utils.UIRenderable;
import mchorse.bbs_mod.ui.home.UIHomePanel;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.DataPath;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.resources.Pixels;

import net.minecraft.client.MinecraftClient;

import org.lwjgl.opengl.GL11;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class UIWorldFilmsBrowserPanel extends UIDashboardPanel
{
    private static final int BANNER_HEIGHT = UIHomePanel.HOME_BANNER_HEIGHT;

    private final UIElement page;
    private final UIDataPathList filmsList;
    private final UIFilmMosaicGrid filmsMosaic;
    private final UISearchList<DataPath> filmsSearch;
    private final UIButton joinWorld;
    private final Map<String, CrossWorldFilmEntry> crossWorldFilmEntries = new HashMap<>();
    private final Map<String, String> crossWorldWorldLabels = new HashMap<>();
    private final Map<String, Long> worldLastPlayed = new HashMap<>();
    private final Map<String, Texture> thumbnails = new HashMap<>();
    private final Map<String, Texture> worldIconTextures = new HashMap<>();
    private final Set<String> missingThumbnailIds = new HashSet<>();
    private final Set<String> missingWorldIconIds = new HashSet<>();

    private CrossWorldFilmEntry pendingJoin;
    private boolean scanning;

    public UIWorldFilmsBrowserPanel(UIDashboard dashboard)
    {
        super(dashboard);

        this.page = new UIElement()
        {
            @Override
            protected boolean subMouseClicked(UIContext context)
            {
                UIWorldFilmsBrowserPanel.this.filmsMosaic.selectedId = null;
                UIWorldFilmsBrowserPanel.this.handleSelection(null);

                return super.subMouseClicked(context);
            }
        };

        this.filmsList = new UIDataPathList((list) ->
        {
            if (!list.isEmpty())
            {
                this.handleSelection(list.get(0).toString());
            }
        })
        {
            @Override
            protected boolean sortElements()
            {
                if (this.getPath().strings.isEmpty())
                {
                    this.list.sort((a, b) ->
                    {
                        if (a.toString().endsWith("/.."))
                        {
                            return -1;
                        }

                        if (b.toString().endsWith("/.."))
                        {
                            return 1;
                        }

                        if (a.folder && b.folder)
                        {
                            Long aTime = UIWorldFilmsBrowserPanel.this.worldLastPlayed.get(a.getLast());
                            Long bTime = UIWorldFilmsBrowserPanel.this.worldLastPlayed.get(b.getLast());

                            if (aTime != null && bTime != null)
                            {
                                int cmp = Long.compare(bTime, aTime);

                                if (cmp != 0)
                                {
                                    return cmp;
                                }
                            }
                        }

                        if (a.folder && !b.folder)
                        {
                            return -1;
                        }

                        if (b.folder && !a.folder)
                        {
                            return 1;
                        }

                        return mchorse.bbs_mod.utils.NaturalOrderComparator.compare(true, a.toString(), b.toString());
                    });

                    return true;
                }

                return super.sortElements();
            }
        };
        this.filmsList.setFileIcon(Icons.FILM);
        this.filmsList.background();
        this.filmsList.setVisible(false);

        this.filmsMosaic = new UIFilmMosaicGrid(
            this.filmsList,
            this::getThumbnail,
            this::getCardLabel,
            (id) -> this.handleSelection(id),
            (id) -> this.openFilm(id)
        );
        this.filmsMosaic.setFolderThumbnailProvider(this::getWorldFolderThumbnail);

        this.filmsSearch = new UISearchList<>(this.filmsList).label(UIKeys.GENERAL_SEARCH);
        this.filmsSearch.search.w(1F);

        Consumer<String> oldCallback = this.filmsSearch.search.callback;
        this.filmsSearch.search.callback = (str) ->
        {
            if (oldCallback != null)
            {
                oldCallback.accept(str);
            }

            this.filmsMosaic.filter(str);
        };

        this.joinWorld = new UIButton(UIKeys.FILM_JOIN_WORLD, (b) -> this.joinSelectedWorld());
        this.joinWorld.relative(this).x(1F, -12).y(1F, -28).anchor(1F, 1F).w(120).h(20);
        this.joinWorld.setVisible(false);

        this.page.relative(this).x(0.5F, -250).y(0).w(500).h(1F);
        this.filmsSearch.relative(this.page).x(0).y(BANNER_HEIGHT + 20).w(1F).h(20);
        this.filmsMosaic.relative(this.page).x(0).y(BANNER_HEIGHT + 40).w(1F).h(1F, -(BANNER_HEIGHT + 40 + 10));
        this.page.add(new UIRenderable(this::renderBanner), this.filmsSearch, this.filmsMosaic);

        this.add(this.page, this.joinWorld);
    }

    public static boolean isBrowserPanel(UIDashboardPanel panel)
    {
        return panel instanceof UIWorldFilmsBrowserPanel;
    }

    @Override
    public boolean needsBackground()
    {
        return false;
    }

    @Override
    public void appear()
    {
        super.appear();

        this.refreshFilms();
    }

    @Override
    public void disappear()
    {
        super.disappear();

        this.pendingJoin = null;
        this.joinWorld.setVisible(false);
    }

    private void refreshFilms()
    {
        if (this.scanning)
        {
            return;
        }

        this.scanning = true;

        CrossWorldFilmScanner.scanAsync().whenComplete((entries, error) ->
        {
            MinecraftClient.getInstance().execute(() ->
            {
                this.scanning = false;
                this.crossWorldFilmEntries.clear();
                this.crossWorldWorldLabels.clear();
                this.worldLastPlayed.clear();
                this.thumbnails.clear();
                this.worldIconTextures.clear();
                this.missingThumbnailIds.clear();
                this.missingWorldIconIds.clear();

                if (error != null || entries == null)
                {
                    if (error != null)
                    {
                        error.printStackTrace();
                    }

                    this.fillNames(Collections.emptyList());

                    return;
                }

                List<String> paths = new ArrayList<>();
                MinecraftClient client = MinecraftClient.getInstance();
                String currentWorldFolder = null;

                if (client.world != null && client.player != null)
                {
                    File worldFolder = BBSMod.getWorldFolder();

                    if (worldFolder != null)
                    {
                        currentWorldFolder = worldFolder.getName();
                    }
                }

                for (CrossWorldFilmEntry entry : entries)
                {
                    if (currentWorldFolder != null && currentWorldFolder.equals(entry.worldFolder))
                    {
                        continue;
                    }

                    String path = entry.worldFolder + "/" + entry.filmId;

                    paths.add(path);
                    this.crossWorldFilmEntries.put(path, entry);
                    this.crossWorldWorldLabels.put(entry.worldFolder, entry.worldLabel);
                    this.worldLastPlayed.put(entry.worldFolder, entry.lastPlayed);
                }

                this.fillNames(paths);
            });
        });
    }

    private void fillNames(List<String> paths)
    {
        DataPath selected = this.filmsList.getCurrentFirst();
        String current = selected != null && !selected.folder ? selected.toString() : null;

        this.filmsList.fill(paths);
        this.filmsMosaic.fill(paths, current);
    }

    private String getCardLabel(DataPath path)
    {
        if (path.getLast().equals(".."))
        {
            return "../";
        }

        if (path.folder && path.size() == 1)
        {
            String label = this.crossWorldWorldLabels.get(path.getLast());

            if (label != null)
            {
                return label + "/";
            }
        }

        return path.getLast();
    }

    private File getThumbnailFile(String listPath)
    {
        CrossWorldFilmEntry entry = this.crossWorldFilmEntries.get(listPath);
        String filmId = entry != null ? entry.filmId : listPath;

        if (filmId.endsWith("/"))
        {
            return null;
        }

        File crossWorldFile = CrossWorldFilmLoader.getFilmFile(
            entry != null ? entry.worldFolder : null,
            filmId
        );

        if (crossWorldFile != null)
        {
            File worldRoot = crossWorldFile.getParentFile().getParentFile().getParentFile();
            File worldThumbnail = new File(worldRoot, "config/bbs/thumbnails/films/" + filmId + ".png");

            if (worldThumbnail.exists())
            {
                return worldThumbnail;
            }
        }

        return new File(BBS.getGameFolder(), "config/bbs/thumbnails/films/" + filmId + ".png");
    }

    private Texture getThumbnail(String listPath)
    {
        if (listPath == null || listPath.isEmpty() || listPath.endsWith("/"))
        {
            return null;
        }

        CrossWorldFilmEntry entry = this.crossWorldFilmEntries.get(listPath);
        String cacheKey = entry != null ? entry.encodeKey() : listPath;
        Texture cached = this.thumbnails.get(cacheKey);

        if (cached != null)
        {
            return cached;
        }

        if (this.missingThumbnailIds.contains(cacheKey))
        {
            return null;
        }

        File file = this.getThumbnailFile(listPath);

        if (file == null || !file.exists())
        {
            this.missingThumbnailIds.add(cacheKey);

            return null;
        }

        try (FileInputStream stream = new FileInputStream(file))
        {
            Pixels pixels = Pixels.fromPNGStream(stream);

            if (pixels != null)
            {
                Texture texture = Texture.textureFromPixels(pixels, GL11.GL_LINEAR);

                this.thumbnails.put(cacheKey, texture);
                this.missingThumbnailIds.remove(cacheKey);

                return texture;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        this.missingThumbnailIds.add(cacheKey);

        return null;
    }

    private Texture getWorldFolderThumbnail(DataPath path)
    {
        if (path == null || !path.folder || path.size() != 1 || path.getLast().equals(".."))
        {
            return null;
        }

        String worldFolder = path.getLast();

        if (this.missingWorldIconIds.contains(worldFolder))
        {
            return null;
        }

        Texture cached = this.worldIconTextures.get(worldFolder);

        if (cached != null)
        {
            return cached;
        }

        File iconFile = CrossWorldFilmLoader.getWorldIconFile(worldFolder);

        if (iconFile == null || !iconFile.exists())
        {
            this.missingWorldIconIds.add(worldFolder);

            return null;
        }

        try (FileInputStream stream = new FileInputStream(iconFile))
        {
            Pixels pixels = Pixels.fromPNGStream(stream);

            if (pixels != null)
            {
                Texture texture = Texture.textureFromPixels(pixels, GL11.GL_LINEAR);

                this.worldIconTextures.put(worldFolder, texture);
                this.missingWorldIconIds.remove(worldFolder);

                return texture;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        this.missingWorldIconIds.add(worldFolder);

        return null;
    }

    private void handleSelection(String selected)
    {
        if (selected == null)
        {
            this.pendingJoin = null;
            this.joinWorld.setVisible(false);

            return;
        }

        DataPath path = this.filmsMosaic.findPath(selected);

        if (path != null && path.folder)
        {
            if (path.getLast().equals(".."))
            {
                this.filmsList.goTo(this.filmsList.getPath().getParent());
            }
            else
            {
                this.filmsList.goTo(path);
            }

            this.filmsMosaic.filter("");
            this.pendingJoin = null;
            this.updateJoinButton();

            return;
        }

        CrossWorldFilmEntry entry = this.crossWorldFilmEntries.get(selected);

        if (entry != null && !entry.filmId.isEmpty() && !entry.filmId.endsWith("/"))
        {
            this.pendingJoin = entry;
        }
        else
        {
            this.pendingJoin = null;
        }

        this.updateJoinButton();
    }

    private void updateJoinButton()
    {
        this.joinWorld.setVisible(this.canShowJoinWorld());
    }

    private boolean canShowJoinWorld()
    {
        if (this.pendingJoin == null || this.pendingJoin.filmId.isEmpty() || this.pendingJoin.filmId.endsWith("/"))
        {
            return false;
        }

        MinecraftClient client = MinecraftClient.getInstance();

        if (client.world != null && client.player != null)
        {
            return false;
        }

        return !WorldLaunchHelper.isCurrentWorld(client, this.pendingJoin.worldFolder);
    }

    private void joinSelectedWorld()
    {
        if (this.pendingJoin != null)
        {
            FilmLaunchHelper.launch(this.pendingJoin);
            this.pendingJoin = null;
            this.updateJoinButton();
        }
    }

    private void openFilm(String selected)
    {
        CrossWorldFilmEntry entry = this.crossWorldFilmEntries.get(selected);

        if (entry == null || entry.filmId.isEmpty() || entry.filmId.endsWith("/"))
        {
            return;
        }

        FilmLaunchHelper.openCrossWorldFilm(entry);
    }

    private void renderBanner(UIContext context)
    {
        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), Colors.setA(0x0b0b0b, 1F));

        UIHomePanel home = this.dashboard.getPanel(UIHomePanel.class);

        if (home != null)
        {
            home.renderCardAndBanners(context, this.page, this.page.area.x, UIKeys.FILM_HOME_WORLDS.get(), false);
        }
    }
}
