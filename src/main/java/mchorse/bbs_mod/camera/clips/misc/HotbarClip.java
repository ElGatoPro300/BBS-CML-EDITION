package mchorse.bbs_mod.camera.clips.misc;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class HotbarClip extends CameraClip
{
    private static final float MAX_HEALTH_CONTAINER = 1200F; /* 60 rows * 10 hearts * 2 HP */

    public final KeyframeChannel<Integer> selectedSlot = new KeyframeChannel<>("selected_slot", KeyframeFactories.INTEGER);
    public final KeyframeChannel<ItemStack> slot0 = new KeyframeChannel<>("slot_0", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<ItemStack> slot1 = new KeyframeChannel<>("slot_1", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<ItemStack> slot2 = new KeyframeChannel<>("slot_2", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<ItemStack> slot3 = new KeyframeChannel<>("slot_3", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<ItemStack> slot4 = new KeyframeChannel<>("slot_4", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<ItemStack> slot5 = new KeyframeChannel<>("slot_5", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<ItemStack> slot6 = new KeyframeChannel<>("slot_6", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<ItemStack> slot7 = new KeyframeChannel<>("slot_7", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<ItemStack> slot8 = new KeyframeChannel<>("slot_8", KeyframeFactories.ITEM_STACK);
    public final KeyframeChannel<Double> health = new KeyframeChannel<>("health", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> healthContainer = new KeyframeChannel<>("health_container", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> absorption = new KeyframeChannel<>("absorption", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> absorptionContainer = new KeyframeChannel<>("absorption_container", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Integer> heartType = new KeyframeChannel<>("heart_type", KeyframeFactories.INTEGER);
    public final KeyframeChannel<Boolean> hardcore = new KeyframeChannel<>("hardcore", KeyframeFactories.BOOLEAN);
    public final KeyframeChannel<Double> armor = new KeyframeChannel<>("armor", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> hunger = new KeyframeChannel<>("hunger", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Boolean> hungerEffect = new KeyframeChannel<>("hunger_effect", KeyframeFactories.BOOLEAN);
    public final KeyframeChannel<Double> experience = new KeyframeChannel<>("experience", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Integer> experienceLevel = new KeyframeChannel<>("experience_level", KeyframeFactories.INTEGER);
    public final KeyframeChannel<Double> x = new KeyframeChannel<>("x", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> y = new KeyframeChannel<>("y", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> scale = new KeyframeChannel<>("scale", KeyframeFactories.DOUBLE);

    public final KeyframeChannel[] channels;
    public HotbarClip()
    {
        this.channels = new KeyframeChannel[] {
            this.selectedSlot,
            this.slot0, this.slot1, this.slot2, this.slot3, this.slot4, this.slot5, this.slot6, this.slot7, this.slot8,
            this.health, this.healthContainer, this.absorption, this.absorptionContainer, this.heartType, this.hardcore, this.armor, this.hunger, this.hungerEffect, this.experience, this.experienceLevel,
            this.x, this.y, this.scale
        };

        for (KeyframeChannel channel : this.channels)
        {
            this.add(channel);
        }

        this.selectedSlot.insert(0, 0);
        this.health.insert(0, 20D);
        this.healthContainer.insert(0, 20D);
        this.absorption.insert(0, 0D);
        this.absorptionContainer.insert(0, 0D);
        this.heartType.insert(0, HotbarState.HEART_NORMAL);
        this.hardcore.insert(0, false);
        this.armor.insert(0, 0D);
        this.hunger.insert(0, 20D);
        this.hungerEffect.insert(0, false);
        this.experience.insert(0, 0D);
        this.experienceLevel.insert(0, 0);
        this.x.insert(0, 0D);
        this.y.insert(0, 0D);
        this.scale.insert(0, 1D);
    }

    public static List<HotbarState> getHotbars(ClipContext context)
    {
        return context.clipData.get("hotbars", ArrayList::new);
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        float t = context.relativeTick + context.transition;
        float alpha = this.envelope.factorEnabled(this.duration.get(), t);

        if (alpha <= 0F)
        {
            return;
        }

        HotbarState state = new HotbarState();

        state.selectedSlot = Math.max(0, Math.min(8, this.selectedSlot.interpolate(t)));
        state.items[0] = this.copyItem(this.slot0.interpolate(t));
        state.items[1] = this.copyItem(this.slot1.interpolate(t));
        state.items[2] = this.copyItem(this.slot2.interpolate(t));
        state.items[3] = this.copyItem(this.slot3.interpolate(t));
        state.items[4] = this.copyItem(this.slot4.interpolate(t));
        state.items[5] = this.copyItem(this.slot5.interpolate(t));
        state.items[6] = this.copyItem(this.slot6.interpolate(t));
        state.items[7] = this.copyItem(this.slot7.interpolate(t));
        state.items[8] = this.copyItem(this.slot8.interpolate(t));
        state.healthContainer = this.clampHealthContainer(this.healthContainer.interpolate(t));
        state.health = this.clampHealth(this.health.interpolate(t), state.healthContainer);
        state.absorptionContainer = this.clampHealthContainer(this.absorptionContainer.interpolate(t));
        state.absorption = this.clampHealth(this.absorption.interpolate(t), state.absorptionContainer);
        state.heartType = this.clampHeartType(this.heartType.interpolate(t));
        state.hardcore = this.interpolateHardcore(t);
        state.armor = this.clampStat(this.armor.interpolate(t));
        state.hunger = this.clampStat(this.hunger.interpolate(t));
        state.hungerEffect = this.hungerEffect.interpolate(t, false);
        state.experience = this.clampExperience(this.experience.interpolate(t));
        state.experienceLevel = this.clampExperienceLevel(this.experienceLevel.interpolate(t));
        state.x = this.x.interpolate(t).floatValue();
        state.y = this.y.interpolate(t).floatValue();
        state.scale = Math.max(0.05F, this.scale.interpolate(t).floatValue());
        state.alpha = alpha;

        getHotbars(context).add(state);
    }

    private ItemStack copyItem(ItemStack stack)
    {
        return stack == null ? ItemStack.EMPTY : stack.copy();
    }

    private float clampStat(Double value)
    {
        return Math.max(0F, Math.min(20F, value.floatValue()));
    }

    private float clampHealth(Double value, float healthContainer)
    {
        return Math.max(0F, Math.min(healthContainer, value.floatValue()));
    }

    private int clampHeartType(Integer value)
    {
        return Math.max(HotbarState.HEART_NORMAL, Math.min(HotbarState.HEART_FROZEN, value));
    }

    private float clampHealthContainer(Double value)
    {
        return Math.max(0F, Math.min(MAX_HEALTH_CONTAINER, value.floatValue()));
    }

    private float clampExperience(Double value)
    {
        return Math.max(0F, Math.min(1F, value.floatValue()));
    }

    private int clampExperienceLevel(Integer value)
    {
        return Math.max(0, Math.min(9999, value));
    }

    @Override
    public void fromData(BaseType data)
    {
        if (data != null && data.isMap())
        {
            MapType map = data.asMap();
            MapType hardcoreData = map.getMap("hardcore", null);

            if (hardcoreData != null && !"boolean".equals(hardcoreData.getString("type")))
            {
                hardcoreData.putString("type", "boolean");
            }
        }

        super.fromData(data);
    }

    @SuppressWarnings("rawtypes")
    private boolean interpolateHardcore(float tick)
    {
        if (this.hardcore.getFactory() == KeyframeFactories.BOOLEAN)
        {
            return this.hardcore.interpolate(tick, false);
        }

        Object value = ((KeyframeChannel) this.hardcore).interpolate(tick, 0);

        if (value instanceof Number number)
        {
            return number.intValue() > 0;
        }

        if (value instanceof Boolean bool)
        {
            return bool;
        }

        return false;
    }

    @Override
    protected Clip create()
    {
        return new HotbarClip();
    }
}
