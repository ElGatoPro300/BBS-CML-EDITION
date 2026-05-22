package mchorse.bbs_mod.ui.framework.elements.buttons;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.ITextColoring;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.function.Consumer;

public class UIToggle extends UIClickable<UIToggle> implements ITextColoring
{
    public IKey label;
    public int color = Colors.WHITE;
    public boolean textShadow = true;
    private boolean value;

    public UIToggle(IKey label, Consumer<UIToggle> callback)
    {
        this(label, false, callback);
    }

    public UIToggle(IKey label, boolean value, Consumer<UIToggle> callback)
    {
        super(callback);

        this.label = label;
        this.value = value;
        this.h(14);
    }

    @Override
    public void setColor(int color, boolean shadow)
    {
        this.color(color, shadow);
    }

    public UIToggle label(IKey label)
    {
        this.label = label;

        return this;
    }

    public UIToggle setValue(boolean value)
    {
        this.value = value;

        return this;
    }

    public UIToggle color(int color)
    {
        return this.color(color, true);
    }

    public UIToggle color(int color, boolean textShadow)
    {
        this.color = color;
        this.textShadow = textShadow;

        return this;
    }

    public boolean getValue()
    {
        return this.value;
    }

    @Override
    protected void click(int mouseWheel)
    {
        this.value = !this.value;

        super.click(mouseWheel);
    }

    @Override
    protected UIToggle get()
    {
        return this;
    }

    @Override
    protected void renderSkin(UIContext context)
    {
        Batcher2D batcher = context.batcher;
        FontRenderer font = batcher.getFont();

        /* Square (cube-shaped) toggle switch. */
        int w = 22;
        int h = 12;
        int x = this.area.ex() - w - 2;
        int y = this.area.my() - h / 2;

        String label = font.limitToWidth(this.label.get(), this.area.w - w - 8);
        batcher.text(label, this.area.x, this.area.my(font.getHeight()), this.color, this.textShadow);

        /* Track — primary color when on, dark grey when off, with a 1px border. */
        int primary = 0xFF000000 | BBSSettings.primaryColor.get();
        int trackColor;

        if (this.value)
        {
            trackColor = this.hover ? Colors.mulRGB(primary, 1.15F) : primary;
        }
        else
        {
            trackColor = this.hover ? 0xFF4A4A52 : 0xFF3B3B43;
        }

        batcher.box(x, y, x + w, y + h, trackColor);
        batcher.outline(x, y, x + w, y + h, 0xFF000000 | Colors.mulRGB(trackColor, 0.55F));

        /* Knob — a square block that slides to the right when on, with a
           1px drop shadow for depth. */
        int knobSize = h - 4;
        int knobX = this.value ? (x + w - knobSize - 2) : (x + 2);
        int knobY = y + 2;

        batcher.box(knobX, knobY + 1, knobX + knobSize, knobY + knobSize + 1, 0x66000000);
        batcher.box(knobX, knobY, knobX + knobSize, knobY + knobSize, 0xFFFFFFFF);
        batcher.box(knobX, knobY + knobSize - 2, knobX + knobSize, knobY + knobSize, 0xFFD2D2D6);

        if (!this.isEnabled())
        {
            batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xAA000000);
            batcher.outlinedIcon(Icons.LOCKED, this.area.mx(), this.area.my(), 0.5F, 0.5F);
        }
    }
}