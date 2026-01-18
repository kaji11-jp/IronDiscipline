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
 * /grant コマンド
 * プレイヤーに一時的な発言権を付与
 */
public class GrantCommand implements CommandExecutor, TabCompleter {

    private final IronDiscipline plugin;

    public GrantCommand(IronDiscipline plugin) {
        this.plugin = plugin;
        plugin.getCommand("grant").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("iron.pts.grant")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no_permission"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§c使用法: /grant <プレイヤー名> [秒数]");
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player_not_found",
                "%player%", targetName));
            return true;
        }

        // 秒数（省略時はデフォルト値）
        int seconds = plugin.getConfigManager().getDefaultGrantDuration();
        if (args.length >= 2) {
            try {
                seconds = Integer.parseInt(args[1]);
                if (seconds <= 0) {
                    sender.sendMessage("§c秒数は正の整数で指定せよ。");
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage("§c無効な秒数: " + args[1]);
                return true;
            }
        }

        // 発言許可を付与
        plugin.getPTSManager().grantPermission(target, seconds);
        
        sender.sendMessage(plugin.getConfigManager().getMessage("pts_granted",
            "%player%", target.getName(),
            "%seconds%", String.valueOf(seconds)));

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
            // 秒数のサジェスト
            completions.add("30");
            completions.add("60");
            completions.add("120");
            completions.add("300");
        }
        return completions;
    }
}
