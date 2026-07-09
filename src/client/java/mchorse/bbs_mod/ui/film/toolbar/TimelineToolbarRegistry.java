package mchorse.bbs_mod.ui.film.toolbar;

import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.utils.icons.Icons;

import java.util.ArrayList;
import java.util.List;

/**
 * Static definitions of the toolbar hierarchies for each of the three
 * timelines. Phase 1 only wires the section tree with icons, labels and
 * keyboard shortcuts. {@link TimelineToolbarWiring} plugs runnables and
 * enabled conditions in during Phase 2.
 *
 * <p>All lists are returned as fresh copies so integration code can mount the
 * toolbar directly without worrying about shared mutable state.</p>
 */
public final class TimelineToolbarRegistry
{
    /* Camera / Action clip timelines */

    public static List<ToolbarSection> forClipsCamera()
    {
        List<ToolbarSection> sections = new ArrayList<>();

        sections.add(transportSection(true, true));
        sections.add(addSectionCamera());
        sections.add(editSectionClips(true));
        sections.add(selectSectionClips());
        sections.add(transformSectionClips());
        sections.add(historySection());

        return sections;
    }

    public static List<ToolbarSection> forClipsAction()
    {
        List<ToolbarSection> sections = new ArrayList<>();

        sections.add(transportSection(false, true));
        sections.add(addSectionAction());
        sections.add(editSectionClips(false));
        sections.add(selectSectionClips());
        sections.add(transformSectionClips());
        sections.add(historySection());

        return sections;
    }

    /* Replay / keyframe timeline (no loop submenu: `[` / `]` are keyframe prev/next) */

    public static List<ToolbarSection> forReplays(boolean showActor)
    {
        List<ToolbarSection> sections = new ArrayList<>();

        sections.add(transportSection(true, false));
        sections.add(addSectionKeyframes());
        sections.add(editSectionKeyframes());
        sections.add(selectSectionKeyframes());
        sections.add(keyframesSection());
        sections.add(tracksSection());

        if (showActor)
        {
            sections.add(actorSection());
        }

        sections.add(historySection());

        return sections;
    }

    /* Sections shared across timelines */

    private static ToolbarSection transportSection(boolean includeClipNavigation, boolean includeLooping)
    {
        ToolbarSection s = new ToolbarSection(UIKeys.TIMELINE_TOOLBAR_TRANSPORT, Icons.PLAY);

        s.add(ToolbarItem.action(UIKeys.CAMERA_EDITOR_KEYS_EDITOR_PLAUSE)
            .icon(Icons.PLAY)
            .shortcut(Keys.PLAUSE));
        s.add(ToolbarItem.separator());
        s.add(ToolbarItem.action(UIKeys.CAMERA_EDITOR_KEYS_EDITOR_NEXT)
            .icon(Icons.FRAME_NEXT)
            .shortcut(Keys.NEXT));
        s.add(ToolbarItem.action(UIKeys.CAMERA_EDITOR_KEYS_EDITOR_PREV)
            .icon(Icons.FRAME_PREV)
            .shortcut(Keys.PREV));
        s.add(ToolbarItem.action(UIKeys.CAMERA_EDITOR_KEYS_EDITOR_JUMP_FORWARD)
            .icon(Icons.SHIFT_FORWARD)
            .shortcut(Keys.JUMP_FORWARD));
        s.add(ToolbarItem.action(UIKeys.CAMERA_EDITOR_KEYS_EDITOR_JUMP_BACKWARD)
            .icon(Icons.SHIFT_BACKWARD)
            .shortcut(Keys.JUMP_BACKWARD));

        if (includeClipNavigation)
        {
            s.add(ToolbarItem.separator());
            s.add(ToolbarItem.action(UIKeys.CAMERA_EDITOR_KEYS_EDITOR_NEXT_CLIP)
                .icon(Icons.FORWARD)
                .shortcut(Keys.NEXT_CLIP));
            s.add(ToolbarItem.action(UIKeys.CAMERA_EDITOR_KEYS_EDITOR_PREV_CLIP)
                .icon(Icons.BACKWARD)
                .shortcut(Keys.PREV_CLIP));
        }

        if (includeLooping)
        {
            s.add(ToolbarItem.separator());
            s.add(ToolbarItem.submenu(UIKeys.CAMERA_EDITOR_KEYS_MODES_LOOPING,
                ToolbarItem.action(UIKeys.CAMERA_EDITOR_KEYS_LOOPING_SET_MIN)
                    .icon(Icons.IN)
                    .shortcut(Keys.LOOPING_SET_MIN),
                ToolbarItem.action(UIKeys.CAMERA_EDITOR_KEYS_LOOPING_SET_MAX)
                    .icon(Icons.OUT)
                    .shortcut(Keys.LOOPING_SET_MAX),
                ToolbarItem.action(UIKeys.CAMERA_EDITOR_KEYS_MODES_LOOPING)
                    .shortcut(Keys.LOOPING)
            ).icon(Icons.REVERSE));
        }

        return s;
    }

