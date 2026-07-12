package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.gui.BBSLogoButtonWidget;
import mchorse.bbs_mod.ui.dashboard.UIDashboard;
import mchorse.bbs_mod.ui.film.UIWorldFilmsBrowserPanel;
import mchorse.bbs_mod.ui.framework.UIScreen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class SelectWorldScreenMixin
{
    @Shadow
    protected int width;

    @Shadow
    protected int height;

    @Shadow
    protected abstract <T extends Element & Drawable & Selectable> T addDrawableChild(T drawableElement);

    @Inject(method = "init", at = @At("TAIL"))
    private void bbs$addSelectWorldBbsButton(CallbackInfo ci)
    {
        if (!((Object) this instanceof SelectWorldScreen))
        {
            return;
        }

        this.bbs$ensureBbsButton();
    }

    @Inject(method = "resize", at = @At("TAIL"))
    private void bbs$resizeSelectWorldBbsButton(MinecraftClient client, int width, int height, CallbackInfo ci)
    {
        if (!((Object) this instanceof SelectWorldScreen))
        {
            return;
        }

        this.bbs$ensureBbsButton();
    }

    private BBSLogoButtonWidget bbs$findBbsButton()
    {
        Screen screen = (Screen) (Object) this;

        for (Element element : screen.children())
        {
            if (element instanceof BBSLogoButtonWidget widget)
            {
                return widget;
            }
        }

        return null;
    }

    private void bbs$ensureBbsButton()
    {
        if (MinecraftClient.getInstance().world != null)
        {
            return;
        }

        int size = 20;
        int vanillaLeft = this.width / 2 - 154;
        int x = Math.max(4, vanillaLeft - size - 4);
        int y = this.height - 52;
        BBSLogoButtonWidget button = this.bbs$findBbsButton();

        if (button == null)
        {
            button = new BBSLogoButtonWidget(x, y, size, size, (widget) ->
            {
                SelectWorldScreen selectWorld = (SelectWorldScreen) (Object) this;
                UIDashboard dashboard = BBSModClient.getDashboard();

                dashboard.setReturnScreen(selectWorld);
                dashboard.setPanel(dashboard.getPanel(UIWorldFilmsBrowserPanel.class));
                UIScreen.open(dashboard);
            });

            this.addDrawableChild(button);
        }
        else
        {
            button.setX(x);
            button.setY(y);
            button.setWidth(size);
            button.setHeight(size);
        }
    }
}
