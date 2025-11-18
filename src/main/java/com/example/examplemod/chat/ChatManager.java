package com.example.examplemod.chat;

import com.example.examplemod.User;
import com.example.examplemod.item.ItemKPK;
import com.example.examplemod.kpk.KPKServerManager;
import com.example.examplemod.network.PacketBroadcastChatMessageToClient;
import com.example.examplemod.network.PacketHandler;
import com.example.examplemod.network.PacketNotifyChannelCreatedOrUpdated;
import com.example.examplemod.network.PacketNotifyChannelDeleted;
import com.example.examplemod.network.PacketNotifyNewMessage;
import com.example.examplemod.network.PacketSyncChatHistoryToClient;
import com.example.examplemod.network.PacketSyncSubscribedChannels;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ChatManager {
    private static final String CHAT_DATA_ID = "examplemod_chatdata_v2";
    private static ChatWorldData chatData;

    private static final Map<String, LinkedList<ChatMessage>> chatHistories = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY_PER_CHANNEL = 50;

    public static void load(MinecraftServer server) {
        if (server == null || server.getWorld(0) == null) {
            System.err.println("ChatManager: Cannot load, server or overworld is null!");
            return;
        }
        MapStorage storage = server.getWorld(0).getMapStorage();
        chatData = (ChatWorldData) storage.getOrLoadData(ChatWorldData.class, CHAT_DATA_ID);
        if (chatData == null) {
            chatData = new ChatWorldData(CHAT_DATA_ID);
            storage.setData(CHAT_DATA_ID, chatData);
        }

        if (chatData.getChannel(ChatChannel.COMMON_CHANNEL_ID_PREFIX) == null) {
            ChatChannel commonChannel = new ChatChannel(ChatChannel.COMMON_CHANNEL_ID_PREFIX, "Общий", ChatChannelType.COMMON_SERVER_WIDE);
            chatData.addOrUpdateChannel(commonChannel);
        }

        chatHistories.clear();
        chatData.loadHistories(chatHistories, MAX_HISTORY_PER_CHANNEL);
    }

    public static void save(MinecraftServer server) {
        if (chatData != null && server != null && server.getWorld(0) != null) {
            chatData.saveHistories(chatHistories);
            MapStorage storage = server.getWorld(0).getMapStorage();
            storage.setData(CHAT_DATA_ID, chatData);
            System.out.println("[ExampleMod] ChatManager data saved.");
        } else {
            System.err.println("[ExampleMod] ChatManager could not save data (chatData or server is null).");
        }
    }

    @Nullable
    private static Pair<UUID, User> getKPKUserAndOwner(EntityPlayerMP player) {
        ItemStack mainHand = player.getHeldItemMainhand();
        if (!(mainHand.getItem() instanceof ItemKPK) || !ItemKPK.isEnabled(mainHand)) {
            mainHand = player.getHeldItemOffhand();
        }

        if (mainHand.getItem() instanceof ItemKPK && ItemKPK.isEnabled(mainHand)) {
            User kpkUser = ItemKPK.getUserData(mainHand);
            if (kpkUser != null && kpkUser.pozivnoy != null && !kpkUser.pozivnoy.isEmpty()) {
                Pair<UUID, User> serverData = KPKServerManager.findUserByCallsign(kpkUser.pozivnoy);
                if (serverData != null) {
                    return serverData;
                }
            }
        }

        User ownUser = KPKServerManager.getUser(player.getUniqueID());
        if (ownUser != null) {
            return Pair.of(player.getUniqueID(), ownUser);
        }

        return null;
    }

    public static boolean isPlayerHoldingEnabledKPK(EntityPlayerMP player) {
        if (player == null) return false;

        ItemStack mainHand = player.getHeldItemMainhand();
        if (mainHand.getItem() instanceof ItemKPK && ItemKPK.isEnabled(mainHand)) {
            return true;
        }

        ItemStack offHand = player.getHeldItemOffhand();
        if (offHand.getItem() instanceof ItemKPK && ItemKPK.isEnabled(offHand)) {
            return true;
        }

        return false;
    }

    public static List<ChatChannel> getSubscribedChannels(EntityPlayerMP player) {
        if (chatData == null) return Collections.emptyList();

        List<ChatChannel> result = new ArrayList<>();

        ChatChannel commonChannel = chatData.getChannel(ChatChannel.COMMON_CHANNEL_ID_PREFIX);
        if (commonChannel != null) {
            result.add(commonChannel);
        }

        ItemStack kpkStack = getKPKItemStack(player);
        if (kpkStack != null) {
            List<String> privateChannelIds = ItemKPK.getPrivateChannelIds(kpkStack);
            for (String channelId : privateChannelIds) {
                ChatChannel channel = chatData.getChannel(channelId);
                if (channel != null) {
                    result.add(channel);
                }
            }
        }

        return result;
    }

    @Nullable
    private static ItemStack getKPKItemStack(EntityPlayerMP player) {
        ItemStack mainHand = player.getHeldItemMainhand();
        if (mainHand.getItem() instanceof ItemKPK && ItemKPK.isEnabled(mainHand)) {
            return mainHand;
        }
        ItemStack offHand = player.getHeldItemOffhand();
        if (offHand.getItem() instanceof ItemKPK && ItemKPK.isEnabled(offHand)) {
            return offHand;
        }
        return null;
    }

    @Nullable
    public static ChatChannel getChannel(String channelId) {
        if (chatData == null) return null;
        return chatData.getChannel(channelId);
    }

    public static boolean createPrivateMessageChannel(EntityPlayerMP creatorPlayer, UUID targetPlayerUuid) {
        if (chatData == null) return false;

        ItemStack creatorKpk = getKPKItemStack(creatorPlayer);
        if (creatorKpk == null) {
            creatorPlayer.sendMessage(new TextComponentString(TextFormatting.RED + "Ваш КПК не инициализирован или вы не держите его в руке."));
            return false;
        }
        User creatorUser = ItemKPK.getUserData(creatorKpk);
        if (creatorUser == null) {
            creatorPlayer.sendMessage(new TextComponentString(TextFormatting.RED + "Ваш КПК не настроен."));
            return false;
        }
        UUID creatorUuid = creatorPlayer.getUniqueID();

        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        EntityPlayerMP targetPlayer = server.getPlayerList().getPlayerByUUID(targetPlayerUuid);

        if (targetPlayer == null) {
            creatorPlayer.sendMessage(new TextComponentString(TextFormatting.RED + "Целевой игрок не найден (оффлайн?)."));
            return false;
        }

        ItemStack targetKpk = getKPKItemStack(targetPlayer);
        if (targetKpk == null) {
            creatorPlayer.sendMessage(new TextComponentString(TextFormatting.RED + "КПК целевого игрока не инициализирован или не находится в руке."));
            return false;
        }
        User targetUser = ItemKPK.getUserData(targetKpk);
        if (targetUser == null) {
            creatorPlayer.sendMessage(new TextComponentString(TextFormatting.RED + "КПК целевого игрока не настроен."));
            return false;
        }
        UUID targetUuid = targetPlayer.getUniqueID();

        boolean isSelfChat = creatorUuid.equals(targetUuid);

        String channelId = ChatChannel.generatePMChannelId(creatorUuid, targetUuid);
        if (chatData.getChannel(channelId) != null) {
            creatorPlayer.sendMessage(new TextComponentString(TextFormatting.YELLOW + "ЛС с " + targetPlayer.getName() + " уже существует."));
            return true;
        }

        List<UUID> members = new ArrayList<>();
        members.add(creatorUuid);
        if (!isSelfChat) {
            members.add(targetUuid);
        }

        String displayName = "ЛС: " + creatorUser.pozivnoy + " / " + targetUser.pozivnoy;
        ChatChannel pmChannel = new ChatChannel(channelId, displayName, ChatChannelType.PRIVATE_MESSAGE, creatorUuid, members, 2);

        chatData.addOrUpdateChannel(pmChannel);
        chatHistories.putIfAbsent(channelId, new LinkedList<>());
        chatData.markDirty();

        ItemKPK.addPrivateChannelId(creatorKpk, channelId);
        if (!isSelfChat) {
            ItemKPK.addPrivateChannelId(targetKpk, channelId);
        }

        PacketNotifyChannelCreatedOrUpdated packet = new PacketNotifyChannelCreatedOrUpdated(pmChannel);

        EntityPlayerMP creatorOnline = server.getPlayerList().getPlayerByUUID(creatorUuid);
        if(creatorOnline != null) PacketHandler.INSTANCE.sendTo(packet, creatorOnline);

        if (!isSelfChat) {
            EntityPlayerMP targetOnline = server.getPlayerList().getPlayerByUUID(targetUuid);
            if(targetOnline != null) PacketHandler.INSTANCE.sendTo(packet, targetOnline);
        }

        creatorPlayer.sendMessage(new TextComponentString(TextFormatting.GREEN + "ЛС с " + targetPlayer.getName() + " создано."));
        if (!isSelfChat) {
            targetPlayer.sendMessage(new TextComponentString(TextFormatting.GREEN + creatorPlayer.getName() + " создал(а) ЛС с вами."));
        }
        return true;
    }

    public static boolean createPrivateGroupChannel(EntityPlayerMP creatorPlayer, String channelName, List<UUID> targetMemberUuidsFromContacts) {
        if (chatData == null) return false;

        Pair<UUID, User> creatorIdentity = getKPKUserAndOwner(creatorPlayer);
        if (creatorIdentity == null) {
            creatorPlayer.sendMessage(new TextComponentString(TextFormatting.RED + "Ваш КПК не инициализирован или вы не держите его в руке."));
            return false;
        }
        UUID creatorUuid = creatorIdentity.getLeft();

        if (channelName == null || channelName.trim().isEmpty() || channelName.length() > 20 || channelName.length() < 2) {
            creatorPlayer.sendMessage(new TextComponentString(TextFormatting.RED + "Неверное имя канала (2-20 симв)."));
            return false;
        }
        if (targetMemberUuidsFromContacts.size() > 2) {
            creatorPlayer.sendMessage(new TextComponentString(TextFormatting.RED + "Слишком много участников для ЗК (макс 2 выбранных + вы)."));
            return false;
        }

        String displayName = "ЗК: " + channelName.trim();
        for (ChatChannel existingChannel : chatData.getAllChannels()) {
            if (existingChannel.getType() == ChatChannelType.PRIVATE_GROUP && existingChannel.getDisplayName().equalsIgnoreCase(displayName)) {
                creatorPlayer.sendMessage(new TextComponentString(TextFormatting.RED + "Канал с названием '" + channelName.trim() + "' уже существует."));
                return false;
            }
        }

        List<UUID> allMemberUuids = new ArrayList<>(targetMemberUuidsFromContacts);
        if (!allMemberUuids.contains(creatorUuid)) {
            allMemberUuids.add(creatorUuid);
        }
        if (allMemberUuids.size() > 3) {
            creatorPlayer.sendMessage(new TextComponentString(TextFormatting.RED + "Общее число участников превышает 3."));
            return false;
        }

        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        for(UUID memberId : targetMemberUuidsFromContacts) {
            EntityPlayerMP memberPlayer = server.getPlayerList().getPlayerByUUID(memberId);
            if(memberPlayer == null) {
                creatorPlayer.sendMessage(new TextComponentString(TextFormatting.RED + "Один из выбранных участников не найден (оффлайн?)."));
                return false;
            }
            if (getKPKUserAndOwner(memberPlayer) == null){
                creatorPlayer.sendMessage(new TextComponentString(TextFormatting.RED + "У участника " + memberPlayer.getName() + " не настроен КПК или он не в руке."));
                return false;
            }
        }

        String channelId = ChatChannel.generateGroupChannelId(creatorUuid, channelName);
        ChatChannel groupChannel = new ChatChannel(channelId, displayName, ChatChannelType.PRIVATE_GROUP, creatorUuid, allMemberUuids, 3);
        chatData.addOrUpdateChannel(groupChannel);
        chatHistories.putIfAbsent(channelId, new LinkedList<>());
        chatData.markDirty();

        creatorPlayer.sendMessage(new TextComponentString(TextFormatting.GREEN + "Закрытый канал '" + displayName + "' создан."));
        PacketNotifyChannelCreatedOrUpdated packet = new PacketNotifyChannelCreatedOrUpdated(groupChannel);
        for (UUID memberUuid : allMemberUuids) {
            EntityPlayerMP memberPlayer = server.getPlayerList().getPlayerByUUID(memberUuid);
            if (memberPlayer != null) {
                ItemStack memberKpk = getKPKItemStack(memberPlayer);
                if (memberKpk != null) {
                    ItemKPK.addPrivateChannelId(memberKpk, channelId);
                }
                PacketHandler.INSTANCE.sendTo(packet, memberPlayer);
                if (!memberUuid.equals(creatorUuid)) {
                    memberPlayer.sendMessage(new TextComponentString(TextFormatting.GREEN + creatorPlayer.getName() + " добавил(а) вас в ЗК '" + displayName + "'."));
                }
            }
        }
        return true;
    }

    public static boolean deleteChannel(String channelId, EntityPlayerMP requesterPlayer) {
        if (chatData == null || channelId == null || requesterPlayer == null) return false;

        if (channelId.startsWith(ChatChannel.COMMON_CHANNEL_ID_PREFIX)) {
            requesterPlayer.sendMessage(new TextComponentString(TextFormatting.RED + "Нельзя покинуть общий канал."));
            return false;
        }

        ChatChannel channel = chatData.getChannel(channelId);
        if (channel == null) {
            requesterPlayer.sendMessage(new TextComponentString(TextFormatting.RED + "Канал не найден."));
            return false;
        }

        Pair<UUID, User> requesterIdentity = getKPKUserAndOwner(requesterPlayer);
        if (requesterIdentity == null) {
            requesterPlayer.sendMessage(new TextComponentString(TextFormatting.RED + "Ваш КПК не инициализирован."));
            return false;
        }
        UUID requesterUuid = requesterIdentity.getLeft();
        String requesterName = requesterIdentity.getRight().pozivnoy;

        if (!channel.isMember(requesterUuid)) {
            requesterPlayer.sendMessage(new TextComponentString(TextFormatting.RED + "Вы не являетесь участником этого канала."));
            return false;
        }

        MinecraftServer server = requesterPlayer.getServer();
        List<UUID> originalMembers = new ArrayList<>(channel.getMembers());

        switch (channel.getType()) {
            case PRIVATE_MESSAGE: {
                PacketNotifyChannelDeleted packet = new PacketNotifyChannelDeleted(channelId);
                for (UUID memberUuid : originalMembers) {
                    EntityPlayerMP memberPlayer = server.getPlayerList().getPlayerByUUID(memberUuid);
                    if (memberPlayer != null) {
                        ItemStack memberKpk = getKPKItemStack(memberPlayer);
                        if (memberKpk != null) {
                            ItemKPK.removePrivateChannelId(memberKpk, channelId);
                        }
                        PacketHandler.INSTANCE.sendTo(packet, memberPlayer);
                        if (memberUuid.equals(requesterUuid)) {
                            memberPlayer.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Вы удалили чат '" + channel.getDisplayName() + "'."));
                        } else {
                            memberPlayer.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Чат '" + channel.getDisplayName() + "' был удален пользователем " + requesterName + "."));
                        }
                    }
                }
                chatData.removeChannel(channelId);
                chatHistories.remove(channelId);
                break;
            }
            case PRIVATE_GROUP: {
                if (channel.getCreatorUuid().equals(requesterUuid)) {
                    // Creator disbands the channel
                    String messageToMembers = TextFormatting.YELLOW + "Канал '" + channel.getDisplayName() + "' был расформирован создателем.";
                    PacketNotifyChannelDeleted packet = new PacketNotifyChannelDeleted(channelId);

                    for (UUID memberUuid : originalMembers) {
                        EntityPlayerMP memberPlayer = server.getPlayerList().getPlayerByUUID(memberUuid);
                        if (memberPlayer != null) {
                            ItemStack memberKpk = getKPKItemStack(memberPlayer);
                            if (memberKpk != null) {
                                ItemKPK.removePrivateChannelId(memberKpk, channelId);
                            }
                            if (!memberUuid.equals(requesterUuid)) {
                                memberPlayer.sendMessage(new TextComponentString(messageToMembers));
                            }
                            PacketHandler.INSTANCE.sendTo(packet, memberPlayer);
                        }
                    }
                    requesterPlayer.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Канал '" + channel.getDisplayName() + "' расформирован."));
                    chatData.removeChannel(channelId);
                    chatHistories.remove(channelId);
                } else {
                    // A member leaves the channel
                    channel.removeMember(requesterUuid);
                    chatData.addOrUpdateChannel(channel);
                    requesterPlayer.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Вы покинули канал '" + channel.getDisplayName() + "'."));

                    ItemStack requesterKpk = getKPKItemStack(requesterPlayer);
                    if (requesterKpk != null) {
                        ItemKPK.removePrivateChannelId(requesterKpk, channelId);
                    }

                    PacketHandler.INSTANCE.sendTo(new PacketNotifyChannelDeleted(channelId), requesterPlayer);

                    String messageToMembers = TextFormatting.YELLOW + "Участник " + requesterName + " покинул(а) канал '" + channel.getDisplayName() + "'.";
                    PacketNotifyChannelCreatedOrUpdated updatePacket = new PacketNotifyChannelCreatedOrUpdated(channel);

                    for (UUID memberUuid : channel.getMembers()) {
                        EntityPlayerMP memberPlayer = server.getPlayerList().getPlayerByUUID(memberUuid);
                        if (memberPlayer != null) {
                            memberPlayer.sendMessage(new TextComponentString(messageToMembers));
                            PacketHandler.INSTANCE.sendTo(updatePacket, memberPlayer);
                        }
                    }
                }
                break;
            }
        }
        chatData.markDirty();
        return true;
    }

    public static void addMessageToChannel(EntityPlayerMP senderPlayer, ChatMessage chatMessage) {
        if (chatData == null || chatMessage == null || chatMessage.channelId == null || senderPlayer == null) return;

        ChatChannel channel = chatData.getChannel(chatMessage.channelId);
        if (channel == null) {
            System.err.println("[ExampleMod] Attempted to add message to non-existent channel: " + chatMessage.channelId);
            return;
        }

        Pair<UUID, User> senderIdentity = getKPKUserAndOwner(senderPlayer);
        if (senderIdentity == null) {
            senderPlayer.sendMessage(new TextComponentString(TextFormatting.RED + "Не удалось отправить сообщение: КПК не инициализирован."));
            return;
        }

        if (!channel.isMember(senderIdentity.getLeft()) && !chatMessage.isAnonymous) {
            System.err.println("[ExampleMod] Player " + chatMessage.senderPlayerName + " tried to send message to channel " + channel.getChannelId() + " they are not a member of.");
            senderPlayer.sendMessage(new TextComponentString(TextFormatting.RED + "Вы не являетесь участником канала '" + channel.getDisplayName() + "'."));
            return;
        }

        LinkedList<ChatMessage> history = chatHistories.computeIfAbsent(chatMessage.channelId, k -> new LinkedList<>());
        synchronized (history) {
            history.add(chatMessage);
            while (history.size() > MAX_HISTORY_PER_CHANNEL) {
                history.removeFirst();
            }
        }
        chatData.updateSingleHistoryNBT(chatMessage.channelId, history);

        PacketBroadcastChatMessageToClient packet = new PacketBroadcastChatMessageToClient(chatMessage);
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) return;

        PacketNotifyNewMessage notificationPacket = null;
        if (channel.getType() == ChatChannelType.PRIVATE_MESSAGE || channel.getType() == ChatChannelType.PRIVATE_GROUP) {
            notificationPacket = new PacketNotifyNewMessage(channel.getType());
        }

        if (channel.getType() == ChatChannelType.COMMON_SERVER_WIDE) {
            for (EntityPlayerMP p : server.getPlayerList().getPlayers()) {
                if (getKPKUserAndOwner(p) != null) {
                    PacketHandler.INSTANCE.sendTo(packet, p);
                }
            }
        } else {
            for (UUID memberUuid : channel.getMembers()) {
                EntityPlayerMP memberPlayer = server.getPlayerList().getPlayerByUUID(memberUuid);
                if (memberPlayer != null) {
                    PacketHandler.INSTANCE.sendTo(packet, memberPlayer);
                    if (notificationPacket != null && !isPlayerHoldingEnabledKPK(memberPlayer) && !memberUuid.equals(senderPlayer.getUniqueID())) {
                        PacketHandler.INSTANCE.sendTo(notificationPacket, memberPlayer);
                    }
                }
            }
        }

        // JSON logging
        try {
            ChatJsonLogger.log(server, channel, chatMessage);
        } catch (Throwable ignored) {}
    }

    public static List<ChatMessage> getChatHistory(String channelId) {
        LinkedList<ChatMessage> history = chatHistories.get(channelId);
        if (history != null) {
            synchronized (history) {
                return new ArrayList<>(history);
            }
        }
        return Collections.emptyList();
    }

    public static void onPlayerLogin(EntityPlayerMP player) {
        if (chatData == null) {
            load(player.mcServer);
            if (chatData == null) {
                System.err.println("[ExampleMod] ChatManager.onPlayerLogin: chatData is STILL null after load. Cannot sync chat for " + player.getName());
                return;
            }
        }

        List<ChatChannel> subscribed = getSubscribedChannels(player);
        PacketHandler.INSTANCE.sendTo(new PacketSyncSubscribedChannels(subscribed), player);

        for (ChatChannel channel : subscribed) {
            List<ChatMessage> history = getChatHistory(channel.getChannelId());
            if (!history.isEmpty()) {
                PacketHandler.INSTANCE.sendTo(new PacketSyncChatHistoryToClient(channel.getChannelId(), history), player);
            }
        }
    }

    public static boolean addPlayerToGroupChannelByCallsign(String channelId, String targetCallsign, EntityPlayerMP addingPlayer) {
        if (chatData == null) return false;
        ChatChannel channel = chatData.getChannel(channelId);
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();

        if (channel == null || channel.getType() != ChatChannelType.PRIVATE_GROUP) {
            addingPlayer.sendMessage(new TextComponentString(TextFormatting.RED + "Канал не найден или не является групповым."));
            return false;
        }

        Pair<UUID, User> adderIdentity = getKPKUserAndOwner(addingPlayer);
        if (adderIdentity == null) {
            addingPlayer.sendMessage(new TextComponentString(TextFormatting.RED + "Ваш КПК не настроен."));
            return false;
        }

        if (!channel.getCreatorUuid().equals(adderIdentity.getLeft())) {
            addingPlayer.sendMessage(new TextComponentString(TextFormatting.RED + "Только создатель канала может добавлять участников."));
            return false;
        }

        EntityPlayerMP playerToAdd = null;
        Pair<UUID, User> targetIdentity = null;
        for (EntityPlayerMP p : server.getPlayerList().getPlayers()) {
            Pair<UUID, User> identity = getKPKUserAndOwner(p);
            if (identity != null && targetCallsign.equalsIgnoreCase(identity.getRight().pozivnoy)) {
                playerToAdd = p;
                targetIdentity = identity;
                break;
            }
        }

        if (playerToAdd == null || targetIdentity == null) {
            addingPlayer.sendMessage(new TextComponentString(TextFormatting.RED + "Игрок с позывным '" + targetCallsign + "' не найден или его КПК не настроен/не в руке."));
            return false;
        }

        if (channel.isMember(targetIdentity.getLeft())) {
            addingPlayer.sendMessage(new TextComponentString(TextFormatting.YELLOW + playerToAdd.getName() + " уже в канале."));
            return false;
        }
        if (channel.getMembers().size() >= channel.getMaxMembers()) {
            addingPlayer.sendMessage(new TextComponentString(TextFormatting.RED + "Не удалось добавить " + playerToAdd.getName() + ". Канал полон."));
            return false;
        }

        channel.addMember(targetIdentity.getLeft());
        chatData.addOrUpdateChannel(channel);

        ItemStack targetKpk = getKPKItemStack(playerToAdd);
        if (targetKpk != null) {
            ItemKPK.addPrivateChannelId(targetKpk, channelId);
        }

        PacketNotifyChannelCreatedOrUpdated packetUpdate = new PacketNotifyChannelCreatedOrUpdated(channel);
        String notification = TextFormatting.GREEN + addingPlayer.getName() + " добавил(а) " + playerToAdd.getName() + " в ЗК '" + channel.getDisplayName() + "'.";

        for (UUID memberUuid : channel.getMembers()) {
            EntityPlayerMP memberPlayer = server.getPlayerList().getPlayerByUUID(memberUuid);
            if (memberPlayer != null) {
                PacketHandler.INSTANCE.sendTo(packetUpdate, memberPlayer);
                if (memberUuid.equals(targetIdentity.getLeft())) {
                    memberPlayer.sendMessage(new TextComponentString(TextFormatting.GREEN + addingPlayer.getName() + " добавил(а) вас в ЗК '" + channel.getDisplayName() + "'."));
                } else if(!memberUuid.equals(adderIdentity.getLeft())){
                    memberPlayer.sendMessage(new TextComponentString(notification));
                }
            }
        }
        addingPlayer.sendMessage(new TextComponentString(TextFormatting.GREEN + playerToAdd.getName() + " добавлен(а) в канал."));
        return true;
    }

    public static boolean removeMemberFromGroupChannel(String channelId, UUID memberToRemoveUuid, EntityPlayerMP requester) {
        if (chatData == null || channelId == null || memberToRemoveUuid == null || requester == null) return false;
        ChatChannel channel = chatData.getChannel(channelId);
        MinecraftServer server = requester.getServer();

        if (channel == null || channel.getType() != ChatChannelType.PRIVATE_GROUP) {
            requester.sendMessage(new TextComponentString(TextFormatting.RED + "Канал не найден или не является групповым."));
            return false;
        }

        Pair<UUID, User> requesterIdentity = getKPKUserAndOwner(requester);
        if (requesterIdentity == null) return false;

        if (channel.getCreatorUuid() == null || !channel.getCreatorUuid().equals(requesterIdentity.getLeft())) {
            requester.sendMessage(new TextComponentString(TextFormatting.RED + "Только создатель канала может исключать участников."));
            return false;
        }

        if (memberToRemoveUuid.equals(requesterIdentity.getLeft())) {
            requester.sendMessage(new TextComponentString(TextFormatting.RED + "Вы не можете исключить самого себя. Для выхода используйте кнопку удаления чата."));
            return false;
        }

        if (!channel.isMember(memberToRemoveUuid)) {
            requester.sendMessage(new TextComponentString(TextFormatting.RED + "Участник не найден в этом канале."));
            return false;
        }

        User removedUser = KPKServerManager.getUser(memberToRemoveUuid);
        String removedPlayerName = (removedUser != null) ? removedUser.pozivnoy : "???";

        channel.removeMember(memberToRemoveUuid);
        chatData.addOrUpdateChannel(channel);
        chatData.markDirty();

        requester.sendMessage(new TextComponentString(TextFormatting.GREEN + "Вы исключили " + removedPlayerName + " из канала '" + channel.getDisplayName() + "'."));

        EntityPlayerMP removedPlayer = server.getPlayerList().getPlayerByUUID(memberToRemoveUuid);
        if (removedPlayer != null) {
            ItemStack removedPlayerKpk = getKPKItemStack(removedPlayer);
            if (removedPlayerKpk != null) {
                ItemKPK.removePrivateChannelId(removedPlayerKpk, channelId);
            }
            removedPlayer.sendMessage(new TextComponentString(TextFormatting.YELLOW + "Вы были исключены из канала '" + channel.getDisplayName() + "'."));
            PacketHandler.INSTANCE.sendTo(new PacketNotifyChannelDeleted(channelId), removedPlayer);
        }

        PacketNotifyChannelCreatedOrUpdated packetUpdate = new PacketNotifyChannelCreatedOrUpdated(channel);
        for (UUID memberUuid : channel.getMembers()) {
            EntityPlayerMP member = server.getPlayerList().getPlayerByUUID(memberUuid);
            if (member != null) {
                if (!member.equals(requester)) {
                    member.sendMessage(new TextComponentString(TextFormatting.YELLOW + removedPlayerName + " был(а) исключен(а) из канала '" + channel.getDisplayName() + "'."));
                }
                PacketHandler.INSTANCE.sendTo(packetUpdate, member);
            }
        }
        return true;
    }

    public static class ChatWorldData extends WorldSavedData {
        private Map<String, ChatChannel> channels = new ConcurrentHashMap<>();
        private Map<String, NBTTagList> chatHistoriesNBT = new ConcurrentHashMap<>();

        public ChatWorldData(String name) {
            super(name);
        }

        public ChatWorldData(){
            super(CHAT_DATA_ID);
        }

        public void loadHistories(Map<String, LinkedList<ChatMessage>> target, int maxHistory) {
            for (Map.Entry<String, NBTTagList> entry : chatHistoriesNBT.entrySet()) {
                String channelId = entry.getKey();
                NBTTagList historyNbt = entry.getValue();
                LinkedList<ChatMessage> history = new LinkedList<>();
                for (int i = 0; i < historyNbt.tagCount(); i++) {
                    history.add(new ChatMessage(historyNbt.getCompoundTagAt(i)));
                }
                while(history.size() > maxHistory) {
                    history.removeFirst();
                }
                target.put(channelId, history);
            }
        }

        public void updateSingleHistoryNBT(String channelId, LinkedList<ChatMessage> singleHistory) {
            NBTTagList historyNbt = new NBTTagList();
            synchronized (singleHistory) {
                for (ChatMessage msg : singleHistory) {
                    historyNbt.appendTag(msg.toNBT());
                }
            }
            chatHistoriesNBT.put(channelId, historyNbt);
            markDirty();
        }

        public void saveHistories(Map<String, LinkedList<ChatMessage>> source) {
            chatHistoriesNBT.clear();
            for (Map.Entry<String, LinkedList<ChatMessage>> entry : source.entrySet()) {
                String channelId = entry.getKey();
                LinkedList<ChatMessage> history = entry.getValue();
                NBTTagList historyNbt = new NBTTagList();
                synchronized (history) {
                    for (ChatMessage msg : history) {
                        historyNbt.appendTag(msg.toNBT());
                    }
                }
                chatHistoriesNBT.put(channelId, historyNbt);
            }
            markDirty();
        }

        public ChatChannel getChannel(String channelId) {
            return channels.get(channelId);
        }

        public List<ChatChannel> getAllChannels() {
            return new ArrayList<>(channels.values());
        }

        public void addOrUpdateChannel(ChatChannel channel) {
            channels.put(channel.getChannelId(), channel);
            markDirty();
        }

        public void removeChannel(String channelId) {
            channels.remove(channelId);
            chatHistoriesNBT.remove(channelId);
            markDirty();
        }

        @Override
        public void readFromNBT(NBTTagCompound nbt) {
            NBTTagList channelsListNBT = nbt.getTagList("ChatChannels", Constants.NBT.TAG_COMPOUND);
            this.channels.clear();
            for (int i = 0; i < channelsListNBT.tagCount(); i++) {
                NBTTagCompound channelNBT = channelsListNBT.getCompoundTagAt(i);
                ChatChannel channel = ChatChannel.fromNBT(channelNBT);
                this.channels.put(channel.getChannelId(), channel);
            }

            this.chatHistoriesNBT.clear();
            if (nbt.hasKey("ChatHistories", Constants.NBT.TAG_COMPOUND)) {
                NBTTagCompound historiesRoot = nbt.getCompoundTag("ChatHistories");
                for (String channelId : historiesRoot.getKeySet()) {
                    this.chatHistoriesNBT.put(channelId, historiesRoot.getTagList(channelId, Constants.NBT.TAG_COMPOUND));
                }
            }
        }

        @Override
        public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
            NBTTagList channelsListNBT = new NBTTagList();
            for (ChatChannel channel : this.channels.values()) {
                channelsListNBT.appendTag(channel.toNBT());
            }
            nbt.setTag("ChatChannels", channelsListNBT);

            NBTTagCompound historiesRoot = new NBTTagCompound();
            for (Map.Entry<String, NBTTagList> entry : this.chatHistoriesNBT.entrySet()) {
                historiesRoot.setTag(entry.getKey(), entry.getValue());
            }
            nbt.setTag("ChatHistories", historiesRoot);

            return nbt;
        }
    }
}