    private static ToolbarSection historySection()
    {
        ToolbarSection s = new ToolbarSection(UIKeys.TIMELINE_TOOLBAR_HISTORY, Icons.UNDO);

        s.add(ToolbarItem.action(UIKeys.CAMERA_EDITOR_KEYS_EDITOR_UNDO)
            .icon(Icons.UNDO)
            .shortcut(Keys.UNDO));
        s.add(ToolbarItem.action(UIKeys.CAMERA_EDITOR_KEYS_EDITOR_REDO)
            .icon(Icons.REDO)
            .shortcut(Keys.REDO));

        return s;
    }

    /* Add / Edit / Select / Transform for clip timelines */

    private static ToolbarSection addSectionCamera()
    {
        ToolbarSection s = new ToolbarSection(UIKeys.TIMELINE_TOOLBAR_ADD, Icons.ADD);

        s.add(ToolbarItem.action(UIKeys.CAMERA_TIMELINE_CONTEXT_ADD_AT_CURSOR)
            .icon(Icons.ADD)
            .shortcut(Keys.ADD_AT_CURSOR));
        s.add(ToolbarItem.action(UIKeys.CAMERA_TIMELINE_CONTEXT_ADD_AT_TICK)
            .icon(Icons.ADD)
            .shortcut(Keys.ADD_AT_TICK));
        s.add(ToolbarItem.action(UIKeys.CAMERA_TIMELINE_CONTEXT_ADD_ON_TOP)
            .icon(Icons.ADD)
            .shortcut(Keys.ADD_ON_TOP));
        s.add(ToolbarItem.separator());
        s.add(ToolbarItem.submenu(UIKeys.CAMERA_TIMELINE_CLIPS_TABS_CAMERA).icon(Icons.CAMERA));
        s.add(ToolbarItem.submenu(UIKeys.CAMERA_TIMELINE_CLIPS_TABS_RESOURCE).icon(Icons.FOLDER));
        s.add(ToolbarItem.submenu(UIKeys.CAMERA_TIMELINE_CLIPS_TABS_SCREEN).icon(Icons.CONSOLE));
        s.add(ToolbarItem.submenu(UIKeys.CAMERA_TIMELINE_CLIPS_TABS_ANCHOR).icon(Icons.ORBIT));
        s.add(ToolbarItem.submenu(UIKeys.CAMERA_TIMELINE_CLIPS_TABS_EXTRAS).icon(Icons.MORE));
        s.add(ToolbarItem.separator());
        s.add(ToolbarItem.submenu(UIKeys.CAMERA_TIMELINE_CONTEXT_FROM_PLAYER_RECORDING).icon(Icons.PLAYER));
        s.add(ToolbarItem.action(UIKeys.CAMERA_TIMELINE_CONTEXT_RECORD_MICROPHONE)
            .icon(Icons.SOUND));

        return s;
    }

