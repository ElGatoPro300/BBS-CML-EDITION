package mchorse.bbs_mod.ui.film.clips;

import mchorse.bbs_mod.camera.clips.misc.SubtitleClip;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.film.utils.keyframes.UIFilmKeyframes;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

import net.minecraft.util.math.MathHelper;

public class UISubtitleClip extends UIClip<SubtitleClip>
{
    private static final Color DEFAULT_COLOR = Color.white();
    private static final Color DEFAULT_BACKGROUND = new Color().set(0);

    public UITrackpad x;
    public UITrackpad y;
    public UITrackpad size;
    public UITrackpad anchorX;
    public UITrackpad anchorY;
    public UIColor color;
    public UIToggle textShadow;
    public UITrackpad windowX;
    public UITrackpad windowY;
    public UIColor background;
    public UITrackpad backgroundOffset;
    public UITrackpad shadow;
    public UIToggle shadowOpaque;
    public UITrackpad lineHeight;
    public UITrackpad maxWidth;
    public UIButton edit;
    public UIKeyframeEditor keyframes;

    public UISubtitleClip(SubtitleClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.title.callback = (t) ->
        {
            int tick = this.getClipTick();

            this.clip.text.insert(tick, t);
            this.clip.title.set(t);
            this.fillData();
        };

        this.x = this.createChannelTrackpad(this.clip.x, UIKeys.CAMERA_PANELS_SUBTITLE_OFFSET_X, true, null, null);
        this.y = this.createChannelTrackpad(this.clip.y, UIKeys.CAMERA_PANELS_SUBTITLE_OFFSET_Y, true, null, null);

        this.size = this.createChannelTrackpad(this.clip.size, UIKeys.CAMERA_PANELS_SUBTITLE_SIZE, false, null, null);

        this.anchorX = this.createChannelTrackpad(this.clip.anchorX, UIKeys.CAMERA_PANELS_SUBTITLE_ANCHOR_X, false, 0F, 1F);
        this.anchorY = this.createChannelTrackpad(this.clip.anchorY, UIKeys.CAMERA_PANELS_SUBTITLE_ANCHOR_Y, false, 0F, 1F);

        this.color = this.createColorChannel(this.clip.color);
        this.color.withAlpha();

        this.textShadow = this.createBooleanChannel(this.clip.textShadow, UIKeys.CAMERA_PANELS_SUBTITLE_TEXT_SHADOW);

        this.windowX = this.createChannelTrackpad(this.clip.windowX, UIKeys.CAMERA_PANELS_SUBTITLE_WINDOW_X, false, 0F, 1F);
        this.windowY = this.createChannelTrackpad(this.clip.windowY, UIKeys.CAMERA_PANELS_SUBTITLE_WINDOW_Y, false, 0F, 1F);

        this.background = this.createColorChannel(this.clip.background);
        this.background.withAlpha();

        this.backgroundOffset = this.createChannelTrackpad(this.clip.backgroundOffset, UIKeys.CAMERA_PANELS_SUBTITLE_BACKGROUND_OFFSET, false, null, null);
        this.shadow = this.createChannelTrackpad(this.clip.shadow, UIKeys.CAMERA_PANELS_SUBTITLE_SHADOW, false, 0F, null);
        this.shadowOpaque = this.createBooleanChannel(this.clip.shadowOpaque, UIKeys.CAMERA_PANELS_SUBTITLE_OPAQUE);

        this.lineHeight = this.createIntegerChannelTrackpad(this.clip.lineHeight, UIKeys.CAMERA_PANELS_SUBTITLE_LINE_HEIGHT, true, 0F, null);
        this.lineHeight.tooltip(UIKeys.CAMERA_PANELS_SUBTITLE_LINE_HEIGHT, Direction.BOTTOM);

        this.maxWidth = this.createIntegerChannelTrackpad(this.clip.maxWidth, UIKeys.CAMERA_PANELS_SUBTITLE_MAX_WIDTH, true, 0F, null);
        this.maxWidth.tooltip(UIKeys.CAMERA_PANELS_SUBTITLE_MAX_WIDTH, Direction.BOTTOM);

        this.keyframes = this.createKeyframeEditor("subtitle_keyframes");

        this.edit = new UIButton(UIKeys.GENERAL_EDIT, (b) ->
        {
            this.editor.embedView(this.keyframes);
            this.keyframes.view.resetView();
            this.keyframes.view.getGraph().clearSelection();
        });
        this.edit.keys().register(Keys.FORMS_EDIT, () -> this.edit.clickItself());
    }

