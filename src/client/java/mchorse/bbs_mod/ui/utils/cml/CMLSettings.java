package mchorse.bbs_mod.ui.utils.cml;

import mchorse.bbs_mod.BBS;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.settings.SettingsBuilder;
import mchorse.bbs_mod.ui.UIKeys;

public class CMLSettings
{
    public static void register(SettingsBuilder builder)
    {
        builder.category("general");
        BBSSettings.modelFormsHierarchy = builder.getBoolean("model_forms_hierarchy", false);
        BBSSettings.mediaFoldersEnhancements = builder.getBoolean("media_folders_enhancements", false);
        BBSSettings.replayContextOptions = builder.getInt("compacted_options", 0, 0, 2);
        BBSSettings.replayContextOptions.modes(
            UIKeys.CONFIG_GENERAL_COMPACTED_OPTIONS_DEFAULT,
            UIKeys.CONFIG_GENERAL_COMPACTED_OPTIONS_SEPARATED,
            UIKeys.CONFIG_GENERAL_COMPACTED_OPTIONS_COMPACTED
        );
        BBSSettings.editorDockGuideColor = builder.getInt("dock_guide_color", 0x57CCFF).color();
        BBSSettings.editorDockGuideOpacity = builder.getFloat("dock_guide_opacity", 0.5F, 0F, 1F);
        builder.category("appearance");
        BBSSettings.disablePivotTransform = builder.getBoolean("disable_pivot_transform", false);
        BBSSettings.gizmoYAxisHorizontal = builder.getBoolean("gizmo_y_axis_horizontal", true);
        BBSSettings.pickLimbTexture = builder.getBoolean("pick_limb_texture", true);
        BBSSettings.limbTracks = builder.getBoolean("limb_tracks", true);
        BBSSettings.originalKeyframeUI = builder.getBoolean("original_keyframe_ui", false);
        BBSSettings.simplifiedKeyframeUI = builder.getBoolean("simplified_keyframe_ui", false);
        BBSSettings.defaultInterpolation = builder.getInt("default_interpolation", 0);
        builder.category("editor");
        BBSSettings.editorSafeMarginsColor = builder.getInt("safe_margins_color", 0xcccc0000).colorAlpha();
        BBSSettings.editorSafeMargins = builder.getBoolean("safe_margins", false);
        BBSSettings.editorFlightFreeLook = builder.getBoolean("flight_free_look", false);
        BBSSettings.editorClipTypeLabels = builder.getBoolean("clip_type_labels", false);
        BBSSettings.editorReplaySprintParticles = builder.getBoolean("replay_sprint_particles", false);
        BBSSettings.editorCameraPreviewPlayerSync = builder.getBoolean("camera_preview_player_sync", false);
        BBSSettings.recordingCameraPreviewFutureCount = builder.getInt("camera_preview_future_count", 3, 1, 8);
        BBSSettings.editorReplayStepSound = builder.getBoolean("replay_step_sound", true);
        BBSSettings.editorMuteRenderAudioClips = builder.getBoolean("mute_render_audio_clips", false);
        BBSSettings.editorTimeMode = builder.getInt("time_mode", 0, 0, 2);
        BBSSettings.editorTimeMode.modes(
            UIKeys.CONFIG_EDITOR_TICKS_MODE,
            UIKeys.CONFIG_EDITOR_SECONDS_MODE,
            UIKeys.CONFIG_EDITOR_FRAMES_MODE
        );
        BBSSettings.editorTimeMode.postCallback((changed, flag) ->
        {
            int mode = BBSSettings.editorTimeMode.get();

            if (mode == 0)
            {
                if (BBSSettings.editorSeconds.get()) BBSSettings.editorSeconds.set(false);
                if (BBSSettings.editorFrames.get()) BBSSettings.editorFrames.set(false);
            }
            else if (mode == 1)
            {
                if (!BBSSettings.editorSeconds.get()) BBSSettings.editorSeconds.set(true);
                if (BBSSettings.editorFrames.get()) BBSSettings.editorFrames.set(false);
            }
            else
            {
                if (BBSSettings.editorSeconds.get()) BBSSettings.editorSeconds.set(false);
                if (!BBSSettings.editorFrames.get()) BBSSettings.editorFrames.set(true);
            }
        });

        BBSSettings.editorSeconds.postCallback((changed, flag) ->
        {
            if (BBSSettings.editorSeconds.get())
            {
                if (BBSSettings.editorTimeMode.get() != 1) BBSSettings.editorTimeMode.set(1);
                if (BBSSettings.editorFrames.get()) BBSSettings.editorFrames.set(false);
            }
            else if (!BBSSettings.editorFrames.get())
            {
                if (BBSSettings.editorTimeMode.get() != 0) BBSSettings.editorTimeMode.set(0);
            }
        });
        BBSSettings.editorFrames.postCallback((changed, flag) ->
        {
            if (BBSSettings.editorFrames.get())
            {
                if (BBSSettings.editorTimeMode.get() != 2) BBSSettings.editorTimeMode.set(2);
                if (BBSSettings.editorSeconds.get()) BBSSettings.editorSeconds.set(false);
            }
            else if (!BBSSettings.editorSeconds.get())
            {
                if (BBSSettings.editorTimeMode.get() != 0) BBSSettings.editorTimeMode.set(0);
            }
        });
        builder.category("display");
        BBSSettings.editorReplayHud = builder.getBoolean("replay_hud", false);
        BBSSettings.editorReplayHudPosition = builder.getInt("replay_hud_position", 0, 0, 3);
        BBSSettings.editorReplayHudDisplayName = builder.getBoolean("replay_hud_display_name", true);
        builder.category("fluid_simulation");
        BBSSettings.fluidRealisticModelInteraction = builder.getBoolean("realistic_model_interaction", false);
        builder.category("model_blocks");
        BBSSettings.modelBlockCategoriesPanelEnabled = builder.getBoolean("categories_panel_enabled", false);
        BBSSettings.modelPbrPanelControls = builder.getBoolean("model_pbr_panel_controls", false);
        builder.category("pose_track_selection");
        BBSSettings.boneAnchoringEnabled = builder.getBoolean("bone_anchoring_enabled", true);
        BBSSettings.anchorOverrideEnabled = builder.getBoolean("anchor_override_enabled", false);
        BBSSettings.autoKeyframe = builder.getBoolean("auto_keyframe", false);
        BBSSettings.poseBonesFilterMarked = builder.getBoolean("pose_bones_filter_marked", false);
        BBSSettings.poseBonesFilterMarked.invisible();
        BBSSettings.replayMarkedBonesOnly = builder.getBoolean("replay_marked_bones_only", false);
        builder.category("replay_editor");
        BBSSettings.editorReplayEditorTitleLimit = builder.getInt("replay_editor_title_limit", 12, 0, 64);
    }
}
