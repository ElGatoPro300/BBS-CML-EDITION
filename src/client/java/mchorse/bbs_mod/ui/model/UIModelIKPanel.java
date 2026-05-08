package mchorse.bbs_mod.ui.model;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.model.IKChainConfig;
import mchorse.bbs_mod.cubic.model.ModelConfig;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.core.ValueList;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.context.UIContextMenu;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.colors.Colors;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * IK Editor panel — mirrors the Geometry Editor layout:
 *
 *   LEFT side panel  │   free 3D viewport (renderer)   │  RIGHT side panel
 *   ─────────────────┤                                  ├─────────────────────
 *   IK Chain list    │   (model visible here)           │  Tip Bone picker
 *   [+ Add]          │                                  │  Root Bone picker
 *   [- Remove]       │                                  │  Enabled / Weight
 *                    │                                  │  Iterations / Tolerance
 *                    │                                  │  Chain Length
 *                    │                                  │  Target X / Y / Z
 *
 * Both side panels are anchored to `this` (which covers mainView at w=1 h=1).
 * The centre area is never covered, so the 3D model is always fully visible.
 */
public class UIModelIKPanel extends UIElement
{
    /* ---- geometry constants (matches UIModelGeometryPanel) ---- */
    private static final int SIDE_MARGIN  = 10;
    private static final int LEFT_WIDTH   = 220;
    private static final int RIGHT_WIDTH  = 260;

    /* ---- LEFT side (chain list) ---- */
    private final UIStringList chainList;
    private final UIButton     addChain;
    private final UIButton     removeChain;

    /* ---- RIGHT side (detail editor) ---- */
    private final UIScrollView detailScroll;
    private final UILabel      noSelectionLabel;
    private final UIButton     tipBoneButton;
    private final UIButton     rootBoneButton;
    private final UIButton     targetBoneButton;
    private final UIToggle     enabledToggle;
    private final UITrackpad   weightPad;
    private final UITrackpad   iterationsPad;
    private final UITrackpad   tolerancePad;
    private final UITrackpad   chainLengthPad;
    private final UITrackpad   targetX;
    private final UITrackpad   targetY;
    private final UITrackpad   targetZ;
    private final UILabel      chainNameLabel;

    /* ---- state ---- */
    private final UIModelPanel editor;
    private ModelConfig        config;
    private IKChainConfig      selected;
    private List<String>       boneNames = new ArrayList<>();

    /* ======================================================================
     * Constructor
     * ==================================================================== */