    private static ToolbarSection addSectionAction()
    {
        ToolbarSection s = new ToolbarSection(UIKeys.TIMELINE_TOOLBAR_ADD, Icons.ADD);

        s.add(ToolbarItem.action(UIKeys.CAMERA_TIMELINE_CONTEXT_ADD_AT_CURSOR)
            .icon(Icons.ADD)
            .shortcut(Keys.ADD_AT_CURSOR));
        s.add(ToolbarItem.action(UIKeys.CAMERA_TIMELINE_CONTEXT_ADD_AT_TICK)
            .icon(Icons.ADD)
            .shortcut(Keys.ADD_AT_TICK));
        s.add(ToolbarItem.separator());
        s.add(ToolbarItem.submenu(UIKeys.ACTION_TIMELINE_CLIPS_TABS_BLOCKS).icon(Icons.BLOCK));
        s.add(ToolbarItem.submenu(UIKeys.ACTION_TIMELINE_CLIPS_TABS_ITEMS).icon(Icons.POINTER));
        s.add(ToolbarItem.submenu(UIKeys.ACTION_TIMELINE_CLIPS_TABS_COMBAT).icon(Icons.DROP));
        s.add(ToolbarItem.submenu(UIKeys.ACTION_TIMELINE_CLIPS_TABS_OTHER).icon(Icons.MORE));

        return s;
    }

    private static ToolbarSection editSectionClips(boolean isCamera)
    {
        ToolbarSection s = new ToolbarSection(UIKeys.TIMELINE_TOOLBAR_EDIT, Icons.EDIT);

        s.add(ToolbarItem.action(UIKeys.CAMERA_TIMELINE_CONTEXT_COPY)
            .icon(Icons.COPY)
            .shortcut(Keys.COPY));
        s.add(ToolbarItem.action(UIKeys.CAMERA_TIMELINE_CONTEXT_CUT)
            .icon(Icons.CUT)
            .shortcut(Keys.CUT));
        s.add(ToolbarItem.action(UIKeys.CAMERA_TIMELINE_CONTEXT_PASTE)
            .icon(Icons.PASTE)
            .shortcut(Keys.PASTE));
        s.add(ToolbarItem.action(UIKeys.GENERAL_PRESETS)
            .icon(Icons.MORE)
            .shortcut(Keys.PRESETS));
        s.add(ToolbarItem.separator());
        s.add(ToolbarItem.action(UIKeys.CAMERA_TIMELINE_CONTEXT_CUT)
            .icon(Icons.CUT)
            .shortcut(Keys.CLIP_CUT));
        s.add(ToolbarItem.action(UIKeys.CAMERA_TIMELINE_CONTEXT_SHIFT)
            .shortcut(Keys.CLIP_SHIFT));
        s.add(ToolbarItem.action(UIKeys.CAMERA_TIMELINE_CONTEXT_SHIFT_DURATION)
            .shortcut(Keys.CLIP_DURATION));
        s.add(ToolbarItem.action(UIKeys.CAMERA_TIMELINE_KEYS_ENABLED)
            .shortcut(Keys.CLIP_ENABLE));

        if (isCamera)
        {
            s.add(ToolbarItem.separator());
            s.add(ToolbarItem.submenu(UIKeys.CAMERA_TIMELINE_CONTEXT_CONVERT)
                .icon(Icons.REFRESH));
            s.add(ToolbarItem.action(UIKeys.CAMERA_TIMELINE_CONTEXT_REORGANIZE)
                .icon(Icons.EXCHANGE));
        }

        s.add(ToolbarItem.separator());
        s.add(ToolbarItem.action(UIKeys.CAMERA_TIMELINE_CONTEXT_REMOVE_CLIPS)
            .icon(Icons.TRASH)
            .shortcut(Keys.DELETE)
            .destructive());

        return s;
    }

