package mchorse.bbs_mod.ui.dashboard.panels.overlay;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.utils.colors.Colors;

public class UIAboutOverlayPanel extends UIOverlayPanel
{
    public UIAboutOverlayPanel(IKey title, UIDashboard dashboard)
    {
        super(title);

        UIElement layout = new UIElement();
        layout.relative(this.content).full(this.content).column(5).vertical().stretch().padding(10);

        UIElement header = new UIElement()
        {
            @Override
            public void render(UIContext context)
            {
                int color = BBSSettings.primaryColor.get();

                /* Draw background gradient */
                context.batcher.gradientHBox(this.area.x, this.area.y, this.area.ex(), this.area.ey(), color | Colors.A25, 0);
                
                /* Draw brand title */
                String brand = "BBS";
                int y = this.area.my(context.batcher.getFont().getHeight());

                context.batcher.textShadow(brand, this.area.x, y, Colors.WHITE);
                
                super.render(context);
            }
        };
        header.h(16);

        UIElement info = new UIElement();
        info.column(2).vertical().stretch();

        UILabel version = new UILabel(IKey.raw("Version: " + BBSMod.VERSION)).background();
        version.h(16);

        info.add(version);
        info.marginTop(6);

        layout.add(header, info);
        this.content.add(layout);
    }
}
