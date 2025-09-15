package com.example.examplemod.discord;

import com.example.examplemod.ExampleMod;

import javax.net.ssl.HttpsURLConnection;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordBridge {
    public static void init(java.io.File configDir) {
        DiscordConfig.load(configDir);
        if (DiscordConfig.isWebhookEnabled()) {
            ExampleMod.logger.info("Discord webhook is configured. Common chat -> Discord forwarding enabled.");
        } else {
            ExampleMod.logger.info("Discord webhook is not configured. Forwarding disabled.");
        }
    }

    public static void shutdown() {
        // no-op for webhook mode
    }

    public static void sendToDiscord(String content) {
        if (!DiscordConfig.isWebhookEnabled() || content == null || content.trim().isEmpty()) return;
        String url = DiscordConfig.getWebhookUrl();
        try {
            String payload = toJsonPayload(content);
            byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
            URL webhook = new URL(url);
            HttpsURLConnection conn = (HttpsURLConnection) webhook.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("User-Agent", "KPK-DiscordBridge/1.0");
            conn.setFixedLengthStreamingMode(bytes.length);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bytes);
            }
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                ExampleMod.logger.warn("Discord webhook responded with status {}", code);
            }
        } catch (Exception e) {
            ExampleMod.logger.error("Failed to send message to Discord: {}", e.getMessage());
        }
    }

    private static String toJsonPayload(String content) {
        String escaped = content
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
        return "{\"content\":\"" + escaped + "\"}";
    }
}


