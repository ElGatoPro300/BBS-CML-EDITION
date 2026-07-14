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
import org.spongepowered.asm.mixin.Unique;
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

    @Unique
    private BBSLogoButtonWidget bbs$logoButton;

    @Inject(method = "init", at = @At("TAIL"))
    private void bbs$addSelectWorldBbsButton(CallbackInfo ci)
    {
        if (!((Object) this instanceof SelectWorldScreen))
        {
            return;
        }

        this.bbs$logoButton = null;
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

    @Unique
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

        if (this.bbs$logoButton == null)
        {
            this.bbs$logoButton = new BBSLogoButtonWidget(x, y, size, size, (button) ->
            {
                UIDashboard dashboard = BBSModClient.getDashboard();

                dashboard.setPanel(dashboard.getPanel(UIWorldFilmsBrowserPanel.class));
                UIScreen.open(dashboard);
            });

            this.addDrawableChild(this.bbs$logoButton);
        }
        else
        {
            this.bbs$logoButton.setX(x);
            this.bbs$logoButton.setY(y);
            this.bbs$logoButton.setWidth(size);
            this.bbs$logoButton.setHeight(size);
        }
    }
}
