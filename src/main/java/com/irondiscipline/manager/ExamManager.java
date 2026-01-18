package com.irondiscipline.manager;

import com.irondiscipline.IronDiscipline;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 試験システムマネージャー
 * チームの試験中状態とチャットミュートを管理
 */
public class ExamManager {

    private final IronDiscipline plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    // チーム名 -> メンバーUUID
    private final Map<String, Set<UUID>> teams = new ConcurrentHashMap<>();
    // 試験中のチーム
    private final Set<String> activeExams = ConcurrentHashMap.newKeySet();
    // プレイヤー -> 所属チーム
    private final Map<UUID, String> playerTeams = new ConcurrentHashMap<>();
    
    private File dataFile;

    public ExamManager(IronDiscipline plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "exams.json");
        loadData();
    }

    /**
     * チームを作成
     */
    public boolean createTeam(String teamName) {
        if (teams.containsKey(teamName.toLowerCase())) {
            return false;
        }
        teams.put(teamName.toLowerCase(), ConcurrentHashMap.newKeySet());
        saveData();
        return true;
    }

    /**
     * チームを削除
     */
    public boolean deleteTeam(String teamName) {
        String key = teamName.toLowerCase();
        if (!teams.containsKey(key)) {
            return false;
        }
        
        // メンバーの所属を解除
        Set<UUID> members = teams.get(key);
        for (UUID uuid : members) {
            playerTeams.remove(uuid);
        }
        
        teams.remove(key);
        activeExams.remove(key);
        saveData();
        return true;
    }

    /**
     * プレイヤーをチームに追加
     */
    public boolean addToTeam(UUID playerId, String teamName) {
        String key = teamName.toLowerCase();
        if (!teams.containsKey(key)) {
            return false;
        }
        
        // 既存チームから削除
        String currentTeam = playerTeams.get(playerId);
        if (currentTeam != null) {
            teams.get(currentTeam).remove(playerId);
        }
        
        teams.get(key).add(playerId);
        playerTeams.put(playerId, key);
        saveData();
        return true;
    }

    /**
     * プレイヤーをチームから削除
     */
    public boolean removeFromTeam(UUID playerId) {
        String teamName = playerTeams.remove(playerId);
        if (teamName == null) {
            return false;
        }
        
        Set<UUID> members = teams.get(teamName);
        if (members != null) {
            members.remove(playerId);
        }
        saveData();
        return true;
    }

    /**
     * 試験開始
     */
    public boolean startExam(String teamName) {
        String key = teamName.toLowerCase();
        if (!teams.containsKey(key)) {
            return false;
        }
        
        activeExams.add(key);
        
        // チームメンバーに通知
        notifyTeam(key, "§c§l【試験開始】§r§7 試験中はチャットが禁止されます");
        return true;
    }

    /**
     * 試験終了
     */
    public boolean endExam(String teamName) {
        String key = teamName.toLowerCase();
        if (!activeExams.remove(key)) {
            return false;
        }
        
        // チームメンバーに通知
        notifyTeam(key, "§a§l【試験終了】§r§7 チャットが解禁されました");
        return true;
    }

    /**
     * プレイヤーが試験中かチェック
     */
    public boolean isInExam(UUID playerId) {
        String teamName = playerTeams.get(playerId);
        if (teamName == null) {
            return false;
        }
        return activeExams.contains(teamName);
    }

    /**
     * プレイヤーの所属チームを取得
     */
    public String getPlayerTeam(UUID playerId) {
        return playerTeams.get(playerId);
    }

    /**
     * チームが存在するかチェック
     */
    public boolean teamExists(String teamName) {
        return teams.containsKey(teamName.toLowerCase());
    }

    /**
     * チームが試験中かチェック
     */
    public boolean isExamActive(String teamName) {
        return activeExams.contains(teamName.toLowerCase());
    }

    /**
     * すべてのチーム名を取得
     */
    public Set<String> getTeamNames() {
        return new HashSet<>(teams.keySet());
    }

    /**
     * チームメンバーを取得
     */
    public Set<UUID> getTeamMembers(String teamName) {
        Set<UUID> members = teams.get(teamName.toLowerCase());
        return members != null ? new HashSet<>(members) : new HashSet<>();
    }

    /**
     * チームに通知を送信
     */
    private void notifyTeam(String teamName, String message) {
        Set<UUID> members = teams.get(teamName);
        if (members == null) return;
        
        for (UUID uuid : members) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }

    /**
     * データを保存
     */
    private void saveData() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                ExamData data = new ExamData();
                
                // チームデータ変換
                for (Map.Entry<String, Set<UUID>> entry : teams.entrySet()) {
                    List<String> memberIds = new ArrayList<>();
                    for (UUID uuid : entry.getValue()) {
                        memberIds.add(uuid.toString());
                    }
                    data.teams.put(entry.getKey(), memberIds);
                }
                
                try (Writer writer = new FileWriter(dataFile)) {
                    gson.toJson(data, writer);
                }
            } catch (IOException e) {
                plugin.getLogger().warning("試験データ保存失敗: " + e.getMessage());
            }
        });
    }

    /**
     * データを読み込み
     */
    private void loadData() {
        if (!dataFile.exists()) {
            return;
        }
        
        try (Reader reader = new FileReader(dataFile)) {
            ExamData data = gson.fromJson(reader, ExamData.class);
            if (data != null && data.teams != null) {
                for (Map.Entry<String, List<String>> entry : data.teams.entrySet()) {
                    Set<UUID> members = ConcurrentHashMap.newKeySet();
                    for (String id : entry.getValue()) {
                        try {
                            UUID uuid = UUID.fromString(id);
                            members.add(uuid);
                            playerTeams.put(uuid, entry.getKey());
                        } catch (IllegalArgumentException ignored) {}
                    }
                    teams.put(entry.getKey(), members);
                }
            }
            plugin.getLogger().info("試験データ読み込み完了: " + teams.size() + "チーム");
        } catch (IOException e) {
            plugin.getLogger().warning("試験データ読み込み失敗: " + e.getMessage());
        }
    }

    /**
     * データクラス
     */
    private static class ExamData {
        Map<String, List<String>> teams = new HashMap<>();
    }
}
