package com.irondiscipline.command;

import com.irondiscipline.IronDiscipline;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * /division コマンド
 * 部隊管理
 */
public class DivisionCommand implements CommandExecutor, TabCompleter {

    private final IronDiscipline plugin;

    public DivisionCommand(IronDiscipline plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 階級チェック
        if (!plugin.getRankUtil().canExecuteCommand(sender)) {
            return true;
        }

        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "set" -> handleSet(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "members" -> handleMembers(sender, args);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§c使用法: /division set <プレイヤー> <部隊>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cプレイヤーが見つかりません");
            return;
        }

        String division = args[2].toLowerCase();
        plugin.getDivisionManager().setDivision(target.getUniqueId(), division);
        
        String displayName = plugin.getDivisionManager().getDivisionDisplayName(division);
        sender.sendMessage("§a" + target.getName() + " を " + displayName + " §aに配属した");
        target.sendMessage("§e" + displayName + " §eに配属されました");
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c使用法: /division remove <プレイヤー>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cプレイヤーが見つかりません");
            return;
        }

        plugin.getDivisionManager().removeDivision(target.getUniqueId());
        sender.sendMessage("§a" + target.getName() + " の部隊配属を解除した");
        target.sendMessage("§e部隊配属が解除されました");
    }

    private void handleList(CommandSender sender) {
        Set<String> divisions = plugin.getDivisionManager().getAllDivisions();
        
        sender.sendMessage("§6=== 部隊一覧 ===");
        for (String div : divisions) {
            String display = plugin.getDivisionManager().getDivisionDisplayName(div);
            int count = plugin.getDivisionManager().getDivisionMembers(div).size();
            sender.sendMessage(display + " §7(" + count + "人)");
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        Player target;
        if (args.length > 1) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cプレイヤーが見つかりません");
                return;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage("§cプレイヤーを指定してください");
            return;
        }

        String division = plugin.getDivisionManager().getDivision(target.getUniqueId());
        if (division == null) {
            sender.sendMessage("§7" + target.getName() + " は部隊に所属していません");
        } else {
            String display = plugin.getDivisionManager().getDivisionDisplayName(division);
            sender.sendMessage("§f" + target.getName() + " の所属: " + display);
        }
    }

    private void handleMembers(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c使用法: /division members <部隊>");
            return;
        }

        String division = args[1].toLowerCase();
        Set<UUID> members = plugin.getDivisionManager().getDivisionMembers(division);
        
        String display = plugin.getDivisionManager().getDivisionDisplayName(division);
        sender.sendMessage("§6=== " + display + " §6のメンバー ===");
        
        if (members.isEmpty()) {
            sender.sendMessage("§7メンバーなし");
            return;
        }

        for (UUID uuid : members) {
            Player p = Bukkit.getPlayer(uuid);
            String name = p != null ? p.getName() : Bukkit.getOfflinePlayer(uuid).getName();
            String online = p != null && p.isOnline() ? "§a●" : "§c○";
            sender.sendMessage(online + " §f" + name);
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§6=== 部隊コマンド ===");
        sender.sendMessage("§e/division set <プレイヤー> <部隊> §7- 配属");
        sender.sendMessage("§e/division remove <プレイヤー> §7- 解除");
        sender.sendMessage("§e/division list §7- 部隊一覧");
        sender.sendMessage("§e/division info [プレイヤー] §7- 所属確認");
        sender.sendMessage("§e/division members <部隊> §7- メンバー一覧");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subs = Arrays.asList("set", "remove", "list", "info", "members");
            for (String s : subs) {
                if (s.startsWith(args[0].toLowerCase())) {
                    completions.add(s);
                }
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("set") || sub.equals("remove") || sub.equals("info")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(p.getName());
                    }
                }
            } else if (sub.equals("members")) {
                for (String div : plugin.getDivisionManager().getAllDivisions()) {
                    if (div.startsWith(args[1].toLowerCase())) {
                        completions.add(div);
                    }
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            for (String div : plugin.getDivisionManager().getAllDivisions()) {
                if (div.startsWith(args[2].toLowerCase())) {
                    completions.add(div);
                }
            }
        }

        return completions;
    }
}
