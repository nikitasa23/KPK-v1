package com.example.examplemod.discord;

import com.example.examplemod.ExampleMod;

import javax.net.ssl.HttpsURLConnection;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class DiscordBridge {
    private static final int MAX_REQUESTS_PER_SECOND = 3;
    private static final Deque<Long> SEND_TIMESTAMPS = new ArrayDeque<>();
    private static ExecutorService EXECUTOR;

    public static void init(java.io.File configDir) {
        DiscordConfig.load(configDir);
        if (DiscordConfig.isWebhookEnabled()) {
            ExampleMod.logger.info("Discord webhook is configured. Common chat -> Discord forwarding enabled.");
        } else {
            ExampleMod.logger.info("Discord webhook is not configured. Forwarding disabled.");
        }
        if (EXECUTOR == null) {
            EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "KPK-DiscordBridge");
                    t.setDaemon(true);
                    return t;
                }
            });
        }
    }

    public static void shutdown() {
        if (EXECUTOR != null) {
            EXECUTOR.shutdown();
            try {
                EXECUTOR.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}
            EXECUTOR = null;
        }
    }

    public static void sendToDiscord(String content) {
        if (!DiscordConfig.isWebhookEnabled() || content == null || content.trim().isEmpty()) return;
        if (EXECUTOR == null) return;

        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                // Simple sliding-window rate limiter
                long now = System.nanoTime();
                long oneSecondAgo = now - TimeUnit.SECONDS.toNanos(1);
                synchronized (SEND_TIMESTAMPS) {
                    while (!SEND_TIMESTAMPS.isEmpty() && SEND_TIMESTAMPS.peekFirst() < oneSecondAgo) {
                        SEND_TIMESTAMPS.pollFirst();
                    }
                    if (SEND_TIMESTAMPS.size() >= MAX_REQUESTS_PER_SECOND) {
                        long oldest = SEND_TIMESTAMPS.peekFirst();
                        long waitNanos = (oldest + TimeUnit.SECONDS.toNanos(1)) - now;
                        if (waitNanos > 0) {
                            try {
                                Thread.sleep(TimeUnit.NANOSECONDS.toMillis(waitNanos));
                            } catch (InterruptedException ignored) {}
                        }
                        // purge again after wait
                        long now2 = System.nanoTime();
                        long oneSecondAgo2 = now2 - TimeUnit.SECONDS.toNanos(1);
                        while (!SEND_TIMESTAMPS.isEmpty() && SEND_TIMESTAMPS.peekFirst() < oneSecondAgo2) {
                            SEND_TIMESTAMPS.pollFirst();
                        }
                    }
                    SEND_TIMESTAMPS.addLast(System.nanoTime());
                }
                doSend(content);
            }
        });
    }

    private static void doSend(String content) {
        String url = DiscordConfig.getWebhookUrl();
        try {
            String payload = toJsonPayload(content);
            byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
            URL webhook = new URL(url);
            HttpsURLConnection conn = (HttpsURLConnection) webhook.openConnection();
            // Timeouts to avoid hanging threads/network stalls
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(6000);
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
            conn.disconnect();
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


