package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.morphing.IMorphProvider;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * For some unknown reason to me, if these methods are used in {@link PlayerEntityMorphMixin}
 * then the world will be locked for some reason... by extracting write/read NBT method to
 * a separate mixin fixes it...
 */
@Mixin(Player.class)
public class PlayerEntityMixin
{
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    public void onWriteCustomData(ValueOutput view, CallbackInfo info)
    {
        if (this instanceof IMorphProvider provider)
        {
            view.putString("BBSMorph", provider.getMorph().toNbt().toString());
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    public void onReadCustomData(ValueInput view, CallbackInfo info)
    {
        if (this instanceof IMorphProvider provider)
        {
            view.getString("BBSMorph").ifPresent((serialized) -> {
                try
                {
                    provider.getMorph().fromNbt(TagParser.parseCompoundFully(serialized));
                }
                catch (Exception ignored)
                {}
            });
        }
    }

}