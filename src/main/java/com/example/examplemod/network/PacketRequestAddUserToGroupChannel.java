package com.example.examplemod.network;

import com.example.examplemod.chat.ChatManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketRequestAddUserToGroupChannel implements IMessage {
    private String channelId;
    private String targetUserCallsign;

    public PacketRequestAddUserToGroupChannel() {}

    public PacketRequestAddUserToGroupChannel(String channelId, String targetUserCallsign) {
        this.channelId = channelId;
        this.targetUserCallsign = targetUserCallsign;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.channelId = ByteBufUtils.readUTF8String(buf);
        this.targetUserCallsign = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.channelId);
        ByteBufUtils.writeUTF8String(buf, this.targetUserCallsign);
    }

    public static class Handler implements IMessageHandler<PacketRequestAddUserToGroupChannel, IMessage> {
        @Override
        public IMessage onMessage(PacketRequestAddUserToGroupChannel message, MessageContext ctx) {
            EntityPlayerMP sender = ctx.getServerHandler().player;
            sender.getServer().addScheduledTask(() -> {
                ChatManager.addPlayerToGroupChannelByCallsign(message.channelId, message.targetUserCallsign, sender);
            });
            return null;
        }
    }
}