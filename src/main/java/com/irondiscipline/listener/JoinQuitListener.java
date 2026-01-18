package com.irondiscipline.listener;

import com.irondiscipline.IronDiscipline;
import com.irondiscipline.util.TabNametagUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 参加/退出リスナー
 * キャッシュとTab/ネームタグの管理
 */
public class JoinQuitListener implements Listener {

    private final IronDiscipline plugin;

    public JoinQuitListener(IronDiscipline plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        
        // 階級キャッシュロード & Tab/ネームタグ更新
        plugin.getRankManager().loadPlayerCache(player);
        
        // 隔離状態の復元
        plugin.getJailManager().onPlayerJoin(player);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onQuit(PlayerQuitEvent event) {
        var player = event.getPlayer();
        var playerId = player.getUniqueId();
        
        // キャッシュクリア
        plugin.getRankManager().unloadPlayerCache(playerId);
        plugin.getPTSManager().cleanup(playerId);
        plugin.getRadioManager().cleanup(playerId);
        
        // Tab/ネームタグクリア
        TabNametagUtil.cleanup(player);
    }
}
