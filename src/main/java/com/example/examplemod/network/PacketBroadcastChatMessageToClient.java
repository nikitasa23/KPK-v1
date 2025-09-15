package com.example.examplemod.network;

import com.example.examplemod.chat.ChatMessage;
import com.example.examplemod.chat.ClientChatCache;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PacketBroadcastChatMessageToClient implements IMessage {
    private ChatMessage chatMessage;

    public PacketBroadcastChatMessageToClient() {}

    public PacketBroadcastChatMessageToClient(ChatMessage chatMessage) {
        this.chatMessage = chatMessage;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.chatMessage = ChatMessage.fromBytes(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        this.chatMessage.toBytes(buf);
    }

    public static class Handler implements IMessageHandler<PacketBroadcastChatMessageToClient, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketBroadcastChatMessageToClient message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                ClientChatCache.addMessage(message.chatMessage);
            });
            return null;
        }
    }
}