    private static ToolbarSection selectSectionClips()
    {
        ToolbarSection s = new ToolbarSection(UIKeys.TIMELINE_TOOLBAR_SELECT, Icons.POINTER);

        s.add(ToolbarItem.action(UIKeys.CAMERA_EDITOR_KEYS_CLIPS_DESELECT)
            .shortcut(Keys.DESELECT));
        s.add(ToolbarItem.action(UIKeys.CAMERA_EDITOR_KEYS_EDITOR_SELECT_BEFORE)
            .shortcut(Keys.CLIP_SELECT_BEFORE));
        s.add(ToolbarItem.action(UIKeys.CAMERA_EDITOR_KEYS_EDITOR_SELECT_AFTER)
            .shortcut(Keys.CLIP_SELECT_AFTER));

        return s;
    }

    private static ToolbarSection transformSectionClips()
    {
        ToolbarSection s = new ToolbarSection(UIKeys.TIMELINE_TOOLBAR_TRANSFORM, Icons.SCALE);

        s.add(ToolbarItem.action(UIKeys.CAMERA_EDITOR_KEYS_EDITOR_FADE_IN)
            .shortcut(Keys.FADE_IN));
        s.add(ToolbarItem.action(UIKeys.CAMERA_EDITOR_KEYS_EDITOR_FADE_OUT)
            .shortcut(Keys.FADE_OUT));

        return s;
    }

    /* Sections for keyframe / replay timeline */

    private static ToolbarSection addSectionKeyframes()
    {
        ToolbarSection s = new ToolbarSection(UIKeys.TIMELINE_TOOLBAR_ADD, Icons.ADD);

        ToolbarItem insertKeyframe = ToolbarItem.submenu(UIKeys.FILM_CONTROLLER_KEYS_INSERT_FRAME,
            ToolbarItem.action(UIKeys.KEYFRAMES_INSERT_AT_TIMELINE)
                .icon(Icons.ADD)
                .shortcut(Keys.FILM_CONTROLLER_INSERT_FRAME),
            ToolbarItem.action(UIKeys.KEYFRAMES_INSERT_AT_CURSOR)
                .icon(Icons.CURSOR),
            ToolbarItem.separator(),
            ToolbarItem.action(UIKeys.KEYFRAMES_INSERT_SINGLE_AT_TIMELINE)
                .icon(Icons.ADD),
            ToolbarItem.action(UIKeys.KEYFRAMES_INSERT_SINGLE_AT_CURSOR)
                .icon(Icons.POINTER)
                .shortcut(Keys.KEYFRAMES_INSERT_INDIVIDUAL)
        ).icon(Icons.ADD).shortcut(Keys.FILM_CONTROLLER_INSERT_FRAME);

        s.add(insertKeyframe);
        ToolbarItem duplicateKeyframes = ToolbarItem.submenu(UIKeys.KEYFRAMES_DUPLICATE,
            ToolbarItem.action(UIKeys.KEYFRAMES_DUPLICATE_AT_CURSOR)
                .icon(Icons.CURSOR)
                .shortcut(Keys.KEYFRAMES_DUPLICATE),
            ToolbarItem.action(UIKeys.KEYFRAMES_DUPLICATE_AT_TIMELINE)
                .icon(Icons.ADD)
        ).icon(Icons.COPY).shortcut(Keys.KEYFRAMES_DUPLICATE);

        s.add(duplicateKeyframes);
        s.add(ToolbarItem.separator());
        s.add(ToolbarItem.action(UIKeys.GENERAL_PRESETS)
            .icon(Icons.MORE)
            .shortcut(Keys.PRESETS));

        return s;
    }

