package com.irondiscipline.command;

import com.irondiscipline.IronDiscipline;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * /radio コマンド
 * 無線周波数に参加/離脱
 */
public class RadioCommand implements CommandExecutor, TabCompleter {

    private final IronDiscipline plugin;

    public RadioCommand(IronDiscipline plugin) {
        this.plugin = plugin;
        plugin.getCommand("radio").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ使用可能。");
            return true;
        }

        if (!player.hasPermission("iron.radio.use")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no_permission"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(plugin.getConfigManager().getMessage("radio_usage"));
            return true;
        }

        String frequency = args[0];

        // "off" で離脱
        if (frequency.equalsIgnoreCase("off") || frequency.equalsIgnoreCase("leave")) {
            plugin.getRadioManager().leaveFrequency(player);
            return true;
        }

        // 周波数に参加
        plugin.getRadioManager().joinFrequency(player, frequency);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("off");
            completions.add(plugin.getConfigManager().getDefaultFrequency());
            completions.add("118.0");
            completions.add("121.5");
            completions.add("123.0");
        }
        return completions;
    }
}
