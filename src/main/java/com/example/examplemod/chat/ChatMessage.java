package com.example.examplemod.chat;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class ChatMessage {
    public UUID senderUuid;
    public String senderCallsign;
    public String senderPlayerName;
    public long timestamp;
    public String messageContent;
    public String channelId;
    public boolean isAnonymous;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ChatMessage(UUID senderUuid, String senderCallsign, String senderPlayerName, long timestamp, String messageContent, String channelId, boolean isAnonymous) {
        this.senderUuid = senderUuid;
        this.senderCallsign = senderCallsign;
        this.senderPlayerName = senderPlayerName;
        this.timestamp = timestamp;
        this.messageContent = messageContent;
        this.channelId = channelId;
        this.isAnonymous = isAnonymous;
    }

    public ChatMessage(NBTTagCompound nbt) {
        String uuidStr = nbt.getString("senderUuid");
        if (uuidStr != null && !uuidStr.isEmpty()) {
            try {
                this.senderUuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                this.senderUuid = null;
            }
        } else {
            this.senderUuid = null;
        }

        this.senderCallsign = nbt.getString("senderCallsign");
        this.senderPlayerName = nbt.getString("senderPlayerName");
        this.timestamp = nbt.getLong("timestamp");
        this.messageContent = nbt.getString("messageContent");
        this.channelId = nbt.getString("channelId");
        this.isAnonymous = nbt.getBoolean("isAnonymous");
    }

    public NBTTagCompound toNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("senderUuid", this.senderUuid != null ? this.senderUuid.toString() : "");
        nbt.setString("senderCallsign", this.senderCallsign);
        nbt.setString("senderPlayerName", this.senderPlayerName);
        nbt.setLong("timestamp", this.timestamp);
        nbt.setString("messageContent", this.messageContent);
        nbt.setString("channelId", this.channelId);
        nbt.setBoolean("isAnonymous", this.isAnonymous);
        return nbt;
    }

    public String getFormattedTimestamp() {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        return localDateTime.format(TIME_FORMATTER);
    }

    public static ChatMessage fromBytes(ByteBuf buf) {
        UUID senderUuid = null;
        if (buf.readBoolean()) {
            senderUuid = UUID.fromString(ByteBufUtils.readUTF8String(buf));
        }
        String senderCallsign = ByteBufUtils.readUTF8String(buf);
        String senderPlayerName = ByteBufUtils.readUTF8String(buf);
        long timestamp = buf.readLong();
        String messageContent = ByteBufUtils.readUTF8String(buf);
        String channelId = ByteBufUtils.readUTF8String(buf);
        boolean isAnonymous = buf.readBoolean();
        return new ChatMessage(senderUuid, senderCallsign, senderPlayerName, timestamp, messageContent, channelId, isAnonymous);
    }

    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(senderUuid != null);
        if (senderUuid != null) {
            ByteBufUtils.writeUTF8String(buf, senderUuid.toString());
        }
        ByteBufUtils.writeUTF8String(buf, senderCallsign);
        ByteBufUtils.writeUTF8String(buf, senderPlayerName);
        buf.writeLong(timestamp);
        ByteBufUtils.writeUTF8String(buf, messageContent);
        ByteBufUtils.writeUTF8String(buf, channelId);
        buf.writeBoolean(this.isAnonymous);
    }
}