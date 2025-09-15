package com.example.examplemod.network;

import com.example.examplemod.Gender;
import com.example.examplemod.User;
import com.example.examplemod.chat.ClientChatCache;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PacketSyncKpkDatabaseToClient implements IMessage {

    private Map<UUID, User> kpkDatabase;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public PacketSyncKpkDatabaseToClient() {}

    public PacketSyncKpkDatabaseToClient(Map<UUID, User> kpkDatabase) {
        this.kpkDatabase = kpkDatabase;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int size = buf.readInt();
        this.kpkDatabase = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            UUID uuid = UUID.fromString(ByteBufUtils.readUTF8String(buf));
            String familiya = ByteBufUtils.readUTF8String(buf);
            String name = ByteBufUtils.readUTF8String(buf);
            String pozivnoy = ByteBufUtils.readUTF8String(buf);
            Gender gender = Gender.valueOf(ByteBufUtils.readUTF8String(buf));
            LocalDate birthdate = LocalDate.parse(ByteBufUtils.readUTF8String(buf), DATE_FORMATTER);
            this.kpkDatabase.put(uuid, new User(familiya, name, pozivnoy, gender, birthdate));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.kpkDatabase.size());
        for (Map.Entry<UUID, User> entry : this.kpkDatabase.entrySet()) {
            ByteBufUtils.writeUTF8String(buf, entry.getKey().toString());
            User user = entry.getValue();
            ByteBufUtils.writeUTF8String(buf, user.familiya);
            ByteBufUtils.writeUTF8String(buf, user.name);
            ByteBufUtils.writeUTF8String(buf, user.pozivnoy);
            ByteBufUtils.writeUTF8String(buf, user.gender.name());
            ByteBufUtils.writeUTF8String(buf, user.birthdate.format(DATE_FORMATTER));
        }
    }

    public static class Handler implements IMessageHandler<PacketSyncKpkDatabaseToClient, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketSyncKpkDatabaseToClient message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                ClientChatCache.setKpkUserDatabase(message.kpkDatabase);
            });
            return null;
        }
    }
}