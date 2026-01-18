package com.irondiscipline.listener;

import com.irondiscipline.IronDiscipline;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ジェスチャーリスナー
 * ダブルスニークでPTSリクエスト（モバイル向けUI）
 */
public class GestureListener implements Listener {

    private final IronDiscipline plugin;
    
    // 最後にスニークした時刻
    private final Map<UUID, Long> lastSneakTimes = new ConcurrentHashMap<>();

    public GestureListener(IronDiscipline plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        // スニーク開始時のみ処理
        if (!event.isSneaking()) {
            return;
        }
        
        // ダブルスニーク機能が無効なら無視
        if (!plugin.getConfigManager().isSneakRequestEnabled()) {
            return;
        }
        
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        
        // 前回スニーク時刻を取得
        Long lastSneak = lastSneakTimes.get(playerId);
        lastSneakTimes.put(playerId, now);
        
        if (lastSneak == null) {
            return;
        }
        
        // ダブルスニーク判定
        int threshold = plugin.getConfigManager().getDoubleSneakThreshold();
        if (now - lastSneak < threshold) {
            // ダブルスニーク検知！
            handleDoubleSneak(player);
            
            // 連続検知を防ぐためリセット
            lastSneakTimes.remove(playerId);
        }
    }

    /**
     * ダブルスニーク時の処理
     */
    private void handleDoubleSneak(Player player) {
        // PTSが必要な階級かチェック
        if (!plugin.getRankManager().requiresPTS(player)) {
            return;
        }
        
        // 既に発言許可があるならスキップ
        if (plugin.getPTSManager().hasPermissionToSpeak(player)) {
            return;
        }
        
        // PTSリクエストを送信
        plugin.getPTSManager().sendRequest(player);
    }

    /**
     * 隔離プレイヤーの移動制限
     */
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        // 隔離中なら位置チェック
        if (plugin.getJailManager().isJailed(player)) {
            plugin.getJailManager().preventEscape(player);
        }
    }
}
