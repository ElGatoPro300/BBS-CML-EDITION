package mchorse.bbs_mod.mixin.client;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSRendering;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardMixin
{
    @Inject(method = "keyPress", at = @At("HEAD"))
    public void onOnKey(long window, int key, KeyEvent input, CallbackInfo info)
    {
        BBSRendering.lastAction = input.isDown() ? 1 : 0;
    }

    @Inject(method = "keyPress", at = @At("TAIL"))
    public void onOnEndKey(long window, int key, KeyEvent input, CallbackInfo info)
    {
        BBSModClient.onEndKey(window, key, input.scancode(), input.isDown() ? 1 : 0, input.modifiers(), info);
    }
}
