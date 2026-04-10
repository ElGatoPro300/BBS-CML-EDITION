package mchorse.bbs_mod.actions;

import com.google.common.collect.MapMaker;
import com.mojang.authlib.GameProfile;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.scores.PlayerTeam;

public class SuperFakePlayer extends ServerPlayer
{
    private static final GameProfile PROFILE = new GameProfile(UUID.fromString("12345678-9ABC-DEF1-2345-6789ABCDEF69"), "[BBS Player]");
    private static final Map<SuperFakePlayer.FakePlayerKey, SuperFakePlayer> FAKE_PLAYER_MAP = new MapMaker().weakValues().makeMap();

    public static SuperFakePlayer get(ServerLevel world)
    {
        Objects.requireNonNull(world, "World may not be null.");

        return FAKE_PLAYER_MAP.computeIfAbsent(new SuperFakePlayer.FakePlayerKey(world, PROFILE), key -> new SuperFakePlayer(key.world, key.profile));
    }

    protected SuperFakePlayer(ServerLevel world, GameProfile profile)
    {
        super(world.getServer(), world, profile, ClientInformation.createDefault());

        this.connection = new SuperFakePlayerNetworkHandler(this);
    }

    public int getPermissionLevel()
    {
        return 2;
    }

    public boolean shouldBroadcastConsoleToOps()
    {
        return false;
    }

    public boolean shouldReceiveFeedback()
    {
        return false;
    }

    @Override
    public void tick()
    {}

    @Override
    public void updateOptions(ClientInformation settings)
    {}

    @Override
    public void awardStat(Stat<?> stat, int amount)
    {}

    @Override
    public void resetStat(Stat<?> stat)
    {}

    public boolean isInvulnerableTo(DamageSource damageSource)
    {
        return true;
    }

    @Nullable
    @Override
    public PlayerTeam getTeam()
    {
        return null;
    }

    @Override
    public void startSleeping(BlockPos pos)
    {}

    public boolean startRiding(Entity entity, boolean force)
    {
        return false;
    }

    @Override
    public void openTextEdit(SignBlockEntity sign, boolean front)
    {}

    @Override
    public OptionalInt openMenu(@Nullable MenuProvider factory)
    {
        return OptionalInt.empty();
    }

    @Override
    public void openHorseInventory(AbstractHorse horse, Container inventory)
    {}

    private record FakePlayerKey(ServerLevel world, GameProfile profile)
    {}
}