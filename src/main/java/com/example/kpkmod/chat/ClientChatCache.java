package com.example.kpkmod.chat;

import com.example.kpkmod.User;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@SideOnly(Side.CLIENT)
public class ClientChatCache {
    private static final Map<String, LinkedList<ChatMessage>> chatHistories = new ConcurrentHashMap<>();
    private static final Map<String, ChatChannel> subscribedChannels = new ConcurrentHashMap<>();
    private static final int MAX_CLIENT_HISTORY_PER_CHANNEL = 50;

    private static final Map<UUID, String> knownCallsigns = new ConcurrentHashMap<>();
    private static final Map<UUID, User> kpkUserDatabase = new ConcurrentHashMap<>();
    public static UUID activeKpkOwnerUUID = null;

    private static final List<Runnable> onChatDataUpdatedListeners = new CopyOnWriteArrayList<>();

    public static void addOnChatDataUpdatedListener(Runnable listener) {
        if (listener != null && !onChatDataUpdatedListeners.contains(listener)) {
            onChatDataUpdatedListeners.add(listener);
        }
    }

    public static void removeOnChatDataUpdatedListener(Runnable listener) {
        onChatDataUpdatedListeners.remove(listener);
    }

    private static void notifyListeners() {
        for (Runnable listener : onChatDataUpdatedListeners) {
            listener.run();
        }
    }

    public static void addMessage(ChatMessage message) {
        if (message == null || message.channelId == null) return;

        LinkedList<ChatMessage> history = chatHistories.computeIfAbsent(message.channelId, k -> new LinkedList<>());
        synchronized (history) {
            history.add(message);
            while (history.size() > MAX_CLIENT_HISTORY_PER_CHANNEL) {
                history.removeFirst();
            }
        }
        if (message.senderUuid != null && message.senderCallsign != null) {
            knownCallsigns.put(message.senderUuid, message.senderCallsign);
        }
        notifyListeners();
    }

    public static void setHistory(String channelId, List<ChatMessage> messages) {
        if (channelId == null || messages == null) return;
        LinkedList<ChatMessage> history = new LinkedList<>(messages);
        while (history.size() > MAX_CLIENT_HISTORY_PER_CHANNEL) {
            history.removeFirst();
        }
        chatHistories.put(channelId, history);

        for (ChatMessage msg : messages) {
            if (msg.senderUuid != null && msg.senderCallsign != null) {
                knownCallsigns.put(msg.senderUuid, msg.senderCallsign);
            }
        }
        notifyListeners();
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

    public static void addOrUpdateChannel(ChatChannel channel) {
        if (channel == null) return;
        subscribedChannels.put(channel.getChannelId(), channel);
        chatHistories.putIfAbsent(channel.getChannelId(), new LinkedList<>());
        notifyListeners();
    }

    public static void setSubscribedChannels(List<ChatChannel> channels) {
        // ЭТО КЛЮЧЕВОЕ ИЗМЕНЕНИЕ: ПОЛНАЯ ОЧИСТКА ПЕРЕД ОБНОВЛЕНИЕМ
        subscribedChannels.clear();
        chatHistories.clear();

        for (ChatChannel channel : channels) {
            subscribedChannels.put(channel.getChannelId(), channel);
            chatHistories.putIfAbsent(channel.getChannelId(), new LinkedList<>());
        }

        notifyListeners();
    }

    public static void removeChannel(String channelId) {
        if (channelId == null) return;
        subscribedChannels.remove(channelId);
        chatHistories.remove(channelId);
        notifyListeners();
    }

    public static ChatChannel getChannel(String channelId) {
        return subscribedChannels.get(channelId);
    }

    public static String getCallsignForUUID(UUID uuid) {
        if (uuid == null) return "???";
        if (knownCallsigns.containsKey(uuid)) {
            return knownCallsigns.get(uuid);
        }
        User user = kpkUserDatabase.get(uuid);
        if (user != null) {
            return user.pozivnoy;
        }
        return uuid.toString().substring(0, 8);
    }

    public static List<ChatChannel> getSubscribedChannels() {
        return subscribedChannels.values().stream()
                .sorted((c1, c2) -> {
                    if (c1.getType() == ChatChannelType.COMMON_SERVER_WIDE) return -1;
                    if (c2.getType() == ChatChannelType.COMMON_SERVER_WIDE) return 1;
                    if (c1.getType().ordinal() != c2.getType().ordinal()) {
                        return Integer.compare(c1.getType().ordinal(), c2.getType().ordinal());
                    }
                    return c1.getDisplayName().compareToIgnoreCase(c2.getDisplayName());
                })
                .collect(Collectors.toList());
    }

    public static void clearCache() {
        chatHistories.clear();
        subscribedChannels.clear();
        knownCallsigns.clear();
        kpkUserDatabase.clear();
        activeKpkOwnerUUID = null;
        notifyListeners();
    }

    public static void setKpkUserDatabase(Map<UUID, User> database) {
        kpkUserDatabase.clear();
        kpkUserDatabase.putAll(database);
    }

    public static UUID findUserByCallsign(String callsign) {
        if (callsign == null || callsign.isEmpty()) return null;
        for (Map.Entry<UUID, User> entry : kpkUserDatabase.entrySet()) {
            if (callsign.equalsIgnoreCase(entry.getValue().pozivnoy)) {
                return entry.getKey();
            }
        }
        return null;
    }
}