    public UIModelIKPanel(UIModelPanel editor)
    {
        this.editor = editor;

        /* This element fills mainView entirely (same as UIModelGeometryPanel) */
        this.relative(editor.mainView).w(1F).h(1F);

        /* ----------------------------------------------------------------
         * LEFT side: chain list
         * -------------------------------------------------------------- */
        UILabel listTitle = UI.label(UIKeys.MODELS_IK_EDITOR).background();
        listTitle.relative(this).x(SIDE_MARGIN).y(10).w(LEFT_WIDTH).h(12);

        this.chainList = new UIStringList((items) ->
        {
            if (items != null && !items.isEmpty())
            {
                this.selectChain(items.get(0));
            }
        });
        this.chainList.relative(this)
                      .x(SIDE_MARGIN).y(26)
                      .w(LEFT_WIDTH).h(1F, -44);
        this.chainList.background();
        this.chainList.scroll.scrollItemSize = 18;

        this.addChain = new UIButton(UIKeys.MODELS_IK_CHAIN_ADD, (b) -> this.onAddChain());
        this.addChain.relative(this)
                     .x(SIDE_MARGIN).y(1F, -40)
                     .w(LEFT_WIDTH).h(18);
        this.addChain.tooltip(UIKeys.MODELS_IK_CHAIN_ADD_TOOLTIP);

        this.removeChain = new UIButton(UIKeys.MODELS_IK_CHAIN_REMOVE, (b) -> this.onRemoveChain());
        this.removeChain.relative(this)
                        .x(SIDE_MARGIN).y(1F, -20)
                        .w(LEFT_WIDTH).h(18);
        this.removeChain.tooltip(UIKeys.MODELS_IK_CHAIN_REMOVE_TOOLTIP);

        /* ----------------------------------------------------------------
         * RIGHT side: detail editor
         * Anchored from the right edge — same pattern as Geometry Editor
         * -------------------------------------------------------------- */

        /* Title label (bone name of selected chain) */
        UILabel editorTitle = UI.label(UIKeys.MODELS_IK_EDITOR).background();
        editorTitle.relative(this)
                   .x(1F, -RIGHT_WIDTH - SIDE_MARGIN).y(10)
                   .w(RIGHT_WIDTH).h(12);

        this.chainNameLabel = UI.label(IKey.raw("-"));
        this.chainNameLabel.relative(editorTitle).y(1F, 4).w(1F).h(12);

        /* "No selection" hint shown when nothing is selected */
        this.noSelectionLabel = UI.label(UIKeys.MODELS_IK_NO_SELECTION);
        this.noSelectionLabel.relative(this)
                             .x(1F, -RIGHT_WIDTH - SIDE_MARGIN)
                             .y(38)
                             .w(RIGHT_WIDTH).h(20);

        /* Detail scroll view (right column) */
        this.detailScroll = UI.scrollView(20, 8);
        this.detailScroll.scroll.cancelScrolling().opposite().scrollSpeed *= 3;
        this.detailScroll.relative(this)
                         .x(1F, -RIGHT_WIDTH - SIDE_MARGIN).y(38)
                         .w(RIGHT_WIDTH).h(1F, -48);

        /* Build the form rows inside the scroll view */
        UIElement fields = new UIElement();
        fields.relative(this.detailScroll).w(1F);
        fields.column().stretch().vertical().height(20).padding(4);

        this.tipBoneButton = new UIButton(UIKeys.MODELS_IK_TIP_BONE, (b) ->
            this.openBonePicker((bone) ->
            {
                if (this.selected != null)
                {
                    this.selected.tipBone.set(bone);
                    this.updateTipLabel();
                    this.editor.dirty();
                }
            })
        );
        this.tipBoneButton.tooltip(UIKeys.MODELS_IK_TIP_BONE_TOOLTIP);

        this.rootBoneButton = new UIButton(UIKeys.MODELS_IK_ROOT_BONE, (b) ->
            this.openBonePicker((bone) ->
            {
                if (this.selected != null)
                {
                    this.selected.rootBone.set(bone);
                    this.updateRootLabel();
                    this.editor.dirty();
                }
            })
        );
        this.rootBoneButton.tooltip(UIKeys.MODELS_IK_ROOT_BONE_TOOLTIP);

        this.targetBoneButton = new UIButton(UIKeys.MODELS_IK_TARGET_BONE, (b) ->
            this.openBonePicker((bone) ->
            {
                if (this.selected != null)
                {
                    this.selected.targetBone.set(bone);
                    this.updateTargetBoneLabel();
                    this.editor.dirty();
                }
            })
        );
        this.targetBoneButton.tooltip(UIKeys.MODELS_IK_TARGET_BONE_TOOLTIP);

        this.enabledToggle = new UIToggle(UIKeys.MODELS_IK_ENABLED, (b) ->
        {
            if (this.selected != null)
            {
                this.selected.enabled.set(b.getValue());
                this.editor.dirty();
                this.editor.renderer.setActiveIKChain(b.getValue() ? this.selected : null);
            }
        });
        this.enabledToggle.tooltip(UIKeys.MODELS_IK_ENABLED_TOOLTIP);

        this.weightPad = this.buildPad((v) ->
        {
            if (this.selected != null)
            {
                this.selected.weight.set(v.floatValue());
                this.editor.dirty();
            }
        }, 0D, 1D, 0.05, 0.01, 0.1);
        this.weightPad.tooltip(UIKeys.MODELS_IK_WEIGHT_TOOLTIP);

        this.iterationsPad = this.buildPad((v) ->
        {
            if (this.selected != null)
            {
                this.selected.iterations.set(v.intValue());
                this.editor.dirty();
            }
        }, 1D, 100D, 1, 1, 5);
        this.iterationsPad.integer();
        this.iterationsPad.tooltip(UIKeys.MODELS_IK_ITERATIONS_TOOLTIP);

        this.tolerancePad = this.buildPad((v) ->
        {
            if (this.selected != null)
            {
                this.selected.tolerance.set(v.floatValue());
                this.editor.dirty();
            }
        }, 0.001D, 1D, 0.005, 0.001, 0.01);
        this.tolerancePad.tooltip(UIKeys.MODELS_IK_TOLERANCE_TOOLTIP);

        this.chainLengthPad = this.buildPad((v) ->
        {
            if (this.selected != null)
            {
                this.selected.chainLength.set(v.intValue());
                this.editor.dirty();
            }
        }, 0D, 32D, 1, 1, 5);
        this.chainLengthPad.integer();
        this.chainLengthPad.tooltip(UIKeys.MODELS_IK_CHAIN_LENGTH_TOOLTIP);

        this.targetX = this.buildTargetAxisPad(0);
        this.targetY = this.buildTargetAxisPad(1);
        this.targetZ = this.buildTargetAxisPad(2);

        /* Assemble rows */
        fields.add(
            UI.label(UIKeys.MODELS_IK_TIP_BONE),    this.tipBoneButton,
            UI.label(UIKeys.MODELS_IK_ROOT_BONE),   this.rootBoneButton,
            this.enabledToggle,
            UI.label(UIKeys.MODELS_IK_WEIGHT),       this.weightPad,
            UI.label(UIKeys.MODELS_IK_ITERATIONS),   this.iterationsPad,
            UI.label(UIKeys.MODELS_IK_TOLERANCE),    this.tolerancePad,
            UI.label(UIKeys.MODELS_IK_CHAIN_LENGTH), this.chainLengthPad,
            UI.label(UIKeys.MODELS_IK_TARGET_BONE),  this.targetBoneButton,
            UI.label(UIKeys.MODELS_IK_TARGET),
            UI.label(IKey.raw("X")),  this.targetX,
            UI.label(IKey.raw("Y")),  this.targetY,
            UI.label(IKey.raw("Z")),  this.targetZ
        );
        this.detailScroll.add(fields);

        /* ---- assemble whole panel ---- */
        this.add(listTitle, this.chainList, this.addChain, this.removeChain);
        this.add(editorTitle, this.chainNameLabel, this.noSelectionLabel, this.detailScroll);

        this.setDetailVisible(false);
    }

