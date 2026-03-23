package mchorse.bbs_mod.ui.news;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.news.NewsReadManager;
import mchorse.bbs_mod.news.PriorityAnnouncementStateManager;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.resources.packs.URLSourcePack;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.graphics.texture.TextureManager;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UISidebarDashboardPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.framework.elements.utils.UIText;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.NaturalOrderComparator;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.resources.Pixels;
import mchorse.bbs_mod.utils.Timer;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.opengl.GL11;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class UINewsPanel extends UISidebarDashboardPanel
{
    private static final String NEWS_URL = "https://raw.githubusercontent.com/BBSCommunity/CML-NEWS/refs/heads/main/News/news.json";
    private static final String PRIORITY_ANNOUNCEMENT_URL = "https://raw.githubusercontent.com/BBSCommunity/CML-NEWS/refs/heads/main/News/priority_announcement.json";

    private final UIUnreadNewsList list = new UIUnreadNewsList((items) -> this.showSelected());
    private final UISearchList<String> search = new UISearchList<>(this.list);
    private final UIScrollView content = UI.scrollView(6, 6);
    private final UIIcon reload = new UIIcon(Icons.REFRESH, (b) -> this.reload(false));

    private final Gson gson = new Gson();
    private final Type type = new TypeToken<List<NewsEntry>>(){}.getType();
    private final Type priorityType = new TypeToken<PriorityAnnouncement>(){}.getType();
    private List<NewsEntry> entries = new ArrayList<>();

    private static final NewsReadManager readManager = new NewsReadManager();
    private static final PriorityAnnouncementStateManager priorityAnnouncementStateManager = new PriorityAnnouncementStateManager();
    private static boolean hasUnread;
    private static UIIcon newsIcon;

    private static final Timer autoTimer = new Timer(60L * 60L * 1000L);
    private static boolean autoInitialized;
    private static boolean sessionInitialReloadDone;
    private static boolean sessionPriorityFetchStarted;
    private static boolean sessionPriorityFetchDone;
    private static PriorityAnnouncement pendingPriorityAnnouncement;
    private static final Set<Link> prefetchingImages = Collections.synchronizedSet(new HashSet<>());

    public UINewsPanel(UIDashboard dashboard)
    {
        super(dashboard);

        UILabel title = new UILabel(UIKeys.NEWS_TITLE);
        title.color(Colors.WHITE);
        title.relative(this.editor).x(10).y(10).h(12);

        this.list.background();
        this.list.bindIds(new ArrayList<>(), new HashSet<>());
        this.search.label(UIKeys.NEWS_SEARCH);
        this.search.relative(this.editor).x(10).y(26).w(220).h(1F, -36);

        this.content.relative(this.editor).x(250).y(10).w(1F, -260).h(1F, -20);

        this.editor.add(title, this.search, this.content);

        this.reload.tooltip(UIKeys.NEWS_RELOAD);
        this.iconBar.add(this.reload);
    }

    public static void attachIcon(UIIcon icon)
    {
        newsIcon = icon;
        if (newsIcon != null)
        {
            newsIcon.both(() -> hasUnread ? Icons.NEWS_UNREAD : Icons.NEWS);
        }

        if (!autoInitialized)
        {
            autoTimer.mark();
            autoInitialized = true;
        }
    }

    private static void updateIcon()
    {
        // Icon uses supplier bound in attachIcon
    }

    public static void tickAuto(UIDashboard dashboard)
    {
        if (!autoInitialized)
        {
            return;
        }

        if (!autoTimer.checkRepeat())
        {
            return;
        }

        UINewsPanel panel = dashboard.getPanel(UINewsPanel.class);

        if (panel != null)
        {
            panel.reload(true);
        }
    }

    public static void onDashboardOpened(UIDashboard dashboard)
    {
        UINewsPanel panel = dashboard.getPanel(UINewsPanel.class);

        if (panel == null)
        {
            return;
        }

        if (!sessionInitialReloadDone)
        {
            sessionInitialReloadDone = true;
            panel.reload(false);
        }

        if (!sessionPriorityFetchStarted)
        {
            sessionPriorityFetchStarted = true;
            panel.fetchPriorityAnnouncement();
        }
    }

    public static void tickPriorityAnnouncement(UIDashboard dashboard)
    {
        if (pendingPriorityAnnouncement == null)
        {
            return;
        }

        UIContext context = dashboard.context;

        if (context == null || UIOverlay.has(context))
        {
            return;
        }

        priorityAnnouncementStateManager.markShown(pendingPriorityAnnouncement.id);
        UIOverlay.addOverlay(context, new UIPriorityAnnouncementOverlayPanel(dashboard, pendingPriorityAnnouncement), 0.6F, 0.7F);
        pendingPriorityAnnouncement = null;
    }

    @Override
    public void requestNames()
    {
        this.reload(false);
    }

    private void reload(boolean fromAuto)
    {
        CompletableFuture.runAsync(() ->
        {
            try
            {
                List<String> oldIds = new ArrayList<>();

                for (NewsEntry e : this.entries)
                {
                    oldIds.add(e.id);
                }

                String json = null;

                try
                {
                    HttpClient client = HttpClient.newBuilder().build();
                    HttpRequest req = HttpRequest.newBuilder(URI.create(NEWS_URL))
                        .GET()
                        .build();
                    HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                    if (resp.statusCode() == 200)
                    {
                        json = resp.body();
                    }
                }
                catch (Exception ignored) {}

                if (json == null || json.isEmpty())
                {
                    this.entries = new ArrayList<>();
                }
                else
                {
                    List<NewsEntry> loaded = this.gson.fromJson(json, this.type);
                    this.entries = loaded != null ? loaded : new ArrayList<>();
                }

                hasUnread = !this.getUnreadIdsLocal().isEmpty();

                final boolean hasNewEntries;

                if (!fromAuto)
                {
                    hasNewEntries = false;
                }
                else
                {
                    List<String> newIds = new ArrayList<>();

                    for (NewsEntry e : this.entries)
                    {
                        newIds.add(e.id);
                    }

                    boolean tmpHasNew = false;

                    for (String id : newIds)
                    {
                        if (id != null && !oldIds.contains(id))
                        {
                            tmpHasNew = true;
                            break;
                        }
                    }

                    hasNewEntries = tmpHasNew;
                }

                prefetchImages(this.entries);

                MinecraftClient.getInstance().execute(() ->
                {
                    updateIcon();
                    this.populate();

                    if (hasNewEntries)
                    {
                        this.getContext().notifyInfo(UIKeys.NEWS_UPDATED);
                    }
                });
            }
            catch (Exception e)
            {
                e.printStackTrace();
                MinecraftClient.getInstance().execute(this::populate);
            }
        });
    }

    private void fetchPriorityAnnouncement()
    {
        CompletableFuture.runAsync(() ->
        {
            PriorityAnnouncement announcement = null;

            try
            {
                HttpClient client = HttpClient.newBuilder().build();
                HttpRequest req = HttpRequest.newBuilder(URI.create(PRIORITY_ANNOUNCEMENT_URL))
                    .GET()
                    .build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

                if (resp.statusCode() == 200 && resp.body() != null && !resp.body().isEmpty())
                {
                    announcement = this.gson.fromJson(resp.body(), this.priorityType);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            PriorityAnnouncement finalAnnouncement = announcement;

            MinecraftClient.getInstance().execute(() ->
            {
                sessionPriorityFetchDone = true;

                if (finalAnnouncement == null || !finalAnnouncement.isValid() || !finalAnnouncement.enabled)
                {
                    return;
                }

                if (!priorityAnnouncementStateManager.shouldShow(finalAnnouncement.id))
                {
                    return;
                }

                if (finalAnnouncement.image != null && !finalAnnouncement.image.isEmpty())
                {
                    prefetchImage(Link.create(finalAnnouncement.image));
                }

                pendingPriorityAnnouncement = finalAnnouncement;
            });
        });
    }

    private static void prefetchImages(List<NewsEntry> entries)
    {
        if (entries == null)
        {
            return;
        }

        for (NewsEntry entry : entries)
        {
            if (entry == null || entry.images == null)
            {
                continue;
            }

            for (String url : entry.images)
            {
                if (url == null || url.isEmpty())
                {
                    continue;
                }

                Link link = Link.create(url);
                prefetchImage(link);
            }
        }
    }

    private static void prefetchImage(Link link)
    {
        if (link == null || link.source == null || !link.source.startsWith("http"))
        {
            return;
        }

        TextureManager textures = BBSModClient.getTextures();

        if (textures.textures.get(link) != null)
        {
            return;
        }

        if (!prefetchingImages.add(link))
        {
            return;
        }

        CompletableFuture.runAsync(() ->
        {
            try
            {
                try (InputStream stream = URLSourcePack.downloadImage(link))
                {
                    if (stream == null)
                    {
                        return;
                    }

                    Pixels pixels = Pixels.fromPNGStream(stream);

                    if (pixels == null)
                    {
                        return;
                    }

                    RenderSystem.recordRenderCall(() ->
                    {
                        try
                        {
                            Texture texture = Texture.textureFromPixels(pixels, GL11.GL_NEAREST);

                            BBSModClient.getTextures().textures.put(link, texture);
                        }
                        catch (Exception exception)
                        {
                            exception.printStackTrace();
                        }
                    });
                }
            }
            catch (Exception exception)
            {
                exception.printStackTrace();
            }
            finally
            {
                prefetchingImages.remove(link);
            }
        });
    }

    private void populate()
    {
        this.list.clear();
        List<String> ids = new ArrayList<>();

        Collections.sort(this.entries, (a, b) ->
        {
            String da = a.date == null ? "" : a.date;
            String db = b.date == null ? "" : b.date;

            int cmp = db.compareTo(da); // más nuevo primero

            if (cmp != 0)
            {
                return cmp;
            }

            return NaturalOrderComparator.compare(true, a.title, b.title);
        });

        for (NewsEntry entry : this.entries)
        {
            this.list.add(entry.title);
            ids.add(entry.id);
        }

        this.list.bindIds(ids, getUnreadIdsLocal());
        this.list.setIndex(this.entries.isEmpty() ? -1 : 0);
        this.showSelected();
    }

    private void showSelected()
    {
        this.content.removeAll();

        int index = this.list.getIndex();
        if (index < 0 || index >= this.entries.size())
        {
            UILabel empty = new UILabel(UIKeys.NEWS_EMPTY);
            empty.color(Colors.LIGHTER_GRAY);
            this.content.add(empty);
            this.content.resize();
            return;
        }

        NewsEntry entry = this.entries.get(index);

        if (entry.id != null)
        {
            readManager.markRead(entry.id);
            hasUnread = !getUnreadIdsLocal().isEmpty();
            updateIcon();
            this.list.bindIds(collectIdsLocal(), getUnreadIdsLocal());
        }

        UILabel title = new UILabel(IKey.raw(entry.title));
        title.color(Colors.WHITE);
        title.h(16);

        String metaText = entry.date;
        if (entry.tags != null && !entry.tags.isEmpty())
        {
            metaText += "  •  " + String.join(", ", entry.tags);
        }

        UILabel meta = new UILabel(IKey.raw(metaText));
        meta.color(Colors.GRAY);
        meta.h(12);

        UIText body = new UIText(IKey.raw(entry.body));
        body.color(Colors.LIGHTER_GRAY, true).padding(0, 2).lineHeight(12);

        this.content.add(UI.column(6, title, meta, body).marginTop(6));

        if (entry.images != null)
        {
            Set<String> uniqueUrls = new LinkedHashSet<>(entry.images);

            for (String url : uniqueUrls)
            {
                if (url == null || url.isEmpty())
                {
                    continue;
                }

                Link link = Link.create(url);
                this.content.add(new UINewsImage(link));
            }
        }

        this.content.resize();
    }

    public static class NewsEntry
    {
        public String id;
        public String date;
        public String title;
        public String summary;
        public String body;
        public List<String> tags;
        public List<String> images;
    }

    public static class PriorityAnnouncement
    {
        public String id;
        public String title;
        public String body;
        public String image;
        public boolean enabled = true;
        public boolean showOpenNewsButton = true;
        public boolean showActionButton = true;
        public boolean hideOpenNewsButton;
        public boolean hideActionButton;
        public String actionLabel;
        public String actionUrl;

        public boolean isValid()
        {
            return this.id != null && !this.id.isEmpty() && this.title != null && !this.title.isEmpty();
        }

        public String getActionLabel()
        {
            return this.actionLabel == null || this.actionLabel.isEmpty() ? UIKeys.PRIORITY_ANNOUNCEMENT_OPEN_LINK.get() : this.actionLabel;
        }

        public boolean shouldShowOpenNewsButton()
        {
            return this.showOpenNewsButton && !this.hideOpenNewsButton;
        }

        public boolean shouldShowActionButton()
        {
            return this.showActionButton && !this.hideActionButton;
        }
    }

    private List<String> collectIdsLocal()
    {
        List<String> ids = new ArrayList<>();

        for (NewsEntry entry : this.entries)
        {
            ids.add(entry.id == null ? "" : entry.id);
        }

        return ids;
    }

    private Set<String> getUnreadIdsLocal()
    {
        Set<String> unread = new HashSet<>();

        for (NewsEntry entry : this.entries)
        {
            if (entry.id != null && !readManager.isRead(entry.id))
            {
                unread.add(entry.id);
            }
        }

        return unread;
    }

    public static class UINewsImage extends UIElement
    {
        private final Link link;
        private final int placeholderHeight;

        public UINewsImage(Link link)
        {
            this(link, 512);
        }

        public UINewsImage(Link link, int placeholderHeight)
        {
            this.link = link;
            this.placeholderHeight = placeholderHeight;
            this.h(placeholderHeight);
        }

        @Override
        public void render(UIContext context)
        {
            super.render(context);

            TextureManager textures = BBSModClient.getTextures();
            Texture texture;

            if (this.link.source != null && this.link.source.startsWith("http"))
            {
                texture = textures.textures.get(this.link);
            }
            else
            {
                texture = textures.getTexture(this.link);
            }

            if (texture == null)
            {
                long frame = System.currentTimeMillis() / 200L;
                int index = (int) (frame % 3L);
                float cx = this.area.mx();
                float cy = this.area.my() - 12;
                float scale = 2.25F;
                Icon icon;

                if (index == 0)
                {
                    icon = Icons.LOADING_BBS_1;
                }
                else if (index == 1)
                {
                    icon = Icons.LOADING_BBS_2;
                }
                else
                {
                    icon = Icons.LOADING_BBS_3;
                }

                float iw = icon.w * scale;
                float ih = icon.h * scale;

                Texture atlas = BBSModClient.getTextures().getTexture(icon.texture);
                context.batcher.texturedBox(
                    atlas,
                    Colors.WHITE,
                    cx - iw / 2F,
                    cy - ih / 2F,
                    iw,
                    ih,
                    icon.x,
                    icon.y,
                    icon.x + icon.w,
                    icon.y + icon.h,
                    icon.textureW,
                    icon.textureH
                );

                String loading = UIKeys.NEWS_IMAGE_LOADING.get();
                int lw = context.batcher.getFont().getWidth(loading);

                context.batcher.textShadow(loading, cx - lw / 2F, cy + ih / 2F + 4, Colors.LIGHTER_GRAY);

                return;
            }

            int targetHeight = Math.max(96, Math.round(this.area.w / (texture.width / (float) texture.height)));
            targetHeight = Math.max(96, Math.min(targetHeight, this.placeholderHeight));

            if (Math.abs(targetHeight - this.area.h) > 1)
            {
                this.h(targetHeight);
                UIElement parent = this.getParentContainer();

                if (parent != null)
                {
                    parent.resize();
                }
            }

            float w = this.area.w;
            float h = this.area.h;
            float ar = texture.width / (float) texture.height;

            if (w / h > ar)
            {
                w = h * ar;
            }
            else
            {
                h = w / ar;
            }

            float x = this.area.x + (this.area.w - w) / 2F;
            float y = this.area.y + (this.area.h - h) / 2F;

            context.batcher.fullTexturedBox(texture, x, y, w, h);
        }
    }
}
