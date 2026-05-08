package mchorse.bbs_mod.ui.utility.audio;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.audio.SoundManager;
import mchorse.bbs_mod.audio.SoundPlayer;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.resources.packs.URLSourcePack;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UISidebarDashboardPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.navigation.UIControlBar;
import mchorse.bbs_mod.ui.framework.elements.navigation.UIIconTabButton;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIConfirmOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIPromptOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UISoundOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.UIRenderable;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.resources.Pixels;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;

import org.joml.Matrix4f;

import com.mojang.blaze3d.systems.RenderSystem;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.lwjgl.opengl.GL11;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class UIAudioEditorPanel extends UISidebarDashboardPanel
{
    public UIIcon pickAudio;
    public UIIcon plause;
    public UIIcon saveColors;
    public UIAudioEditor audioEditor;

    // Tab and Home fields
    public static final int AUDIO_DOCUMENT_TABS_HEIGHT = 20;

    private UIControlBar audioTabsBar;
    private UIElement audioTabs;
    private int activeAudioDocumentTab = -1;
    private final List<AudioDocumentTab> audioDocumentTabs = new ArrayList<>();

    private UIElement mainView;

    private UIElement homePage;
    private UISearchList<String> homeAudiosSearch;
    private UIStringList homeAudiosList;
    private UIElement homeActionsPanel;
    private UIButton homeOpenFolder;
    private UIButton homeRefreshList;
    private UIButton homeRenameCurrent;
    private UIButton homeDeleteCurrent;
    private String homeLastClickedAudio;
    private long homeLastClickTime;
    private boolean showingHomePage = true;

    // Homepage banners
    private final List<BannerEntry> homeBanners = new ArrayList<>();
    private static final Set<Link> prefetchingBanners = Collections.synchronizedSet(new HashSet<>());
    private final List<Integer> bannerSequence = new ArrayList<>();
    private int sequenceIndex = 0;
    private int bannerIndex = 0;
    private float lastBannerTicks = -1;

    private static final int HOME_BANNER_HEIGHT = 108;
    private static final int BANNER_DURATION = 140;
    private static final int BANNER_TRANSITION = 40;
    private static final String BANNERS_URL = "https://raw.githubusercontent.com/BBSCommunity/CML-NEWS/main/Banners_Panel/banners.json";

    public static class AudioDocumentTab
    {
        public boolean home;
        public Link audioLink;

        public AudioDocumentTab(boolean home, Link audioLink)
        {
            this.home = home;
            this.audioLink = audioLink;
        }
    }

    public static class BannerEntry
    {
        public String author;
        public String url;
        public transient Link link;
    }

    public UIAudioEditorPanel(UIDashboard dashboard)
    {
        super(dashboard);

        // Document tabs layout
        this.audioTabsBar = new UIControlBar();
        this.audioTabsBar.relative(this.editor).x(0).y(0).w(1F).h(AUDIO_DOCUMENT_TABS_HEIGHT);
        this.audioTabs = new UIElement();
        this.audioTabs.relative(this.audioTabsBar).x(8).y(0).w(1F, -16).h(AUDIO_DOCUMENT_TABS_HEIGHT).row(0).resize();
        this.audioTabsBar.add(this.audioTabs);

        this.mainView = new UIElement();
        this.mainView.relative(this.editor).y(AUDIO_DOCUMENT_TABS_HEIGHT).w(1F).h(1F, -AUDIO_DOCUMENT_TABS_HEIGHT);

        this.pickAudio = new UIIcon(Icons.MORE, (b) -> UIOverlay.addOverlay(this.getContext(), new UISoundOverlayPanel(this::pickAudioFromOverlay)));
        this.plause = new UIIcon(() ->
        {
            SoundPlayer player = this.audioEditor.getPlayer();

            if (player == null)
            {
                return Icons.STOP;
            }

            return player.isPlaying() ? Icons.PAUSE : Icons.PLAY;
        }, (b) -> this.audioEditor.togglePlayback());
        this.saveColors = new UIIcon(Icons.SAVED, (b) -> this.saveColors());
        this.audioEditor = new UIAudioEditor();
        this.audioEditor.full(this.mainView);

        this.mainView.add(this.audioEditor);

        this.iconBar.add(this.pickAudio, this.plause, this.saveColors);

        // Home dashboard layout
        this.homePage = new UIElement()
        {
            @Override
            protected boolean subMouseClicked(UIContext context)
            {
                UIAudioEditorPanel.this.homeAudiosList.deselect();
                UIAudioEditorPanel.this.handleHomeAudiosSelection(null);

                return super.subMouseClicked(context);
            }
        };

        this.homeActionsPanel = new UIElement();
        this.homeAudiosList = new UIStringList((list) -> this.handleHomeAudiosSelection(list));
        this.homeAudiosSearch = new UISearchList<>(this.homeAudiosList).label(UIKeys.GENERAL_SEARCH);
        this.homeAudiosSearch.list.background();

        this.homeOpenFolder = this.createHomeButton(L10n.lang("bbs.ui.audio.crud.open_folder"), Icons.FOLDER, (b) ->
        {
            UIUtils.openFolder(new File(BBSMod.getAssetsFolder(), "audio"));
        });

        this.homeRefreshList = this.createHomeButton(L10n.lang("bbs.ui.audio.crud.refresh"), Icons.REFRESH, (b) ->
        {
            this.requestNames();
        });

        this.homeRenameCurrent = this.createHomeButton(L10n.lang("bbs.ui.audio.crud.rename"), Icons.EDIT, (b) ->
        {
            String selected = this.getSelectedHomeAudio();

            if (selected == null)
            {
                return;
            }

            UIPromptOverlayPanel panel = new UIPromptOverlayPanel(
                L10n.lang("bbs.ui.audio.crud.rename"),
                IKey.raw(""),
                (str) ->
                {
                    if (str == null || str.trim().isEmpty()) return;

                    String ext = selected.endsWith(".wav") ? ".wav" : ".ogg";
                    String oldFileName = selected.replace("assets:audio/", "");
                    String newFileName = str.endsWith(ext) ? str : str + ext;

                    File oldFile = new File(BBSMod.getAssetsFolder(), "audio/" + oldFileName);
                    File newFile = new File(BBSMod.getAssetsFolder(), "audio/" + newFileName);

                    if (newFile.exists())
                    {
                        this.getContext().notifyError(IKey.raw("File already exists!"));
                        return;
                    }

                    if (oldFile.renameTo(newFile))
                    {
                        Link oldLink = Link.create(selected);
                        Link newLink = Link.create("assets:audio/" + newFileName);

                        for (AudioDocumentTab tab : this.audioDocumentTabs)
                        {
                            if (!tab.home && oldLink.equals(tab.audioLink))
                            {
                                tab.audioLink = newLink;
                            }
                        }

                        if (this.audioEditor.getAudio() != null && oldLink.equals(this.audioEditor.getAudio()))
                        {
                            this.audioEditor.setup(newLink);
                        }

                        this.rebuildAudioDocumentTabs();
                        this.requestNames();
                    }
                }
            );

            String baseName = selected.replace("assets:audio/", "");
            panel.text.setText(baseName);
            panel.text.filename();
            UIOverlay.addOverlay(this.getContext(), panel);
        });

        this.homeDeleteCurrent = this.createHomeButton(L10n.lang("bbs.ui.audio.crud.remove"), Icons.REMOVE, (b) ->
        {
            String selected = this.getSelectedHomeAudio();

            if (selected == null)
            {
                return;
            }

            UIConfirmOverlayPanel panel = new UIConfirmOverlayPanel(
                L10n.lang("bbs.ui.audio.crud.remove"),
                UIKeys.PANELS_MODALS_REMOVE,
                (bool) ->
                {
                    if (bool)
                    {
                        String fileName = selected.replace("assets:audio/", "");
                        File audioFile = new File(BBSMod.getAssetsFolder(), "audio/" + fileName);

                        if (audioFile.exists() && audioFile.delete())
                        {
                            Link targetLink = Link.create(selected);

                            if (BBSModClient.getSounds() != null)
                            {
                                BBSModClient.getSounds().stop(Link.assets("audio/" + fileName));
                            }

                            for (int i = this.audioDocumentTabs.size() - 1; i >= 0; i--)
                            {
                                AudioDocumentTab tab = this.audioDocumentTabs.get(i);
                                if (!tab.home && targetLink.equals(tab.audioLink))
                                {
                                    this.removeAudioDocumentTab(i);
                                }
                            }

                            this.requestNames();
                        }
                    }
                }
            );

            UIOverlay.addOverlay(this.getContext(), panel);
        });

        this.updateHomeButtonsState();

        this.homePage.relative(this.editor).x(0.5F, -250).y(AUDIO_DOCUMENT_TABS_HEIGHT).w(500).h(1F, -AUDIO_DOCUMENT_TABS_HEIGHT);
        this.homeActionsPanel.relative(this.homePage).x(0).y(HOME_BANNER_HEIGHT + 20).w(0.35F).h(1F, -(HOME_BANNER_HEIGHT + 20)).column(0).vertical().stretch();
        
        UIElement spacing = new UIElement();
        spacing.h(8);

        this.homeActionsPanel.add(this.homeOpenFolder, this.homeRefreshList, spacing, this.homeRenameCurrent, this.homeDeleteCurrent);
        this.homeAudiosSearch.relative(this.homePage).x(0.35F).y(HOME_BANNER_HEIGHT + 20).w(0.65F).h(1F, -(HOME_BANNER_HEIGHT + 20));
        this.homePage.add(new UIRenderable(this::renderHomeBackground), this.homeActionsPanel, this.homeAudiosSearch);

        this.editor.add(this.mainView, this.homePage, this.audioTabsBar);

        this.createHomeDocumentTab(true);
        this.openAudio(null);
        this.updateAudioDocumentView();

        this.initBanners();

        this.keys().register(Keys.PLAUSE, this.audioEditor::togglePlayback);
        this.keys().register(Keys.SAVE, this::saveColors);
        this.keys().register(Keys.OPEN_DATA_MANAGER, this.pickAudio::clickItself);
    }

    private void handleHomeAudiosSelection(List<String> selections)
    {
        String selected = selections == null || selections.isEmpty() ? null : selections.get(0);

        this.homeLastClickedAudio = selected;
        this.updateHomeButtonsState();

        if (selected != null)
        {
            long now = System.currentTimeMillis();

            if (now - this.homeLastClickTime < 250)
            {
                this.openAudioInDocumentTabs(Link.create(selected));
            }

            this.homeLastClickTime = now;
        }
    }

    private void updateHomeButtonsState()
    {
        String selected = this.getSelectedHomeAudio();
        boolean hasSelected = selected != null;

        this.homeRenameCurrent.setEnabled(hasSelected);
        this.homeDeleteCurrent.setEnabled(hasSelected);
    }

    private String getSelectedHomeAudio()
    {
        return this.homeAudiosList == null ? null : this.homeAudiosList.getCurrentFirst();
    }

    private void pickAudioFromOverlay(Link link)
    {
        if (link != null)
        {
            this.openAudioInDocumentTabs(link);
        }
    }

    private void createHomeDocumentTab(boolean activate)
    {
        this.audioDocumentTabs.add(new AudioDocumentTab(true, null));
        int index = this.audioDocumentTabs.size() - 1;

        this.rebuildAudioDocumentTabs();

        if (activate)
        {
            this.activateAudioDocumentTab(index, false);
        }
    }

    private void addHomeDocumentTab()
    {
        int insertAt = Math.max(0, this.activeAudioDocumentTab + 1);

        this.audioDocumentTabs.add(insertAt, new AudioDocumentTab(true, null));
        this.rebuildAudioDocumentTabs();
        this.activateAudioDocumentTab(insertAt, false);
    }

    private int findTabByAudio(Link link)
    {
        for (int i = 0; i < this.audioDocumentTabs.size(); i++)
        {
            AudioDocumentTab tab = this.audioDocumentTabs.get(i);

            if (!tab.home && link.equals(tab.audioLink))
            {
                return i;
            }
        }

        return -1;
    }

    private void openAudioInDocumentTabs(Link link)
    {
        if (link == null)
        {
            return;
        }

        int existingIndex = this.findTabByAudio(link);

        if (existingIndex >= 0)
        {
            this.activateAudioDocumentTab(existingIndex, true);
            return;
        }

        if (this.activeAudioDocumentTab < 0 || this.activeAudioDocumentTab >= this.audioDocumentTabs.size())
        {
            if (this.audioDocumentTabs.isEmpty())
            {
                this.audioDocumentTabs.add(new AudioDocumentTab(true, null));
            }
            this.activeAudioDocumentTab = 0;
        }

        AudioDocumentTab active = this.audioDocumentTabs.get(this.activeAudioDocumentTab);

        if (active.home)
        {
            active.home = false;
            active.audioLink = link;
            this.rebuildAudioDocumentTabs();
            this.activateAudioDocumentTab(this.activeAudioDocumentTab, true);
        }
        else
        {
            int insertAt = this.activeAudioDocumentTab + 1;
            this.audioDocumentTabs.add(insertAt, new AudioDocumentTab(false, link));
            this.rebuildAudioDocumentTabs();
            this.activateAudioDocumentTab(insertAt, true);
        }
    }

    private void activateAudioDocumentTab(int index, boolean loadAudio)
    {
        if (index < 0 || index >= this.audioDocumentTabs.size())
        {
            return;
        }

        this.activeAudioDocumentTab = index;

        AudioDocumentTab tab = this.audioDocumentTabs.get(index);

        if (tab.home)
        {
            this.updateAudioDocumentView();
        }
        else
        {
            if (loadAudio || this.audioEditor.getAudio() == null || !this.audioEditor.getAudio().equals(tab.audioLink))
            {
                this.openAudio(tab.audioLink);
            }
            else
            {
                this.updateAudioDocumentView();
            }
        }

        this.rebuildAudioDocumentTabs();
    }

    private void removeAudioDocumentTab(int index)
    {
        if (index < 0 || index >= this.audioDocumentTabs.size())
        {
            return;
        }

        this.audioDocumentTabs.remove(index);

        if (this.audioDocumentTabs.isEmpty())
        {
            this.audioDocumentTabs.add(new AudioDocumentTab(true, null));
            this.activeAudioDocumentTab = 0;
            this.rebuildAudioDocumentTabs();
            this.activateAudioDocumentTab(0, false);
            return;
        }

        if (index < this.activeAudioDocumentTab)
        {
            this.activeAudioDocumentTab--;
        }
        else if (index == this.activeAudioDocumentTab)
        {
            this.activeAudioDocumentTab = Math.max(0, Math.min(this.activeAudioDocumentTab, this.audioDocumentTabs.size() - 1));
        }

        this.rebuildAudioDocumentTabs();
        this.activateAudioDocumentTab(this.activeAudioDocumentTab, false);
    }

    private void rebuildAudioDocumentTabs()
    {
        this.audioTabs.removeAll();

        for (int i = 0; i < this.audioDocumentTabs.size(); i++)
        {
            int tabIndex = i;
            AudioDocumentTab tab = this.audioDocumentTabs.get(i);
            IKey title = tab.home ? L10n.lang("bbs.ui.audio.home.title") : IKey.constant(tab.audioLink.path);
            UIIconTabButton button = new UIIconTabButton(title, tab.home ? Icons.FOLDER : Icons.SOUND, (b) -> this.activateAudioDocumentTab(tabIndex, false));
            button.color(this.activeAudioDocumentTab == tabIndex ? BBSSettings.primaryColor.get() : 0x2d2d2d);
            button.w(tab.home ? 88 : 140).h(AUDIO_DOCUMENT_TABS_HEIGHT);

            if (!tab.home || this.audioDocumentTabs.size() > 1)
            {
                button.removable((b) -> this.removeAudioDocumentTab(tabIndex));
            }

            this.audioTabs.add(button);
        }

        UIIconTabButton add = new UIIconTabButton(IKey.constant(""), Icons.ADD, (b) -> this.addHomeDocumentTab());
        add.color(0x2d2d2d);
        add.background(false);
        add.w(24).h(AUDIO_DOCUMENT_TABS_HEIGHT);
        this.audioTabs.add(add);
        this.audioTabs.resize();
    }

    private void syncActiveDocumentTabWithData(Link link)
    {
        if (link != null)
        {
            if (this.activeAudioDocumentTab < 0 || this.activeAudioDocumentTab >= this.audioDocumentTabs.size())
            {
                this.audioDocumentTabs.add(new AudioDocumentTab(false, link));
                this.activeAudioDocumentTab = this.audioDocumentTabs.size() - 1;
            }
            else
            {
                AudioDocumentTab tab = this.audioDocumentTabs.get(this.activeAudioDocumentTab);
                if (tab.home)
                {
                    tab.home = false;
                    tab.audioLink = link;
                }
                else if (!link.equals(tab.audioLink))
                {
                    int existing = this.findTabByAudio(link);
                    if (existing >= 0)
                    {
                        this.activeAudioDocumentTab = existing;
                    }
                    else
                    {
                        tab.audioLink = link;
                    }
                }
            }
        }

        this.rebuildAudioDocumentTabs();
        this.updateAudioDocumentView();
    }

    private void updateAudioDocumentView()
    {
        boolean home = this.activeAudioDocumentTab < 0
            || this.activeAudioDocumentTab >= this.audioDocumentTabs.size()
            || this.audioDocumentTabs.get(this.activeAudioDocumentTab).home
            || this.audioEditor.getAudio() == null;

        this.showingHomePage = home;
        this.homePage.setVisible(home);
        this.mainView.setVisible(!home);

        this.updateHomeButtonsState();
    }

    private UIButton createHomeButton(IKey label, Icon icon, Consumer<UIButton> callback)
    {
        UIButton button = new UIButton(label, callback) {
            @Override
            protected void renderSkin(UIContext context)
            {
                int bg = this.hover ? Colors.setA(Colors.WHITE, 0.25F) : Colors.setA(0, 0.4F);
                this.area.render(context.batcher, bg);

                int color = this.isEnabled() ? Colors.LIGHTEST_GRAY : 0x88444444;

                if (icon != null) {
                    context.batcher.icon(icon, color, this.area.x + 4, this.area.y + this.area.h / 2 - icon.h / 2);
                }

                context.batcher.textShadow(this.label.get(), this.area.x + 22, this.area.y + this.area.h / 2 - 4, color);
            }
        };
        button.h(20);
        return button;
    }

    @Override
    public void requestNames()
    {
        List<String> entries = new ArrayList<>();
        Set<String> locations = getSoundEvents();
        entries.addAll(locations);
        entries.sort(null);

        this.homeAudiosList.clear();
        this.homeAudiosList.add(entries);
        this.updateHomeButtonsState();
    }

    private static Set<String> getSoundEvents()
    {
        Set<String> locations = new HashSet<>();

        for (Link link : BBSMod.getProvider().getLinksFromPath(Link.assets("audio")))
        {
            String pathLower = link.path.toLowerCase();
            boolean supported = pathLower.endsWith(".wav") || pathLower.endsWith(".ogg");

            if (supported)
            {
                locations.add(link.toString());
            }
        }

        return locations;
    }

    private void openAudio(Link link)
    {
        this.audioEditor.setup(link);
        this.saveColors.setEnabled(this.audioEditor.isEditing());
        this.syncActiveDocumentTabWithData(link);
    }

    private void saveColors()
    {
        Link audio = this.audioEditor.getAudio();
        SoundManager sounds = BBSModClient.getSounds();

        sounds.saveColorCodes(new Link(audio.source, audio.path + ".json"), this.audioEditor.getColorCodes());
        sounds.deleteSound(audio);
    }

    // Banners drawing/fetching logic
    private void initBanners()
    {
        BannerEntry home = new BannerEntry();
        home.author = "ElGatoPro300";
        home.link = Link.assets("textures/banners/films/Home.png");
        this.homeBanners.add(home);

        this.fetchRemoteBanners();
    }

    private void fetchRemoteBanners()
    {
        CompletableFuture.runAsync(() ->
        {
            try
            {
                HttpClient client = HttpClient.newBuilder().build();
                HttpRequest req = HttpRequest.newBuilder(URI.create(BANNERS_URL)).GET().build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

                if (resp.statusCode() == 200)
                {
                    List<BannerEntry> remote = new Gson().fromJson(resp.body(), new TypeToken<List<BannerEntry>>(){}.getType());
                    if (remote != null)
                    {
                        for (BannerEntry entry : remote)
                        {
                            entry.link = Link.create(entry.url);
                            this.prefetchBannerImage(entry.link);
                        }

                        MinecraftClient.getInstance().execute(() -> this.homeBanners.addAll(remote));
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        });
    }

    private void regenerateBannerSequence()
    {
        this.bannerSequence.clear();
        for (int i = 0; i < this.homeBanners.size(); i++)
        {
            this.bannerSequence.add(i);
        }
        this.shuffleRemoteBanners();
        this.sequenceIndex = 0;
        this.bannerIndex = 0; // Always start with local
    }

    private void shuffleRemoteBanners()
    {
        if (this.bannerSequence.size() > 2)
        {
            List<Integer> remote = this.bannerSequence.subList(1, this.bannerSequence.size());
            Collections.shuffle(remote);
        }
    }

    private void prefetchBannerImage(Link link)
    {
        if (link == null || link.source == null || !link.source.startsWith("http")) return;
        if (BBSModClient.getTextures().textures.get(link) != null) return;
        if (!prefetchingBanners.add(link)) return;

        CompletableFuture.runAsync(() ->
        {
            try (InputStream stream = URLSourcePack.downloadImage(link))
            {
                if (stream != null)
                {
                    Pixels pixels = Pixels.fromPNGStream(stream);
                    if (pixels != null)
                    {
                        RenderSystem.recordRenderCall(() ->
                        {
                            Texture texture = Texture.textureFromPixels(pixels, GL11.GL_LINEAR);
                            BBSModClient.getTextures().textures.put(link, texture);
                        });
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        });
    }

    private void drawBanner(UIContext context, BannerEntry entry, int x, int y, int w, int h, float alpha, float textAlpha, boolean drawStripe)
    {
        if (alpha < 0.001F && textAlpha < 0.001F) return;

        Link link = entry.link;
        Texture texture = link.source != null && link.source.startsWith("http") ? 
            BBSModClient.getTextures().textures.get(link) : 
            BBSModClient.getTextures().getTexture(link);

        if (texture != null)
        {
            float scale = Math.min(w / (float) texture.width, h / (float) texture.height);
            int tw = Math.max(1, Math.round(texture.width * scale));
            int th = Math.max(1, Math.round(texture.height * scale));
            int tx = x + (w - tw) / 2;
            int ty = y + (h - th) / 2;

            if (alpha > 0.001F)
            {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                context.batcher.texturedBox(texture, Colors.setA(Colors.WHITE, alpha), tx, ty, tw, th, 0, 0, texture.width, texture.height);
            }

            if (textAlpha > 0.001F && entry.author != null && !entry.author.isEmpty())
            {
                String label = UIKeys.FILM_HOME_BANNER_AUTHOR.format(entry.author).get();
                int lw = context.batcher.getFont().getWidth(label);
                
                int stripeH = 16;
                int stripeY = ty + th - stripeH - 6;
                int bx = tx + tw - lw - 6;

                if (drawStripe)
                {
                    context.batcher.box(bx - 6, stripeY, tx + tw, ty + th - 6, Colors.setA(0, textAlpha * 0.6F));
                }
                context.batcher.textShadow(label, bx, stripeY + (stripeH - 8) / 2, Colors.setA(Colors.WHITE, textAlpha));
            }
        }
    }

    private void renderHomeBackground(UIContext context)
    {
        if (!this.showingHomePage)
        {
            return;
        }

        int editorX = this.editor.area.x;
        int editorY = this.editor.area.y;
        int editorW = this.editor.area.w;
        int editorH = this.editor.area.h;
        int pageX = this.homePage.area.x;
        int pageY = this.homePage.area.y;
        int pageW = this.homePage.area.w;
        int pageH = this.homePage.area.h;
        int dividerX = this.homeAudiosSearch.area.x;

        // Render deeper background
        context.batcher.box(editorX, editorY, editorX + editorW, editorY + editorH, Colors.setA(0x0b0b0b, 1F));

        // Render Animated Aurora Effect
        int primary = BBSSettings.primaryColor.get();
        float tick = context.getTickTransition() * 0.015F;
        int segments = 40;
        float segW = editorW / (float) segments;

        Matrix4f matrix4f = context.batcher.getContext().getMatrices().peek().getPositionMatrix();
        BufferBuilder builder = Tessellator.getInstance().getBuffer();

        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        builder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        float[] yBot1 = new float[segments + 1];
        float[] yMid1 = new float[segments + 1];
        int[] cMid1 = new int[segments + 1];

        float[] yBot2 = new float[segments + 1];
        float[] yMid2 = new float[segments + 1];
        int[] cMid2 = new int[segments + 1];

        for (int i = 0; i <= segments; i++)
        {
            float nx = (float) i / segments;

            // Layer 1
            float w1 = (float) Math.sin(tick * 1.2F + nx * 8F);
            float w2 = (float) Math.sin(tick * 0.7F + nx * 15F);
            float w3 = (float) Math.cos(tick * 0.4F - nx * 12F);
            float comb1 = (w1 + w2 + w3) / 3F;

            float curtainYTop = editorY + editorH * 0.05F;
            float curtainYBot = editorY + editorH * 0.5F + comb1 * (editorH * 0.35F);
            if (curtainYBot < curtainYTop + 10) curtainYBot = curtainYTop + 10;

            float transitionY = curtainYBot - editorH * 0.3F;
            if (transitionY < curtainYTop) transitionY = curtainYTop;

            yBot1[i] = curtainYBot;
            yMid1[i] = transitionY;
            cMid1[i] = Colors.setA(primary, 0.15F + Math.max(0, comb1) * 0.2F);

            // Layer 2
            float w4 = (float) Math.sin(tick * 1.5F - nx * 10F);
            float w5 = (float) Math.cos(tick * 0.9F + nx * 18F);
            float comb2 = (w4 + w5) / 2F;

            float curtain2YTop = editorY + editorH * 0.15F;
            float curtain2YBot = editorY + editorH * 0.75F + comb2 * (editorH * 0.25F);
            if (curtain2YBot < curtain2YTop + 10) curtain2YBot = curtain2YTop + 10;

            float transition2Y = curtain2YBot - editorH * 0.25F;
            if (transition2Y < curtain2YTop) transition2Y = curtain2YTop;

            yBot2[i] = curtain2YBot;
            yMid2[i] = transition2Y;
            cMid2[i] = Colors.setA(Colors.mulRGB(primary, 0.8F), 0.1F + Math.max(0, comb2) * 0.15F);
        }

        int colTop = Colors.setA(primary, 0.0F);
        int colBot = Colors.setA(primary, 0.0F);
        float yTop1 = editorY + editorH * 0.05F;
        float yTop2 = editorY + editorH * 0.15F;

        for (int i = 0; i < segments; i++)
        {
            float x1 = editorX + i * segW;
            float x2 = editorX + (i + 1) * segW;

            // Layer 1 - Upper Quad (yTop1 -> yMid1)
            builder.vertex(matrix4f, x1, yTop1, 0).color(colTop).next();
            builder.vertex(matrix4f, x1, yMid1[i], 0).color(cMid1[i]).next();
            builder.vertex(matrix4f, x2, yMid1[i+1], 0).color(cMid1[i+1]).next();
            builder.vertex(matrix4f, x2, yTop1, 0).color(colTop).next();

            // Layer 1 - Lower Quad (yMid1 -> yBot1)
            builder.vertex(matrix4f, x1, yMid1[i], 0).color(cMid1[i]).next();
            builder.vertex(matrix4f, x1, yBot1[i], 0).color(colBot).next();
            builder.vertex(matrix4f, x2, yBot1[i+1], 0).color(colBot).next();
            builder.vertex(matrix4f, x2, yMid1[i+1], 0).color(cMid1[i+1]).next();

            // Layer 2 - Upper Quad (yTop2 -> yMid2)
            builder.vertex(matrix4f, x1, yTop2, 0).color(colTop).next();
            builder.vertex(matrix4f, x1, yMid2[i], 0).color(cMid2[i]).next();
            builder.vertex(matrix4f, x2, yMid2[i+1], 0).color(cMid2[i+1]).next();
            builder.vertex(matrix4f, x2, yTop2, 0).color(colTop).next();

            // Layer 2 - Lower Quad (yMid2 -> yBot2)
            builder.vertex(matrix4f, x1, yMid2[i], 0).color(cMid2[i]).next();
            builder.vertex(matrix4f, x1, yBot2[i], 0).color(colBot).next();
            builder.vertex(matrix4f, x2, yBot2[i+1], 0).color(colBot).next();
            builder.vertex(matrix4f, x2, yMid2[i+1], 0).color(cMid2[i+1]).next();
        }

        BufferRenderer.drawWithGlobalProgram(builder.end());

        // Drop shadow for the main page panel
        context.batcher.gradientHBox(pageX - 18, pageY, pageX, pageY + pageH, 0, Colors.setA(0x000000, 0.7F));
        context.batcher.gradientHBox(pageX + pageW, pageY, pageX + pageW + 18, pageY + pageH, Colors.setA(0x000000, 0.7F), 0);

        // Panel backgrounds
        context.batcher.box(pageX, pageY, pageX + pageW, pageY + pageH, Colors.setA(0x1e1e1e, 1F));

        // Background stripe drawing
        int bannerH = HOME_BANNER_HEIGHT;
        int stripeH = 16;
        int stripeY = pageY + bannerH - stripeH;

        float currentTicks = context.getTickTransition();
        if (this.lastBannerTicks < 0) this.lastBannerTicks = currentTicks - BANNER_TRANSITION;

        float elapsed = Math.max(0, currentTicks - this.lastBannerTicks);

        if (elapsed >= BANNER_DURATION)
        {
            if (this.homeBanners.size() > 1)
            {
                if (this.bannerSequence.size() != this.homeBanners.size())
                {
                    this.regenerateBannerSequence();
                }

                this.sequenceIndex++;
                if (this.sequenceIndex >= this.bannerSequence.size())
                {
                    this.sequenceIndex = 0;
                    this.shuffleRemoteBanners();
                }
                this.bannerIndex = this.bannerSequence.get(this.sequenceIndex);
            }
            this.lastBannerTicks = currentTicks;
            elapsed = 0;
        }

        float transition = 0F;
        float textTransitionPrev = 1F;
        float textTransitionCurr = 0F;

        if (elapsed < BANNER_TRANSITION && this.homeBanners.size() > 1)
        {
            transition = (float) Interpolations.CUBIC_INOUT.interpolate(1F, 0F, elapsed / (float) BANNER_TRANSITION);
            transition = Math.max(0F, Math.min(1F, transition));

            // Staggered text transition: new text waits 20 ticks (1 second) to start fading in
            textTransitionPrev = transition;
            float textElapsed = Math.max(0, elapsed - 20);
            textTransitionCurr = (float) Interpolations.CUBIC_INOUT.interpolate(0F, 1F, textElapsed / (float) (BANNER_TRANSITION - 20));
        }
        else
        {
            textTransitionCurr = 1F;
        }

        int prevIndex = this.bannerSequence.isEmpty() ? 0 : this.bannerSequence.get((this.sequenceIndex + this.bannerSequence.size() - 1) % this.bannerSequence.size());
        BannerEntry current = this.homeBanners.get(this.bannerIndex);
        BannerEntry prev = this.homeBanners.get(prevIndex);

        if (transition > 0.001F)
        {
            this.drawBanner(context, prev, pageX, pageY, pageW, bannerH, transition, textTransitionPrev, true);
            this.drawBanner(context, current, pageX, pageY, pageW, bannerH, 1F - transition, textTransitionCurr, true);
        }
        else
        {
            this.drawBanner(context, current, pageX, pageY, pageW, bannerH, 1F, textTransitionCurr, true);
        }

        int splitY = pageY + bannerH;
        context.batcher.box(pageX, splitY, pageX + pageW, splitY + 1, Colors.A12);
        context.batcher.box(dividerX, splitY + 1, dividerX + 1, pageY + pageH, Colors.A12);
        context.batcher.textShadow(L10n.lang("bbs.ui.audio.home.actions").get(), pageX + 4, splitY + 6);
        context.batcher.textShadow(L10n.lang("bbs.ui.audio.home.list").get(), dividerX + 4, splitY + 6);
    }
}