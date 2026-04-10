package mchorse.bbs_mod.forms.entities;

import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.utils.AABB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Map;

public class StubEntity implements IEntity
{
    private Level world;
    private int age;

    private Form form;
    private boolean sneaking;
    private boolean sprinting;
    private boolean onGround = true;
    private float fallDistance;
    private int hurtTimer;

    private double prevX;
    private double prevY;
    private double prevZ;
    private double x;
    private double y;
    private double z;

    private float prevYaw;
    private float prevHeadYaw;
    private float prevPitch;
    private float prevBodyYaw;
    private float prevPrevBodyYaw;

    private float yaw;
    private float headYaw;
    private float pitch;
    private float bodyYaw;

    private int armSwing;

    private Vec3 velocity = Vec3.ZERO;

    private float[] extraVariables = new float[10];
    private float[] prevExtraVariables = new float[10];

    private WalkAnimationState limbAnimator = new WalkAnimationState();
    private final Map<EquipmentSlot, ItemStack> items = new HashMap<>();

    public StubEntity(Level world)
    {
        this.world = world;
    }

    public StubEntity()
    {
        for (EquipmentSlot value : EquipmentSlot.values())
        {
            this.items.put(value, ItemStack.EMPTY);
        }
    }

    @Override
    public void setWorld(Level world)
    {
        this.world = world;
    }

    @Override
    public Level getWorld()
    {
        return this.world;
    }

    @Override
    public Form getForm()
    {
        return this.form;
    }

    @Override
    public void setForm(Form form)
    {
        this.form = form;
    }

    @Override
    public ItemStack getEquipmentStack(EquipmentSlot slot)
    {
        return this.items.getOrDefault(slot, ItemStack.EMPTY);
    }

    @Override
    public void setEquipmentStack(EquipmentSlot slot, ItemStack stack)
    {
        if (stack == null)
        {
            stack = ItemStack.EMPTY;
        }

        this.items.put(slot, stack);
    }

    @Override
    public int getSelectedSlot()
    {
        return 0;
    }

    @Override
    public boolean isSneaking()
    {
        return this.sneaking;
    }

    @Override
    public void setSneaking(boolean sneaking)
    {
        this.sneaking = sneaking;
    }

    @Override
    public boolean isSprinting()
    {
        return this.sprinting;
    }

    @Override
    public void setSprinting(boolean sprinting)
    {
        this.sprinting = sprinting;
    }

    @Override
    public boolean isOnGround()
    {
        return this.onGround;
    }

    @Override
    public void setOnGround(boolean ground)
    {
        this.onGround = ground;
    }

    @Override
    public void swingArm()
    {
        this.armSwing = 6;
    }

    @Override
    public float getHandSwingProgress(float tickDelta)
    {
        return this.armSwing <= 0 ? 0F : 1F - (this.armSwing - tickDelta) / 6F;
    }

    @Override
    public int getAge()
    {
        return this.age;
    }

    @Override
    public void setAge(int ticks)
    {
        this.age = ticks;
    }

    @Override
    public float getFallDistance()
    {
        return this.fallDistance;
    }

    @Override
    public void setFallDistance(float fallDistance)
    {
        this.fallDistance = fallDistance;
    }

    @Override
    public int getHurtTimer()
    {
        return this.hurtTimer;
    }

    @Override
    public void setHurtTimer(int hurtTimer)
    {
        this.hurtTimer = hurtTimer;
    }

    @Override
    public double getX()
    {
        return this.x;
    }

    @Override
    public double getPrevX()
    {
        return this.prevX;
    }

    @Override
    public void setPrevX(double x)
    {
        this.prevX = x;
    }

    @Override
    public double getY()
    {
        return this.y;
    }

    @Override
    public double getPrevY()
    {
        return this.prevY;
    }

    @Override
    public void setPrevY(double y)
    {
        this.prevY = y;
    }

    @Override
    public double getZ()
    {
        return this.z;
    }

    @Override
    public double getPrevZ()
    {
        return this.prevZ;
    }

    @Override
    public void setPrevZ(double z)
    {
        this.prevZ = z;
    }

