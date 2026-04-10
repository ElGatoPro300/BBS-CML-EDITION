package mchorse.bbs_mod.actions;

import javax.annotation.Nullable;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public class SuperFakePlayerNetworkHandler extends ServerGamePacketListenerImpl
{
    private static final Connection FAKE_CONNECTION = new FakeClientConnection();

    public SuperFakePlayerNetworkHandler(ServerPlayer player)
    {
        super(player.level().getServer(), FAKE_CONNECTION, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
    }

    public void send(Packet<?> packet)
    {}

    private static final class FakeClientConnection extends Connection
    {
        private FakeClientConnection()
        {
            super(PacketFlow.CLIENTBOUND);
        }

        public void setPacketListener(PacketListener packetListener)
        {}
    }
}