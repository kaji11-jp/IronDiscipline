package com.irondiscipline.listener;

import com.irondiscipline.IronDiscipline;
import com.irondiscipline.model.Rank;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * チャットリスナー
 * PTSロジックの心臓部 - 非同期処理でラグを生まない
 */
public class ChatListener implements Listener {

    private final IronDiscipline plugin;

    public ChatListener(IronDiscipline plugin) {
        this.plugin = plugin;
    }

    /**
     * 非同期チャットイベント処理
     * EventPriority.HIGHEST で他プラグインの後に処理
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        
        // ===== 1. 隔離チェック（完全ブロック）=====
        if (plugin.getJailManager().isJailed(player)) {
            event.setCancelled(true);
            player.sendMessage(plugin.getConfigManager().getJailBlockedMessage());
            return;
        }
        
        // ===== 2. 試験中チェック（完全ブロック）=====
        if (plugin.getExamManager() != null && plugin.getExamManager().isInExam(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage("§c§l【試験中】§r§7 試験中はチャットが禁止されています");
            return;
        }
        
        // ===== 2. 階級取得（キャッシュから高速取得）=====
        Rank rank = plugin.getRankManager().getRank(player);
        
        // ===== 3. PTS（発言許可）チェック =====
        if (!plugin.getPTSManager().hasPermissionToSpeak(player)) {
            // 発言許可なし → PTSリクエストモード
            
            // メッセージにPTSリクエストプレフィックスを付与
            String ptsPrefix = plugin.getConfigManager().getPTSRequestPrefix();
            
            // 上官にのみ見えるようにフィルタ（一般プレイヤーには見せない）
            event.setCancelled(true);
            
            // 上官に通知
            plugin.getPTSManager().notifyOfficersWithMessage(player, event.getMessage());
            
            // 本人にもPTS必要のメッセージ
            player.sendMessage(plugin.getConfigManager().getMessage("pts_required"));
            
            return;
        }
        
        // ===== 4. チャットフォーマットに階級プレフィックスを適用 =====
        String format = rank.getDisplay() + " " + ChatColor.WHITE + "%s" + 
                       ChatColor.GRAY + ": " + ChatColor.WHITE + "%s";
        event.setFormat(format);
    }
}
