package mchorse.bbs_mod.ui.film.clips;

import mchorse.bbs_mod.camera.clips.screen.ColorClip;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.film.utils.keyframes.UIFilmKeyframes;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

import java.util.HashMap;
import java.util.Map;

public class UIColorClip extends UIClip<ColorClip>
{
    private static final int COLOR_OVERLAY = Colors.YELLOW;
    private static final int COLOR_VIGNETTE = Colors.CYAN;
    private static final int COLOR_GRADE = Colors.MAGENTA;
    private static final int COLOR_LIFT = Colors.RED;
    private static final int COLOR_GAMMA = Colors.GREEN;
    private static final int COLOR_GAIN = 0xffffff;
    private static final int COLOR_GROUP = Colors.LIGHTEST_GRAY & 0xffffff;

    public UIColor overlayColor;
    public UIButton edit;
    public UIKeyframeEditor keyframes;

    private final Map<String, Boolean> collapsed = new HashMap<>();

    public UIColorClip(ColorClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.overlayColor = new UIColor((c) -> this.editor.editMultiple(this.clip.overlayColor, (value) ->
        {
            value.set(c);
        }));

        this.keyframes = new UIKeyframeEditor((consumer) -> new UIFilmKeyframes(this.editor, consumer));
        this.keyframes.view.backgroundRenderer((context) ->
        {
            UIReplaysEditor.renderBackground(context, this.keyframes.view, (Clips) this.clip.getParent(), this.clip.tick.get(), this.clip);
        });
        this.keyframes.view.duration(() -> this.clip.duration.get());
        this.keyframes.setUndoId("color_keyframes");

        this.edit = new UIButton(UIKeys.GENERAL_EDIT, (b) ->
        {
            this.editor.embedView(this.keyframes);
            this.keyframes.view.resetView();
            this.keyframes.view.getGraph().clearSelection();
        });
        this.edit.keys().register(Keys.FORMS_EDIT, () -> this.edit.clickItself());
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(UI.column(UIClip.label(UIKeys.SCREEN_PANELS_OVERLAY_COLOR), this.overlayColor).marginTop(6));
        this.panels.add(UI.column(UIClip.label(UIKeys.SCREEN_PANELS_KEYFRAMES), this.edit).marginTop(6));
    }

    @Override
    public void fillData()
    {
        super.fillData();

        this.overlayColor.setColor(this.clip.overlayColor.get());
        this.rebuildChannels();
    }

    private void rebuildChannels()
    {
        UIKeyframes view = this.keyframes.view;

        view.removeAllSheets();

        this.addGroup(view, "overlay", IKey.constant("Overlay"), COLOR_OVERLAY,
            new KeyframeChannel[] {this.clip.overlayAlpha},
            new int[] {COLOR_OVERLAY});

        this.addGroup(view, "grade", IKey.constant("Grade"), COLOR_GRADE,
            new KeyframeChannel[] {this.clip.saturation, this.clip.hue, this.clip.brightness, this.clip.contrast},
            new int[] {Colors.YELLOW, Colors.MAGENTA, Colors.WHITE & 0xffffff, Colors.CYAN});

        this.addGroup(view, "lift", IKey.constant("Lift"), COLOR_LIFT,
            new KeyframeChannel[] {this.clip.liftR, this.clip.liftG, this.clip.liftB},
            new int[] {Colors.RED, Colors.GREEN, Colors.BLUE});

        this.addGroup(view, "gamma", IKey.constant("Gamma"), COLOR_GAMMA,
            new KeyframeChannel[] {this.clip.gammaR, this.clip.gammaG, this.clip.gammaB},
            new int[] {Colors.RED, Colors.GREEN, Colors.BLUE});

        this.addGroup(view, "gain", IKey.constant("Gain"), COLOR_GAIN,
            new KeyframeChannel[] {this.clip.gainR, this.clip.gainG, this.clip.gainB},
            new int[] {Colors.RED, Colors.GREEN, Colors.BLUE});

        this.addCinematicGroup(view);

        this.keyframes.view.getGraph().clearSelection();
    }

