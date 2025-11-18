package com.example.examplemod.chat;

import com.example.examplemod.User;
import com.example.examplemod.kpk.KPKServerManager;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Writes chat history to JSON Lines files under the world save directory.
 * Structure:
 *   world/kpk_chat/common/history-YYYY-MM-DD.jsonl
 *   world/kpk_chat/pm/{CALLSIGN_A}__{CALLSIGN_B}/history-YYYY-MM-DD.jsonl
 *   world/kpk_chat/groups/{CALLSIGN1}__{CALLSIGN2}__.../history-YYYY-MM-DD.jsonl
 */
class ChatJsonLogger {
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd");

    static void log(MinecraftServer server, ChatChannel channel, ChatMessage msg) {
        try {
            if (server == null || channel == null || msg == null) return;
            File worldDir = server.getWorld(0).getSaveHandler().getWorldDirectory();
            if (worldDir == null) return;

            File base = new File(worldDir, "kpk_chat");
            if (!base.exists()) base.mkdirs();

            String date = DATE_FMT.format(new Date(msg.timestamp));
            File folder;
            switch (channel.getType()) {
                case COMMON_SERVER_WIDE:
                    folder = new File(base, "common");
                    break;
                case PRIVATE_MESSAGE:
                    folder = new File(base, "pm/" + buildPmFolderName(server, channel));
                    break;
                case PRIVATE_GROUP:
                default:
                    folder = new File(base, "groups/" + buildGroupFolderName(server, channel));
                    break;
            }
            if (!folder.exists()) folder.mkdirs();
            File file = new File(folder, "history-" + date + ".jsonl");

            String json = toSimpleLine(msg);
            try (FileWriter fw = new FileWriter(file, true)) {
                fw.write(json);
                fw.write("\n");
            }
        } catch (Throwable ignored) {
        }
    }

    private static String toSimpleLine(ChatMessage msg) {
        String time = escape(msg.getFormattedTimestamp());
        String original;
        if (msg.isAnonymous) {
            User u = (msg.senderUuid != null) ? KPKServerManager.getUser(msg.senderUuid) : null;
            original = (u != null && u.pozivnoy != null) ? u.pozivnoy : "";
        } else {
            original = msg.senderCallsign != null ? msg.senderCallsign : "";
        }
        String callsign = msg.isAnonymous ? ("Аноним(\"" + escape(original) + "\")") : escape(original);
        String content = escape(msg.messageContent);
        return "[" + time + "] " + callsign + ": " + content;
    }

    private static String buildPmFolderName(MinecraftServer server, ChatChannel channel) {
        List<String> callsigns = new ArrayList<>(channel.getMemberCallsigns());
        Collections.sort(callsigns, String.CASE_INSENSITIVE_ORDER);
        return sanitize(String.join("__", callsigns));
    }

    private static String buildGroupFolderName(MinecraftServer server, ChatChannel channel) {
        List<String> callsigns = new ArrayList<>(channel.getMemberCallsigns());
        Collections.sort(callsigns, String.CASE_INSENSITIVE_ORDER);
        return sanitize(String.join("__", callsigns));
    }

    private static String sanitize(String in) {
        if (in == null) return "";
        // Put '-' at the end to avoid range, '.' is literal in char class
        return in.replaceAll("[^A-Za-z0-9_.А-Яа-яЁё-]", "_");
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}


