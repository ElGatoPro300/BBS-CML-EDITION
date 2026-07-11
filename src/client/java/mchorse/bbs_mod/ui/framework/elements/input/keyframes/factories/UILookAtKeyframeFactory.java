package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.utils.LookAt;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.elements.input.UILookAtEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.utils.keyframes.Keyframe;

import java.util.ArrayList;
import java.util.List;

public class UILookAtKeyframeFactory extends UIKeyframeFactory<LookAt>
{
    private UILookAtEditor lookAtEditor;

    public UILookAtKeyframeFactory(Keyframe<LookAt> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.lookAtEditor = new UILookAtEditor();
        this.lookAtEditor.callbacks(
            () -> this.keyframe.getValue(),
            (consumer) -> BaseValue.edit(this.keyframe, (value) -> consumer.accept(value.getValue()))
        );
        this.lookAtEditor.fillBones(this.collectBones());

        this.scroll.add(this.lookAtEditor);
    }

    /**
     * This replay's model bones (like the pose editor does).
     */
    private List<String> collectBones()
    {
        List<String> bones = new ArrayList<>();
        UIFilmPanel panel = this.editor.getParent(UIFilmPanel.class);

        if (panel == null)
        {
            return bones;
        }

        int index = panel.replayEditor.replays.replays.getIndex();
        IEntity entity = panel.getController().getEntities().get(index);

        if (entity == null || entity.getForm() == null)
        {
            return bones;
        }

        bones.addAll(FormUtilsClient.getRenderer(entity.getForm()).collectMatrices(entity, 0F).keySet());

        return bones;
    }

    @Override
    public void update()
    {
        super.update();

        this.lookAtEditor.refresh();
    }
}
