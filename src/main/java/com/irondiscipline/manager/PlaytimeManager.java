package com.irondiscipline.manager;

import com.irondiscipline.IronDiscipline;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 勤務時間マネージャー
 * ログイン/ログアウト時間を記録
 */
public class PlaytimeManager implements Listener {

    private final IronDiscipline plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    // プレイヤー -> 累計勤務時間(ミリ秒)
    private final Map<UUID, Long> totalPlaytime = new ConcurrentHashMap<>();
    
    // 現在のセッション開始時刻
    private final Map<UUID, Long> sessionStart = new ConcurrentHashMap<>();
    
    private File dataFile;

    public PlaytimeManager(IronDiscipline plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playtime.json");
        loadData();
        
        // リスナー登録
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // 現在オンラインのプレイヤーのセッション開始
        for (Player player : Bukkit.getOnlinePlayers()) {
            sessionStart.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        sessionStart.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        Long start = sessionStart.remove(playerId);
        if (start != null) {
            long sessionTime = System.currentTimeMillis() - start;
            totalPlaytime.merge(playerId, sessionTime, Long::sum);
            saveData();
        }
    }

    /**
     * 累計勤務時間を取得（ミリ秒）
     */
    public long getTotalPlaytime(UUID playerId) {
        long total = totalPlaytime.getOrDefault(playerId, 0L);
        
        // 現在オンラインなら現在のセッションも加算
        Long start = sessionStart.get(playerId);
        if (start != null) {
            total += System.currentTimeMillis() - start;
        }
        
        return total;
    }

    /**
     * フォーマットされた勤務時間を取得
     */
    public String getFormattedPlaytime(UUID playerId) {
        return formatTime(getTotalPlaytime(playerId));
    }

    /**
     * 今日の勤務時間を取得（現在セッションのみ）
     */
    public long getTodayPlaytime(UUID playerId) {
        Long start = sessionStart.get(playerId);
        if (start != null) {
            return System.currentTimeMillis() - start;
        }
        return 0;
    }

    /**
     * 今日の勤務時間（フォーマット済み）
     */
    public String getFormattedTodayPlaytime(UUID playerId) {
        return formatTime(getTodayPlaytime(playerId));
    }

    /**
     * 勤務時間ランキング取得
     */
    public List<Map.Entry<UUID, Long>> getTopPlaytime(int limit) {
        Map<UUID, Long> current = new HashMap<>();
        
        // オフラインプレイヤーの累計
        for (Map.Entry<UUID, Long> entry : totalPlaytime.entrySet()) {
            current.put(entry.getKey(), getTotalPlaytime(entry.getKey()));
        }
        
        // オンラインプレイヤーの累計（まだtotalPlaytimeにない場合）
        for (UUID uuid : sessionStart.keySet()) {
            current.putIfAbsent(uuid, getTotalPlaytime(uuid));
        }
        
        List<Map.Entry<UUID, Long>> sorted = new ArrayList<>(current.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        
        return sorted.subList(0, Math.min(limit, sorted.size()));
    }

    /**
     * 時間をフォーマット
     */
    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return String.format("%d日 %d時間 %d分", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%d時間 %d分", hours, minutes % 60);
        } else {
            return String.format("%d分", minutes);
        }
    }

    /**
     * シャットダウン時に全員のセッションを保存
     */
    public void saveAll() {
        for (Map.Entry<UUID, Long> entry : sessionStart.entrySet()) {
            long sessionTime = System.currentTimeMillis() - entry.getValue();
            totalPlaytime.merge(entry.getKey(), sessionTime, Long::sum);
        }
        sessionStart.clear();
        saveDataSync();
    }

    private void saveData() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveDataSync);
    }

    private void saveDataSync() {
        try {
            Map<String, Long> data = new HashMap<>();
            for (Map.Entry<UUID, Long> entry : totalPlaytime.entrySet()) {
                data.put(entry.getKey().toString(), entry.getValue());
            }
            
            try (Writer writer = new FileWriter(dataFile)) {
                gson.toJson(data, writer);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("勤務時間データ保存失敗: " + e.getMessage());
        }
    }

    private void loadData() {
        if (!dataFile.exists()) {
            return;
        }
        
        try (Reader reader = new FileReader(dataFile)) {
            java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<Map<String, Long>>(){}.getType();
            Map<String, Long> data = gson.fromJson(reader, type);
            if (data != null) {
                for (Map.Entry<String, Long> entry : data.entrySet()) {
                    try {
                        UUID uuid = UUID.fromString(entry.getKey());
                        totalPlaytime.put(uuid, entry.getValue());
                    } catch (IllegalArgumentException ignored) {}
                }
            }
            plugin.getLogger().info("勤務時間データ読み込み完了");
        } catch (IOException e) {
            plugin.getLogger().warning("勤務時間データ読み込み失敗: " + e.getMessage());
        }
    }
}
