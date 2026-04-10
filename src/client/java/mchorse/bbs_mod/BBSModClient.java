package mchorse.bbs_mod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.serialization.MapCodec;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.audio.SoundManager;
import mchorse.bbs_mod.addons.AddonInfo;
import mchorse.bbs_mod.blocks.entities.ModelProperties;
import mchorse.bbs_mod.camera.clips.ClipFactoryData;
import mchorse.bbs_mod.camera.clips.misc.AudioClientClip;
import mchorse.bbs_mod.camera.clips.misc.CurveClientClip;
import mchorse.bbs_mod.camera.clips.misc.TrackerClientClip;
import mchorse.bbs_mod.camera.controller.CameraController;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.client.renderer.ModelBlockEntityRenderer;
import mchorse.bbs_mod.client.renderer.TriggerBlockEntityRenderer;
import mchorse.bbs_mod.client.renderer.entity.ActorEntityRenderer;
import mchorse.bbs_mod.client.renderer.entity.GunProjectileEntityRenderer;
import mchorse.bbs_mod.client.renderer.item.GunItemRenderer;
import mchorse.bbs_mod.client.renderer.item.ModelBlockItemRenderer;
import mchorse.bbs_mod.cubic.model.ModelManager;
import mchorse.bbs_mod.events.BBSAddonMod;
import mchorse.bbs_mod.events.register.RegisterClientSettingsEvent;
import mchorse.bbs_mod.events.register.RegisterDashboardPanelsEvent;
import mchorse.bbs_mod.events.register.RegisterFormCategoriesEvent;
import mchorse.bbs_mod.events.register.RegisterImportersEvent;
import mchorse.bbs_mod.events.register.RegisterInterpolationsEvent;
import mchorse.bbs_mod.events.register.RegisterIconsEvent;
import mchorse.bbs_mod.events.register.RegisterUIKeyframeFactoriesEvent;
import mchorse.bbs_mod.events.register.RegisterKeyframeShapesEvent;
import mchorse.bbs_mod.events.register.RegisterFormsRenderersEvent;
import mchorse.bbs_mod.events.register.RegisterUIValueFactoriesEvent;
import mchorse.bbs_mod.events.register.RegisterFormEditorsEvent;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.settings.ui.UIValueMap;
import mchorse.bbs_mod.ui.forms.editors.UIFormEditor;
import mchorse.bbs_mod.events.register.RegisterL10nEvent;
import mchorse.bbs_mod.events.register.RegisterModelLoadersEvent;
import mchorse.bbs_mod.events.register.RegisterParticleComponentsEvent;
import mchorse.bbs_mod.events.register.RegisterPropTransformEvent;
import mchorse.bbs_mod.events.register.RegisterStencilMapEvent;
import mchorse.bbs_mod.events.register.RegisterRayTracingEvent;
import mchorse.bbs_mod.events.register.RegisterFilmPreviewEvent;
import mchorse.bbs_mod.events.register.RegisterReplayListContextMenuEvent;
import mchorse.bbs_mod.events.register.RegisterReplayPanelEvent;
import mchorse.bbs_mod.events.register.RegisterShadersEvent;
import mchorse.bbs_mod.events.register.RegisterSourcePacksEvent;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.Films;
import mchorse.bbs_mod.film.Recorder;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormCategories;
import mchorse.bbs_mod.forms.categories.UserFormCategory;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.graphics.Draw;
import mchorse.bbs_mod.graphics.FramebufferManager;
import mchorse.bbs_mod.graphics.texture.TextureManager;
import mchorse.bbs_mod.items.GunProperties;
import mchorse.bbs_mod.items.GunZoom;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.network.ClientNetwork;
import mchorse.bbs_mod.network.ServerNetwork;
import mchorse.bbs_mod.particles.ParticleManager;
import mchorse.bbs_mod.particles.ParticleScheme;
import mchorse.bbs_mod.utils.interps.Interpolations;
import mchorse.bbs_mod.resources.AssetProvider;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.resources.packs.URLError;
import mchorse.bbs_mod.resources.packs.URLRepository;
import mchorse.bbs_mod.resources.packs.URLSourcePack;
import mchorse.bbs_mod.resources.packs.URLTextureErrorCallback;
import mchorse.bbs_mod.selectors.EntitySelectors;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.replays.overlays.UIQuickReplayOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.shapes.KeyframeShapeRenderers;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.model_blocks.UIModelBlockEditorMenu;
import mchorse.bbs_mod.ui.morphing.UIMorphingPanel;
import mchorse.bbs_mod.ui.utils.cml.CMLSettings;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.keys.KeyCombo;
import mchorse.bbs_mod.ui.utils.keys.KeybindSettings;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.ScreenshotRecorder;
import mchorse.bbs_mod.utils.VideoRecorder;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.resources.MinecraftSourcePack;
import mchorse.bbs_mod.blocks.entities.TriggerBlockEntity;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ContactInformation;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4fStack;
import com.mojang.blaze3d.platform.Window;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.joml.Matrix4f;


