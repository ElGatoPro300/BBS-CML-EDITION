package mchorse.bbs_mod.ui.utils.context;

import com.mojang.blaze3d.vertex.PoseStack;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.world.item.ItemStack;

public class ItemStackContextAction extends ContextAction
{
    public ItemStack stack = ItemStack.EMPTY;

    public ItemStackContextAction(ItemStack stack, IKey label, Runnable runnable)
    {
        super(Icons.NONE, label, runnable);

        this.stack = stack;
    }

    @Override
    public void render(UIContext context, FontRenderer font, int x, int y, int w, int h, boolean hover, boolean selected)
    {
        this.renderBackground(context, x, y, w, h, hover, selected);

        if (this.stack != null && !this.stack.isEmpty())
        {
            PoseStack matrices = new PoseStack();
            CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();

            matrices.pushPose();
            consumers.setUI(true);
            context.batcher.getContext().item(this.stack, x + 2, y + 2);
            context.batcher.getContext().itemDecorations(context.batcher.getFont().getRenderer(), this.stack, x + 2, y + 2);
            consumers.setUI(false);
            matrices.popPose();
        }

        context.batcher.text(this.label.get(), x + 22, y + (h - font.getHeight()) / 2 + 1, Colors.WHITE, false);
    }
}

