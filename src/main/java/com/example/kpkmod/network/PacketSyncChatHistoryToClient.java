package com.example.kpkmod.network;

import com.example.kpkmod.chat.ChatMessage;
import com.example.kpkmod.chat.ClientChatCache;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

public class PacketSyncChatHistoryToClient implements IMessage {
    private String channelId;
    private List<ChatMessage> history;

    public PacketSyncChatHistoryToClient() {}

    public PacketSyncChatHistoryToClient(String channelId, List<ChatMessage> history) {
        this.channelId = channelId;
        this.history = history;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.channelId = ByteBufUtils.readUTF8String(buf);
        int size = buf.readInt();
        this.history = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            this.history.add(ChatMessage.fromBytes(buf));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.channelId);
        buf.writeInt(this.history.size());
        for (ChatMessage msg : this.history) {
            msg.toBytes(buf);
        }
    }

    public static class Handler implements IMessageHandler<PacketSyncChatHistoryToClient, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketSyncChatHistoryToClient message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                ClientChatCache.setHistory(message.channelId, message.history);
            });
            return null;
        }
    }
}