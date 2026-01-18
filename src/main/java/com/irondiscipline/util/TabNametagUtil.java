package com.irondiscipline.util;

import com.irondiscipline.model.Rank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Tab & ネームタグユーティリティ
 * 階級に応じたTabリストとネームタグの即時更新
 */
public class TabNametagUtil {

    private static final String TEAM_PREFIX = "iron_";

    /**
     * プレイヤーのTab/ネームタグを更新
     */
    public static void updatePlayer(Player player, Rank rank) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        
        // チーム名（ソート順のため重みを使用）
        String teamName = TEAM_PREFIX + String.format("%03d", 100 - rank.getWeight());
        
        // 既存チームから削除
        for (Team team : scoreboard.getTeams()) {
            if (team.getName().startsWith(TEAM_PREFIX)) {
                team.removeEntry(player.getName());
            }
        }
        
        // チーム取得または作成
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        
        // プレフィックス設定（16文字制限対応）
        String prefix = rank.getDisplay() + " " + ChatColor.RESET;
        if (prefix.length() > 64) {
            prefix = prefix.substring(0, 64);
        }
        team.setPrefix(prefix);
        
        // チームにプレイヤーを追加
        team.addEntry(player.getName());
        
        // Tabリストのヘッダー表示名も更新
        player.setPlayerListName(rank.getDisplay() + " " + ChatColor.WHITE + player.getName());
    }

    /**
     * 全プレイヤーのTab/ネームタグを再構築
     */
    public static void refreshAll() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        
        // 既存チームをクリア
        for (Team team : scoreboard.getTeams()) {
            if (team.getName().startsWith(TEAM_PREFIX)) {
                team.unregister();
            }
        }
    }

    /**
     * プレイヤー退出時のクリーンアップ
     */
    public static void cleanup(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team team : scoreboard.getTeams()) {
            if (team.getName().startsWith(TEAM_PREFIX)) {
                team.removeEntry(player.getName());
            }
        }
    }
}
