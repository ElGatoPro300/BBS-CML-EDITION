package mchorse.bbs_mod.utils;

import com.mojang.authlib.GameProfile;
import mchorse.bbs_mod.network.ClientNetwork;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.GameMode;

public class PlayerUtils
{
    public static void teleport(double x, double y, double z, float yaw, float pitch)
    {
        teleport(x, y, z, yaw, yaw, pitch);
    }

    public static void teleport(double x, double y, double z, float yaw, float bodyYaw, float pitch)
    {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        if (!ClientNetwork.isIsBBSModOnServer())
        {
            String command = "tp " + player.getName().getString() + " " + x + " " + y + " " + z + " " + yaw + " " + pitch;

            player.networkHandler.sendChatCommand(command);
        }
        else
        {
            ClientNetwork.sendTeleport(x, y, z, yaw, bodyYaw, pitch);
            player.setYaw(yaw);
            player.setHeadYaw(yaw);
            player.setBodyYaw(bodyYaw);
            player.setPitch(pitch);
        }
    }

    public static void teleport(double x, double y, double z)
    {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        if (!ClientNetwork.isIsBBSModOnServer())
        {
            player.networkHandler.sendChatCommand("tp " + player.getName().getString() + " " + x + " " + y + " " + z);
        }
        else
        {
            ClientNetwork.sendTeleport(player, x, y, z);
        }
    }

    public static class ProtectedAccess extends PlayerEntity
    {
        public static TrackedData<Byte> getModelParts()
        {
            // TODO: Fix PLAYER_MODEL_PARTS access
            return null; // PLAYER_MODEL_PARTS;
        }

        public ProtectedAccess(World world, BlockPos pos, float yaw, GameProfile gameProfile)
        {
            super(world, gameProfile);
            this.setPosition(pos.getX(), pos.getY(), pos.getZ());
            this.setYaw(yaw);
        }

        @Override
        public boolean isSpectator()
        {
            return false;
        }

        @Override
        public boolean isCreative()
        {
            return false;
        }

        @Override
        public GameMode getGameMode()
        {
            return GameMode.SURVIVAL;
        }
    }
}
