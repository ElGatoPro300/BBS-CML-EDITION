package mchorse.bbs_mod.ui.forms.editors.panels;

import mchorse.bbs_mod.forms.forms.BlockForm;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIBlockStateEditor;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.colors.Color;

import net.minecraft.block.BlockState;

public class UIBlockFormPanel extends UIFormPanel<BlockForm>
{
    public UIColor color;
    public UIBlockStateEditor stateEditor;
    public UITrackpad breaking;

    public UIBlockFormPanel(UIForm editor)
    {
        super(editor);

        this.color = new UIColor((c) -> this.form.color.set(Color.rgba(c))).withAlpha();
        this.stateEditor = new UIBlockStateEditor((blockState) -> this.form.blockState.set(blockState));
        this.breaking = new UITrackpad((v) -> this.form.breaking.set(v.intValue())).integer().limit(0, 10);
        this.breaking.tooltip(UIKeys.FORMS_EDITORS_BLOCK_BREAKING);

        this.options.add(this.color, this.stateEditor);
        this.options.add(UI.label(UIKeys.FORMS_EDITORS_BLOCK_BREAKING).marginTop(6), this.breaking);
    }

    @Override
    public void startEdit(BlockForm form)
    {
        super.startEdit(form);

        BlockState blockState = this.form.blockState.get();

        this.color.setColor(form.color.get().getARGBColor());
        this.stateEditor.setBlockState(blockState);
        this.breaking.setValue(form.breaking.get());
    }
}