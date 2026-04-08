package mchorse.bbs_mod.ui.film.clips;

import mchorse.bbs_mod.camera.clips.screen.ColorClip;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.film.utils.keyframes.UIFilmKeyframes;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.clips.Clips;

public class UIColorClip extends UIClip<ColorClip>
{
    public UIColor overlayColor;
    public UIColor vignetteColor;
    public UIButton edit;
    public UIKeyframeEditor keyframes;

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

        this.vignetteColor = new UIColor((c) -> this.editor.editMultiple(this.clip.vignetteColor, (value) ->
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
        this.panels.add(UI.column(UIClip.label(UIKeys.SCREEN_PANELS_VIGNETTE_COLOR), this.vignetteColor).marginTop(6));
        this.panels.add(UI.column(UIClip.label(UIKeys.SCREEN_PANELS_KEYFRAMES), this.edit).marginTop(6));
    }

    @Override
    public void fillData()
    {
        super.fillData();

        this.overlayColor.setColor(this.clip.overlayColor.get());
        this.vignetteColor.setColor(this.clip.vignetteColor.get());
        this.keyframes.setChannels(this.clip.channels);
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
