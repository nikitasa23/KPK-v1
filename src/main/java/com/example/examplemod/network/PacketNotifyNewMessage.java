package com.example.examplemod.network;

import com.example.examplemod.chat.ChatChannelType;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PacketNotifyNewMessage implements IMessage {

    private ChatChannelType channelType;

    public PacketNotifyNewMessage() {
    }

    public PacketNotifyNewMessage(ChatChannelType channelType) {
        this.channelType = channelType;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.channelType = ChatChannelType.valueOf(ByteBufUtils.readUTF8String(buf));
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.channelType.name());
    }

    public static class Handler implements IMessageHandler<PacketNotifyNewMessage, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketNotifyNewMessage message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                EntityPlayerSP player = Minecraft.getMinecraft().player;
                if (player == null) {
                    return;
                }

                TextComponentString notificationMessage;
                switch (message.channelType) {
                    case PRIVATE_MESSAGE:
                        notificationMessage = new TextComponentString(TextFormatting.GOLD + "[КПК] " + TextFormatting.AQUA + "Новое личное сообщение!");
                        break;
                    case PRIVATE_GROUP:
                        notificationMessage = new TextComponentString(TextFormatting.GOLD + "[КПК] " + TextFormatting.GREEN + "Новое сообщение в группе!");
                        break;
                    default:
                        notificationMessage = new TextComponentString(TextFormatting.GOLD + "[КПК] " + TextFormatting.WHITE + "Новое сообщение!");
                        break;
                }

                player.sendMessage(notificationMessage);
                player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7F, 1.0F);
            });
            return null;
        }
    }
}