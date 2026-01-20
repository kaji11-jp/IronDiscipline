package com.irondiscipline.manager;

import com.irondiscipline.IronDiscipline;
import com.irondiscipline.model.Rank;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 * 自動昇進マネージャー
 * プレイ時間に基づいた自動昇進を管理する
 */
public class AutoPromotionManager {

    private final IronDiscipline plugin;
    private final RankManager rankManager;
    private BukkitTask task;

    public AutoPromotionManager(IronDiscipline plugin, RankManager rankManager) {
        this.plugin = plugin;
        this.rankManager = rankManager;
    }

    /**
     * タスクを開始
     */
    public void startTask() {
        stopTask(); // 既存タスクがあれば停止

        if (!plugin.getConfigManager().isTimeBasedPromotionEnabled()) {
            return;
        }

        int intervalSeconds = plugin.getConfigManager().getTimeBasedPromotionInterval();

        task = Bukkit.getScheduler().runTaskTimer(plugin, this::checkPromotions, intervalSeconds * 20L,
                intervalSeconds * 20L);
    }

    /**
     * タスクを停止
     */
    public void stopTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /**
     * シャットダウン処理
     */
    public void shutdown() {
        stopTask();
    }

    /**
     * 全プレイヤーの昇進判定
     */
    private void checkPromotions() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            checkAndPromote(player);
        }
    }

    /**
     * 個別プレイヤーの昇進判定
     */
    private void checkAndPromote(Player player) {
        Rank currentRank = rankManager.getRank(player);
        Rank nextRank = currentRank.getNextRank();

        if (nextRank == null) {
            return; // 最高ランク
        }

        // 次のランクの必要時間を取得
        int requiredMinutes = plugin.getConfigManager().getServerPlaytimeRequirement(nextRank.getId());

        if (requiredMinutes <= 0) {
            return; // 自動昇進対象外
        }

        // プレイ時間を取得 (TICKS単位なので分に変換)
        int playedTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        int playedMinutes = playedTicks / 1200; // 20 ticks * 60 seconds

        if (playedMinutes >= requiredMinutes) {
            // 昇進実行
            plugin.getLogger().info("自動昇進: " + player.getName() + " -> " + nextRank.getId() + " (" + playedMinutes
                    + "m / " + requiredMinutes + "m)");
            rankManager.promote(player).thenAccept(newRank -> {
                if (newRank != null) {
                    player.sendMessage(plugin.getConfigManager().getMessage("rank_promoted",
                            "%player%", player.getName(),
                            "%rank%", newRank.getDisplay()));
                    // 必要であればDiscord通知など
                }
            });
        }
    }
}
