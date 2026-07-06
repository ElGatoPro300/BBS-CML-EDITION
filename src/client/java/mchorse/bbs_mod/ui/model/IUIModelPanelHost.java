package mchorse.bbs_mod.ui.model;

import mchorse.bbs_mod.cubic.model.ModelConfig;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanel;
import mchorse.bbs_mod.ui.framework.elements.UIElement;

public interface IUIModelPanelHost extends IUIModelSectionEditor
{
    UIElement getMainView();

    UIModelEditorRenderer getModelRenderer();

    void setWorkspacePanel(UIElement panel);

    ModelConfig getModelConfig();

    void openTransformEditor(UIDashboardPanel panel);

    void returnFromSubEditor();
}
