package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.morphing.IMorphProvider;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.entity.IEntityFormProvider;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import mchorse.bbs_mod.entity.IEntityFormProvider;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.morphing.IMorphProvider;
import net.minecraft.entity.Entity;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin
{
    @Inject(method = "writeData", at = @At("HEAD"))
    public void onWriteData(WriteView nbt, CallbackInfo info)
    {
        if (this instanceof IMorphProvider provider)
        {
            nbt.put("BBSMorph", NbtCompound.CODEC, (NbtCompound) provider.getMorph().toNbt());
        }
    }

    @Inject(method = "readData", at = @At("HEAD"))
    public void onReadData(ReadView nbt, CallbackInfo info)
    {
        if (this instanceof IMorphProvider provider)
        {
            var morphNbt = nbt.read("BBSMorph", NbtCompound.CODEC);
            if (morphNbt.isPresent())
            {
                provider.getMorph().fromNbt(morphNbt.get());
            }
        }
    }

    @Inject(method = "getEyeHeight", at = @At("HEAD"), cancellable = true)
    public void getEyeHeight(EntityPose pose, CallbackInfoReturnable<Float> info)
    {
        if (this instanceof IMorphProvider provider)
        {
            Morph morph = provider.getMorph();

            if (morph != null)
            {
                Form form = morph.getForm();

                if (form != null && form.hitbox.get())
                {
                    PlayerEntity player = (PlayerEntity) (Object) this;
                    float height = form.hitboxHeight.get() * (player.isSneaking() ? form.hitboxSneakMultiplier.get() : 1F);

                    info.setReturnValue(form.hitboxEyeHeight.get() * height);
                }
            }
        }
        else if (this instanceof IEntityFormProvider provider)
        {
            Form form = provider.getForm();

            if (form != null && form.hitbox.get())
            {
                Entity entity = (Entity) (Object) this;
                float height = form.hitboxHeight.get() * (entity.isSneaking() ? form.hitboxSneakMultiplier.get() : 1F);

                info.setReturnValue(form.hitboxEyeHeight.get() * height);
            }
        }
    }
    @Inject(method = "isCollidable", at = @At("HEAD"), cancellable = true)
    public void onIsCollidable(CallbackInfoReturnable<Boolean> info)
    {
        if ((Object) this instanceof IMorphProvider provider)
        {
            Form form = provider.getMorph().getForm();

            if (form != null && form.hitbox.get())
            {
                info.setReturnValue(true);
            }
        }
        else if ((Object) this instanceof IEntityFormProvider provider)
        {
            Form form = provider.getForm();

            if (form != null && form.hitbox.get())
            {
                info.setReturnValue(true);
            }
        }
    }

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    public void onIsPushable(CallbackInfoReturnable<Boolean> info)
    {
        if ((Object) this instanceof IMorphProvider provider)
        {
            Form form = provider.getMorph().getForm();

            if (form != null && form.hitbox.get())
            {
                info.setReturnValue(false);
            }
        }
        else if ((Object) this instanceof IEntityFormProvider provider)
        {
            Form form = provider.getForm();

            if (form != null && form.hitbox.get())
            {
                info.setReturnValue(false);
            }
        }
    }
}
