package mchorse.bbs_mod.ui.dashboard;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

import java.util.function.IntConsumer;

/**
 * Applies world-property changes through the integrated-server API when available (no chat spam, no
 * command parsing lag). Falls back to silent {@code sendChatCommand} only on multiplayer without direct access.
 */
public class WorldPropertiesHelper
{
    private static volatile long clientTimeOverride = -1L;

    private WorldPropertiesHelper()
    {}

    /**
     * Client-side time override for smooth sun rotation while dragging the World Properties time slider
     * (same mechanism as the Sun Rotation curve via {@code ClientWorldPropertiesMixin}).
     */
    public static void setClientTimeOverride(long time)
    {
        clientTimeOverride = time % 24000L;

        if (clientTimeOverride < 0L)
        {
            clientTimeOverride += 24000L;
        }
    }

    public static Long getClientTimeOverride()
    {
        return clientTimeOverride >= 0L ? clientTimeOverride : null;
    }

    public static void clearClientTimeOverride()
    {
        clientTimeOverride = -1L;
    }

    public static void setTimeOfDay(long time)
    {
        setClientTimeOverride(time);

        MinecraftClient mc = MinecraftClient.getInstance();
        MinecraftServer server = mc.getServer();

        if (server != null)
        {
            server.execute(() ->
            {
                ServerWorld world = server.getOverworld();

                if (world != null)
                {
                    world.setTimeOfDay(time);
                }
            });

            return;
        }

        sendSilentCommand("time set " + time);
    }

    public static void setGamerule(String key, boolean value)
    {
        MinecraftClient mc = MinecraftClient.getInstance();

        sendSilentCommand("gamerule " + key + " " + value);
    }

    public static void setWeatherClear()
    {
        executeWeatherCommand("weather clear");
    }

    public static void setWeatherRain()
    {
        executeWeatherCommand("weather rain");
    }

    public static void setWeatherThunder()
    {
        executeWeatherCommand("weather thunder");
    }

    public static void killAllMobs(IntConsumer callback)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        MinecraftServer server = mc.getServer();

        if (server != null)
        {
            server.execute(() ->
            {
                ServerWorld world = server.getOverworld();
                int count = 0;

                if (world != null)
                {
                    for (Entity entity : world.iterateEntities())
                    {
                        if (!(entity instanceof PlayerEntity))
                        {
                            count++;
                        }
                    }
                }

                sendSilentCommandOnServer(server, "kill @e[type=!minecraft:player]");

                if (callback != null)
                {
                    int finalCount = count;
                    mc.execute(() -> callback.accept(finalCount));
                }
            });

            return;
        }

        sendSilentCommand("kill @e[type=!minecraft:player]");

        if (callback != null)
        {
            callback.accept(-1);
        }
    }

    private static void executeWeatherCommand(String command)
    {
        MinecraftClient mc = MinecraftClient.getInstance();
        MinecraftServer server = mc.getServer();

        if (server != null)
        {
            server.execute(() -> sendSilentCommandOnServer(server, command));

            return;
        }

        sendSilentCommand(command);
    }

    public static boolean readGamerule(String key, boolean fallback)
    {
        return fallback;
    }

    private static void sendSilentCommandOnServer(MinecraftServer server, String command)
    {
        server.getCommandManager().parseAndExecute(server.getCommandSource(), command);
    }

    private static void sendSilentCommand(String command)
    {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;

        if (player != null)
        {
            player.networkHandler.sendChatCommand(command);
        }
    }
}
