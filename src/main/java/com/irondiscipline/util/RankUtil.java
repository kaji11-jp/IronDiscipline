package com.irondiscipline.util;

import com.irondiscipline.IronDiscipline;
import com.irondiscipline.model.Rank;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * 階級ベース権限チェックユーティリティ
 */
public class RankUtil {

    private final IronDiscipline plugin;

    public RankUtil(IronDiscipline plugin) {
        this.plugin = plugin;
    }

    /**
     * 実行者が少尉以上かチェック
     * 
     * @return true=操作可能, false=権限不足
     */
    public boolean canExecuteCommand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return true; // コンソールは常に可
        }

        Rank rank = plugin.getRankManager().getRank(player);
        // LIEUTENANT = 少尉 (weight 40)
        if (rank.getWeight() < Rank.LIEUTENANT.getWeight()) {
            sender.sendMessage(plugin.getConfigManager().getMessage("rank_insufficient")
                    .replace("{required}", Rank.LIEUTENANT.getDisplay()));
            return false;
        }
        return true;
    }

    /**
     * 実行者が対象より上位の階級かチェック
     * 
     * @return true=操作可能, false=権限不足
     */
    public boolean canOperateOn(CommandSender sender, Player target) {
        if (!(sender instanceof Player executor)) {
            return true; // コンソールは常に可
        }

        Rank executorRank = plugin.getRankManager().getRank(executor);
        Rank targetRank = plugin.getRankManager().getRank(target);

        if (executorRank.getWeight() <= targetRank.getWeight()) {
            sender.sendMessage(plugin.getConfigManager().getMessage("rank_cannot_operate")
                    .replace("{target}", target.getName())
                    .replace("{target_rank}", targetRank.getDisplay()));
            return false;
        }
        return true;
    }

    /**
     * 実行者が対象より上位の階級かチェック（UUID版）
     */
    public boolean canOperateOn(CommandSender sender, UUID targetId) {
        if (!(sender instanceof Player)) {
            return true;
        }

        // UUIDからオンラインプレイヤーを取得
        Player target = org.bukkit.Bukkit.getPlayer(targetId);
        if (target == null) {
            // オフラインプレイヤーは操作可能（階級確認不可）
            return true;
        }

        return canOperateOn(sender, target);
    }

    /**
     * 両方のチェックを行う
     */
    public boolean checkAll(CommandSender sender, Player target) {
        return canExecuteCommand(sender) && canOperateOn(sender, target);
    }
}
