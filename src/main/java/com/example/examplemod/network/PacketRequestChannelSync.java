package com.example.examplemod.network;

import com.example.examplemod.chat.ChatManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketRequestChannelSync implements IMessage {

    public PacketRequestChannelSync() {}

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<PacketRequestChannelSync, IMessage> {
        @Override
        public IMessage onMessage(PacketRequestChannelSync message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServer().addScheduledTask(() -> {
                ChatManager.onPlayerLogin(player); // Используем существующий метод для полной синхронизации
            });
            return null;
        }
    }
}