    /* ======================================================================
     * Small builder helpers (keep constructor tidy)
     * ==================================================================== */

    private UITrackpad buildPad(Consumer<Double> callback,
                                double min, double max,
                                double step, double smallStep, double bigStep)
    {
        UITrackpad pad = new UITrackpad((v) -> callback.accept(v.doubleValue()));
        pad.limit(min, max).values(step, smallStep, bigStep);
        return pad;
    }

    private UITrackpad buildTargetAxisPad(int axis)
    {
        return new UITrackpad((v) ->
        {
            if (this.selected != null)
            {
                Vector3f t = new Vector3f(this.selected.target.get());

                if (axis == 0) t.x = v.floatValue();
                else if (axis == 1) t.y = v.floatValue();
                else                t.z = v.floatValue();

                this.selected.target.set(t);
                this.editor.dirty();
            }
        }).values(0.05, 0.01, 0.25);
    }

    /* ======================================================================
     * Visibility helpers
     * ==================================================================== */

    public void onBoneSelected(String bone)
    {
        if (this.config == null) return;

        for (IKChainConfig chain : this.config.ikChains.getList())
        {
            if (chain.isInChain(bone))
            {
                this.chainList.setCurrent(chain.getId());
                this.selectChain(chain.getId());

                return;
            }
        }
    }

    private void setDetailVisible(boolean visible)
    {
        this.detailScroll.setEnabled(visible);
        this.detailScroll.setVisible(visible);
        this.noSelectionLabel.setVisible(!visible);
        this.chainNameLabel.setVisible(visible);

        /* Show/hide gizmo in renderer */
        this.editor.renderer.setActiveIKChain(visible ? this.selected : null);
    }

    private void updateTipLabel()
    {
        if (this.selected == null) { this.tipBoneButton.label = UIKeys.MODELS_IK_TIP_BONE; return; }
        String tip = this.selected.tipBone.get();
        this.tipBoneButton.label = tip.isEmpty() ? UIKeys.MODELS_IK_TIP_BONE : IKey.constant(tip);
    }

    private void updateRootLabel()
    {
        if (this.selected == null) { this.rootBoneButton.label = UIKeys.MODELS_IK_ROOT_BONE; return; }
        String root = this.selected.rootBone.get();
        this.rootBoneButton.label = root.isEmpty() ? UIKeys.MODELS_IK_ROOT_BONE : IKey.constant(root);
    }

    private void updateTargetBoneLabel()
    {
        if (this.selected == null) { this.targetBoneButton.label = UIKeys.MODELS_IK_TARGET_BONE; return; }
        String tb = this.selected.targetBone.get();
        this.targetBoneButton.label = tb.isEmpty() ? UIKeys.MODELS_IK_TARGET_BONE : IKey.constant(tb);
    }

    private void refreshDetailFields()
    {
        if (this.selected == null)
        {
            this.setDetailVisible(false);
            return;
        }

        this.setDetailVisible(true);

        this.chainNameLabel.label = IKey.raw(this.selected.getId());
        this.updateTipLabel();
        this.updateRootLabel();
        this.updateTargetBoneLabel();
        this.enabledToggle.setValue(this.selected.enabled.get());
        this.weightPad.setValue(this.selected.weight.get());
        this.iterationsPad.setValue(this.selected.iterations.get());
        this.tolerancePad.setValue(this.selected.tolerance.get());
        this.chainLengthPad.setValue(this.selected.chainLength.get());

        Vector3f t = this.selected.target.get();
        this.targetX.setValue(t.x);
        this.targetY.setValue(t.y);
        this.targetZ.setValue(t.z);
    }

