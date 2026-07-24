package mchorse.bbs_mod.mixin;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.actions.types.AttackActionClip;
import mchorse.bbs_mod.forms.structure.ModelBlockSolidCollisions;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin
{
    @Inject(method = "applyDamage", at = @At("HEAD"))
    public void onApplyDamage(DamageSource source, float amount, CallbackInfo info)
    {
        Entity attacker = source.getAttacker();

        if (source.isDirect() && attacker != null && attacker.getClass() == ServerPlayerEntity.class)
        {
            BBSMod.getActions().addAction((ServerPlayerEntity) attacker, () ->
            {
                AttackActionClip clip = new AttackActionClip();

                clip.damage.set(amount);

                return clip;
            });
        }
    }

    /**
     * LivingEntity overrides {@link Entity#getStepHeight()}, so the boost must live here
     * (not on Entity) or players never receive the higher step for short solid hitboxes.
     */
    @Inject(method = "getStepHeight", at = @At("RETURN"), cancellable = true)
    private void bbs$boostSolidHitboxStepHeight(CallbackInfoReturnable<Float> info)
    {
        Entity entity = (Entity) (Object) this;
        float boosted = ModelBlockSolidCollisions.boostStepHeight(entity, info.getReturnValueF());

        if (boosted > info.getReturnValueF())
        {
            info.setReturnValue(boosted);
        }
    }

    /* @Inject(method = "swingHand(Lnet/minecraft/util/Hand;Z)V", at = @At("HEAD"), cancellable = true)
    public void onSwingHand(Hand hand, boolean fromServerPlayer, CallbackInfo info)
    {
        info.cancel();
    } */
}