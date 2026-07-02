package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.morphing.IMorphProvider;
import mchorse.bbs_mod.morphing.Morph;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * For some unknown reason to me, if these methods are used in {@link PlayerEntityMorphMixin}
 * then the world will be locked for some reason... by extracting write/read NBT method to
 * a separate mixin fixes it...
 */
@Mixin(PlayerEntity.class)
public class PlayerEntityMixin
{
    @Inject(method = "writeCustomData", at = @At("TAIL"))
    public void onWriteCustomData(WriteView view, CallbackInfo info)
    {
        if (this instanceof IMorphProvider provider)
        {
            view.putString("BBSMorph", provider.getMorph().toNbt().toString());
        }
    }

    @Inject(method = "readCustomData", at = @At("TAIL"))
    public void onReadCustomData(ReadView view, CallbackInfo info)
    {
        if (this instanceof IMorphProvider provider)
        {
            view.getOptionalString("BBSMorph").ifPresent((serialized) -> {
                try
                {
                    provider.getMorph().fromNbt(StringNbtReader.readCompound(serialized));
                }
                catch (Exception ignored)
                {}
            });
        }
    }

}