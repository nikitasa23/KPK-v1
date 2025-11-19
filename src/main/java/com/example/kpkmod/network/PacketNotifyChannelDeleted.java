package com.example.kpkmod.network;

import com.example.kpkmod.chat.ClientChatCache;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PacketNotifyChannelDeleted implements IMessage {
    private String channelId;

    public PacketNotifyChannelDeleted() {}

    public PacketNotifyChannelDeleted(String channelId) {
        this.channelId = channelId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.channelId = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.channelId);
    }

    public static class Handler implements IMessageHandler<PacketNotifyChannelDeleted, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketNotifyChannelDeleted message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                ClientChatCache.removeChannel(message.channelId);
            });
            return null;
        }
    }
}