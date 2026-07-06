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
    }

    /* Keyframe toolbar (embedded in clip panel or replay editor) */

    public static void wireKeyframesToolbar(UIFilmPanel filmPanel, UIKeyframes keyframes, TimelineToolbar toolbar)
    {
        wireTransportAndHistory(filmPanel, toolbar);
        wireKeyframesInstant(keyframes, toolbar);
        wireTracksInstant(keyframes, toolbar);
    }

    public static void wireReplaysToolbar(UIReplaysEditor editor)
    {
        wireTransportAndHistory(editor.getFilmPanel(), editor.toolbar);
        wireKeyframesInstant(editor, editor.toolbar);
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
    }

    private static void wireClipsEditInstant(UIClips clips, TimelineToolbar toolbar)
    {
        BooleanSupplier canUse = () -> clips.canUseToolbarKeybinds();
        BooleanSupplier hasSelection = () -> canUse.getAsBoolean() && !clips.getSelection().isEmpty();

        bindShortcut(toolbar, Keys.CLIP_ENABLE, clips::toolbarToggleEnabled, hasSelection);
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
