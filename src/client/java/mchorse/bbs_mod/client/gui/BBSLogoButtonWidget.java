package mchorse.bbs_mod.client.gui;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.input.AbstractInput;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class BBSLogoButtonWidget extends PressableWidget
{
    private static final Identifier LOGO = Identifier.of("bbs", "textures/gui/bbs_logo.png");

    private final Runnable action;

    public BBSLogoButtonWidget(int x, int y, int width, int height, Runnable action)
    {
        super(x, y, width, height, Text.literal(" "));

        this.action = action;
    }

    @Override
    public void onPress(AbstractInput input)
    {
        this.action.run();
    }

    @Override
    public void appendClickableNarrations(NarrationMessageBuilder builder)
    {
    }

    @Override
    protected void drawIcon(DrawContext context, int x, int y, float alpha)
    {
        int logoSize = Math.min(this.width, this.height) - 6;
        int logoX = x + (this.width - logoSize) / 2;
        int logoY = y + (this.height - logoSize) / 2;

        context.drawTexture(RenderPipelines.GUI_TEXTURED, LOGO, logoX, logoY, 0f, 0f, logoSize, logoSize, logoSize, logoSize);
    }
}
