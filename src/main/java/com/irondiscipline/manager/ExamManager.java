package com.irondiscipline.manager;

import com.irondiscipline.IronDiscipline;
import com.irondiscipline.model.Rank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 試験マネージャー
 * 試験セッションとSTS(整列)機能を管理
 */
public class ExamManager implements Listener {

    private final IronDiscipline plugin;

    // クイズ試験中のプレイヤーと現在の状態
    // Map<PlayerUUID, QuizState>
    private final Map<UUID, QuizSession> quizSessions = new ConcurrentHashMap<>();

    public ExamManager(IronDiscipline plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * STS (Shoulder To Shoulder) - 整列号令
     * 指定したライン上にプレイヤーを整列させる、または整列を指示する
     */
    public void startSTS(Player officer) {
        // RP的な演出として、周囲のプレイヤーにメッセージとサウンドを送信
        String message = ChatColor.YELLOW + "" + ChatColor.BOLD + "=== STS (SHOULDER TO SHOULDER) ===";
        String subMessage = ChatColor.RED + officer.getName() + " が整列を命じた！直ちに整列せよ！";

        officer.getWorld().getPlayers().stream()
                .filter(p -> p.getLocation().distance(officer.getLocation()) < 50)
                .forEach(p -> {
                    p.sendMessage(message);
                    p.sendMessage(subMessage);
                    p.playSound(p.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
                });

        // オプション: 強制テレポートによる整列 (Roblox RP風)
        // 実装が複雑になるため、今回はメッセージのみとするが、
        // 必要に応じて officerの視線方向に一列に並べるロジックを追加可能
    }

    /**
     * 試験開始メッセージ
     */
    public void startExamSession(Player instructor, Player target, String type) {
        String msg = String.format("&e[試験] &f%s &eが &f%s &eの &b%s &e試験を開始した。",
                instructor.getName(), target.getName(), type);
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', msg));
    }

    /**
     * 試験合格
     */
    public void passExam(Player instructor, Player target) {
        String msg = String.format("&a[合格] &f%s &aは試験に合格した！", target.getName());
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', msg));

        plugin.getRankManager().promote(target).thenAccept(newRank -> {
            if (newRank != null) {
                target.sendMessage(ChatColor.GREEN + "昇進おめでとう！: " + newRank.getDisplay());
            }
        });
    }

    /**
     * 試験不合格
     */
    public void failExam(Player instructor, Player target) {
        String msg = String.format("&c[不合格] &f%s &cは試験に不合格となった。", target.getName());
        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', msg));
        // 必要ならキックなどの処理
    }

    // ===== クイズ機能 =====

    public void startQuiz(Player instructor, Player target) {
        if (quizSessions.containsKey(target.getUniqueId())) {
            instructor.sendMessage(ChatColor.RED + "そのプレイヤーは既に試験中です。");
            return;
        }

        // デモ用の簡単な問題リスト
        List<Question> questions = new ArrayList<>();
        questions.add(new Question("上官の命令は？", "絶対", Arrays.asList("絶対", "ぜったい")));
        questions.add(new Question("無許可での発砲は許されるか？(はい/いいえ)", "いいえ", Arrays.asList("いいえ", "no")));
        questions.add(new Question("サーバーの最強の階級は？", "司令官", Arrays.asList("司令官", "commander")));

        QuizSession session = new QuizSession(target.getUniqueId(), questions);
        quizSessions.put(target.getUniqueId(), session);

        target.sendMessage(ChatColor.GREEN + "=== 筆記試験開始 ===");
        target.sendMessage(ChatColor.YELLOW + "チャットで回答してください。");
        askNextQuestion(target, session);
    }

    private void askNextQuestion(Player player, QuizSession session) {
        Question q = session.getCurrentQuestion();
        if (q == null) {
            finishQuiz(player, session);
            return;
        }
        player.sendMessage(ChatColor.GOLD + "問" + (session.currentIndex + 1) + ": " + ChatColor.WHITE + q.text);
    }

    private void finishQuiz(Player player, QuizSession session) {
        quizSessions.remove(player.getUniqueId());

        int score = session.score;
        int total = session.questions.size();
        double percentage = (double) score / total;

        player.sendMessage(ChatColor.YELLOW + "試験終了！");
        player.sendMessage(ChatColor.WHITE + "正解数: " + score + " / " + total);

        if (percentage >= 0.8) { // 80%以上で合格
            Bukkit.broadcastMessage(ChatColor.GREEN + player.getName() + " が筆記試験に合格しました！");
            plugin.getRankManager().promote(player);
        } else {
            player.sendMessage(ChatColor.RED + "不合格です。出直してこい！");
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!quizSessions.containsKey(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true); // チャットをキャンセル
        QuizSession session = quizSessions.get(player.getUniqueId());
        String answer = event.getMessage();

        // 強制終了コマンド
        if (answer.equalsIgnoreCase("cancel")) {
            quizSessions.remove(player.getUniqueId());
            player.sendMessage(ChatColor.RED + "試験をキャンセルしました。");
            return;
        }

        Question q = session.getCurrentQuestion();
        if (q != null) {
            boolean isCorrect = q.isCorrect(answer);
            if (isCorrect) {
                player.sendMessage(ChatColor.GREEN + "正解！");
                session.score++;
            } else {
                player.sendMessage(ChatColor.RED + "不正解... (正解: " + q.correctDisplay + ")");
            }

            session.currentIndex++;

            // 次の問題へ（少し遅延させると親切だが、今回は即時）
            Bukkit.getScheduler().runTask(plugin, () -> askNextQuestion(player, session));
        }
    }

    // 内部クラス
    private static class QuizSession {
        UUID playerId;
        List<Question> questions;
        int currentIndex = 0;
        int score = 0;

        QuizSession(UUID playerId, List<Question> questions) {
            this.playerId = playerId;
            this.questions = questions;
        }

        Question getCurrentQuestion() {
            if (currentIndex < questions.size()) {
                return questions.get(currentIndex);
            }
            return null;
        }
    }

    private static class Question {
        String text;
        String correctDisplay;
        List<String> validAnswers;

        Question(String text, String correctDisplay, List<String> validAnswers) {
            this.text = text;
            this.correctDisplay = correctDisplay;
            this.validAnswers = validAnswers;
        }

        boolean isCorrect(String answer) {
            for (String valid : validAnswers) {
                if (valid.equalsIgnoreCase(answer))
                    return true;
            }
            return false;
        }
    }
}
