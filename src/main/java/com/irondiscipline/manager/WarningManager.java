package com.irondiscipline.manager;

import com.irondiscipline.IronDiscipline;
import com.irondiscipline.model.Rank;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 警告マネージャー
 * 警告の蓄積と自動処分
 */
public class WarningManager {

    private final IronDiscipline plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    // プレイヤー -> 警告リスト
    private final Map<UUID, List<Warning>> warnings = new ConcurrentHashMap<>();
    
    // 設定値
    private int jailThreshold = 3;
    private int kickThreshold = 5;
    
    private File dataFile;

    public WarningManager(IronDiscipline plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "warnings.json");
        loadData();
    }

    /**
     * 警告を追加
     */
    public int addWarning(UUID playerId, String playerName, String reason, UUID warnedBy) {
        List<Warning> playerWarnings = warnings.computeIfAbsent(playerId, k -> new ArrayList<>());
        
        Warning warning = new Warning();
        warning.reason = reason;
        warning.warnedBy = warnedBy != null ? warnedBy.toString() : "CONSOLE";
        warning.timestamp = System.currentTimeMillis();
        
        playerWarnings.add(warning);
        saveData();
        
        int count = playerWarnings.size();
        
        // 自動処分
        Player target = Bukkit.getPlayer(playerId);
        if (target != null && target.isOnline()) {
            if (count >= kickThreshold) {
                // 5回以上 → キック
                Bukkit.getScheduler().runTask(plugin, () -> {
                    target.kickPlayer("§c警告が" + kickThreshold + "回に達したため、キックされました。");
                });
            } else if (count >= jailThreshold) {
                // 3回以上 → 隔離
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getJailManager().jail(target, null, "警告" + count + "回による自動隔離");
                });
            }
        }
        
        return count;
    }

    /**
     * 警告数を取得
     */
    public int getWarningCount(UUID playerId) {
        List<Warning> playerWarnings = warnings.get(playerId);
        return playerWarnings != null ? playerWarnings.size() : 0;
    }

    /**
     * 警告リストを取得
     */
    public List<Warning> getWarnings(UUID playerId) {
        return new ArrayList<>(warnings.getOrDefault(playerId, new ArrayList<>()));
    }

    /**
     * 警告をクリア
     */
    public void clearWarnings(UUID playerId) {
        warnings.remove(playerId);
        saveData();
    }

    /**
     * 最新の警告を削除
     */
    public boolean removeLastWarning(UUID playerId) {
        List<Warning> playerWarnings = warnings.get(playerId);
        if (playerWarnings != null && !playerWarnings.isEmpty()) {
            playerWarnings.remove(playerWarnings.size() - 1);
            if (playerWarnings.isEmpty()) {
                warnings.remove(playerId);
            }
            saveData();
            return true;
        }
        return false;
    }

    /**
     * データを保存
     */
    private void saveData() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Map<String, List<Warning>> data = new HashMap<>();
                for (Map.Entry<UUID, List<Warning>> entry : warnings.entrySet()) {
                    data.put(entry.getKey().toString(), entry.getValue());
                }
                
                try (Writer writer = new FileWriter(dataFile)) {
                    gson.toJson(data, writer);
                }
            } catch (IOException e) {
                plugin.getLogger().warning("警告データ保存失敗: " + e.getMessage());
            }
        });
    }

    /**
     * データを読み込み
     */
    private void loadData() {
        if (!dataFile.exists()) {
            return;
        }
        
        try (Reader reader = new FileReader(dataFile)) {
            java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<Map<String, List<Warning>>>(){}.getType();
            Map<String, List<Warning>> data = gson.fromJson(reader, type);
            if (data != null) {
                for (Map.Entry<String, List<Warning>> entry : data.entrySet()) {
                    try {
                        UUID uuid = UUID.fromString(entry.getKey());
                        warnings.put(uuid, new ArrayList<>(entry.getValue()));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
            plugin.getLogger().info("警告データ読み込み完了");
        } catch (IOException e) {
            plugin.getLogger().warning("警告データ読み込み失敗: " + e.getMessage());
        }
    }

    /**
     * 警告データクラス
     */
    public static class Warning {
        public String reason;
        public String warnedBy;
        public long timestamp;
        
        public String getFormattedDate() {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm");
            return sdf.format(new java.util.Date(timestamp));
        }
    }
}
