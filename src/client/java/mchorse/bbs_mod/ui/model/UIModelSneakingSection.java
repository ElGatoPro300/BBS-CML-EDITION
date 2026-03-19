package mchorse.bbs_mod.ui.model;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.context.UIContextMenu;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.pose.UIPoseEditor;
import mchorse.bbs_mod.ui.utils.presets.UIDataContextMenu;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.pose.PoseManager;

import java.util.List;

public class UIModelSneakingSection extends UIModelSection
{
    public UIButton menu;
    public UIButton lookAtLimb;

    public UIModelSneakingSection(UIModelPanel editor)
    {
        super(editor);

        this.title.label = UIKeys.MODELS_SNEAKING_TITLE;

        this.menu = new UIButton(UIKeys.MODELS_PICK_SNEAKING_POSE, (b) ->
            {
                if (this.config == null)
                {
                    return;
                }

                String group = this.config.poseGroup.get();

                if (group.isEmpty())
                {
                    group = this.config.getId();
                }

                UIDataContextMenu menu = new UIDataContextMenu(PoseManager.INSTANCE, group, () ->
                {
                    BaseType data = this.config.sneakingPose.toData();
                    return data.isMap() ? data.asMap() : new MapType();
                }, (data) ->
                {
                    this.config.sneakingPose.fromData(data);
                    this.editor.dirty();
                });

                menu.remove(menu.row);
                menu.entries.relative(menu).y(5).h(1F, -10);

                this.getContext().setContextMenu(menu);
            });

        this.lookAtLimb = new UIButton(UIKeys.MODELS_PICK_LOOK_AT_LIMB, (b) ->
        {
            if (this.config != null)
            {
                UIPoseEditor poseEditor = this.editor.getPoseEditor();

                if (poseEditor != null)
                {
                    UILimbContextMenu menu = new UILimbContextMenu(poseEditor.groupsList.getList(), (limb) ->
                    {
                        this.config.lookAtHead.set(limb);
                        this.editor.dirty();
                    });

                    String current = this.config.lookAtHead.get();
                    menu.list.list.setCurrent(current.isEmpty() ? "<none>" : current);
                    this.getContext().setContextMenu(menu);
                }
            }
        });

        UILabel lookAtLabel = UI.label(UIKeys.MODELS_LOOK_AT_LIMB).background(() -> Colors.A50 | BBSSettings.primaryColor.get());
        lookAtLabel.marginTop(15);

        this.fields.add(this.menu);
        this.fields.add(lookAtLabel, this.lookAtLimb);
    }

    public static class UILimbContextMenu extends UIContextMenu
    {
        public UISearchList<String> list;

        public UILimbContextMenu(List<String> bones, java.util.function.Consumer<String> callback)
        {
            this.list = new UISearchList<>(new UIStringList((l) ->
            {
                String selected = l.get(0);
                callback.accept(selected.equals("<none>") ? "" : selected);
                this.removeFromParent();
            }));
            this.list.list.add("<none>");
            this.list.list.add(bones);
            this.list.list.sort();
            this.list.relative(this).xy(5, 5).w(1F, -10).h(1F, -10);
            this.list.search.placeholder(UIKeys.POSE_CONTEXT_NAME);

            this.add(this.list);
        }

        @Override
        public boolean isEmpty()
        {
            return false;
        }

        @Override
        public void setMouse(UIContext context)
        {
            this.xy(context.mouseX(), context.mouseY()).w(160).h(200).bounds(context.menu.overlay, 5);
        }
    }

    @Override
    public IKey getTitle()
    {
        return UIKeys.MODELS_SNEAKING_TITLE;
    }
}