    private void addCinematicGroup(UIKeyframes view)
    {
        String key = "cinematic";
        boolean expanded = !this.collapsed.getOrDefault(key, false);

        UIKeyframeSheet header = UIKeyframeSheet.groupHeader(
            "__color__" + key,
            L10n.lang("bbs.ui.color_clip.cinematic"),
            COLOR_GRADE,
            key,
            expanded,
            () ->
            {
                this.collapsed.put(key, !this.collapsed.getOrDefault(key, false));
                this.rebuildChannels();
            }
        );

        header.level = 0;
        view.addSheet(header);

        if (expanded)
        {
            UIKeyframeSheet shAberration = new UIKeyframeSheet(
                "aberration",
                L10n.lang("bbs.ui.camera.clips.channel.aberration"),
                Colors.RED,
                false,
                this.clip.aberration,
                null
            );
            shAberration.level = 1;
            shAberration.groupKey = key;
            view.addSheet(shAberration);

            UIKeyframeSheet shVHS = new UIKeyframeSheet(
                "vhs",
                L10n.lang("bbs.ui.camera.clips.channel.vhs"),
                Colors.GREEN,
                false,
                this.clip.vhs,
                null
            );
            shVHS.level = 1;
            shVHS.groupKey = key;
            view.addSheet(shVHS);

            UIKeyframeSheet shLensDistortion = new UIKeyframeSheet(
                "lensDistortion",
                L10n.lang("bbs.ui.camera.clips.channel.lens_distortion"),
                Colors.BLUE,
                false,
                this.clip.lensDistortion,
                null
            );
            shLensDistortion.level = 1;
            shLensDistortion.groupKey = key;
            view.addSheet(shLensDistortion);

            UIKeyframeSheet shVintage = new UIKeyframeSheet(
                "vintage",
                L10n.lang("bbs.ui.camera.clips.channel.vintage"),
                Colors.YELLOW,
                false,
                this.clip.vintage,
                null
            );
            shVintage.level = 1;
            shVintage.groupKey = key;
            view.addSheet(shVintage);

            UIKeyframeSheet shRadialBlur = new UIKeyframeSheet(
                "radialBlur",
                L10n.lang("bbs.ui.camera.clips.channel.radial_blur"),
                Colors.CYAN,
                false,
                this.clip.radialBlur,
                null
            );
            shRadialBlur.level = 1;
            shRadialBlur.groupKey = key;
            view.addSheet(shRadialBlur);

            UIKeyframeSheet shRain = new UIKeyframeSheet(
                "rain",
                L10n.lang("bbs.ui.camera.clips.channel.rain"),
                0xff5577ff, // Light blue color
                false,
                this.clip.rain,
                null
            );
            shRain.level = 1;
            shRain.groupKey = key;
            view.addSheet(shRain);

            UIKeyframeSheet shDust = new UIKeyframeSheet(
                "dust",
                L10n.lang("bbs.ui.camera.clips.channel.dust"),
                0xffcccccc, // Light gray color
                false,
                this.clip.dust,
                null
            );
            shDust.level = 1;
            shDust.groupKey = key;
            view.addSheet(shDust);

            UIKeyframeSheet shLightLeak = new UIKeyframeSheet(
                "lightLeak",
                L10n.lang("bbs.ui.camera.clips.channel.light_leak"),
                0xffffa033, // Warm orange color
                false,
                this.clip.lightLeak,
                null
            );
            shLightLeak.level = 1;
            shLightLeak.groupKey = key;
            view.addSheet(shLightLeak);

            UIKeyframeSheet shNightVision = new UIKeyframeSheet(
                "nightVision",
                L10n.lang("bbs.ui.camera.clips.channel.night_vision"),
                0xff33ff33, // Bright green color
                false,
                this.clip.nightVision,
                null
            );
            shNightVision.level = 1;
            shNightVision.groupKey = key;
            view.addSheet(shNightVision);
        }
    }

    private void addGroup(UIKeyframes view, String key, IKey title, int color, KeyframeChannel[] channels, int[] colors)
    {
        boolean expanded = !this.collapsed.getOrDefault(key, false);

        UIKeyframeSheet header = UIKeyframeSheet.groupHeader(
            "__color__" + key,
            title,
            COLOR_GROUP,
            key,
            expanded,
            () ->
            {
                this.collapsed.put(key, !this.collapsed.getOrDefault(key, false));
                this.rebuildChannels();
            }
        );

        header.level = 0;
        view.addSheet(header);

        if (expanded)
        {
            for (int i = 0; i < channels.length; i++)
            {
                UIKeyframeSheet sheet = new UIKeyframeSheet(colors[i % colors.length], false, channels[i], null);

                sheet.level = 1;
                sheet.groupKey = key;
                view.addSheet(sheet);
            }
        }
    }

    @Override
    public void applyUndoData(MapType data)
    {
        super.applyUndoData(data);

        if (data.getString("embed").equals("color_keyframes"))
        {
            this.editor.embedView(this.keyframes);
            this.keyframes.view.resetView();
        }
    }

    @Override
    public void collectUndoData(MapType data)
    {
        super.collectUndoData(data);

        if (this.keyframes.hasParent())
        {
            data.putString("embed", "color_keyframes");
        }
    }
}
