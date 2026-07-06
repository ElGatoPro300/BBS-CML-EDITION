package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.UIClips;
import mchorse.bbs_mod.ui.film.UIClipsPanel;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.keys.KeyCombo;
import mchorse.bbs_mod.utils.interps.Interpolations;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Phase 2 wiring for timeline toolbar actions. Handlers mirror the existing
 * keybind / context-menu behaviour on {@link UIFilmPanel}, {@link UIClips} and
 * {@link UIKeyframes}.
 *
 * <p>Bindings are applied after each {@link TimelineToolbar#setSections(List)}
 * call because the registry returns fresh item trees.</p>
 */
public final class TimelineToolbarWiring
{
    /* Clips timelines (camera / action) */

    public static void wireClipsToolbar(UIClipsPanel panel)
    {
        UIFilmPanel filmPanel = panel.filmPanel;
        UIClips clips = panel.clips;
        TimelineToolbar toolbar = panel.toolbar;

        wireTransportAndHistory(filmPanel, toolbar);
        wireClipsSelection(clips, toolbar);
        wireClipsEditInstant(clips, toolbar);
        wireClipsEditSelection(clips, toolbar);
        wireClipsTransform(clips, toolbar);
    }

    /* Keyframe toolbar (embedded in clip panel or replay editor) */

    public static void wireKeyframesToolbar(UIFilmPanel filmPanel, UIKeyframes keyframes, TimelineToolbar toolbar)
    {
        wireTransportAndHistory(filmPanel, toolbar);
        wireKeyframesInstant(keyframes, toolbar);
        wireKeyframesEditSelection(keyframes, toolbar);
        wireKeyframesSelect(keyframes, toolbar);
        wireKeyframesGraphSelection(keyframes, toolbar);
        wireTracksInstant(keyframes, toolbar);
    }

    public static void wireReplaysToolbar(UIReplaysEditor editor)
    {
        wireTransportAndHistory(editor.getFilmPanel(), editor.toolbar);
        wireKeyframesInstant(editor, editor.toolbar);
        wireKeyframesEditSelection(editor, editor.toolbar);
        wireKeyframesSelect(editor, editor.toolbar);
        wireKeyframesGraphSelection(editor, editor.toolbar);
        wireTracksInstant(editor, editor.toolbar);
    }

    /* Shared transport + undo/redo */

    private static void wireTransportAndHistory(UIFilmPanel filmPanel, TimelineToolbar toolbar)
    {
        BooleanSupplier editorActive = () -> filmPanel.getData() != null && !filmPanel.isFlying();
        BooleanSupplier filmLoaded = () -> filmPanel.getData() != null;

        bindShortcut(toolbar, Keys.PLAUSE, () -> filmPanel.preview.plause.clickItself(), editorActive);
        bindShortcut(toolbar, Keys.NEXT, () -> filmPanel.setCursor(filmPanel.getCursor() + 1), editorActive);
        bindShortcut(toolbar, Keys.PREV, () -> filmPanel.setCursor(filmPanel.getCursor() - 1), editorActive);
        bindShortcut(toolbar, Keys.JUMP_FORWARD,
            () -> filmPanel.setCursor(filmPanel.getCursor() + BBSSettings.editorJump.get()), editorActive);
        bindShortcut(toolbar, Keys.JUMP_BACKWARD,
            () -> filmPanel.setCursor(filmPanel.getCursor() - BBSSettings.editorJump.get()), editorActive);
        bindShortcut(toolbar, Keys.NEXT_CLIP,
            () -> filmPanel.setCursor(filmPanel.getData().camera.findNextTick(filmPanel.getCursor())), editorActive);
        bindShortcut(toolbar, Keys.PREV_CLIP,
            () -> filmPanel.setCursor(filmPanel.getData().camera.findPreviousTick(filmPanel.getCursor())), editorActive);
        bindShortcut(toolbar, Keys.LOOPING, () ->
        {
            BBSSettings.editorLoop.set(!BBSSettings.editorLoop.get());
            filmPanel.getContext().notifyInfo(UIKeys.CAMERA_EDITOR_KEYS_LOOPING_TOGGLE_NOTIFICATION);
        }, editorActive);
        bindShortcut(toolbar, Keys.UNDO, filmPanel::undo, filmLoaded);
        bindShortcut(toolbar, Keys.REDO, filmPanel::redo, filmLoaded);
    }

    /* Clip timeline instant actions */

    private static void wireClipsSelection(UIClips clips, TimelineToolbar toolbar)
    {
        BooleanSupplier canUse = () -> clips.canUseToolbarKeybinds();
        BooleanSupplier hasSelection = () -> clips.getDelegateClip() != null && canUse.getAsBoolean();

        bindShortcut(toolbar, Keys.DESELECT, clips::toolbarDeselectAll, hasSelection);
        bindShortcut(toolbar, Keys.CLIP_SELECT_BEFORE, clips::toolbarSelectBefore, canUse);
        bindShortcut(toolbar, Keys.CLIP_SELECT_AFTER, clips::toolbarSelectAfter, canUse);
    }

    private static void wireClipsEditInstant(UIClips clips, TimelineToolbar toolbar)
    {
        BooleanSupplier canUse = () -> clips.canUseToolbarKeybinds();
        BooleanSupplier hasSelection = () -> canUse.getAsBoolean() && !clips.getSelection().isEmpty();

        bindShortcut(toolbar, Keys.CLIP_ENABLE, clips::toolbarToggleEnabled, hasSelection);
    }

    private static void wireClipsEditSelection(UIClips clips, TimelineToolbar toolbar)
    {
        BooleanSupplier canUse = () -> clips.canUseToolbarKeybinds();
        BooleanSupplier hasDelegateClip = () -> clips.getDelegateClip() != null && canUse.getAsBoolean();
        BooleanSupplier hasSelection = () -> canUse.getAsBoolean() && !clips.getSelection().isEmpty();

        bindShortcut(toolbar, Keys.COPY, clips::toolbarCopy, hasDelegateClip);
        bindShortcut(toolbar, Keys.CUT, clips::toolbarCut, hasDelegateClip);
        bindShortcut(toolbar, Keys.DELETE, clips::toolbarRemoveSelected, hasSelection);
        bindShortcut(toolbar, Keys.CLIP_CUT, clips::toolbarCutClipAtCursor, hasDelegateClip);
        bindShortcut(toolbar, Keys.CLIP_SHIFT, clips::toolbarShiftToCursor, hasSelection);
        bindShortcut(toolbar, Keys.CLIP_DURATION, clips::toolbarShiftDurationToCursor, hasSelection);
    }

    private static void wireClipsTransform(UIClips clips, TimelineToolbar toolbar)
    {
        BooleanSupplier hasDelegateClip = () -> clips.getDelegateClip() != null && clips.canUseToolbarKeybinds();

        bindShortcut(toolbar, Keys.FADE_IN, clips::toolbarFadeIn, hasDelegateClip);
        bindShortcut(toolbar, Keys.FADE_OUT, clips::toolbarFadeOut, hasDelegateClip);
    }

    /* Keyframe timeline instant actions */

    private static void wireKeyframesInstant(UIKeyframes keyframes, TimelineToolbar toolbar)
    {
        BooleanSupplier canModify = keyframes::isModifyingKeyframes;

        bindShortcut(toolbar, Keys.KEYFRAMES_SELECT_ALL, () -> keyframes.getGraph().selectAll(), canModify);
        bindShortcut(toolbar, Keys.KEYFRAMES_MAXIMIZE, keyframes::resetView, canModify);
    }

    private static void wireKeyframesInstant(UIReplaysEditor editor, TimelineToolbar toolbar)
    {
        BooleanSupplier hasEditor = () -> editor.keyframeEditor != null;
        BooleanSupplier canModify = () -> hasEditor.getAsBoolean()
            && editor.keyframeEditor.view.isModifyingKeyframes();

        bindShortcut(toolbar, Keys.KEYFRAMES_SELECT_ALL, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.getGraph().selectAll();
            }
        }, canModify);
        bindShortcut(toolbar, Keys.KEYFRAMES_MAXIMIZE, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.resetView();
            }
        }, hasEditor);
    }

    private static void wireKeyframesEditSelection(UIKeyframes keyframes, TimelineToolbar toolbar)
    {
        BooleanSupplier canModify = keyframes::isModifyingKeyframes;
        BooleanSupplier hasSelected = () -> canModify.getAsBoolean() && keyframes.hasSelectedKeyframes();

        bindShortcut(toolbar, Keys.COPY, keyframes::toolbarCopy, canModify);
        bindShortcut(toolbar, Keys.CUT, keyframes::toolbarCut, hasSelected);
        bindShortcut(toolbar, Keys.DELETE, keyframes::toolbarRemoveSelected, hasSelected);
        bindLabel(toolbar, UIKeys.KEYFRAMES_CONTEXT_ROUND, keyframes::toolbarRoundSelectedTicks, hasSelected);
    }

    private static void wireKeyframesEditSelection(UIReplaysEditor editor, TimelineToolbar toolbar)
    {
        BooleanSupplier hasEditor = () -> editor.keyframeEditor != null;
        BooleanSupplier canModify = () -> hasEditor.getAsBoolean()
            && editor.keyframeEditor.view.isModifyingKeyframes();
        BooleanSupplier hasSelected = () -> canModify.getAsBoolean()
            && editor.keyframeEditor.view.hasSelectedKeyframes();

        bindShortcut(toolbar, Keys.COPY, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarCopy();
            }
        }, canModify);
        bindShortcut(toolbar, Keys.CUT, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarCut();
            }
        }, hasSelected);
        bindShortcut(toolbar, Keys.DELETE, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarRemoveSelected();
            }
        }, hasSelected);
        bindLabel(toolbar, UIKeys.KEYFRAMES_CONTEXT_ROUND, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarRoundSelectedTicks();
            }
        }, hasSelected);
    }

    private static void wireKeyframesSelect(UIKeyframes keyframes, TimelineToolbar toolbar)
    {
        BooleanSupplier canModify = keyframes::isModifyingKeyframes;

        bindShortcut(toolbar, Keys.KEYFRAMES_SELECT_LEFT, keyframes::toolbarSelectLeft, canModify);
        bindShortcut(toolbar, Keys.KEYFRAMES_SELECT_RIGHT, keyframes::toolbarSelectRight, canModify);
        bindShortcut(toolbar, Keys.KEYFRAMES_SELECT_SAME, keyframes::toolbarSelectSame, canModify);
        bindShortcut(toolbar, Keys.KEYFRAMES_SELECT_PREV, keyframes::toolbarSelectPrevKeyframe, canModify);
        bindShortcut(toolbar, Keys.KEYFRAMES_SELECT_NEXT, keyframes::toolbarSelectNextKeyframe, canModify);
    }

    private static void wireKeyframesSelect(UIReplaysEditor editor, TimelineToolbar toolbar)
    {
        BooleanSupplier canModify = () -> editor.keyframeEditor != null
            && editor.keyframeEditor.view.isModifyingKeyframes();

        bindShortcut(toolbar, Keys.KEYFRAMES_SELECT_LEFT, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarSelectLeft();
            }
        }, canModify);
        bindShortcut(toolbar, Keys.KEYFRAMES_SELECT_RIGHT, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarSelectRight();
            }
        }, canModify);
        bindShortcut(toolbar, Keys.KEYFRAMES_SELECT_SAME, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarSelectSame();
            }
        }, canModify);
        bindShortcut(toolbar, Keys.KEYFRAMES_SELECT_PREV, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarSelectPrevKeyframe();
            }
        }, canModify);
        bindShortcut(toolbar, Keys.KEYFRAMES_SELECT_NEXT, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarSelectNextKeyframe();
            }
        }, canModify);
    }

    private static void wireKeyframesGraphSelection(UIKeyframes keyframes, TimelineToolbar toolbar)
    {
        BooleanSupplier hasSelected = () -> keyframes.isModifyingKeyframes() && keyframes.hasSelectedKeyframes();
        BooleanSupplier canSpread = keyframes::canSpreadSelectedKeyframes;
        BooleanSupplier canAdjust = keyframes::canAdjustSelectedValues;

        bindLabel(toolbar, UIKeys.KEYFRAMES_CONTEXT_INTERPOLATION_LINEAR,
            () -> keyframes.toolbarApplyInterpolation(Interpolations.LINEAR), hasSelected);
        bindLabel(toolbar, UIKeys.KEYFRAMES_CONTEXT_INTERPOLATION_BEZIER,
            () -> keyframes.toolbarApplyInterpolation(Interpolations.BEZIER), hasSelected);
        bindLabel(toolbar, UIKeys.KEYFRAMES_CONTEXT_INTERPOLATION_HERMITE,
            () -> keyframes.toolbarApplyInterpolation(Interpolations.HERMITE), hasSelected);
        bindLabel(toolbar, UIKeys.KEYFRAMES_CONTEXT_INTERPOLATION_STEP,
            () -> keyframes.toolbarApplyInterpolation(Interpolations.STEP), hasSelected);
        bindShortcut(toolbar, Keys.KEYFRAMES_SPREAD, keyframes::toolbarSpreadKeyframes, canSpread);
        bindLabel(toolbar, UIKeys.KEYFRAMES_CONTEXT_ADJUST_VALUES_LEFT,
            () -> keyframes.toolbarAdjustValues(false), canAdjust);
        bindLabel(toolbar, UIKeys.KEYFRAMES_CONTEXT_ADJUST_VALUES_RIGHT,
            () -> keyframes.toolbarAdjustValues(true), canAdjust);
    }

    private static void wireKeyframesGraphSelection(UIReplaysEditor editor, TimelineToolbar toolbar)
    {
        BooleanSupplier hasSelected = () -> editor.keyframeEditor != null
            && editor.keyframeEditor.view.isModifyingKeyframes()
            && editor.keyframeEditor.view.hasSelectedKeyframes();
        BooleanSupplier canSpread = () -> editor.keyframeEditor != null
            && editor.keyframeEditor.view.canSpreadSelectedKeyframes();
        BooleanSupplier canAdjust = () -> editor.keyframeEditor != null
            && editor.keyframeEditor.view.canAdjustSelectedValues();

        bindLabel(toolbar, UIKeys.KEYFRAMES_CONTEXT_INTERPOLATION_LINEAR, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarApplyInterpolation(Interpolations.LINEAR);
            }
        }, hasSelected);
        bindLabel(toolbar, UIKeys.KEYFRAMES_CONTEXT_INTERPOLATION_BEZIER, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarApplyInterpolation(Interpolations.BEZIER);
            }
        }, hasSelected);
        bindLabel(toolbar, UIKeys.KEYFRAMES_CONTEXT_INTERPOLATION_HERMITE, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarApplyInterpolation(Interpolations.HERMITE);
            }
        }, hasSelected);
        bindLabel(toolbar, UIKeys.KEYFRAMES_CONTEXT_INTERPOLATION_STEP, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarApplyInterpolation(Interpolations.STEP);
            }
        }, hasSelected);
        bindShortcut(toolbar, Keys.KEYFRAMES_SPREAD, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarSpreadKeyframes();
            }
        }, canSpread);
        bindLabel(toolbar, UIKeys.KEYFRAMES_CONTEXT_ADJUST_VALUES_LEFT, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarAdjustValues(false);
            }
        }, canAdjust);
        bindLabel(toolbar, UIKeys.KEYFRAMES_CONTEXT_ADJUST_VALUES_RIGHT, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.toolbarAdjustValues(true);
            }
        }, canAdjust);
    }

    private static void wireTracksInstant(UIKeyframes keyframes, TimelineToolbar toolbar)
    {
        bindLabel(toolbar, UIKeys.KEYFRAMES_CONTEXT_EXIT_TRACK, () -> keyframes.editSheet(null),
            keyframes::isEditing);
    }

    private static void wireTracksInstant(UIReplaysEditor editor, TimelineToolbar toolbar)
    {
        bindLabel(toolbar, UIKeys.KEYFRAMES_CONTEXT_EXIT_TRACK, () ->
        {
            if (editor.keyframeEditor != null)
            {
                editor.keyframeEditor.view.editSheet(null);
            }
        }, () -> editor.keyframeEditor != null && editor.keyframeEditor.view.isEditing());
    }

    /* Item lookup + binding helpers */

    private static void bindShortcut(TimelineToolbar toolbar, KeyCombo combo, Runnable runnable,
        BooleanSupplier enabled)
    {
        apply(findShortcutInSections(toolbar.getSections(), combo), runnable, enabled);
    }

    private static void bindLabel(TimelineToolbar toolbar, IKey label, Runnable runnable,
        BooleanSupplier enabled)
    {
        apply(findLabelInSections(toolbar.getSections(), label), runnable, enabled);
    }

    private static void apply(ToolbarItem item, Runnable runnable, BooleanSupplier enabled)
    {
        if (item == null)
        {
            return;
        }

        item.run(runnable);

        if (enabled != null)
        {
            item.enabledIf(enabled);
        }
    }

    private static ToolbarItem findShortcutInSections(List<ToolbarSection> sections, KeyCombo combo)
    {
        for (ToolbarSection section : sections)
        {
            ToolbarItem item = findShortcutInItems(section.items, combo);

            if (item != null)
            {
                return item;
            }
        }

        return null;
    }

    private static ToolbarItem findShortcutInItems(List<ToolbarItem> items, KeyCombo combo)
    {
        for (ToolbarItem item : items)
        {
            if (item.separator)
            {
                continue;
            }

            if (item.keyCombo == combo)
            {
                return item;
            }

            if (!item.children.isEmpty())
            {
                ToolbarItem nested = findShortcutInItems(item.children, combo);

                if (nested != null)
                {
                    return nested;
                }
            }
        }

        return null;
    }

    private static ToolbarItem findLabelInSections(List<ToolbarSection> sections, IKey label)
    {
        for (ToolbarSection section : sections)
        {
            ToolbarItem item = findLabelInItems(section.items, label);

            if (item != null)
            {
                return item;
            }
        }

        return null;
    }

    private static ToolbarItem findLabelInItems(List<ToolbarItem> items, IKey label)
    {
        for (ToolbarItem item : items)
        {
            if (item.separator)
            {
                continue;
            }

            if (item.label == label)
            {
                return item;
            }

            if (!item.children.isEmpty())
            {
                ToolbarItem nested = findLabelInItems(item.children, label);

                if (nested != null)
                {
                    return nested;
                }
            }
        }

        return null;
    }

    private TimelineToolbarWiring()
    {}
}
