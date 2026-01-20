package com.irondiscipline.command;

import com.irondiscipline.IronDiscipline;
import com.irondiscipline.manager.WarningManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * /warn, /warnings, /clearwarnings コマンド
 */
public class WarnCommand implements CommandExecutor, TabCompleter {

    private final IronDiscipline plugin;

    public WarnCommand(IronDiscipline plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName().toLowerCase();

        switch (cmdName) {
            case "warn" -> handleWarn(sender, args);
            case "warnings" -> handleWarnings(sender, args);
            case "clearwarnings" -> handleClearWarnings(sender, args);
            case "unwarn" -> handleUnwarn(sender, args);
        }

        return true;
    }

    private void handleWarn(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c使用法: /warn <プレイヤー> <理由>");
            return;
        }

        OfflinePlayer target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            // オフライン検索
            target = Bukkit.getOfflinePlayer(args[0]);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                sender.sendMessage("§cプレイヤーが見つかりません (未参加の可能性)");
                return;
            }
        }

        // 権限チェック（憲兵 or 少尉以上）
        if (!canWarn(sender, target)) {
            return;
        }

        // 理由を結合
        StringBuilder reason = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1)
                reason.append(" ");
            reason.append(args[i]);
        }

        String reasonStr = reason.toString();
        OfflinePlayer finalTarget = target;

        // 警告追加 (非同期)
        plugin.getWarningManager().addWarning(
                target.getUniqueId(),
                target.getName(),
                reasonStr,
                sender instanceof Player ? ((Player) sender).getUniqueId() : null
        ).thenAccept(count -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                // 通知
                sender.sendMessage("§a" + finalTarget.getName() + " に警告を与えた（" + count + "回目）");

                if (finalTarget.isOnline() && finalTarget.getPlayer() != null) {
                    finalTarget.getPlayer().sendMessage("§c§l【警告】§r§c " + reasonStr + " §7(警告" + count + "回目)");
                }

                // 自動処分の通知
                if (count >= 5) {
                    sender.sendMessage("§c" + finalTarget.getName() + " は警告5回でキックされた");
                } else if (count >= 3) {
                    sender.sendMessage("§e" + finalTarget.getName() + " は警告3回で隔離された");
                }
            });
        });
    }

    private void handleWarnings(CommandSender sender, String[] args) {
        OfflinePlayer target;
        if (args.length > 0) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                target = Bukkit.getOfflinePlayer(args[0]);
            }
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                sender.sendMessage("§cプレイヤーが見つかりません");
                return;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage("§cプレイヤーを指定してください");
            return;
        }

        OfflinePlayer finalTarget = target;
        plugin.getWarningManager().getWarnings(target.getUniqueId()).thenAccept(warnings -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (warnings.isEmpty()) {
                    sender.sendMessage("§a" + finalTarget.getName() + " には警告がありません");
                    return;
                }

                sender.sendMessage("§6=== " + finalTarget.getName() + " の警告履歴 (" + warnings.size() + "件) ===");
                int i = 1;
                for (WarningManager.Warning w : warnings) {
                    sender.sendMessage("§7" + i + ". §f" + w.reason + " §8(" + w.getFormattedDate() + ")");
                    i++;
                }
            });
        });
    }

    private void handleClearWarnings(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§c使用法: /clearwarnings <プレイヤー>");
            return;
        }

        OfflinePlayer target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            target = Bukkit.getOfflinePlayer(args[0]);
        }

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage("§cプレイヤーが見つかりません");
            return;
        }

        OfflinePlayer finalTarget = target;
        plugin.getWarningManager().clearWarnings(target.getUniqueId()).thenRun(() -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                sender.sendMessage("§a" + finalTarget.getName() + " の警告をすべて削除した");
            });
        });
    }

    private void handleUnwarn(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§c使用法: /unwarn <プレイヤー>");
            return;
        }

        OfflinePlayer target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            target = Bukkit.getOfflinePlayer(args[0]);
        }

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage("§cプレイヤーが見つかりません");
            return;
        }

        OfflinePlayer finalTarget = target;
        plugin.getWarningManager().removeLastWarning(target.getUniqueId()).thenAccept(success -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    sender.sendMessage("§a" + finalTarget.getName() + " の最新の警告を削除した");
                } else {
                    sender.sendMessage("§c" + finalTarget.getName() + " には警告がありません");
                }
            });
        });
    }

    private boolean canWarn(CommandSender sender, OfflinePlayer target) {
        if (!(sender instanceof Player executor)) {
            return true; // コンソール
        }

        // 憲兵チェック
        if (plugin.getDivisionManager().isMP(executor)) {
            // 憲兵は士官未満のみ
            if (target instanceof Player) {
                if (plugin.getRankManager().getRank((Player) target)
                        .getWeight() >= com.irondiscipline.model.Rank.LIEUTENANT
                                .getWeight()) {
                    sender.sendMessage("§c憲兵は士官を警告できません");
                    return false;
                }
            }
            return true;
        }

        // 通常の階級チェック (オンライン時のみ厳密チェック、オフラインは一旦許可)
        if (target instanceof Player) {
            return plugin.getRankUtil().checkAll(sender, (Player) target);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(p.getName());
                }
            }
        }

        return completions;
    }
}