    private static ToolbarSection editSectionKeyframes()
    {
        ToolbarSection s = new ToolbarSection(UIKeys.TIMELINE_TOOLBAR_EDIT, Icons.EDIT);

        s.add(ToolbarItem.action(UIKeys.KEYFRAMES_CONTEXT_COPY)
            .icon(Icons.COPY)
            .shortcut(Keys.COPY));
        s.add(ToolbarItem.action(UIKeys.KEYFRAMES_CONTEXT_CUT)
            .icon(Icons.CUT)
            .shortcut(Keys.CUT));
        s.add(ToolbarItem.submenu(UIKeys.KEYFRAMES_CONTEXT_PASTE,
            ToolbarItem.action(UIKeys.KEYFRAMES_CONTEXT_PASTE_AT_CURSOR)
                .icon(Icons.PASTE),
            ToolbarItem.action(UIKeys.KEYFRAMES_CONTEXT_PASTE_AT_TIMELINE)
                .icon(Icons.PASTE)
        ).icon(Icons.PASTE).shortcut(Keys.PASTE));
        s.add(ToolbarItem.separator());
        s.add(ToolbarItem.action(UIKeys.KEYFRAMES_CONTEXT_MAXIMIZE)
            .icon(Icons.MAXIMIZE)
            .shortcut(Keys.KEYFRAMES_MAXIMIZE));
        s.add(ToolbarItem.action(UIKeys.KEYFRAMES_CONTEXT_ROUND)
            .icon(Icons.OUTLINE_SPHERE));
        s.add(ToolbarItem.separator());
        s.add(ToolbarItem.action(UIKeys.KEYFRAMES_CONTEXT_REMOVE)
            .icon(Icons.TRASH)
            .shortcut(Keys.DELETE)
            .destructive());

        return s;
    }

    private static ToolbarSection selectSectionKeyframes()
    {
        ToolbarSection s = new ToolbarSection(UIKeys.TIMELINE_TOOLBAR_SELECT, Icons.POINTER);

        s.add(ToolbarItem.action(UIKeys.KEYFRAMES_CONTEXT_SELECT_ALL)
            .shortcut(Keys.KEYFRAMES_SELECT_ALL));
        s.add(ToolbarItem.action(UIKeys.KEYFRAMES_KEYS_SELECT_LEFT)
            .shortcut(Keys.KEYFRAMES_SELECT_LEFT));
        s.add(ToolbarItem.action(UIKeys.KEYFRAMES_KEYS_SELECT_RIGHT)
            .shortcut(Keys.KEYFRAMES_SELECT_RIGHT));
        s.add(ToolbarItem.submenu(UIKeys.KEYFRAMES_KEYS_SELECT_SAME,
            ToolbarItem.action(UIKeys.KEYFRAMES_KEYS_SELECT_SAME_ALL)
                .shortcut(Keys.KEYFRAMES_SELECT_SAME),
            ToolbarItem.action(UIKeys.KEYFRAMES_KEYS_SELECT_SAME_TRACK)
        ).shortcut(Keys.KEYFRAMES_SELECT_SAME));
        s.add(ToolbarItem.action(UIKeys.KEYFRAMES_KEYS_SELECT_PREV)
            .shortcut(Keys.KEYFRAMES_SELECT_PREV));
        s.add(ToolbarItem.action(UIKeys.KEYFRAMES_KEYS_SELECT_NEXT)
            .shortcut(Keys.KEYFRAMES_SELECT_NEXT));
        s.add(ToolbarItem.separator());
        s.add(ToolbarItem.action(UIKeys.KEYFRAMES_KEYS_SELECT_COLUMN)
            .icon(Icons.FULLSCREEN));

        return s;
    }

