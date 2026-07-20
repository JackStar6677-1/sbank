package com.spearforge.sBank.audit;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Writes one JSON record per money movement without flooding the server console. */
public final class BankAuditLogger {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JavaPlugin plugin;
    private final Object writeLock = new Object();

    public BankAuditLogger(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void record(String movement, String username, String uuid, double amount,
                       double walletBefore, double walletAfter,
                       double bankBefore, double bankAfter, String note) {
        if (!plugin.getConfig().getBoolean("audit.enabled", true)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        String line = "{"
                + "\"timestamp\":\"" + escape(TIMESTAMP.format(now)) + "\","
                + "\"movement\":\"" + escape(movement) + "\","
                + "\"username\":\"" + escape(username) + "\","
                + "\"uuid\":\"" + escape(uuid) + "\","
                + "\"amount\":" + amount + ","
                + "\"wallet_before\":" + walletBefore + ","
                + "\"wallet_after\":" + walletAfter + ","
                + "\"bank_before\":" + bankBefore + ","
                + "\"bank_after\":" + bankAfter + ","
                + "\"note\":\"" + escape(note) + "\""
                + "}" + System.lineSeparator();

        Path directory = plugin.getDataFolder().toPath()
                .resolve(plugin.getConfig().getString("audit.directory", "audit"));
        Path file = directory.resolve("transactions-" + FILE_DATE.format(now) + ".jsonl");

        synchronized (writeLock) {
            try {
                Files.createDirectories(directory);
                Files.write(file, line.getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException exception) {
                plugin.getLogger().warning("Could not write bank audit record: " + exception.getMessage());
            }
        }
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n");
    }
}
