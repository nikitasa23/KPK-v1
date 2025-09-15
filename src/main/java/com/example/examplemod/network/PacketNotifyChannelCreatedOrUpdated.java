package com.example.examplemod.network;

import com.example.examplemod.chat.ChatChannel;
import com.example.examplemod.chat.ClientChatCache;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PacketNotifyChannelCreatedOrUpdated implements IMessage {
    private ChatChannel channel;

    public PacketNotifyChannelCreatedOrUpdated() {}

    public PacketNotifyChannelCreatedOrUpdated(ChatChannel channel) {
        this.channel = channel;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.channel = ChatChannel.fromBytes(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        this.channel.toBytes(buf);
    }

    public static class Handler implements IMessageHandler<PacketNotifyChannelCreatedOrUpdated, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketNotifyChannelCreatedOrUpdated message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                ClientChatCache.addOrUpdateChannel(message.channel);
            });
            return null;
        }
    }
}