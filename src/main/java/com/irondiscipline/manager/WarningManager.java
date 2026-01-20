package com.irondiscipline.manager;

import com.irondiscipline.IronDiscipline;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 警告マネージャー
 * 警告の蓄積と自動処分
 */
public class WarningManager {

    private final IronDiscipline plugin;

    // キャッシュ (Lazy Loading)
    private final Map<UUID, List<Warning>> cache = new ConcurrentHashMap<>();

    public WarningManager(IronDiscipline plugin) {
        this.plugin = plugin;
    }

    /**
     * 警告を追加 (非同期)
     */
    public CompletableFuture<Integer> addWarning(UUID playerId, String playerName, String reason, UUID warnedBy) {
        String warnedById = warnedBy != null ? warnedBy.toString() : "CONSOLE";
        long timestamp = System.currentTimeMillis();

        return plugin.getStorageManager().addWarningAsync(playerId, playerName, reason, warnedById, timestamp)
            .thenCompose(v -> getWarnings(playerId))
            .thenApply(list -> {
                // キャッシュ更新 (getWarningsで既に更新されているはずだが念のため)
                int count = list.size();

                // 自動処分チェック (メインスレッドに戻して実行)
                Bukkit.getScheduler().runTask(plugin, () -> checkAutoPunish(playerId, count));

                return count;
            });
    }

    private void checkAutoPunish(UUID playerId, int count) {
        Player target = Bukkit.getPlayer(playerId);
        if (target != null && target.isOnline()) {
            int kickLimit = plugin.getConfigManager().getWarningKickThreshold();
            int jailLimit = plugin.getConfigManager().getWarningJailThreshold();

            if (count >= kickLimit) {
                target.kickPlayer("§c警告が" + kickLimit + "回に達したため、キックされました。");
            } else if (count >= jailLimit) {
                plugin.getJailManager().jail(target, null, "警告" + count + "回による自動隔離");
            }
        }
    }

    /**
     * 警告リストを取得 (非同期)
     */
    public CompletableFuture<List<Warning>> getWarnings(UUID playerId) {
        if (cache.containsKey(playerId)) {
            return CompletableFuture.completedFuture(new ArrayList<>(cache.get(playerId)));
        }

        return plugin.getStorageManager().getWarningsAsync(playerId)
            .thenApply(list -> {
                cache.put(playerId, list);
                return list;
            });
    }

    /**
     * 警告をクリア (非同期)
     */
    public CompletableFuture<Void> clearWarnings(UUID playerId) {
        cache.remove(playerId);
        return plugin.getStorageManager().clearWarningsAsync(playerId);
    }

    /**
     * 最新の警告を削除 (非同期)
     */
    public CompletableFuture<Boolean> removeLastWarning(UUID playerId) {
        return getWarnings(playerId).thenCompose(list -> {
            if (list.isEmpty()) {
                return CompletableFuture.completedFuture(false);
            }

            return plugin.getStorageManager().removeLastWarningAsync(playerId).thenApply(v -> {
                cache.remove(playerId); // キャッシュ無効化して再読み込みを促す
                return true;
            });
        });
    }

    /**
     * キャッシュ無効化
     */
    public void invalidateCache(UUID playerId) {
        cache.remove(playerId);
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
