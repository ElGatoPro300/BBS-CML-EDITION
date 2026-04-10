package mchorse.bbs_mod.forms.entities;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.utils.AABB;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class MCEntity implements IEntity
{
    private Entity mcEntity;

    private float prevPrevBodyYaw;
    private Vec3 lastVelocity = Vec3.ZERO;

    private float[] extraVariables = new float[10];
    private float[] prevExtraVariables = new float[10];

    public MCEntity(Entity mcEntity)
    {
        this.mcEntity = mcEntity;
    }

    public Entity getMcEntity()
    {
        return this.mcEntity;
    }

    @Override
    public void setWorld(Level world)
    {}

    @Override
    public Level getWorld()
    {
        return this.mcEntity.level();
    }

    @Override
    public Form getForm()
    {
        Morph morph = Morph.getMorph(this.mcEntity);

        return morph == null ? null : morph.getForm();
    }

    @Override
    public void setForm(Form form)
    {
        Morph morph = Morph.getMorph(this.mcEntity);

        if (morph != null)
        {
            morph.setForm(form);
        }
    }

    @Override
    public ItemStack getEquipmentStack(EquipmentSlot slot)
    {
        if (this.mcEntity instanceof LivingEntity living)
        {
            return living.getItemBySlot(slot);
        }

        return ItemStack.EMPTY;
    }

    @Override
    public void setEquipmentStack(EquipmentSlot slot, ItemStack stack)
    {
        if (this.mcEntity instanceof LivingEntity living)
        {
            living.setItemSlot(slot, stack == null ? ItemStack.EMPTY : stack);
        }
    }

    @Override
    public int getSelectedSlot()
    {
        if (this.mcEntity instanceof Player player)
        {
            return player.getInventory().getSelectedSlot();
        }

        return 0;
    }

    @Override
    public boolean isSneaking()
    {
        return this.mcEntity.isShiftKeyDown();
    }

    @Override
    public void setSneaking(boolean sneaking)
    {
        this.mcEntity.setShiftKeyDown(sneaking);
    }

    @Override
    public boolean isSprinting()
    {
        return this.mcEntity.isSprinting();
    }

    @Override
    public void setSprinting(boolean sprinting)
    {
        this.mcEntity.setSprinting(sprinting);
    }

    @Override
    public boolean isOnGround()
    {
        return this.mcEntity.onGround();
    }

    @Override
    public void setOnGround(boolean ground)
    {
        this.mcEntity.setOnGround(ground);
    }

    @Override
    public void swingArm()
    {
        if (this.mcEntity instanceof LivingEntity living)
        {
            living.swing(InteractionHand.MAIN_HAND);
        }
    }

    @Override
    public float getHandSwingProgress(float tickDelta)
    {
        if (this.mcEntity instanceof LivingEntity living)
        {
            return living.getAttackAnim(tickDelta);
        }

        return 0F;
    }

    @Override
    public int getAge()
    {
        return this.mcEntity.tickCount;
    }

    @Override
    public void setAge(int ticks)
    {
        this.mcEntity.tickCount = ticks;
    }

    @Override
    public float getFallDistance()
    {
        return (float) this.mcEntity.fallDistance;
    }

    @Override
    public void setFallDistance(float fallDistance)
    {
        this.mcEntity.fallDistance = fallDistance;
    }

    @Override
    public int getHurtTimer()
    {
        if (this.mcEntity instanceof LivingEntity living)
        {
            return living.hurtTime;
        }

        return 0;
    }

    @Override
    public void setHurtTimer(int hurtTimer)
    {
        if (this.mcEntity instanceof LivingEntity living)
        {
            living.hurtTime = hurtTimer;
        }
    }

    @Override
    public double getX()
    {
        return this.mcEntity.getX();
    }

    @Override
    public double getPrevX()
    {
        return this.mcEntity.xo;
    }

    @Override
    public void setPrevX(double x)
    {
        this.mcEntity.xo = x;
    }

    @Override
    public double getY()
    {
        return this.mcEntity.getY();
    }

    @Override
    public double getPrevY()
    {
        return this.mcEntity.yo;
    }

    @Override
    public void setPrevY(double y)
    {
        this.mcEntity.yo = y;
    }

    @Override
    public double getZ()
    {
        return this.mcEntity.getZ();
    }

    @Override
    public double getPrevZ()
    {
        return this.mcEntity.zo;
    }

    @Override
    public void setPrevZ(double z)
    {
        this.mcEntity.zo = z;
    }

    @Override
    public void setPosition(double x, double y, double z)
    {
        this.mcEntity.setPos(x, y, z);
    }

    @Override
    public double getEyeHeight()
    {
        return this.mcEntity.getEyeHeight(this.mcEntity.getPose());
    }

    @Override
    public Vec3 getVelocity()
    {
        return this.mcEntity.getDeltaMovement();
    }

    @Override
    public void setVelocity(float x, float y, float z)
    {
        this.mcEntity.setDeltaMovement(x, y, z);
    }

    @Override
    public float getYaw()
    {
        return this.mcEntity.getYRot();
    }

    @Override
    public float getPrevYaw()
    {
        return this.mcEntity.yRotO;
    }

    @Override
    public void setYaw(float yaw)
    {
        this.mcEntity.setYRot(yaw);
    }

    @Override
    public void setPrevYaw(float prevYaw)
    {
        this.mcEntity.yRotO = prevYaw;
    }

    @Override
    public float getHeadYaw()
    {
        if (this.mcEntity instanceof LivingEntity living)
        {
            return living.getYHeadRot();
        }

        return this.mcEntity.getYRot();
    }

    @Override
    public float getPrevHeadYaw()
    {
        if (this.mcEntity instanceof LivingEntity living)
        {
            return living.yHeadRotO;
        }

        return this.mcEntity.yRotO;
    }

    @Override
    public void setHeadYaw(float headYaw)
    {
        this.mcEntity.setYHeadRot(headYaw);
    }

    @Override
    public void setPrevHeadYaw(float prevHeadYaw)
    {
        if (this.mcEntity instanceof LivingEntity living)
        {
            living.yHeadRotO = prevHeadYaw;
        }
    }

    @Override
    public float getPitch()
    {
        return this.mcEntity.getXRot();
    }

    @Override
    public float getPrevPitch()
    {
        return this.mcEntity.xRotO;
    }

    @Override
    public void setPitch(float pitch)
    {
        this.mcEntity.setXRot(pitch);
    }

    @Override
    public void setPrevPitch(float prevPitch)
    {
        this.mcEntity.xRotO = prevPitch;
    }

    @Override
    public float getBodyYaw()
    {
        if (this.mcEntity instanceof LivingEntity living)
        {
            return living.yBodyRot;
        }

        return this.getHeadYaw();
    }

    @Override
    public float getPrevBodyYaw()
    {
        if (this.mcEntity instanceof LivingEntity living)
        {
            return living.yBodyRotO;
        }

        return this.getPrevHeadYaw();
    }

    @Override
    public float getPrevPrevBodyYaw()
    {
        return this.prevPrevBodyYaw;
    }

    @Override
    public void setBodyYaw(float bodyYaw)
    {
        this.mcEntity.setYBodyRot(bodyYaw);
    }

    @Override
    public void setPrevBodyYaw(float prevBodyYaw)
    {
        if (this.mcEntity instanceof LivingEntity living)
        {
            living.yBodyRotO = prevBodyYaw;
        }
    }

    @Override
    public void setPrevPrevBodyYaw(float prevPrevBodyYaw)
    {
        this.prevPrevBodyYaw = prevPrevBodyYaw;
    }

    @Override
    public float[] getExtraVariables()
    {
        return this.extraVariables;
    }

    @Override
    public float[] getPrevExtraVariables()
    {
        return this.prevExtraVariables;
    }

    @Override
    public AABB getPickingHitbox()
    {
        float w = this.mcEntity.getBbWidth();
        float h = this.mcEntity.getBbHeight();

        return new AABB(
            this.getX() - w / 2, this.getY(), this.getZ() - w / 2,
            w, h, w
        );
    }

    @Override
    public void update()
    {
        this.lastVelocity = this.mcEntity.getDeltaMovement();
        this.prevPrevBodyYaw = this.getPrevBodyYaw();

        for (int i = 0; i < this.extraVariables.length; i++)
        {
            this.prevExtraVariables[i] = this.extraVariables[i];
        }
    }

    @Override
    public WalkAnimationState getLimbAnimator()
    {
        if (this.mcEntity instanceof LivingEntity living)
        {
            return living.walkAnimation;
        }

        return null;
    }

    @Override
    public float getLimbPos(float tickDelta)
    {
        if (this.mcEntity instanceof LivingEntity living)
        {
            return living.walkAnimation.position(tickDelta);
        }

        return 0F;
    }

    @Override
    public float getLimbSpeed(float tickDelta)
    {
        if (this.mcEntity instanceof LivingEntity living)
        {
            return living.walkAnimation.speed();
        }

        return 0F;
    }

    @Override
    public float getLeaningPitch(float tickDelta)
    {
        if (this.mcEntity instanceof LivingEntity living)
        {
            return living.getSwimAmount(tickDelta);
        }

        return 0F;
    }

    @Override
    public boolean isTouchingWater()
    {
        return this.mcEntity.isInWater();
    }

    @Override
    public Pose getEntityPose()
    {
        return this.mcEntity.getPose();
    }

    @Override
    public int getRoll()
    {
        return 0;
    }

    @Override
    public boolean isFallFlying()
    {
        if (this.mcEntity instanceof LivingEntity living)
        {
            return living.isFallFlying();
        }

        return false;
    }

    @Override
    public Vec3 getRotationVec(float transition)
    {
        return this.mcEntity.getViewVector(transition);
    }

    @Override
    public Vec3 lerpVelocity(float transition)
    {
        return this.lastVelocity.lerp(this.mcEntity.getDeltaMovement(), transition);
    }

    @Override
    public boolean isUsingRiptide()
    {
        if (this.mcEntity instanceof LivingEntity living)
        {
            return living.isAutoSpinAttack();
        }

        return false;
    }
}