    /* ======================================================================
     * Chain list management
     * ==================================================================== */

    private void onAddChain()
    {
        if (this.config == null) { return; }

        ValueList<IKChainConfig> list = this.config.ikChains;
        String newId = String.valueOf(list.getList().size());
        IKChainConfig chain = new IKChainConfig(newId);
        list.add(chain);

        this.refreshChainList();
        this.chainList.setCurrent(newId);
        this.selectChain(newId);
        this.editor.dirty();
    }

    private void onRemoveChain()
    {
        if (this.config == null || this.selected == null) { return; }

        this.config.ikChains.remove(this.selected);
        this.selected = null;
        this.refreshChainList();
        this.setDetailVisible(false);
        this.editor.dirty();
    }

    private void selectChain(String id)
    {
        if (this.config == null) { return; }

        this.selected = null;

        for (IKChainConfig chain : this.config.ikChains.getList())
        {
            if (chain.getId().equals(id))
            {
                this.selected = chain;
                break;
            }
        }

        this.refreshDetailFields();
    }

    private void refreshChainList()
    {
        List<String> ids = new ArrayList<>();

        if (this.config != null)
        {
            for (IKChainConfig chain : this.config.ikChains.getList())
            {
                ids.add(chain.getId());
            }
        }

        this.chainList.setList(ids);
        this.chainList.update();
    }

    /* ======================================================================
     * Bone picker
     * ==================================================================== */

    private void openBonePicker(Consumer<String> callback)
    {
        if (this.boneNames.isEmpty()) { return; }

        UIBonePickerContextMenu menu = new UIBonePickerContextMenu(this.boneNames, callback);
        this.getContext().replaceContextMenu(menu);
        menu.xy(this.area.x + LEFT_WIDTH + SIDE_MARGIN + 4, this.area.y + 40)
            .w(180).h(220).bounds(this.getContext().menu.overlay, 5);
    }

    /* ======================================================================
     * Public API
     * ==================================================================== */

    public void setConfig(ModelConfig config)
    {
        this.config   = config;
        this.selected = null;
        this.boneNames.clear();

        /* Clear gizmo when switching config */
        this.editor.renderer.setActiveIKChain(null);

        if (config != null)
        {
            var instance = BBSModClient.getModels().getModel(config.getId());

            if (instance != null && instance.getModel() != null)
            {
                /* Cubic bone keys */
                this.boneNames.addAll(instance.getModel().getAllGroupKeys());

                /* BOBJ bones (if present) */
                instance.getModel().getAllBOBJBones().forEach(b -> this.boneNames.add(b.name));

                Collections.sort(this.boneNames);
            }
        }

        this.refreshChainList();
        this.setDetailVisible(false);
    }

    /* ======================================================================
     * Rendering — draw semi-transparent side-panel backgrounds
     * ==================================================================== */

    @Override
    public void render(UIContext context)
    {
        int x  = this.area.x;
        int y  = this.area.y;
        int ey = this.area.ey();

        /* Left panel background */
        context.batcher.box(
            x + SIDE_MARGIN - 2, y + 6,
            x + SIDE_MARGIN + LEFT_WIDTH + 2, ey - 6,
            0xaa000000
        );

        /* Right panel background */
        int rx = x + this.area.w - SIDE_MARGIN - RIGHT_WIDTH;
        context.batcher.box(
            rx - 2, y + 6,
            rx + RIGHT_WIDTH + 2, ey - 6,
            0xaa000000
        );

        super.render(context);
    }

    /* ======================================================================
     * Inner class: bone picker context menu
     * ==================================================================== */

    public static class UIBonePickerContextMenu extends UIContextMenu
    {
        public UISearchList<String> list;

        public UIBonePickerContextMenu(List<String> bones, Consumer<String> callback)
        {
            this.list = new UISearchList<>(new UIStringList((items) ->
            {
                if (items != null && !items.isEmpty() && items.get(0) != null)
                {
                    callback.accept(items.get(0));
                }
            }));
            this.list.list.setList(bones);
            this.list.list.background = 0xaa000000;
            this.list.relative(this).xy(5, 5).w(1F, -10).h(1F, -10);
            this.list.search.placeholder(UIKeys.POSE_CONTEXT_NAME);
            this.add(this.list);
        }

        @Override
        public boolean isEmpty()
        {
            return this.list.list.getList().isEmpty();
        }

        @Override
        public void setMouse(UIContext context)
        {
            this.xy(context.mouseX(), context.mouseY()).w(180).h(220)
                .bounds(context.menu.overlay, 5);
        }
    }
}
