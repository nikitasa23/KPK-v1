package com.example.kpkmod.network;

import com.example.kpkmod.chat.ChatChannel;
import com.example.kpkmod.chat.ClientChatCache;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

public class PacketSyncSubscribedChannels implements IMessage {
    private List<ChatChannel> channels;

    public PacketSyncSubscribedChannels() {
        this.channels = new ArrayList<>();
    }

    public PacketSyncSubscribedChannels(List<ChatChannel> channels) {
        this.channels = channels;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int size = buf.readInt();
        this.channels = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            this.channels.add(ChatChannel.fromBytes(buf));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.channels.size());
        for (ChatChannel channel : this.channels) {
            channel.toBytes(buf);
        }
    }

    public static class Handler implements IMessageHandler<PacketSyncSubscribedChannels, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketSyncSubscribedChannels message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                ClientChatCache.setSubscribedChannels(message.channels);
            });
            return null;
        }
    }
}