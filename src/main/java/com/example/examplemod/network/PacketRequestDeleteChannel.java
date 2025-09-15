package com.example.examplemod.network;

import com.example.examplemod.chat.ChatManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketRequestDeleteChannel implements IMessage {
    private String channelId;

    public PacketRequestDeleteChannel() {}

    public PacketRequestDeleteChannel(String channelId) {
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

    public static class Handler implements IMessageHandler<PacketRequestDeleteChannel, IMessage> {
        @Override
        public IMessage onMessage(PacketRequestDeleteChannel message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServer().addScheduledTask(() -> {
                ChatManager.deleteChannel(message.channelId, player);
            });
            return null;
        }
    }
}