package com.example.examplemod.discord;

import com.example.examplemod.ExampleMod;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class DiscordConfig {
    private static final String CONFIG_FILE_NAME = "kpk-discord.properties";
    private static final String KEY_WEBHOOK_URL = "WEBHOOK_URL";

    private static volatile String webhookUrl = null;

    public static void load(File configDirectory) {
        if (configDirectory == null) return;
        File file = new File(configDirectory, CONFIG_FILE_NAME);

        if (!file.exists()) {
            // Create template config
            try {
                Properties template = new Properties();
                template.setProperty(KEY_WEBHOOK_URL, "");
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    String header = "KPK Discord bridge settings. Set WEBHOOK_URL to your Discord webhook URL.";
                    template.store(fos, header);
                }
            } catch (IOException e) {
                ExampleMod.logger.error("Failed to create Discord config: {}", e.getMessage());
            }
            webhookUrl = null;
            return;
        }

        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(file)) {
            props.load(fis);
            String url = props.getProperty(KEY_WEBHOOK_URL, "").trim();
            webhookUrl = url.isEmpty() ? null : url;
            if (webhookUrl == null) {
                ExampleMod.logger.warn("Discord webhook URL is not set. Set '{}' in {} to enable Discord forwarding.", KEY_WEBHOOK_URL, file.getAbsolutePath());
            }
        } catch (IOException e) {
            ExampleMod.logger.error("Failed to read Discord config: {}", e.getMessage());
            webhookUrl = null;
        }
    }

    public static boolean isWebhookEnabled() {
        return webhookUrl != null && !webhookUrl.isEmpty();
    }

    public static String getWebhookUrl() {
        return webhookUrl;
    }
}