    private static ToolbarSection keyframesSection()
    {
        ToolbarSection s = new ToolbarSection(UIKeys.TIMELINE_TOOLBAR_KEYFRAMES, Icons.GRAPH);

        s.add(ToolbarItem.action(UIKeys.KEYFRAMES_KEYS_TOGGLE_INTERP)
            .icon(Icons.CURVES)
            .shortcut(Keys.KEYFRAMES_INTERP));
        s.add(ToolbarItem.submenu(UIKeys.KEYFRAMES_CONTEXT_INTERPOLATION,
            ToolbarItem.action(UIKeys.KEYFRAMES_CONTEXT_INTERPOLATION_LINEAR)
                .icon(Icons.INTERP_LINEAR),
            ToolbarItem.action(UIKeys.KEYFRAMES_CONTEXT_INTERPOLATION_BEZIER)
                .icon(Icons.ARC),
            ToolbarItem.action(UIKeys.KEYFRAMES_CONTEXT_INTERPOLATION_HERMITE)
                .icon(Icons.CURVES),
            ToolbarItem.action(UIKeys.KEYFRAMES_CONTEXT_INTERPOLATION_STEP)
                .icon(Icons.INTERP_STEP)
        ).icon(Icons.CURVES));
        s.add(ToolbarItem.action(UIKeys.KEYFRAMES_KEYS_SCALE_TIME)
            .icon(Icons.SCALE)
            .shortcut(Keys.KEYFRAMES_SCALE_TIME));
        s.add(ToolbarItem.action(UIKeys.KEYFRAMES_KEYS_STACK_KEYFRAMES)
            .icon(Icons.STRUCTURE)
            .shortcut(Keys.KEYFRAMES_STACK_KEYFRAMES));
        s.add(ToolbarItem.action(UIKeys.KEYFRAMES_CONTEXT_SPREAD)
            .shortcut(Keys.KEYFRAMES_SPREAD));
        s.add(ToolbarItem.submenu(UIKeys.KEYFRAMES_CONTEXT_ADJUST_VALUES,
            ToolbarItem.action(UIKeys.KEYFRAMES_CONTEXT_ADJUST_VALUES_LEFT)
                .icon(Icons.ARROW_LEFT),
            ToolbarItem.action(UIKeys.KEYFRAMES_CONTEXT_ADJUST_VALUES_RIGHT)
                .icon(Icons.ARROW_RIGHT)
        ).icon(Icons.SEARCH).shortcut(Keys.KEYFRAMES_ADJUST_VALUES));

        return s;
    }

    private static ToolbarSection tracksSection()
    {
        ToolbarSection s = new ToolbarSection(UIKeys.TIMELINE_TOOLBAR_TRACKS, Icons.LAYOUT);

        s.add(ToolbarItem.action(UIKeys.FILM_REPLAY_RENAME_SHEET)
            .icon(Icons.FONT));
        s.add(ToolbarItem.action(UIKeys.FILM_REPLAY_FILTER_SHEETS)
            .icon(Icons.FILTER));
        s.add(ToolbarItem.separator());
        s.add(ToolbarItem.action(UIKeys.FILM_REPLAY_CONTEXT_ANIMATION_TO_KEYFRAMES)
            .icon(Icons.POSE));
        s.add(ToolbarItem.action(UIKeys.FILM_REPLAY_CONTEXT_POSE_TO_LIMBS)
            .icon(Icons.CONVERT));
        s.add(ToolbarItem.separator());
        s.add(ToolbarItem.action(UIKeys.TIMELINE_TOOLBAR_EDIT_TRACK)
            .icon(Icons.EDIT));
        s.add(ToolbarItem.action(UIKeys.KEYFRAMES_CONTEXT_EXIT_TRACK)
            .icon(Icons.CLOSE));

        return s;
    }

    private static ToolbarSection actorSection()
    {
        ToolbarSection s = new ToolbarSection(UIKeys.TIMELINE_TOOLBAR_ACTOR, Icons.PLAYER);

        s.add(ToolbarItem.action(UIKeys.FILM_REPLAY_CONTEXT_ADD)
            .icon(Icons.ADD));
        s.add(ToolbarItem.action(UIKeys.FILM_REPLAY_CONTEXT_MOVE_HERE)
            .icon(Icons.POINTER));

        return s;
    }

    /* Constructor */

    private TimelineToolbarRegistry()
    {}
}
