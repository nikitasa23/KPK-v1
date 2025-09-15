package com.example.examplemod.network;

import com.example.examplemod.User;
import com.example.examplemod.chat.ChatChannel;
import com.example.examplemod.chat.ChatMessage;
import com.example.examplemod.chat.ChatManager;
import com.example.examplemod.item.ItemKPK;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketChatMessageToServer implements IMessage {
    private String channelId;
    private String content;
    private boolean isAnonymous;

    public PacketChatMessageToServer() {}

    public PacketChatMessageToServer(String channelId, String content, boolean isAnonymous) {
        this.channelId = channelId;
        this.content = content;
        this.isAnonymous = isAnonymous;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.channelId = ByteBufUtils.readUTF8String(buf);
        this.content = ByteBufUtils.readUTF8String(buf);
        this.isAnonymous = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.channelId);
        ByteBufUtils.writeUTF8String(buf, this.content);
        buf.writeBoolean(this.isAnonymous);
    }

    public static class Handler implements IMessageHandler<PacketChatMessageToServer, IMessage> {
        @Override
        public IMessage onMessage(PacketChatMessageToServer message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServer().addScheduledTask(() -> {
                ItemStack kpkStack = player.getHeldItemMainhand();
                if (!(kpkStack.getItem() instanceof ItemKPK) || !ItemKPK.isEnabled(kpkStack)) {
                    kpkStack = player.getHeldItemOffhand();
                }

                if (!(kpkStack.getItem() instanceof ItemKPK) || !ItemKPK.isEnabled(kpkStack)) {
                    player.sendMessage(new TextComponentString(TextFormatting.RED + "Вы должны держать в руках включенный КПК для отправки сообщений."));
                    return;
                }

                User user = ItemKPK.getUserData(kpkStack);

                if (user == null || user.pozivnoy == null || user.pozivnoy.isEmpty()) {
                    player.sendMessage(new TextComponentString(TextFormatting.RED + "Невозможно отправить сообщение: КПК не инициализирован или отсутствует позывной."));
                    return;
                }
                if (message.content == null || message.content.trim().isEmpty()) {
                    return;
                }
                if (message.content.length() > 256) {
                    player.sendMessage(new TextComponentString(TextFormatting.RED + "Сообщение слишком длинное."));
                    return;
                }

                String senderCallsign = user.pozivnoy;
                String senderPlayerName = player.getName();
                boolean isMessageAnonymous = false;

                if (message.isAnonymous && message.channelId.equals(ChatChannel.COMMON_CHANNEL_ID_PREFIX)) {
                    senderCallsign = "Аноним";
                    senderPlayerName = "Аноним";
                    isMessageAnonymous = true;
                }

                ChatMessage chatMsg = new ChatMessage(
                        player.getUniqueID(),
                        senderCallsign,
                        senderPlayerName,
                        System.currentTimeMillis(),
                        message.content,
                        message.channelId,
                        isMessageAnonymous
                );
                ChatManager.addMessageToChannel(player, chatMsg);
            });
            return null;
        }
    }
}