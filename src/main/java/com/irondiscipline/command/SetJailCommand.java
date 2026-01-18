package com.irondiscipline.command;

import com.irondiscipline.IronDiscipline;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /setjail コマンド
 * 隔離場所を設定する
 */
public class SetJailCommand implements CommandExecutor {

    private final IronDiscipline plugin;

    public SetJailCommand(IronDiscipline plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ使用可能。");
            return true;
        }

        if (!player.hasPermission("iron.jail.admin")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no_permission"));
            return true;
        }

        // 現在位置を隔離場所として設定
        plugin.getConfigManager().setJailLocation(player.getLocation());
        
        player.sendMessage(plugin.getConfigManager().getMessage("jail_location_set"));
        
        return true;
    }
}
