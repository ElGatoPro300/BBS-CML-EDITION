package mchorse.bbs_mod.ui.particles;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.renderers.ParticleFormRenderer;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.math.molang.expressions.MolangExpression;
import mchorse.bbs_mod.particles.ParticleScheme;
import mchorse.bbs_mod.particles.emitter.ParticleEmitter;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.resources.packs.URLSourcePack;
import mchorse.bbs_mod.ui.ContentType;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.list.UIDataPathList;
import mchorse.bbs_mod.ui.dashboard.panels.UIDataDashboardPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextEditor;
import mchorse.bbs_mod.ui.framework.elements.navigation.UIControlBar;
import mchorse.bbs_mod.ui.framework.elements.navigation.UIIconTabButton;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIConfirmOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIPromptOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.UIRenderable;
import mchorse.bbs_mod.ui.particles.sections.UIParticleSchemeAppearanceSection;
import mchorse.bbs_mod.ui.particles.sections.UIParticleSchemeCollisionSection;
import mchorse.bbs_mod.ui.particles.sections.UIParticleSchemeCurvesSection;
import mchorse.bbs_mod.ui.particles.sections.UIParticleSchemeExpirationSection;
import mchorse.bbs_mod.ui.particles.sections.UIParticleSchemeGeneralSection;
import mchorse.bbs_mod.ui.particles.sections.UIParticleSchemeInitializationSection;
import mchorse.bbs_mod.ui.particles.sections.UIParticleSchemeLifetimeSection;
import mchorse.bbs_mod.ui.particles.sections.UIParticleSchemeLightingSection;
import mchorse.bbs_mod.ui.particles.sections.UIParticleSchemeMotionSection;
import mchorse.bbs_mod.ui.particles.sections.UIParticleSchemeRateSection;
import mchorse.bbs_mod.ui.particles.sections.UIParticleSchemeSection;
import mchorse.bbs_mod.ui.particles.sections.UIParticleSchemeShapeSection;
import mchorse.bbs_mod.ui.particles.sections.UIParticleSchemeSpaceSection;
import mchorse.bbs_mod.ui.particles.utils.MolangSyntaxHighlighter;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.DataPath;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.IOUtils;
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
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class UIParticleSchemePanel extends UIDataDashboardPanel<ParticleScheme>
{
    /**
     * Default particle placeholder that comes with the engine.
     */
    public static final Link PARTICLE_PLACEHOLDER = Link.assets("particles/default_placeholder.json");

    public UITextEditor textEditor;
    public UIParticleSchemeRenderer renderer;
    public UIScrollView sectionsView;

    public List<UIParticleSchemeSection> sections = new ArrayList<>();

    private String molangId;

    // Tab and Home fields
    public static final int PARTICLE_DOCUMENT_TABS_HEIGHT = 20;

    private UIControlBar particleTabsBar;
    private UIElement particleTabs;
    private int activeParticleDocumentTab = -1;
    private final List<ParticleDocumentTab> particleDocumentTabs = new ArrayList<>();

    private UIElement mainView;

    private UIElement homePage;
    private UISearchList<DataPath> homeParticlesSearch;
    private UIDataPathList homeParticlesList;
    private UIElement homeActionsPanel;
    private UIButton homeCreateParticle;
    private UIButton homeDuplicateCurrent;
    private UIButton homeRenameCurrent;
    private UIButton homeDeleteCurrent;
    private String homeLastClickedParticleId;
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

    public static class ParticleDocumentTab
    {
        public boolean home;
        public String particleId;

        public ParticleDocumentTab(boolean home, String particleId)
        {
            this.home = home;
            this.particleId = particleId;
        }
    }

    public static class BannerEntry
    {
        public String author;
        public String url;
        public transient Link link;
    }

    public UIParticleSchemePanel(UIDashboard dashboard)
    {
        super(dashboard);
        this.overlay.resizable().minSize(260, 220);

        // Document tabs layout
        this.iconBar.relative(this).x(1F, -20).y(PARTICLE_DOCUMENT_TABS_HEIGHT).w(20).h(1F, -PARTICLE_DOCUMENT_TABS_HEIGHT).column(0).stretch();
        this.particleTabsBar = new UIControlBar();
        this.particleTabsBar.relative(this).x(0).y(0).w(1F).h(PARTICLE_DOCUMENT_TABS_HEIGHT);
        this.particleTabs = new UIElement();
        this.particleTabs.relative(this.particleTabsBar).x(8).y(0).w(1F, -16).h(PARTICLE_DOCUMENT_TABS_HEIGHT).row(0).resize();
        this.particleTabsBar.add(this.particleTabs);

        this.mainView = new UIElement();
        this.mainView.relative(this.editor).y(PARTICLE_DOCUMENT_TABS_HEIGHT).w(1F).h(1F, -PARTICLE_DOCUMENT_TABS_HEIGHT);

        this.renderer = new UIParticleSchemeRenderer();
        this.renderer.relative(this).wTo(this.iconBar.getFlex()).h(1F);

        this.textEditor = new UITextEditor(null).highlighter(new MolangSyntaxHighlighter());
        this.textEditor.background().relative(this.mainView).y(1F, -60).w(1F).h(60);
        this.sectionsView = UI.scrollView(20, 10);
        this.sectionsView.scroll.cancelScrolling().opposite().scrollSpeed *= 3;
        this.sectionsView.relative(this.mainView).w(200).hTo(this.textEditor.area);

        this.mainView.prepend(new UIRenderable(this::drawOverlay));
        this.mainView.add(this.textEditor, this.sectionsView);

        this.prepend(this.renderer);

        UIIcon close = new UIIcon(Icons.CLOSE, (b) -> this.editMoLang(null, null, null));
        close.relative(this.textEditor).x(1F, -20);
        this.textEditor.add(close);

        this.overlay.namesList.setFileIcon(Icons.PARTICLE);

        UIIcon restart = new UIIcon(Icons.REFRESH, (b) ->
        {
            this.renderer.setScheme(this.data);
        });
        restart.tooltip(UIKeys.SNOWSTORM_RESTART_EMITTER, Direction.LEFT);
        this.iconBar.add(restart);

        this.addSection(new UIParticleSchemeGeneralSection(this));
        this.addSection(new UIParticleSchemeCurvesSection(this));
        this.addSection(new UIParticleSchemeSpaceSection(this));
        this.addSection(new UIParticleSchemeInitializationSection(this));
        this.addSection(new UIParticleSchemeRateSection(this));
        this.addSection(new UIParticleSchemeLifetimeSection(this));
        this.addSection(new UIParticleSchemeShapeSection(this));
        this.addSection(new UIParticleSchemeMotionSection(this));
        this.addSection(new UIParticleSchemeExpirationSection(this));
        this.addSection(new UIParticleSchemeAppearanceSection(this));
        this.addSection(new UIParticleSchemeLightingSection(this));
        this.addSection(new UIParticleSchemeCollisionSection(this));

        // Home dashboard layout
        this.homePage = new UIElement()
        {
            @Override
            protected boolean subMouseClicked(UIContext context)
            {
                UIParticleSchemePanel.this.homeParticlesList.deselect();
                UIParticleSchemePanel.this.handleHomeParticlesSelection(null);

                return super.subMouseClicked(context);
            }
        };

        this.homeActionsPanel = new UIElement();
        this.homeParticlesList = new UIDataPathList((list) -> this.handleHomeParticlesSelection(list));
        this.homeParticlesList.setFileIcon(Icons.PARTICLE);
        this.homeParticlesList.context((menu) ->
        {
            menu.action(Icons.FOLDER, UIKeys.PANELS_MODALS_ADD_FOLDER_TITLE, this::addFolderFromHome);

            String selectedId = this.getSelectedHomeParticleId();

            if (selectedId != null)
            {
                menu.action(Icons.COPY, UIKeys.PANELS_CONTEXT_COPY, this::copyHomeParticle);
            }

            try
            {
                MapType clipboardData = Window.getClipboardMap("_ContentType_" + this.getType().getId());

                if (clipboardData != null)
                {
                    menu.action(Icons.PASTE, UIKeys.PANELS_CONTEXT_PASTE, () -> this.pasteHomeParticle(clipboardData));
                }
            }
            catch (Exception e)
            {}

            File folder = this.getType().getRepository().getFolder();

            if (folder != null)
            {
                menu.action(Icons.FOLDER, UIKeys.PANELS_CONTEXT_OPEN, () ->
                    UIUtils.openFolder(new File(folder, this.homeParticlesList.getPath().toString()))
                );
            }
        });

        this.homeParticlesList.moveCallback = (from, to) ->
        {
            String fromStr = from.toString();
            String toStr = to.toString();

            this.getType().getRepository().rename(fromStr, toStr);

            for (ParticleDocumentTab tab : this.particleDocumentTabs)
            {
                if (!tab.home && fromStr.equals(tab.particleId))
                {
                    tab.particleId = toStr;
                }
            }

            if (this.data != null && fromStr.equals(this.data.getId()))
            {
                this.data.setId(toStr);
            }

            this.rebuildParticleDocumentTabs();
            this.requestNames();
        };

        this.homeParticlesSearch = new UISearchList<>(this.homeParticlesList).label(UIKeys.GENERAL_SEARCH);
        this.homeParticlesSearch.list.background();

        this.homeCreateParticle = this.createHomeButton(UIKeys.PARTICLES_CRUD_ADD, Icons.ADD, (b) ->
        {
            UIPromptOverlayPanel panel = new UIPromptOverlayPanel(
                UIKeys.PARTICLES_CRUD_ADD,
                IKey.raw(""),
                (str) ->
                {
                    String targetId = this.homeParticlesList.getPath(str).toString();
                    if (!this.homeParticlesList.hasInHierarchy(targetId))
                    {
                        this.save();
                        this.homeParticlesList.addFile(targetId);
                        ParticleScheme data = (ParticleScheme) this.getType().getRepository().create(targetId);
                        this.fillDefaultData(data);
                        this.fill(data);
                        this.save();
                        this.requestNames();
                    }
                }
            );

            panel.text.filename();
            UIOverlay.addOverlay(this.getContext(), panel);
        });

        this.homeDuplicateCurrent = this.createHomeButton(UIKeys.PARTICLES_CRUD_DUPE, Icons.COPY, (b) ->
        {
            String selectedId = this.getSelectedHomeParticleId();

            if (selectedId == null)
            {
                return;
            }

            UIPromptOverlayPanel panel = new UIPromptOverlayPanel(
                UIKeys.PARTICLES_CRUD_DUPE,
                IKey.raw(""),
                (str) ->
                {
                    String targetId = this.homeParticlesList.getPath(str).toString();
                    if (!this.homeParticlesList.hasInHierarchy(targetId))
                    {
                        this.save();

                        File folder = this.getType().getRepository().getFolder();
                        File source = new File(folder, selectedId);
                        File destination = new File(folder, targetId);

                        if (source.isDirectory())
                        {
                            try
                            {
                                IOUtils.copyFolder(source, destination);
                            }
                            catch (IOException e)
                            {
                                e.printStackTrace();
                            }
                        }

                        this.getType().getRepository().save(targetId, this.data.toData().asMap());
                        this.homeParticlesList.addFile(targetId, false);
                        this.requestNames();
                    }
                }
            );

            panel.text.filename();
            UIOverlay.addOverlay(this.getContext(), panel);
        });

        this.homeRenameCurrent = this.createHomeButton(UIKeys.PARTICLES_CRUD_RENAME, Icons.EDIT, (b) ->
        {
            String selectedId = this.getSelectedHomeParticleId();

            if (selectedId == null)
            {
                return;
            }

            UIPromptOverlayPanel panel = new UIPromptOverlayPanel(
                UIKeys.PARTICLES_CRUD_RENAME,
                IKey.raw(""),
                (str) ->
                {
                    String targetId = this.homeParticlesList.getPath(str).toString();
                    if (!this.homeParticlesList.hasInHierarchy(targetId))
                    {
                        this.getType().getRepository().rename(selectedId, targetId);

                        for (ParticleDocumentTab tab : this.particleDocumentTabs)
                        {
                            if (!tab.home && selectedId.equals(tab.particleId))
                            {
                                tab.particleId = targetId;
                            }
                        }

                        if (this.data != null && selectedId.equals(this.data.getId()))
                        {
                            this.data.setId(targetId);
                        }

                        this.rebuildParticleDocumentTabs();
                        this.requestNames();
                    }
                }
            );

            panel.text.filename();
            UIOverlay.addOverlay(this.getContext(), panel);
        });

        this.homeDeleteCurrent = this.createHomeButton(UIKeys.PARTICLES_CRUD_REMOVE, Icons.REMOVE, (b) ->
        {
            String selectedId = this.getSelectedHomeParticleId();

            if (selectedId == null)
            {
                return;
            }

            UIConfirmOverlayPanel panel = new UIConfirmOverlayPanel(
                UIKeys.PARTICLES_CRUD_REMOVE,
                UIKeys.PANELS_MODALS_REMOVE,
                (bool) ->
                {
                    if (bool)
                    {
                        this.getType().getRepository().delete(selectedId);
                        this.homeParticlesList.removeFile(selectedId);

                        for (int i = this.particleDocumentTabs.size() - 1; i >= 0; i--)
                        {
                            ParticleDocumentTab tab = this.particleDocumentTabs.get(i);
                            if (!tab.home && selectedId.equals(tab.particleId))
                            {
                                this.removeParticleDocumentTab(i);
                            }
                        }

                        this.requestNames();
                    }
                }
            );

            UIOverlay.addOverlay(this.getContext(), panel);
        });

        this.updateHomeButtonsState();

        this.homePage.relative(this.editor).x(0.5F, -250).y(PARTICLE_DOCUMENT_TABS_HEIGHT).w(500).h(1F, -PARTICLE_DOCUMENT_TABS_HEIGHT);
        this.homeActionsPanel.relative(this.homePage).x(0).y(HOME_BANNER_HEIGHT + 20).w(0.35F).h(1F, -(HOME_BANNER_HEIGHT + 20)).column(0).vertical().stretch();
        
        UIElement spacing = new UIElement();
        spacing.h(8);

        this.homeActionsPanel.add(this.homeCreateParticle, spacing, this.homeDuplicateCurrent, this.homeRenameCurrent, this.homeDeleteCurrent);
        this.homeParticlesSearch.relative(this.homePage).x(0.35F).y(HOME_BANNER_HEIGHT + 20).w(0.65F).h(1F, -(HOME_BANNER_HEIGHT + 20));
        this.homePage.add(new UIRenderable(this::renderHomeBackground), this.homeActionsPanel, this.homeParticlesSearch);

        this.editor.add(this.mainView, this.homePage);
        this.add(this.particleTabsBar);

        this.createHomeDocumentTab(true);
        this.fill(null);
        this.updateParticleDocumentView();

        this.initBanners();
    }

    private void handleHomeParticlesSelection(List<DataPath> paths)
    {
        DataPath selected = paths == null || paths.isEmpty() ? null : paths.get(0);
        String selectedId = selected != null && !selected.folder ? selected.toString() : null;

        this.homeLastClickedParticleId = selectedId;
        this.updateHomeButtonsState();

        if (selectedId != null)
        {
            long now = System.currentTimeMillis();

            if (now - this.homeLastClickTime < 250)
            {
                this.openParticleInDocumentTabs(selectedId);
            }

            this.homeLastClickTime = now;
        }
    }

    private void updateHomeButtonsState()
    {
        String selectedId = this.getSelectedHomeParticleId();
        boolean hasSelected = selectedId != null;

        this.homeDuplicateCurrent.setEnabled(hasSelected);
        this.homeRenameCurrent.setEnabled(hasSelected);
        this.homeDeleteCurrent.setEnabled(hasSelected);
    }

    private String getSelectedHomeParticleId()
    {
        DataPath selected = this.homeParticlesList == null ? null : this.homeParticlesList.getCurrentFirst();
        return selected != null && !selected.folder ? selected.toString() : null;
    }

    private void addFolderFromHome()
    {
        UIPromptOverlayPanel panel = new UIPromptOverlayPanel(
            UIKeys.PANELS_MODALS_ADD_FOLDER_TITLE,
            IKey.raw(""),
            (str) ->
            {
                String targetId = this.homeParticlesList.getPath(str).toString();
                if (targetId.trim().isEmpty())
                {
                    this.getContext().notifyError(UIKeys.PANELS_MODALS_EMPTY);
                    return;
                }
                this.getType().getRepository().addFolder(targetId, (bool) ->
                {
                    if (bool)
                    {
                        this.requestNames();
                    }
                });
            }
        );

        panel.text.filename();
        UIOverlay.addOverlay(this.getContext(), panel);
    }

    private void copyHomeParticle()
    {
        String selectedId = this.getSelectedHomeParticleId();
        if (selectedId != null && this.data != null && selectedId.equals(this.data.getId()))
        {
            Window.setClipboard(this.data.toData().asMap(), "_ContentType_" + this.getType().getId());
        }
    }

    private void pasteHomeParticle(MapType clipboardData)
    {
        UIPromptOverlayPanel panel = new UIPromptOverlayPanel(
            UIKeys.PANELS_MODALS_ADD,
            IKey.raw(""),
            (str) ->
            {
                String targetId = this.homeParticlesList.getPath(str).toString();
                if (!this.homeParticlesList.hasInHierarchy(targetId))
                {
                    this.save();
                    this.homeParticlesList.addFile(targetId);
                    ParticleScheme data = (ParticleScheme) this.getType().getRepository().create(targetId, clipboardData);
                    this.fill(data);
                    this.save();
                    this.requestNames();
                }
            }
        );

        panel.text.filename();
        UIOverlay.addOverlay(this.getContext(), panel);
    }

    private void createHomeDocumentTab(boolean activate)
    {
        this.particleDocumentTabs.add(new ParticleDocumentTab(true, null));
        int index = this.particleDocumentTabs.size() - 1;

        this.rebuildParticleDocumentTabs();

        if (activate)
        {
            this.activateParticleDocumentTab(index, false);
        }
    }

    private void addHomeDocumentTab()
    {
        int insertAt = Math.max(0, this.activeParticleDocumentTab + 1);

        this.particleDocumentTabs.add(insertAt, new ParticleDocumentTab(true, null));
        this.rebuildParticleDocumentTabs();
        this.activateParticleDocumentTab(insertAt, false);
    }

    private int findTabByParticleId(String id)
    {
        for (int i = 0; i < this.particleDocumentTabs.size(); i++)
        {
            ParticleDocumentTab tab = this.particleDocumentTabs.get(i);

            if (!tab.home && id.equals(tab.particleId))
            {
                return i;
            }
        }

        return -1;
    }

    private void openParticleInDocumentTabs(String id)
    {
        if (id == null || id.trim().isEmpty())
        {
            return;
        }

        int existingIndex = this.findTabByParticleId(id);

        if (existingIndex >= 0)
        {
            this.activateParticleDocumentTab(existingIndex, true);
            return;
        }

        if (this.activeParticleDocumentTab < 0 || this.activeParticleDocumentTab >= this.particleDocumentTabs.size())
        {
            if (this.particleDocumentTabs.isEmpty())
            {
                this.particleDocumentTabs.add(new ParticleDocumentTab(true, null));
            }
            this.activeParticleDocumentTab = 0;
        }

        ParticleDocumentTab active = this.particleDocumentTabs.get(this.activeParticleDocumentTab);

        if (active.home)
        {
            active.home = false;
            active.particleId = id;
            this.rebuildParticleDocumentTabs();
            this.activateParticleDocumentTab(this.activeParticleDocumentTab, true);
        }
        else
        {
            int insertAt = this.activeParticleDocumentTab + 1;
            this.particleDocumentTabs.add(insertAt, new ParticleDocumentTab(false, id));
            this.rebuildParticleDocumentTabs();
            this.activateParticleDocumentTab(insertAt, true);
        }
    }

    private void activateParticleDocumentTab(int index, boolean loadParticle)
    {
        if (index < 0 || index >= this.particleDocumentTabs.size())
        {
            return;
        }

        if (this.data != null && this.activeParticleDocumentTab != index)
        {
            this.save();
        }

        this.activeParticleDocumentTab = index;

        ParticleDocumentTab tab = this.particleDocumentTabs.get(index);

        if (tab.home)
        {
            this.updateParticleDocumentView();
        }
        else
        {
            if (loadParticle || this.data == null || this.data.getId() == null || !this.data.getId().equals(tab.particleId))
            {
                this.requestData(tab.particleId);
            }
            else
            {
                this.updateParticleDocumentView();
            }
        }

        this.rebuildParticleDocumentTabs();
    }

    private void removeParticleDocumentTab(int index)
    {
        if (index < 0 || index >= this.particleDocumentTabs.size())
        {
            return;
        }

        this.particleDocumentTabs.remove(index);

        if (this.particleDocumentTabs.isEmpty())
        {
            this.particleDocumentTabs.add(new ParticleDocumentTab(true, null));
            this.activeParticleDocumentTab = 0;
            this.rebuildParticleDocumentTabs();
            this.activateParticleDocumentTab(0, false);
            return;
        }

        if (index < this.activeParticleDocumentTab)
        {
            this.activeParticleDocumentTab--;
        }
        else if (index == this.activeParticleDocumentTab)
        {
            this.activeParticleDocumentTab = Math.max(0, Math.min(this.activeParticleDocumentTab, this.particleDocumentTabs.size() - 1));
        }

        this.rebuildParticleDocumentTabs();
        this.activateParticleDocumentTab(this.activeParticleDocumentTab, false);
    }

    private void rebuildParticleDocumentTabs()
    {
        this.particleTabs.removeAll();

        for (int i = 0; i < this.particleDocumentTabs.size(); i++)
        {
            int tabIndex = i;
            ParticleDocumentTab tab = this.particleDocumentTabs.get(i);
            IKey title = tab.home ? L10n.lang("bbs.ui.particles.home.title") : IKey.constant(tab.particleId);
            UIIconTabButton button = new UIIconTabButton(title, tab.home ? Icons.FOLDER : Icons.PARTICLE, (b) -> this.activateParticleDocumentTab(tabIndex, false));
            button.color(this.activeParticleDocumentTab == tabIndex ? BBSSettings.primaryColor.get() : 0x2d2d2d);
            button.w(tab.home ? 88 : 122).h(PARTICLE_DOCUMENT_TABS_HEIGHT);

            if (!tab.home || this.particleDocumentTabs.size() > 1)
            {
                button.removable((b) -> this.removeParticleDocumentTab(tabIndex));
            }

            this.particleTabs.add(button);
        }

        UIIconTabButton add = new UIIconTabButton(IKey.constant(""), Icons.ADD, (b) -> this.addHomeDocumentTab());
        add.color(0x2d2d2d);
        add.background(false);
        add.w(24).h(PARTICLE_DOCUMENT_TABS_HEIGHT);
        this.particleTabs.add(add);
        this.particleTabs.resize();
    }

    private void syncActiveDocumentTabWithData(ParticleScheme data)
    {
        if (data != null)
        {
            if (this.activeParticleDocumentTab < 0 || this.activeParticleDocumentTab >= this.particleDocumentTabs.size())
            {
                this.particleDocumentTabs.add(new ParticleDocumentTab(false, data.getId()));
                this.activeParticleDocumentTab = this.particleDocumentTabs.size() - 1;
            }
            else
            {
                ParticleDocumentTab tab = this.particleDocumentTabs.get(this.activeParticleDocumentTab);
                if (tab.home)
                {
                    tab.home = false;
                    tab.particleId = data.getId();
                }
                else if (!data.getId().equals(tab.particleId))
                {
                    int existing = this.findTabByParticleId(data.getId());
                    if (existing >= 0)
                    {
                        this.activeParticleDocumentTab = existing;
                    }
                    else
                    {
                        tab.particleId = data.getId();
                    }
                }
            }
        }

        this.rebuildParticleDocumentTabs();
        this.updateParticleDocumentView();
    }

    private void updateParticleDocumentView()
    {
        boolean home = this.activeParticleDocumentTab < 0
            || this.activeParticleDocumentTab >= this.particleDocumentTabs.size()
            || this.particleDocumentTabs.get(this.activeParticleDocumentTab).home
            || this.data == null;

        this.showingHomePage = home;
        this.homePage.setVisible(home);
        this.mainView.setVisible(!home);

        if (this.renderer != null)
        {
            this.renderer.setVisible(!home);
        }

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

    public void editMoLang(String id, Consumer<String> callback, MolangExpression expression)
    {
        this.molangId = id;
        this.textEditor.callback = callback;
        this.textEditor.setText(expression == null ? "" : expression.toString());
        this.textEditor.setVisible(callback != null);

        if (callback != null)
        {
            this.sectionsView.hTo(this.textEditor.area);
        }
        else
        {
            this.sectionsView.h(1F);
        }

        this.sectionsView.resize();
    }

    @Override
    protected IKey getTitle()
    {
        return UIKeys.SNOWSTORM_TITLE;
    }

    @Override
    public ContentType getType()
    {
        return ContentType.PARTICLES;
    }

    public void dirty()
    {
        this.renderer.emitter.setupVariables();
    }

    private void addSection(UIParticleSchemeSection section)
    {
        this.sections.add(section);
        this.sectionsView.add(section);
    }

    @Override
    public void fill(ParticleScheme data)
    {
        super.fill(data);
        this.editor.setVisible(true);
        this.syncActiveDocumentTabWithData(data);
    }

    @Override
    protected void fillData(ParticleScheme data)
    {
        this.editMoLang(null, null, null);

        if (this.data != null)
        {
            this.renderer.setScheme(this.data);

            for (UIParticleSchemeSection section : this.sections)
            {
                section.setScheme(this.data);
            }

            this.sectionsView.resize();
        }
    }

    @Override
    public void fillNames(Collection<String> names)
    {
        super.fillNames(names);

        DataPath selected = this.homeParticlesList != null ? this.homeParticlesList.getCurrentFirst() : null;
        String current = selected != null && !selected.folder ? selected.toString() : null;

        if (this.homeParticlesList != null)
        {
            this.homeParticlesList.fill(names);
            this.homeParticlesList.setCurrentFile(current);
        }
        this.updateHomeButtonsState();
    }

    @Override
    public void pickData(String id)
    {
        this.save();
        this.openParticleInDocumentTabs(id);
    }

    @Override
    public void forceSave()
    {
        super.forceSave();

        ParticleFormRenderer.lastUpdate = System.currentTimeMillis();
    }

    @Override
    public void fillDefaultData(ParticleScheme data)
    {
        super.fillDefaultData(data);

        try (InputStream asset = BBSMod.getProvider().getAsset(PARTICLE_PLACEHOLDER))
        {
            MapType map = DataToString.mapFromString(IOUtils.readText(asset));

            ParticleScheme.PARSER.fromData(data, map);
        }
        catch (Exception e)
        {}
    }

    @Override
    public void appear()
    {
        super.appear();

        this.textEditor.updateHighlighter();
    }

    @Override
    public void close()
    {
        super.close();

        if (this.renderer.emitter != null)
        {
            this.renderer.emitter.particles.clear();
        }
    }

    @Override
    public void resize()
    {
        super.resize();

        /* Renderer needs to be resized again because iconBar is in front, and wTo() doesn't
         * work earlier for some reason... */
        this.renderer.resize();
    }

    private void drawOverlay(UIContext context)
    {
        /* Draw debug info */
        if (this.editor.isVisible())
        {
            ParticleEmitter emitter = this.renderer.emitter;
            String label = emitter.particles.size() + "P - " + emitter.age + "A";

            int y = (this.textEditor.isVisible() ? this.textEditor.area.y : this.area.ey()) - 12;

            context.batcher.textShadow(label, this.editor.area.ex() - 4 - context.batcher.getFont().getWidth(label), y);
        }
    }

    @Override
    public void render(UIContext context)
    {
        int color = BBSSettings.primaryColor.get();

        this.area.render(context.batcher, Colors.mulRGB(color | Colors.A100, 0.2F));

        super.render(context);

        if (this.molangId != null)
        {
            FontRenderer font = context.batcher.getFont();
            int w = font.getWidth(this.molangId);

            context.batcher.textCard(this.molangId, this.textEditor.area.ex() - 6 - w, this.textEditor.area.ey() - 6 - font.getHeight());
        }
    }

    @Override
    protected boolean shouldOpenOverlayOnFirstResize()
    {
        return false;
    }

    @Override
    protected boolean shouldRenderOpenOverlayHint()
    {
        return false;
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

    private static final mchorse.bbs_mod.utils.colors.Color TEMP_COLOR = new mchorse.bbs_mod.utils.colors.Color();

    private static int getInterpolatedColor(int a, int b, float x)
    {
        Colors.interpolate(TEMP_COLOR, a, b, x);
        return TEMP_COLOR.getARGBColor();
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
        int dividerX = this.homeParticlesSearch.area.x;

        // Render solid dark background matching films
        context.batcher.box(editorX, editorY, editorX + editorW, editorY + editorH, Colors.setA(0x0b0b0b, 1F));

        // Render Dynamic Snow Storm / Snowfall (Optimized & Deterministic)
        int primary = BBSSettings.primaryColor.get();
        int numFlakes = 160;
        float tick = context.getTickTransition();

        for (int i = 0; i < numFlakes; i++)
        {
            float xPercent = (float) ((i * 17.53F + 0.1F) % 1.0F);
            float baseX = editorX + xPercent * editorW;

            float speed = 0.5F + (float) ((i * 13.27F) % 0.8F);
            float fallOffset = (tick * speed) % (editorH + 40);
            float startY = (float) ((i * 37.89F) % (editorH + 40));
            float flakeY = editorY - 20 + ((startY + fallOffset) % (editorH + 40));

            float wobbleSpeed = 0.05F + (float) ((i * 7.41F) % 0.05F);
            float wobbleWidth = 4F + (float) ((i * 9.15F) % 8F);
            float wobbleX = (float) Math.sin(tick * wobbleSpeed + i) * wobbleWidth;
            float flakeX = baseX + wobbleX;

            float size = 1.0F + (float) ((i * 21.63F) % 3.0F);
            float alphaNorm = (size - 1.0F) / 3.0F;
            float baseAlpha = 0.25F + alphaNorm * 0.55F;

            // Fade out near top/bottom edges
            float edgeFade = 1.0F;
            float distToTop = flakeY - editorY;
            float distToBot = (editorY + editorH) - flakeY;
            if (distToTop < 30) edgeFade = distToTop / 30F;
            else if (distToBot < 30) edgeFade = distToBot / 30F;
            float finalAlpha = baseAlpha * edgeFade;

            // Mix user primary color with white: larger flakes are whiter/brighter, smaller ones have beautiful primary color tint
            int baseColor = getInterpolatedColor(primary, Colors.WHITE, 0.3F + alphaNorm * 0.5F);
            int finalColor = Colors.setA(baseColor, finalAlpha);

            int rx = Math.round(flakeX - size / 2F);
            int ry = Math.round(flakeY - size / 2F);
            int rsize = Math.round(size);
            if (rsize < 1) rsize = 1;

            context.batcher.box(rx, ry, rx + rsize, ry + rsize, finalColor);
        }

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
        context.batcher.textShadow(L10n.lang("bbs.ui.particles.home.actions").get(), pageX + 4, splitY + 6);
        context.batcher.textShadow(L10n.lang("bbs.ui.particles.home.list").get(), dividerX + 4, splitY + 6);
    }
}
