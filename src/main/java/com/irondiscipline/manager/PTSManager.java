package com.irondiscipline.manager;

import com.irondiscipline.IronDiscipline;
import com.irondiscipline.model.Rank;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PTS (Permission To Speak) マネージャー
 * 発言許可システムの管理
 */
public class PTSManager {

    private final IronDiscipline plugin;
    
    // 発言許可状態 (UUID -> 期限タイムスタンプ)
    private final Map<UUID, Long> grantedPlayers = new ConcurrentHashMap<>();
    
    // PTS要請中のプレイヤー
    private final Set<UUID> requestingPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    
    // 期限切れチェック用タスク
    private BukkitTask expirationTask;

    public PTSManager(IronDiscipline plugin) {
        this.plugin = plugin;
        startExpirationChecker();
    }

    /**
     * 発言許可があるかチェック
     */
    public boolean hasPermissionToSpeak(Player player) {
        // PTS免除権限を持っている場合
        if (player.hasPermission("iron.pts.bypass")) {
            return true;
        }
        
        // 階級がPTS必要ラインを超えている場合
        if (!plugin.getRankManager().requiresPTS(player)) {
            return true;
        }
        
        // 一時的な発言許可があるかチェック
        Long expiration = grantedPlayers.get(player.getUniqueId());
        if (expiration != null) {
            if (System.currentTimeMillis() < expiration) {
                return true;
            } else {
                // 期限切れ
                revokeGrant(player);
            }
        }
        
        return false;
    }

    /**
     * 発言許可を付与
     */
    public void grantPermission(Player player, int seconds) {
        long expiration = System.currentTimeMillis() + (seconds * 1000L);
        grantedPlayers.put(player.getUniqueId(), expiration);
        requestingPlayers.remove(player.getUniqueId());
        
        // 本人に通知
        player.sendMessage(plugin.getConfigManager().getMessage("pts_received",
            "%seconds%", String.valueOf(seconds)));
        
        // アクションバーで残り時間表示開始
        startCountdownDisplay(player, seconds);
    }

    /**
     * 発言許可を剥奪
     */
    public void revokeGrant(Player player) {
        if (grantedPlayers.remove(player.getUniqueId()) != null) {
            player.sendMessage(plugin.getConfigManager().getMessage("pts_expired"));
        }
    }

    /**
     * PTS要請を送信
     */
    public void sendRequest(Player player) {
        if (requestingPlayers.contains(player.getUniqueId())) {
            return; // 既にリクエスト中
        }
        
        requestingPlayers.add(player.getUniqueId());
        
        // 本人に確認メッセージ
        player.sendMessage(plugin.getConfigManager().getMessage("pts_request_sent"));
        
        // 上官に通知
        notifyOfficers(player);
        
        // アクションバーで要請中表示
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
            TextComponent.fromLegacyText(ChatColor.YELLOW + "✋ PTS要請中..."));
        
        // 30秒後に自動キャンセル
        final UUID playerId = player.getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (requestingPlayers.remove(playerId)) {
                Player p = Bukkit.getPlayer(playerId);
                if (p != null && p.isOnline()) {
                    p.sendMessage(plugin.getConfigManager().getMessage("pts_request_timeout"));
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        TextComponent.fromLegacyText(ChatColor.RED + "⏱ PTS要請がタイムアウトしました"));
                }
            }
        }, 20L * 30); // 30秒 = 600 tick
    }

    /**
     * 上官全員に通知
     */
    public void notifyOfficers(Player requester) {
        String message = plugin.getConfigManager().getMessage("pts_request_notify",
            "%player%", requester.getName());
        
        int threshold = plugin.getConfigManager().getPTSRequireBelowWeight();
        
        for (Player officer : Bukkit.getOnlinePlayers()) {
            // 自分自身はスキップ
            if (officer.equals(requester)) continue;
            
            // PTS付与権限を持っているか、閾値より上の階級
            if (officer.hasPermission("iron.pts.grant") || 
                plugin.getRankManager().getRank(officer).getWeight() > threshold) {
                officer.sendMessage(message);
                
                // サウンド通知
                officer.playSound(officer.getLocation(), 
                    org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
            }
        }
    }

    /**
     * 特定メッセージで上官に通知
     */
    public void notifyOfficersWithMessage(Player requester, String chatMessage) {
        int threshold = plugin.getConfigManager().getPTSRequireBelowWeight();
        Rank requesterRank = plugin.getRankManager().getRank(requester);
        
        String formattedMessage = ChatColor.GRAY + "[PTS要請] " + 
            requesterRank.getDisplay() + " " + requester.getName() + 
            ChatColor.GRAY + ": " + ChatColor.WHITE + chatMessage;
        
        for (Player officer : Bukkit.getOnlinePlayers()) {
            if (officer.equals(requester)) continue;
            
            if (officer.hasPermission("iron.pts.grant") || 
                plugin.getRankManager().getRank(officer).getWeight() > threshold) {
                officer.sendMessage(formattedMessage);
            }
        }
    }

    /**
     * 要請中かどうかチェック
     */
    public boolean isRequesting(UUID playerId) {
        return requestingPlayers.contains(playerId);
    }

    /**
     * 残り許可時間を取得（秒）
     */
    public int getRemainingSeconds(UUID playerId) {
        Long expiration = grantedPlayers.get(playerId);
        if (expiration == null) return 0;
        long remaining = expiration - System.currentTimeMillis();
        return remaining > 0 ? (int)(remaining / 1000) : 0;
    }

    /**
     * カウントダウン表示
     */
    private void startCountdownDisplay(Player player, int totalSeconds) {
        final UUID playerId = player.getUniqueId();
        
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            Player p = Bukkit.getPlayer(playerId);
            if (p == null || !p.isOnline()) {
                task.cancel();
                return;
            }
            
            int remaining = getRemainingSeconds(playerId);
            if (remaining <= 0) {
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(ChatColor.RED + "⏱ 発言許可失効"));
                task.cancel();
                return;
            }
            
            // 色分け
            ChatColor color;
            if (remaining > 30) {
                color = ChatColor.GREEN;
            } else if (remaining > 10) {
                color = ChatColor.YELLOW;
            } else {
                color = ChatColor.RED;
            }
            
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText(color + "🎤 発言許可: " + remaining + "秒"));
            
        }, 0L, 20L); // 毎秒更新
    }

    /**
     * 期限切れチェッカー開始
     */
    private void startExpirationChecker() {
        expirationTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            grantedPlayers.entrySet().removeIf(entry -> {
                if (now >= entry.getValue()) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player != null && player.isOnline()) {
                        player.sendMessage(plugin.getConfigManager().getMessage("pts_expired"));
                    }
                    return true;
                }
                return false;
            });
        }, 20L, 20L); // 毎秒チェック
    }

    /**
     * プレイヤー退出時のクリーンアップ
     */
    public void cleanup(UUID playerId) {
        grantedPlayers.remove(playerId);
        requestingPlayers.remove(playerId);
    }

    /**
     * タスク停止 (シャットダウン処理)
     */
    public void shutdown() {
        if (expirationTask != null && !expirationTask.isCancelled()) {
            expirationTask.cancel();
        }
    }
}
