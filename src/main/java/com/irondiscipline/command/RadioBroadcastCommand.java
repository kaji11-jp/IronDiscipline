package com.irondiscipline.command;

import com.irondiscipline.IronDiscipline;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /radiobroadcast (rb) コマンド
 * 無線でメッセージを送信
 */
public class RadioBroadcastCommand implements CommandExecutor {

    private final IronDiscipline plugin;

    public RadioBroadcastCommand(IronDiscipline plugin) {
        this.plugin = plugin;
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
            player.sendMessage("§c使用法: /rb <メッセージ>");
            return true;
        }

        // メッセージを結合
        String message = String.join(" ", args);
        
        // 無線で送信
        plugin.getRadioManager().broadcast(player, message);
        
        return true;
    }
}
