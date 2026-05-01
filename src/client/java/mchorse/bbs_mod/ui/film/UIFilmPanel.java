package mchorse.bbs_mod.ui.film;

import mchorse.bbs_mod.BBS;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.actions.ActionState;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.camera.clips.misc.VideoClip;
import mchorse.bbs_mod.camera.clips.modifiers.TranslateClip;
import mchorse.bbs_mod.camera.clips.overwrite.IdleClip;
import mchorse.bbs_mod.camera.controller.CameraController;
import mchorse.bbs_mod.camera.controller.RunnerCameraController;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.renderer.MorphRenderer;
import mchorse.bbs_mod.client.video.VideoRenderer;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.events.register.RegisterFilmEditorFactoriesEvent;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.Recorder;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.network.ClientNetwork;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.resources.packs.URLSourcePack;
import mchorse.bbs_mod.settings.values.IValueListener;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.ui.EditorLayoutNode;
import mchorse.bbs_mod.settings.values.ui.ValueEditorLayout;
import mchorse.bbs_mod.ui.ContentType;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.list.UIDataPathList;
import mchorse.bbs_mod.ui.dashboard.panels.IFlightSupported;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanels;
import mchorse.bbs_mod.ui.dashboard.panels.UIDataDashboardPanel;
import mchorse.bbs_mod.ui.dashboard.panels.overlay.UICRUDOverlayPanel;
import mchorse.bbs_mod.ui.dashboard.panels.overlay.UIDataOverlayPanel;
import mchorse.bbs_mod.ui.dashboard.utils.IUIOrbitKeysHandler;
import mchorse.bbs_mod.ui.film.audio.UIAudioRecorder;
import mchorse.bbs_mod.ui.film.controller.UIFilmController;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.film.utils.UIFilmUndoHandler;
import mchorse.bbs_mod.ui.film.utils.undo.UIUndoHistoryOverlay;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.context.UISimpleContextMenu;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.navigation.UIControlBar;
import mchorse.bbs_mod.ui.framework.elements.navigation.UIIconTabButton;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIMessageOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UINumberOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIPromptOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.UIDraggable;
import mchorse.bbs_mod.ui.framework.elements.utils.UIRenderable;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.UIDataUtils;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.context.ContextAction;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.presets.UICopyPasteController;
import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.DataPath;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.PlayerUtils;
import mchorse.bbs_mod.utils.Timer;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.utils.joml.Vectors;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.KeyframeSegment;
import mchorse.bbs_mod.utils.presets.PresetManager;
import mchorse.bbs_mod.utils.resources.Pixels;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;

import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3d;

