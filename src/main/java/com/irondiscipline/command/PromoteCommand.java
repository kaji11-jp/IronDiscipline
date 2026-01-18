package com.irondiscipline.command;

import com.irondiscipline.IronDiscipline;
import com.irondiscipline.model.Rank;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * /promote コマンド
 * プレイヤーを昇進させる
 */
public class PromoteCommand implements CommandExecutor, TabCompleter {

    private final IronDiscipline plugin;

    public PromoteCommand(IronDiscipline plugin) {
        this.plugin = plugin;
        plugin.getCommand("promote").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("iron.rank.promote")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no_permission"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§c使用法: /promote <プレイヤー名>");
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player_not_found",
                "%player%", targetName));
            return true;
        }

        Rank currentRank = plugin.getRankManager().getRank(target);
        if (currentRank.getNextRank() == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("rank_already_max"));
            return true;
        }

        plugin.getRankManager().promote(target).thenAccept(newRank -> {
            if (newRank != null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(plugin.getConfigManager().getMessage("rank_promoted",
                        "%player%", target.getName(),
                        "%rank%", newRank.getDisplay()));
                });
            }
        });

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
        }
        return completions;
    }
}
