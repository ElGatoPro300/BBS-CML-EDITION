package mchorse.bbs_mod.ui.model;

import mchorse.bbs_mod.BBSClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.animation.ActionsConfig;
import mchorse.bbs_mod.cubic.animation.gecko.config.GeckoAnimationsConfig;
import mchorse.bbs_mod.cubic.animation.gecko.validation.GeckoAnimationValidator;
import mchorse.bbs_mod.cubic.model.ModelConfig;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.ui.ContentType;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanels;
import mchorse.bbs_mod.ui.dashboard.panels.UIDataDashboardPanel;
import mchorse.bbs_mod.ui.dashboard.panels.overlay.UIDataOverlayPanel;
import mchorse.bbs_mod.ui.dashboard.list.UIDataPathList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.navigation.UIControlBar;
import mchorse.bbs_mod.ui.framework.elements.navigation.UIIconTabButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIPromptOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.DataPath;

import java.util.function.Consumer;
import java.lang.reflect.Method;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;

import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.packs.URLSourcePack;
import mchorse.bbs_mod.utils.resources.Pixels;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.utils.interps.Interpolations;
import org.lwjgl.opengl.GL11;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.framework.elements.utils.UIRenderable;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.ScrollDirection;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.pose.UIPoseEditor;
import mchorse.bbs_mod.ui.model.UIModelIKPanel;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Colors;

import net.minecraft.client.MinecraftClient;

import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;