import com.mojang.blaze3d.systems.RenderSystem;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class UIFilmPanel extends UIDataDashboardPanel<Film> implements IFlightSupported, IUIOrbitKeysHandler, ICursor
{
    private RunnerCameraController runner;
    private boolean lastRunning;
    private final Position position = new Position(0, 0, 0, 0, 0);
    private final Position lastPosition = new Position(0, 0, 0, 0, 0);

    public UIElement main;
    public UIElement editArea;
    public UIDraggable draggableMain;
    public UIDraggable draggableEditor;
    public UIFilmRecorder recorder;
    public UIFilmPreview preview;
    private final List<UIDraggable> splitterHandles = new ArrayList<>();
    private final List<EditorLayoutNode.SplitterHandleInfo> splitterHandleInfos = new ArrayList<>();

    public UIIcon duplicateFilm;

    /* Main editors */
    public UIClipsPanel cameraEditor;
    public UIReplaysEditor replayEditor;
    public UIClipsPanel actionEditor;
    public UIClipsPanel screenEditor;

    /* Icon bar buttons */
    public UIIcon openHistory;
    public UIIcon openRenderQueue;
    public UIIcon toggleHorizontal;
    public UIIcon layoutLock;
    public UIIcon layoutPresets;
    public UIIcon openCameraEditor;
    public UIIcon openReplayEditor;
    public UIIcon openActionEditor;
    public UIIcon openScreenEditor;
    private UICopyPasteController layoutPresetsController;

    private Camera camera = new Camera();
    private boolean entered;
    private boolean resetFreeFlightLookDrag;
    private boolean freeFlightLookPrimed;
    private int freeFlightLookRawX;
    private int freeFlightLookRawY;

    /* Entity control */
    private boolean performingLayout;
    private UIFilmController controller;
    private UIFilmUndoHandler undoHandler;

    public final Matrix4f lastView = new Matrix4f();
    public final Matrix4f lastProjection = new Matrix4f();

    private Timer flightEditTime = new Timer(100);

    private List<UIElement> panels = new ArrayList<>();
    private UIElement secretPlay;

    private boolean newFilm;
    private final Map<String, UIElement> panelById = new LinkedHashMap<>();
    private final Map<String, UIDraggable> dragHandlesById = new LinkedHashMap<>();
    private static final int FILM_DOCUMENT_TABS_HEIGHT = 20;
    private static final int HOME_BANNER_HEIGHT = 108;
    private static final float DRAG_HANDLE_HEIGHT_NORM = 0.02F;
    private static final float DRAG_HANDLE_TOP_OFFSET_NORM = 0.01F;
    private static final int SPLITTER_HANDLE_PX = 6;
    private static final int DROP_ZONE_CENTER = -1;
    public static final int DROP_ZONE_TAB = 4;
    private static final float DROP_EDGE_MARGIN = 0.2F;
    private static final int EDITOR_MIN_SIZE_FOR_PX_HANDLES = 10;
    private String draggingPanelId;
    private String dropTargetPanelId;
    private int dropTargetZone = DROP_ZONE_CENTER;
    public String mouseHeldPanelId;
    public int clickX, clickY;
    private UIControlBar filmTabsBar;
    private UIElement filmTabs;
    private UIElement homePage;
    private UISearchList<DataPath> homeFilmsSearch;
    private UIDataPathList homeFilmsList;
    private UIElement homeActionsPanel;
    private UIButton homeCreateFilm;
    private UIButton homeOpenManager;
    private UIButton homeDuplicateCurrent;
    private UIButton homeRenameCurrent;
    private UIButton homeDeleteCurrent;
    private String homeLastClickedFilmId;
    private long homeLastClickTime;
    private final List<FilmDocumentTab> filmDocumentTabs = new ArrayList<>();
    private static final String BANNERS_URL = "https://raw.githubusercontent.com/BBSCommunity/CML-NEWS/main/Banners_Panel/banners.json";
    private final List<BannerEntry> homeBanners = new ArrayList<>();
    private static final Set<Link> prefetchingBanners = Collections.synchronizedSet(new HashSet<>());
    private int bannerIndex = 0;
    private float lastBannerTicks = -1;
    private static final int BANNER_DURATION = 200; // 10 seconds at 20 ticks/sec
    private static final int BANNER_TRANSITION = 60; // 3 seconds transition
    private int activeFilmDocumentTab = -1;
    private boolean showingHomePage = true;

    /**
     * Initialize the camera editor with a camera profile.
     */

    public UIFilmPanel(UIDashboard dashboard)
    {
        super(dashboard);

        RegisterFilmEditorFactoriesEvent event = new RegisterFilmEditorFactoriesEvent();
        BBS.getEvents().post(event);

        this.controller = event.createController(this);

        this.runner = new RunnerCameraController(this, (playing) ->
        {
            this.notifyServer(playing ? ActionState.PLAY : ActionState.PAUSE);
        });
        this.runner.getContext().captureSnapshots();
        this.initBanners();

        this.recorder = event.createRecorder(this);

        this.main = new UIElement();
        this.editArea = new UIElement();
        this.preview = event.createPreview(this);
        this.panelById.put("main", this.main);
        this.panelById.put("preview", this.preview);
        this.panelById.put("editArea", this.editArea);

        this.draggableMain = new UIDraggable((context) ->
        {
            ValueEditorLayout layout = BBSSettings.editorLayoutSettings;

            if (layout.isLayoutLocked())
            {
                return;
            }

            if (layout.isHorizontal())
            {
                if (layout.isMainOnTop())
                {
                    layout.setMainSizeH((context.mouseY - this.editor.area.y) / (float) this.editor.area.h);
                }
                else
                {
                    layout.setMainSizeH(1F - (context.mouseY - this.editor.area.y) / (float) this.editor.area.h);
                }

                float normalizedX = (context.mouseX - this.editor.area.x) / (float) this.editor.area.w;

                layout.setEditorSizeH(this.isHorizontalEditorOnLeft(layout) ? normalizedX : 1F - normalizedX);
            }
            else if (layout.isMiddleLayout())
            {
                layout.setMainSizeV((context.mouseX - this.editor.area.x) / (float) this.editor.area.w);
            }
            else
            {
                layout.setMainSizeV(this.calculateMainSizeVFromMouse(layout, context.mouseX));

                layout.setEditorSizeV((context.mouseY - this.editor.area.y) / (float) this.editor.area.h);
            }

            this.setupEditorFlex(true);
        });

        this.draggableMain.reference(() ->
        {
            return this.getMainHandlerReferencePosition();
        });
        this.draggableMain.rendering((context) ->
        {
            int size = 5;
            ValueEditorLayout layout = BBSSettings.editorLayoutSettings;

            if (layout.isHorizontal())
            {
                int dividerX = this.getHorizontalDividerX(layout);
                int x = dividerX + 3;
                int y = (layout.isMainOnTop() ? this.editArea.area.y : this.editArea.area.ey()) - 3;

                context.batcher.box(x, y - size, x + 1, y, Colors.WHITE);
                context.batcher.box(x, y - 1, x + size, y, Colors.WHITE);

                x = dividerX - 3;
                y = (layout.isMainOnTop() ? this.editArea.area.y : this.editArea.area.ey()) - 3;

                context.batcher.box(x - 1, y - size, x, y, Colors.WHITE);
                context.batcher.box(x - size, y - 1, x, y, Colors.WHITE);
            }
            else
            {
                Vector2i position = this.getMainHandlerRenderPosition(layout);
                int x = position.x;
                int y = position.y;

                context.batcher.box(x, y - size, x + 1, y, Colors.WHITE);
                context.batcher.box(x, y - 1, x + size, y, Colors.WHITE);

                y += 1;

                context.batcher.box(x, y, x + 1, y + size, Colors.WHITE);
                context.batcher.box(x, y, x + size, y + 1, Colors.WHITE);
            }
        });

        this.draggableEditor = new UIDraggable((context) ->
        {
            ValueEditorLayout layout = BBSSettings.editorLayoutSettings;

            if (layout.isLayoutLocked() || !layout.isMiddleLayout())
            {
                return;
            }

            float mainSize = layout.getMainSizeV();
            float editorSize = (context.mouseX - this.editor.area.x) / (float) this.editor.area.w - mainSize;

            layout.setEditorSizeH(editorSize);

            this.setupEditorFlex(true);
        });
        this.draggableEditor.reference(() ->
        {
            ValueEditorLayout layout = BBSSettings.editorLayoutSettings;

            if (layout.isMiddleLayout())
            {
                return new Vector2i(this.main.area.ex(), this.main.area.my());
            }

            return new Vector2i(this.editArea.area.x, this.editArea.area.y);
        });
        this.draggableEditor.rendering((context) ->
        {
            int size = 5;
            Vector2i position = this.getEditorHandlerRenderPosition();
            int x = position.x;
            int y = position.y;

            context.batcher.box(x, y - size, x + 1, y, Colors.WHITE);
            context.batcher.box(x, y - 1, x + size, y, Colors.WHITE);

            y += 1;

            context.batcher.box(x, y, x + 1, y + size, Colors.WHITE);
            context.batcher.box(x, y, x + size, y + 1, Colors.WHITE);
        });

        /* Editors */
        this.cameraEditor = new UIClipsPanel(this, BBSMod.getFactoryCameraClips()).target(this.editArea);
        this.cameraEditor.full(this.main);

        this.cameraEditor.clips.context((menu) ->
        {
            UIAudioRecorder.addOption(this, menu);
        });

        this.replayEditor = event.createReplayEditor(this);
        this.replayEditor.full(this.main).setVisible(false);
        this.actionEditor = new UIClipsPanel(this, BBSMod.getFactoryActionClips()).target(this.editArea);
        this.actionEditor.full(this.main).setVisible(false);
        this.screenEditor = new UIClipsPanel(this, BBSMod.getFactoryScreenClips()).target(this.editArea);
        this.screenEditor.full(this.main).setVisible(false);
        this.filmTabsBar = new UIControlBar();
        this.filmTabs = new UIElement();
        this.homePage = new UIElement()
        {
            @Override
            protected boolean subMouseClicked(UIContext context)
            {
                UIFilmPanel.this.homeFilmsList.deselect();
                UIFilmPanel.this.handleHomeFilmsSelection(null);

                return super.subMouseClicked(context);
            }
        };
        this.homeActionsPanel = new UIElement();
        this.homeFilmsList = new UIDataPathList((list) -> this.handleHomeFilmsSelection(list));
        this.homeFilmsList.setFileIcon(Icons.FILM);
        this.homeFilmsSearch = new UISearchList<>(this.homeFilmsList).label(UIKeys.GENERAL_SEARCH);
        this.homeFilmsSearch.list.background();
        this.homeCreateFilm = this.createHomeButton(UIKeys.FILM_CRUD_ADD, Icons.ADD, (b) ->
        {
            UIPromptOverlayPanel panel = new UIPromptOverlayPanel(
                UIKeys.GENERAL_ADD,
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
        this.homeDuplicateCurrent = this.createHomeButton(UIKeys.FILM_CRUD_DUPE, Icons.COPY, (b) -> this.clickWithContext(this.overlay.dupe));
        this.homeRenameCurrent = this.createHomeButton(UIKeys.FILM_CRUD_RENAME, Icons.EDIT, (b) -> this.clickWithContext(this.overlay.rename));
        this.homeDeleteCurrent = this.createHomeButton(UIKeys.FILM_CRUD_REMOVE, Icons.REMOVE, (b) -> this.clickWithContext(this.overlay.remove));
        this.updateHomeButtonsState();

        /* Icon bar buttons */
        this.openHistory = new UIIcon(Icons.LIST, (b) ->
        {
            UIOverlay.addOverlay(this.getContext(), new UIUndoHistoryOverlay(this).resizable().minSize(300, 220), 300, 0.6F);
        });
        this.openHistory.tooltip(UIKeys.FILM_OPEN_HISTORY, Direction.LEFT);

        this.openRenderQueue = new UIIcon(Icons.FILM, (b) ->
        {
            UIOverlay.addOverlay(this.getContext(), new UIRenderQueueOverlayPanel(this), 500, 0.65F);
        });
        this.openRenderQueue.tooltip(UIKeys.FILM_OPEN_RENDER_QUEUE, Direction.LEFT);

        this.openOverlay.tooltip(UIKeys.FILM_OPEN_MANAGER, Direction.LEFT);
        this.saveIcon.tooltip(UIKeys.FILM_SAVE, Direction.LEFT);

        this.toggleHorizontal = new UIIcon(this::getLayoutIcon, (b) -> this.openLayoutSelector())
        {
            @Override
            public boolean subMouseClicked(UIContext context)
            {
                if (context.mouseButton == 1 && this.area.isInside(context))
                {
                    UIFilmPanel.this.invertSelectedLayout();

                    return true;
                }

                return super.subMouseClicked(context);
            }
        };
        this.toggleHorizontal.tooltip(UIKeys.FILM_TOGGLE_LAYOUT, Direction.LEFT);
        this.layoutLock = new UIIcon(this::getLayoutLockIcon, (b) -> this.toggleLayoutLock());
        this.updateLayoutLockTooltip();
        this.openCameraEditor = new UIIcon(Icons.FRUSTUM, (b) -> this.showPanel(this.cameraEditor));
        this.openCameraEditor.tooltip(UIKeys.FILM_OPEN_CAMERA_EDITOR, Direction.LEFT);
        this.openReplayEditor = new UIIcon(Icons.SCENE, (b) -> this.showPanel(this.replayEditor));
        this.openReplayEditor.tooltip(UIKeys.FILM_OPEN_REPLAY_EDITOR, Direction.LEFT);
        this.openActionEditor = new UIIcon(Icons.ACTION, (b) -> this.showPanel(this.actionEditor));
        this.openActionEditor.tooltip(UIKeys.FILM_OPEN_ACTION_EDITOR, Direction.LEFT);
        this.openScreenEditor = new UIIcon(Icons.FILTER, (b) -> this.showPanel(this.screenEditor));
        this.openScreenEditor.tooltip(UIKeys.FILM_OPEN_SCREEN_EDITOR, Direction.LEFT);
        this.layoutPresetsController = new UICopyPasteController(PresetManager.LAYOUTS, "_CopyFilmLayout")
            .supplier(this::getFilmLayoutPresetData)
            .consumer(this::applyFilmLayoutFromPreset);
        this.layoutPresets = new UIIcon(Icons.SAVED, (b) ->
        {
            UIContext ctx = this.getContext();
            this.layoutPresetsController.openPresets(ctx, ctx.mouseX, ctx.mouseY);
        });
        this.layoutPresets.tooltip(UIKeys.FILM_LAYOUT_PRESETS, Direction.LEFT);

        /* Setup elements */
        this.iconBar.add(this.openHistory, this.openRenderQueue, this.openCameraEditor.marginTop(9), this.openReplayEditor, this.openActionEditor, this.openScreenEditor);

        UIElement bottomIcons = new UIElement();

        bottomIcons.relative(this).x(1F, -20).y(1F).wh(20, 60).anchorY(1F).column(0).stretch();
        bottomIcons.add(this.toggleHorizontal, this.layoutLock, this.layoutPresets);
        this.filmTabsBar.relative(this.editor).x(0).y(0).w(1F).h(FILM_DOCUMENT_TABS_HEIGHT);
        this.filmTabs.relative(this.filmTabsBar).x(8).y(0).w(1F, -16).h(FILM_DOCUMENT_TABS_HEIGHT).row(0).resize();
        this.filmTabsBar.add(this.filmTabs);
        this.homePage.relative(this.editor).x(0.5F, -250).y(FILM_DOCUMENT_TABS_HEIGHT).w(500).h(1F, -FILM_DOCUMENT_TABS_HEIGHT);
        this.homeActionsPanel.relative(this.homePage).x(0).y(HOME_BANNER_HEIGHT + 20).w(0.35F).h(1F, -(HOME_BANNER_HEIGHT + 20)).column(0).vertical().stretch();
        
        UIElement spacing = new UIElement();
        spacing.h(8);

        this.homeActionsPanel.add(this.homeCreateFilm, spacing, this.homeDuplicateCurrent, this.homeRenameCurrent, this.homeDeleteCurrent);
        this.homeFilmsSearch.relative(this.homePage).x(0.35F).y(HOME_BANNER_HEIGHT + 20).w(0.65F).h(1F, -(HOME_BANNER_HEIGHT + 20));
        this.homePage.add(new UIRenderable(this::renderHomeBanner), this.homeActionsPanel, this.homeFilmsSearch);

        this.editor.add(this.main, this.editArea, this.preview, this.homePage, new UIRenderable(this::renderIcons), new UIRenderable(this::renderDropZoneHighlight), this.filmTabsBar);
        for (String id : this.panelById.keySet())
        {
            UIDraggable handle = this.createPanelDragHandle(id);
            this.dragHandlesById.put(id, handle);
            this.editor.add(handle);
        }
        this.main.add(this.cameraEditor, this.replayEditor, this.actionEditor, this.screenEditor, this.draggableMain, this.draggableEditor);
        this.add(this.controller, new UIRenderable(this::renderDividers), bottomIcons);
        this.overlay.namesList.setFileIcon(Icons.FILM);
        this.createHomeDocumentTab(true);

        /* Register keybinds */
        IKey modes = UIKeys.CAMERA_EDITOR_KEYS_MODES_TITLE;
        IKey editor = UIKeys.CAMERA_EDITOR_KEYS_EDITOR_TITLE;
        IKey looping = UIKeys.CAMERA_EDITOR_KEYS_LOOPING_TITLE;
        Supplier<Boolean> active = () -> this.data != null && !this.isFlying();

        this.keys().register(Keys.PLAUSE, () -> this.preview.plause.clickItself()).active(active).category(editor);
        this.keys().register(Keys.NEXT_CLIP, () -> this.setCursor(this.data.camera.findNextTick(this.getCursor()))).active(active).category(editor);
        this.keys().register(Keys.PREV_CLIP, () -> this.setCursor(this.data.camera.findPreviousTick(this.getCursor()))).active(active).category(editor);
        this.keys().register(Keys.NEXT, () -> this.setCursor(this.getCursor() + 1)).active(active).category(editor);
        this.keys().register(Keys.PREV, () -> this.setCursor(this.getCursor() - 1)).active(active).category(editor);
        this.keys().register(Keys.UNDO, this::undo).category(editor);
        this.keys().register(Keys.REDO, this::redo).category(editor);
        this.keys().register(Keys.FLIGHT, this::toggleFlight).active(() -> this.data != null).category(modes);
        this.keys().register(Keys.LOOPING, () ->
        {
            BBSSettings.editorLoop.set(!BBSSettings.editorLoop.get());
            this.getContext().notifyInfo(UIKeys.CAMERA_EDITOR_KEYS_LOOPING_TOGGLE_NOTIFICATION);
        }).active(active).category(looping);
        this.keys().register(Keys.LOOPING_SET_MIN, () -> this.cameraEditor.clips.setLoopMin()).active(active).category(looping);
        this.keys().register(Keys.LOOPING_SET_MAX, () -> this.cameraEditor.clips.setLoopMax()).active(active).category(looping);
        this.keys().register(Keys.JUMP_FORWARD, () -> this.setCursor(this.getCursor() + BBSSettings.editorJump.get())).active(active).category(editor);
        this.keys().register(Keys.JUMP_BACKWARD, () -> this.setCursor(this.getCursor() - BBSSettings.editorJump.get())).active(active).category(editor);
        this.keys().register(Keys.FILM_CONTROLLER_CYCLE_EDITORS, () ->
        {
            this.showPanel(MathUtils.cycler(this.getPanelIndex() + (Window.isShiftPressed() ? -1 : 1), this.panels));
            UIUtils.playClick();
        }).category(editor);

        this.openOverlay.context((menu) ->
        {
            if (this.data == null)
            {
                return;
            }

            menu.action(Icons.ARROW_RIGHT, UIKeys.FILM_MOVE_TITLE, () ->
            {
                UIFilmMoveOverlayPanel panel = new UIFilmMoveOverlayPanel((vector) ->
                {
                    int topLayer = this.data.camera.getTopLayer() + 1;
                    int duration = this.data.camera.calculateDuration();
                    double dx = vector.x;
                    double dy = vector.y;
                    double dz = vector.z;

                    BaseValue.edit(this.data, (__) ->
                    {
                        TranslateClip clip = new TranslateClip();

                        clip.layer.set(topLayer);
                        clip.duration.set(duration);
                        clip.translate.get().set(dx, dy, dz);
                        __.camera.addClip(clip);

                        for (Replay replay : __.replays.getList())
                        {
                            for (Keyframe<Double> keyframe : replay.keyframes.x.getKeyframes()) keyframe.setValue(keyframe.getValue() + dx);
                            for (Keyframe<Double> keyframe : replay.keyframes.y.getKeyframes()) keyframe.setValue(keyframe.getValue() + dy);
                            for (Keyframe<Double> keyframe : replay.keyframes.z.getKeyframes()) keyframe.setValue(keyframe.getValue() + dz);

                            replay.actions.shift(dx, dy, dz);
                        }
                    });
                });

                UIOverlay.addOverlay(this.getContext(), panel, 200, 0.9F);
            });

            menu.action(Icons.TIME, UIKeys.FILM_INSERT_SPACE_TITLE, () ->
            {
                UINumberOverlayPanel panel = new UINumberOverlayPanel(UIKeys.FILM_INSERT_SPACE_TITLE, UIKeys.FILM_INSERT_SPACE_DESCRIPTION, (d) ->
                {
                    if (d.intValue() <= 0)
                    {
                        return;
                    }

                    for (Replay replay : this.data.replays.getList())
                    {
                        for (KeyframeChannel<?> channel : replay.keyframes.getChannels())
                        {
                            channel.insertSpace(this.getCursor(), d.intValue());
                        }

                        for (KeyframeChannel channel : replay.properties.properties.values())
                        {
                            channel.insertSpace(this.getCursor(), d.intValue());
                        }
                    }
                });

                panel.value.limit(1).integer().setValue(1D);

                UIOverlay.addOverlay(this.getContext(), panel);
            });

            menu.action(Icons.LINE, UIKeys.FILM_REPLACE_INVENTORY, () ->
            {
                BaseValue.edit(this.getData().inventory, (inv) -> inv.fromPlayer(MinecraftClient.getInstance().player));
            });
        });

        this.fill(null);
        this.setupEditorFlex(false);
        this.updateFilmDocumentView();
        this.flightEditTime.mark();

        this.panels.add(this.cameraEditor);
        this.panels.add(this.replayEditor);
        this.panels.add(this.actionEditor);
        this.panels.add(this.screenEditor);

        this.secretPlay = new UIElement();
        this.secretPlay.keys().register(Keys.PLAUSE, () -> this.preview.plause.clickItself()).active(() -> !this.isFlying() && !this.canBeSeen() && this.data != null).category(editor);

        this.setUndoId("film_panel");
        this.cameraEditor.setUndoId("camera_editor");
        this.replayEditor.setUndoId("replay_editor");
        this.actionEditor.setUndoId("action_editor");
        this.screenEditor.setUndoId("screen_editor");

        UIElement element = new UIElement()
        {
            @Override
            protected boolean subMouseScrolled(UIContext context)
            {
                if (Window.isCtrlPressed() && !UIFilmPanel.this.isFlying())
                {
                    int magnitude = Window.isShiftPressed() ? BBSSettings.editorJump.get() : 1;
                    int newCursor = UIFilmPanel.this.getCursor() + (int) Math.copySign(magnitude, context.mouseWheel);

                    UIFilmPanel.this.setCursor(newCursor);

                    return true;
                }

                return super.subMouseScrolled(context);
            }
        };

        this.add(element);
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

    private Vector2i getMainHandlerReferencePosition()
    {
        ValueEditorLayout layout = BBSSettings.editorLayoutSettings;

        if (layout.isHorizontal())
        {
            return new Vector2i(this.getHorizontalDividerX(layout), layout.isMainOnTop() ? this.editArea.area.y : this.editArea.area.ey());
        }

        if (layout.isMiddleLayout())
        {
            return new Vector2i(this.main.area.x, this.editor.area.my());
        }

        return new Vector2i(this.draggableMain.area.mx(), this.editArea.area.y);
    }

    private Vector2i getMainHandlerRenderPosition(ValueEditorLayout layout)
    {
        if (layout.isMiddleLayout())
        {
            return new Vector2i(this.draggableMain.area.mx(), this.editor.area.my());
        }

        return new Vector2i(this.draggableMain.area.mx(), this.editArea.area.y - 3);
    }

    private Vector2i getEditorHandlerRenderPosition()
    {
        ValueEditorLayout layout = BBSSettings.editorLayoutSettings;

        if (layout.isMiddleLayout())
        {
            return new Vector2i(this.draggableEditor.area.mx(), this.editor.area.my());
        }

        return new Vector2i(this.editArea.area.x + 3, this.editArea.area.y - 3);
    }

    private int getHorizontalDividerX(ValueEditorLayout layout)
    {
        return this.isHorizontalEditorOnLeft(layout) ? this.editArea.area.ex() : this.editArea.area.x;
    }

    private boolean isHorizontalEditorOnLeft(ValueEditorLayout layout)
    {
        return layout.isHorizontalLayoutInverted();
    }

    private boolean isMainOnLeftForCurrentLayout(ValueEditorLayout layout)
    {
        if (layout.isHorizontal() || layout.isMiddleLayout())
        {
            return layout.isMainOnLeft();
        }

        return layout.isMainOnLeft() != layout.isVerticalLayoutInverted();
    }

    private float calculateMainSizeVFromMouse(ValueEditorLayout layout, int mouseX)
    {
        float normalizedX = (mouseX - this.editor.area.x) / (float) this.editor.area.w;

        if (layout.getLayout() == ValueEditorLayout.LAYOUT_VERTICAL_LEFT)
        {
            return layout.isVerticalLayoutInverted() ? 1F - normalizedX : normalizedX;
        }

        if (layout.getLayout() == ValueEditorLayout.LAYOUT_VERTICAL_RIGHT)
        {
            return 1F - normalizedX;
        }

        return this.isMainOnLeftForCurrentLayout(layout) ? normalizedX : 1F - normalizedX;
    }

    private void setupEditorFlex(boolean resize)
    {
        this.setupEditorFlex(resize, false, true);
    }

    private void setupEditorFlex(boolean resize, boolean fast, boolean recreateTabs)
    {
        ValueEditorLayout layout = BBSSettings.editorLayoutSettings;
        EditorLayoutNode root = layout.getFilmLayoutRoot();

        this.resetDynamicLayoutElements(recreateTabs);
        List<EditorLayoutNode.SplitterNode> splitters = layout.getFilmSplitters();

        if (fast && !layout.isLayoutLocked() && resize && splitters.size() == this.splitterHandles.size())
        {
            this.updateEditorFlexBoundsOnly(layout, root);
            this.resize();
            this.resize();
            return;
        }

        Map<String, float[]> bounds = this.computePanelBounds(root);
        
        List<EditorLayoutNode.TabbedNode> tabbedNodes = new ArrayList<>();
        EditorLayoutNode.collectTabbedNodes(root, tabbedNodes);
        Set<String> multiTabPanels = new HashSet<>();
        for (EditorLayoutNode.TabbedNode tabbed : tabbedNodes)
        {
            if (tabbed.tabs.size() > 1)
            {
                for (EditorLayoutNode tab : tabbed.tabs)
                {
                    if (tab instanceof EditorLayoutNode.PanelNode)
                    {
                        multiTabPanels.add(((EditorLayoutNode.PanelNode) tab).getPanelId());
                    }
                }
            }
        }

        this.applyPanelBoundsFromMap(bounds, multiTabPanels);

        if (layout.isLayoutLocked())
        {
            this.setPanelDragHandlesVisible(false);
            
            for (UIDraggable handle : this.splitterHandles)
            {
                handle.removeFromParent();
            }
            this.splitterHandles.clear();
        }
        else
        {
            if (recreateTabs)
            {
                this.rebuildSplitterHandles(layout, root, splitters);
            }

            float th = FILM_DOCUMENT_TABS_HEIGHT / (float) Math.max(1, this.editor.area.h);
            this.splitterHandleInfos.clear();
            EditorLayoutNode.computeSplitterHandles(root, 0F, th, 1F, 1F - th, this.splitterHandleInfos);
            this.syncSplitterHandleBounds();
            this.applyDragHandleBoundsFromMap(bounds);
        }
        
        this.setupTabBars(root, bounds, recreateTabs);

        if (resize)
        {
            this.resize();
            this.resize();
        }

        this.updateFilmDocumentView();
    }

    private void resetDynamicLayoutElements(boolean recreateTabs)
    {
        if (recreateTabs)
        {
            for (UITabBar bar : this.tabBars)
            {
                bar.removeFromParent();
            }
            this.tabBars.clear();
        }
        for (UIElement panel : this.panelById.values())
        {
            panel.resetFlex();
            panel.setVisible(false);
        }
        this.draggableMain.setVisible(false);
        this.draggableEditor.setVisible(false);
        for (UIDraggable handle : this.dragHandlesById.values())
        {
            handle.resetFlex();
        }
    }

    private Map<String, float[]> computePanelBounds(EditorLayoutNode root)
    {
        float th = FILM_DOCUMENT_TABS_HEIGHT / (float) Math.max(1, this.editor.area.h);
        Map<String, float[]> bounds = new HashMap<>();
        root.computeBounds(0F, th, 1F, 1F - th, bounds);
        return bounds;
    }

    private void setPanelDragHandlesVisible(boolean visible)
    {
        for (UIDraggable handle : this.dragHandlesById.values())
        {
            handle.setVisible(visible);
        }
    }

    private void rebuildSplitterHandles(ValueEditorLayout layout, EditorLayoutNode root, List<EditorLayoutNode.SplitterNode> splitters)
    {
        for (UIDraggable handle : this.splitterHandles)
        {
            handle.removeFromParent();
        }
        this.splitterHandles.clear();

        this.splitterHandleInfos.clear();
        float th = FILM_DOCUMENT_TABS_HEIGHT / (float) Math.max(1, this.editor.area.h);
        EditorLayoutNode.computeSplitterHandles(root, 0F, th, 1F, 1F - th, this.splitterHandleInfos);

        for (int i = 0; i < splitters.size(); i++)
        {
            final int index = i;
            UIDraggable handle = new UIDraggable((context) ->
            {
                float ratio = this.getSplitterRatioFromMouse(index, context.mouseX, context.mouseY);
                if (ratio >= 0F)
                {
                    layout.setFilmSplitterRatio(index, ratio);
                    this.setupEditorFlex(true, true, false);
                }
            });

            handle.dragEnd(() -> this.setupEditorFlex(true, false, false));
            handle.rendering((context) -> this.renderSplitter(context, index));
            this.applySplitterHandleBounds(handle, this.splitterHandleInfos.get(index));
            this.splitterHandles.add(handle);

            this.editor.add(handle);
            handle.resize();
        }
    }

    private void applySplitterHandleBounds(UIDraggable handle, EditorLayoutNode.SplitterHandleInfo info)
    {
        int ew = this.editor.area.w;
        int eh = this.editor.area.h;
        if (ew < EDITOR_MIN_SIZE_FOR_PX_HANDLES || eh < EDITOR_MIN_SIZE_FOR_PX_HANDLES)
        {
            handle.relative(this.editor).x(info.hx).y(info.hy).w(info.hw).h(info.hh);
            return;
        }

        if (info.horizontal)
        {
            float centerY = info.hy + info.hh * 0.5F;
            float hyNew = centerY - (SPLITTER_HANDLE_PX / (2F * eh));
            handle.relative(this.editor).x(info.hx).y(hyNew).w(info.hw).h(SPLITTER_HANDLE_PX);
        }
        else
        {
            float centerX = info.hx + info.hw * 0.5F;
            float hxNew = centerX - (SPLITTER_HANDLE_PX / (2F * ew));
            handle.relative(this.editor).x(hxNew).y(info.hy).w(SPLITTER_HANDLE_PX).h(info.hh);
        }
    }

    private void syncSplitterHandleBounds()
    {
        for (int i = 0; i < this.splitterHandles.size() && i < this.splitterHandleInfos.size(); i++)
        {
            this.applySplitterHandleBounds(this.splitterHandles.get(i), this.splitterHandleInfos.get(i));
        }
    }

    private float getSplitterRatioFromMouse(int index, int mouseX, int mouseY)
    {
        if (index < 0 || index >= this.splitterHandleInfos.size())
        {
            return -1F;
        }

        EditorLayoutNode.SplitterHandleInfo info = this.splitterHandleInfos.get(index);
        int ex = this.editor.area.x;
        int ey = this.editor.area.y;
        int ew = Math.max(1, this.editor.area.w);
        int eh = Math.max(1, this.editor.area.h);
        float ratio = info.horizontal
            ? (mouseY - (ey + info.py * eh)) / (info.ph * eh)
            : (mouseX - (ex + info.px * ew)) / (info.pw * ew);

        return MathUtils.clamp(ratio, 0.05F, 0.95F);
    }

    private Vector2i getSplitterHandleReferencePosition(int index, List<EditorLayoutNode.SplitterNode> splitters)
    {
        if (index < 0 || index >= this.splitterHandleInfos.size() || index >= splitters.size())
        {
            return new Vector2i(this.editor.area.x, this.editor.area.y);
        }

        EditorLayoutNode.SplitterHandleInfo info = this.splitterHandleInfos.get(index);
        float r = splitters.get(index).getRatio();
        int ex = this.editor.area.x;
        int ey = this.editor.area.y;
        int ew = Math.max(1, this.editor.area.w);
        int eh = Math.max(1, this.editor.area.h);
        int hx = ex + (int) ((info.px + (info.horizontal ? info.pw * 0.5F : r * info.pw)) * ew);
        int hy = ey + (int) ((info.py + (info.horizontal ? r * info.ph : info.ph * 0.5F)) * eh);

        return new Vector2i(hx, hy);
    }

    private void applyPanelBoundsFromMap(Map<String, float[]> bounds, Set<String> multiTabPanels)
    {
        for (UIElement el : this.panelById.values())
        {
            el.setVisible(false);
        }
        this.homePage.setVisible(false);

        for (Map.Entry<String, float[]> e : bounds.entrySet())
        {
            UIElement el = this.panelById.get(e.getKey());
            if (el != null)
            {
                float[] b = e.getValue();
                int offset = multiTabPanels.contains(e.getKey()) ? 20 : 0;
                el.relative(this.editor).x(b[0]).y(b[1], offset).w(b[2]).h(b[3], -offset);
                el.setVisible(true);
            }
        }
    }

    private void applyDragHandleBoundsFromMap(Map<String, float[]> bounds)
    {
        for (UIDraggable h : this.dragHandlesById.values())
        {
            h.setVisible(false);
        }

        for (Map.Entry<String, float[]> e : bounds.entrySet())
        {
            String id = e.getKey();
            UIDraggable h = this.dragHandlesById.get(id);
            UIElement el = this.panelById.get(id);
            
            if (h != null && el != null && el.isVisible())
            {
                float[] b = e.getValue();
                h.relative(this.editor).x(b[0]).y(b[1] + DRAG_HANDLE_TOP_OFFSET_NORM).w(b[2]).h(DRAG_HANDLE_HEIGHT_NORM);
                h.setVisible(!BBSSettings.editorLayoutSettings.isLayoutLocked());
            }
        }
    }

    private void updateEditorFlexBoundsOnly(ValueEditorLayout layout, EditorLayoutNode root)
    {
        float th = FILM_DOCUMENT_TABS_HEIGHT / (float) Math.max(1, this.editor.area.h);
        Map<String, float[]> bounds = this.computePanelBounds(root);
        
        List<EditorLayoutNode.TabbedNode> tabbedNodes = new ArrayList<>();
        EditorLayoutNode.collectTabbedNodes(root, tabbedNodes);
        Set<String> multiTabPanels = new HashSet<>();
        for (EditorLayoutNode.TabbedNode tabbed : tabbedNodes)
        {
            if (tabbed.tabs.size() > 1)
            {
                for (EditorLayoutNode tab : tabbed.tabs)
                {
                    if (tab instanceof EditorLayoutNode.PanelNode)
                    {
                        multiTabPanels.add(((EditorLayoutNode.PanelNode) tab).getPanelId());
                    }
                }
            }
        }

        this.applyPanelBoundsFromMap(bounds, multiTabPanels);
        this.splitterHandleInfos.clear();
        EditorLayoutNode.computeSplitterHandles(root, 0F, th, 1F, 1F - th, this.splitterHandleInfos);
        this.syncSplitterHandleBounds();
        this.applyDragHandleBoundsFromMap(bounds);
        
        this.setupTabBars(root, bounds, false);
        
        // Ensure indicators and icons are on top
        this.editor.getChildren().removeIf(c -> c instanceof UIRenderable);
        this.editor.add(new UIRenderable(this::renderIcons), new UIRenderable(this::renderDropZoneHighlight));
    }

    private void clearPanelDragState()
    {
        this.draggingPanelId = null;
        this.dropTargetPanelId = null;
        this.dropTargetZone = DROP_ZONE_CENTER;
    }

    private void setDropIntent(DropIntent intent)
    {
        this.dropTargetPanelId = intent == null ? null : intent.targetId;
        this.dropTargetZone = intent == null ? DROP_ZONE_CENTER : intent.zone;
    }

    private void applyPanelDropResult(String dragId, String targetId, int zone)
    {
        ValueEditorLayout layout = BBSSettings.editorLayoutSettings;
        EditorLayoutNode root = layout.getFilmLayoutRoot();
        EditorLayoutNode newRoot = this.buildDroppedLayout(root, dragId, targetId, zone);

        if (newRoot != null)
        {
            layout.setFilmLayoutRoot(newRoot);
            this.setupEditorFlex(true);
        }
    }

    private EditorLayoutNode buildDroppedLayout(EditorLayoutNode root, String draggedId, String targetId, int zone)
    {
        switch (zone)
        {
            case DROP_ZONE_CENTER:
                return root.copyWithSwappedIds(draggedId, targetId);

                        case DROP_ZONE_TAB:
                return EditorLayoutNode.copyWithDockedLeaf(root, targetId, draggedId);

            case EditorLayoutNode.EDGE_LEFT:
            case EditorLayoutNode.EDGE_RIGHT:
            case EditorLayoutNode.EDGE_TOP:
            case EditorLayoutNode.EDGE_BOTTOM:
                return EditorLayoutNode.copyWithInsertSplitAt(root, targetId, draggedId, zone);

            default:
                return root;
        }
    }

    private UIDraggable createPanelDragHandle(String panelId)
    {
        UIDraggable handle = new UIDraggable((context) ->
        {
            this.startPanelDrag(panelId);
            this.updateDropTargetFromMouse(context.mouseX, context.mouseY);
        });

        handle.dragEnd(() ->
        {
            DropIntent intent = new DropIntent(this.dropTargetPanelId, this.dropTargetZone);
            if (!this.canApplyDropIntent(this.draggingPanelId, intent))
            {
                this.clearPanelDragState();
                return;
            }

            this.applyPanelDropResult(this.draggingPanelId, intent.targetId, intent.zone);
            this.clearPanelDragState();
        });

        handle.hoverOnly().rendering((context) -> this.renderPanelDragHandle(context, handle));

        return handle;
    }

    private void startPanelDrag(String panelId)
    {
        if (this.draggingPanelId == null)
        {
            this.draggingPanelId = panelId;
        }
    }

    private void updateDropTargetFromMouse(int mouseX, int mouseY)
    {
        this.setDropIntent(this.resolveDropIntent(mouseX, mouseY));
    }

    private boolean canApplyDropIntent(String draggedId, DropIntent intent)
    {
        return draggedId != null && intent != null && intent.targetId != null && !draggedId.equals(intent.targetId);
    }

    private String resolveSiblingTabId(String targetId)
    {
        EditorLayoutNode root = BBSSettings.editorLayoutSettings.getFilmLayoutRoot();
        if (root == null) return null;
        
        EditorLayoutNode.TabbedNode tabbed = this.findTabbedNodeContaining(root, targetId);
        if (tabbed == null) return null;
        
        for (EditorLayoutNode tab : tabbed.tabs)
        {
            if (tab instanceof EditorLayoutNode.PanelNode)
            {
                String id = ((EditorLayoutNode.PanelNode) tab).getPanelId();
                if (!id.equals(targetId))
                {
                    return id;
                }
            }
        }
        return null;
    }
    
    private EditorLayoutNode.TabbedNode findTabbedNodeContaining(EditorLayoutNode node, String panelId)
    {
        if (node instanceof EditorLayoutNode.TabbedNode)
        {
            EditorLayoutNode.TabbedNode tabbed = (EditorLayoutNode.TabbedNode) node;
            for (EditorLayoutNode tab : tabbed.tabs)
            {
                if (tab instanceof EditorLayoutNode.PanelNode && ((EditorLayoutNode.PanelNode) tab).getPanelId().equals(panelId))
                {
                    return tabbed;
                }
            }
        }
        else if (node instanceof EditorLayoutNode.SplitterNode)
        {
            EditorLayoutNode.SplitterNode splitter = (EditorLayoutNode.SplitterNode) node;
            EditorLayoutNode.TabbedNode res = this.findTabbedNodeContaining(splitter.getFirst(), panelId);
            if (res != null) return res;
            return this.findTabbedNodeContaining(splitter.getSecond(), panelId);
        }
        return null;
    }

    private DropIntent resolveDropIntent(int mouseX, int mouseY)
    {
        for (Map.Entry<String, UIElement> entry : this.panelById.entrySet())
        {
            if (!entry.getValue().isVisible()) continue;
            Area area = entry.getValue().area;
            int guideZone = this.resolveDockGuideZoneFromMouse(area, mouseX, mouseY);

            if (guideZone != Integer.MIN_VALUE)
            {
                String targetId = entry.getKey();
                if (this.draggingPanelId != null && targetId.equals(this.draggingPanelId))
                {
                    if (guideZone != DROP_ZONE_CENTER && guideZone != DROP_ZONE_TAB)
                    {
                        String sibling = this.resolveSiblingTabId(targetId);
                        if (sibling != null)
                        {
                            targetId = sibling;
                        }
                    }
                }
                return new DropIntent(targetId, guideZone);
            }
        }

        for (Map.Entry<String, UIElement> entry : this.panelById.entrySet())
        {
            if (!entry.getValue().isVisible()) continue;
            Area area = entry.getValue().area;
            if (area.isInside(mouseX, mouseY))
            {
                String targetId = entry.getKey();
                int zone = this.resolveDropZone(area, mouseX, mouseY);
                
                if (this.draggingPanelId != null && targetId.equals(this.draggingPanelId))
                {
                    if (zone != DROP_ZONE_CENTER && zone != DROP_ZONE_TAB)
                    {
                        String sibling = this.resolveSiblingTabId(targetId);
                        if (sibling != null)
                        {
                            targetId = sibling;
                        }
                    }
                }

                return new DropIntent(targetId, zone);
            }
        }

        return null;
    }

    private void renderPanelDragHandle(UIContext context, UIDraggable handle)
    {
        boolean active = handle.area.isInside(context) || handle.isDragging();
        int color = active ? Colors.WHITE : Colors.setA(Colors.WHITE, 0.6F);
        int cx = handle.area.mx();
        int cy = handle.area.y + handle.area.h / 2 + 4;
        context.batcher.icon(Icons.ALL_DIRECTIONS, color, cx, cy, 0.5F, 0.5F);
    }

    private int resolveDropZone(Area area, int mouseX, int mouseY)
    {
        int guideZone = this.resolveDockGuideZoneFromMouse(area, mouseX, mouseY);
        if (guideZone != Integer.MIN_VALUE)
        {
            return guideZone;
        }

        float nx = area.w <= 0 ? 0.5F : MathUtils.clamp((mouseX - area.x) / (float) area.w, 0F, 1F);
        float ny = area.h <= 0 ? 0.5F : MathUtils.clamp((mouseY - area.y) / (float) area.h, 0F, 1F);
        float edge = DROP_EDGE_MARGIN;

        if (nx > edge && nx < 1F - edge && ny > edge && ny < 1F - edge)
        {
            return DROP_ZONE_CENTER;
        }

        float left = nx;
        float right = 1F - nx;
        float top = ny;
        float bottom = 1F - ny;

        float nearest = left;
        int zone = EditorLayoutNode.EDGE_LEFT;
        if (right < nearest)
        {
            nearest = right;
            zone = EditorLayoutNode.EDGE_RIGHT;
        }
        if (top < nearest)
        {
            nearest = top;
            zone = EditorLayoutNode.EDGE_TOP;
        }
        if (bottom < nearest)
        {
            zone = EditorLayoutNode.EDGE_BOTTOM;
        }

        return zone;
    }

    private int resolveDockGuideZoneFromMouse(Area area, int mouseX, int mouseY)
    {
        int[] zones = new int[] {
            DROP_ZONE_CENTER,
            DROP_ZONE_TAB,
            EditorLayoutNode.EDGE_LEFT,
            EditorLayoutNode.EDGE_RIGHT,
            EditorLayoutNode.EDGE_TOP,
            EditorLayoutNode.EDGE_BOTTOM
        };
        int hitPadding = 8;

        for (int zone : zones)
        {
            int[] rect = this.getDockGuideRect(area, zone);
            if (rect == null)
            {
                continue;
            }

            if (mouseX >= rect[0] - hitPadding && mouseX <= rect[2] + hitPadding && mouseY >= rect[1] - hitPadding && mouseY <= rect[3] + hitPadding)
            {
                return zone;
            }
        }

        return Integer.MIN_VALUE;
    }

    private static class DropIntent
    {
        private final String targetId;
        private final int zone;

        private DropIntent(String targetId, int zone)
        {
            this.targetId = targetId;
            this.zone = zone;
        }
    }

    private void renderDropZoneHighlight(UIContext context)
    {
        if (this.showingHomePage)
        {
            return;
        }

        if (BBSSettings.editorLayoutSettings.isLayoutLocked() || this.draggingPanelId == null || this.dropTargetPanelId == null)
        {
            return;
        }

        UIElement target = this.panelById.get(this.dropTargetPanelId);
        if (target == null)
        {
            return;
        }

        int[] zones = new int[] {
            EditorLayoutNode.EDGE_LEFT,
            EditorLayoutNode.EDGE_RIGHT,
            EditorLayoutNode.EDGE_TOP,
            EditorLayoutNode.EDGE_BOTTOM,
            DROP_ZONE_CENTER,
            DROP_ZONE_TAB
        };

        for (int zone : zones)
        {
            this.renderDockGuideZone(context, target.area, zone, zone == this.dropTargetZone);
        }

        this.renderDropPreviewLayout(context);
    }

    private void renderDockGuideZone(UIContext context, Area area, int zone, boolean active)
    {
        int[] rect = this.getDockGuideRect(area, zone);
        if (rect == null)
        {
            return;
        }

        int baseColor = this.getDockGuideBaseColor();
        float opacity = this.getDockGuideOpacity();
        int border = this.withAlpha(Colors.mulRGB(baseColor, active ? 1.25F : 1.1F), opacity * (active ? 0.95F : 0.7F));
        int fill = this.withAlpha(baseColor, opacity * (active ? 0.6F : 0.3F));
        int glow = this.withAlpha(baseColor, opacity * (active ? 0.45F : 0.2F));

        context.batcher.dropShadow(rect[0] - 2, rect[1] - 2, rect[2] + 2, rect[3] + 2, 4, glow, 0x00000000);
        this.renderDropZoneRect(context, rect[0], rect[1], rect[2], rect[3], border, fill);

        int width = rect[2] - rect[0];
        int height = rect[3] - rect[1];
        int core = Math.max(3, Math.min(width, height) / 4);
        int cx = (rect[0] + rect[2]) / 2;
        int cy = (rect[1] + rect[3]) / 2;
        int coreColor = this.withAlpha(Colors.mulRGB(baseColor, 1.4F), opacity * (active ? 0.85F : 0.55F));

        context.batcher.box(cx - core, cy - core, cx + core, cy + core, coreColor);
    }

    private int[] getDockGuideRect(Area area, int zone)
    {
        int x = area.x;
        int y = area.y;
        int ex = area.ex();
        int ey = area.ey();
        int w = Math.max(1, ex - x);
        int h = Math.max(1, ey - y);
        int min = Math.min(w, h);
        int size = Math.max(14, Math.min(34, Math.round(min * 0.17F)));
        int half = size / 2;
        int cx = x + w / 2;
        int cy = y + h / 2;
        int orbitX = Math.max(size, Math.round(w * 0.24F));
        int orbitY = Math.max(size, Math.round(h * 0.24F));
        int rx1 = cx - half;
        int ry1 = cy - half;

        switch (zone)
        {
            case DROP_ZONE_TAB:
                int margin2 = 2;
                return new int[] {x + margin2, y + margin2, ex - margin2, Math.min(ey - margin2, y + margin2 + 14)};
            case EditorLayoutNode.EDGE_LEFT:
                rx1 = cx - orbitX - half;
                ry1 = cy - half;
                break;
            case EditorLayoutNode.EDGE_RIGHT:
                rx1 = cx + orbitX - half;
                ry1 = cy - half;
                break;
            case EditorLayoutNode.EDGE_TOP:
                rx1 = cx - half;
                ry1 = cy - orbitY - half;
                break;
            case EditorLayoutNode.EDGE_BOTTOM:
                rx1 = cx - half;
                ry1 = cy + orbitY - half;
                break;
            case DROP_ZONE_CENTER:
                rx1 = cx - half;
                ry1 = cy - half;
                break;
            default:
                return null;
        }

        int margin = 2;
        int rx2 = rx1 + size;
        int ry2 = ry1 + size;

        if (rx1 < x + margin)
        {
            rx2 += (x + margin) - rx1;
            rx1 = x + margin;
        }

        if (ry1 < y + margin)
        {
            ry2 += (y + margin) - ry1;
            ry1 = y + margin;
        }

        if (rx2 > ex - margin)
        {
            rx1 -= rx2 - (ex - margin);
            rx2 = ex - margin;
        }

        if (ry2 > ey - margin)
        {
            ry1 -= ry2 - (ey - margin);
            ry2 = ey - margin;
        }

        if (rx2 - rx1 <= 2 || ry2 - ry1 <= 2)
        {
            return null;
        }

        return new int[] {rx1, ry1, rx2, ry2};
    }

    private void renderDropPreviewLayout(UIContext context)
    {
        if (this.draggingPanelId == null || this.dropTargetPanelId == null)
        {
            return;
        }

        ValueEditorLayout layout = BBSSettings.editorLayoutSettings;
        EditorLayoutNode root = layout.getFilmLayoutRoot();
        EditorLayoutNode preview = this.buildDroppedLayout(root, this.draggingPanelId, this.dropTargetPanelId, this.dropTargetZone);

        if (preview == null)
        {
            return;
        }

        Map<String, float[]> bounds = this.computePanelBounds(preview);
        int baseColor = this.getDockGuideBaseColor();
        float opacity = this.getDockGuideOpacity();
        int previewFill = this.withAlpha(baseColor, opacity * 0.18F);
        int previewStrong = this.withAlpha(Colors.mulRGB(baseColor, 1.2F), opacity * 0.34F);
        int previewBorder = this.withAlpha(Colors.mulRGB(baseColor, 1.35F), opacity * 0.6F);

        for (Map.Entry<String, float[]> entry : bounds.entrySet())
        {
            float[] b = entry.getValue();
            int x = this.editor.area.x + Math.round(this.editor.area.w * b[0]);
            int y = this.editor.area.y + Math.round(this.editor.area.h * b[1]);
            int w = Math.max(1, Math.round(this.editor.area.w * b[2]));
            int h = Math.max(1, Math.round(this.editor.area.h * b[3]));
            int fill = entry.getKey().equals(this.draggingPanelId) ? previewStrong : previewFill;

            this.renderDropZoneRect(context, x, y, x + w, y + h, previewBorder, fill);
        }
    }

    private void renderDropZoneRect(UIContext context, Area a, int border, int fill)
    {
        this.renderDropZoneRect(context, a.x, a.y, a.ex(), a.ey(), border, fill);
    }

    private void renderDropZoneRect(UIContext context, int x, int y, int ex, int ey, int border, int fill)
    {
        context.batcher.box(x, y, ex, ey, fill);
        int t = 2;
        context.batcher.box(x, y, ex, y + t, border);
        context.batcher.box(x, ey - t, ex, ey, border);
        context.batcher.box(x, y, x + t, ey, border);
        context.batcher.box(ex - t, y, ex, ey, border);
    }

    private int getDockGuideBaseColor()
    {
        return BBSSettings.editorDockGuideColor == null ? 0x57CCFF : BBSSettings.editorDockGuideColor.get();
    }

    private float getDockGuideOpacity()
    {
        return BBSSettings.editorDockGuideOpacity == null ? 0.5F : MathUtils.clamp(BBSSettings.editorDockGuideOpacity.get(), 0F, 1F);
    }

    private int withAlpha(int color, float alpha)
    {
        return Colors.setA(color, MathUtils.clamp(alpha, 0F, 1F));
    }

    private void renderSplitter(UIContext context, int index)
    {
        if (index < 0 || index >= this.splitterHandles.size() || index >= this.splitterHandleInfos.size())
        {
            return;
        }

        UIDraggable splitter = this.splitterHandles.get(index);
        EditorLayoutNode.SplitterHandleInfo info = this.splitterHandleInfos.get(index);
        boolean active = splitter.area.isInside(context) || splitter.isDragging();
        int lineColor = active ? BBSSettings.primaryColor(Colors.A50) : 0x22ffffff;

        if (active)
        {
            context.batcher.box(splitter.area.x, splitter.area.y, splitter.area.ex(), splitter.area.ey(), lineColor);
        }

        if (info.horizontal)
        {
            int cy = splitter.area.y + splitter.area.h / 2;
            context.batcher.box(splitter.area.x, cy - 1, splitter.area.ex(), cy + 1, lineColor);
        }
        else
        {
            int cx = splitter.area.x + splitter.area.w / 2;
            context.batcher.box(cx - 1, splitter.area.y, cx + 1, splitter.area.ey(), lineColor);
        }
    }

    public void pickClip(Clip clip, UIClipsPanel panel)
    {
        if (panel == this.cameraEditor)
        {
            this.setFlight(false);
        }
    }

    public int getPanelIndex()
    {
        for (int i = 0; i < this.panels.size(); i++)
        {
            if (this.panels.get(i).isVisible())
            {
                return i;
            }
        }

        return -1;
    }

    public void showPanel(int index)
    {
        this.showPanel(this.panels.get(index));
    }

    public void showPanel(UIElement element)
    {
        this.cameraEditor.setVisible(false);
        this.replayEditor.setVisible(false);
        this.actionEditor.setVisible(false);
        this.screenEditor.setVisible(false);

        element.setVisible(true);

        if (this.isFlying())
        {
            this.toggleFlight();
        }

        /* Re-sync tab visibility so tabbed panels are correctly shown/hidden */
        this.setupEditorFlex(true);
    }

    public UIFilmController getController()
    {
        return this.controller;
    }

    public UIFilmUndoHandler getUndoHandler()
    {
        return this.undoHandler;
    }

    public RunnerCameraController getRunner()
    {
        return this.runner;
    }

    @Override
    protected UICRUDOverlayPanel createOverlayPanel()
    {
        UICRUDOverlayPanel crudPanel = super.createOverlayPanel();

        this.duplicateFilm = new UIIcon(Icons.SCENE, (b) ->
        {
            UIPromptOverlayPanel panel = new UIPromptOverlayPanel(
                UIKeys.GENERAL_DUPE,
                UIKeys.PANELS_MODALS_DUPE,
                (str) -> this.dupeData(crudPanel.namesList.getPath(str).toString())
            );

            panel.text.setText(crudPanel.namesList.getCurrentFirst().getLast());
            panel.text.filename();

            UIOverlay.addOverlay(this.getContext(), panel);
        });

        this.duplicateFilm.tooltip(UIKeys.FILM_CRUD_DUPE, Direction.LEFT);

        return crudPanel;
    }

    private void dupeData(String name)
    {
        if (this.getData() != null && !this.overlay.namesList.hasInHierarchy(name))
        {
            this.save();
            this.overlay.namesList.addFile(name);

            Film data = new Film();
            Position position = new Position();
            IdleClip idle = new IdleClip();
            int tick = this.getCursor();

            position.set(this.getCamera());
            idle.duration.set(BBSSettings.getDefaultDuration());
            idle.position.set(position);
            data.camera.addClip(idle);
            data.setId(name);

            for (Replay replay : this.data.replays.getList())
            {
                Replay copy = new Replay(replay.getId());

                copy.form.set(FormUtils.copy(replay.form.get()));

                for (KeyframeChannel<?> channel : replay.keyframes.getChannels())
                {
                    if (!channel.isEmpty())
                    {
                        KeyframeChannel newChannel = (KeyframeChannel) copy.keyframes.get(channel.getId());

                        newChannel.insert(0, channel.interpolate(tick));
                    }
                }

                for (Map.Entry<String, KeyframeChannel> entry : replay.properties.properties.entrySet())
                {
                    KeyframeChannel channel = entry.getValue();

                    if (channel.isEmpty())
                    {
                        continue;
                    }

                    KeyframeChannel newChannel = new KeyframeChannel(channel.getId(), channel.getFactory());
                    KeyframeSegment segment = channel.find(tick);

                    if (segment != null)
                    {
                        newChannel.insert(0, segment.createInterpolated());
                    }

                    if (!newChannel.isEmpty())
                    {
                        copy.properties.properties.put(newChannel.getId(), newChannel);
                        copy.properties.add(newChannel);
                    }
                }

                data.replays.add(copy);
            }

            this.fill(data);
            this.save();
        }
    }

    private void openLayoutSelector()
    {
        ValueEditorLayout layout = BBSSettings.editorLayoutSettings;
        UIContext context = this.getContext();

        context.replaceContextMenu((menu) ->
        {
            menu.custom(new UISimpleContextMenu()
            {
                @Override
                public void setMouse(UIContext context)
                {
                    int w = 100;

                    for (ContextAction action : this.actions.getList())
                    {
                        w = Math.max(action.getWidth(context.batcher.getFont()), w);
                    }

                    int x = UIFilmPanel.this.toggleHorizontal.area.x;
                    int y = UIFilmPanel.this.toggleHorizontal.area.ey();

                    this.set(x, y, w, 0).h(this.actions.scroll.scrollSize).maxH(context.menu.height - 10).bounds(context.menu.overlay, 5);
                }
            });

            menu.action(Icons.EXCHANGE, UIKeys.FILM_LAYOUT_HORIZONTAL_BOTTOM, layout.getLayout() == ValueEditorLayout.LAYOUT_HORIZONTAL_BOTTOM, () ->
            {
                layout.setLayout(ValueEditorLayout.LAYOUT_HORIZONTAL_BOTTOM);
                this.applyLegacyLayoutSelection();
                this.setupEditorFlex(true);
            });

            menu.action(Icons.CONVERT, UIKeys.FILM_LAYOUT_VERTICAL_LEFT, layout.getLayout() == ValueEditorLayout.LAYOUT_VERTICAL_LEFT, () ->
            {
                layout.setLayout(ValueEditorLayout.LAYOUT_VERTICAL_LEFT);
                this.applyLegacyLayoutSelection();
                this.setupEditorFlex(true);
            });

            menu.action(Icons.ARROW_RIGHT, UIKeys.FILM_LAYOUT_VERTICAL_RIGHT, layout.getLayout() == ValueEditorLayout.LAYOUT_VERTICAL_RIGHT, () ->
            {
                layout.setLayout(ValueEditorLayout.LAYOUT_VERTICAL_RIGHT);
                this.applyLegacyLayoutSelection();
                this.setupEditorFlex(true);
            });

            menu.action(Icons.MAIN_HANDLE, UIKeys.FILM_LAYOUT_VERTICAL_MIDDLE, layout.getLayout() == ValueEditorLayout.LAYOUT_VERTICAL_MIDDLE, () ->
            {
                layout.setLayout(ValueEditorLayout.LAYOUT_VERTICAL_MIDDLE);
                this.applyLegacyLayoutSelection();
                this.setupEditorFlex(true);
            });

        });
    }

    private void toggleLayoutLock()
    {
        ValueEditorLayout layout = BBSSettings.editorLayoutSettings;

        layout.setLayoutLocked(!layout.isLayoutLocked());
        this.clearPanelDragState();
        this.updateLayoutLockTooltip();
        this.setupEditorFlex(true);
    }

    private void updateLayoutLockTooltip()
    {
        ValueEditorLayout layout = BBSSettings.editorLayoutSettings;
        boolean locked = layout.isLayoutLocked();

        this.layoutLock.active(locked);

        if (locked)
        {
            this.layoutLock.tooltip(UIKeys.FILM_LAYOUT_UNLOCK, Direction.LEFT);
        }
        else
        {
            this.layoutLock.tooltip(UIKeys.FILM_LAYOUT_LOCK, Direction.LEFT);
        }
    }

    private void invertSelectedLayout()
    {
        ValueEditorLayout layout = BBSSettings.editorLayoutSettings;

        if (layout.getLayout() == ValueEditorLayout.LAYOUT_HORIZONTAL_BOTTOM)
        {
            layout.setHorizontalLayoutInverted(!layout.isHorizontalLayoutInverted());
        }
        else if (layout.getLayout() == ValueEditorLayout.LAYOUT_HORIZONTAL_TOP)
        {
            layout.setHorizontalLayoutInverted(!layout.isHorizontalLayoutInverted());
        }
        else if (layout.getLayout() == ValueEditorLayout.LAYOUT_VERTICAL_LEFT)
        {
            layout.setVerticalLayoutInverted(!layout.isVerticalLayoutInverted());
        }
        else if (layout.getLayout() == ValueEditorLayout.LAYOUT_VERTICAL_RIGHT)
        {
            layout.setVerticalLayoutInverted(!layout.isVerticalLayoutInverted());
        }
        else if (layout.getLayout() == ValueEditorLayout.LAYOUT_VERTICAL_MIDDLE)
        {
            float editorSize = MathUtils.clamp(layout.getEditorSizeH(), 0.05F, 0.95F);
            float maxMainSize = Math.max(0.05F, 0.95F - editorSize);
            float mirroredMainSize = 1F - layout.getMainSizeV() - editorSize;

            layout.setMainSizeV(MathUtils.clamp(mirroredMainSize, 0.05F, maxMainSize));
            layout.setMiddleLayoutInverted(!layout.isMiddleLayoutInverted());
        }

        this.applyLegacyLayoutSelection();
        this.setupEditorFlex(true);
    }

    private Icon getLayoutIcon()
    {
        ValueEditorLayout layout = BBSSettings.editorLayoutSettings;

        if (layout.getLayout() == ValueEditorLayout.LAYOUT_VERTICAL_RIGHT)
        {
            return Icons.ARROW_RIGHT;
        }

        if (layout.getLayout() == ValueEditorLayout.LAYOUT_VERTICAL_LEFT)
        {
            return Icons.CONVERT;
        }

        if (layout.getLayout() == ValueEditorLayout.LAYOUT_VERTICAL_MIDDLE)
        {
            return Icons.MAIN_HANDLE;
        }

        return Icons.EXCHANGE;
    }

    private void applyLegacyLayoutSelection()
    {
        ValueEditorLayout layout = BBSSettings.editorLayoutSettings;
        layout.setFilmLayoutRoot(layout.buildFilmLayoutFromLegacyState());
    }

    private Icon getLayoutLockIcon()
    {
        return BBSSettings.editorLayoutSettings.isLayoutLocked() ? Icons.LOCKED : Icons.UNLOCKED;
    }

    private MapType getFilmLayoutPresetData()
    {
        MapType data = new MapType();
        data.put("film_layout", BBSSettings.editorLayoutSettings.getFilmLayoutRoot().toData());
        return data;
    }

    private void applyFilmLayoutFromPreset(MapType data, int mouseX, int mouseY)
    {
        BaseType layoutData = data.get("film_layout");
        if (layoutData == null)
        {
            return;
        }

        EditorLayoutNode root = EditorLayoutNode.fromData(layoutData);
        if (root != null)
        {
            BBSSettings.editorLayoutSettings.setFilmLayoutRoot(root);
            this.setupEditorFlex(true);
        }
    }

    @Override
    public void resize()
    {
        super.resize();

        if (this.editor.area.w >= EDITOR_MIN_SIZE_FOR_PX_HANDLES && this.editor.area.h >= EDITOR_MIN_SIZE_FOR_PX_HANDLES)
        {
            this.updateEditorFlexBoundsOnly(BBSSettings.editorLayoutSettings, BBSSettings.editorLayoutSettings.getFilmLayoutRoot());

            this.editor.resize();
        }

        this.updateFilmDocumentView();
    }

    @Override
    public void open()
    {
        super.open();

        Recorder recorder = BBSModClient.getFilms().stopRecording();

        if (recorder == null || recorder.hasNotStarted())
        {
            this.notifyServer(ActionState.RESTART);

            return;
        }

        this.applyRecordedKeyframes(recorder, this.data);
    }

    public void receiveActions(String filmId, int replayId, int tick, BaseType clips)
    {
        Film film = this.data;

        if (film != null && film.getId().equals(filmId) && CollectionUtils.inRange(film.replays.getList(), replayId))
        {
            BaseValue.edit(film.replays.getList().get(replayId), IValueListener.FLAG_UNMERGEABLE, (replay) ->
            {
                Clips newClips = new Clips("", BBSMod.getFactoryActionClips());

                newClips.fromData(clips);
                replay.actions.copyOver(newClips, tick);
            });
        }

        this.save();
    }

    public void applyRecordedKeyframes(Recorder recorder, Film film)
    {
        int replayId = recorder.exception;
        Replay rp = CollectionUtils.getSafe(film.replays.getList(), replayId);

        if (rp != null)
        {
            BaseValue.edit(film, (f) ->
            {
                rp.keyframes.copyOver(recorder.keyframes, 0);

                Form form = rp.form.get();

                if (form != null)
                {
                    for (Map.Entry<String, KeyframeChannel> entry : recorder.properties.properties.entrySet())
                    {
                        KeyframeChannel channel = rp.properties.getOrCreate(form, entry.getKey());

                        if (channel != null && entry.getValue() != null)
                        {
                            channel.copyOver(entry.getValue(), 0);
                        }
                    }
                }

                rp.inventory.fromData(recorder.inventory.toData());
                f.hp.set(recorder.hp);
                f.hunger.set(recorder.hunger);
                f.xpLevel.set(recorder.xpLevel);
                f.xpProgress.set(recorder.xpProgress);
            });
        }
    }

    @Override
    public void appear()
    {
        super.appear();

        BBSRendering.setCustomSize(true);
        MorphRenderer.hidePlayer = true;

        CameraController cameraController = this.getCameraController();

        this.fillData();
        this.setFlight(false);
        cameraController.add(this.runner);

        this.getContext().menu.getRoot().add(this.secretPlay);
    }

    @Override
    public void close()
    {
        super.close();

        BBSRendering.setCustomSize(false);
        MorphRenderer.hidePlayer = false;
        VideoRenderer.stopAll();

        CameraController cameraController = this.getCameraController();

        this.cameraEditor.embedView(null);
        this.screenEditor.embedView(null);
        this.setFlight(false);
        cameraController.remove(this.runner);

        this.disableContext();
        this.replayEditor.close();

        this.notifyServer(ActionState.STOP);
    }

    @Override
    public void disappear()
    {
        VideoRenderer.cleanup();

        super.disappear();

        BBSRendering.setCustomSize(false);
        MorphRenderer.hidePlayer = false;

        this.setFlight(false);
        this.getCameraController().remove(this.runner);

        this.disableContext();
        this.secretPlay.removeFromParent();
    }

    private void disableContext()
    {
        this.runner.getContext().shutdown();
    }

    @Override
    public boolean needsBackground()
    {
        return false;
    }

    @Override
    public boolean canPause()
    {
        return false;
    }

    @Override
    public boolean canRefresh()
    {
        return false;
    }

    @Override
    public ContentType getType()
    {
        return ContentType.FILMS;
    }

    @Override
    public IKey getTitle()
    {
        return UIKeys.FILM_TITLE;
    }

    @Override
    public void pickData(String id)
    {
        this.save();
        this.openFilmInDocumentTabs(id);
    }

    @Override
    public void requestNames()
    {
        UIDataUtils.requestNames(this.getType(), this::fillNames);
    }

    @Override
    public void fillNames(Collection<String> names)
    {
        super.fillNames(names);

        DataPath selected = this.homeFilmsList.getCurrentFirst();
        String current = selected != null && !selected.folder ? selected.toString() : null;

        this.homeFilmsList.fill(names);
        this.homeFilmsList.setCurrentFile(current);
        this.updateHomeButtonsState();
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

    @Override
    public void fillDefaultData(Film data)
    {
        super.fillDefaultData(data);

        IdleClip clip = new IdleClip();
        Camera camera = new Camera();
        MinecraftClient mc = MinecraftClient.getInstance();

        camera.set(mc.player, MathUtils.toRad(mc.options.getFov().getValue()));

        clip.layer.set(8);
        clip.duration.set(BBSSettings.getDefaultDuration());
        clip.fromCamera(camera);
        data.camera.addClip(clip);

        this.newFilm = true;
    }

    @Override
    public void fill(Film data)
    {
        this.notifyServer(ActionState.STOP);
        super.fill(data);
        this.editor.setVisible(true);

        if (data != null)
        {
            this.notifyServer(ActionState.RESTART);
        }

        this.syncActiveDocumentTabWithData(data);
    }

    @Override
    protected void fillData(Film data)
    {
        if (this.data != null)
        {
            this.disableContext();
        }

        if (data != null)
        {
            this.undoHandler = new UIFilmUndoHandler(this);

            data.preCallback(this.undoHandler::handlePreValues);
        }
        else
        {
            this.undoHandler = null;
            BBSModClient.setSelectedReplay(null);
        }

        this.preview.replays.setEnabled(data != null);
        this.openHistory.setEnabled(data != null);
        this.toggleHorizontal.setEnabled(data != null);
        this.layoutLock.setEnabled(data != null);
        this.layoutPresets.setEnabled(data != null);
        this.openCameraEditor.setEnabled(data != null);
        this.openReplayEditor.setEnabled(data != null);
        this.openActionEditor.setEnabled(data != null);
        this.openScreenEditor.setEnabled(data != null);
        this.duplicateFilm.setEnabled(data != null);

        this.actionEditor.setClips(null);
        this.runner.setWork(data == null ? null : data.camera);
        this.cameraEditor.setClips(data == null ? null : data.camera);
        this.screenEditor.setClips(data == null ? null : data.screen);
        this.replayEditor.setFilm(data);
        this.cameraEditor.pickClip(null);

        this.fillData();
        this.controller.createEntities();

        if (this.newFilm)
        {
            Clip main = this.data.camera.get(0);

            this.cameraEditor.clips.setSelected(main);
            this.cameraEditor.pickClip(main);
        }

        this.entered = data != null;
        this.updateHomeButtonsState();
        this.newFilm = false;
    }

    public void undo()
    {
        if (this.data != null && this.undoHandler.undo(this.data)) UIUtils.playClick();
    }

    public void redo()
    {
        if (this.data != null && this.undoHandler.redo(this.data)) UIUtils.playClick();
    }

    public boolean isFlying()
    {
        return this.dashboard.orbitUI.canControl();
    }

    public void toggleFlight()
    {
        this.setFlight(!this.isFlying());
    }

    /**
     * Set flight mode
     */
    public void setFlight(boolean flight)
    {
        if (!this.isRunning() || !flight)
        {
            this.runner.setManual(flight ? this.position : null);
            this.dashboard.orbitUI.setControl(flight);
            this.updateFreeFlightMouseCapture(flight);

            /* Marking the latest undo as unmergeable */
            if (this.undoHandler != null && !flight)
            {
                this.undoHandler.getUndoManager().markLastUndoNoMerging();
            }
            else
            {
                this.lastPosition.set(Position.ZERO);
            }
        }
    }

    public Vector2i getLoopingRange()
    {
        Clip clip = this.cameraEditor.getClip();

        int min = -1;
        int max = -1;

        if (clip != null)
        {
            min = clip.tick.get();
            max = min + clip.duration.get();
        }

        UIClips clips = this.cameraEditor.clips;

        if (clips.loopMin != clips.loopMax && clips.loopMin >= 0 && clips.loopMin < clips.loopMax)
        {
            min = clips.loopMin;
            max = clips.loopMax;
        }

        max = Math.min(max, this.data.camera.calculateDuration());

        return new Vector2i(min, max);
    }

    @Override
    public void update()
    {
        this.controller.update();

        if (BBSSettings.editorCameraPreviewPlayerSync.get() && this.data != null && this.controller.getPovMode() == UIFilmController.CAMERA_MODE_CAMERA)
        {
            this.teleportToCamera();
        }

        super.update();
    }

    /* Rendering code */

    @Override
    public void renderPanelBackground(UIContext context)
    {
        super.renderPanelBackground(context);

        Texture texture = BBSRendering.getTexture();

        if (texture != null)
        {
            context.batcher.box(0, 0, context.menu.width, context.menu.height, Colors.A100);

            int w = context.menu.width;
            int h = context.menu.height;
            Vector2i resize = Vectors.resize(texture.width / (float) texture.height, w, h);
            Area area = new Area();

            area.setSize(resize.x, resize.y);
            area.setPos((w - area.w) / 2, (h - area.h) / 2);

            context.batcher.texturedBox(texture.id, Colors.WHITE, area.x, area.y, area.w, area.h, 0, texture.height, texture.width, 0, texture.width, texture.height);
        }

        this.updateLogic(context);
    }

    @Override
    protected void renderBackground(UIContext context)
    {
        super.renderBackground(context);

        if (this.cameraEditor.isVisible()) UIDashboardPanels.renderHighlightHorizontal(context.batcher, this.openCameraEditor.area);
        if (this.replayEditor.isVisible()) UIDashboardPanels.renderHighlightHorizontal(context.batcher, this.openReplayEditor.area);
        if (this.actionEditor.isVisible()) UIDashboardPanels.renderHighlightHorizontal(context.batcher, this.openActionEditor.area);
        if (this.screenEditor.isVisible()) UIDashboardPanels.renderHighlightHorizontal(context.batcher, this.openScreenEditor.area);
        if (BBSSettings.editorLayoutSettings.isLayoutLocked()) UIDashboardPanels.renderHighlightHorizontal(context.batcher, this.layoutLock.area);

    }

    /**
     * Draw everything on the screen
     */
    @Override
    public void render(UIContext context)
    {
        if (this.data != null)
        {
            /*
            int tick = this.getCursor();

            for (Clip clip : this.data.camera.get())
            {
                if (clip instanceof VideoClip && clip.isInside(tick) && clip.enabled.get())
                {
                    VideoClip video = (VideoClip) clip;

                    VideoRenderer.render(context.batcher.getContext().getMatrices(),
                        video.video.get(),
                        tick - video.tick.get() + video.offset.get(),
                        this.runner.isRunning(),
                        video.volume.get());
                }
            }
            */
        }

        if (this.controller.isControlling())
        {
            context.mouseX = context.mouseY = -1;
        }

        this.controller.orbit.update(context);

        if (this.undoHandler != null)
        {
            this.undoHandler.submitUndo();
        }

        this.updateLogic(context);

        int color = BBSSettings.primaryColor.get();

        this.area.render(context.batcher, Colors.mulRGB(color | Colors.A100, 0.2F));

        if (this.editor.isVisible() && this.preview.isVisible())
        {
            this.preview.area.render(context.batcher, Colors.A75);
        }

        super.render(context);
        this.renderDropZoneHighlight(context);

        if (this.entered)
        {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            Vec3d pos = player.getPos();
            Vector3d cameraPos = this.camera.position;
            double distance = cameraPos.distance(pos.x, pos.y, pos.z);
            int value = MinecraftClient.getInstance().options.getViewDistance().getValue();

            if (distance > value * 12)
            {
                this.getContext().notifyError(UIKeys.FILM_TELEPORT_DESCRIPTION);
            }

            this.entered = false;
        }
    }

    /**
     * Update logic for such components as repeat fixture, minema recording,
     * sync mode, flight mode, etc.
     */
    private void updateLogic(UIContext context)
    {
        Clip clip = this.cameraEditor.getClip();

        /* Loop fixture */
        if (BBSSettings.editorLoop.get() && this.isRunning())
        {
            Vector2i loop = this.getLoopingRange();
            int min = loop.x;
            int max = loop.y;
            int ticks = this.getCursor();

            if (!this.recorder.isRecording() && !this.controller.isRecording() && min >= 0 && max >= 0 && min < max && (ticks >= max - 1 || ticks < min))
            {
                this.setCursor(min);
            }
        }

        /* Animate flight mode */
        if (this.dashboard.orbitUI.canControl())
        {
            if (BBSSettings.editorFlightFreeLook.get())
            {
                this.updateFreeFlightLookFromRawCursor();
            }
            else if (this.resetFreeFlightLookDrag || this.freeFlightLookPrimed)
            {
                this.resetFreeFlightLookDrag = false;
                this.freeFlightLookPrimed = false;
                this.dashboard.orbitUI.orbit.release();
            }

            this.dashboard.orbit.apply(this.position);

            Position current = new Position(this.getCamera());
            boolean check = this.flightEditTime.check();

            if (this.cameraEditor.getClip() != null && this.cameraEditor.isVisible() && this.controller.getPovMode() != UIFilmController.CAMERA_MODE_FREE)
            {
                if (!this.lastPosition.equals(current) && check)
                {
                    this.cameraEditor.editClip(current);
                }
            }

            if (check)
            {
                this.lastPosition.set(current);
            }
        }
        else
        {
            this.dashboard.orbit.setup(this.getCamera());
        }

        /* Rewind playback back to 0 */
        if (this.lastRunning && !this.isRunning())
        {
            this.lastRunning = this.runner.isRunning();

            if (BBSSettings.editorRewind.get())
            {
                this.setCursor(0);
                this.notifyServer(ActionState.RESTART);
            }
        }
    }

    private void updateFreeFlightMouseCapture(boolean flight)
    {
        if (!BBSSettings.editorFlightFreeLook.get())
        {
            return;
        }

        net.minecraft.client.util.Window window = MinecraftClient.getInstance().getWindow();

        if (flight)
        {
            this.centerCursor(window);
            GLFW.glfwSetInputMode(window.getHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
            this.resetFreeFlightLookDrag = true;
            this.freeFlightLookPrimed = false;
            this.dashboard.orbitUI.orbit.release();
        }
        else if (!this.controller.isControlling())
        {
            GLFW.glfwSetInputMode(window.getHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
            this.resetFreeFlightLookDrag = false;
            this.freeFlightLookPrimed = false;
            this.dashboard.orbitUI.orbit.release();
        }
    }

    private boolean enforceFreeFlightMouseCapture()
    {
        if (!this.isFlying() || !BBSSettings.editorFlightFreeLook.get())
        {
            return false;
        }

        net.minecraft.client.util.Window window = MinecraftClient.getInstance().getWindow();

        if (GLFW.glfwGetInputMode(window.getHandle(), GLFW.GLFW_CURSOR) != GLFW.GLFW_CURSOR_DISABLED)
        {
            this.centerCursor(window);
            GLFW.glfwSetInputMode(window.getHandle(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
            return true;
        }

        return false;
    }

    private void centerCursor(net.minecraft.client.util.Window window)
    {
        mchorse.bbs_mod.graphics.window.Window.moveCursor(window.getWidth() / 2, window.getHeight() / 2);
    }

    private void updateFreeFlightLookFromRawCursor()
    {
        net.minecraft.client.util.Window window = MinecraftClient.getInstance().getWindow();

        if (this.enforceFreeFlightMouseCapture())
        {
            this.resetFreeFlightLookDrag = true;
            this.freeFlightLookPrimed = false;
        }

        double[] rawX = new double[1];
        double[] rawY = new double[1];
        GLFW.glfwGetCursorPos(window.getHandle(), rawX, rawY);
        int mouseX = (int) Math.round(rawX[0]);
        int mouseY = (int) Math.round(rawY[0]);

        if (this.resetFreeFlightLookDrag || !this.freeFlightLookPrimed)
        {
            this.freeFlightLookRawX = mouseX;
            this.freeFlightLookRawY = mouseY;

            this.resetFreeFlightLookDrag = false;
            this.freeFlightLookPrimed = true;
            this.dashboard.orbitUI.orbit.release();

            return;
        }

        int dx = mouseX - this.freeFlightLookRawX;
        int dy = mouseY - this.freeFlightLookRawY;

        this.freeFlightLookRawX = mouseX;
        this.freeFlightLookRawY = mouseY;

        if (dx != 0 || dy != 0)
        {
            float angleSpeed = this.dashboard.orbitUI.orbit.getAngleSpeed();

            this.dashboard.orbitUI.orbit.rotation.x += dy * angleSpeed;
            this.dashboard.orbitUI.orbit.rotation.y += dx * angleSpeed;
        }
    }

    @Override
    protected IUIElement childrenMouseScrolled(UIContext context)
    {
        if (this.dashboard.orbitUI.canControl() && context.mouseWheel != 0D)
        {
            int step = (int) Math.copySign(1, context.mouseWheel);

            this.dashboard.orbitUI.orbit.scroll(step);

            /* Consume scroll so other sections won't react */
            context.mouseWheel = 0D;

            return this;
        }

        return super.childrenMouseScrolled(context);
    }

    /**
     * Draw icons for indicating different active states (like syncing
     * or flight mode)
     */
    private void renderIcons(UIContext context)
    {
        if (this.showingHomePage)
        {
            return;
        }

        int x = this.iconBar.area.ex() - 18;
        int y = this.iconBar.area.ey() - 18;

        if (BBSSettings.editorLoop.get())
        {
            context.batcher.icon(Icons.REFRESH, x, y);
        }
    }

    private void renderDividers(UIContext context)
    {
        if (this.showingHomePage)
        {
            return;
        }

        Area a1 = this.openHistory.area;

        context.batcher.box(a1.x + 3, a1.ey() + 4, a1.ex() - 3, a1.ey() + 5, 0x22ffffff);
    }

    @Override
    public void startRenderFrame(float tickDelta)
    {
        super.startRenderFrame(tickDelta);

        this.controller.startRenderFrame(tickDelta);
    }

    @Override
    public void renderInWorld(WorldRenderContext context)
    {
        super.renderInWorld(context);

        if (!BBSRendering.isIrisShadowPass())
        {
            this.lastProjection.set(RenderSystem.getProjectionMatrix());
            MatrixStack ms = context.matrixStack();
            if (ms != null)
            {
                this.lastView.set(ms.peek().getPositionMatrix());
            }
            else
            {
                this.lastView.set(RenderSystem.getModelViewMatrix());
            }
        }

        this.controller.renderFrame(context);
    }

    /* IUICameraWorkDelegate implementation */

    public void notifyServer(ActionState state)
    {
        if (this.data == null || !ClientNetwork.isIsBBSModOnServer())
        {
            return;
        }

        String id = this.data.getId();
        int tick = this.getCursor();

        ClientNetwork.sendActionState(id, state, tick);
    }

    public Camera getCamera()
    {
        return this.camera;
    }

    public Camera getWorldCamera()
    {
        return BBSModClient.getCameraController().camera;
    }

    public CameraController getCameraController()
    {
        return BBSModClient.getCameraController();
    }

    @Override
    public int getCursor()
    {
        return this.runner.ticks;
    }

    @Override
    public void setCursor(int value)
    {
        this.flightEditTime.mark();
        this.lastPosition.set(Position.ZERO);

        this.runner.ticks = Math.max(0, value);

        this.notifyServer(ActionState.SEEK);
    }

    public boolean isRunning()
    {
        return this.runner.isRunning();
    }

    public void togglePlayback()
    {
        this.setFlight(false);

        this.runner.toggle(this.getCursor());
        this.lastRunning = this.runner.isRunning();

        if (this.runner.isRunning())
        {
            this.cameraEditor.clips.scale.shiftIntoMiddle(this.getCursor());

            if (this.replayEditor.keyframeEditor != null)
            {
                this.replayEditor.keyframeEditor.view.getXAxis().shiftIntoMiddle(this.getCursor());
            }
        }
    }

    public boolean canUseKeybinds()
    {
        return !this.isFlying();
    }

    public void fillData()
    {
        this.cameraEditor.fillData();
        this.actionEditor.fillData();
        this.screenEditor.fillData();

        if (this.replayEditor.keyframeEditor != null && this.replayEditor.keyframeEditor.editor != null)
        {
            this.replayEditor.keyframeEditor.editor.update();
        }
    }

    public void teleportToCamera()
    {
        Camera camera = this.getCamera();
        Vector3d cameraPos = camera.position;
        double x = cameraPos.x;
        double y = cameraPos.y;
        double z = cameraPos.z;

        PlayerUtils.teleport(x, y, z, MathUtils.toDeg(camera.rotation.y) - 180F, MathUtils.toDeg(camera.rotation.x));
    }

    public boolean checkShowNoCamera()
    {
        boolean noCamera = this.getData().camera.calculateDuration() <= 0;

        if (noCamera)
        {
            UIOverlay.addOverlay(this.getContext(), new UIMessageOverlayPanel(
                UIKeys.FILM_NO_CAMERA_TITLE,
                UIKeys.FILM_NO_CAMERA_DESCRIPTION
            ));
        }

        return noCamera;
    }

    public void updateActors(String filmId, Map<String, Integer> actors)
    {
        if (this.data != null && this.data.getId().equals(filmId))
        {
            this.controller.updateActors(actors);
        }
    }

    @Override
    public boolean handleKeyPressed(UIContext context)
    {
        return this.controller.orbit.keyPressed(context, this.preview.area);
    }

    @Override
    public void applyUndoData(MapType data)
    {
        super.applyUndoData(data);

        this.showPanel(data.getInt("panel"));
        this.setCursor(data.getInt("tick"));
        this.controller.createEntities();
    }

    @Override
    public void collectUndoData(MapType data)
    {
        super.collectUndoData(data);

        data.putInt("panel", this.getPanelIndex());
        data.putInt("tick", this.getCursor());
    }

    @Override
    protected boolean canSave(UIContext context)
    {
        return !this.recorder.isRecording();
    }

    private void openSelectedFilmFromHome()
    {
        String selected = this.getSelectedHomeFilmId();

        if (selected != null)
        {
            this.openFilmInDocumentTabs(selected);
        }
    }

    private String getSelectedHomeFilmId()
    {
        DataPath selected = this.homeFilmsList.getCurrentFirst();

        if (selected == null || selected.folder)
        {
            return null;
        }

        return selected.toString();
    }

    private void handleHomeFilmsSelection(List<DataPath> list)
    {
        String selected = this.getSelectedHomeFilmId();
        this.overlay.namesList.setCurrentFile(selected);

        this.updateHomeButtonsState();

        if (selected == null)
        {
            return;
        }

        long now = System.currentTimeMillis();
        boolean sameAsPrevious = selected.equals(this.homeLastClickedFilmId);
        boolean doubleClick = sameAsPrevious && now - this.homeLastClickTime <= 300L;

        this.homeLastClickedFilmId = selected;
        this.homeLastClickTime = now;

        if (doubleClick)
        {
            this.openFilmInDocumentTabs(selected);
        }
    }

    private void updateHomeButtonsState()
    {
        boolean hasSelectedFilm = this.homeFilmsList.getCurrentFirst() != null;
        boolean enableIcons = !this.showingHomePage;

        if (this.homeDuplicateCurrent != null)
        {
            this.homeDuplicateCurrent.setEnabled(hasSelectedFilm);
        }

        if (this.homeRenameCurrent != null)
        {
            this.homeRenameCurrent.setEnabled(hasSelectedFilm);
        }

        if (this.homeDeleteCurrent != null)
        {
            this.homeDeleteCurrent.setEnabled(hasSelectedFilm);
        }

        if (this.openHistory != null) this.openHistory.setEnabled(enableIcons);
        if (this.openRenderQueue != null) this.openRenderQueue.setEnabled(enableIcons);
        if (this.openOverlay != null) this.openOverlay.setEnabled(enableIcons);
        if (this.saveIcon != null) this.saveIcon.setEnabled(enableIcons);
        if (this.toggleHorizontal != null) this.toggleHorizontal.setEnabled(enableIcons);
        
        if (this.openCameraEditor != null) this.openCameraEditor.setEnabled(enableIcons);
        if (this.openReplayEditor != null) this.openReplayEditor.setEnabled(enableIcons);
        if (this.openActionEditor != null) this.openActionEditor.setEnabled(enableIcons);
        if (this.openScreenEditor != null) this.openScreenEditor.setEnabled(enableIcons);
        if (this.layoutLock != null) this.layoutLock.setEnabled(enableIcons);
        if (this.layoutPresets != null) this.layoutPresets.setEnabled(enableIcons);
    }

    private UIButton createHomeButton(IKey label, Icon icon, Consumer<UIButton> callback)
    {
        UIButton button = new UIButton(label, callback) {
            @Override
            protected void renderSkin(UIContext context)
            {
                int bg = this.hover ? Colors.setA(Colors.WHITE, 0.25F) : Colors.setA(0, 0.4F);
                this.area.render(context.batcher, bg);

                int color = this.isEnabled() ? Colors.LIGHTEST_GRAY : 0x44ffffff;

                if (icon != null) {
                    context.batcher.icon(icon, color, this.area.x + 4, this.area.y + this.area.h / 2 - icon.h / 2);
                }

                context.batcher.textShadow(this.label.get(), this.area.x + 22, this.area.y + this.area.h / 2 - 4, color);
            }
        };
        button.h(20);
        return button;
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

    private void createHomeDocumentTab(boolean activate)
    {
        this.filmDocumentTabs.add(new FilmDocumentTab(true, null));
        int index = this.filmDocumentTabs.size() - 1;

        this.rebuildFilmDocumentTabs();

        if (activate)
        {
            this.activateFilmDocumentTab(index, false);
        }
    }

    private void addHomeDocumentTab()
    {
        int insertAt = Math.max(0, this.activeFilmDocumentTab + 1);

        this.filmDocumentTabs.add(insertAt, new FilmDocumentTab(true, null));
        this.rebuildFilmDocumentTabs();
        this.activateFilmDocumentTab(insertAt, false);
    }

    private void ensureHomeDocumentTab()
    {
        for (FilmDocumentTab tab : this.filmDocumentTabs)
        {
            if (tab.home)
            {
                return;
            }
        }

        this.filmDocumentTabs.add(new FilmDocumentTab(true, null));
    }

    private int findTabByFilmId(String id)
    {
        for (int i = 0; i < this.filmDocumentTabs.size(); i++)
        {
            FilmDocumentTab tab = this.filmDocumentTabs.get(i);

            if (!tab.home && id.equals(tab.filmId))
            {
                return i;
            }
        }

        return -1;
    }

    private void openFilmInDocumentTabs(String id)
    {
        if (id == null || id.trim().isEmpty())
        {
            return;
        }

        int existingIndex = this.findTabByFilmId(id);

        if (existingIndex >= 0)
        {
            this.activateFilmDocumentTab(existingIndex, true);

            return;
        }

        if (this.activeFilmDocumentTab < 0 || this.activeFilmDocumentTab >= this.filmDocumentTabs.size())
        {
            if (this.filmDocumentTabs.isEmpty())
            {
                this.filmDocumentTabs.add(new FilmDocumentTab(true, null));
            }

            this.activeFilmDocumentTab = 0;
        }

        FilmDocumentTab active = this.filmDocumentTabs.get(this.activeFilmDocumentTab);

        if (active.home)
        {
            active.home = false;
            active.filmId = id;
            this.rebuildFilmDocumentTabs();
            this.activateFilmDocumentTab(this.activeFilmDocumentTab, true);
        }
        else
        {
            int insertAt = this.activeFilmDocumentTab + 1;
            this.filmDocumentTabs.add(insertAt, new FilmDocumentTab(false, id));
            this.rebuildFilmDocumentTabs();
            this.activateFilmDocumentTab(insertAt, true);
        }
    }

    private void activateFilmDocumentTab(int index, boolean loadFilm)
    {
        if (index < 0 || index >= this.filmDocumentTabs.size())
        {
            return;
        }

        if (this.data != null && this.activeFilmDocumentTab != index)
        {
            this.save();
        }

        this.activeFilmDocumentTab = index;

        FilmDocumentTab tab = this.filmDocumentTabs.get(index);

        if (tab.home)
        {
            this.updateFilmDocumentView();
        }
        else
        {
            if (loadFilm || this.data == null || this.data.getId() == null || !this.data.getId().equals(tab.filmId))
            {
                this.requestData(tab.filmId);
            }
            else
            {
                this.updateFilmDocumentView();
            }
        }

        this.rebuildFilmDocumentTabs();
    }

    private void removeFilmDocumentTab(int index)
    {
        if (index < 0 || index >= this.filmDocumentTabs.size())
        {
            return;
        }

        this.filmDocumentTabs.remove(index);

        if (this.filmDocumentTabs.isEmpty())
        {
            this.filmDocumentTabs.add(new FilmDocumentTab(true, null));
            this.activeFilmDocumentTab = 0;
            this.rebuildFilmDocumentTabs();
            this.activateFilmDocumentTab(0, false);

            return;
        }

        if (index < this.activeFilmDocumentTab)
        {
            this.activeFilmDocumentTab--;
        }
        else if (index == this.activeFilmDocumentTab)
        {
            this.activeFilmDocumentTab = Math.max(0, Math.min(this.activeFilmDocumentTab, this.filmDocumentTabs.size() - 1));
        }

        this.rebuildFilmDocumentTabs();
        this.activateFilmDocumentTab(this.activeFilmDocumentTab, false);
    }

    private void rebuildFilmDocumentTabs()
    {
        this.filmTabs.removeAll();

        for (int i = 0; i < this.filmDocumentTabs.size(); i++)
        {
            int tabIndex = i;
            FilmDocumentTab tab = this.filmDocumentTabs.get(i);
            IKey title = tab.home ? L10n.lang("bbs.ui.film.home.title") : IKey.constant(tab.filmId);
            UIIconTabButton button = new UIIconTabButton(title, tab.home ? Icons.FOLDER : Icons.FILM, (b) -> this.activateFilmDocumentTab(tabIndex, false));
            button.color(this.activeFilmDocumentTab == tabIndex ? BBSSettings.primaryColor.get() : 0x2d2d2d);
            button.w(tab.home ? 88 : 122).h(FILM_DOCUMENT_TABS_HEIGHT);

            if (!tab.home || this.filmDocumentTabs.size() > 1)
            {
                button.removable((b) -> this.removeFilmDocumentTab(tabIndex));
            }

            this.filmTabs.add(button);
        }

        UIIconTabButton add = new UIIconTabButton(IKey.constant(""), Icons.ADD, (b) -> this.addHomeDocumentTab());
        add.color(0x2d2d2d);
        add.background(false);
        add.w(24).h(FILM_DOCUMENT_TABS_HEIGHT);
        this.filmTabs.add(add);
        this.filmTabs.resize();
    }

    private void syncActiveDocumentTabWithData(Film data)
    {
        if (data != null)
        {
            if (this.activeFilmDocumentTab < 0 || this.activeFilmDocumentTab >= this.filmDocumentTabs.size())
            {
                this.filmDocumentTabs.add(new FilmDocumentTab(false, data.getId()));
                this.activeFilmDocumentTab = this.filmDocumentTabs.size() - 1;
            }
            else
            {
                FilmDocumentTab tab = this.filmDocumentTabs.get(this.activeFilmDocumentTab);

                tab.home = false;
                tab.filmId = data.getId();
            }
        }

        this.rebuildFilmDocumentTabs();
        this.updateFilmDocumentView();
    }

    private void setWorkspaceVisible(boolean visible)
    {
        if (!visible)
        {
            this.main.setVisible(false);
            this.editArea.setVisible(false);
            this.preview.setVisible(false);
        }
        this.draggableMain.setVisible(visible);
        this.draggableEditor.setVisible(visible);

        for (UIDraggable handle : this.dragHandlesById.values())
        {
            handle.setVisible(visible && !BBSSettings.editorLayoutSettings.isLayoutLocked());
        }

        for (UIDraggable handle : this.splitterHandles)
        {
            handle.setVisible(visible);
        }

        for (UITabBar tabBar : this.tabBars)
        {
            tabBar.setVisible(visible);
        }
    }

    private void updateFilmDocumentView()
    {
        if (this.performingLayout)
        {
            return;
        }

        this.performingLayout = true;

        boolean home = this.activeFilmDocumentTab < 0
            || this.activeFilmDocumentTab >= this.filmDocumentTabs.size()
            || this.filmDocumentTabs.get(this.activeFilmDocumentTab).home
            || this.data == null;

        this.showingHomePage = home;
        this.homePage.setVisible(home);
        this.editor.setVisible(true);
        this.setWorkspaceVisible(!home);
        this.updateHomeButtonsState();
        
        if (!home)
        {
            this.setupEditorFlex(true, false, false);
        }

        this.performingLayout = false;
    }

    private void renderHomeBanner(UIContext context)
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
        int bannerH = HOME_BANNER_HEIGHT;
        int splitY = pageY + bannerH;
        int dividerX = this.homeFilmsSearch.area.x;

        // Render deeper background for the aurora to pop
        context.batcher.box(editorX, editorY, editorX + editorW, editorY + editorH, Colors.setA(0x0b0b0b, 1F));
        
        // Render Animated Aurora Effect
        int primary = BBSSettings.primaryColor.get();
        float tick = context.getTickTransition() * 0.015F;
        int segments = 40; // We can use fewer segments because it interpolates smoothly!
        float segW = editorW / (float) segments;
        
        Matrix4f matrix4f = context.batcher.getContext().getMatrices().peek().getPositionMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder builder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        
        RenderSystem.enableBlend();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        
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
            builder.vertex(matrix4f, x1, yTop1, 0).color(colTop);
            builder.vertex(matrix4f, x1, yMid1[i], 0).color(cMid1[i]);
            builder.vertex(matrix4f, x2, yMid1[i+1], 0).color(cMid1[i+1]);
            builder.vertex(matrix4f, x2, yTop1, 0).color(colTop);
            
            // Layer 1 - Lower Quad (yMid1 -> yBot1)
            builder.vertex(matrix4f, x1, yMid1[i], 0).color(cMid1[i]);
            builder.vertex(matrix4f, x1, yBot1[i], 0).color(colBot);
            builder.vertex(matrix4f, x2, yBot1[i+1], 0).color(colBot);
            builder.vertex(matrix4f, x2, yMid1[i+1], 0).color(cMid1[i+1]);
            
            // Layer 2 - Upper Quad (yTop2 -> yMid2)
            builder.vertex(matrix4f, x1, yTop2, 0).color(colTop);
            builder.vertex(matrix4f, x1, yMid2[i], 0).color(cMid2[i]);
            builder.vertex(matrix4f, x2, yMid2[i+1], 0).color(cMid2[i+1]);
            builder.vertex(matrix4f, x2, yTop2, 0).color(colTop);
            
            // Layer 2 - Lower Quad (yMid2 -> yBot2)
            builder.vertex(matrix4f, x1, yMid2[i], 0).color(cMid2[i]);
            builder.vertex(matrix4f, x1, yBot2[i], 0).color(colBot);
            builder.vertex(matrix4f, x2, yBot2[i+1], 0).color(colBot);
            builder.vertex(matrix4f, x2, yMid2[i+1], 0).color(cMid2[i+1]);
        }
        
        BufferRenderer.drawWithGlobalProgram(builder.end());
        
        // Black shadow gradients on the sides of the central column
        context.batcher.gradientHBox(pageX - 18, pageY, pageX, pageY + pageH, 0, Colors.setA(0x000000, 0.7F));
        context.batcher.gradientHBox(pageX + pageW, pageY, pageX + pageW + 18, pageY + pageH, Colors.setA(0x000000, 0.7F), 0);
        
        // Panel backgrounds
        context.batcher.box(pageX, pageY, pageX + pageW, pageY + pageH, Colors.setA(0x1e1e1e, 1F));
        
        // Background stripe drawing
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
        
        context.batcher.box(pageX, splitY, pageX + pageW, splitY + 1, Colors.A12);
        context.batcher.box(dividerX, splitY + 1, dividerX + 1, pageY + pageH, Colors.A12);
        context.batcher.textShadow(L10n.lang("bbs.ui.film.home.actions").get(), pageX + 4, splitY + 6);
        context.batcher.textShadow(L10n.lang("bbs.ui.film.home.list").get(), dividerX + 4, splitY + 6);
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

    public static class BannerEntry
    {
        public String author;
        public String url;
        public transient Link link;
    }

    private static class FilmDocumentTab
    {
        private boolean home;
        private String filmId;

        private FilmDocumentTab(boolean home, String filmId)
        {
            this.home = home;
            this.filmId = filmId;
        }
    }

    private final List<UITabBar> tabBars = new ArrayList<>();

    private void setupTabBars(EditorLayoutNode root, Map<String, float[]> bounds, boolean recreate)
    {
        List<EditorLayoutNode.TabbedNode> tabbedNodes = new ArrayList<>();
        EditorLayoutNode.collectTabbedNodes(root, tabbedNodes);
        
        int tabBarIndex = 0;
        for (int i = 0; i < tabbedNodes.size(); i++)
        {
            EditorLayoutNode.TabbedNode tabbed = tabbedNodes.get(i);
            if (tabbed.tabs.size() < 2) continue;
            
            int safeActiveTab = Math.max(0, Math.min(tabbed.tabs.size() - 1, tabbed.activeTab));
            EditorLayoutNode activeNode = tabbed.tabs.get(safeActiveTab);
            
            if (activeNode instanceof EditorLayoutNode.PanelNode)
            {
                String activeId = ((EditorLayoutNode.PanelNode) activeNode).getPanelId();
                float[] b = bounds.get(activeId);
                if (b != null)
                {
                    UITabBar tabBar;
                    if (recreate)
                    {
                        tabBar = new UITabBar(this, tabbed);
                        this.tabBars.add(tabBar);
                        this.editor.add(tabBar);
                    }
                    else if (tabBarIndex < this.tabBars.size())
                    {
                        tabBar = this.tabBars.get(tabBarIndex);
                    }
                    else
                    {
                        continue;
                    }
                    
                    tabBarIndex++;
                    
                    tabBar.relative(this.editor).x(b[0]).y(b[1]).w(b[2]).h(0F, 20);
                    tabBar.setVisible(true);
                    
                    boolean locked = BBSSettings.editorLayoutSettings.isLayoutLocked();
                    
                    for (EditorLayoutNode tab : tabbed.tabs)
                    {
                        if (tab instanceof EditorLayoutNode.PanelNode)
                        {
                            String panelId = ((EditorLayoutNode.PanelNode) tab).getPanelId();
                            UIElement el = this.panelById.get(panelId);
                            if (el != null)
                            {
                                el.relative(this.editor).x(b[0]).y(b[1], 20).w(b[2]).h(b[3], -20);
                                boolean isActive = tab == activeNode;
                                el.setVisible(isActive);
                                
                                UIDraggable handle = this.dragHandlesById.get(panelId);
                                if (handle != null) handle.setVisible(isActive && !BBSSettings.editorLayoutSettings.isLayoutLocked());
                            }
                            
                            UIDraggable handle = this.dragHandlesById.get(panelId);
                            if (handle != null)
                            {
                                if (locked)
                                {
                                    handle.setVisible(false);
                                }
                                else
                                {
                                    handle.setVisible(tab == activeNode);
                                    if (tab == activeNode)
                                    {
                                        handle.relative(this.editor).x(b[0]).y(b[1] + DRAG_HANDLE_TOP_OFFSET_NORM, 20).w(b[2]).h(DRAG_HANDLE_HEIGHT_NORM);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static class UITabBar extends UIElement
    {
        private UIFilmPanel panel;
        private EditorLayoutNode.TabbedNode tabbedNode;

        public UITabBar(UIFilmPanel panel, EditorLayoutNode.TabbedNode tabbedNode)
        {
            this.panel = panel;
            this.tabbedNode = tabbedNode;
            
            for (int i = 0; i < tabbedNode.tabs.size(); i++)
            {
                EditorLayoutNode tab = tabbedNode.tabs.get(i);
                if (tab instanceof EditorLayoutNode.PanelNode)
                {
                    String panelId = ((EditorLayoutNode.PanelNode) tab).getPanelId();
                    UITab tabButton = new UITab(panel, tabbedNode, i, panelId);
                    this.add(tabButton);
                }
            }
        }

        @Override
        public void render(UIContext context)
        {
            int x = this.area.x;
            for (IUIElement child : this.getChildren())
            {
                if (child instanceof UITab)
                {
                    UITab tab = (UITab) child;
                    tab.area.x = x;
                    tab.area.y = this.area.y;
                    tab.area.h = this.area.h;
                    
                    IKey nameKey = IKey.raw(tab.panelId);
                    switch (tab.panelId) {
                        case "cameraEditor": nameKey = UIKeys.FILM_OPEN_CAMERA_EDITOR; break;
                        case "replayEditor": nameKey = UIKeys.FILM_OPEN_REPLAY_EDITOR; break;
                        case "actionEditor": nameKey = UIKeys.FILM_OPEN_ACTION_EDITOR; break;
                        case "screenEditor": nameKey = UIKeys.FILM_OPEN_SCREEN_EDITOR; break;
                        case "editArea": nameKey = IKey.raw("Properties"); break;
                        case "preview": nameKey = IKey.raw("Preview"); break;
                        case "main": nameKey = IKey.raw("Main"); break;
                    }
                    int w = 20 + context.batcher.getFont().getWidth(nameKey.get()) + 8;
                    tab.area.w = w;
                    x += w;
                }
            }
            super.render(context);
        }
    }

    private static class UITab extends UIElement
    {
        private UIFilmPanel panel;
        private EditorLayoutNode.TabbedNode tabbedNode;
        private int index;
        private String panelId;
        private boolean mouseHeld;
        private int clickX, clickY;
        private static final int DRAG_THRESHOLD = 5;

        public UITab(UIFilmPanel panel, EditorLayoutNode.TabbedNode tabbedNode, int index, String panelId)
        {
            this.panel = panel;
            this.tabbedNode = tabbedNode;
            this.index = index;
            this.panelId = panelId;
        }

        @Override
        public void render(UIContext context)
        {
            /* Detect drag initiation: if mouse is held and moved far enough, start panel drag */
            if (this.panel.mouseHeldPanelId != null && this.panel.mouseHeldPanelId.equals(this.panelId) && this.panel.draggingPanelId == null)
            {
                int dx = context.mouseX - this.panel.clickX;
                int dy = context.mouseY - this.panel.clickY;
                if (dx * dx + dy * dy > DRAG_THRESHOLD * DRAG_THRESHOLD)
                {
                    this.panel.mouseHeldPanelId = null;
                    this.panel.startPanelDrag(this.panelId);
                    this.panel.updateDropTargetFromMouse(context.mouseX, context.mouseY);
                }
            }

            if (this.panel.draggingPanelId != null && this.panel.draggingPanelId.equals(this.panelId))
            {
                this.panel.updateDropTargetFromMouse(context.mouseX, context.mouseY);
            }
            
            boolean active = this.tabbedNode.activeTab == this.index;
            if (active)
            {
                int primary = BBSSettings.primaryColor.get();
                context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.y + 2, Colors.A100 | primary);
                context.batcher.gradientVBox(this.area.x, this.area.y + 2, this.area.ex(), this.area.ey(), Colors.A75 | primary, primary);
            }
            else
            {
                int bg = Colors.setA(0, 0.6F);
                if (this.area.isInside(context))
                {
                    bg = Colors.mulRGB(bg, 1.2F);
                }
                context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), bg);
            }
            
            // Render icon and name based on panelId
            Icon icon = Icons.FILM;
            IKey name = IKey.raw(this.panelId);
            if (this.panelId.equals("cameraEditor")) { icon = Icons.FRUSTUM; name = UIKeys.FILM_OPEN_CAMERA_EDITOR; }
            else if (this.panelId.equals("replayEditor")) { icon = Icons.SCENE; name = UIKeys.FILM_OPEN_REPLAY_EDITOR; }
            else if (this.panelId.equals("actionEditor")) { icon = Icons.ACTION; name = UIKeys.FILM_OPEN_ACTION_EDITOR; }
            else if (this.panelId.equals("screenEditor")) { icon = Icons.FILTER; name = UIKeys.FILM_OPEN_SCREEN_EDITOR; }
            else if (this.panelId.equals("editArea")) { icon = Icons.EDIT; name = IKey.raw("Properties"); }
            else if (this.panelId.equals("preview")) { icon = Icons.SPHERE; name = IKey.raw("Preview"); }
            else if (this.panelId.equals("main")) { icon = Icons.GEAR; name = IKey.raw("Main"); }

            context.batcher.icon(icon, Colors.WHITE, this.area.x + 2, this.area.y + 2);
            context.batcher.textShadow(name.get(), this.area.x + 20, this.area.y + 6);
            
            this.area.w = 20 + context.batcher.getFont().getWidth(name.get()) + 8;
            
            super.render(context);
        }

        @Override
        protected boolean subMouseClicked(UIContext context)
        {
            if (super.subMouseClicked(context)) return true;
            
            if (this.area.isInside(context) && context.mouseButton == 0)
            {
                this.tabbedNode.activeTab = this.index;
                ValueEditorLayout layout = BBSSettings.editorLayoutSettings;
                layout.setFilmLayoutRoot(layout.getFilmLayoutRoot()); // trigger save
                this.panel.setupEditorFlex(true, false, true);

                /* Track click position for drag detection */
                if (!layout.isLayoutLocked())
                {
                    this.panel.mouseHeldPanelId = this.panelId;
                    this.panel.clickX = context.mouseX;
                    this.panel.clickY = context.mouseY;
                }
                return true;
            }
            return false;
        }
        
        @Override
        protected boolean subMouseReleased(UIContext context)
        {
            if (this.panel.mouseHeldPanelId != null && this.panel.mouseHeldPanelId.equals(this.panelId))
            {
                this.panel.mouseHeldPanelId = null;
            }

            if (super.subMouseReleased(context)) return true;
            
            if (this.panel.draggingPanelId != null && this.panel.draggingPanelId.equals(this.panelId))
            {
                DropIntent intent = new DropIntent(this.panel.dropTargetPanelId, this.panel.dropTargetZone);
                if (!this.panel.canApplyDropIntent(this.panel.draggingPanelId, intent))
                {
                    this.panel.clearPanelDragState();
                    return true;
                }

                this.panel.applyPanelDropResult(this.panel.draggingPanelId, intent.targetId, intent.zone);
                this.panel.clearPanelDragState();
                return true;
            }
            return false;
        }
    }
}