    @Override
    public void setPosition(double x, double y, double z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public double getEyeHeight()
    {
        return 1.8F * 0.9F;
    }

    @Override
    public Vec3 getVelocity()
    {
        return this.velocity;
    }

    @Override
    public void setVelocity(float x, float y, float z)
    {
        this.velocity = new Vec3(x, y, z);
    }

    @Override
    public float getYaw()
    {
        return this.yaw;
    }

    @Override
    public float getPrevYaw()
    {
        return this.prevYaw;
    }

    @Override
    public void setYaw(float yaw)
    {
        this.yaw = yaw;
    }

    @Override
    public void setPrevYaw(float prevYaw)
    {
        this.prevYaw = prevYaw;
    }

    @Override
    public float getHeadYaw()
    {
        return this.headYaw;
    }

    @Override
    public float getPrevHeadYaw()
    {
        return this.prevHeadYaw;
    }

    @Override
    public void setHeadYaw(float headYaw)
    {
        this.headYaw = headYaw;
    }

    @Override
    public void setPrevHeadYaw(float prevHeadYaw)
    {
        this.prevHeadYaw = prevHeadYaw;
    }

    @Override
    public float getPitch()
    {
        return this.pitch;
    }

    @Override
    public float getPrevPitch()
    {
        return this.prevPitch;
    }

    @Override
    public void setPitch(float pitch)
    {
        this.pitch = pitch;
    }

    @Override
    public void setPrevPitch(float prevPitch)
    {
        this.prevPitch = prevPitch;
    }

    @Override
    public float getBodyYaw()
    {
        return this.bodyYaw;
    }

    @Override
    public float getPrevBodyYaw()
    {
        return this.prevBodyYaw;
    }

    @Override
    public float getPrevPrevBodyYaw()
    {
        return this.prevPrevBodyYaw;
    }

    @Override
    public void setBodyYaw(float bodyYaw)
    {
        this.bodyYaw = bodyYaw;
    }

    @Override
    public void setPrevBodyYaw(float prevBodyYaw)
    {
        this.prevBodyYaw = prevBodyYaw;
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
        Form form = this.getForm();
        float w = 0.6F;
        float h = 1.8F;

        if (form != null && form.hitbox.get())
        {
            w = form.hitboxWidth.get();
            h = form.hitboxHeight.get();
        }

        return new AABB(
            this.getX() - w / 2, this.getY(), this.getZ() - w / 2,
            w, h, w
        );
    }

    @Override
    public void update()
    {
        float delta = (float) Mth.length(this.x - this.prevX, 0D, this.z - this.prevZ);
        float speed = Math.min(delta * 4F, 1F);

        this.limbAnimator.update(speed, 0.4F, 1F);

        this.armSwing -= 1;
        this.age += 1;

        this.prevX = this.x;
        this.prevY = this.y;
        this.prevZ = this.z;

        this.prevPrevBodyYaw = this.prevBodyYaw;

        this.prevYaw = this.yaw;
        this.prevHeadYaw = this.headYaw;
        this.prevPitch = this.pitch;
        this.prevBodyYaw = this.bodyYaw;

        for (int i = 0; i < this.extraVariables.length; i++)
        {
            this.prevExtraVariables[i] = this.extraVariables[i];
        }
    }

    @Override
    public WalkAnimationState getLimbAnimator()
    {
        return this.limbAnimator;
    }

    @Override
    public float getLimbPos(float tickDelta)
    {
        return this.limbAnimator.position(tickDelta);
    }

    @Override
    public float getLimbSpeed(float tickDelta)
    {
        return this.limbAnimator.speed();
    }

    @Override
    public float getLeaningPitch(float tickDelta)
    {
        return 0;
    }

    @Override
    public boolean isTouchingWater()
    {
        return false;
    }

    @Override
    public Pose getEntityPose()
    {
        return Pose.STANDING;
    }

    @Override
    public int getRoll()
    {
        return 0;
    }

    @Override
    public boolean isFallFlying()
    {
        return false;
    }

    @Override
    public Vec3 getRotationVec(float transition)
    {
        return Vec3.ZERO;
    }

    @Override
    public Vec3 lerpVelocity(float transition)
    {
        return Vec3.ZERO;
    }

    @Override
    public boolean isUsingRiptide()
    {
        return false;
    }
}