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
 * /unjail コマンド
 * プレイヤーを釈放する
 */
public class UnjailCommand implements CommandExecutor, TabCompleter {

    private final IronDiscipline plugin;

    public UnjailCommand(IronDiscipline plugin) {
        this.plugin = plugin;
        plugin.getCommand("unjail").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("iron.jail.use")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no_permission"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§c使用法: /unjail <プレイヤー名>");
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player_not_found",
                "%player%", targetName));
            return true;
        }

        // 釈放実行
        boolean success = plugin.getJailManager().unjail(target);

        if (success) {
            sender.sendMessage(plugin.getConfigManager().getMessage("jail_released",
                "%player%", target.getName()));
        } else {
            sender.sendMessage("§cこのプレイヤーは拘留されていない。");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            // 隔離中のプレイヤーのみを表示
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (plugin.getJailManager().isJailed(player) && 
                    player.getName().toLowerCase().startsWith(prefix)) {
                    completions.add(player.getName());
                }
            }
        }
        return completions;
    }
}
