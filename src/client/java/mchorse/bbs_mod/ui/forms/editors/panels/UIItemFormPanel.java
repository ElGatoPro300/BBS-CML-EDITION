package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.forms.forms.ItemForm;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIItemStack;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Color;
import net.minecraft.world.item.ItemDisplayContext;

public class UIItemFormPanel extends UIFormPanel<ItemForm>
{
    public UIColor color;
    public UIButton modelTransform;
    public UIItemStack itemStackEditor;

    public UIItemFormPanel(UIForm editor)
    {
        super(editor);

        this.color = new UIColor((c) -> this.form.color.set(Color.rgba(c))).withAlpha();
        this.modelTransform = new UIButton(IKey.EMPTY, (b) ->
        {
            this.getContext().replaceContextMenu((menu) ->
            {
                for (ItemDisplayContext value : ItemDisplayContext.values())
                {
                    if (this.form.modelTransform.get() == value)
                    {
                        menu.action(Icons.LINE, IKey.constant(value.getSerializedName()), true, () -> {});
                    }
                    else
                    {
                        menu.action(Icons.LINE, IKey.constant(value.getSerializedName()), () -> this.setModelTransform(value));
                    }
                }
            });
        });

        this.itemStackEditor = new UIItemStack((itemStack) -> this.form.stack.set(itemStack.copy()));

        this.options.add(this.color, UI.label(UIKeys.FORMS_EDITORS_ITEM_TRANSFORMS), this.modelTransform, this.itemStackEditor);
    }

    private void setModelTransform(ItemDisplayContext value)
    {
        this.form.modelTransform.set(value);

        this.modelTransform.label = IKey.constant(value.getSerializedName());
    }

    @Override
    public void startEdit(ItemForm form)
    {
        super.startEdit(form);

        this.color.setColor(form.color.get().getARGBColor());
        this.modelTransform.label = IKey.constant(form.modelTransform.get().getSerializedName());
        this.itemStackEditor.setStack(form.stack.get());
    }
}