    private UITrackpad createChannelTrackpad(KeyframeChannel<Double> channel, IKey tooltip, boolean integer, Float min, Float max)
    {
        UITrackpad trackpad = new UITrackpad((v) ->
        {
            int tick = this.getClipTick();

            channel.insert(tick, v.doubleValue());
            this.fillData();
        });

        if (integer)
        {
            trackpad.integer();
        }

        if (min != null)
        {
            if (max != null)
            {
                trackpad.limit(min, max);
            }
            else
            {
                trackpad.limit(min);
            }
        }

        if (tooltip != null)
        {
            trackpad.tooltip(tooltip);
        }

        return trackpad;
    }

    private UITrackpad createIntegerChannelTrackpad(KeyframeChannel<Integer> channel, IKey tooltip, boolean integer, Float min, Float max)
    {
        UITrackpad trackpad = new UITrackpad((v) ->
        {
            int tick = this.getClipTick();

            channel.insert(tick, v.intValue());
            this.fillData();
        });

        if (integer)
        {
            trackpad.integer();
        }

        if (min != null)
        {
            if (max != null)
            {
                trackpad.limit(min, max);
            }
            else
            {
                trackpad.limit(min);
            }
        }

        if (tooltip != null)
        {
            trackpad.tooltip(tooltip);
        }

        return trackpad;
    }

    private UIColor createColorChannel(KeyframeChannel<Color> channel)
    {
        return new UIColor((c) ->
        {
            int tick = this.getClipTick();

            channel.insert(tick, Color.rgba(c));
            this.fillData();
        });
    }

    private UIToggle createBooleanChannel(KeyframeChannel<Boolean> channel, IKey label)
    {
        return new UIToggle(label, (b) ->
        {
            int tick = this.getClipTick();

            channel.insert(tick, b.getValue());
            this.fillData();
        });
    }

    private UIKeyframeEditor createKeyframeEditor(String undoId)
    {
        UIKeyframeEditor editor = new UIKeyframeEditor((consumer) -> new UIFilmKeyframes(this.editor, consumer));

        editor.view.backgroundRenderer((context) ->
        {
            UIReplaysEditor.renderBackground(context, editor.view, (Clips) this.clip.getParent(), this.clip.tick.get(), this.clip);
        });
        editor.view.duration(() -> this.clip.duration.get());
        editor.setUndoId(undoId);

        return editor;
    }

    private int getClipTick()
    {
        return MathHelper.clamp(this.editor.getCursor() - this.clip.tick.get(), 0, this.clip.duration.get());
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(UI.column(UIClip.label(UIKeys.CAMERA_PANELS_SUBTITLE_OFFSET), UI.row(this.x, this.y)).marginTop(6));
        this.panels.add(UI.column(UIClip.label(UIKeys.CAMERA_PANELS_SUBTITLE_SIZE), this.size, this.color, this.textShadow).marginTop(6));
        this.panels.add(UI.column(UIClip.label(UIKeys.CAMERA_PANELS_SUBTITLE_ANCHOR), UI.row(this.anchorX, this.anchorY)).marginTop(6));
        this.panels.add(UI.column(UIClip.label(UIKeys.CAMERA_PANELS_SUBTITLE_WINDOW), UI.row(this.windowX, this.windowY)).marginTop(6));
        this.panels.add(UI.column(UIClip.label(UIKeys.CAMERA_PANELS_SUBTITLE_BACKGROUND), this.background, this.backgroundOffset).marginTop(6));
        this.panels.add(UI.column(UIClip.label(UIKeys.CAMERA_PANELS_SUBTITLE_SHADOW), this.shadow, this.shadowOpaque).marginTop(6));
        this.panels.add(UI.column(UIClip.label(UIKeys.CAMERA_PANELS_SUBTITLE_CONSTRAINT), UI.row(this.lineHeight, this.maxWidth)).marginTop(6));
        this.panels.add(UI.column(UIClip.label(UIKeys.SCREEN_PANELS_KEYFRAMES), this.edit).marginTop(6));
    }