public class UIModelPanel extends UIDataDashboardPanel<ModelConfig>
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final GeckoAnimationValidator GECKO_VALIDATOR = new GeckoAnimationValidator();

    public UIModelEditorRenderer renderer;
    public UIIcon reloadIcon;
    
    public UIElement mainView;
    public List<UIElement> panels = new ArrayList<>();
    public List<UIIcon> panelButtons = new ArrayList<>();
    
    private UIControlBar modelTabsBar;
    private UIElement modelTabs;
    private UIElement homePage;
    private UISearchList<DataPath> homeModelsSearch;
    private UIDataPathList homeModelsList;
    private UIElement homeActionsPanel;
    private UIButton homeCreateModel;
    private UIButton homeDuplicateCurrent;
    private UIButton homeRenameCurrent;
    private UIButton homeDeleteCurrent;
    private String homeLastClickedModelId;
    private long homeLastClickTime;
    private final List<ModelDocumentTab> modelDocumentTabs = new ArrayList<>();
    private static final String BANNERS_URL = "https://raw.githubusercontent.com/BBSCommunity/CML-NEWS/main/Banners_Panel/banners.json";
    private final List<BannerEntry> homeBanners = new ArrayList<>();
    private static final Set<Link> prefetchingBanners = Collections.synchronizedSet(new HashSet<>());
    private int bannerIndex = 0;
    private float lastBannerTicks = -1;
    private static final int BANNER_DURATION = 200; // 10 seconds at 20 ticks/sec
    private static final int BANNER_TRANSITION = 60; // 3 seconds transition
    private static final int HOME_BANNER_HEIGHT = 108;
    private int activeModelDocumentTab = -1;
    private boolean showingHomePage = true;
    private static final int MODEL_DOCUMENT_TABS_HEIGHT = 20;

    public UIElement modelSettingsPanel;
    public UIElement placeholderPanel;
    public UIModelGeometryPanel geometryPanel;
    public UIModelIKPanel ikPanel;
    public UIScrollView sectionsView;
    public UIScrollView rightView;
    public List<UIModelSection> sections = new ArrayList<>();

    public UIModelPanel(UIDashboard dashboard)
    {
        super(dashboard);
        this.overlay.resizable().minSize(260, 220);

        this.overlay.add.setEnabled(false);

        this.reloadIcon = new UIIcon(Icons.REFRESH, (b) ->
        {
            if (this.data != null)
            {
                String modelId = this.data.getId();
                BBSClient.getModels().loadModel(modelId);
                this.renderer.invalidatePreviewModel();
                this.fillData(this.data);
            }
        });
        this.reloadIcon.tooltip(UIKeys.MODELS_RELOAD, Direction.LEFT);
        this.iconBar.add(this.reloadIcon);

        this.renderer = new UIModelEditorRenderer();
        this.renderer.relative(this).wTo(this.iconBar.getFlex()).h(1F);
        this.renderer.setCallback(this::pickBone);
        
        this.prepend(this.renderer);
        
        this.initBanners();

        this.modelTabsBar = new UIControlBar();
        this.modelTabs = new UIElement();
        this.homePage = new UIElement()
        {
            @Override
            protected boolean subMouseClicked(UIContext context)
            {
                UIModelPanel.this.homeModelsList.deselect();
                UIModelPanel.this.handleHomeModelsSelection(null);

                return super.subMouseClicked(context);
            }
        };
        this.homeActionsPanel = new UIElement();
        this.homeModelsList = new UIDataPathList((list) -> this.handleHomeModelsSelection(list));
        this.homeModelsList.setFileIcon(Icons.MORPH);
        this.homeModelsSearch = new UISearchList<>(this.homeModelsList).label(UIKeys.GENERAL_SEARCH);
        this.homeModelsSearch.list.background();
        
        this.homeCreateModel = this.createHomeButton(UIKeys.MODELS_CRUD_ADD, Icons.ADD, (b) ->
        {
            UIPromptOverlayPanel panel = new UIPromptOverlayPanel(
                UIKeys.MODELS_CRUD_ADD,
                UIKeys.PANELS_MODALS_ADD,
                (str) -> {
                    try {
                        Method m = UIDataOverlayPanel.class.getDeclaredMethod("addNewData", String.class, MapType.class);
                        m.setAccessible(true);
                        m.invoke(this.overlay, str, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            );
            panel.text.filename();
            UIOverlay.addOverlay(this.getContext(), panel);
        });
        this.homeCreateModel.setEnabled(false);
        this.homeDuplicateCurrent = this.createHomeButton(UIKeys.MODELS_CRUD_DUPE, Icons.COPY, (b) -> this.clickWithContext(this.overlay.dupe));
        this.homeRenameCurrent = this.createHomeButton(UIKeys.MODELS_CRUD_RENAME, Icons.EDIT, (b) -> this.clickWithContext(this.overlay.rename));
        this.homeDeleteCurrent = this.createHomeButton(UIKeys.MODELS_CRUD_REMOVE, Icons.REMOVE, (b) -> this.clickWithContext(this.overlay.remove));
        this.updateHomeButtonsState();

        this.modelTabsBar.relative(this.editor).x(0).y(0).w(1F).h(MODEL_DOCUMENT_TABS_HEIGHT);
        this.modelTabs.relative(this.modelTabsBar).x(8).y(0).w(1F, -16).h(MODEL_DOCUMENT_TABS_HEIGHT).row(0).resize();
        this.modelTabsBar.add(this.modelTabs);
        
        this.homePage.relative(this.editor).x(0.5F, -250).y(MODEL_DOCUMENT_TABS_HEIGHT).w(500).h(1F, -MODEL_DOCUMENT_TABS_HEIGHT);
        this.homeActionsPanel.relative(this.homePage).x(0).y(HOME_BANNER_HEIGHT + 20).w(0.35F).h(1F, -(HOME_BANNER_HEIGHT + 20)).column(0).vertical().stretch();
        
        UIElement spacing = new UIElement();
        spacing.h(8);

        this.homeActionsPanel.add(this.homeCreateModel, spacing, this.homeDuplicateCurrent, this.homeRenameCurrent, this.homeDeleteCurrent);
        this.homeModelsSearch.relative(this.homePage).x(0.35F).y(HOME_BANNER_HEIGHT + 20).w(0.65F).h(1F, -(HOME_BANNER_HEIGHT + 20));
        this.homePage.add(new UIRenderable(this::renderHomeBackground), this.homeActionsPanel, this.homeModelsSearch);

        this.mainView = new UIElement();
        this.mainView.relative(this.editor).y(MODEL_DOCUMENT_TABS_HEIGHT).w(1F).h(1F, -MODEL_DOCUMENT_TABS_HEIGHT);

        this.editor.add(this.mainView, this.homePage, this.modelTabsBar);
        this.iconBar.prepend(new UIRenderable(this::renderIcons));

        /* Model Settings Panel */
        this.modelSettingsPanel = new UIElement();
        this.modelSettingsPanel.relative(this.mainView).w(1F).h(1F);
        
        this.sectionsView = UI.scrollView(20, 10);
        this.sectionsView.scroll.cancelScrolling().opposite().scrollSpeed *= 3;
        this.sectionsView.relative(this.modelSettingsPanel).y(0).w(200).h(1F);
        
        this.rightView = UI.scrollView(20, 10);
        this.rightView.scroll.cancelScrolling().opposite().scrollSpeed *= 3;
        this.rightView.relative(this.modelSettingsPanel).x(1F, -200).y(0).w(200).h(1F);
        
        this.modelSettingsPanel.add(this.sectionsView, this.rightView);

        this.placeholderPanel = this.createPlaceholderPanel();

        /* Sections setup */
        this.overlay.namesList.setFileIcon(Icons.MORPH);

        this.addSection(new UIModelGeneralSection(this));
        
        UIModelPartsSection parts = new UIModelPartsSection(this);
        this.sections.add(parts);
        this.setRight(parts.poseEditor);
        this.renderer.transform = parts.poseEditor.transform;

        this.addSection(new UIModelArmorSection(this));
        this.addSection(new UIModelItemsSection(this));
        this.addSection(new UIModelHandsSection(this));
        this.addSection(new UIModelSneakingSection(this));
        this.addSection(new UIModelLookAtSection(this));

        /* Register Panels */
        UIElement spacer = new UIElement();
        spacer.relative(this.iconBar).w(1F).h(10);
        this.iconBar.add(spacer);

        this.geometryPanel = new UIModelGeometryPanel(this);
        this.ikPanel = new UIModelIKPanel(this);

        this.registerPanel(this.modelSettingsPanel, UIKeys.MODELS_SETTINGS, Icons.MODELS_SETTINGS);
        this.registerPanel(this.ikPanel, UIKeys.MODELS_IK_EDITOR, Icons.IK);
        this.registerPanel(this.placeholderPanel, UIKeys.COMING_SOON, Icons.GEAR);
        this.registerPanel(this.geometryPanel, UIKeys.MODELS_GEOMETRY_EDITOR, Icons.GEOMETRY_EDITOR);

        this.setPanel(this.modelSettingsPanel);
        
        this.createHomeDocumentTab(true);
        this.fill(null);
        this.updateModelDocumentView();
    }
    
    private void renderIcons(UIContext context)
    {
        for (int i = 0, c = this.panels.size(); i < c; i++)
        {
            if (this.mainView.getChildren().contains(this.panels.get(i)))
            {
                int index = this.iconBar.getChildren().size() - this.panels.size() + i;

                if (index >= 0 && index < this.iconBar.getChildren().size())
                {
                    IUIElement child = this.iconBar.getChildren().get(index);

                    if (child instanceof UIIcon)
                    {
                        UIDashboardPanels.renderHighlightHorizontal(context.batcher, ((UIIcon) child).area);
                    }
                }
            }
        }

        if (this.reloadIcon != null)
        {
            Area a = this.reloadIcon.area;

            context.batcher.box(a.x + 3, a.ey() + 4, a.ex() - 3, a.ey() + 5, 0x22ffffff);
        }
        else if (this.saveIcon != null)
        {
            Area a = this.saveIcon.area;

            context.batcher.box(a.x + 3, a.ey() + 4, a.ex() - 3, a.ey() + 5, 0x22ffffff);
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
        int dividerX = this.homeModelsSearch.area.x;

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
        
        // Black shadow gradients on the sides of the central column
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
            this.bannerIndex = (this.bannerIndex + 1) % this.homeBanners.size();
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

        int prevIndex = (this.bannerIndex + this.homeBanners.size() - 1) % this.homeBanners.size();
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
        context.batcher.textShadow(IKey.raw("Actions").get(), pageX + 4, splitY + 6);
        context.batcher.textShadow(IKey.raw("Model List").get(), dividerX + 4, splitY + 6);
    }

    private void clickWithContext(UIElement element)
    {
        UIContext context = this.getContext();

        if (context == null || element == null)
        {
            return;
        }

        element.clickItself(context);
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

    private void updateHomeButtonsState()
    {
        boolean hasSelectedModel = this.homeModelsList != null && this.homeModelsList.getCurrentFirst() != null;
        boolean enableIcons = !this.showingHomePage;

        if (this.homeDuplicateCurrent != null)
        {
            this.homeDuplicateCurrent.setEnabled(hasSelectedModel);
        }

        if (this.homeRenameCurrent != null)
        {
            this.homeRenameCurrent.setEnabled(hasSelectedModel);
        }

        if (this.homeDeleteCurrent != null)
        {
            this.homeDeleteCurrent.setEnabled(hasSelectedModel);
        }

        if (this.reloadIcon != null) this.reloadIcon.setEnabled(enableIcons && this.data != null);
        if (this.saveIcon != null) this.saveIcon.setEnabled(enableIcons && this.data != null);
        if (this.openOverlay != null) this.openOverlay.setEnabled(enableIcons);
        for (UIIcon button : this.panelButtons) {
            button.setEnabled(enableIcons && this.data != null);
        }
    }

    private String getSelectedHomeModelId()
    {
        DataPath selected = this.homeModelsList.getCurrentFirst();

        if (selected == null || selected.folder)
        {
            return null;
        }

        return selected.toString();
    }

    private void handleHomeModelsSelection(List<DataPath> list)
    {
        String selected = this.getSelectedHomeModelId();
        this.overlay.namesList.setCurrentFile(selected);

        this.updateHomeButtonsState();

        if (selected == null)
        {
            return;
        }

        long now = System.currentTimeMillis();
        boolean sameAsPrevious = selected.equals(this.homeLastClickedModelId);
        boolean doubleClick = sameAsPrevious && now - this.homeLastClickTime <= 300L;

        this.homeLastClickedModelId = selected;
        this.homeLastClickTime = now;

        if (doubleClick)
        {
            this.openModelInDocumentTabs(selected);
        }
    }

    private void createHomeDocumentTab(boolean activate)
    {
        this.modelDocumentTabs.add(new ModelDocumentTab(true, null));
        int index = this.modelDocumentTabs.size() - 1;

        this.rebuildModelDocumentTabs();

        if (activate)
        {
            this.activateModelDocumentTab(index, false);
        }
    }

    private void addHomeDocumentTab()
    {
        int insertAt = Math.max(0, this.activeModelDocumentTab + 1);

        this.modelDocumentTabs.add(insertAt, new ModelDocumentTab(true, null));
        this.rebuildModelDocumentTabs();
        this.activateModelDocumentTab(insertAt, false);
    }

    private int findTabByModelId(String id)
    {
        for (int i = 0; i < this.modelDocumentTabs.size(); i++)
        {
            ModelDocumentTab tab = this.modelDocumentTabs.get(i);

            if (!tab.home && id.equals(tab.modelId))
            {
                return i;
            }
        }

        return -1;
    }

    private void openModelInDocumentTabs(String id)
    {
        if (id == null || id.trim().isEmpty())
        {
            return;
        }

        int existingIndex = this.findTabByModelId(id);

        if (existingIndex >= 0)
        {
            this.activateModelDocumentTab(existingIndex, true);

            return;
        }

        if (this.activeModelDocumentTab < 0 || this.activeModelDocumentTab >= this.modelDocumentTabs.size())
        {
            if (this.modelDocumentTabs.isEmpty())
            {
                this.modelDocumentTabs.add(new ModelDocumentTab(true, null));
            }

            this.activeModelDocumentTab = 0;
        }

        ModelDocumentTab active = this.modelDocumentTabs.get(this.activeModelDocumentTab);

        if (active.home)
        {
            active.home = false;
            active.modelId = id;
            this.rebuildModelDocumentTabs();
            this.activateModelDocumentTab(this.activeModelDocumentTab, true);
        }
        else
        {
            int insertAt = this.activeModelDocumentTab + 1;
            this.modelDocumentTabs.add(insertAt, new ModelDocumentTab(false, id));
            this.rebuildModelDocumentTabs();
            this.activateModelDocumentTab(insertAt, true);
        }
    }

    private void activateModelDocumentTab(int index, boolean loadModel)
    {
        if (index < 0 || index >= this.modelDocumentTabs.size())
        {
            return;
        }

        if (this.data != null && this.activeModelDocumentTab != index)
        {
            this.save();
        }

        this.activeModelDocumentTab = index;

        ModelDocumentTab tab = this.modelDocumentTabs.get(index);

        if (tab.home)
        {
            this.updateModelDocumentView();
        }
        else
        {
            if (loadModel || this.data == null || this.data.getId() == null || !this.data.getId().equals(tab.modelId))
            {
                this.requestData(tab.modelId);
            }
            else
            {
                this.updateModelDocumentView();
            }
        }

        this.rebuildModelDocumentTabs();
    }

    private void removeModelDocumentTab(int index)
    {
        if (index < 0 || index >= this.modelDocumentTabs.size())
        {
            return;
        }

        this.modelDocumentTabs.remove(index);

        if (this.modelDocumentTabs.isEmpty())
        {
            this.modelDocumentTabs.add(new ModelDocumentTab(true, null));
            this.activeModelDocumentTab = 0;
            this.rebuildModelDocumentTabs();
            this.activateModelDocumentTab(0, false);

            return;
        }

        if (index < this.activeModelDocumentTab)
        {
            this.activeModelDocumentTab--;
        }
        else if (index == this.activeModelDocumentTab)
        {
            this.activeModelDocumentTab = Math.max(0, Math.min(this.activeModelDocumentTab, this.modelDocumentTabs.size() - 1));
        }

        this.rebuildModelDocumentTabs();
        this.activateModelDocumentTab(this.activeModelDocumentTab, false);
    }

    private void rebuildModelDocumentTabs()
    {
        this.modelTabs.removeAll();

        for (int i = 0; i < this.modelDocumentTabs.size(); i++)
        {
            int tabIndex = i;
            ModelDocumentTab tab = this.modelDocumentTabs.get(i);
            IKey title = tab.home ? IKey.raw("Home") : IKey.constant(tab.modelId);
            UIIconTabButton button = new UIIconTabButton(title, tab.home ? Icons.FOLDER : Icons.MORPH, (b) -> this.activateModelDocumentTab(tabIndex, false));
            button.color(this.activeModelDocumentTab == tabIndex ? BBSSettings.primaryColor.get() : 0x2d2d2d);
            button.w(tab.home ? 88 : 122).h(MODEL_DOCUMENT_TABS_HEIGHT);

            if (!tab.home || this.modelDocumentTabs.size() > 1)
            {
                button.removable((b) -> this.removeModelDocumentTab(tabIndex));
            }

            this.modelTabs.add(button);
        }

        UIIconTabButton add = new UIIconTabButton(IKey.constant(""), Icons.ADD, (b) -> this.addHomeDocumentTab());
        add.color(0x2d2d2d);
        add.background(false);
        add.w(24).h(MODEL_DOCUMENT_TABS_HEIGHT);
        this.modelTabs.add(add);
        this.modelTabs.resize();
    }

    private void syncActiveDocumentTabWithData(ModelConfig data)
    {
        if (data != null)
        {
            if (this.activeModelDocumentTab < 0 || this.activeModelDocumentTab >= this.modelDocumentTabs.size())
            {
                this.modelDocumentTabs.add(new ModelDocumentTab(false, data.getId()));
                this.activeModelDocumentTab = this.modelDocumentTabs.size() - 1;
            }
            else
            {
                ModelDocumentTab tab = this.modelDocumentTabs.get(this.activeModelDocumentTab);

                tab.home = false;
                tab.modelId = data.getId();
            }
        }

        this.rebuildModelDocumentTabs();
        this.updateModelDocumentView();
    }

    private void updateModelDocumentView()
    {
        boolean home = this.activeModelDocumentTab < 0
            || this.activeModelDocumentTab >= this.modelDocumentTabs.size()
            || this.modelDocumentTabs.get(this.activeModelDocumentTab).home
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

    @Override
    public void fill(ModelConfig data)
    {
        super.fill(data);
        this.editor.setVisible(true);
        this.syncActiveDocumentTabWithData(data);
    }

    @Override
    public void fillNames(Collection<String> names)
    {
        super.fillNames(names);

        DataPath selected = this.homeModelsList != null ? this.homeModelsList.getCurrentFirst() : null;
        String current = selected != null && !selected.folder ? selected.toString() : null;

        if (this.homeModelsList != null) {
            this.homeModelsList.fill(names);
            this.homeModelsList.setCurrentFile(current);
        }
        this.updateHomeButtonsState();
    }

    @Override
    public void pickData(String id)
    {
        this.save();
        this.openModelInDocumentTabs(id);
    }

    @Override
    protected boolean shouldOpenOverlayOnFirstResize()
    {
        return false;
    }

    private static class ModelDocumentTab
    {
        private boolean home;
        private String modelId;

        private ModelDocumentTab(boolean home, String modelId)
        {
            this.home = home;
            this.modelId = modelId;
        }
    }

    private UIElement createUnavailablePanel()
    {
        UIElement panel = new UIElement();
        panel.relative(this.mainView).w(1F).h(1F);
        
        UILabel label = new UILabel(UIKeys.COMING_SOON)
        {
            @Override
            public void render(UIContext context)
            {
                context.batcher.getContext().getMatrices().push();
                
                int cx = this.area.mx();
                int cy = this.area.my();
                
                context.batcher.getContext().getMatrices().translate(cx, cy, 0);
                context.batcher.getContext().getMatrices().scale(2F, 2F, 1F);
                context.batcher.getContext().getMatrices().translate(-cx, -cy, 0);
                
                super.render(context);
                
                context.batcher.getContext().getMatrices().pop();
            }
        }.background();
        
        label.relative(panel).w(1F).xy(0.5F, 0.5F).anchor(0.5F, 0.5F);
        label.labelAnchor(0.5F, 0.5F);
        panel.add(label);
        
        return panel;
    }

    private UIElement createPlaceholderPanel()
    {
        UIElement panel = new UIElement();
        panel.relative(this.mainView).w(1F).h(1F);

        UILabel label = new UILabel(UIKeys.COMING_SOON).background();
        label.relative(panel).w(0.9F).h(0.78F).xy(0.5F, 0.5F).anchor(0.5F, 0.5F);
        label.labelAnchor(0.5F, 0.5F);
        panel.add(label);

        return panel;
    }

    public UIIcon registerPanel(UIElement panel, IKey tooltip, Icon icon)
    {
        UIIcon button = new UIIcon(icon, (b) -> this.setPanel(panel));

        if (tooltip != null)
        {
            button.tooltip(tooltip, Direction.LEFT);
        }

        this.panels.add(panel);
        this.panelButtons.add(button);
        this.iconBar.add(button);

        return button;
    }

    public void setPanel(UIElement panel)
    {
        this.mainView.removeAll();
        this.mainView.add(panel);
        this.resetEditorScrolls();
        this.rightView.removeAll();

        if (panel == this.modelSettingsPanel)
        {
            this.setRight(this.getPoseEditor());
            this.renderer.transform = this.getPoseEditor().transform;
        }
        else if (panel == this.geometryPanel)
        {
            this.renderer.transform = this.geometryPanel.getGizmoTransformEditor();
        }
        else if (panel == this.ikPanel)
        {
            /* No special gizmo for IK panel in v1 */
        }

        this.mainView.resize();
    }
    
    public void setRight(UIElement element)
    {
        this.rightView.removeAll();

        if (element != null && element.getParent() != null && element.getParent() != this.rightView)
        {
            element.removeFromParent();
        }

        this.rightView.add(element);
        this.rightView.scroll.setScroll(0);
        this.rightView.resize();
    }

    private void resetEditorScrolls()
    {
        this.sectionsView.scroll.dragging = false;
        this.rightView.scroll.dragging = false;
        this.sectionsView.scroll.setScroll(0);
        this.rightView.scroll.setScroll(0);
    }
    
    @Override
    public void save()
    {
        boolean hasData = this.data != null;
        boolean editorEnabled = this.editor != null && this.editor.isEnabled();

        LOGGER.debug("Model Editor save requested: hasData={}, update={}, editorEnabled={}", hasData, this.update, editorEnabled);

        if (!hasData)
        {
            LOGGER.warn("Model Editor save skipped: no model is selected");
            return;
        }

        if (!editorEnabled)
        {
            LOGGER.warn("Model Editor save skipped: editor is disabled for model {}", this.data.getId());
            return;
        }

        if (this.update)
        {
            LOGGER.warn("Model Editor save requested while update flag is true for model {}. Forcing save anyway", this.data.getId());
        }

        this.forceSave();
    }

    @Override
    public void forceSave()
    {
        if (this.data == null)
        {
            LOGGER.warn("Model Editor forceSave skipped: no model data");
            return;
        }

        if (!this.prepareAnimationCode())
        {
            return;
        }

        LOGGER.debug("Model Editor forceSave start: model={}", this.data.getId());

        for (UIModelSection section : this.sections)
        {
            section.setConfig(this.data);
        }

        try
        {
            super.forceSave();
        }
        catch (Exception e)
        {
            LOGGER.error("Model Editor forceSave failed during repository save for model {}", this.data.getId(), e);
            return;
        }

        if (this.data == null)
        {
            return;
        }

        if (this.geometryPanel != null)
        {
            this.geometryPanel.setConfig(this.data);
        }

        this.sectionsView.resize();
        this.rightView.resize();

        Morph morph = Morph.getMorph(MinecraftClient.getInstance().player);

        if (morph != null)
        {
            Form form = morph.getForm();

            if (form instanceof ModelForm && ((ModelForm) form).model.get().equals(this.data.getId()))
            {
                FormRenderer renderer = FormUtilsClient.getRenderer(form);

                if (renderer instanceof ModelFormRenderer)
                {
                    ((ModelFormRenderer) renderer).invalidateCachedModel();
                }
            }
        }

        LOGGER.debug("Model Editor forceSave completed: model={}", this.data.getId());
        this.setSaveDirty(false);
    }

    public void persistModelDataWithoutReload()
    {
        if (this.data == null)
        {
            LOGGER.warn("Model Editor persist without reload skipped: no model data");
            return;
        }

        if (!this.prepareAnimationCode())
        {
            return;
        }

        LOGGER.debug("Model Editor persist without reload start: model={}", this.data.getId());

        try
        {
            super.forceSave();
        }
        catch (Exception e)
        {
            LOGGER.error("Model Editor persist without reload failed for model {}", this.data.getId(), e);
            return;
        }

        Morph morph = Morph.getMorph(MinecraftClient.getInstance().player);

        if (morph != null)
        {
            Form form = morph.getForm();

            if (form instanceof ModelForm && ((ModelForm) form).model.get().equals(this.data.getId()))
            {
                FormRenderer renderer = FormUtilsClient.getRenderer(form);

                if (renderer instanceof ModelFormRenderer)
                {
                    ((ModelFormRenderer) renderer).invalidateCachedModel();
                }
            }
        }

        LOGGER.debug("Model Editor persist without reload completed: model={}", this.data.getId());
        this.setSaveDirty(false);
    }

    private boolean prepareAnimationCode()
    {
        if (this.data == null)
        {
            return false;
        }

        ActionsConfig actions = this.data.animations.get();

        if (actions == null)
        {
            return true;
        }

        GeckoAnimationsConfig geckoSanitized = GECKO_VALIDATOR.sanitize(actions.geckoAnimations);
        ModelInstance preview = this.renderer == null ? null : this.renderer.getPreviewModelInstance();
        Set<String> bones = new HashSet<>();
        Set<String> animations = new HashSet<>();

        if (preview != null && preview.model != null)
        {
            preview.model.getAllGroups().forEach((group) -> bones.add(group.id));
            preview.model.getAllBOBJBones().forEach((bone) -> bones.add(bone.name));
        }

        if (preview != null && preview.animations != null)
        {
            animations.addAll(preview.animations.animations.keySet());
        }

        List<String> geckoValidationErrors = GECKO_VALIDATOR.validate(geckoSanitized, bones, animations);

        if (!geckoValidationErrors.isEmpty())
        {
            LOGGER.error("Model Editor save blocked by invalid gecko animation config for model {}: {}", this.data.getId(), String.join("; ", geckoValidationErrors));
            return false;
        }

        actions.geckoAnimations.copy(geckoSanitized);
        actions.geckoAnimationsJavascript = "var geckoAnimations = { enabled: " + geckoSanitized.enabled + " };";

        return true;
    }

    public UIPoseEditor getPoseEditor()
    {
        for (UIModelSection section : this.sections)
        {
            if (section instanceof UIModelPartsSection)
            {
                return ((UIModelPartsSection) section).poseEditor;
            }
        }

        return null;
    }

    private void pickBone(String bone)
    {
        for (UIModelSection section : this.sections)
        {
            section.deselect();
            section.onBoneSelected(bone);

            if (section instanceof UIModelPartsSection)
            {
                ((UIModelPartsSection) section).selectBone(bone);
                this.setRight(((UIModelPartsSection) section).poseEditor);
            }
        }

        if (this.ikPanel.hasParent())
        {
            this.ikPanel.onBoneSelected(bone);
        }
    }
    
    public void dirty()
    {
        this.renderer.dirty();
        this.setSaveDirty(true);
    }

    private void setSaveDirty(boolean dirty)
    {
        if (this.saveIcon != null)
        {
            this.saveIcon.both(dirty ? Icons.SAVE : Icons.SAVED);
        }
    }

    private void addSection(UIModelSection section)
    {
        this.sections.add(section);
        this.sectionsView.add(section);
    }

    @Override
    public ContentType getType()
    {
        return ContentType.MODELS;
    }

    @Override
    protected IKey getTitle()
    {
        return UIKeys.MODELS_TITLE;
    }

    @Override
    protected void fillData(ModelConfig data)
    {
        this.setSaveDirty(false);

        for (UIIcon button : this.panelButtons)
        {
            button.setEnabled(data != null);
        }

        if (data != null)
        {
            this.renderer.setModel(data.getId());
            this.renderer.setConfig(data);
            
            for (UIModelSection section : this.sections)
            {
                section.setConfig(data);
            }

            if (this.geometryPanel != null)
            {
                this.geometryPanel.setConfig(data);
            }

            if (this.ikPanel != null)
            {
                this.ikPanel.setConfig(data);
            }
            
            this.sectionsView.resize();
            this.rightView.resize();
            this.resetEditorScrolls();
        }
    }

    @Override
    public void render(UIContext context)
    {
        int color = BBSSettings.primaryColor.get();

        this.area.render(context.batcher, Colors.mulRGB(color | Colors.A100, 0.1F));

        super.render(context);
    }

    @Override
    public void resize()
    {
        super.resize();

        this.renderer.resize();
    }

    @Override
    public void close()
    {}

    public static class BannerEntry
    {
        public String author;
        public String url;
        public transient Link link;
    }

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
            finally
            {
                prefetchingBanners.remove(link);
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
}
