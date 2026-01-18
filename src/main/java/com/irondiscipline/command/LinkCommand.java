package com.irondiscipline.command;

import com.irondiscipline.IronDiscipline;
import com.irondiscipline.manager.LinkManager;
import com.irondiscipline.model.Rank;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /link コマンド
 * Discordアカウントとの連携
 */
public class LinkCommand implements CommandExecutor {

    private final IronDiscipline plugin;

    public LinkCommand(IronDiscipline plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ実行可能");
            return true;
        }

        if (args.length < 1) {
            // 連携状況確認
            if (plugin.getLinkManager().isLinked(player.getUniqueId())) {
                sender.sendMessage("§a✅ Discordと連携済みです");
                sender.sendMessage("§7連携を解除するには: §f/unlink");
            } else {
                sender.sendMessage("§e❌ Discordと未連携です");
                sender.sendMessage("§7連携するには:");
                sender.sendMessage("§f1. Discordで §e/link §fを実行");
                sender.sendMessage("§f2. 表示されたコードを §e/link <コード> §fで入力");
            }
            return true;
        }

        String code = args[0].toUpperCase();
        LinkManager.LinkResult result = plugin.getLinkManager().attemptLink(player.getUniqueId(), code);

        switch (result) {
            case SUCCESS -> {
                player.sendMessage("§a§l✅ 連携成功！");
                player.sendMessage("§aDiscordアカウントと連携しました。");
                
                // Discord側ロール・ニックネーム変更
                Long discordId = plugin.getLinkManager().getDiscordId(player.getUniqueId());
                if (discordId != null && plugin.getDiscordManager().isEnabled()) {
                    Rank rank = plugin.getRankManager().getRank(player);
                    plugin.getDiscordManager().onLinkComplete(discordId, player.getName(), rank);
                    
                    plugin.getDiscordManager().sendNotification(
                        "🔗 連携完了", 
                        "**" + player.getName() + "** がMinecraftアカウントと連携しました",
                        java.awt.Color.GREEN
                    );
                }
            }
            case INVALID_CODE -> {
                player.sendMessage("§c無効な認証コードです。");
                player.sendMessage("§7Discordで §f/link §7を実行して新しいコードを取得してください。");
            }
            case EXPIRED -> {
                player.sendMessage("§c認証コードの有効期限が切れています。");
                player.sendMessage("§7Discordで §f/link §7を再実行してください。");
            }
        }

        return true;
    }
}
