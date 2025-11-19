package com.example.kpkmod.network;

import com.example.kpkmod.User;
import com.example.kpkmod.chat.ChatManager;
import com.example.kpkmod.item.ItemKPK;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketRequestCreatePMChannel implements IMessage {
    private String targetPlayerCallsign;

    public PacketRequestCreatePMChannel() {}

    public PacketRequestCreatePMChannel(String targetPlayerCallsign) {
        this.targetPlayerCallsign = targetPlayerCallsign;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.targetPlayerCallsign = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.targetPlayerCallsign);
    }

    public static class Handler implements IMessageHandler<PacketRequestCreatePMChannel, IMessage> {
        @Override
        public IMessage onMessage(PacketRequestCreatePMChannel message, MessageContext ctx) {
            EntityPlayerMP sender = ctx.getServerHandler().player;
            MinecraftServer server = sender.getServer();
            server.addScheduledTask(() -> {
                EntityPlayerMP targetPlayer = null;
                for (EntityPlayerMP p : server.getPlayerList().getPlayers()) {
                    ItemStack kpkStack = p.getHeldItemMainhand();
                    if (!(kpkStack.getItem() instanceof ItemKPK)) kpkStack = p.getHeldItemOffhand();

                    if (kpkStack.getItem() instanceof ItemKPK) {
                        User kpkUser = ItemKPK.getUserData(kpkStack);
                        if (kpkUser != null && message.targetPlayerCallsign.equalsIgnoreCase(kpkUser.pozivnoy)) {
                            targetPlayer = p;
                            break;
                        }
                    }
                }

                if (targetPlayer != null) {
                    ChatManager.createPrivateMessageChannel(sender, targetPlayer.getUniqueID());
                } else {
                    sender.sendMessage(new TextComponentString(TextFormatting.RED + "Игрок с позывным '" + message.targetPlayerCallsign + "' не найден, оффлайн или не держит КПК в руке."));
                }
            });
            return null;
        }
    }
}