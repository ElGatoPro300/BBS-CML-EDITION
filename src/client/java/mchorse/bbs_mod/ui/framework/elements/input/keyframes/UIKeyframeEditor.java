package mchorse.bbs_mod.ui.framework.elements.input.keyframes;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.camera.clips.overwrite.KeyframeClip;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UIPoseKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories.UITransformKeyframeFactory;
import mchorse.bbs_mod.ui.framework.elements.utils.UIDraggable;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class UIKeyframeEditor extends UIElement
{
    public static final int[] COLORS = {Colors.RED, Colors.GREEN, Colors.BLUE, Colors.CYAN, Colors.MAGENTA, Colors.YELLOW, Colors.LIGHTEST_GRAY & 0xffffff, Colors.DEEP_PINK};
    private static final int SIDE_PANEL_WIDTH = 140;
    private static final int SIDE_PANEL_WIDTH_MIN = 90;
    private static final int SIDE_PANEL_WIDTH_MAX = 420;
    private static final int BOTTOM_PANEL_HEIGHT = 140;
    private static final int BOTTOM_PANEL_HEIGHT_MIN = 90;
    private static final int BOTTOM_PANEL_HEIGHT_MAX = 420;
    private static final int GLOBAL_TRACKERS_TOP_GAP = 36;

    public UIKeyframes view;
    public UIKeyframeFactory editor;

    private UIElement target;
    private boolean stackedLayout;
    private int sidePanelWidth = SIDE_PANEL_WIDTH;
    private int bottomPanelHeight = BOTTOM_PANEL_HEIGHT;
    private UIDraggable sidePanelResizer;

    public UIKeyframeEditor(Function<Consumer<Keyframe>, UIKeyframes> factory)
    {
        this.view = factory.apply(this::pickKeyframe);
        this.view.changed(() ->
        {
            if (this.editor != null)
            {
                this.editor.update();
            }
        });
        this.view.getDopeSheet().setTopMargin(GLOBAL_TRACKERS_TOP_GAP);

        this.add(this.view.full(this).w(1F, -SIDE_PANEL_WIDTH));

        this.sidePanelResizer = new UIDraggable((context) ->
        {
            if (this.stackedLayout)
            {
                int height = this.area.ey() - context.mouseY;

                this.bottomPanelHeight = Math.max(BOTTOM_PANEL_HEIGHT_MIN, Math.min(BOTTOM_PANEL_HEIGHT_MAX, height));
            }
            else
            {
                int width = this.area.ex() - context.mouseX;

                this.sidePanelWidth = Math.max(SIDE_PANEL_WIDTH_MIN, Math.min(SIDE_PANEL_WIDTH_MAX, width));
            }

            this.applyLayout();
            this.resize();
        })
        {
            @Override
            protected boolean subMouseClicked(UIContext context)
            {
                if (this.area.isInside(context) && context.mouseButton == 0 && Window.isCtrlPressed())
                {
                    if (UIKeyframeEditor.this.stackedLayout)
                    {
                        UIKeyframeEditor.this.bottomPanelHeight = BOTTOM_PANEL_HEIGHT;
                    }
                    else
                    {
                        UIKeyframeEditor.this.sidePanelWidth = SIDE_PANEL_WIDTH;
                    }
                    UIKeyframeEditor.this.applyLayout();
                    UIKeyframeEditor.this.resize();

                    return true;
                }

                return super.subMouseClicked(context);
            }
        }.rendering((context) ->
        {
            float alpha = (this.sidePanelResizer.isDragging() || this.sidePanelResizer.area.isInside(context)) ? 0.75F : 0.5F;
            int color = Colors.setA(BBSSettings.primaryColor.get(), alpha);

            context.batcher.box(this.sidePanelResizer.area.x, this.sidePanelResizer.area.y, this.sidePanelResizer.area.ex(), this.sidePanelResizer.area.ey(), color);
        });
        this.add(this.sidePanelResizer);
        this.updateSidePanelResizerState();
    }

    public UIKeyframeEditor target(UIElement target)
    {
        this.target = target;

        this.view.resetFlex().full(this).w(1F);

        return this;
    }

    private void pickKeyframe(Keyframe keyframe)
    {
        UIKeyframeFactory.saveScroll(this.editor);

        if (this.editor != null)
        {
            this.editor.removeFromParent();
            this.editor = null;
        }

        if (keyframe != null)
        {
            this.editor = UIKeyframeFactory.createPanel(keyframe, this.view);

            this.add(this.editor);
        }

        this.applyLayout();
        this.resize();
    }

    private void applyLayout()
    {
        if (this.target != null)
        {
            this.view.resetFlex().full(this).w(1F);

            if (this.editor != null)
            {
                this.editor.full(this.target);
                this.target.resize();
            }

            return;
        }

        if (this.stackedLayout)
        {
            this.view.resetFlex().relative(this).xy(0, 0).w(1F).h(1F, this.editor == null ? 0 : -this.bottomPanelHeight);

            if (this.editor != null)
            {
                this.editor.relative(this).x(0).y(1F, -this.bottomPanelHeight).w(1F).h(this.bottomPanelHeight);
            }
        }
        else
        {
            this.view.resetFlex().relative(this).xy(0, 0).w(1F, this.editor == null ? 0 : -this.sidePanelWidth).h(1F);

            if (this.editor != null)
            {
                this.editor.relative(this).x(1F, -this.sidePanelWidth).y(0).w(this.sidePanelWidth).h(1F);
            }
        }

        this.updateSidePanelResizerState();
    }

    @Override
    public void resize()
    {
        super.resize();
        this.updateSidePanelResizerState();
    }

    @Override
    public void render(UIContext context)
    {
        this.updateSidePanelResizerState();
        super.render(context);
    }

    private void updateSidePanelResizerState()
    {
        if (this.sidePanelResizer == null)
        {
            return;
        }

        boolean locked = BBSSettings.editorLayoutSettings != null && BBSSettings.editorLayoutSettings.isLayoutLocked();
        boolean visible = this.target == null && this.editor != null && !locked;

        this.sidePanelResizer.setVisible(visible);
        this.sidePanelResizer.setEnabled(visible);

        if (!visible)
        {
            return;
        }

        if (this.stackedLayout)
        {
            this.bottomPanelHeight = Math.max(BOTTOM_PANEL_HEIGHT_MIN, Math.min(BOTTOM_PANEL_HEIGHT_MAX, this.bottomPanelHeight));

            int x = this.area.mx() - 20;
            int y = this.area.ey() - this.bottomPanelHeight - 3;

            this.sidePanelResizer.relative(this).x(x - this.area.x).y(y - this.area.y).w(40).h(6);
        }
        else
        {
            this.sidePanelWidth = Math.max(SIDE_PANEL_WIDTH_MIN, Math.min(SIDE_PANEL_WIDTH_MAX, this.sidePanelWidth));

            int x = this.area.ex() - this.sidePanelWidth - 3;
            int y = this.area.my() - 20;

            this.sidePanelResizer.relative(this).x(x - this.area.x).y(y - this.area.y).w(6).h(40);
        }

        this.sidePanelResizer.resize();
    }

    public void toggleLayout()
    {
        this.setStackedLayout(!this.stackedLayout);
    }

    public boolean isStackedLayout()
    {
        return this.stackedLayout;
    }

    public void setStackedLayout(boolean stackedLayout)
    {
        this.stackedLayout = stackedLayout;
        this.applyLayout();
        this.resize();
    }

    public void setChannel(KeyframeChannel channel, int color)
    {
        this.view.removeAllSheets();
        this.view.addSheet(new UIKeyframeSheet(color, false, channel, null));

        this.pickKeyframe(null);
    }

    public void setClip(KeyframeClip clip)
    {
        this.setChannels(clip.channels);
    }

    public void setChannels(KeyframeChannel[] channels)
    {
        this.view.removeAllSheets();

        for (int i = 0; i < channels.length; i++)
        {
            this.view.addSheet(new UIKeyframeSheet(COLORS[i % COLORS.length], false, channels[i], null));
        }

        this.pickKeyframe(null);
    }

    public UIKeyframeSheet getSheet(Keyframe keyframe)
    {
        if (keyframe == null)
        {
            return null;
        }

        for (UIKeyframeSheet sheet : this.view.getGraph().getSheets())
        {
            if (sheet.channel == keyframe.getParent())
            {
                return sheet;
            }
        }

        return null;
    }

    public Pair<String, Boolean> getBone()
    {
        UIKeyframeFactory editor = this.editor;
        String bone = null;
        boolean local = false;

        if (editor instanceof UIPoseKeyframeFactory || editor instanceof UITransformKeyframeFactory)
        {
            UIKeyframeSheet sheet = this.getSheet(editor.getKeyframe());

            if (sheet != null)
            {
                String id = StringUtils.fileName(sheet.id);
                int colon = id.indexOf(':');
                String propertyId = colon != -1 ? id.substring(0, colon) : id;
                String boneName = colon != -1 ? id.substring(colon + 1) : null;

                boolean isPose = propertyId.equals("pose") || propertyId.startsWith("pose_overlay");

                if (isPose)
                {
                    String targetBone = boneName;

                    if (targetBone == null)
                    {
                        if (editor instanceof UIPoseKeyframeFactory pose)
                        {
                            targetBone = pose.poseEditor.getCurrentBone();

                            if (targetBone == null || targetBone.isEmpty())
                            {
                                targetBone = pose.poseEditor.groups.list.getCurrentFirst();
                            }
                        }
                    }

                    /* If the ID includes a property path (e.g., formPath/pose or formPath/pose_overlayX),
                     * retain the form prefix to correctly position the bone in the renderer.*/
                    if (sheet.id.contains("/pose") || sheet.id.contains("/pose_overlay"))
                    {
                        int lastSlash = sheet.id.lastIndexOf('/');
                        String prefix = sheet.id.substring(0, lastSlash);

                        bone = targetBone == null || targetBone.isEmpty() ? prefix : prefix + "/" + targetBone;
                    }
                    else
                    {
                        bone = targetBone;
                    }

                    if (editor instanceof UIPoseKeyframeFactory pose)
                    {
                        local = pose.poseEditor.transform.isLocal();
                    }
                    else if (editor instanceof UITransformKeyframeFactory transform)
                    {
                        local = transform.transform.isLocal();
                    }
                }
                else if (propertyId.equals("transform") || propertyId.startsWith("transform_overlay"))
                {
                    int lastSlash = sheet.id.lastIndexOf('/');

                    bone = lastSlash >= 0 ? sheet.id.substring(0, lastSlash) : "";

                    if (editor instanceof UITransformKeyframeFactory transform)
                    {
                        local = transform.transform.isLocal();
                    }
                }
            }
        }

        if (bone != null)
        {
            return new Pair<>(bone, local);
        }

        return null;
    }

    @Override
    public void applyUndoData(MapType data)
    {
        super.applyUndoData(data);

        KeyframeState state = new KeyframeState();

        state.extra = data.getMap("extra");

        for (BaseType type : data.getList("selection"))
        {
            state.selected.add(DataStorageUtils.intListFromData(type));
        }

        this.view.applyState(state);
    }

    @Override
    public void collectUndoData(MapType data)
    {
        super.collectUndoData(data);

        KeyframeState keyframeState = this.view.cacheState();
        ListType selection = new ListType();

        for (List<Integer> integers : keyframeState.selected)
        {
            selection.add(DataStorageUtils.intListToData(integers));
        }

        data.put("extra", keyframeState.extra);
        data.put("selection", selection);
    }
}
