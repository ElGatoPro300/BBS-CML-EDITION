package mchorse.bbs_mod.ui.model;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.data.model.Model;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.cubic.model.IKChainConfig;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.utils.UI;

import java.util.ArrayList;
import java.util.List;

public class UIModelIKHierarchyOverlayPanel extends UIOverlayPanel
{
    private final UIModelIKPanel panel;
    private final IKChainConfig chain;
    private final List<String> hierarchyBoneIds = new ArrayList<>();
    private final UIStringList hierarchyList;
    private final UIStringList chainList;

    public UIModelIKHierarchyOverlayPanel(UIModelIKPanel panel, IKChainConfig chain, String modelId)
    {
        super(UIKeys.MODELS_IK_HIERARCHY);

        this.panel = panel;
        this.chain = chain;
        this.content.removeAll();

        this.hierarchyList = new UIStringList((l) ->
        {
            String bone = this.getSelectedHierarchyBone();

            if (bone != null)
            {
                this.panel.previewBone(bone);
            }
        })
        {
            @Override
            protected boolean sortElements()
            {
                return false;
            }
        };
        this.hierarchyList.background();

        this.chainList = new UIStringList((l) -> {})
        {
            @Override
            protected boolean sortElements()
            {
                return false;
            }

            @Override
            protected void handleSwap(int from, int to)
            {
                super.handleSwap(from, to);
                UIModelIKHierarchyOverlayPanel.this.pushChain();
            }
        };
        this.chainList.sorting();
        this.chainList.background();

        UISearchList<String> hierarchySearch = new UISearchList<>(this.hierarchyList);
        hierarchySearch.label(UIKeys.GENERAL_SEARCH);
        hierarchySearch.relative(this.content).x(6).y(6).w(0.5F, -9).h(1F, -42);

        UISearchList<String> chainSearch = new UISearchList<>(this.chainList);
        chainSearch.label(UIKeys.GENERAL_SEARCH);
        chainSearch.relative(this.content).x(0.5F, 3).y(6).w(0.5F, -9).h(1F, -42);

        UIButton add = new UIButton(UIKeys.MODELS_IK_ADD_SELECTED, (b) -> this.addSelected());
        UIButton remove = new UIButton(UIKeys.MODELS_IK_REMOVE_SELECTED, (b) -> this.removeSelected());

        add.w(0.5F, -4);
        remove.w(0.5F, -4);

        UIElement bar = UI.row(8, add, remove);
        bar.relative(this.content).x(6).y(1F, -6).w(1F, -12).anchorY(1F);

        this.content.add(hierarchySearch, chainSearch, bar);

        this.reloadHierarchy(modelId);
        this.reloadChain();
    }

    private void reloadHierarchy(String modelId)
    {
        this.hierarchyList.clear();
        this.hierarchyBoneIds.clear();

        ModelInstance instance = BBSModClient.getModels().getModel(modelId);

        if (instance == null || !(instance.model instanceof Model model))
        {
            return;
        }

        for (ModelGroup group : model.topGroups)
        {
            this.collectHierarchy(group, 0);
        }
    }

    private void collectHierarchy(ModelGroup group, int depth)
    {
        StringBuilder prefix = new StringBuilder();

        for (int i = 0; i < depth; i++)
        {
            prefix.append("  ");
        }

        this.hierarchyList.add(prefix + "└ " + group.id);
        this.hierarchyBoneIds.add(group.id);

        for (ModelGroup child : group.children)
        {
            this.collectHierarchy(child, depth + 1);
        }
    }

    private void reloadChain()
    {
        this.chainList.clear();

        for (String bone : this.chain.getBones())
        {
            this.chainList.add(bone);
        }
    }

    private String getSelectedHierarchyBone()
    {
        int index = this.hierarchyList.getIndex();

        if (index < 0 || index >= this.hierarchyBoneIds.size())
        {
            return null;
        }

        return this.hierarchyBoneIds.get(index);
    }

    private void addSelected()
    {
        String selected = this.getSelectedHierarchyBone();

        if (selected == null || this.chainList.getList().contains(selected))
        {
            return;
        }

        this.chainList.add(selected);
        this.chainList.setCurrent(selected);
        this.pushChain();
        this.panel.previewBone(selected);
    }

    private void removeSelected()
    {
        int index = this.chainList.getIndex();

        if (index < 0 || index >= this.chainList.getList().size())
        {
            return;
        }

        this.chainList.remove(this.chainList.getList().get(index));
        this.pushChain();
    }

    private void pushChain()
    {
        this.panel.applyHierarchyBones(this.chain, new ArrayList<>(this.chainList.getList()));
    }
}
