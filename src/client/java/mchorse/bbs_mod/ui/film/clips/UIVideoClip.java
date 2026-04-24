package mchorse.bbs_mod.ui.film.clips;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.camera.clips.misc.VideoClip;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIVideoOverlayPanel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icons;

public class UIVideoClip extends UIClip<VideoClip>
{
    public UIButton pickVideo;
    public UIIcon openFolder;
    public UITrackpad offset;
    public UITrackpad volume;
    public UITrackpad x;
    public UITrackpad y;
    public UITrackpad width;
    public UITrackpad height;
    public UITrackpad opacity;
    public UIToggle loops;
    public UIToggle global;

    public UIVideoClip(VideoClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.pickVideo = new UIButton(UIKeys.CAMERA_PANELS_VIDEO_PICK_VIDEO, (b) ->
        {
            UIVideoOverlayPanel panel = new UIVideoOverlayPanel((value) -> this.clip.video.set(value), this.getContext());

            UIOverlay.addOverlay(this.getContext(), panel.set(this.clip.video.get()));
        });

        this.openFolder = new UIIcon(Icons.FOLDER, (b) ->
        {
            String videoPath = this.clip.video.get();

            if (videoPath != null && !videoPath.isEmpty())
            {
                if (videoPath.startsWith("external:"))
                {
                    String rawPath = videoPath.substring("external:".length());
                    java.io.File file = new java.io.File(rawPath);

                    if (!file.isAbsolute())
                    {
                        file = new java.io.File(BBSMod.getGameFolder(), rawPath);
                    }

                    if (file.isDirectory())
                    {
                        UIUtils.openFolder(file);
                        return;
                    }
                    else if (file.exists())
                    {
                        UIUtils.openFolder(file.getParentFile());
                        return;
                    }
                }
                else
                {
                    Link link = Link.create(videoPath);
                    java.io.File file = BBSMod.getProvider().getFile(link);

                    if (file != null && file.exists())
                    {
                        UIUtils.openFolder(file.getParentFile());
                        return;
                    }
                }
            }

            UIUtils.openFolder(BBSMod.getAssetsPath("video"));
        });

        this.offset = new UITrackpad((v) -> this.clip.offset.set(v.intValue()));
         this.offset.integer();
        this.offset.tooltip(UIKeys.CAMERA_PANELS_VIDEO_OFFSET);

        this.volume = new UITrackpad((v) -> this.clip.volume.set(v.intValue()));
        this.volume.integer();
        this.volume.limit(0, 100);
        this.volume.tooltip(UIKeys.CAMERA_PANELS_VIDEO_VOLUME);

        this.x = new UITrackpad((v) -> this.clip.x.set(v.intValue()));
        this.x.integer();
        this.x.tooltip(UIKeys.C_CLIP.get("bbs:x"));

        this.y = new UITrackpad((v) -> this.clip.y.set(v.intValue()));
        this.y.integer();
        this.y.tooltip(UIKeys.C_CLIP.get("bbs:y"));

        this.width = new UITrackpad((v) -> this.clip.width.set(v.intValue()));
        this.width.integer();
        this.width.tooltip(UIKeys.C_CLIP.get("bbs:width"));

        this.height = new UITrackpad((v) -> this.clip.height.set(v.intValue()));
        this.height.integer();
        this.height.tooltip(UIKeys.C_CLIP.get("bbs:height"));

        this.opacity = new UITrackpad((v) -> this.clip.opacity.set(v.floatValue()));
        this.opacity.limit(0.0F, 1.0F);
        this.opacity.tooltip(UIKeys.C_CLIP.get("bbs:opacity"));

        this.loops = new UIToggle(UIKeys.C_CLIP.get("bbs:loops"), (b) -> this.clip.loops.set(b.getValue()));
        this.global = new UIToggle(UIKeys.C_CLIP.get("bbs:global"), (b) -> this.clip.global.set(b.getValue()));
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(UI.column(UIClip.label(UIKeys.C_CLIP.get("bbs:video")), UI.row(this.pickVideo, this.openFolder)).marginTop(12));
        this.panels.add(UI.column(UIClip.label(UIKeys.CAMERA_PANELS_VIDEO_OFFSET).marginTop(6), this.offset).marginTop(12));
        this.panels.add(UI.column(UIClip.label(UIKeys.CAMERA_PANELS_VIDEO_VOLUME).marginTop(6), this.volume).marginTop(12));
        this.panels.add(UI.row(
            UI.column(UIClip.label(UIKeys.C_CLIP.get("bbs:x")), this.x),
            UI.column(UIClip.label(UIKeys.C_CLIP.get("bbs:y")), this.y)
        ).marginTop(12));
        this.panels.add(UI.row(
            UI.column(UIClip.label(UIKeys.C_CLIP.get("bbs:width")), this.width),
            UI.column(UIClip.label(UIKeys.C_CLIP.get("bbs:height")), this.height)
        ).marginTop(12));
        this.panels.add(UI.column(UIClip.label(UIKeys.C_CLIP.get("bbs:opacity")).marginTop(6), this.opacity).marginTop(12));
        this.panels.add(UI.row(this.loops, this.global).marginTop(12));
    }

    @Override
    public void fillData()
    {
        super.fillData();

        this.offset.setValue(this.clip.offset.get());
        this.volume.setValue(this.clip.volume.get());
        this.x.setValue(this.clip.x.get());
        this.y.setValue(this.clip.y.get());
        this.width.setValue(this.clip.width.get());
        this.height.setValue(this.clip.height.get());
        this.opacity.setValue(this.clip.opacity.get());
        this.loops.setValue(this.clip.loops.get());
        this.global.setValue(this.clip.global.get());
    }
}
