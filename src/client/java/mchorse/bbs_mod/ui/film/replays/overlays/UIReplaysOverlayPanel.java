package mchorse.bbs_mod.ui.film.replays.overlays;

import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.replays.UIReplayList;
import mchorse.bbs_mod.ui.forms.UINestedEdit;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIAnchorKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIDataUtils;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class UIReplaysOverlayPanel extends UIOverlayPanel
{
    public static final List<Consumer<UIReplaysOverlayPanel>> extensions = new ArrayList<>();

    public UIReplayList replays;

    public UIElement replayProperties;
    public UIElement groupProperties;
    public UIElement replayLabel;
    public UINestedEdit pickEdit;
    public UIToggle enabled;
    public UITextbox label;
    public UITextbox groupLabel;
    public UITextbox nameTag;
    public UIToggle shadow;
    public UITrackpad shadowSize;
    public UIElement loopingLabel;
    public UITrackpad looping;
    public UIToggle actor;
    public UIToggle fp;
    public UIToggle relative;
    public UIElement relativeRow;
    public UITrackpad relativeOffsetX;
    public UITrackpad relativeOffsetY;
    public UITrackpad relativeOffsetZ;
    public UIToggle axesPreview;
    public UIButton pickAxesPreviewBone;

    /* Item drop velocity configuration */
    public UITrackpad dropVelocityMinX;
    public UITrackpad dropVelocityMaxX;
    public UITrackpad dropVelocityMinY;
    public UITrackpad dropVelocityMaxY;
    public UITrackpad dropVelocityMinZ;
    public UITrackpad dropVelocityMaxZ;

    private Consumer<Replay> callback;
    private boolean docked;

    public UIReplaysOverlayPanel(UIFilmPanel filmPanel, Consumer<Replay> callback)
    {
        super(UIKeys.FILM_REPLAY_TITLE);

        this.callback = callback;
        this.replays = new UIReplayList((l) -> this.callback.accept(l.isEmpty() ? null : l.get(0)), this, filmPanel);

        this.pickEdit = new UINestedEdit((editing) ->
        {
            this.replays.openFormEditor(this.replays.getCurrent().get(0).form, editing, this.pickEdit::setForm);
        });
        this.pickEdit.keybinds();
        this.pickEdit.pick.tooltip(UIKeys.SCENE_REPLAYS_CONTEXT_PICK_FORM);
        this.pickEdit.edit.tooltip(UIKeys.SCENE_REPLAYS_CONTEXT_EDIT_FORM);
        this.enabled = new UIToggle(UIKeys.CAMERA_PANELS_ENABLED, (b) ->
        {
            this.edit((replay) -> replay.enabled.set(b.getValue()));
            filmPanel.getController().createEntities();
        });
        this.label = new UITextbox(1000, (s) -> this.edit((replay) -> replay.label.set(s)));
        this.label.textbox.setPlaceholder(UIKeys.FILM_REPLAY_LABEL);

        this.groupLabel = new UITextbox(1000, (s) ->
        {
             this.edit((replay) ->
             {
                 if (replay.isGroup.get())
                 {
                     replay.label.set(s);
                     this.replays.buildVisualList();
                     this.replays.setCurrentDirect(replay);
                 }
             });
        });
        this.groupLabel.textbox.setPlaceholder(UIKeys.FILM_REPLAY_LABEL);
        this.nameTag = new UITextbox(1000, (s) -> this.edit((replay) -> replay.nameTag.set(s)));
        this.nameTag.textbox.setPlaceholder(UIKeys.FILM_REPLAY_NAME_TAG);
        this.shadow = new UIToggle(UIKeys.FILM_REPLAY_SHADOW, (b) -> this.edit((replay) -> replay.shadow.set(b.getValue())));
        this.shadowSize = new UITrackpad((v) -> this.edit((replay) -> replay.shadowSize.set(v.floatValue())));
        this.shadowSize.tooltip(UIKeys.FILM_REPLAY_SHADOW_SIZE);
        this.looping = new UITrackpad((v) -> this.edit((replay) -> replay.looping.set(v.intValue())));
        this.looping.limit(0).integer().tooltip(UIKeys.FILM_REPLAY_LOOPING_TOOLTIP);
        this.actor = new UIToggle(UIKeys.FILM_REPLAY_ACTOR, (b) -> this.edit((replay) -> replay.actor.set(b.getValue())));
        this.actor.tooltip(UIKeys.FILM_REPLAY_ACTOR_TOOLTIP);
        this.fp = new UIToggle(UIKeys.FILM_REPLAY_FP, (b) ->
        {
            for (Replay replay : this.replays.getList())
            {
                if (replay.fp.get())
                {
                    replay.fp.set(false);
                }
            }

            this.replays.getCurrentFirst().fp.set(b.getValue());
        });
        this.relative = new UIToggle(UIKeys.CAMERA_PANELS_RELATIVE, (b) -> this.edit((replay) -> replay.relative.set(b.getValue())));
        this.relative.tooltip(UIKeys.FILM_REPLAY_RELATIVE_TOOLTIP);
        this.relativeOffsetX = new UITrackpad((v) -> this.edit((replay) -> BaseValue.edit(replay.relativeOffset, (value) -> value.get().x = v)));
        this.relativeOffsetY = new UITrackpad((v) -> this.edit((replay) -> BaseValue.edit(replay.relativeOffset, (value) -> value.get().y = v)));
        this.relativeOffsetZ = new UITrackpad((v) -> this.edit((replay) -> BaseValue.edit(replay.relativeOffset, (value) -> value.get().z = v)));
        this.axesPreview = new UIToggle(UIKeys.FILM_REPLAY_AXES_PREVIEW, (b) ->
        {
            this.edit((replay) -> replay.axesPreview.set(b.getValue()));
        });
        this.pickAxesPreviewBone = new UIButton(UIKeys.FILM_REPLAY_PICK_AXES_PREVIEW, (b) ->
        {
            Replay replay = filmPanel.replayEditor.getReplay();

            UIAnchorKeyframeFactory.displayAttachments(filmPanel, filmPanel.getData().replays.getList().indexOf(replay), replay.axesPreviewBone.get(), (s) ->
            {
                this.edit((r) -> r.axesPreviewBone.set(s));
            });
        });

        /* Item drop velocity configuration */
        this.dropVelocityMinX = new UITrackpad((v) -> this.edit((replay) -> replay.dropVelocityMinX.set(v.floatValue())));
        this.dropVelocityMinX.tooltip(UIKeys.FILM_REPLAY_DROP_VELOCITY_MIN_X);
        this.dropVelocityMaxX = new UITrackpad((v) -> this.edit((replay) -> replay.dropVelocityMaxX.set(v.floatValue())));
        this.dropVelocityMaxX.tooltip(UIKeys.FILM_REPLAY_DROP_VELOCITY_MAX_X);
        this.dropVelocityMinY = new UITrackpad((v) -> this.edit((replay) -> replay.dropVelocityMinY.set(v.floatValue())));
        this.dropVelocityMinY.tooltip(UIKeys.FILM_REPLAY_DROP_VELOCITY_MIN_Y);
        this.dropVelocityMaxY = new UITrackpad((v) -> this.edit((replay) -> replay.dropVelocityMaxY.set(v.floatValue())));
        this.dropVelocityMaxY.tooltip(UIKeys.FILM_REPLAY_DROP_VELOCITY_MAX_Y);
        this.dropVelocityMinZ = new UITrackpad((v) -> this.edit((replay) -> replay.dropVelocityMinZ.set(v.floatValue())));
        this.dropVelocityMinZ.tooltip(UIKeys.FILM_REPLAY_DROP_VELOCITY_MIN_Z);
        this.dropVelocityMaxZ = new UITrackpad((v) -> this.edit((replay) -> replay.dropVelocityMaxZ.set(v.floatValue())));
        this.dropVelocityMaxZ.tooltip(UIKeys.FILM_REPLAY_DROP_VELOCITY_MAX_Z);


        this.replayLabel = UI.label(UIKeys.FILM_REPLAY_REPLAY);
        this.loopingLabel = UI.label(UIKeys.FILM_REPLAY_LOOPING);
        this.relativeRow = UI.row(this.relativeOffsetX, this.relativeOffsetY, this.relativeOffsetZ);

        this.replayProperties = UI.scrollView(5, 6,
            this.replayLabel,
            this.pickEdit, this.enabled,
            this.label, this.nameTag,
            this.shadow, this.shadowSize,
            this.loopingLabel,
            this.looping, this.actor, this.fp,
            this.relative, this.relativeRow,
            this.axesPreview, this.pickAxesPreviewBone,
            UI.label(UIKeys.FILM_REPLAY_DROP_VELOCITY),
            UI.row(5, 0, this.dropVelocityMinX, this.dropVelocityMaxX),
            UI.row(5, 0, this.dropVelocityMinY, this.dropVelocityMaxY),
            UI.row(5, 0, this.dropVelocityMinZ, this.dropVelocityMaxZ)
        );
        this.groupProperties = UI.scrollView(0, 0, this.groupLabel);

        this.replayProperties.relative(this.replays).x(1F).wTo(this.icons.area).h(1F);
        this.groupProperties.relative(this.replays).x(1F).wTo(this.icons.area).h(1F);
        this.replays.relative(this.content).w(0.5F).h(1F);

        this.content.add(this.replays, this.replayProperties, this.groupProperties);

        for (Consumer<UIReplaysOverlayPanel> consumer : extensions)
        {
            consumer.accept(this);
        }
    }

    public void setDocked(boolean docked)
    {
        this.docked = docked;

        if (docked)
        {
            this.title.setVisible(false);
            this.icons.setVisible(false);
            this.close.setVisible(false);

            this.title.area.set(0, 0, 0, 0);

            this.content.relative(this).xy(0, 0).w(1F).h(1F);
        }
        else
        {
            this.title.setVisible(true);
            this.icons.setVisible(true);
            this.close.setVisible(true);

            this.title.labelAnchor(0, 0.5F).relative(this).xy(6, 0).w(0.6F).h(20);
            this.icons.relative(this).x(1F, -20).y(0).w(20).h(1F).column(0).stretch();
            this.content.relative(this).xy(0, 20).w(1F, -20).h(1F, -20);
        }
    }

    private void edit(Consumer<Replay> consumer)
    {
        if (consumer != null)
        {
            List<Replay> current = this.replays.getCurrent();

            for (Replay replay : current)
            {
                consumer.accept(replay);
            }
        }
    }

    public void setReplay(Replay replay)
    {
        boolean hasReplay = replay != null;
        boolean isGroup = hasReplay && replay.isGroup.get();

        this.replayProperties.setVisible(hasReplay && !isGroup);
        this.groupProperties.setVisible(hasReplay && isGroup);

        if (hasReplay)
        {
            if (isGroup)
            {
                this.groupLabel.setText(replay.label.get());
            }
            else
            {
                this.label.setText(replay.label.get());

                this.pickEdit.setForm(replay.form.get());
                this.enabled.setValue(replay.enabled.get());
                this.nameTag.setText(replay.nameTag.get());
                this.shadow.setValue(replay.shadow.get());
                this.shadowSize.setValue(replay.shadowSize.get());
                this.looping.setValue(replay.looping.get());
                this.actor.setValue(replay.actor.get());
                this.fp.setValue(replay.fp.get());
                this.relative.setValue(replay.relative.get());
                this.relativeOffsetX.setValue(replay.relativeOffset.get().x);
                this.relativeOffsetY.setValue(replay.relativeOffset.get().y);
                this.relativeOffsetZ.setValue(replay.relativeOffset.get().z);
                this.axesPreview.setValue(replay.axesPreview.get());
                this.dropVelocityMinX.setValue(replay.dropVelocityMinX.get());
                this.dropVelocityMaxX.setValue(replay.dropVelocityMaxX.get());
                this.dropVelocityMinY.setValue(replay.dropVelocityMinY.get());
                this.dropVelocityMaxY.setValue(replay.dropVelocityMaxY.get());
                this.dropVelocityMinZ.setValue(replay.dropVelocityMinZ.get());
                this.dropVelocityMaxZ.setValue(replay.dropVelocityMaxZ.get());
            }
        }
    }

    @Override
    protected void renderBackground(UIContext context)
    {
        if (!this.docked)
        {
            super.renderBackground(context);
        }

        this.content.area.render(context.batcher, Colors.A100);

        if (this.replays.getList().size() < 3)
        {
            UIDataUtils.renderRightClickHere(context, this.replays.area);
        }
    }
}
