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
import mchorse.bbs_mod.ui.dashboard.UIPanelSwitcher;
import mchorse.bbs_mod.ui.dashboard.list.UIDataPathList;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
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
import mchorse.bbs_mod.ui.home.UIHomePanel;
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
import mchorse.bbs_mod.utils.RecentAssetsTracker;
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

    private int activeParticleDocumentTab = -1;
    private final List<ParticleDocumentTab> particleDocumentTabs = new ArrayList<>();

    private UIElement mainView;

    private UIElement homePage;
    private UISearchList<DataPath> homeParticlesSearch;
    private UIDataPathList homeParticlesList;
    private UIPanelSwitcher panelSwitcher;
    private UIElement homeActionsPanel;
    private UIButton homeCreateParticle;
    private UIButton homeDuplicateCurrent;
    private UIButton homeRenameCurrent;
    private UIButton homeDeleteCurrent;
    private String homeLastClickedParticleId;
    private long homeLastClickTime;
    private boolean showingHomePage = true;



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



    public UIParticleSchemePanel(UIDashboard dashboard)
    {
        super(dashboard);
        this.overlay.resizable().minSize(260, 220);

        this.iconBar.relative(this).x(1F, -20).y(0).w(20).h(1F).column(0).stretch();

        this.mainView = new UIElement();
        this.mainView.relative(this.editor).y(0).w(1F).h(1F);

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

        this.homePage.relative(this.editor).x(0.5F, -250).y(0).w(500).h(1F);
        this.homeActionsPanel.relative(this.homePage).x(0).y(UIHomePanel.HOME_BANNER_HEIGHT + 20).w(0.35F).h(1F, -(UIHomePanel.HOME_BANNER_HEIGHT + 20 + 44)).column(0).vertical().stretch();
        
        this.panelSwitcher = new UIPanelSwitcher(this.dashboard);
        this.panelSwitcher.relative(this.homePage).x(0.5F, -87).y(1F, -32).w(175).h(24);

        UIElement spacing = new UIElement();
        spacing.h(8);

        this.homeActionsPanel.add(this.homeCreateParticle, spacing, this.homeDuplicateCurrent, this.homeRenameCurrent, this.homeDeleteCurrent);
        this.homeParticlesSearch.relative(this.homePage).x(0.35F).y(UIHomePanel.HOME_BANNER_HEIGHT + 20).w(0.65F).h(1F, -(UIHomePanel.HOME_BANNER_HEIGHT + 20 + 44));
        this.homePage.add(new UIRenderable(this::renderHomeBackground), this.homeActionsPanel, this.homeParticlesSearch, this.panelSwitcher);

        this.editor.add(this.mainView, this.homePage);

        this.createHomeDocumentTab(true);
        this.fill(null);
        this.updateParticleDocumentView();
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
        /* No-op: the legacy tab bar UI was removed; the unified UIDocumentTabsBar at the dashboard level replaces it. */
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
        this.iconBar.setVisible(!home);

        if (this.renderer != null)
        {
            this.renderer.setVisible(!home);
        }

        if (home)
        {
            this.editor.resetFlex().relative(this).w(1F).h(1F);
        }
        else
        {
            this.editor.resetFlex().relative(this).wTo(this.iconBar.area).h(1F);
        }
        this.resize();

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

    @Override
    public UIDashboardPanel getMainPanel()
    {
        UIHomePanel home = this.dashboard.getPanel(UIHomePanel.class);

        return home != null ? home : this;
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
    public void showHomeView()
    {
        this.fill(null);
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
        RecentAssetsTracker.add(this.getType(), id);
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

        UIHomePanel home = this.dashboard.getPanel(UIHomePanel.class);
        if (home != null)
        {
            home.renderCardAndBanners(context, this.homePage, dividerX, L10n.lang("bbs.ui.particles.home.list").get());
        }
    }
}
