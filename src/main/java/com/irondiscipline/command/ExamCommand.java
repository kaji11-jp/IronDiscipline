package com.irondiscipline.command;

import com.irondiscipline.IronDiscipline;
import com.irondiscipline.manager.ExamManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ExamCommand implements CommandExecutor, TabCompleter {

    private final IronDiscipline plugin;

    public ExamCommand(IronDiscipline plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "このコマンドはプレイヤーのみ使用可能です。");
            return true;
        }

        Player player = (Player) sender;
        ExamManager examManager = plugin.getExamManager();

        // 権限チェック (簡易的に少尉以上とする、本来はRankManagerでチェックすべき)
        if (!plugin.getRankManager().getRank(player).isHigherThan(com.irondiscipline.model.Rank.SERGEANT)) {
            player.sendMessage(ChatColor.RED + "権限がありません。少尉以上の階級が必要です。");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "start":
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "使用法: /exam start <プレイヤー> <試験タイプ>");
                    return true;
                }
                handleStart(player, args[1], args[2]);
                break;
            case "pass":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "使用法: /exam pass <プレイヤー>");
                    return true;
                }
                handlePass(player, args[1]);
                break;
            case "fail":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "使用法: /exam fail <プレイヤー>");
                    return true;
                }
                handleFail(player, args[1]);
                break;
            case "quiz":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "使用法: /exam quiz <プレイヤー>");
                    return true;
                }
                handleQuiz(player, args[1]);
                break;
            case "sts":
                examManager.startSTS(player);
                outputSTS(player);
                break;
            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void handleStart(Player instructor, String targetName, String type) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            instructor.sendMessage(plugin.getConfigManager().getMessage("player_not_found", "%player%", targetName));
            return;
        }
        plugin.getExamManager().startExamSession(instructor, target, type);
    }

    private void handlePass(Player instructor, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            instructor.sendMessage(plugin.getConfigManager().getMessage("player_not_found", "%player%", targetName));
            return;
        }
        plugin.getExamManager().passExam(instructor, target);
    }

    private void handleFail(Player instructor, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            instructor.sendMessage(plugin.getConfigManager().getMessage("player_not_found", "%player%", targetName));
            return;
        }
        plugin.getExamManager().failExam(instructor, target);
    }

    private void handleQuiz(Player instructor, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            instructor.sendMessage(plugin.getConfigManager().getMessage("player_not_found", "%player%", targetName));
            return;
        }
        plugin.getExamManager().startQuiz(instructor, target);
    }

    private void outputSTS(Player instructor) {
        // Chat broadcasting is handled in ExamManager, added local confirm if needed
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Exam Command Help ===");
        player.sendMessage(ChatColor.YELLOW + "/exam start <player> <type> " + ChatColor.WHITE + "- 試験開始宣言");
        player.sendMessage(ChatColor.YELLOW + "/exam pass <player> " + ChatColor.WHITE + "- 合格させる(昇進)");
        player.sendMessage(ChatColor.YELLOW + "/exam fail <player> " + ChatColor.WHITE + "- 不合格にする");
        player.sendMessage(ChatColor.YELLOW + "/exam quiz <player> " + ChatColor.WHITE + "- 筆記試験を開始");
        player.sendMessage(ChatColor.YELLOW + "/exam sts " + ChatColor.WHITE + "- STS(整列)号令");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("start", "pass", "fail", "quiz", "sts");
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("sts")) {
            return null; // Player list
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("start")) {
            return Arrays.asList("Basic", "Advanced", "Officer", "Driving", "Shooting");
        }
        return Collections.emptyList();
    }
}
