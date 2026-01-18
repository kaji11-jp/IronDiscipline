package com.irondiscipline.manager;

import com.irondiscipline.IronDiscipline;
import com.irondiscipline.model.Rank;
import com.irondiscipline.util.TabNametagUtil;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.MetaNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 階級マネージャー
 * LuckPerms APIとの完全連携による階級管理
 */
public class RankManager {

    private final IronDiscipline plugin;
    private final LuckPerms luckPerms;
    private final String metaKey;
    
    // キャッシュ - 毎チャットでのAPI呼び出しを避ける
    private final Map<UUID, Rank> rankCache = new ConcurrentHashMap<>();

    public RankManager(IronDiscipline plugin, LuckPerms luckPerms) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
        this.metaKey = plugin.getConfigManager().getRankMetaKey();
        
        // LuckPermsのイベントリスナーでキャッシュ更新
        luckPerms.getEventBus().subscribe(plugin, 
            net.luckperms.api.event.user.UserDataRecalculateEvent.class,
            event -> invalidateCache(event.getUser().getUniqueId())
        );
    }

    /**
     * プレイヤーの現在階級を取得（キャッシュ優先）
     */
    public Rank getRank(Player player) {
        return rankCache.computeIfAbsent(player.getUniqueId(), uuid -> fetchRankFromLuckPerms(player));
    }

    /**
     * UUIDで階級取得（オフラインプレイヤー対応）
     */
    public CompletableFuture<Rank> getRankAsync(UUID playerId) {
        if (rankCache.containsKey(playerId)) {
            return CompletableFuture.completedFuture(rankCache.get(playerId));
        }
        
        return luckPerms.getUserManager().loadUser(playerId)
            .thenApply(user -> {
                String rankId = user.getCachedData().getMetaData().getMetaValue(metaKey);
                Rank rank = Rank.fromId(rankId);
                rankCache.put(playerId, rank);
                return rank;
            });
    }

    /**
     * LuckPermsから階級を直接取得
     */
    private Rank fetchRankFromLuckPerms(Player player) {
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            return Rank.PRIVATE;
        }
        String rankId = user.getCachedData().getMetaData().getMetaValue(metaKey);
        return Rank.fromId(rankId);
    }

    /**
     * プレイヤーの階級を設定
     */
    public CompletableFuture<Boolean> setRank(Player player, Rank newRank) {
        return setRankByUUID(player.getUniqueId(), newRank).thenApply(success -> {
            if (success) {
                // キャッシュ更新
                rankCache.put(player.getUniqueId(), newRank);
                
                // Tab/ネームタグ即時更新
                Bukkit.getScheduler().runTask(plugin, () -> {
                    TabNametagUtil.updatePlayer(player, newRank);
                    
                    // 本人に通知
                    player.sendMessage(plugin.getConfigManager().getMessage("rank_changed_self",
                        "%rank%", newRank.getDisplay()));
                });
            }
            return success;
        });
    }

    /**
     * UUIDで階級設定
     */
    public CompletableFuture<Boolean> setRankByUUID(UUID playerId, Rank newRank) {
        return luckPerms.getUserManager().loadUser(playerId).thenApply(user -> {
            try {
                // 既存のメタノードを削除
                user.data().clear(node -> 
                    node instanceof MetaNode && ((MetaNode) node).getMetaKey().equals(metaKey)
                );
                
                // 新しいメタノードを追加
                MetaNode node = MetaNode.builder(metaKey, newRank.getId()).build();
                user.data().add(node);
                
                // 保存
                luckPerms.getUserManager().saveUser(user);
                
                // キャッシュ更新
                rankCache.put(playerId, newRank);
                
                return true;
            } catch (Exception e) {
                plugin.getLogger().warning("階級設定失敗: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * 昇進
     */
    public CompletableFuture<Rank> promote(Player player) {
        Rank current = getRank(player);
        Rank next = current.getNextRank();
        
        if (next == null) {
            return CompletableFuture.completedFuture(null); // 最高階級
        }
        
        return setRank(player, next).thenApply(success -> success ? next : null);
    }

    /**
     * 降格
     */
    public CompletableFuture<Rank> demote(Player player) {
        Rank current = getRank(player);
        Rank prev = current.getPreviousRank();
        
        if (prev == null) {
            return CompletableFuture.completedFuture(null); // 最低階級
        }
        
        return setRank(player, prev).thenApply(success -> success ? prev : null);
    }

    /**
     * PTSが必要かどうか
     */
    public boolean requiresPTS(Player player) {
        int threshold = plugin.getConfigManager().getPTSRequireBelowWeight();
        return getRank(player).getWeight() <= threshold;
    }

    /**
     * 対象より上位階級かどうか
     */
    public boolean isHigherRank(Player officer, Player target) {
        return getRank(officer).isHigherThan(getRank(target));
    }

    /**
     * キャッシュを無効化
     */
    public void invalidateCache(UUID playerId) {
        rankCache.remove(playerId);
        
        // オンラインならTab更新
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Rank rank = fetchRankFromLuckPerms(player);
                rankCache.put(playerId, rank);
                TabNametagUtil.updatePlayer(player, rank);
            });
        }
    }

    /**
     * プレイヤー参加時のキャッシュ読み込み
     */
    public void loadPlayerCache(Player player) {
        Rank rank = fetchRankFromLuckPerms(player);
        rankCache.put(player.getUniqueId(), rank);
        TabNametagUtil.updatePlayer(player, rank);
    }

    /**
     * プレイヤー退出時のキャッシュクリア
     */
    public void unloadPlayerCache(UUID playerId) {
        rankCache.remove(playerId);
    }
}
