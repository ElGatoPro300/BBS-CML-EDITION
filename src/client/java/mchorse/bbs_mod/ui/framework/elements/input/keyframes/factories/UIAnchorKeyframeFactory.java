package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import io.netty.util.collection.IntObjectMap;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.utils.Anchor;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class UIAnchorKeyframeFactory extends UIKeyframeFactory<Anchor>
{
    private UIButton actor;
    private UIButton attachment;
    private UIToggle translate;
    private UIToggle scale;

    public static void displayActors(UIContext context, IntObjectMap<IEntity> entities, int value, Consumer<Integer> callback)
    {
        List<UIFilmPanel> children = context.menu.main.getChildren(UIFilmPanel.class);
        UIFilmPanel panel = children.isEmpty() ? null : children.get(0);
        List<Replay> replays = panel != null ? panel.getData().replays.getList() : null;

        context.replaceContextMenu((menu) ->
        {
            menu.action(Icons.CLOSE, UIKeys.GENERAL_NONE, Colors.NEGATIVE, () -> callback.accept(-1));

            for (int i = 0; i < entities.size(); i++)
            {
                final int actor = i;
                IEntity entity = entities.get(i);

                if (entity == null)
                {
                    continue;
                }

                Replay replay = replays == null ? null : replays.get(i);
                Form form = entity.getForm();
                String stringLabel = i + (replay != null ? " - " + replay.getName() : (form == null ? "" : " - " + form.getFormIdOrName()));
                IKey label = IKey.constant(stringLabel);

                menu.action(Icons.CLOSE, label, actor == value, () -> callback.accept(actor));
            }
        });
    }

    public static void displayAttachments(UIFilmPanel panel, int index, String value, Consumer<String> consumer)
    {
        IEntity entity = panel.getController().getEntities().get(index);

        if (entity == null || entity.getForm() == null)
        {
            return;
        }

        Form form = entity.getForm();
        List<String> attachments = new ArrayList<>(FormUtilsClient.getRenderer(form).collectMatrices(entity, 0F).keySet());

        for (int i = attachments.size() - 1; i >= 0; i--)
        {
            String name = attachments.get(i);
            if (name.endsWith("#origin"))
            {
                attachments.remove(i);
            }
        }

        attachments.sort(String::compareToIgnoreCase);

        boolean fallbackToBones = attachments.isEmpty() || (attachments.size() == 1 && attachments.get(0).isEmpty());

        if (fallbackToBones)
        {
            List<String> bones = FormUtilsClient.getRenderer(form).getBones();

            if (bones.isEmpty())
            {
                return;
            }

            attachments = new ArrayList<>(bones);
        }

        /* Collect labels (substitute track names) */
        List<String> labels = new ArrayList<>(attachments);

        if (!fallbackToBones)
        {
            for (int i = 0; i < labels.size(); i++)
            {
                String label = labels.get(i);
                Form path = FormUtils.getForm(form, label);

                if (path != null)
                {
                    labels.set(i, path.getTrackName(label));
                }
            }
        }

        if (attachments.isEmpty())
        {
            return;
        }

        String normalized = value == null ? null : value.replace("#origin", "");
        final List<String> attachmentsFinal = attachments;
        final List<String> labelsFinal = labels;

        panel.getContext().replaceContextMenu((menu) ->
        {
            for (int i = 0; i < attachmentsFinal.size(); i++)
            {
                String attachment = attachmentsFinal.get(i);
                String label = labelsFinal.get(i);

                menu.action(Icons.LIMB, IKey.constant(label), attachment.equals(normalized), () -> consumer.accept(attachment));
            }
        });
    }

    public UIAnchorKeyframeFactory(Keyframe<Anchor> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.actor = new UIButton(UIKeys.GENERIC_KEYFRAMES_ANCHOR_PICK_ACTOR, (b) -> this.displayActors());
        this.attachment = new UIButton(UIKeys.GENERIC_KEYFRAMES_ANCHOR_PICK_ATTACHMENT, (b) ->
        {
            displayAttachments(this.getPanel(), this.keyframe.getValue().replay, this.keyframe.getValue().attachment, this::setAttachment);
        });
        this.translate = new UIToggle(UIKeys.TRANSFORMS_TRANSLATE, (b) -> this.setTranslate(b.getValue()));
        this.translate.setValue(keyframe.getValue().translate);
        this.scale = new UIToggle(UIKeys.TRANSFORMS_SCALE, (b) -> this.setScale(b.getValue()));
        this.scale.setValue(keyframe.getValue().scale);

        this.scroll.add(this.actor, this.attachment, this.translate, this.scale);
    }

    private void displayActors()
    {
        UIFilmPanel panel = this.getPanel();

        displayActors(this.getContext(), panel.getController().getEntities(), this.keyframe.getValue().replay, this::setActor);
    }

    private void setActor(int actor)
    {
        BaseValue.edit(this.keyframe, (value) -> value.getValue().replay = actor);
    }

    private void setAttachment(String attachment)
    {
        BaseValue.edit(this.keyframe, (value) -> value.getValue().attachment = attachment);
    }

    private void setTranslate(boolean translate)
    {
        BaseValue.edit(this.keyframe, (value) -> value.getValue().translate = translate);
    }

    private void setScale(boolean scale)
    {
        BaseValue.edit(this.keyframe, (value) -> value.getValue().scale = scale);
    }

    private UIFilmPanel getPanel()
    {
        return this.getParent(UIFilmPanel.class);
    }
}
