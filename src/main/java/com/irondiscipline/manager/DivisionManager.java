package com.irondiscipline.manager;

import com.irondiscipline.IronDiscipline;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 部隊マネージャー
 * 憲兵(MP)、歩兵、砲兵などの部隊管理
 */
public class DivisionManager {

    private final IronDiscipline plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // プレイヤー -> 部隊
    private final Map<UUID, String> playerDivisions = new ConcurrentHashMap<>();

    // 部隊一覧
    private final Set<String> divisions = ConcurrentHashMap.newKeySet();

    // 憲兵部隊のID
    private static final String MP_DIVISION = "mp";

    private File dataFile;

    public DivisionManager(IronDiscipline plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "divisions.json");
        loadData();

        // デフォルト部隊を追加
        initDefaultDivisions();
    }

    private void initDefaultDivisions() {
        divisions.add("mp"); // 憲兵 (Military Police)
        divisions.add("infantry"); // 歩兵
        divisions.add("artillery");// 砲兵
        divisions.add("aviation"); // 航空
        divisions.add("medical"); // 衛生
        divisions.add("command"); // 司令部
    }

    /**
     * プレイヤーの部隊を設定
     */
    public void setDivision(UUID playerId, String division) {
        String div = division.toLowerCase();
        divisions.add(div);
        playerDivisions.put(playerId, div);
        saveData();
    }

    /**
     * プレイヤーの部隊を解除
     */
    public void removeDivision(UUID playerId) {
        playerDivisions.remove(playerId);
        saveData();
    }

    /**
     * プレイヤーの部隊を取得
     */
    public String getDivision(UUID playerId) {
        return playerDivisions.get(playerId);
    }

    /**
     * プレイヤーの部隊表示名を取得
     */
    public String getDivisionDisplay(UUID playerId) {
        String div = playerDivisions.get(playerId);
        if (div == null)
            return null;
        return getDivisionDisplayName(div);
    }

    /**
     * 部隊IDから表示名を取得
     */
    public String getDivisionDisplayName(String divisionId) {
        return switch (divisionId.toLowerCase()) {
            case "mp" -> "§c[憲兵]";
            case "infantry" -> "§a[歩兵]";
            case "artillery" -> "§6[砲兵]";
            case "aviation" -> "§b[航空]";
            case "medical" -> "§d[衛生]";
            case "command" -> "§4[司令部]";
            default -> "§7[" + divisionId + "]";
        };
    }

    /**
     * 憲兵かどうか
     */
    public boolean isMP(Player player) {
        return isMP(player.getUniqueId());
    }

    public boolean isMP(UUID playerId) {
        String div = playerDivisions.get(playerId);
        return MP_DIVISION.equalsIgnoreCase(div);
    }

    /**
     * 部隊が存在するか
     */
    public boolean divisionExists(String division) {
        return divisions.contains(division.toLowerCase());
    }

    /**
     * 全部隊一覧
     */
    public Set<String> getAllDivisions() {
        return new HashSet<>(divisions);
    }

    /**
     * 部隊のメンバーを取得
     */
    public Set<UUID> getDivisionMembers(String division) {
        Set<UUID> members = new HashSet<>();
        String div = division.toLowerCase();
        for (Map.Entry<UUID, String> entry : playerDivisions.entrySet()) {
            if (entry.getValue().equals(div)) {
                members.add(entry.getKey());
            }
        }
        return members;
    }

    /**
     * 新しい部隊を作成
     */
    public void createDivision(String division) {
        divisions.add(division.toLowerCase());
        saveData();
    }

    /**
     * データを保存
     */
    private void saveData() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                DivisionData data = new DivisionData();
                data.divisions = new ArrayList<>(divisions);
                for (Map.Entry<UUID, String> entry : playerDivisions.entrySet()) {
                    data.players.put(entry.getKey().toString(), entry.getValue());
                }

                try (Writer writer = new FileWriter(dataFile)) {
                    gson.toJson(data, writer);
                }
            } catch (IOException e) {
                plugin.getLogger().warning("部隊データ保存失敗: " + e.getMessage());
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
            DivisionData data = gson.fromJson(reader, DivisionData.class);
            if (data != null) {
                if (data.divisions != null) {
                    divisions.addAll(data.divisions);
                }
                if (data.players != null) {
                    for (Map.Entry<String, String> entry : data.players.entrySet()) {
                        try {
                            UUID uuid = UUID.fromString(entry.getKey());
                            playerDivisions.put(uuid, entry.getValue());
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            }
            plugin.getLogger().info("部隊データ読み込み完了");
        } catch (IOException e) {
            plugin.getLogger().warning("部隊データ読み込み失敗: " + e.getMessage());
        }
    }

    private static class DivisionData {
        List<String> divisions = new ArrayList<>();
        Map<String, String> players = new HashMap<>();
    }
}
