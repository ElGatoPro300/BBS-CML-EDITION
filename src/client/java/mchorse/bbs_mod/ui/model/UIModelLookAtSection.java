package mchorse.bbs_mod.ui.model;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.model.ModelConfig;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.context.UIContextMenu;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UIModelLookAtSection extends UIModelSection
{
    public UIButton lookAtLimb;

    public UIModelLookAtSection(UIModelPanel editor)
    {
        super(editor);

        this.lookAtLimb = new UIButton(UIKeys.MODELS_PICK_LOOK_AT_LIMB, (b) -> this.openLookAtContextMenu());

        this.fields.add(this.lookAtLimb);
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
}
