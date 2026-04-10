package mchorse.bbs_mod.ui.model;

import com.mojang.logging.LogUtils;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.animation.ActionsConfig;
import mchorse.bbs_mod.cubic.animation.legacy.config.LegacyAnimationsConfig;
import mchorse.bbs_mod.cubic.animation.legacy.config.LegacyLimbAnimationConfig;
import mchorse.bbs_mod.cubic.animation.legacy.validation.LegacyAnimationValidator;
import mchorse.bbs_mod.cubic.model.ModelConfig;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.context.UIContextMenu;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.utils.UI;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UIModelLookAtSection extends UIModelSection
{
    private static final Logger LOGGER = LogUtils.getLogger();

    public UIButton lookAtLimb;
    public UIButton legacyLimb;
    public UIButton wheelAxis;
    public UIToggle legacyEnabled;
    public UIToggle swinging;
    public UIToggle swiping;
    public UIToggle lookX;
    public UIToggle lookY;
    public UIToggle idle;
    public UIToggle invert;
    public UIToggle wheel;
    public UIToggle wheelReverse;
    public UITrackpad wheelSpeed;

    private String currentLegacyLimb;
    private boolean updatingLegacyUI;
    private final LegacyAnimationValidator validator = new LegacyAnimationValidator();

    public UIModelLookAtSection(UIModelPanel editor)
    {
        super(editor);

        this.lookAtLimb = new UIButton(UIKeys.MODELS_PICK_LOOK_AT_LIMB, (b) -> this.openLookAtContextMenu());
        this.legacyLimb = new UIButton(IKey.constant("Pick legacy limb..."), (b) -> this.openLegacyLimbContextMenu());
        this.wheelAxis = new UIButton(IKey.constant("x"), (b) -> this.openWheelAxisContextMenu());

        this.legacyEnabled = new UIToggle(IKey.constant("Legacy animations"), (b) ->
        {
            if (this.updatingLegacyUI)
            {
                return;
            }

            LegacyAnimationsConfig config = this.getLegacyConfig();

            if (config != null)
            {
                config.enabled = b.getValue();

                if (config.enabled && this.config != null && !this.config.procedural.get())
                {
                    this.config.procedural.set(true);
                }

                this.pushLegacyChanges();
            }
        });
        this.swinging = new UIToggle(IKey.constant("Swinging"), (b) -> this.applyLegacy((c) -> c.swinging = b.getValue()));
        this.swiping = new UIToggle(IKey.constant("Swiping"), (b) -> this.applyLegacy((c) -> c.swiping = b.getValue()));
        this.lookX = new UIToggle(IKey.constant("Look X"), (b) -> this.applyLegacy((c) -> c.lookX = b.getValue()));
        this.lookY = new UIToggle(IKey.constant("Look Y"), (b) -> this.applyLegacy((c) -> c.lookY = b.getValue()));
        this.idle = new UIToggle(IKey.constant("Idle"), (b) -> this.applyLegacy((c) -> c.idle = b.getValue()));
        this.invert = new UIToggle(IKey.constant("Invert"), (b) -> this.applyLegacy((c) -> c.invert = b.getValue()));
        this.wheel = new UIToggle(IKey.constant("Wheel"), (b) -> this.applyLegacy((c) -> c.wheel = b.getValue()));
        this.wheelReverse = new UIToggle(IKey.constant("Wheel reverse"), (b) -> this.applyLegacy((c) -> c.wheelReverse = b.getValue()));
        this.wheelSpeed = new UITrackpad((value) -> this.applyLegacy((c) -> c.wheelSpeed = value.floatValue()));
        this.wheelSpeed.limit(0, 100);

        this.fields.add(this.lookAtLimb);
        this.fields.add(UI.label(IKey.constant("Legacy limb animations")).background());
        this.fields.add(this.legacyEnabled);
        this.fields.add(this.legacyLimb);
        this.fields.add(this.swinging, this.swiping, this.lookX, this.lookY, this.idle, this.invert, this.wheel, this.wheelReverse);
        this.fields.add(UI.label(IKey.constant("Wheel axis")), this.wheelAxis);
        this.fields.add(UI.label(IKey.constant("Wheel speed")), this.wheelSpeed);

        this.updateLegacyUI();
    }

    private void openLookAtContextMenu()
    {
        if (this.config == null) return;

        ModelInstance model = BBSModClient.getModels().getModel(this.config.getId());
        if (model == null) return;

        List<String> groups = new ArrayList<>(model.getModel().getAllGroupKeys());
        Collections.sort(groups);
        groups.add(0, "<none>");

        UILookAtStringListContextMenu menu = new UILookAtStringListContextMenu(groups, (group) ->
        {
            if (group.equals("<none>"))
            {
                this.config.lookAtHead.set("");
            }
            else
            {
                this.config.lookAtHead.set(group);
            }

            this.editor.dirty();
            this.editor.forceSave();
        });

        String current = this.config.lookAtHead.get();
        menu.list.list.setCurrent(current.isEmpty() ? "<none>" : current);

        this.getContext().replaceContextMenu(menu);
        menu.xy(this.lookAtLimb.area.x, this.lookAtLimb.area.ey()).w(this.lookAtLimb.area.w).h(200).bounds(this.getContext().menu.overlay, 5);
    }

    private void openLegacyLimbContextMenu()
    {
        if (this.config == null)
        {
            return;
        }

        ModelInstance model = BBSModClient.getModels().getModel(this.config.getId());

        if (model == null)
        {
            return;
        }

        List<String> groups = new ArrayList<>(model.getModel().getAllGroupKeys());
        Collections.sort(groups);

        UILookAtStringListContextMenu menu = new UILookAtStringListContextMenu(groups, (group) ->
        {
            this.currentLegacyLimb = group;
            this.updateLegacyUI();
        });

        if (this.currentLegacyLimb != null && !this.currentLegacyLimb.isEmpty())
        {
            menu.list.list.setCurrent(this.currentLegacyLimb);
        }

        this.getContext().replaceContextMenu(menu);
        menu.xy(this.legacyLimb.area.x, this.legacyLimb.area.ey()).w(this.legacyLimb.area.w).h(200).bounds(this.getContext().menu.overlay, 5);
    }

    private void openWheelAxisContextMenu()
    {
        List<String> values = List.of("x", "y", "z");

        UILookAtStringListContextMenu menu = new UILookAtStringListContextMenu(values, (selected) ->
        {
            this.applyLegacy((config) -> config.wheelAxis = selected);
            this.updateLegacyUI();
        });

        LegacyLimbAnimationConfig config = this.getCurrentLimbConfig(false);

        if (config != null && config.wheelAxis != null)
        {
            menu.list.list.setCurrent(config.wheelAxis.toLowerCase());
        }

        this.getContext().replaceContextMenu(menu);
        menu.xy(this.wheelAxis.area.x, this.wheelAxis.area.ey()).w(this.wheelAxis.area.w).h(100).bounds(this.getContext().menu.overlay, 5);
    }

    private LegacyAnimationsConfig getLegacyConfig()
    {
        ActionsConfig actions = this.getActionsConfig();

        return actions == null ? null : actions.legacyAnimations;
    }

    private ActionsConfig getActionsConfig()
    {
        if (this.config == null)
        {
            return null;
        }

        return this.config.legacyAnimations.get();
    }

    private void applyLegacy(java.util.function.Consumer<LegacyLimbAnimationConfig> callback)
    {
        if (this.updatingLegacyUI)
        {
            return;
        }

        LegacyAnimationsConfig config = this.getLegacyConfig();
        LegacyLimbAnimationConfig limb = this.getCurrentLimbConfig(true);

        if (config == null || limb == null)
        {
            return;
        }

        callback.accept(limb);

        if (limb.isEmpty() && this.currentLegacyLimb != null)
        {
            config.limbs.remove(this.currentLegacyLimb);
        }

        this.pushLegacyChanges();
        this.updateLegacyUI();
    }

    private LegacyLimbAnimationConfig getCurrentLimbConfig(boolean create)
    {
        LegacyAnimationsConfig config = this.getLegacyConfig();

        if (config == null || this.currentLegacyLimb == null || this.currentLegacyLimb.isEmpty())
        {
            return null;
        }

        if (create)
        {
            return config.limbs.computeIfAbsent(this.currentLegacyLimb, (key) -> new LegacyLimbAnimationConfig());
        }

        return config.limbs.get(this.currentLegacyLimb);
    }

    private void pushLegacyChanges()
    {
        LegacyAnimationsConfig config = this.getLegacyConfig();

        if (config != null && !config.enabled && config.limbs.isEmpty())
        {
            config.enabled = false;
        }

        this.logLegacyState("pushLegacyChanges");
        this.syncLegacyJavascript(config);
        this.editor.renderer.syncLegacyAnimationsAndRefreshAnimator();
        this.editor.dirty();
        this.editor.persistModelDataWithoutReload();
    }

    private void logLegacyState(String source)
    {
        LegacyAnimationsConfig animations = this.getLegacyConfig();
        LegacyLimbAnimationConfig limb = this.getCurrentLimbConfig(false);
        String modelId = this.config == null ? "<none>" : this.config.getId();
        String limbName = this.currentLegacyLimb == null || this.currentLegacyLimb.isEmpty() ? "<none>" : this.currentLegacyLimb;
        boolean enabled = animations != null && animations.enabled;
        int totalLimbs = animations == null ? 0 : animations.limbs.size();

        if (limb == null)
        {
            LOGGER.debug("Legacy animations state [{}] model={} enabled={} selectedLimb={} limbsConfigured={} limbConfig=<none>", source, modelId, enabled, limbName, totalLimbs);
            return;
        }

        LOGGER.debug(
            "Legacy animations state [{}] model={} enabled={} selectedLimb={} limbsConfigured={} swinging={} swiping={} lookX={} lookY={} idle={} invert={} wheel={} wheelAxis={} wheelSpeed={} wheelReverse={}",
            source,
            modelId,
            enabled,
            limbName,
            totalLimbs,
            limb.swinging,
            limb.swiping,
            limb.lookX,
            limb.lookY,
            limb.idle,
            limb.invert,
            limb.wheel,
            limb.wheelAxis,
            limb.wheelSpeed,
            limb.wheelReverse
        );
    }

    private void updateLegacyUI()
    {
        this.updatingLegacyUI = true;

        if (this.currentLegacyLimb == null || this.currentLegacyLimb.isEmpty())
        {
            this.currentLegacyLimb = this.editor.renderer.getSelectedBone();
        }

        LegacyAnimationsConfig animations = this.getLegacyConfig();
        LegacyLimbAnimationConfig config = this.getCurrentLimbConfig(false);
        boolean hasLimb = this.currentLegacyLimb != null && !this.currentLegacyLimb.isEmpty();
        boolean enabled = hasLimb && animations != null;

        this.legacyLimb.label = IKey.constant(hasLimb ? this.currentLegacyLimb : "Pick legacy limb...");
        this.legacyEnabled.setValue(animations != null && animations.enabled);
        this.wheelAxis.label = IKey.constant(config == null ? "x" : config.wheelAxis);

        this.swinging.setValue(config != null && config.swinging);
        this.swiping.setValue(config != null && config.swiping);
        this.lookX.setValue(config != null && config.lookX);
        this.lookY.setValue(config != null && config.lookY);
        this.idle.setValue(config != null && config.idle);
        this.invert.setValue(config != null && config.invert);
        this.wheel.setValue(config != null && config.wheel);
        this.wheelReverse.setValue(config != null && config.wheelReverse);
        this.wheelSpeed.setValue(config == null ? 1F : config.wheelSpeed);

        this.swinging.setEnabled(enabled);
        this.swiping.setEnabled(enabled);
        this.lookX.setEnabled(enabled);
        this.lookY.setEnabled(enabled);
        this.idle.setEnabled(enabled);
        this.invert.setEnabled(enabled);
        this.wheel.setEnabled(enabled);
        this.wheelReverse.setEnabled(enabled);
        this.wheelSpeed.setEnabled(enabled);
        this.wheelAxis.setEnabled(enabled);

        this.updatingLegacyUI = false;
    }

    private void syncLegacyJavascript(LegacyAnimationsConfig config)
    {
        ActionsConfig actions = this.getActionsConfig();

        if (actions == null || config == null)
        {
            return;
        }

        LegacyAnimationsConfig sanitized = this.validator.sanitize(config);
        actions.legacyAnimations.copy(sanitized);
        actions.legacyAnimationsJavascript = this.validator.toJavascript(sanitized);
    }

    public static class UILookAtStringListContextMenu extends UIContextMenu
    {
        public UISearchList<String> list;

        public UILookAtStringListContextMenu(List<String> groups, java.util.function.Consumer<String> callback)
        {
            this.list = new UISearchList<>(new UIStringList((l) ->
            {
                if (l.get(0) != null) callback.accept(l.get(0));
            }));
            this.list.list.setList(groups);
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
            this.xy(context.mouseX(), context.mouseY()).w(120).h(200).bounds(context.menu.overlay, 5);
        }
    }

    @Override
    public IKey getTitle()
    {
        return UIKeys.MODELS_LOOK_AT_LIMB;
    }

    @Override
    public void onBoneSelected(String bone)
    {
        this.currentLegacyLimb = bone;
        this.updateLegacyUI();
    }

    @Override
    public void setConfig(ModelConfig config)
    {
        super.setConfig(config);
        this.updateLegacyUI();
    }
}