import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BBSModClient implements ClientModInitializer
{
    public static final List<AddonInfo> registeredAddons = new ArrayList<>();

    public static void registerAddon(AddonInfo info)
    {
        registeredAddons.add(info);
    }
    private static TextureManager textures;
    private static FramebufferManager framebuffers;
    private static SoundManager sounds;
    private static L10n l10n;

    private static ModelManager models;
    private static FormCategories formCategories;
    private static ScreenshotRecorder screenshotRecorder;
    private static VideoRecorder videoRecorder;
    private static EntitySelectors selectors;
    private static final KeyMapping.Category MAIN_KEY_CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "main"));

    private static ParticleManager particles;

    private static KeyMapping keyDashboard;
    private static KeyMapping keyItemEditor;
    private static KeyMapping keyPlayFilm;
    private static KeyMapping keyPauseFilm;
    private static KeyMapping keyRecordReplay;
    private static KeyMapping keyRecordVideo;
    private static KeyMapping keyOpenReplays;
    private static KeyMapping keyOpenQuickReplays;
    private static KeyMapping keyOpenMorphing;
    private static KeyMapping keyDemorph;
    private static KeyMapping keyTeleport;
    private static KeyMapping keyZoom;
    private static KeyMapping keyToggleReplayHud;

    private static UIDashboard dashboard;

    private static CameraController cameraController = new CameraController();
    private static ModelBlockItemRenderer modelBlockItemRenderer = new ModelBlockItemRenderer();
    private static GunItemRenderer gunItemRenderer = new GunItemRenderer();
    private static Films films;
    private static GunZoom gunZoom;

    private static Replay selectedReplay;

    private static float originalFramebufferScale;

    public static TextureManager getTextures()
    {
        return textures;
    }

    public static FramebufferManager getFramebuffers()
    {
        return framebuffers;
    }

    public static SoundManager getSounds()
    {
        return sounds;
    }

    public static L10n getL10n()
    {
        return l10n;
    }

    public static ModelManager getModels()
    {
        return models;
    }

    public static FormCategories getFormCategories()
    {
        return formCategories;
    }

    public static ScreenshotRecorder getScreenshotRecorder()
    {
        return screenshotRecorder;
    }

    public static VideoRecorder getVideoRecorder()
    {
        return videoRecorder;
    }

    public static EntitySelectors getSelectors()
    {
        return selectors;
    }

    public static ParticleManager getParticles()
    {
        return particles;
    }

    public static CameraController getCameraController()
    {
        return cameraController;
    }

    public static Films getFilms()
    {
        return films;
    }

     public static void setSelectedReplay(Replay replay)
    {
        selectedReplay = replay;
    }

    public static Replay getSelectedReplay()
    {
        return selectedReplay;
    }


    public static GunZoom getGunZoom()
    {
        return gunZoom;
    }

    public static GunItemRenderer getGunItemRenderer()
    {
        return gunItemRenderer;
    }

    public static ModelBlockItemRenderer getModelBlockItemRenderer()
    {
        return modelBlockItemRenderer;
    }

    public static KeyMapping getKeyZoom()
    {
        return keyZoom;
    }

    public static KeyMapping getKeyRecordVideo()
    {
        return keyRecordVideo;
    }

    public static KeyMapping getKeyOpenQuickReplays()
    {
        return keyOpenQuickReplays;
    }

    public static UIDashboard getDashboard()
    {
        if (dashboard == null)
        {
            dashboard = new UIDashboard();
        }

        return dashboard;
    }

    public static int getGUIScale()
    {
        int scale = BBSSettings.userIntefaceScale.get();

        if (scale == 0)
        {
            return Minecraft.getInstance().options.guiScale().get();
        }

        return scale;
    }

    public static float getOriginalFramebufferScale()
    {
        return Math.max(originalFramebufferScale, 1);
    }

    public static ModelProperties getItemStackProperties(ItemStack stack)
    {
        ModelBlockItemRenderer.Item item = modelBlockItemRenderer.get(stack);

        if (item != null)
        {
            return item.entity.getProperties();
        }

        GunItemRenderer.Item gunItem = gunItemRenderer.get(stack);

        if (gunItem != null)
        {
            return gunItem.properties;
        }

        return null;
    }

    public static void onEndKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo info)
    {
        if (action != GLFW.GLFW_PRESS)
        {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;

        if (player == null || Minecraft.getInstance().screen != null)
        {
            return;
        }

        Morph morph = Morph.getMorph(player);

        /* Animation state trigger */
        if (morph != null && morph.getForm() != null && morph.getForm().findState(key, (form, state) ->
        {
            ClientNetwork.sendFormTrigger(state.id.get(), ServerNetwork.STATE_TRIGGER_MORPH);
            form.playState(state);
        }))
            return;

        /* Animation state trigger for items*/
        ModelProperties main = getItemStackProperties(player.getItemInHand(InteractionHand.MAIN_HAND));
        ModelProperties offhand = getItemStackProperties(player.getItemInHand(InteractionHand.OFF_HAND));

        if (main != null && main.getForm() != null && main.getForm().findState(key, (form, state) ->
        {
            ClientNetwork.sendFormTrigger(state.id.get(), ServerNetwork.STATE_TRIGGER_MAIN_HAND_ITEM);
            form.playState(state);
        }))
            return;

        if (offhand != null && offhand.getForm() != null && offhand.getForm().findState(key, (form, state) ->
        {
            ClientNetwork.sendFormTrigger(state.id.get(), ServerNetwork.STATE_TRIGGER_OFF_HAND_ITEM);
            form.playState(state);
        }))
            return;

        /* Change form based on the hotkey */
        for (Form form : BBSModClient.getFormCategories().getRecentForms().getCategories().get(0).getForms())
        {
            if (form.hotkey.get() == key)
            {
                ClientNetwork.sendPlayerForm(form);

                return;
            }
        }

        for (UserFormCategory category : BBSModClient.getFormCategories().getUserForms().categories)
        {
            for (Form form : category.getForms())
            {
                if (form.hotkey.get() == key)
                {
                    ClientNetwork.sendPlayerForm(form);

                    return;
                }
            }
        }
    }

    @Override
    public void onInitializeClient()
    {
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) ->
        {
            if (world.getBlockEntity(pos) instanceof TriggerBlockEntity)
            {
                if (player.isCreative())
                {
                    return InteractionResult.PASS;
                }

                ClientNetwork.sendTriggerBlockClick(pos);

                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        });

        FabricLoader.getInstance()
            .getEntrypointContainers("bbs-addon-client", BBSAddonMod.class)
            .forEach((container) ->
            {
                BBSMod.events.register(container.getEntrypoint());
            });

        AssetProvider provider = BBSMod.getProvider();

        textures = new TextureManager(provider);
        framebuffers = new FramebufferManager();
        sounds = new SoundManager(provider);
        l10n = new L10n();
        l10n.register((lang) -> Collections.singletonList(Link.assets("strings/" + lang + ".json")));
        l10n.reload();

        BBSMod.events.post(new RegisterL10nEvent(l10n));

        File parentFile = BBSMod.getSettingsFolder().getParentFile();

        particles = new ParticleManager(() -> new File(BBSMod.getAssetsFolder(), "particles"));

        models = new ModelManager(provider);
        BBSMod.events.post(new RegisterModelLoadersEvent(models));
        formCategories = new FormCategories();
        BBSMod.events.post(new RegisterFormCategoriesEvent(formCategories));
        BBSMod.events.post(new RegisterImportersEvent());
        BBSMod.events.post(new RegisterParticleComponentsEvent(ParticleScheme.PARSER.components));
        BBSMod.events.post(new RegisterInterpolationsEvent(Interpolations.MAP));
        BBSMod.events.post(new RegisterFormsRenderersEvent());
        BBSMod.events.post(new RegisterFormEditorsEvent(UIFormEditor.panels));
        BBSMod.events.post(new RegisterIconsEvent());
        BBSMod.events.post(new RegisterUIValueFactoriesEvent(UIValueMap.factories));
        BBSMod.events.post(new RegisterUIKeyframeFactoriesEvent(UIKeyframeFactory.FACTORIES));
        BBSMod.events.post(new RegisterKeyframeShapesEvent(KeyframeShapeRenderers.SHAPES));
        BBSMod.events.post(new RegisterPropTransformEvent());
        BBSMod.events.post(new RegisterStencilMapEvent());
        BBSMod.events.post(new RegisterRayTracingEvent());
        BBSMod.events.post(new RegisterFilmPreviewEvent());
        BBSMod.events.post(new RegisterReplayListContextMenuEvent());
        BBSMod.events.post(new RegisterReplayPanelEvent());
        screenshotRecorder = new ScreenshotRecorder(new File(parentFile, "screenshots"));
        videoRecorder = new VideoRecorder();
        selectors = new EntitySelectors();
        selectors.read();
        films = new Films();

        BBSResources.init();

        URLRepository repository = new URLRepository(new File(parentFile, "url_cache"));

        provider.register(new URLSourcePack("http", repository));
        provider.register(new URLSourcePack("https", repository));

        KeybindSettings.registerClasses();

        BBSMod.setupConfig(Icons.KEY_CAP, "keybinds", new File(BBSMod.getSettingsFolder(), "keybinds.json"), KeybindSettings::register);
        BBSMod.setupConfig(Icons.SETTINGS, "cml", new File(BBSMod.getSettingsFolder(), "cml.json"), CMLSettings::register);

        BBSMod.events.post(new RegisterClientSettingsEvent());

        BBSSettings.language.postCallback((v, f) -> reloadLanguage(getLanguageKey()));

        BBSSettings.editorTimeMode.postCallback((v, f) ->
        {
            if (dashboard != null && dashboard.getPanels().panel instanceof UIFilmPanel panel)
            {
                panel.fillData();
            }
        });

        BBSSettings.tooltipStyle.modes(
            UIKeys.ENGINE_TOOLTIP_STYLE_LIGHT,
            UIKeys.ENGINE_TOOLTIP_STYLE_DARK
        );

        BBSSettings.keystrokeMode.modes(
            UIKeys.ENGINE_KEYSTROKES_POSITION_AUTO,
            UIKeys.ENGINE_KEYSTROKES_POSITION_BOTTOM_LEFT,
            UIKeys.ENGINE_KEYSTROKES_POSITION_BOTTOM_RIGHT,
            UIKeys.ENGINE_KEYSTROKES_POSITION_TOP_RIGHT,
            UIKeys.ENGINE_KEYSTROKES_POSITION_TOP_LEFT
        );

        UIKeys.C_KEYBIND_CATGORIES.load(KeyCombo.getCategoryKeys());
        UIKeys.C_KEYBIND_CATGORIES_TOOLTIP.load(KeyCombo.getCategoryKeys());

        /* Replace audio clip with client version that plays audio */
        BBSMod.getFactoryCameraClips()
            .register(Link.bbs("audio"), AudioClientClip.class, new ClipFactoryData(Icons.SOUND, 0xffc825))
            .register(Link.bbs("tracker"), TrackerClientClip.class, new ClipFactoryData(Icons.USER, 0x4cedfc))
            .register(Link.bbs("curve"), CurveClientClip.class, new ClipFactoryData(Icons.ARC, 0xff1493));

        /* Keybinds */
        keyDashboard = this.createKey("dashboard", GLFW.GLFW_KEY_0);
        keyItemEditor = this.createKey("item_editor", GLFW.GLFW_KEY_HOME);
        keyPlayFilm = this.createKey("play_film", GLFW.GLFW_KEY_RIGHT_CONTROL);
        keyPauseFilm = this.createKey("pause_film", GLFW.GLFW_KEY_BACKSLASH);
        keyRecordReplay = this.createKey("record_replay", GLFW.GLFW_KEY_RIGHT_ALT);
        keyRecordVideo = this.createKey("record_video", GLFW.GLFW_KEY_F4);
        keyOpenReplays = this.createKey("open_replays", GLFW.GLFW_KEY_RIGHT_SHIFT);
        keyOpenQuickReplays = this.createKey("open_quick_replays", GLFW.GLFW_KEY_RIGHT_BRACKET);
        keyOpenMorphing = this.createKey("open_morphing", GLFW.GLFW_KEY_B);
        keyDemorph = this.createKey("demorph", GLFW.GLFW_KEY_PERIOD);
        keyTeleport = this.createKey("teleport", GLFW.GLFW_KEY_Y);
        keyZoom = this.createKeyMouse("zoom", 2);
        keyToggleReplayHud = this.createKey("toggle_replay_hud", GLFW.GLFW_KEY_P);

        LevelRenderEvents.AFTER_SOLID_FEATURES.register((context) ->
        {
            BBSRendering.renderCoolStuff(context);

            if (BBSSettings.chromaSkyEnabled.get())
            {
                float d = BBSSettings.chromaSkyBillboard.get();

                if (d > 0)
                {
                    // Disabled until legacy immediate-mode path is fully ported to 26.1 render APIs.
                }
            }
        });

        // LAST was removed from newer world render events; frame capture is handled elsewhere.

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
        {
            dashboard = null;
            films = new Films();
            setSelectedReplay(null);

            ClientNetwork.resetHandshake();
            films.reset();
            cameraController.reset();
        });

        ClientTickEvents.START_CLIENT_TICK.register((client) ->
        {
            BBSRendering.startTick();
            TriggerBlockEntityRenderer.capturedTriggerBlocks.clear();
        });

        ClientTickEvents.END_LEVEL_TICK.register((world) ->
        {
            Minecraft mc = Minecraft.getInstance();

            if (!mc.isPaused())
            {
                films.updateEndWorld();
            }

            BBSResources.tick();
        });

        ClientTickEvents.END_CLIENT_TICK.register((client) ->
        {
            Minecraft mc = Minecraft.getInstance();

            if (mc.screen instanceof UIScreen screen)
            {
                screen.update();
            }

            cameraController.update();

            if (!mc.isPaused())
            {
                films.update();
                modelBlockItemRenderer.update();
                gunItemRenderer.update();
                textures.update();
            }

            while (keyDashboard.consumeClick()) UIScreen.open(getDashboard());
            while (keyItemEditor.consumeClick()) this.keyOpenModelBlockEditor(mc);
            while (keyPlayFilm.consumeClick()) this.keyPlayFilm();
            while (keyPauseFilm.consumeClick()) this.keyPauseFilm();
            while (keyRecordReplay.consumeClick()) this.keyRecordReplay();
            while (keyRecordVideo.consumeClick())
            {
                Window window = mc.getWindow();
                int width = Math.max(window.getWidth(), 2);
                int height = Math.max(window.getHeight(), 2);

                if (width % 2 == 1) width -= width % 2;
                if (height % 2 == 1) height -= height % 2;

                videoRecorder.toggleRecording(BBSRendering.getTexture().id, width, height);
                BBSRendering.setCustomSize(videoRecorder.isRecording(), width, height);
            }
            while (keyOpenReplays.consumeClick()) this.keyOpenReplays();
            while (keyOpenQuickReplays.consumeClick())
            {
                if (!UIQuickReplayOverlayPanel.isOpened())
                {
                    this.keyOpenQuickReplays();
                }
            }
            while (keyOpenMorphing.consumeClick())
            {
                UIDashboard dashboard = getDashboard();

                UIScreen.open(dashboard);
                dashboard.setPanel(dashboard.getPanel(UIMorphingPanel.class));
            }
            while (keyDemorph.consumeClick()) ClientNetwork.sendPlayerForm(null);
            while (keyTeleport.consumeClick()) this.keyTeleport();
            while (keyToggleReplayHud.consumeClick()) BBSSettings.editorReplayHud.set(!BBSSettings.editorReplayHud.get());

            if (mc.player != null)
            {
                boolean zoom = keyZoom.isDown();
                ItemStack stack = mc.player.getMainHandItem();

                if (gunZoom == null && zoom && stack.getItem() == BBSMod.GUN_ITEM)
                {
                    GunProperties properties = GunProperties.get(stack);

                    ClientNetwork.sendZoom(true);
                    gunZoom = new GunZoom(properties.fovTarget, properties.fovInterp, properties.fovDuration);
                }
            }
        });

        // HUD callback API changed in this target version; HUD rendering is driven by GUI/mixin hooks.

        ClientLifecycleEvents.CLIENT_STOPPING.register((e) -> BBSResources.stopWatchdog());
        ClientLifecycleEvents.CLIENT_STARTED.register((e) ->
        {
            BBSRendering.setupFramebuffer();
            provider.register(new MinecraftSourcePack());

            Window window = Minecraft.getInstance().getWindow();

            originalFramebufferScale = 1;
        });

        URLTextureErrorCallback.EVENT.register((url, error) ->
        {
            UIBaseMenu menu = UIScreen.getCurrentMenu();

            if (menu != null)
            {
                url = url.substring(0, MathUtils.clamp(url.length(), 0, 40));

                if (error == URLError.FFMPEG)
                {
                    menu.context.notifyError(UIKeys.TEXTURE_URL_ERROR_FFMPEG.format(url));
                }
                else if (error == URLError.HTTP_ERROR)
                {
                    menu.context.notifyError(UIKeys.TEXTURE_URL_ERROR_HTTP.format(url));
                }
            }
        });

        BBSRendering.setup();

        /* Network */
        ClientNetwork.setup();

        /* Register addons from FabricLoader */
        FabricLoader.getInstance()
            .getEntrypointContainers("bbs-addon", BBSAddonMod.class)
            .forEach((container) ->
            {
                net.fabricmc.loader.api.metadata.ModMetadata meta = container.getProvider().getMetadata();
                String id = meta.getId();
                String name = meta.getName();
                String version = meta.getVersion().getFriendlyString();
                String description = meta.getDescription();
                List<String> authors = meta.getAuthors().stream().map(Person::getName).toList();
                
                Link icon = null;
                Optional<String> iconPath = meta.getIconPath(64);
                if (iconPath.isPresent())
                {
                    String path = iconPath.get();
                    if (path.startsWith("assets/"))
                    {
                        String relative = path.substring("assets/".length());
                        icon = new Link("mod_icons", relative);
                    }
                }
                
                ContactInformation contact = meta.getContact();
                String website = contact.get("homepage").orElse("");
                String issues = contact.get("issues").orElse("");
                String source = contact.get("sources").orElse("");

                registerAddon(new AddonInfo(id, name, version, description, authors, icon, website, issues, source));
            });

        /* Entity renderers */
        EntityRendererRegistry.register(BBSMod.ACTOR_ENTITY, ActorEntityRenderer::new);
        EntityRendererRegistry.register(BBSMod.GUN_PROJECTILE_ENTITY, GunProjectileEntityRenderer::new);

        /* Block entity renderers */

        SpecialModelRenderers.ID_MAPPER.put(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "gun"), GunItemRenderer.Unbaked.CODEC);
        SpecialModelRenderers.ID_MAPPER.put(Identifier.fromNamespaceAndPath(BBSMod.MOD_ID, "model_block"), ModelBlockItemRenderer.Unbaked.CODEC);

        /* Create folders */
        BBSMod.getAudioFolder().mkdirs();
        BBSMod.getAssetsPath("textures").mkdirs();

        for (String path : List.of("alex", "alex_simple", "steve", "steve_simple"))
        {
            BBSMod.getAssetsPath("models/emoticons/" + path + "/").mkdirs();
        }

        for (String path : List.of("alex", "alex_bends", "eyes", "eyes_1px", "steve", "steve_bends"))
        {
            BBSMod.getAssetsPath("models/player/" + path + "/").mkdirs();
        }
    }

    private KeyMapping createKey(String id, int key)
    {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key." + BBSMod.MOD_ID + "." + id,
            InputConstants.Type.KEYSYM,
            key,
            MAIN_KEY_CATEGORY
        ));
    }

    private KeyMapping createKeyMouse(String id, int button)
    {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key." + BBSMod.MOD_ID + "." + id,
            InputConstants.Type.MOUSE,
            button,
            MAIN_KEY_CATEGORY
        ));
    }

    private void keyOpenModelBlockEditor(Minecraft mc)
    {
        ItemStack stack = mc.player.getItemBySlot(EquipmentSlot.MAINHAND);
        ModelBlockItemRenderer.Item item = modelBlockItemRenderer.get(stack);
        GunItemRenderer.Item gunItem = gunItemRenderer.get(stack);

        if (item != null)
        {
            UIScreen.open(new UIModelBlockEditorMenu(item.entity.getProperties()));
        }
        else if (gunItem != null)
        {
            UIScreen.open(new UIModelBlockEditorMenu(gunItem.properties));
        }
    }

    private void keyPlayFilm()
    {
        UIFilmPanel panel = getDashboard().getPanel(UIFilmPanel.class);

        if (panel.getData() != null)
        {
            Films.playFilm(panel.getData().getId(), false);
        }
    }

    private void keyPauseFilm()
    {
        UIFilmPanel panel = getDashboard().getPanel(UIFilmPanel.class);

        if (panel.getData() != null)
        {
            Films.pauseFilm(panel.getData().getId());
        }
    }

    private void keyRecordReplay()
    {
        UIDashboard dashboard = getDashboard();
        UIFilmPanel panel = dashboard.getPanel(UIFilmPanel.class);

        if (panel != null && panel.getData() != null)
        {
            Recorder recorder = getFilms().getRecorder();

            if (recorder != null)
            {
                recorder = BBSModClient.getFilms().stopRecording();

                if (recorder == null || recorder.hasNotStarted() || panel.getData() == null)
                {
                    return;
                }

                panel.applyRecordedKeyframes(recorder, panel.getData());
            }
            else
            {
                Replay replay = panel.replayEditor.getReplay();
                int index = panel.getData().replays.getList().indexOf(replay);

                if (index >= 0)
                {
                    getFilms().startRecording(panel.getData(), index, 0);
                }
            }
        }
    }

    private void keyOpenReplays()
    {
        UIDashboard dashboard = getDashboard();

        UIScreen.open(dashboard);

        if (dashboard.getPanels().panel instanceof UIFilmPanel panel && panel.getData() != null)
        {
            panel.preview.openReplays();
        }
        else
        {
            dashboard.setPanel(dashboard.getPanel(UIFilmPanel.class));
        }
    }

    private void keyOpenQuickReplays()
    {
        UIDashboard dashboard = getDashboard();

        Film quickReplayFilm = this.getQuickReplayFilm(dashboard);

        if (quickReplayFilm != null && !quickReplayFilm.replays.getList().isEmpty())
        {
            UIQuickReplayOverlayPanel.open(
                new UIQuickReplayOverlayPanel(
                    quickReplayFilm.replays.getList(),
                    getSelectedReplay(),
                    this::setQuickReplaySelection
                )
            );

            return;
        }
    }

    private void setQuickReplaySelection(Replay replay)
    {
        setSelectedReplay(replay);

        UIDashboard dashboard = getDashboard();
        UIFilmPanel panel = dashboard.getPanel(UIFilmPanel.class);

        if (panel != null && panel.getData() != null && panel.getData().replays.getList().contains(replay))
        {
            panel.replayEditor.setReplay(replay);
        }
    }

    private Film getQuickReplayFilm(UIDashboard dashboard)
    {
        Replay selected = getSelectedReplay();
        UIFilmPanel panel = dashboard.getPanel(UIFilmPanel.class);
        Film film = panel == null ? null : panel.getData();

        if (this.isFilmUsableForQuickSelection(film, selected))
        {
            return film;
        }

        Recorder recorder = getFilms().getRecorder();

        if (recorder != null && this.isFilmUsableForQuickSelection(recorder.film, selected))
        {
            return recorder.film;
        }

        for (BaseFilmController controller : getFilms().getControllers())
        {
            if (this.isFilmUsableForQuickSelection(controller.film, selected))
            {
                return controller.film;
            }
        }

        return null;
    }

    private boolean isFilmUsableForQuickSelection(Film film, Replay selected)
    {
        if (film == null || film.replays.getList().isEmpty())
        {
            return false;
        }

        return selected == null || film.replays.getList().contains(selected);
    }

    private void keyTeleport()
    {
        UIDashboard dashboard = getDashboard();
        UIFilmPanel panel = dashboard.getPanel(UIFilmPanel.class);

        if (panel != null)
        {
            panel.replayEditor.teleport();
        }
    }

    public static String getLanguageKey()
    {
        return getLanguageKey(BBSSettings.language.get());
    }

    public static String getLanguageKey(String key)
    {
        if (key.isEmpty())
        {
            key = Minecraft.getInstance().options.languageCode;
        }

        return key;
    }

    public static void reloadLanguage(String language)
    {
        l10n.reload(language, BBSMod.getProvider());
    }
}
