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
import java.util.Map;
import java.util.UUID;

/**
 * /playtime コマンド
 * 勤務時間の表示
 */
public class PlaytimeCommand implements CommandExecutor, TabCompleter {

    private final IronDiscipline plugin;

    public PlaytimeCommand(IronDiscipline plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("top")) {
            showTopPlaytime(sender);
            return true;
        }
        
        Player target;
        if (args.length > 0) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cプレイヤーが見つかりません");
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage("§cプレイヤーを指定してください");
            return true;
        }
        
        String totalTime = plugin.getPlaytimeManager().getFormattedPlaytime(target.getUniqueId());
        String todayTime = plugin.getPlaytimeManager().getFormattedTodayPlaytime(target.getUniqueId());
        
        sender.sendMessage("§6=== " + target.getName() + " の勤務時間 ===");
        sender.sendMessage("§7累計: §f" + totalTime);
        sender.sendMessage("§7本日: §f" + todayTime);
        
        return true;
    }

    private void showTopPlaytime(CommandSender sender) {
        List<Map.Entry<UUID, Long>> top = plugin.getPlaytimeManager().getTopPlaytime(10);
        
        sender.sendMessage("§6=== 勤務時間ランキング ===");
        
        int rank = 1;
        for (Map.Entry<UUID, Long> entry : top) {
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null) name = entry.getKey().toString().substring(0, 8);
            
            String time = plugin.getPlaytimeManager().getFormattedPlaytime(entry.getKey());
            
            String prefix = switch (rank) {
                case 1 -> "§6§l1位";
                case 2 -> "§f§l2位";
                case 3 -> "§c§l3位";
                default -> "§7" + rank + "位";
            };
            
            sender.sendMessage(prefix + " §f" + name + " §7- §f" + time);
            rank++;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("top");
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(p.getName());
                }
            }
        }
        
        return completions;
    }
}
