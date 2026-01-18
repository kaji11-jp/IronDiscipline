package com.irondiscipline.manager;

import com.irondiscipline.IronDiscipline;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 試験問題GUIマネージャー
 * マルチチョイス問題の出題と回答
 */
public class ExamQuestionManager implements Listener {

    private final IronDiscipline plugin;
    
    // チーム -> 現在の問題
    private final Map<String, ExamQuestion> activeQuestions = new ConcurrentHashMap<>();
    
    // プレイヤー -> 回答
    private final Map<UUID, String> playerAnswers = new ConcurrentHashMap<>();
    
    // 問題テンプレート
    private final List<QuestionTemplate> templates = new ArrayList<>();
    
    private static final String GUI_TITLE = "§6§l【試験問題】";

    public ExamQuestionManager(IronDiscipline plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        initTemplates();
    }

    /**
     * 問題テンプレートを初期化
     */
    private void initTemplates() {
        // テンプレート1: 一般知識
        templates.add(new QuestionTemplate(
            "敬礼の正しい順序は？",
            Arrays.asList("目を合わせる → 敬礼 → 「忠誠！」", "「忠誠！」→ 敬礼 → 目を合わせる", 
                          "敬礼 → 目を合わせる → 「忠誠！」", "敬礼のみ"),
            "A"
        ));
        
        // テンプレート2: 規律
        templates.add(new QuestionTemplate(
            "上官に話しかける前に必要なのは？",
            Arrays.asList("PTS（発言許可）を得る", "敬礼する", "自己紹介する", "何もしない"),
            "A"
        ));
        
        // テンプレート3: 命令
        templates.add(new QuestionTemplate(
            "命令に従えない場合の正しい対応は？",
            Arrays.asList("理由を述べて代替案を提案", "黙って無視する", "その場で抗議する", "他の兵士に押し付ける"),
            "A"
        ));
        
        // テンプレート4: 階級
        templates.add(new QuestionTemplate(
            "少尉は何に分類されるか？",
            Arrays.asList("士官", "下士官", "兵卒", "准士官"),
            "A"
        ));
        
        // テンプレート5: 行動規範
        templates.add(new QuestionTemplate(
            "戦闘中に負傷した同僚を発見した場合は？",
            Arrays.asList("衛生兵を呼ぶ", "無視して戦闘を続ける", "一人で救助する", "撤退する"),
            "A"
        ));
    }

    /**
     * 問題を出題
     */
    public void sendQuestion(String teamName, String question, List<String> choices, String correctAnswer) {
        String key = teamName.toLowerCase();
        
        ExamQuestion eq = new ExamQuestion(question, choices, correctAnswer);
        activeQuestions.put(key, eq);
        playerAnswers.clear();
        
        Set<UUID> members = plugin.getExamManager().getTeamMembers(key);
        for (UUID uuid : members) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                openQuestionGUI(player, eq);
            }
        }
    }

    /**
     * テンプレートから問題を出題
     */
    public void sendTemplateQuestion(String teamName, int templateIndex) {
        if (templateIndex < 0 || templateIndex >= templates.size()) {
            return;
        }
        
        QuestionTemplate t = templates.get(templateIndex);
        sendQuestion(teamName, t.question, t.choices, t.correctAnswer);
    }

    /**
     * ランダムなテンプレート問題を出題
     */
    public void sendRandomQuestion(String teamName) {
        if (templates.isEmpty()) return;
        int index = new Random().nextInt(templates.size());
        sendTemplateQuestion(teamName, index);
    }

    /**
     * 問題GUIを開く
     */
    private void openQuestionGUI(Player player, ExamQuestion question) {
        Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);
        
        // 問題文（本）
        ItemStack questionItem = new ItemStack(Material.BOOK);
        ItemMeta qMeta = questionItem.getItemMeta();
        qMeta.setDisplayName("§e§l" + question.question);
        qMeta.setLore(Arrays.asList("§7---", "§7下の選択肢から回答を選んでください"));
        questionItem.setItemMeta(qMeta);
        inv.setItem(4, questionItem);
        
        // 選択肢
        Material[] colors = {Material.RED_WOOL, Material.BLUE_WOOL, Material.GREEN_WOOL, Material.YELLOW_WOOL};
        String[] labels = {"A", "B", "C", "D"};
        int[] slots = {10, 12, 14, 16};
        
        for (int i = 0; i < Math.min(question.choices.size(), 4); i++) {
            ItemStack choice = new ItemStack(colors[i]);
            ItemMeta cMeta = choice.getItemMeta();
            cMeta.setDisplayName("§f§l" + labels[i] + ". " + question.choices.get(i));
            cMeta.setLore(Arrays.asList("§7クリックして回答"));
            choice.setItemMeta(cMeta);
            inv.setItem(slots[i], choice);
        }
        
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(GUI_TITLE)) return;
        
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        int slot = event.getSlot();
        String answer = switch (slot) {
            case 10 -> "A";
            case 12 -> "B";
            case 14 -> "C";
            case 16 -> "D";
            default -> null;
        };
        
        if (answer != null) {
            playerAnswers.put(player.getUniqueId(), answer);
            player.closeInventory();
            player.sendMessage("§a回答を受け付けました: §f" + answer);
        }
    }

    /**
     * 採点して結果を返す
     */
    public Map<String, List<String>> gradeAnswers(String teamName) {
        String key = teamName.toLowerCase();
        ExamQuestion question = activeQuestions.get(key);
        
        if (question == null) {
            return null;
        }
        
        List<String> correct = new ArrayList<>();
        List<String> incorrect = new ArrayList<>();
        List<String> noAnswer = new ArrayList<>();
        
        Set<UUID> members = plugin.getExamManager().getTeamMembers(key);
        for (UUID uuid : members) {
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            String answer = playerAnswers.get(uuid);
            
            if (answer == null) {
                noAnswer.add(name);
            } else if (answer.equalsIgnoreCase(question.correctAnswer)) {
                correct.add(name);
            } else {
                incorrect.add(name);
            }
        }
        
        Map<String, List<String>> result = new HashMap<>();
        result.put("correct", correct);
        result.put("incorrect", incorrect);
        result.put("noAnswer", noAnswer);
        
        // クリア
        activeQuestions.remove(key);
        
        return result;
    }

    /**
     * テンプレート数を取得
     */
    public int getTemplateCount() {
        return templates.size();
    }

    /**
     * テンプレート情報を取得
     */
    public QuestionTemplate getTemplate(int index) {
        if (index < 0 || index >= templates.size()) return null;
        return templates.get(index);
    }

    // データクラス
    public static class ExamQuestion {
        public final String question;
        public final List<String> choices;
        public final String correctAnswer;

        public ExamQuestion(String question, List<String> choices, String correctAnswer) {
            this.question = question;
            this.choices = choices;
            this.correctAnswer = correctAnswer;
        }
    }

    public static class QuestionTemplate {
        public final String question;
        public final List<String> choices;
        public final String correctAnswer;

        public QuestionTemplate(String question, List<String> choices, String correctAnswer) {
            this.question = question;
            this.choices = choices;
            this.correctAnswer = correctAnswer;
        }
    }
}
