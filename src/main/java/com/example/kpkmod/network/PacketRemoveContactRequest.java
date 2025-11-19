package com.example.kpkmod.network;

import com.example.kpkmod.item.ItemKPK;
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

import java.util.List;

public class PacketRemoveContactRequest implements IMessage {

    private String contactNameToRemove;

    public PacketRemoveContactRequest() {
    }

    public PacketRemoveContactRequest(String contactNameToRemove) {
        this.contactNameToRemove = contactNameToRemove;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.contactNameToRemove = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.contactNameToRemove);
    }

    public static class Handler implements IMessageHandler<PacketRemoveContactRequest, IMessage> {
        @Override
        public IMessage onMessage(PacketRemoveContactRequest message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            MinecraftServer server = player.getServer();

            server.addScheduledTask(() -> {
                System.out.println("[KPK-DEBUG] Received PacketRemoveContactRequest for callsign: " + message.contactNameToRemove);

                EnumHand hand = EnumHand.MAIN_HAND;
                ItemStack kpkStack = player.getHeldItemMainhand();
                if (!(kpkStack.getItem() instanceof ItemKPK)) {
                    kpkStack = player.getHeldItemOffhand();
                    hand = EnumHand.OFF_HAND;
                }

                if (!(kpkStack.getItem() instanceof ItemKPK)) {
                    player.sendMessage(new TextComponentString(TextFormatting.RED + "Вы должны держать КПК в руке."));
                    System.out.println("[KPK-DEBUG] Player not holding KPK for removal.");
                    return;
                }

                List<String> currentContacts = ItemKPK.getContacts(kpkStack);
                System.out.println("[KPK-DEBUG] Pre-remove contacts: " + currentContacts.toString());

                if (currentContacts.contains(message.contactNameToRemove)) {
                    ItemKPK.removeContact(kpkStack, message.contactNameToRemove);
                    System.out.println("[KPK-DEBUG] Post-remove contacts: " + ItemKPK.getContacts(kpkStack).toString());
                    player.sendMessage(new TextComponentString(TextFormatting.GREEN + "Контакт '" + message.contactNameToRemove + "' удален из вашего КПК."));

                    player.setHeldItem(hand, kpkStack);
                    player.inventory.markDirty();
                    System.out.println("[KPK-DEBUG] Updated and synced ItemStack after removal.");

                } else {
                    player.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Контакт '" + message.contactNameToRemove + "' не найден в вашем КПК."));
                    System.out.println("[KPK-DEBUG] Contact to remove was not found in the list.");
                }
            });
            return null;
        }
    }
}