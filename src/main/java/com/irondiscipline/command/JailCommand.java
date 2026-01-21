package com.irondiscipline.command;

import com.irondiscipline.IronDiscipline;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * /jail コマンド
 * プレイヤーを隔離する
 */
public class JailCommand implements CommandExecutor, TabCompleter {

    private final IronDiscipline plugin;

    public JailCommand(IronDiscipline plugin) {
        this.plugin = plugin;
        plugin.getCommand("jail").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("iron.jail.use")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no_permission"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§c使用法: /jail <プレイヤー名> [理由]");
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);

        // 隔離場所チェック
        if (plugin.getConfigManager().getJailLocation() == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("jail_not_set"));
            return true;
        }

        // 理由（オプション）
        String reason = "理由なし";
        if (args.length >= 2) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                if (i > 1)
                    sb.append(" ");
                sb.append(args[i]);
            }
            reason = sb.toString();
        }

        // 隔離実行
        Player jailer = (sender instanceof Player) ? (Player) sender : null;
        boolean success;

        if (target != null) {
            success = plugin.getJailManager().jail(target, jailer, reason);
        } else {
            // オフラインプレイヤー
            org.bukkit.OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
            if (!offlineTarget.hasPlayedBefore() && !offlineTarget.isOnline()) {
                sender.sendMessage(plugin.getConfigManager().getMessage("player_not_found",
                        "%player%", targetName));
                return true;
            }
            success = plugin.getJailManager().jailOffline(offlineTarget.getUniqueId(), offlineTarget.getName(),
                    jailer != null ? jailer.getUniqueId() : null, reason);
        }

        if (success) {
            sender.sendMessage(plugin.getConfigManager().getMessage("jail_sent",
                    "%player%", target != null ? target.getName() : targetName,
                    "%reason%", reason));
        } else {
            sender.sendMessage("§c隔離に失敗した。既に隔離中か、設定エラーの可能性ある。");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(prefix)) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 2) {
            completions.add("規律違反");
            completions.add("命令無視");
            completions.add("不正行為");
        }
        return completions;
    }
}
