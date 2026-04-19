package mchorse.bbs_mod.client;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.client.renderer.ModelBlockEntityRenderer;
import mchorse.bbs_mod.client.renderer.TriggerBlockEntityRenderer;
import mchorse.bbs_mod.camera.clips.misc.CurveClip;
import mchorse.bbs_mod.camera.clips.misc.SubtitleClip;
import mchorse.bbs_mod.camera.controller.CameraWorkCameraController;
import mchorse.bbs_mod.camera.controller.PlayCameraController;
import mchorse.bbs_mod.events.ModelBlockEntityUpdateCallback;
import mchorse.bbs_mod.events.TriggerBlockEntityUpdateCallback;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.utils.RecolorVertexConsumer;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.graphics.texture.TextureFormat;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.client.video.VideoRenderer;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.UISubtitleRenderer;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIRenderingContext;
import mchorse.bbs_mod.ui.framework.UIScreen;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.VideoRecorder;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.iris.IrisUtils;
import mchorse.bbs_mod.utils.iris.ShaderCurves;
import mchorse.bbs_mod.utils.sodium.SodiumUtils;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.loader.api.FabricLoader;
import net.irisshaders.iris.uniforms.custom.cached.CachedUniform;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class BBSRendering
{
    /**
     * Cached rendered model blocks
     */
    public static final Set<ModelBlockEntity> capturedModelBlocks = new HashSet<>();

    public static boolean canRender;

    public static boolean renderingWorld;
    public static int lastAction;

    public static final Matrix4f camera = new Matrix4f();

    private static boolean customSize;
    private static boolean iris;
    private static boolean sodium;
    private static boolean optifine;

    private static int width;
    private static int height;

    private static final UIBaseMenu replayHudMenu = new UIBaseMenu() {};

    private static boolean toggleFramebuffer;
    private static RenderTarget framebuffer;
    private static RenderTarget clientFramebuffer;
    private static Texture texture;
    private static CloudStatus cachedCloudRenderMode;
    private static boolean cloudsForced;

    public static int getMotionBlur()
    {
        return getMotionBlur(BBSSettings.videoSettings.frameRate.get(), getMotionBlurFactor());
    }

    public static int getMotionBlur(double fps, int target)
    {
        int i = 0;

        while (fps < target)
        {
            fps *= 2;

            i++;
        }

        return i;
    }

    public static int getMotionBlurFactor()
    {
        return getMotionBlurFactor(BBSSettings.videoSettings.motionBlur.get());
    }

    public static int getMotionBlurFactor(int integer)
    {
        return integer == 0 ? 0 : (int) Math.pow(2, 6 + integer);
    }

    public static int getVideoWidth()
    {
        return width == 0 ? BBSSettings.videoSettings.width.get() : width;
    }

    public static int getVideoHeight()
    {
        return height == 0 ? BBSSettings.videoSettings.height.get() : height;
    }

    public static int getVideoFrameRate()
    {
        int frameRate = BBSSettings.videoSettings.frameRate.get();

        return frameRate * (1 << getMotionBlur(frameRate, getMotionBlurFactor()));
    }

    public static File getVideoFolder()
    {
        File movies = new File(BBSMod.getSettingsFolder().getParentFile(), "movies");
        File exportPath = new File(BBSSettings.videoSettings.path.get());

        if (exportPath.isDirectory())
        {
            movies = exportPath;
        }

        movies.mkdirs();

        return movies;
    }

    public static boolean canReplaceFramebuffer()
    {
        return customSize && renderingWorld;
    }

    public static boolean isCustomSize()
    {
        return customSize;
    }

    public static boolean isFramebufferToggled()
    {
        return toggleFramebuffer;
    }

    public static void setCustomSize(boolean customSize)
    {
        setCustomSize(customSize, 0, 0);
    }

    public static void setCustomSize(boolean customSize, int w, int h)
    {
        BBSRendering.customSize = customSize;

        width = !customSize ? 0 : w;
        height = !customSize ? 0 : h;

        if (!customSize)
        {
            resizeExtraFramebuffers();
        }
    }

    public static Texture getTexture()
    {
        if (texture == null)
        {
            texture = new Texture();
            texture.setFormat(TextureFormat.RGB_U8);
            texture.setFilter(GL11.GL_NEAREST);
        }

        return texture;
    }

    public static void startTick()
    {
        capturedModelBlocks.clear();
        TriggerBlockEntityRenderer.capturedTriggerBlocks.clear();
    }

    public static void setup()
    {
        iris = FabricLoader.getInstance().isModLoaded("iris");
        sodium = FabricLoader.getInstance().isModLoaded("sodium");
        optifine = FabricLoader.getInstance().isModLoaded("optifabric");

        ModelBlockEntityUpdateCallback.EVENT.register((entity) ->
        {
            if (entity.getLevel() != null && entity.getLevel().isClientSide())
            {
                capturedModelBlocks.add(entity);
            }
        });

        TriggerBlockEntityUpdateCallback.EVENT.register((entity) ->
        {
            if (entity.getLevel() != null && entity.getLevel().isClientSide())
            {
                TriggerBlockEntityRenderer.capturedTriggerBlocks.add(entity);
            }
        });

        if (!iris)
        {
            return;
        }

        IrisUtils.setup();
    }

    /* Framebuffers */

    public static RenderTarget getFramebuffer()
    {
        return framebuffer;
    }

    public static void setupFramebuffer()
    {
        Window window = Minecraft.getInstance().getWindow();

        framebuffer = new MainTarget(window.getWidth(), window.getHeight());
    }

    public static void resizeExtraFramebuffers()
    {
        Set<RenderTarget> buffers = new HashSet<>();
        Minecraft mc = Minecraft.getInstance();

        buffers.add(mc.levelRenderer.entityOutlineTarget());
        buffers.add(mc.levelRenderer.getTranslucentTarget());
        buffers.add(mc.levelRenderer.getItemEntityTarget());
        buffers.add(mc.levelRenderer.getParticlesTarget());
        buffers.add(mc.levelRenderer.getWeatherTarget());
        buffers.add(mc.levelRenderer.getCloudsTarget());

        for (RenderTarget buffer : buffers)
        {
            resizeFramebuffer(buffer);
        }
    }

    public static void resizeFramebuffer(RenderTarget framebuffer)
    {
        if (framebuffer == null)
        {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();

        if (framebuffer.width == w && framebuffer.height == h)
        {
            return;
        }

        framebuffer.resize(w, h);
    }

    public static void toggleFramebuffer(boolean toggleFramebuffer)
    {
        if (toggleFramebuffer == BBSRendering.toggleFramebuffer)
        {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Window window = mc.getWindow();

        BBSRendering.toggleFramebuffer = toggleFramebuffer;

        if (toggleFramebuffer)
        {
            int w = mc.getWindow().getWidth();
            int h = mc.getWindow().getHeight();

            resizeExtraFramebuffers();

            if (framebuffer.width != w || framebuffer.height != h)
            {
                framebuffer.resize(w, h);
            }

            clientFramebuffer = mc.getMainRenderTarget();

            reassignFramebuffer(framebuffer);

            // Framebuffer is already assigned above for this render branch.
        }
        else
        {
            reassignFramebuffer(clientFramebuffer);

            // Client framebuffer was restored above.

            if (width != 0)
            {
                // Framebuffer draw API changed in 1.21.11; skip legacy blit here.
            }
        }
    }

    private static void reassignFramebuffer(RenderTarget framebuffer)
    {
        Minecraft.getInstance().mainRenderTarget = framebuffer;
    }

    /* Rendering */

    public static void onWorldRenderBegin()
    {
        Minecraft mc = Minecraft.getInstance();
        BBSModClient.getFilms().startRenderFrame(mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));

        UIBaseMenu menu = UIScreen.getCurrentMenu();

        if (menu != null)
        {
            menu.startRenderFrame(mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        }

        renderingWorld = true;
        updateCloudRenderMode(mc);

        if (!customSize)
        {
            return;
        }

        toggleFramebuffer(true);
    }

    public static void onWorldRenderEnd()
    {
        Minecraft mc = Minecraft.getInstance();

        if (BBSModClient.getCameraController().getCurrent() instanceof PlayCameraController controller)
        {
            GuiGraphicsExtractor drawContext = new GuiGraphicsExtractor(mc, new GuiRenderState(), 0, 0);
            Batcher2D batcher = new Batcher2D(drawContext);

            UISubtitleRenderer.renderSubtitles(new PoseStack(), batcher, SubtitleClip.getSubtitles(controller.getContext()));

            Window window = mc.getWindow();
            Area area = new Area(0, 0, window.getGuiScaledWidth(), window.getGuiScaledHeight());
            Matrix4f cache = new Matrix4f(RenderSystem.getModelViewMatrix());
            Matrix4f ortho = new Matrix4f().ortho(0, area.w, area.h, 0, -1000, 3000);

            /* projection matrix state managed by 1.21.11 renderer */
            VideoRenderer.renderClips(new PoseStack(), batcher, controller.getContext().clips.getClips(controller.getContext().relativeTick), controller.getContext().relativeTick, true, area, area, null, area.w, area.h, false);
            /* projection matrix state managed by 1.21.11 renderer */
        }

        if (!customSize)
        {
            renderingWorld = false;

            return;
        }

        UIBaseMenu currentMenu = UIScreen.getCurrentMenu();

        if (currentMenu instanceof UIDashboard dashboard)
        {
            if (dashboard.getPanels().panel instanceof UIFilmPanel panel && panel.getData() != null)
            {
                UISubtitleRenderer.renderSubtitles(new PoseStack(), currentMenu.context.batcher, SubtitleClip.getSubtitles(panel.getRunner().getContext()));

                Window window = mc.getWindow();
                Matrix4f cache = new Matrix4f(RenderSystem.getModelViewMatrix());
                Matrix4f ortho = new Matrix4f().ortho(0, window.getGuiScaledWidth(), window.getGuiScaledHeight(), 0, -1000, 3000);

                /* projection matrix state managed by 1.21.11 renderer */
                Area fullScreen = new Area(0, 0, window.getGuiScaledWidth(), window.getGuiScaledHeight());
                VideoRenderer.renderClips(new PoseStack(), currentMenu.context.batcher, panel.getData().camera.getClips(panel.getCursor()), panel.getCursor(), panel.getRunner().isRunning(), fullScreen, fullScreen, null, window.getGuiScaledWidth(), window.getGuiScaledHeight(), false);
                /* projection matrix state managed by 1.21.11 renderer */
            }
        }

        renderingWorld = false;
    }

    private static void updateCloudRenderMode(Minecraft mc)
    {
        boolean shouldHideClouds = BBSSettings.chromaSkyEnabled.get() && !BBSSettings.chromaSkyClouds.get();

        if (shouldHideClouds)
        {
            if (!cloudsForced)
            {
                cachedCloudRenderMode = mc.options.cloudStatus().get();
                cloudsForced = true;
            }

            if (mc.options.cloudStatus().get() != CloudStatus.OFF)
            {
                mc.options.cloudStatus().set(CloudStatus.OFF);
            }
        }
        else if (cloudsForced)
        {
            if (cachedCloudRenderMode != null)
            {
                mc.options.cloudStatus().set(cachedCloudRenderMode);
            }

            cloudsForced = false;
        }
    }

    public static void onRenderBeforeScreen()
    {
        Texture texture = getTexture();

        texture.bind();
        texture.setSize(framebuffer.width, framebuffer.height);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, framebuffer.width, framebuffer.height);

        toggleFramebuffer(false);
    }

    public static void onRenderChunkLayer(PoseStack stack)
    {
        // Left intentionally empty for 1.21.11: world render context is provided directly by events.
    }

    public static void onRenderChunkLayer(Matrix4f positionMatrix, Matrix4f projectionMatrix)
    {
        // Left intentionally empty for 1.21.11: world render context is provided directly by events.
    }

    public static void renderHud(GuiGraphicsExtractor drawContext, float tickDelta)
    {
        Batcher2D batcher2D = new Batcher2D(drawContext);
        VideoRecorder videoRecorder = BBSModClient.getVideoRecorder();

        BBSModClient.getFilms().renderHud(batcher2D, tickDelta);

        boolean showRecordingOverlay = videoRecorder.isRecording() && BBSSettings.recordingOverlays.get() && UIScreen.getCurrentMenu() == null;

        if (showRecordingOverlay)
        {
            int count = videoRecorder.getCounter();
            String label = UIKeys.FILM_VIDEO_RECORDING.format(
                count,
                BBSModClient.getKeyRecordVideo().getTranslatedKeyMessage().getString()
            ).get();

            int x = 5;
            int y = 5;
            int w = batcher2D.getFont().getWidth(label);

            batcher2D.box(x, y, x + 18 + w + 3, y + 16, Colors.A50);
            batcher2D.icon(Icons.SPHERE, Colors.RED | Colors.A100, x, y);
            batcher2D.textShadow(label, x + 18, y + 4);
        }

        if (UIScreen.getCurrentMenu() == null && BBSSettings.editorReplayHud.get())
        {
            renderSelectedReplayHud(drawContext, batcher2D, showRecordingOverlay ? 20 : 0);
        }
    }

    private static void renderSelectedReplayHud(GuiGraphicsExtractor drawContext, Batcher2D batcher2D, int yOffset)
    {
        Replay replay = BBSModClient.getSelectedReplay();

        if (replay == null)
        {
            return;
        }

        Form form = replay.form.get();
        String label = getReplayHudLabel(replay);
        boolean hasLabel = BBSSettings.editorReplayHudDisplayName.get() && !label.isEmpty();
        boolean hasForm = form != null;

        if (!hasForm && !hasLabel)
        {
            return;
        }

        int size = hasForm ? 24 : 0;
        int padding = 3;
        int gap = hasForm && hasLabel ? 4 : 0;

        int margin = 5;
        float textScale = 0.67F;
        int textWidth = hasLabel ? batcher2D.getFont().getWidth(label) : 0;
        int textHeight = hasLabel ? batcher2D.getFont().getHeight() : 0;
        int scaledTextWidth = Math.round(textWidth * textScale);
        int scaledTextHeight = Math.round(textHeight * textScale);
        int boxH = Math.max(size, scaledTextHeight) + padding * 2;
        int textBoxW = hasLabel ? scaledTextWidth + padding * 2 : 0;
        int totalW = (hasForm ? size : 0) + (hasLabel ? gap + textBoxW : 0);
        int x = getReplayHudX(margin, totalW);
        int y = getReplayHudY(margin + yOffset, boxH);
        int contentX = x + padding;
        int contentY = y + padding;

        int textBoxX = contentX + (hasForm ? size + gap : 0) - padding;
        int textBoxH = scaledTextHeight + padding * 2;
        int textBoxY = y + (boxH - textBoxH) / 2;

        if (hasLabel)
        {
            batcher2D.box(textBoxX, textBoxY, textBoxX + textBoxW, textBoxY + textBoxH, Colors.A50);
        }

        if (hasForm)
        {
            Minecraft mc = Minecraft.getInstance();
            Window window = mc.getWindow();

            replayHudMenu.resize(window.getGuiScaledWidth(), window.getGuiScaledHeight());
            replayHudMenu.context.setup(new UIRenderingContext(drawContext));

            int modelX1 = contentX;
            int modelY1 = contentY + (boxH - padding * 2 - size) / 2;
            int modelX2 = modelX1 + size;
            int modelY2 = modelY1 + size;

            try
            {
                FormRenderer.setSuppressFormDisplayName(true);
                FormUtilsClient.renderUI(form, replayHudMenu.context, modelX1, modelY1, modelX2, modelY2);
            }
            finally
            {
                FormRenderer.setSuppressFormDisplayName(false);
            }
        }

        if (hasLabel)
        {
            int textX = textBoxX + padding;
            int textY = textBoxY + padding;

            drawContext.pose().pushMatrix();
            drawContext.pose().scale(textScale, textScale);
            batcher2D.textShadow(label, textX / textScale, textY / textScale);
            drawContext.pose().popMatrix();
        }
    }

    private static String getReplayHudLabel(Replay replay)
    {
        String label = replay.label.get();

        if (!label.isEmpty())
        {
            return label;
        }

        Form form = replay.form.get();

        return form == null ? "" : form.getDefaultDisplayNameForHud();
    }

    private static int getReplayHudX(int margin, int totalW)
    {
        int position = BBSSettings.editorReplayHudPosition.get();
        int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        boolean right = position == 1 || position == 3;

        return right ? screenW - margin - totalW : margin;
    }

    private static int getReplayHudY(int margin, int boxH)
    {
        int position = BBSSettings.editorReplayHudPosition.get();
        int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        boolean bottom = position == 2 || position == 3;
        int extraTopLeft = position == 0 ? 12 : 0;

        return bottom ? screenH - margin - boxH : margin + extraTopLeft;
    }

    public static void renderCoolStuff(LevelRenderContext worldRenderContext)
    {
        if (Minecraft.getInstance().screen instanceof UIScreen screen)
        {
            screen.renderInWorld(worldRenderContext);
        }

        BBSModClient.getFilms().render(worldRenderContext);
    }

    public static boolean isOptifinePresent()
    {
        return optifine;
    }

    public static boolean isRenderingWorld()
    {
        return renderingWorld;
    }

    public static boolean isIrisShadersEnabled()
    {
        if (!iris)
        {
            return false;
        }

        return IrisUtils.isShaderPackEnabled();
    }

    public static boolean isIrisShadowPass()
    {
        if (!iris)
        {
            return false;
        }

        return IrisUtils.isShadowPass();
    }

    public static void trackTexture(Texture texture)
    {
        if (!iris)
        {
            return;
        }

        IrisUtils.trackTexture(texture);
    }

    public static void setPBRTextureIntensity(float normalIntensity, float specularIntensity)
    {
        if (!iris)
        {
            return;
        }

        IrisUtils.setPBRTextureIntensity(normalIntensity, specularIntensity);
    }

    public static void clearPBRTextureIntensity()
    {
        if (!iris)
        {
            return;
        }

        IrisUtils.clearPBRTextureIntensity();
    }

    public static float[] calculateTangents(float[] t, float[] v, float[] n, float[] u)
    {
        if (!iris)
        {
            return t;
        }

        return IrisUtils.calculateTangents(t, v, n, u);
    }

    public static float[] calculateTangents(float[] v, float[] n, float[] u)
    {
        if (!iris)
        {
            return v;
        }

        return IrisUtils.calculateTangents(v, n, u);
    }

    public static void addUniforms(List<CachedUniform> list, Map<String, ShaderCurves.ShaderVariable> variableMap)
    {
        if (!iris)
        {
            return;
        }

        IrisUtils.addUniforms(list, variableMap);
    }

    public static List<String> getShadersSliderOptions()
    {
        if (!iris)
        {
            return Collections.emptyList();
        }

        return IrisUtils.getSliderProperties();
    }

    public static Map<String, String> getShadersLanguageMap(String language)
    {
        if (!iris)
        {
            return Collections.emptyMap();
        }

        return IrisUtils.getShadersLanguageMap(language);
    }

    /* Curves */

    public static Long getTimeOfDay()
    {
        if (!Minecraft.getInstance().isSameThread())
        {
            return null;
        }

        if (BBSModClient.getCameraController().getCurrent() instanceof CameraWorkCameraController controller)
        {
            Map<String, Double> values = CurveClip.getValues(controller.getContext());
            Double v = values != null ? values.get(ShaderCurves.SUN_ROTATION) : null;

            if (v != null)
            {
                return (long) (v * 1000L);
            }
        }

        return null;
    }

    public static Double getBrightness()
    {
        if (!Minecraft.getInstance().isSameThread())
        {
            return null;
        }

        if (BBSModClient.getCameraController().getCurrent() instanceof CameraWorkCameraController controller)
        {
            Map<String, Double> values = CurveClip.getValues(controller.getContext());
            Double v = values != null ? values.get(ShaderCurves.BRIGHTNESS) : null;

            if (v != null)
            {
                return v;
            }
        }

        return null;
    }

    public static Double getWeather()
    {
        if (!Minecraft.getInstance().isSameThread())
        {
            return null;
        }

        if (BBSModClient.getCameraController().getCurrent() instanceof CameraWorkCameraController controller)
        {
            Map<String, Double> values = CurveClip.getValues(controller.getContext());
            Double v = values != null ? values.get(ShaderCurves.WEATHER) : null;

            if (v != null)
            {
                return v;
            }
        }

        return null;
    }

    public static Function<VertexConsumer, VertexConsumer> getColorConsumer(Color color)
    {
        if (sodium)
        {
            return (b) -> SodiumUtils.createVertexBuffer(b, color);
        }

        return (b) -> new RecolorVertexConsumer(b, color);
    }
}
