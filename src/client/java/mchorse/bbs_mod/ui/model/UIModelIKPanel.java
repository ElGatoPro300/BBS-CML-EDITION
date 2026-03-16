package mchorse.bbs_mod.ui.model;

import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;

import java.util.ArrayList;
import java.util.List;

public class UIModelIKPanel extends UIElement
{
    private final UIModelPanel parent;

    private final UISearchList<String> ikSearch;
    private final UIStringList ikList;
    private final UIButton addIkButton;
    private final UIButton removeIkButton;
    private final UIButton editIkButton;
    private final UIToggle ikVisualizerToggle;

    private final UIButton chooseIconButton;
    private final UIPropTransform ikTransform;
    private final UITextbox ikNameField;

    private final List<String> chains = new ArrayList<>();

    public UIModelIKPanel(UIModelPanel parent)
    {
        this.parent = parent;

        this.relative(parent.mainView).w(1F).h(1F);

        int sideMargin = 10;
        int leftWidth = 220;
        int rightWidth = 220;
        int rowGap = 6;

        UILabel leftTitle = UI.label(UIKeys.MODELS_IK_HIERARCHY).background();
        leftTitle.relative(this).x(sideMargin).y(10).w(leftWidth).h(12);

        this.ikList = new UIStringList(null);
        this.ikList.background();

        this.ikSearch = new UISearchList<>(this.ikList);
        this.ikSearch.label(UIKeys.GENERAL_SEARCH);
        this.ikSearch.relative(this).x(sideMargin).y(26).w(leftWidth).h(1F, -210);

        this.addIkButton = new UIButton(UIKeys.GENERAL_ADD, (b) -> this.addChain());
        this.removeIkButton = new UIButton(UIKeys.GENERAL_REMOVE, (b) -> this.removeSelectedChain());

        this.addIkButton.w(0.5F, -4).h(20);
        this.removeIkButton.w(0.5F, -4).h(20);

        UIElement addRemoveBar = UI.row(8, this.addIkButton, this.removeIkButton);
        addRemoveBar.relative(this.ikSearch).y(1F, rowGap).w(1F).h(20);

        this.editIkButton = new UIButton(UIKeys.GENERAL_EDIT, (b) -> {});
        this.editIkButton.relative(addRemoveBar).y(1F, rowGap).w(1F).h(20);

        UILabel visualizerLabel = UI.label(UIKeys.MODELS_IK_VISUALIZER);
        visualizerLabel.relative(this.editIkButton).y(1F, 10).w(leftWidth).h(12);

        this.ikVisualizerToggle = new UIToggle(UIKeys.MODELS_IK_SHOW, false, (b) -> {});
        this.ikVisualizerToggle.relative(visualizerLabel).y(1F, 4).w(120);

        this.chooseIconButton = new UIButton(UIKeys.MODELS_IK_CHOOSE_ICON, (b) -> {});
        this.chooseIconButton.relative(this).x(1F, -rightWidth - sideMargin).y(10).w(rightWidth).h(20);

        this.ikTransform = new UIPropTransform();
        this.ikTransform.relative(this.chooseIconButton).y(1F, 10).w(1F).h(120);

        UILabel nameLabel = UI.label(UIKeys.MODELS_IK_NAME);
        nameLabel.relative(this.ikTransform).y(1F, 10).w(1F).h(12);

        this.ikNameField = new UITextbox(1000, null);
        this.ikNameField.relative(nameLabel).y(1F, 4).w(1F).h(20);

        this.add(leftTitle, this.ikSearch, addRemoveBar,
            this.editIkButton, visualizerLabel, this.ikVisualizerToggle,
            this.chooseIconButton, this.ikTransform, nameLabel, this.ikNameField);

        this.bootstrapExampleChains();
    }

    private void bootstrapExampleChains()
    {
        this.chains.clear();
        this.refreshChainList();
    }

    private void addChain()
    {
        String base = "IK-";
        int index = this.chains.size() + 1;

        this.chains.add(base + index);
        this.refreshChainList();
        this.ikList.setIndex(this.chains.size() - 1);
    }

    private void removeSelectedChain()
    {
        int index = this.ikList.getIndex();

        if (index >= 0 && index < this.chains.size())
        {
            this.chains.remove(index);
            this.refreshChainList();
            this.ikList.setIndex(Math.min(index, this.chains.size() - 1));
        }
    }

    private void refreshChainList()
    {
        this.ikList.clear();
        this.ikList.add(this.chains);
        this.ikList.sort();
    }
}

