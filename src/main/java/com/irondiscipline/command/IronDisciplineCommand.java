package com.irondiscipline.command;

import com.irondiscipline.IronDiscipline;
import org.bukkit.Bukkit;
import org.bukkit.BanList;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * /irondiscipline (iron, id) コマンド
 * プラグイン管理 + Adonis風管理コマンド
 */
public class IronDisciplineCommand implements CommandExecutor, TabCompleter {

    private final IronDiscipline plugin;
    
    // freeze中のプレイヤー
    private final Set<UUID> frozenPlayers = ConcurrentHashMap.newKeySet();

    public IronDisciplineCommand(IronDiscipline plugin) {
        this.plugin = plugin;
        plugin.getCommand("irondiscipline").setTabCompleter(this);
        
        // Freeze中の移動を防ぐ
        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onMove(org.bukkit.event.player.PlayerMoveEvent e) {
                if (frozenPlayers.contains(e.getPlayer().getUniqueId())) {
                    if (e.getFrom().getX() != e.getTo().getX() || 
                        e.getFrom().getZ() != e.getTo().getZ()) {
                        e.setTo(e.getFrom());
                    }
                }
            }
        }, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("iron.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no_permission"));
            return true;
        }

        if (args.length < 1) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload" -> {
                plugin.reload();
                sender.sendMessage(plugin.getConfigManager().getMessage("config_reloaded"));
            }
            case "version", "ver" -> {
                sender.sendMessage("§6鉄の規律 (IronDiscipline) v" + plugin.getDescription().getVersion());
            }
            case "cleanup" -> {
                plugin.getStorageManager().cleanupOldLogs();
                sender.sendMessage("§a古いログの削除を開始した。");
            }
            // Adonis風コマンド
            case "kick" -> handleKick(sender, args);
            case "ban" -> handleBan(sender, args);
            case "tp" -> handleTp(sender, args);
            case "bring" -> handleBring(sender, args);
            case "freeze" -> handleFreeze(sender, args);
            case "unfreeze" -> handleUnfreeze(sender, args);
            case "announce", "ann" -> handleAnnounce(sender, args);
            default -> showHelp(sender);
        }

        return true;
    }

    private void handleKick(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c使用法: /iron kick <プレイヤー> [理由]");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cプレイヤーが見つかりません");
            return;
        }
        
        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "規律違反";
        target.kickPlayer("§c§lキックされました\n\n§f理由: " + reason);
        
        Bukkit.broadcastMessage("§c§l【通知】§r §f" + target.getName() + " §7がキックされました。理由: " + reason);
        sender.sendMessage("§a" + target.getName() + " をキックした");
    }

    private void handleBan(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c使用法: /iron ban <プレイヤー> [理由]");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cプレイヤーが見つかりません");
            return;
        }
        
        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "規律違反";
        
        Bukkit.getBanList(BanList.Type.NAME).addBan(target.getName(), reason, null, sender.getName());
        target.kickPlayer("§c§lBANされました\n\n§f理由: " + reason);
        
        Bukkit.broadcastMessage("§4§l【BAN】§r §f" + target.getName() + " §7がBANされました。理由: " + reason);
        sender.sendMessage("§a" + target.getName() + " をBANした");
    }

    private void handleTp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player executor)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ実行可能");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§c使用法: /iron tp <プレイヤー>");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cプレイヤーが見つかりません");
            return;
        }
        
        executor.teleport(target.getLocation());
        sender.sendMessage("§a" + target.getName() + " にテレポートした");
    }

    private void handleBring(CommandSender sender, String[] args) {
        if (!(sender instanceof Player executor)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ実行可能");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§c使用法: /iron bring <プレイヤー>");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cプレイヤーが見つかりません");
            return;
        }
        
        target.teleport(executor.getLocation());
        sender.sendMessage("§a" + target.getName() + " を呼び出した");
        target.sendMessage("§e上官によって呼び出されました");
    }

    private void handleFreeze(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c使用法: /iron freeze <プレイヤー>");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cプレイヤーが見つかりません");
            return;
        }
        
        frozenPlayers.add(target.getUniqueId());
        sender.sendMessage("§a" + target.getName() + " を凍結した");
        target.sendMessage("§c§l【凍結】§r§c 移動が禁止されました");
    }

    private void handleUnfreeze(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c使用法: /iron unfreeze <プレイヤー>");
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cプレイヤーが見つかりません");
            return;
        }
        
        if (frozenPlayers.remove(target.getUniqueId())) {
            sender.sendMessage("§a" + target.getName() + " の凍結を解除した");
            target.sendMessage("§a凍結が解除されました");
        } else {
            sender.sendMessage("§c" + target.getName() + " は凍結されていません");
        }
    }

    private void handleAnnounce(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c使用法: /iron announce <メッセージ>");
            return;
        }
        
        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        
        // タイトル表示と全体ブロードキャスト
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle("§6§l【通達】", "§f" + message, 10, 100, 20);
            p.sendMessage("§6§l【通達】§r §f" + message);
        }
        
        sender.sendMessage("§aアナウンスを送信した");
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage("§6===== 鉄の規律 コマンド =====");
        sender.sendMessage("§e/iron reload §7- 設定をリロード");
        sender.sendMessage("§e/iron version §7- バージョン表示");
        sender.sendMessage("§e/iron cleanup §7- 古いログを削除");
        sender.sendMessage("§6----- 管理コマンド -----");
        sender.sendMessage("§e/iron kick <player> [理由] §7- キック");
        sender.sendMessage("§e/iron ban <player> [理由] §7- BAN");
        sender.sendMessage("§e/iron tp <player> §7- テレポート");
        sender.sendMessage("§e/iron bring <player> §7- 呼び出し");
        sender.sendMessage("§e/iron freeze <player> §7- 凍結");
        sender.sendMessage("§e/iron unfreeze <player> §7- 解凍");
        sender.sendMessage("§e/iron announce <msg> §7- 全体通達");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            for (String sub : new String[]{"reload", "version", "cleanup", "kick", "ban", "tp", "bring", "freeze", "unfreeze", "announce"}) {
                if (sub.startsWith(prefix)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (List.of("kick", "ban", "tp", "bring", "freeze", "unfreeze").contains(sub)) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(p.getName());
                    }
                }
            }
        }
        return completions;
    }
    
    /**
     * 凍結中かチェック
     */
    public boolean isFrozen(UUID playerId) {
        return frozenPlayers.contains(playerId);
    }
}
