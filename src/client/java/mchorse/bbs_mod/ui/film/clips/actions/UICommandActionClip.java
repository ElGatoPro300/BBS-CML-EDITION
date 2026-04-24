package mchorse.bbs_mod.ui.film.clips.actions;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.actions.types.chat.CommandActionClip;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextarea;
import mchorse.bbs_mod.ui.framework.elements.input.text.undo.TextEditUndo;
import mchorse.bbs_mod.ui.framework.elements.input.text.utils.TextLine;
import mchorse.bbs_mod.ui.utils.UI;

public class UICommandActionClip extends UIActionClip<CommandActionClip>
{
    public UITextarea<TextLine> command;

    public UICommandActionClip(CommandActionClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.command = new UITextarea<>((t) -> this.clip.command.set(t.replace("\n", "")))
        {
            @Override

            @Override
            public void writeString(String string)
            {
                super.writeString(string.replace("\n", ""));
            }
        };
        this.command.w(BBSSettings.editorCommandWidth.get());
        this.command.h(BBSSettings.editorCommandHeight.get());
        this.command.wrap(BBSSettings.editorCommandAutoWrap.get());
        this.command.background();
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();
        this.panels.add(UI.label(UIKeys.ACTIONS_COMMAND_COMMAND).marginTop(12), this.command);
    }

    @Override
    public void fillData()
    {
        super.fillData();

        this.command.setText(this.clip.command.get());
        this.updateCommandHeight();
    }

    @Override
    public void render(mchorse.bbs_mod.ui.framework.UIContext context)
    {
        this.updateCommandHeight();
        super.render(context);
    }

    private void updateCommandHeight()
    {
        int wrappedLines = 0;

        for (TextLine line : this.command.getLines())
        {
            wrappedLines += Math.max(1, line.getLines());
        }

        boolean empty = this.command.getText().isEmpty();
        int desired = empty
            ? BASE_COMMAND_HEIGHT
            : Math.max(BASE_COMMAND_HEIGHT, wrappedLines * COMMAND_LINE_HEIGHT + COMMAND_PADDING);

        if (this.command.area.h != desired)
        {
            this.command.h(desired);
            this.panels.resize();
        }
    }
}
