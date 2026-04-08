package mchorse.bbs_mod.ui.triggers;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.settings.values.core.ValueList;
import mchorse.bbs_mod.triggers.Trigger;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.UIFormPalette;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIList;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIEditorOverlayPanel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

public class UITriggerOverlayPanel extends UIEditorOverlayPanel<Trigger>
{
    private ValueList<Trigger> triggers;
    private Runnable onClose;

    public UITriggerOverlayPanel(ValueList<Trigger> triggers, Runnable onClose)
    {
        this(triggers);
        this.onClose = onClose;
    }

    public UITriggerOverlayPanel(ValueList<Trigger> triggers)
    {
        super(TriggerKeys.TITLE);

        this.triggers = triggers;
        this.list.add(triggers.getAllTyped());
        this.pickItem(triggers.getAllTyped().isEmpty() ? null : triggers.getAllTyped().get(0), true);
    }

    @Override
    public void close()
    {
        super.close();

        if (this.onClose != null)
        {
            this.onClose.run();
        }
    }

    @Override
    protected IKey getAddLabel()
    {
        return TriggerKeys.ADD_TRIGGER;
    }

    @Override
    protected IKey getRemoveLabel()
    {
        return TriggerKeys.REMOVE_TRIGGER;
    }

    @Override
    protected UIList<Trigger> createList()
    {
        return new UIList<Trigger>((l) -> this.pickItem(l.get(0), false))
        {
            @Override
            protected String elementToString(UIContext context, int i, Trigger element)
            {
                String type = element.type.get();

                if (type.equals("command"))
                {
                    String command = element.command.get();

                    return command.startsWith("/") ? command : "/" + command;
                }
                else if (type.equals("form"))
                {
                    Form form = element.form.get();

                    if (form == null)
                    {
                        return UIKeys.MORPHING_DEMORPH.get();
                    }

                    return TriggerKeys.MORPH_INTO.format(form.getDisplayName()).get();
                }
                else if (type.equals("block"))
                {
                    Form form = element.blockForm.get();

                    if (form == null)
                    {
                        return TriggerKeys.REMOVE_FORM.get();
                    }

                    return TriggerKeys.ADD_FORM_NAME.format(form.getDisplayName()).get();
                }

                return type;
            }

            @Override
            protected void renderElementPart(UIContext context, Trigger element, int i, int x, int y, boolean hover, boolean selected)
            {
                int color = Colors.WHITE & Colors.RGB;
                String type = element.type.get();

                if (type.equals("block"))
                {
                    color = Colors.ACTIVE;
                }
                else if (type.equals("command"))
                {
                    color = 0xA020F0;
                }
                else if (type.equals("form"))
                {
                    color = Colors.ORANGE;
                }

                context.batcher.box(x, y, x + 2, y + this.scroll.scrollItemSize, Colors.A100 | color);
                context.batcher.gradientHBox(x + 2, y, x + 24, y + this.scroll.scrollItemSize, Colors.A25 | color, color);

                super.renderElementPart(context, element, i, x + 4, y, hover, selected);
            }
        };
    }

    @Override
    protected void addItem()
    {
        UIContext context = this.getContext();

        context.replaceContextMenu((menu) ->
        {
            menu.action(Icons.CONSOLE, TriggerKeys.ADD_COMMAND, 0xA020F0, () -> this.addTrigger("command"));
            menu.action(Icons.MORPH, TriggerKeys.ADD_FORM, Colors.ORANGE, () -> this.addTrigger("form"));
            menu.action(Icons.BLOCK, TriggerKeys.ADD_BLOCK, Colors.ACTIVE, () -> this.addTrigger("block"));
        });
    }

    @Override
    protected void fillData(Trigger item)
    {
        this.editor.removeAll();

        String type = item.type.get();

        if (type.equals("command"))
        {
            UITextbox command = new UITextbox(1000, (t) -> item.command.set(t));
            command.setText(item.command.get());

            this.editor.add(UI.label(TriggerKeys.ACTION_COMMAND), command);
        }
        else if (type.equals("form"))
        {
            UIButton edit = new UIButton(UIKeys.GENERAL_EDIT, (b) ->
            {
                UIFormPalette.open(this.getParentContainer(), true, item.form.get(), (f) ->
                {
                    item.form.set(f);
                });
            });

            edit.setEnabled(item.form.get() != null);

            UIButton pick = new UIButton(UIKeys.GENERAL_PICK, (b) ->
            {
                UIFormPalette.open(this.getParentContainer(), false, item.form.get(), (f) ->
                {
                    item.form.set(f);
                    edit.setEnabled(f != null);

                    if (f == null)
                    {
                        PlayerEntity player = MinecraftClient.getInstance().player;

                        if (player != null)
                        {
                            Morph morph = Morph.getMorph(player);

                            if (morph != null)
                            {
                                morph.setForm(null);
                            }
                        }
                    }
                });
            });

            this.editor.add(UI.label(TriggerKeys.ACTION_FORM), UI.row(pick, edit));
        }
        else if (type.equals("block"))
        {
            UIButton edit = new UIButton(UIKeys.GENERAL_EDIT, (b) ->
            {
                UIFormPalette.open(this.getParentContainer(), true, item.blockForm.get(), (f) ->
                {
                    item.blockForm.set(f);
                });
            });

            edit.setEnabled(item.blockForm.get() != null);

            UIButton pick = new UIButton(UIKeys.GENERAL_PICK, (b) ->
            {
                UIFormPalette.open(this.getParentContainer(), false, item.blockForm.get(), (f) ->
                {
                    item.blockForm.set(f);
                    edit.setEnabled(f != null);
                });
            });

            UITrackpad x = new UITrackpad((v) -> item.x.set(v.intValue()));
            x.setValue(item.x.get());
            x.integer();

            UITrackpad y = new UITrackpad((v) -> item.y.set(v.intValue()));
            y.setValue(item.y.get());
            y.integer();

            UITrackpad z = new UITrackpad((v) -> item.z.set(v.intValue()));
            z.setValue(item.z.get());
            z.integer();

            this.editor.add(UI.label(TriggerKeys.ACTION_BLOCK_POS));
            this.editor.add(UI.row(x, y, z));
            this.editor.add(UI.label(TriggerKeys.ACTION_BLOCK_FORM), UI.row(pick, edit));
        }
    }

    private void addTrigger(String type)
    {
        Trigger trigger = new Trigger("");

        trigger.type.set(type);
        this.triggers.add(trigger);
        this.list.add(trigger);
        this.list.update();
        this.pickItem(trigger, true);
    }

    @Override
    protected void removeItem()
    {
        if (this.item != null)
        {
            this.triggers.getAllTyped().remove(this.item);
        }

        super.removeItem();
    }
}
