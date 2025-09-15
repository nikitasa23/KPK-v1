package com.example.examplemod.network;

import com.example.examplemod.User;
import com.example.examplemod.item.ItemKPK;
import com.example.examplemod.kpk.KPKServerManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.UUID;

public class PacketAddContactRequest implements IMessage {

    private String targetCallsign;

    public PacketAddContactRequest() {}

    public PacketAddContactRequest(String targetCallsign) {
        this.targetCallsign = targetCallsign;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.targetCallsign = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.targetCallsign);
    }

    public static class Handler implements IMessageHandler<PacketAddContactRequest, IMessage> {
        @Override
        public IMessage onMessage(PacketAddContactRequest message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            MinecraftServer server = player.getServer();

            server.addScheduledTask(() -> {
                System.out.println("[KPK-DEBUG] Received PacketAddContactRequest for callsign: " + message.targetCallsign);

                EnumHand hand = EnumHand.MAIN_HAND;
                ItemStack kpkStack = player.getHeldItemMainhand();

                if (!(kpkStack.getItem() instanceof ItemKPK)) {
                    kpkStack = player.getHeldItemOffhand();
                    hand = EnumHand.OFF_HAND;
                }

                if (!(kpkStack.getItem() instanceof ItemKPK)) {
                    player.sendMessage(new TextComponentString(TextFormatting.RED + "Вы должны держать КПК в руке."));
                    System.out.println("[KPK-DEBUG] Player is not holding a KPK.");
                    return;
                }

                User playerUser = ItemKPK.getUserData(kpkStack);
                if (playerUser == null) {
                    player.sendMessage(new TextComponentString(TextFormatting.RED + "Ваш КПК не настроен."));
                    System.out.println("[KPK-DEBUG] Player's KPK is not configured.");
                    return;
                }
                if (message.targetCallsign.equalsIgnoreCase(playerUser.pozivnoy)) {
                    player.sendMessage(new TextComponentString(TextFormatting.RED + "Вы не можете добавить себя в контакты."));
                    System.out.println("[KPK-DEBUG] Player tried to add self.");
                    return;
                }

                List<String> currentContacts = ItemKPK.getContacts(kpkStack);
                if (currentContacts.contains(message.targetCallsign)) {
                    player.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Позывной '" + message.targetCallsign + "' уже в контактах этого КПК."));
                    System.out.println("[KPK-DEBUG] Contact already exists.");
                    return;
                }
                System.out.println("[KPK-DEBUG] Pre-add contacts: " + currentContacts.toString());

                Pair<UUID, User> targetUserPair = KPKServerManager.findUserByCallsign(message.targetCallsign);

                if (targetUserPair != null) {
                    System.out.println("[KPK-DEBUG] Found user by callsign: " + targetUserPair.getRight().pozivnoy + " with UUID " + targetUserPair.getLeft());

                    ItemKPK.addContact(kpkStack, message.targetCallsign);

                    System.out.println("[KPK-DEBUG] Post-add contacts: " + ItemKPK.getContacts(kpkStack).toString());

                    player.sendMessage(new TextComponentString(TextFormatting.GREEN + "Контакт '" + message.targetCallsign + "' добавлен в ваш КПК."));

                    player.setHeldItem(hand, kpkStack);
                    player.inventory.markDirty();
                    System.out.println("[KPK-DEBUG] Updated and synced ItemStack in hand: " + hand.name());

                } else {
                    player.sendMessage(new TextComponentString(TextFormatting.RED + "Пользователь с позывным '" + message.targetCallsign + "' не зарегистрирован в базе данных сервера."));
                    System.out.println("[KPK-DEBUG] User not found in KPKServerManager for callsign: " + message.targetCallsign);
                }
            });
            return null;
        }
    }
}