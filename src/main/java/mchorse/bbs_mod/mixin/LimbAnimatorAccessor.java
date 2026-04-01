package mchorse.bbs_mod.mixin;

import net.minecraft.entity.LimbAnimator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LimbAnimator.class)
public interface LimbAnimatorAccessor
{
    @Accessor("lastSpeed")
    public float getPrevSpeed();

    @Accessor("lastSpeed")
    public void setPrevSpeed(float v);

    @Accessor
    public float getSpeed();

    @Accessor
    public void setSpeed(float v);

    @Accessor("animationProgress")
    public float getPos();

    @Accessor("animationProgress")
    public void setPos(float v);
}
