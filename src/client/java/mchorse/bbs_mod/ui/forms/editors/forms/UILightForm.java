package mchorse.bbs_mod.ui.forms.editors.forms;

import mchorse.bbs_mod.forms.forms.LightForm;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.forms.editors.panels.UILightFormPanel;
import mchorse.bbs_mod.ui.utils.icons.Icons;

public class UILightForm extends UIForm<LightForm>
{
    public UILightForm()
    {
        super();

        this.defaultPanel = new UILightFormPanel(this);

        this.registerPanel(this.defaultPanel, L10n.lang("bbs.ui.raw.light"), Icons.LIGHT);
        this.registerDefaultPanels();
    }
}

