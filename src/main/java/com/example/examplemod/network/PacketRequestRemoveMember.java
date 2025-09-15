package com.example.examplemod.network;

import com.example.examplemod.chat.ChatManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.UUID;

public class PacketRequestRemoveMember implements IMessage {
    private String channelId;
    private UUID memberToRemoveUuid;

    public PacketRequestRemoveMember() {}

    public PacketRequestRemoveMember(String channelId, UUID memberToRemoveUuid) {
        this.channelId = channelId;
        this.memberToRemoveUuid = memberToRemoveUuid;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.channelId = ByteBufUtils.readUTF8String(buf);
        this.memberToRemoveUuid = UUID.fromString(ByteBufUtils.readUTF8String(buf));
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.channelId);
        ByteBufUtils.writeUTF8String(buf, this.memberToRemoveUuid.toString());
    }

    public static class Handler implements IMessageHandler<PacketRequestRemoveMember, IMessage> {
        @Override
        public IMessage onMessage(PacketRequestRemoveMember message, MessageContext ctx) {
            EntityPlayerMP requester = ctx.getServerHandler().player;
            requester.getServer().addScheduledTask(() -> {
                ChatManager.removeMemberFromGroupChannel(message.channelId, message.memberToRemoveUuid, requester);
            });
            return null;
        }
    }
}