    @Override
    public void fillData()
    {
        super.fillData();

        /* setText() moves the caret to the start — skip while the user is typing. */
        if (!this.title.isFocused())
        {
            this.title.setText(this.getStringValue(this.clip.text, this.clip.title.get()));
        }

        this.x.setValue(this.getDoubleValue(this.clip.x, 0D));
        this.y.setValue(this.getDoubleValue(this.clip.y, 0D));
        this.size.setValue(this.getDoubleValue(this.clip.size, 10D));
        this.anchorX.setValue(this.getDoubleValue(this.clip.anchorX, 0.5D));
        this.anchorY.setValue(this.getDoubleValue(this.clip.anchorY, 0.5D));
        this.color.setColor(this.getColorValue(this.clip.color, DEFAULT_COLOR).getARGBColor());
        this.textShadow.setValue(this.getBooleanValue(this.clip.textShadow, true));
        this.windowX.setValue(this.getDoubleValue(this.clip.windowX, 0.5D));
        this.windowY.setValue(this.getDoubleValue(this.clip.windowY, 0.5D));
        this.background.setColor(this.getColorValue(this.clip.background, DEFAULT_BACKGROUND).getARGBColor());
        this.backgroundOffset.setValue(this.getDoubleValue(this.clip.backgroundOffset, 2D));
        this.shadow.setValue(this.getDoubleValue(this.clip.shadow, 0D));
        this.shadowOpaque.setValue(this.getBooleanValue(this.clip.shadowOpaque, false));
        this.lineHeight.setValue(this.getIntegerValue(this.clip.lineHeight, 12));
        this.maxWidth.setValue(this.getIntegerValue(this.clip.maxWidth, 0));

        this.keyframes.setChannels(this.clip.channels);
        this.updateTrackTitles();
    }

    private String getStringValue(KeyframeChannel<String> channel, String fallback)
    {
        int tick = this.getClipTick();

        if (channel.isEmpty())
        {
            return fallback;
        }

        return channel.interpolate(tick, fallback);
    }

    private double getDoubleValue(KeyframeChannel<Double> channel, double fallback)
    {
        int tick = this.getClipTick();

        if (channel.isEmpty())
        {
            return fallback;
        }

        return channel.interpolate(tick);
    }

    private int getIntegerValue(KeyframeChannel<Integer> channel, int fallback)
    {
        int tick = this.getClipTick();

        if (channel.isEmpty())
        {
            return fallback;
        }

        return channel.interpolate(tick, fallback);
    }

    private boolean getBooleanValue(KeyframeChannel<Boolean> channel, boolean fallback)
    {
        int tick = this.getClipTick();

        if (channel.isEmpty())
        {
            return fallback;
        }

        return channel.interpolate(tick, fallback);
    }

    private Color getColorValue(KeyframeChannel<Color> channel, Color fallback)
    {
        int tick = this.getClipTick();

        if (channel.isEmpty())
        {
            return fallback;
        }

        return channel.interpolate(tick, fallback);
    }

    private void updateTrackTitles()
    {
        for (UIKeyframeSheet sheet : this.keyframes.view.getGraph().getSheets())
        {
            sheet.title = this.getTrackTitle(sheet.id);
        }
    }

    private IKey getTrackTitle(String id)
    {
        return switch (id)
        {
            case "text" -> UIKeys.CAMERA_PANELS_TITLE;
            case "x" -> UIKeys.CAMERA_PANELS_SUBTITLE_OFFSET_X;
            case "y" -> UIKeys.CAMERA_PANELS_SUBTITLE_OFFSET_Y;
            case "size" -> UIKeys.CAMERA_PANELS_SUBTITLE_SIZE;
            case "anchorX" -> UIKeys.CAMERA_PANELS_SUBTITLE_ANCHOR_X;
            case "anchorY" -> UIKeys.CAMERA_PANELS_SUBTITLE_ANCHOR_Y;
            case "color" -> UIKeys.CAMERA_PANELS_SUBTITLE_COLOR;
            case "textShadow" -> UIKeys.CAMERA_PANELS_SUBTITLE_TEXT_SHADOW;
            case "windowX" -> UIKeys.CAMERA_PANELS_SUBTITLE_WINDOW_X;
            case "windowY" -> UIKeys.CAMERA_PANELS_SUBTITLE_WINDOW_Y;
            case "background" -> UIKeys.CAMERA_PANELS_SUBTITLE_BACKGROUND;
            case "backgroundOffset" -> UIKeys.CAMERA_PANELS_SUBTITLE_BACKGROUND_OFFSET;
            case "shadow" -> UIKeys.CAMERA_PANELS_SUBTITLE_SHADOW;
            case "shadowOpaque" -> UIKeys.CAMERA_PANELS_SUBTITLE_OPAQUE;
            case "lineHeight" -> UIKeys.CAMERA_PANELS_SUBTITLE_LINE_HEIGHT;
            case "maxWidth" -> UIKeys.CAMERA_PANELS_SUBTITLE_MAX_WIDTH;
            default -> IKey.constant(id);
        };
    }

    @Override
    public void applyUndoData(MapType data)
    {
        super.applyUndoData(data);

        if (data.getString("embed").equals("subtitle_keyframes"))
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
            data.putString("embed", "subtitle_keyframes");
        }
    }
}
