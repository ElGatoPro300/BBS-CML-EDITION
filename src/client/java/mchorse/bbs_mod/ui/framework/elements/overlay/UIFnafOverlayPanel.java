package mchorse.bbs_mod.ui.framework.elements.overlay;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UIScrollView;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.framework.elements.utils.UIText;
import mchorse.bbs_mod.ui.news.UINewsPanel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.colors.Colors;

public class UIFnafOverlayPanel extends UIOverlayPanel
{
    public UIFnafOverlayPanel(IKey message, IKey messageSmall)
    {
        super(UIKeys.PRIORITY_ANNOUNCEMENT_TITLE);

        this.content.removeAll();

        UIScrollView scroll = UI.scrollView(6, 8);
        scroll.relative(this.content).full(this.content);

        UILabel title = new UILabel(message);
        title.color(Colors.WHITE);
        title.h(16);

        UIText body = new UIText(messageSmall);
        body.color(Colors.LIGHTER_GRAY, true).padding(0, 2).lineHeight(12);

        UIText subText = new UIText(IKey.raw("(OBS: O update lançou tarde.)"));
        subText.color(Colors.GRAY, true).padding(0, 2).lineHeight(10);

        Link jokeImage = Link.assets("textures/banners/JokeFnaF.png");
        UINewsPanel.UINewsImage image = new UINewsPanel.UINewsImage(jokeImage, 180);

        UIElement column = UI.column(6, title, body, subText, image).marginTop(6);
        scroll.add(column);

        this.content.add(scroll);
    }
}
