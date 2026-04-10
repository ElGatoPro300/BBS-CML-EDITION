package mchorse.bbs_mod.mixin.client.audio;

import com.mojang.blaze3d.audio.DeviceList;
import com.mojang.blaze3d.audio.DeviceTracker;
import com.mojang.blaze3d.audio.Library;
import com.mojang.blaze3d.audio.PollingDeviceTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Library.class)
public class LibraryMixin
{
    @Inject(method = "createDeviceTracker", at = @At("HEAD"), cancellable = true)
    private static void bbs$forcePollingTracker(CallbackInfoReturnable<DeviceTracker> cir)
    {
        cir.setReturnValue(new PollingDeviceTracker(DeviceList.query()));
    }
}
