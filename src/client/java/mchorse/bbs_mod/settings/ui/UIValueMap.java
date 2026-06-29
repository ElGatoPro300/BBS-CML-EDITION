package mchorse.bbs_mod.settings.ui;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.value.ValueKeyCombo;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.core.ValueLink;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueDouble;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.settings.values.ui.ValueLanguage;
import mchorse.bbs_mod.settings.values.ui.ValueVideoSettings;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.context.UIInterpolationContextMenu;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UIKeybind;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UILabelOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.framework.elements.utils.UIText;
import mchorse.bbs_mod.ui.utils.Label;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.FFMpegUtils;
import mchorse.bbs_mod.utils.OS;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.interps.Interpolation;
import mchorse.bbs_mod.utils.interps.Interpolations;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class UIValueMap
{
    public static Map<Class<? extends BaseValue>, IUIValueFactory<? extends BaseValue>> factories = new HashMap<>();

    static
    {
        register(ValueBoolean.class, (value, ui) ->
        {
            UIToggle toggle = UIValueFactory.booleanUINoLabel(value, null);

            toggle.w(18);

            return Arrays.asList(UIValueFactory.column(toggle, value));
        });

        register(ValueDouble.class, (value, ui) ->
        {
            UITrackpad trackpad = UIValueFactory.doubleUI(value, null);

            trackpad.w(90);

            return Arrays.asList(UIValueFactory.column(trackpad, value));
        });

        register(ValueFloat.class, (value, ui) ->
        {
            UITrackpad trackpad = UIValueFactory.floatUI(value, null);

            trackpad.w(90);

            return Arrays.asList(UIValueFactory.column(trackpad, value));
        });

        register(ValueInt.class, (value, ui) ->
        {
            if (value == BBSSettings.defaultInterpolation || value == BBSSettings.defaultPathInterpolation)
            {
                List<IKey> labels = value.getLabels();
                int currentIndex = value.get();
                IKey label = UIKeys.CAMERA_PANELS_INTERPOLATION;

                if (labels != null && !labels.isEmpty())
                {
                    if (currentIndex < 0 || currentIndex >= labels.size())
                    {
                        currentIndex = 0;
                    }

                    label = labels.get(currentIndex);
                }

                UIButton button = new UIButton(label, (b) ->
                {
                    Interpolation interpolation = new Interpolation("default", Interpolations.MAP);
                    int index = value.get();
                    int i = 0;

                    for (IInterp interp : Interpolations.MAP.values())
                    {
                        if (i == index)
                        {
                            interpolation.setInterp(interp);
                            break;
                        }

                        i++;
                    }

                    UIInterpolationContextMenu menu = new UIInterpolationContextMenu(interpolation).callback(() ->
                    {
                        IInterp selected = interpolation.getInterp();
                        int idx = 0;

                        for (IInterp interp : Interpolations.MAP.values())
                        {
                            if (interp == selected || interp.equals(selected))
                            {
                                value.set(idx);

                                if (labels != null && !labels.isEmpty())
                                {
                                    if (idx < 0 || idx >= labels.size())
                                    {
                                        idx = 0;
                                    }

                                    b.label = labels.get(idx);
                                }

                                break;
                            }

                            idx++;
                        }
                    });

                    ui.getContext().replaceContextMenu(menu);
                });

                button.w(90);

                return Arrays.asList(UIValueFactory.column(button, value));
            }

            if (value.getSubtype() == ValueInt.Subtype.COLOR || value.getSubtype() == ValueInt.Subtype.COLOR_ALPHA)
            {
                UIColor color = UIValueFactory.colorUI(value, null);

                color.w(90);

                return Arrays.asList(UIValueFactory.column(color, value));
            }
            else if (value.getSubtype() == ValueInt.Subtype.MODES)
            {
                UICirculate button = new UICirculate(null);

                for (IKey key : value.getLabels())
                {
                    button.addLabel(key);
                }

                button.callback = (b) -> value.set(button.getValue());
                button.setValue(value.get());
                button.w(90);

                return Arrays.asList(UIValueFactory.column(button, value));
            }

            UITrackpad trackpad = UIValueFactory.intUI(value, null);

            trackpad.w(90);

            return Arrays.asList(UIValueFactory.column(trackpad, value));
        });

        register(ValueLanguage.class, (value, ui) ->
        {
            Supplier<IKey> getLangLabel = () -> {
                for (Label<String> label : BBSModClient.getL10n().getSupportedLanguageLabels())
                {
                    if (label.value.equals(value.get()))
                    {
                        return label.title;
                    }
                }
                return UIKeys.LANGUAGE_PICK;
            };

            UIButton button = new UIButton(getLangLabel.get(), (b) ->
            {
                List<Label<String>> labels = BBSModClient.getL10n().getSupportedLanguageLabels();
                UILabelOverlayPanel<String> panel = new UILabelOverlayPanel<>(UIKeys.LANGUAGE_PICK_TITLE, labels, (str) -> value.set(str.value));

                panel.set(value.get());
                UIOverlay.addOverlay(ui.getContext(), panel);
            });

            value.postCallback((changed, flag) -> button.label = getLangLabel.get());

            button.w(150);

            UIText credits = new UIText().text(UIKeys.LANGUAGE_CREDITS).updates();

            return Arrays.asList(UIValueFactory.column(button, value), credits.marginBottom(8));
        });

        register(ValueLink.class, (value, ui) ->
        {
            UIButton pick = new UIButton(UIKeys.TEXTURE_PICK_TEXTURE, (button) ->
            {
                UITexturePicker.open(ui.getContext(), value.get(), value::set);
            });

            pick.w(90);

            return Arrays.asList(UIValueFactory.column(pick, value));
        });

        register(ValueString.class, (value, ui) ->
        {
            UITextbox textbox = UIValueFactory.stringUI(value, null);

            textbox.w(90);

            if (value == BBSSettings.videoEncoderPath && OS.CURRENT == OS.WINDOWS)
            {
                textbox.context((menu) ->
                {
                    menu.action(Icons.SEARCH, UIKeys.GENERAL_FFMPEG_FIND, () ->
                    {
                        textbox.getContext().replaceContextMenu((submenu) ->
                        {
                            File[] files = File.listRoots();
                            File file = files.length == 0 ? new File("C:\\") : files[0];
                            Optional<Path> ffmpeg = FFMpegUtils.findFFMpeg(file.toPath());

                            if (ffmpeg.isPresent())
                            {
                                Path path = ffmpeg.get();
                                String pathString = path.toAbsolutePath().toString();

                                submenu.action(Icons.VIDEO_CAMERA, IKey.constant(pathString), () ->
                                {
                                    textbox.setText(pathString);
                                    value.set(pathString);
                                });
                            }
                        });
                    });
                });
            }

            return Arrays.asList(UIValueFactory.column(textbox, value));
        });

        register(ValueKeyCombo.class, (value, ui) ->
        {
            UILabel label = UI.label(value.get().label, 0).labelAnchor(0, 0.5F);
            UIKeybind keybind = new UIKeybind(value::set).mouse().escape();

            keybind.setKeyCombo(value.get());
            keybind.w(100);

            return Arrays.asList(UI.row(label, keybind).tooltip(value.get().label));
        });

        register(ValueVideoSettings.class, (value, ui) ->
        {
            List<UIElement> list = new ArrayList<>();

            UITextbox arguments = UIValueFactory.stringUI(value.arguments, null);
            arguments.w(180);
            list.add(customColumn(arguments, UIKeys.VIDEO_SETTINGS_ARGS, IKey.raw("")));

            UITextbox argumentsAudio = UIValueFactory.stringUI(value.argumentsAudio, null);
            argumentsAudio.w(180);
            list.add(customColumn(argumentsAudio, UIKeys.VIDEO_SETTINGS_AUDIO_ARGS, IKey.raw("")));

            UIToggle audio = new UIToggle(UIKeys.VIDEO_SETTINGS_AUDIO, value.audio.get(), (b) ->
            {
                value.audio.set(b.getValue());
            });
            audio.tooltip(UIKeys.VIDEO_SETTINGS_AUDIO_TOOLTIP);
            audio.w(1F).h(20);
            list.add(audio);

            UIToggle audioEnvironment = new UIToggle(UIKeys.VIDEO_SETTINGS_AUDIO_ENVIRONMENT, value.audioEnvironment.get(), (b) ->
            {
                value.audioEnvironment.set(b.getValue());
            });
            audioEnvironment.tooltip(UIKeys.VIDEO_SETTINGS_AUDIO_ENVIRONMENT_TOOLTIP);
            audioEnvironment.w(1F).h(20);
            list.add(audioEnvironment);

            UITrackpad width = UIValueFactory.intUI(value.width, null);
            value.width.postCallback((changed, flag) -> width.setValue(value.width.get()));
            width.w(90);
            list.add(customColumn(width, UIKeys.VIDEO_SETTINGS_WIDTH, IKey.raw("")));

            UITrackpad height = UIValueFactory.intUI(value.height, null);
            value.height.postCallback((changed, flag) -> height.setValue(value.height.get()));
            height.w(90);
            list.add(customColumn(height, UIKeys.VIDEO_SETTINGS_HEIGHT, IKey.raw("")));

            UITrackpad frameRate = UIValueFactory.intUI(value.frameRate, null);
            frameRate.w(90);
            list.add(customColumn(frameRate, UIKeys.VIDEO_SETTINGS_FRAME_RATE, IKey.raw("")));

            UITrackpad motionBlur = UIValueFactory.intUI(value.motionBlur, null);
            motionBlur.w(90);
            list.add(customColumn(motionBlur, UIKeys.VIDEO_SETTINGS_MOTION_BLUR, UIKeys.VIDEO_SETTINGS_MOTION_BLUR_TOOLTIP));

            UITrackpad heldFrames = UIValueFactory.intUI(value.heldFrames, null);
            heldFrames.w(90);
            list.add(customColumn(heldFrames, UIKeys.VIDEO_SETTINGS_HELD_FRAMES, UIKeys.VIDEO_SETTINGS_HELD_FRAMES_TOOLTIP));

            UITextbox path = UIValueFactory.stringUI(value.path, null);
            path.w(140);
            list.add(customColumn(path, UIKeys.VIDEO_SETTINGS_PATH, IKey.raw("")));

            return list;
        });
    }

    private static UIElement customColumn(UIElement control, IKey label, IKey tooltip)
    {
        UIElement element = new UIElement();
        control.removeTooltip();

        String comment = tooltip.get();
        boolean hasComment = !comment.isEmpty()
            && !comment.startsWith("bbs.settings.")
            && !comment.startsWith("cml.settings.")
            && (BBSSettings.hideSettingDescriptions == null || !BBSSettings.hideSettingDescriptions.get());

        if (hasComment)
        {
            UILabel titleLabel = UI.label(label, 0).labelAnchor(0, 0.5F);
            titleLabel.relative(element).w(1F).h(11);
            UIText desc = new UIText(tooltip)
                .color(0xFF777788, true);
            desc.relative(element).w(1F);

            element.column(3).vertical().padding(0).height(0);
            element.add(titleLabel, desc.marginTop(1), control.marginTop(2));
        }
        else
        {
            element.row(0).preferred(0).height(20);
            element.add(UI.label(label, 0).labelAnchor(0, 0.5F), control);
            control.marginTop(0);
        }

        return element;
    }

    public static <T extends BaseValue> void register(Class<T> clazz, IUIValueFactory<T> factory)
    {
        factories.put(clazz, factory);
    }

    public static <T extends BaseValue> List<UIElement> create(T value, UIElement element)
    {
        IUIValueFactory<T> factory = (IUIValueFactory<T>) factories.get(value.getClass());

        return factory == null ? Collections.emptyList() : factory.create(value, element);
    }

    public static interface IUIValueFactory <T extends BaseValue>
    {
        public List<UIElement> create(T value, UIElement element);
    }
}
