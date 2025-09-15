package com.example.examplemod.network;

import com.example.examplemod.User;
import com.example.examplemod.chat.ChatManager;
import com.example.examplemod.item.ItemKPK;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PacketRequestCreateGroupChannel implements IMessage {
    private String channelName;
    private List<String> memberCallsigns;

    public PacketRequestCreateGroupChannel() {
        this.memberCallsigns = new ArrayList<>();
    }

    public PacketRequestCreateGroupChannel(String channelName, List<String> memberCallsigns) {
        this.channelName = channelName;
        this.memberCallsigns = memberCallsigns;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.channelName = ByteBufUtils.readUTF8String(buf);
        int size = buf.readInt();
        this.memberCallsigns = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            this.memberCallsigns.add(ByteBufUtils.readUTF8String(buf));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, this.channelName);
        buf.writeInt(this.memberCallsigns.size());
        for (String callsign : this.memberCallsigns) {
            ByteBufUtils.writeUTF8String(buf, callsign);
        }
    }

    public static class Handler implements IMessageHandler<PacketRequestCreateGroupChannel, IMessage> {
        @Override
        public IMessage onMessage(PacketRequestCreateGroupChannel message, MessageContext ctx) {
            EntityPlayerMP sender = ctx.getServerHandler().player;
            MinecraftServer server = sender.getServer();
            sender.getServer().addScheduledTask(() -> {
                List<UUID> memberUuids = new ArrayList<>();
                boolean allFound = true;
                for (String callsign : message.memberCallsigns) {
                    EntityPlayerMP targetPlayer = null;
                    for (EntityPlayerMP p : server.getPlayerList().getPlayers()) {
                        ItemStack kpkStack = p.getHeldItemMainhand();
                        if (!(kpkStack.getItem() instanceof ItemKPK)) kpkStack = p.getHeldItemOffhand();

                        if (kpkStack.getItem() instanceof ItemKPK) {
                            User kpkUser = ItemKPK.getUserData(kpkStack);
                            if (kpkUser != null && callsign.equalsIgnoreCase(kpkUser.pozivnoy)) {
                                targetPlayer = p;
                                break;
                            }
                        }
                    }
                    if (targetPlayer != null) {
                        memberUuids.add(targetPlayer.getUniqueID());
                    } else {
                        sender.sendMessage(new TextComponentString(TextFormatting.RED + "Не удалось найти участника с позывным: " + callsign + ". Канал не создан."));
                        allFound = false;
                        break;
                    }
                }

                if (allFound) {
                    ChatManager.createPrivateGroupChannel(sender, message.channelName, memberUuids);
                }
            });
            return null;
        }
    }
}