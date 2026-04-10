package mchorse.bbs_mod.ui.model;

import com.mojang.logging.LogUtils;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.BBSClient;
import mchorse.bbs_mod.cubic.animation.ActionsConfig;
import mchorse.bbs_mod.cubic.animation.legacy.config.LegacyAnimationsConfig;
import mchorse.bbs_mod.cubic.animation.legacy.validation.LegacyAnimationValidator;
import mchorse.bbs_mod.cubic.model.ModelConfig;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.ui.ContentType;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanels;
import mchorse.bbs_mod.ui.dashboard.panels.UIDataDashboardPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.framework.elements.utils.UIRenderable;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.ScrollDirection;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.pose.UIPoseEditor;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class UIModelPanel extends UIDataDashboardPanel<ModelConfig>
{
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final LegacyAnimationValidator LEGACY_VALIDATOR = new LegacyAnimationValidator();

    public UIModelEditorRenderer renderer;
    public UIIcon reloadIcon;
    
    public UIElement mainView;
    public List<UIElement> panels = new ArrayList<>();
    public List<UIIcon> panelButtons = new ArrayList<>();
    
    public UIElement modelSettingsPanel;
    public UIElement dynamicBonesPanel;
    public UIModelGeometryPanel geometryPanel;
    public UIScrollView sectionsView;
    public UIScrollView rightView;
    public UIScrollView dynamicBonesView;
    public UIScrollView dynamicBonesRightView;
    public List<UIModelSection> sections = new ArrayList<>();

    public UIModelPanel(UIDashboard dashboard)
    {
        super(dashboard);
        this.overlay.resizable().minSize(260, 220);

        this.overlay.add.setEnabled(false);

        this.reloadIcon = new UIIcon(Icons.REFRESH, (b) ->
        {
            if (this.data != null)
            {
                String modelId = this.data.getId();
                BBSClient.getModels().loadModel(modelId);
                this.renderer.invalidatePreviewModel();
                this.fillData(this.data);
            }
        });
        this.reloadIcon.tooltip(UIKeys.MODELS_RELOAD, Direction.LEFT);
        this.iconBar.add(this.reloadIcon);

        this.renderer = new UIModelEditorRenderer();
        this.renderer.relative(this).wTo(this.iconBar.getFlex()).h(1F);
        this.renderer.setCallback(this::pickBone);
        
        this.prepend(this.renderer);

        this.mainView = new UIElement();
        this.mainView.relative(this.editor).w(1F).h(1F);

        this.editor.add(this.mainView);
        this.iconBar.prepend(new UIRenderable(this::renderIcons));

        /* Model Settings Panel */
        this.modelSettingsPanel = new UIElement();
        this.modelSettingsPanel.relative(this.mainView).w(1F).h(1F);
        
        this.sectionsView = UI.scrollView(20, 10);
        this.sectionsView.scroll.cancelScrolling().opposite().scrollSpeed *= 3;
        this.sectionsView.relative(this.modelSettingsPanel).w(200).h(1F);
        
        this.rightView = UI.scrollView(20, 10);
        this.rightView.scroll.cancelScrolling().opposite().scrollSpeed *= 3;
        this.rightView.relative(this.modelSettingsPanel).x(1F, -200).w(200).h(1F);
        
        this.modelSettingsPanel.add(this.sectionsView, this.rightView);

        this.dynamicBonesPanel = new UIElement();
        this.dynamicBonesPanel.relative(this.mainView).w(1F).h(1F);

        this.dynamicBonesView = UI.scrollView(20, 10);
        this.dynamicBonesView.scroll.cancelScrolling().opposite().scrollSpeed *= 3;
        this.dynamicBonesView.relative(this.dynamicBonesPanel).w(200).h(1F);

        this.dynamicBonesRightView = UI.scrollView(20, 10);
        this.dynamicBonesRightView.scroll.cancelScrolling().opposite().scrollSpeed *= 3;
        this.dynamicBonesRightView.relative(this.dynamicBonesPanel).x(1F, -200).w(200).h(1F);

        this.dynamicBonesPanel.add(this.dynamicBonesView, this.dynamicBonesRightView);

        /* Sections setup */
        this.overlay.namesList.setFileIcon(Icons.MORPH);

        this.addSection(new UIModelGeneralSection(this));
        
        UIModelPartsSection parts = new UIModelPartsSection(this);
        this.sections.add(parts);
        this.setRight(parts.poseEditor);
        this.renderer.transform = parts.poseEditor.transform;

        this.addSection(new UIModelArmorSection(this));
        this.addSection(new UIModelItemsSection(this));
        this.addSection(new UIModelHandsSection(this));
        this.addSection(new UIModelSneakingSection(this));
        this.addSection(new UIModelLookAtSection(this));

        UIModelDynamicBonesSection dynamicBonesSection = new UIModelDynamicBonesSection(this);
        this.sections.add(dynamicBonesSection);
        this.dynamicBonesView.add(dynamicBonesSection);
        
        /* Register Panels */
        UIElement spacer = new UIElement();
        spacer.relative(this.iconBar).w(1F).h(10);
        this.iconBar.add(spacer);

        this.geometryPanel = new UIModelGeometryPanel(this);

        this.registerPanel(this.modelSettingsPanel, UIKeys.MODELS_SETTINGS, Icons.MODELS_SETTINGS);
        this.registerPanel(this.createUnavailablePanel(), UIKeys.MODELS_IK_EDITOR, Icons.IK);
        this.registerPanel(this.dynamicBonesPanel, UIKeys.MODELS_DYNAMIC_BONES, Icons.DYNAMIC_BONES);
        this.registerPanel(this.geometryPanel, UIKeys.MODELS_GEOMETRY_EDITOR, Icons.GEOMETRY_EDITOR);

        this.setPanel(this.modelSettingsPanel);
        
        this.fill(null);
    }
    
    private void renderIcons(UIContext context)
    {
        for (int i = 0, c = this.panels.size(); i < c; i++)
        {
            if (this.mainView.getChildren().contains(this.panels.get(i)))
            {
                int index = this.iconBar.getChildren().size() - this.panels.size() + i;

                if (index >= 0 && index < this.iconBar.getChildren().size())
                {
                    IUIElement child = this.iconBar.getChildren().get(index);

                    if (child instanceof UIIcon)
                    {
                        UIDashboardPanels.renderHighlightHorizontal(context.batcher, ((UIIcon) child).area);
                    }
                }
            }
        }

        if (this.reloadIcon != null)
        {
            Area a = this.reloadIcon.area;

            context.batcher.box(a.x + 3, a.ey() + 4, a.ex() - 3, a.ey() + 5, 0x22ffffff);
        }
        else if (this.saveIcon != null)
        {
            Area a = this.saveIcon.area;

            context.batcher.box(a.x + 3, a.ey() + 4, a.ex() - 3, a.ey() + 5, 0x22ffffff);
        }
    }
    
    private UIElement createUnavailablePanel()
    {
        UIElement panel = new UIElement();
        panel.relative(this.mainView).w(1F).h(1F);
        
        UILabel label = new UILabel(UIKeys.COMING_SOON)
        {
            @Override
            public void render(UIContext context)
            {
                context.batcher.getContext().getMatrices().push();
                
                int cx = this.area.mx();
                int cy = this.area.my();
                
                context.batcher.getContext().getMatrices().translate(cx, cy, 0);
                context.batcher.getContext().getMatrices().scale(2F, 2F, 1F);
                context.batcher.getContext().getMatrices().translate(-cx, -cy, 0);
                
                super.render(context);
                
                context.batcher.getContext().getMatrices().pop();
            }
        }.background();
        
        label.relative(panel).w(1F).xy(0.5F, 0.5F).anchor(0.5F, 0.5F);
        label.labelAnchor(0.5F, 0.5F);
        panel.add(label);
        
        return panel;
    }

    public UIIcon registerPanel(UIElement panel, IKey tooltip, Icon icon)
    {
        UIIcon button = new UIIcon(icon, (b) -> this.setPanel(panel));

        if (tooltip != null)
        {
            button.tooltip(tooltip, Direction.LEFT);
        }

        this.panels.add(panel);
        this.panelButtons.add(button);
        this.iconBar.add(button);

        return button;
    }

    public void setPanel(UIElement panel)
    {
        this.mainView.removeAll();
        this.mainView.add(panel);

        if (panel == this.dynamicBonesPanel)
        {
            this.setDynamicRight(this.getPoseEditor());
            this.renderer.transform = this.getPoseEditor().transform;
        }
        else if (panel == this.modelSettingsPanel)
        {
            this.setRight(this.getPoseEditor());
            this.renderer.transform = this.getPoseEditor().transform;
        }
        else if (panel == this.geometryPanel)
        {
            this.renderer.transform = this.geometryPanel.getGizmoTransformEditor();
        }

        this.mainView.resize();
    }
    
    public void setRight(UIElement element)
    {
        this.rightView.removeAll();
        this.rightView.add(element);
        this.rightView.resize();
    }

    public void setDynamicRight(UIElement element)
    {
        this.dynamicBonesRightView.removeAll();
        this.dynamicBonesRightView.add(element);
        this.dynamicBonesRightView.resize();
    }
    
    @Override
    public void save()
    {
        boolean hasData = this.data != null;
        boolean editorEnabled = this.editor != null && this.editor.isEnabled();

        LOGGER.debug("Model Editor save requested: hasData={}, update={}, editorEnabled={}", hasData, this.update, editorEnabled);

        if (!hasData)
        {
            LOGGER.warn("Model Editor save skipped: no model is selected");
            return;
        }

        if (!editorEnabled)
        {
            LOGGER.warn("Model Editor save skipped: editor is disabled for model {}", this.data.getId());
            return;
        }

        if (this.update)
        {
            LOGGER.warn("Model Editor save requested while update flag is true for model {}. Forcing save anyway", this.data.getId());
        }

        this.forceSave();
    }

    @Override
    public void forceSave()
    {
        if (this.data == null)
        {
            LOGGER.warn("Model Editor forceSave skipped: no model data");
            return;
        }

        if (!this.prepareLegacyAnimationCode())
        {
            return;
        }

        LOGGER.debug("Model Editor forceSave start: model={}", this.data.getId());

        for (UIModelSection section : this.sections)
        {
            section.setConfig(this.data);
        }

        try
        {
            super.forceSave();
        }
        catch (Exception e)
        {
            LOGGER.error("Model Editor forceSave failed during repository save for model {}", this.data.getId(), e);
            return;
        }

        if (this.data == null)
        {
            return;
        }

        if (this.geometryPanel != null)
        {
            this.geometryPanel.setConfig(this.data);
        }

        this.sectionsView.resize();
        this.rightView.resize();
        this.dynamicBonesView.resize();
        this.dynamicBonesRightView.resize();

        Morph morph = Morph.getMorph(MinecraftClient.getInstance().player);

        if (morph != null)
        {
            Form form = morph.getForm();

            if (form instanceof ModelForm && ((ModelForm) form).model.get().equals(this.data.getId()))
            {
                FormRenderer renderer = FormUtilsClient.getRenderer(form);

                if (renderer instanceof ModelFormRenderer)
                {
                    ((ModelFormRenderer) renderer).invalidateCachedModel();
                }
            }
        }

        LOGGER.debug("Model Editor forceSave completed: model={}", this.data.getId());
    }

    public void persistModelDataWithoutReload()
    {
        if (this.data == null)
        {
            LOGGER.warn("Model Editor persist without reload skipped: no model data");
            return;
        }

        if (!this.prepareLegacyAnimationCode())
        {
            return;
        }

        LOGGER.debug("Model Editor persist without reload start: model={}", this.data.getId());

        try
        {
            super.forceSave();
        }
        catch (Exception e)
        {
            LOGGER.error("Model Editor persist without reload failed for model {}", this.data.getId(), e);
            return;
        }

        Morph morph = Morph.getMorph(MinecraftClient.getInstance().player);

        if (morph != null)
        {
            Form form = morph.getForm();

            if (form instanceof ModelForm && ((ModelForm) form).model.get().equals(this.data.getId()))
            {
                FormRenderer renderer = FormUtilsClient.getRenderer(form);

                if (renderer instanceof ModelFormRenderer)
                {
                    ((ModelFormRenderer) renderer).invalidateCachedModel();
                }
            }
        }

        LOGGER.debug("Model Editor persist without reload completed: model={}", this.data.getId());
    }

    private boolean prepareLegacyAnimationCode()
    {
        if (this.data == null)
        {
            return false;
        }

        ActionsConfig actions = this.data.legacyAnimations.get();

        if (actions == null)
        {
            return true;
        }

        LegacyAnimationsConfig sanitized = LEGACY_VALIDATOR.sanitize(actions.legacyAnimations);
        List<String> validationErrors = LEGACY_VALIDATOR.validate(sanitized);

        if (!validationErrors.isEmpty())
        {
            LOGGER.error("Model Editor save blocked by invalid legacy animation config for model {}: {}", this.data.getId(), String.join("; ", validationErrors));
            return false;
        }

        String javascript = LEGACY_VALIDATOR.toJavascript(sanitized);

        if (!LEGACY_VALIDATOR.isValidJavascript(javascript))
        {
            LOGGER.error("Model Editor save blocked: generated legacy animation JavaScript is invalid for model {}", this.data.getId());
            return false;
        }

        actions.legacyAnimations.copy(sanitized);
        actions.legacyAnimationsJavascript = javascript;

        return true;
    }

    public UIPoseEditor getPoseEditor()
    {
        for (UIModelSection section : this.sections)
        {
            if (section instanceof UIModelPartsSection)
            {
                return ((UIModelPartsSection) section).poseEditor;
            }
        }

        return null;
    }

    private void pickBone(String bone)
    {
        for (UIModelSection section : this.sections)
        {
            section.deselect();
            section.onBoneSelected(bone);

            if (section instanceof UIModelPartsSection)
            {
                ((UIModelPartsSection) section).selectBone(bone);
                this.setRight(((UIModelPartsSection) section).poseEditor);
            }
        }
    }
    
    public void dirty()
    {
        this.renderer.dirty();
    }

    private void addSection(UIModelSection section)
    {
        this.sections.add(section);
        this.sectionsView.add(section);
    }

    @Override
    public ContentType getType()
    {
        return ContentType.MODELS;
    }

    @Override
    protected IKey getTitle()
    {
        return UIKeys.MODELS_TITLE;
    }

    @Override
    protected void fillData(ModelConfig data)
    {
        for (UIIcon button : this.panelButtons)
        {
            button.setEnabled(data != null);
        }

        if (data != null)
        {
            this.renderer.setModel(data.getId());
            this.renderer.setConfig(data);
            
            for (UIModelSection section : this.sections)
            {
                section.setConfig(data);
            }

            if (this.geometryPanel != null)
            {
                this.geometryPanel.setConfig(data);
            }
            
            this.sectionsView.resize();
            this.rightView.resize();
            this.dynamicBonesView.resize();
            this.dynamicBonesRightView.resize();
        }
    }

    @Override
    public void render(UIContext context)
    {
        int color = BBSSettings.primaryColor.get();

        this.area.render(context.batcher, Colors.mulRGB(color | Colors.A100, 0.1F));

        super.render(context);
    }

    @Override
    public void resize()
    {
        super.resize();

        this.renderer.resize();
    }

    @Override
    public void close()
    {}
}
