package mchorse.bbs_mod.ui.model;

import mchorse.bbs_mod.BBSSettings;
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

import java.util.ArrayList;
import java.util.List;

public class UIModelPanel extends UIDataDashboardPanel<ModelConfig>
{
    public UIModelEditorRenderer renderer;
    
    public UIElement mainView;
    public List<UIElement> panels = new ArrayList<>();
    
    public UIElement modelSettingsPanel;
    public UIScrollView sectionsView;
    public UIScrollView rightView;
    public List<UIModelSection> sections = new ArrayList<>();

    public UIModelPanel(UIDashboard dashboard)
    {
        super(dashboard);

        this.overlay.add.setEnabled(false);

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
        
        /* Register Panels */
        UIElement spacer = new UIElement();
        spacer.relative(this.iconBar).w(1F).h(10);
        this.iconBar.add(spacer);

        this.registerPanel(this.modelSettingsPanel, UIKeys.MODELS_SETTINGS, Icons.POSE);
        this.registerPanel(this.createUnavailablePanel(), UIKeys.MODELS_IK_EDITOR, Icons.LIMB);
        this.registerPanel(this.createUnavailablePanel(), UIKeys.MODELS_DYNAMIC_BONES, Icons.CURVES);

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

        if (this.saveIcon != null)
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
        this.iconBar.add(button);

        return button;
    }

    public void setPanel(UIElement panel)
    {
        this.mainView.removeAll();
        this.mainView.add(panel);
        this.mainView.resize();
    }
    
    public void setRight(UIElement element)
    {
        this.rightView.removeAll();
        this.rightView.add(element);
        this.rightView.resize();
    }
    
    @Override
    public void forceSave()
    {
        super.forceSave();

        if (this.data == null)
        {
            return;
        }

        for (UIModelSection section : this.sections)
        {
            section.setConfig(this.data);
        }

        this.sectionsView.resize();
        this.rightView.resize();

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
        if (data != null)
        {
            this.renderer.setModel(data.getId());
            this.renderer.setConfig(data);
            
            for (UIModelSection section : this.sections)
            {
                section.setConfig(data);
            }
            
            this.sectionsView.resize();
            this.rightView.resize();
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