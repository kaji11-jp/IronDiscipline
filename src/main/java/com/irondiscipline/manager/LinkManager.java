package com.irondiscipline.manager;

import com.irondiscipline.IronDiscipline;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Discord-Minecraft アカウント連携マネージャー
 */
public class LinkManager {

    private final IronDiscipline plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    // 認証コード -> Discord ID (一時的、有効期限5分)
    private final Map<String, PendingLink> pendingLinks = new ConcurrentHashMap<>();
    
    // Discord ID -> Minecraft UUID
    private final Map<Long, UUID> discordToMinecraft = new ConcurrentHashMap<>();
    
    // Minecraft UUID -> Discord ID
    private final Map<UUID, Long> minecraftToDiscord = new ConcurrentHashMap<>();
    
    private File dataFile;
    private final SecureRandom random = new SecureRandom();
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    public LinkManager(IronDiscipline plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "links.json");
        loadData();
        
        // 期限切れコードのクリーンアップ（1分毎）
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::cleanupExpiredCodes, 20 * 60, 20 * 60);
    }

    /**
     * 認証コード生成（Discord側で呼ばれる）
     */
    public String generateLinkCode(long discordId) {
        // 既存のコードがあれば削除
        pendingLinks.entrySet().removeIf(e -> e.getValue().discordId == discordId);
        
        // 新しいコード生成
        String code = generateRandomCode();
        pendingLinks.put(code, new PendingLink(discordId, System.currentTimeMillis() + 5 * 60 * 1000));
        
        return code;
    }

    /**
     * 連携実行（Minecraft側で呼ばれる）
     */
    public LinkResult attemptLink(UUID minecraftId, String code) {
        PendingLink pending = pendingLinks.remove(code.toUpperCase());
        
        if (pending == null) {
            return LinkResult.INVALID_CODE;
        }
        
        if (System.currentTimeMillis() > pending.expiresAt) {
            return LinkResult.EXPIRED;
        }
        
        // 既存の連携があれば解除
        Long existingDiscord = minecraftToDiscord.get(minecraftId);
        if (existingDiscord != null) {
            discordToMinecraft.remove(existingDiscord);
        }
        
        UUID existingMinecraft = discordToMinecraft.get(pending.discordId);
        if (existingMinecraft != null) {
            minecraftToDiscord.remove(existingMinecraft);
        }
        
        // 新しい連携
        discordToMinecraft.put(pending.discordId, minecraftId);
        minecraftToDiscord.put(minecraftId, pending.discordId);
        
        saveData();
        
        return LinkResult.SUCCESS;
    }

    /**
     * 連携解除
     */
    public boolean unlink(UUID minecraftId) {
        Long discordId = minecraftToDiscord.remove(minecraftId);
        if (discordId != null) {
            discordToMinecraft.remove(discordId);
            saveData();
            return true;
        }
        return false;
    }

    public boolean unlinkByDiscord(long discordId) {
        UUID minecraftId = discordToMinecraft.remove(discordId);
        if (minecraftId != null) {
            minecraftToDiscord.remove(minecraftId);
            saveData();
            return true;
        }
        return false;
    }

    /**
     * 連携状態を取得
     */
    public boolean isLinked(UUID minecraftId) {
        return minecraftToDiscord.containsKey(minecraftId);
    }

    public boolean isLinked(long discordId) {
        return discordToMinecraft.containsKey(discordId);
    }

    public Long getDiscordId(UUID minecraftId) {
        return minecraftToDiscord.get(minecraftId);
    }

    public UUID getMinecraftId(long discordId) {
        return discordToMinecraft.get(discordId);
    }

    /**
     * 全連携プレイヤー取得
     */
    public Set<UUID> getAllLinkedPlayers() {
        return new HashSet<>(minecraftToDiscord.keySet());
    }

    public int getLinkCount() {
        return minecraftToDiscord.size();
    }

    private String generateRandomCode() {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private void cleanupExpiredCodes() {
        long now = System.currentTimeMillis();
        pendingLinks.entrySet().removeIf(e -> now > e.getValue().expiresAt);
    }

    private void saveData() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                LinkData data = new LinkData();
                for (Map.Entry<Long, UUID> entry : discordToMinecraft.entrySet()) {
                    data.links.put(entry.getKey().toString(), entry.getValue().toString());
                }
                
                try (Writer writer = new FileWriter(dataFile)) {
                    gson.toJson(data, writer);
                }
            } catch (IOException e) {
                plugin.getLogger().warning("連携データ保存失敗: " + e.getMessage());
            }
        });
    }

    private void loadData() {
        if (!dataFile.exists()) {
            return;
        }
        
        try (Reader reader = new FileReader(dataFile)) {
            LinkData data = gson.fromJson(reader, LinkData.class);
            if (data != null && data.links != null) {
                for (Map.Entry<String, String> entry : data.links.entrySet()) {
                    try {
                        long discordId = Long.parseLong(entry.getKey());
                        UUID minecraftId = UUID.fromString(entry.getValue());
                        discordToMinecraft.put(discordId, minecraftId);
                        minecraftToDiscord.put(minecraftId, discordId);
                    } catch (Exception ignored) {}
                }
            }
            plugin.getLogger().info("連携データ読み込み完了: " + minecraftToDiscord.size() + "件");
        } catch (IOException e) {
            plugin.getLogger().warning("連携データ読み込み失敗: " + e.getMessage());
        }
    }

    // データクラス
    private static class PendingLink {
        final long discordId;
        final long expiresAt;
        
        PendingLink(long discordId, long expiresAt) {
            this.discordId = discordId;
            this.expiresAt = expiresAt;
        }
    }

    private static class LinkData {
        Map<String, String> links = new HashMap<>();
    }

    public enum LinkResult {
        SUCCESS,
        INVALID_CODE,
        EXPIRED
    }
}
