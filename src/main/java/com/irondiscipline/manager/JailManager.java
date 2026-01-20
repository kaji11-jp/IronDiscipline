package com.irondiscipline.manager;

import com.irondiscipline.IronDiscipline;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.irondiscipline.util.InventoryUtil;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 隔離マネージャー
 * プレイヤーの拘留と釈放を管理
 */
public class JailManager {

    private final IronDiscipline plugin;

    // 隔離中プレイヤー (キャッシュ)
    private final Map<UUID, JailData> jailedPlayers = new ConcurrentHashMap<>();

    public JailManager(IronDiscipline plugin) {
        this.plugin = plugin;
        loadJailedPlayers();
    }

    /**
     * プレイヤーを隔離
     */
    public boolean jail(Player target, Player jailer, String reason) {
        Location jailLocation = plugin.getConfigManager().getJailLocation();
        if (jailLocation == null) {
            return false;
        }

        UUID targetId = target.getUniqueId();

        // 既に隔離中なら何もしない
        if (isJailed(target)) {
            return false;
        }

        // 現在位置を保存
        Location originalLocation = target.getLocation();
        String locString = serializeLocation(originalLocation);

        // インベントリバックアップ (Base64)
        String invBackup = InventoryUtil.toBase64(target.getInventory().getContents());
        String armorBackup = InventoryUtil.toBase64(target.getInventory().getArmorContents());

        // DB保存を先に実行 (トランザクション的安全性)
        // 戻り値はvoidではないので、呼び出し元での同期的な戻り値はfalseにし、完了後に処理する形が理想だが
        // 現状のAPI仕様上、同期的にtrue/falseを返す必要があるため、処理の開始を返す形にする。

        plugin.getStorageManager().saveJailedPlayerAsync(targetId, target.getName(), reason,
                jailer != null ? jailer.getUniqueId() : null, locString, invBackup, armorBackup)
            .thenAccept(success -> {
                if (success) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        // DB保存成功後にインベントリ操作とテレポート
                        if (!target.isOnline()) return;

                        // インベントリクリア
                        target.getInventory().clear();
                        target.getInventory().setArmorContents(new ItemStack[4]);

                        // ゲームモードをアドベンチャーに
                        target.setGameMode(GameMode.ADVENTURE);

                        // 隔離場所へテレポート
                        target.teleport(jailLocation);

                        // データ保存 (キャッシュ)
                        JailData data = new JailData(targetId, target.getName(), reason,
                                System.currentTimeMillis(), jailer != null ? jailer.getUniqueId() : null, locString);
                        jailedPlayers.put(targetId, data);

                        // 通知
                        target.sendMessage(plugin.getConfigManager().getMessage("jail_you_jailed",
                                "%reason%", reason != null ? reason : "理由なし"));
                    });
                } else {
                    plugin.getLogger().warning("隔離処理中断: DB保存に失敗しました - " + target.getName());
                }
            });

        return true; // 処理を開始したことを返す
    }

    /**
     * オフラインプレイヤーを隔離 (DBのみ更新)
     */
    public boolean jailOffline(UUID targetId, String targetName, UUID jailerId, String reason) {
        if (isJailed(targetId)) {
            return false;
        }

        // DB保存 (インベントリバックアップはnull = ログイン時にバックアップ)
        plugin.getStorageManager().saveJailedPlayer(targetId, targetName, reason,
                jailerId, null, null, null);

        // キャッシュ更新 (一応)
        JailData data = new JailData(targetId, targetName, reason,
                System.currentTimeMillis(), jailerId, null);
        jailedPlayers.put(targetId, data);

        return true;
    }

    /**
     * プレイヤーを釈放
     */
    public boolean unjail(Player target) {
        UUID targetId = target.getUniqueId();

        if (!isJailed(target)) {
            return false;
        }

        JailData data = jailedPlayers.remove(targetId);

        // 元の場所へテレポート
        if (data != null && data.originalLocation != null) {
            Location original = deserializeLocation(data.originalLocation);
            if (original != null) {
                target.teleport(original);
            }
        }

        // ゲームモード復元
        target.setGameMode(GameMode.SURVIVAL);

        // インベントリ復元 (DBから非同期取得)
        plugin.getStorageManager().getInventoryBackupAsync(targetId).thenAccept(invData -> {
            plugin.getStorageManager().getArmorBackupAsync(targetId).thenAccept(armorData -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (invData != null) {
                        ItemStack[] items = InventoryUtil.fromBase64(invData);
                        if (items != null) {
                            target.getInventory().setContents(items);
                        }
                    }

                    if (armorData != null) {
                        ItemStack[] armor = InventoryUtil.fromBase64(armorData);
                        if (armor != null) {
                            target.getInventory().setArmorContents(armor);
                        }
                    }

                    // DB削除 (非同期)
                    plugin.getStorageManager().removeJailedPlayerAsync(targetId).thenRun(() -> {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            // 通知 (DB削除完了後)
                            target.sendMessage(plugin.getConfigManager().getMessage("jail_you_released"));
                        });
                    });
                });
            });
        });

        return true;
    }

    /**
     * 隔離中かどうかチェック
     */
    public boolean isJailed(Player player) {
        return jailedPlayers.containsKey(player.getUniqueId());
    }

    /**
     * UUIDで隔離中かチェック
     */
    public boolean isJailed(UUID playerId) {
        return jailedPlayers.containsKey(playerId);
    }

    /**
     * ログイン時の隔離チェックと復元
     */
    public void onPlayerJoin(Player player) {
        UUID playerId = player.getUniqueId();

        // 元のゲームモードを保存 (Race Condition対策で一時的にスペクテイターにするため)
        GameMode originalMode = player.getGameMode();

        // 即座に行動制限
        player.setGameMode(GameMode.SPECTATOR);

        // 非同期チェック
        plugin.getStorageManager().isJailedAsync(playerId).thenAccept(isJailed -> {
            if (!isJailed) {
                // 隔離中でなければ元のモードに復元
                Bukkit.getScheduler().runTask(plugin, () -> {
                   if (player.isOnline()) {
                       player.setGameMode(originalMode);
                   }
                });
                return;
            }

            // DBからバックアップ状況を確認
            plugin.getStorageManager().getInventoryBackupAsync(playerId).thenAccept(invBackup -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!player.isOnline()) return;

                    // バックアップがない場合（オフライン処罰時）は今すぐバックアップ
                    if (invBackup == null) {
                        // インベントリバックアップ
                        String newInvBackup = InventoryUtil.toBase64(player.getInventory().getContents());
                        String newArmorBackup = InventoryUtil.toBase64(player.getInventory().getArmorContents());

                        // 元の場所保存
                        String locString = serializeLocation(player.getLocation());

                        // DB更新
                        plugin.getStorageManager().saveJailedPlayer(playerId, player.getName(), "Offline Jail",
                                null, locString, newInvBackup, newArmorBackup);

                        // インベントリクリア
                        player.getInventory().clear();
                        player.getInventory().setArmorContents(new ItemStack[4]);
                    }

                    // DBに隔離記録がある場合
                    // キャッシュ復元
                    if (!jailedPlayers.containsKey(playerId)) {
                        jailedPlayers.put(playerId,
                                new JailData(playerId, player.getName(), "再接続", System.currentTimeMillis(), null, null));
                    }

                    // 隔離場所にテレポート
                    Location jailLocation = plugin.getConfigManager().getJailLocation();
                    if (jailLocation != null) {
                        player.teleport(jailLocation);
                        player.setGameMode(GameMode.ADVENTURE);
                        player.sendMessage(plugin.getConfigManager().getMessage("jail_you_jailed",
                                "%reason%", "拘留中のため再配置"));
                    }
                });
            });
        });
    }

    /**
     * プレイヤーが隔離場所から逃げようとした時の処理
     */
    public void preventEscape(Player player) {
        if (!isJailed(player))
            return;

        Location jailLocation = plugin.getConfigManager().getJailLocation();
        if (jailLocation != null) {
            // 隔離場所から離れすぎていたら戻す
            if (player.getLocation().distance(jailLocation) > 10) {
                player.teleport(jailLocation);
            }
        }
    }

    /**
     * 保存済み隔離プレイヤーをロード
     */
    private void loadJailedPlayers() {
        // 起動時にDBから読み込み
        // オンラインプレイヤーがいれば状態を復元
        for (Player player : Bukkit.getOnlinePlayers()) {
            onPlayerJoin(player);
        }
    }

    /**
     * 全データ保存
     */
    public void saveAll() {
        // シャットダウン時に呼ばれる
        // インベントリバックアップは消えるがDBに隔離状態は残る
    }

    /**
     * 位置のシリアライズ
     */
    private String serializeLocation(Location loc) {
        return loc.getWorld().getName() + ";" +
                loc.getX() + ";" + loc.getY() + ";" + loc.getZ() + ";" +
                loc.getYaw() + ";" + loc.getPitch();
    }

    /**
     * 位置のデシリアライズ
     */
    private Location deserializeLocation(String str) {
        try {
            String[] parts = str.split(";");
            return new Location(
                    Bukkit.getWorld(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3]),
                    Float.parseFloat(parts[4]),
                    Float.parseFloat(parts[5]));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 隔離データ内部クラス
     */
    private static class JailData {
        final UUID playerId;
        final String playerName;
        final String reason;
        final long jailedAt;
        final UUID jailedBy;
        final String originalLocation;

        JailData(UUID playerId, String playerName, String reason,
                long jailedAt, UUID jailedBy, String originalLocation) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.reason = reason;
            this.jailedAt = jailedAt;
            this.jailedBy = jailedBy;
            this.originalLocation = originalLocation;
        }
    }
}
