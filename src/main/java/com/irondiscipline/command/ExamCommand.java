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
import java.util.Map;
import java.util.UUID;

/**
 * /exam コマンド
 * 試験システムの管理
 */
public class ExamCommand implements CommandExecutor, TabCompleter {

    private final IronDiscipline plugin;

    public ExamCommand(IronDiscipline plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 階級チェック（少尉以上）
        if (!plugin.getRankUtil().canExecuteCommand(sender)) {
            return true;
        }

        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "add" -> handleAdd(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "start" -> handleStart(sender, args);
            case "end" -> handleEnd(sender, args);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "question", "q" -> handleQuestion(sender, args);
            case "grade" -> handleGrade(sender, args);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c使用法: /exam create <チーム名>");
            return;
        }

        String teamName = args[1];
        if (plugin.getExamManager().createTeam(teamName)) {
            sender.sendMessage("§aチーム「" + teamName + "」を作成しました");
        } else {
            sender.sendMessage("§cそのチームは既に存在します");
        }
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c使用法: /exam delete <チーム名>");
            return;
        }

        String teamName = args[1];
        if (plugin.getExamManager().deleteTeam(teamName)) {
            sender.sendMessage("§aチーム「" + teamName + "」を削除しました");
        } else {
            sender.sendMessage("§cそのチームは存在しません");
        }
    }

    private void handleAdd(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§c使用法: /exam add <プレイヤー> <チーム名>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cプレイヤーが見つかりません");
            return;
        }

        // 階級チェック
        if (!plugin.getRankUtil().canOperateOn(sender, target)) {
            return;
        }

        String teamName = args[2];
        if (plugin.getExamManager().addToTeam(target.getUniqueId(), teamName)) {
            sender.sendMessage("§a" + target.getName() + " をチーム「" + teamName + "」に追加しました");
            target.sendMessage("§eチーム「" + teamName + "」に配属されました");
        } else {
            sender.sendMessage("§cチーム「" + teamName + "」は存在しません");
        }
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c使用法: /exam remove <プレイヤー>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cプレイヤーが見つかりません");
            return;
        }

        // 階級チェック
        if (!plugin.getRankUtil().canOperateOn(sender, target)) {
            return;
        }

        if (plugin.getExamManager().removeFromTeam(target.getUniqueId())) {
            sender.sendMessage("§a" + target.getName() + " をチームから除外しました");
            target.sendMessage("§eチームから除外されました");
        } else {
            sender.sendMessage("§cそのプレイヤーはチームに所属していません");
        }
    }

    private void handleStart(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c使用法: /exam start <チーム名>");
            return;
        }

        String teamName = args[1];
        if (plugin.getExamManager().startExam(teamName)) {
            sender.sendMessage("§aチーム「" + teamName + "」の試験を開始しました");
            Bukkit.broadcastMessage("§c§l【試験開始】§r チーム「" + teamName + "」の試験が開始されました");
        } else {
            sender.sendMessage("§cチーム「" + teamName + "」は存在しません");
        }
    }

    private void handleEnd(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c使用法: /exam end <チーム名>");
            return;
        }

        String teamName = args[1];
        if (plugin.getExamManager().endExam(teamName)) {
            sender.sendMessage("§aチーム「" + teamName + "」の試験を終了しました");
            Bukkit.broadcastMessage("§a§l【試験終了】§r チーム「" + teamName + "」の試験が終了しました");
        } else {
            sender.sendMessage("§cそのチームは試験中ではありません");
        }
    }

    private void handleList(CommandSender sender) {
        var teams = plugin.getExamManager().getTeamNames();
        if (teams.isEmpty()) {
            sender.sendMessage("§7登録されているチームはありません");
            return;
        }

        sender.sendMessage("§6=== 試験チーム一覧 ===");
        for (String team : teams) {
            boolean active = plugin.getExamManager().isExamActive(team);
            String status = active ? "§c[試験中]" : "§a[待機中]";
            int memberCount = plugin.getExamManager().getTeamMembers(team).size();
            sender.sendMessage("§7- §f" + team + " " + status + " §7(" + memberCount + "人)");
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c使用法: /exam info <チーム名>");
            return;
        }

        String teamName = args[1].toLowerCase();
        if (!plugin.getExamManager().teamExists(teamName)) {
            sender.sendMessage("§cそのチームは存在しません");
            return;
        }

        var members = plugin.getExamManager().getTeamMembers(teamName);
        boolean active = plugin.getExamManager().isExamActive(teamName);

        sender.sendMessage("§6=== チーム「" + teamName + "」===");
        sender.sendMessage("§7状態: " + (active ? "§c試験中" : "§a待機中"));
        sender.sendMessage("§7メンバー (" + members.size() + "人):");
        
        for (UUID uuid : members) {
            Player p = Bukkit.getPlayer(uuid);
            String name = p != null ? p.getName() : uuid.toString().substring(0, 8);
            String online = p != null && p.isOnline() ? "§a●" : "§c○";
            sender.sendMessage("  " + online + " §f" + name);
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§6=== 試験コマンド ===");
        sender.sendMessage("§e/exam create <チーム名> §7- チーム作成");
        sender.sendMessage("§e/exam delete <チーム名> §7- チーム削除");
        sender.sendMessage("§e/exam add <プレイヤー> <チーム> §7- 追加");
        sender.sendMessage("§e/exam remove <プレイヤー> §7- 除外");
        sender.sendMessage("§e/exam start <チーム名> §7- 試験開始");
        sender.sendMessage("§e/exam end <チーム名> §7- 試験終了");
        sender.sendMessage("§e/exam list §7- チーム一覧");
        sender.sendMessage("§e/exam info <チーム名> §7- 詳細");
        sender.sendMessage("§6--- 試験問題 ---");
        sender.sendMessage("§e/exam question <チーム> [番号] §7- 問題出題");
        sender.sendMessage("§e/exam grade <チーム> §7- 採点");
    }

    private void handleQuestion(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c使用法: /exam question <チーム> [テンプレート番号]");
            sender.sendMessage("§7番号を省略するとランダム出題");
            // テンプレート一覧表示
            sender.sendMessage("§6--- テンプレート一覧 ---");
            for (int i = 0; i < plugin.getExamQuestionManager().getTemplateCount(); i++) {
                var t = plugin.getExamQuestionManager().getTemplate(i);
                sender.sendMessage("§7" + (i + 1) + ". §f" + t.question);
            }
            return;
        }

        String teamName = args[1];
        if (!plugin.getExamManager().teamExists(teamName)) {
            sender.sendMessage("§cチームが存在しません");
            return;
        }

        if (!plugin.getExamManager().isExamActive(teamName)) {
            sender.sendMessage("§c試験を開始してから問題を出題してください");
            return;
        }

        if (args.length >= 3) {
            try {
                int index = Integer.parseInt(args[2]) - 1;
                plugin.getExamQuestionManager().sendTemplateQuestion(teamName, index);
                sender.sendMessage("§aテンプレート " + (index + 1) + " を出題しました");
            } catch (NumberFormatException e) {
                sender.sendMessage("§c無効な番号です");
            }
        } else {
            plugin.getExamQuestionManager().sendRandomQuestion(teamName);
            sender.sendMessage("§aランダム問題を出題しました");
        }
    }

    private void handleGrade(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c使用法: /exam grade <チーム>");
            return;
        }

        String teamName = args[1];
        Map<String, List<String>> result = plugin.getExamQuestionManager().gradeAnswers(teamName);

        if (result == null) {
            sender.sendMessage("§c出題中の問題がありません");
            return;
        }

        sender.sendMessage("§6=== 採点結果 ===");
        sender.sendMessage("§a正解: §f" + String.join(", ", result.get("correct")));
        sender.sendMessage("§c不正解: §f" + String.join(", ", result.get("incorrect")));
        sender.sendMessage("§7未回答: §f" + String.join(", ", result.get("noAnswer")));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subs = Arrays.asList("create", "delete", "add", "remove", "start", "end", "list", "info", "question", "grade");
            for (String sub : subs) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("add") || sub.equals("remove")) {
                // プレイヤー名
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(p.getName());
                    }
                }
            } else if (sub.equals("delete") || sub.equals("start") || sub.equals("end") || sub.equals("info")) {
                // チーム名
                for (String team : plugin.getExamManager().getTeamNames()) {
                    if (team.startsWith(args[1].toLowerCase())) {
                        completions.add(team);
                    }
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
            // チーム名
            for (String team : plugin.getExamManager().getTeamNames()) {
                if (team.startsWith(args[2].toLowerCase())) {
                    completions.add(team);
                }
            }
        }
        
        return completions;
    }
}
