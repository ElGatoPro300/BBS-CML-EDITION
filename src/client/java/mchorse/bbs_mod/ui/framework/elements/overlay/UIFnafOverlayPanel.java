package mchorse.bbs_mod.ui.framework.elements.overlay;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.utils.colors.Colors;

public class UIFnafOverlayPanel extends UIOverlayPanel
{
    public UIFnafOverlayPanel(IKey message, IKey messageSmall)
    {
        super(IKey.EMPTY);

        this.content.column().stretch();
        
        UIElement bigText = new UIElement()
        {
            @Override
            public void render(UIContext context)
            {
                context.batcher.getContext().getMatrices().push();
                context.batcher.getContext().getMatrices().translate(this.area.mx(), this.area.my() - 10, 0);
                context.batcher.getContext().getMatrices().scale(3.0F, 3.0F, 1.0F);
                
                String label = message.get();
                int w = context.batcher.getFont().getWidth(label);
                int h = context.batcher.getFont().getHeight();
                
                context.batcher.text(label, -w / 2, -h / 2, Colors.WHITE, true);
                
                context.batcher.getContext().getMatrices().pop();

                context.batcher.getContext().getMatrices().push();
                context.batcher.getContext().getMatrices().translate(this.area.mx(), this.area.my() + 30, 0);
                context.batcher.getContext().getMatrices().scale(1.2F, 1.2F, 1.0F);

                String labelSmall = messageSmall.get();
                int wSmall = context.batcher.getFont().getWidth(labelSmall);
                int hSmall = context.batcher.getFont().getHeight();

                context.batcher.text(labelSmall, -wSmall / 2, -hSmall / 2, Colors.WHITE, true);

                context.batcher.getContext().getMatrices().pop();
                
                super.render(context);
            }
        };
        
        this.content.add(bigText.full(this.content));
    }
}
