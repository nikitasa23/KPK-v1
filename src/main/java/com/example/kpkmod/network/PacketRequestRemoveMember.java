package com.example.kpkmod.network;

import com.example.kpkmod.User;
import com.example.kpkmod.chat.ChatManager;
import com.example.kpkmod.item.ItemKPK;
import com.example.kpkmod.kpk.KPKServerManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.apache.commons.lang3.tuple.Pair;

import java.util.UUID;

public class PacketRequestRemoveMember implements IMessage {
    private String channelId;
    private String memberToRemoveCallsign;

    public PacketRequestRemoveMember() {}

    public PacketRequestRemoveMember(String channelId, String memberToRemoveCallsign) {
        this.channelId = channelId;
        this.memberToRemoveCallsign = memberToRemoveCallsign;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.channelId = ByteBufUtils.readUTF8String(buf);
        this.memberToRemoveCallsign = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.channelId);
        ByteBufUtils.writeUTF8String(buf, this.memberToRemoveCallsign);
    }

    public static class Handler implements IMessageHandler<PacketRequestRemoveMember, IMessage> {
        @Override
        public IMessage onMessage(PacketRequestRemoveMember message, MessageContext ctx) {
            EntityPlayerMP requester = ctx.getServerHandler().player;
            requester.getServer().addScheduledTask(() -> {
                // Находим UUID по позывному для обратной совместимости с методом removeMemberFromGroupChannel
                MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
                UUID memberToRemoveUuid = null;
                for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
                    ItemStack kpkStack = player.getHeldItemMainhand();
                    if (!(kpkStack.getItem() instanceof ItemKPK)) {
                        kpkStack = player.getHeldItemOffhand();
                    }
                    if (kpkStack.getItem() instanceof ItemKPK) {
                        User kpkUser = ItemKPK.getUserData(kpkStack);
                        if (kpkUser != null && kpkUser.pozivnoy != null && message.memberToRemoveCallsign.equalsIgnoreCase(kpkUser.pozivnoy)) {
                            memberToRemoveUuid = player.getUniqueID();
                            break;
                        }
                    }
                }
                // Если не нашли онлайн игрока, пытаемся найти в базе данных
                if (memberToRemoveUuid == null) {
                    Pair<UUID, User> found = KPKServerManager.findUserByCallsign(message.memberToRemoveCallsign);
                    if (found != null) {
                        memberToRemoveUuid = found.getLeft();
                    }
                }
                if (memberToRemoveUuid != null) {
                    ChatManager.removeMemberFromGroupChannel(message.channelId, memberToRemoveUuid, requester);
                } else {
                    requester.sendMessage(new TextComponentString(TextFormatting.RED + "Не удалось найти игрока с позывным '" + message.memberToRemoveCallsign + "'."));
                }
            });
            return null;
        }
    }
}