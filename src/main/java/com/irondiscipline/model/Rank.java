package com.irondiscipline.model;

import org.bukkit.ChatColor;

/**
 * 軍階級enum
 * 重みが低いほど下位階級
 */
public enum Rank {
    
    PRIVATE("PRIVATE", "&7[二等兵]", 10),
    PRIVATE_FIRST_CLASS("PRIVATE_FIRST_CLASS", "&7[一等兵]", 15),
    CORPORAL("CORPORAL", "&7[伍長]", 20),
    SERGEANT("SERGEANT", "&e[軍曹]", 30),
    LIEUTENANT("LIEUTENANT", "&6[少尉]", 40),
    CAPTAIN("CAPTAIN", "&6[大尉]", 50),
    MAJOR("MAJOR", "&c[少佐]", 60),
    COLONEL("COLONEL", "&c[大佐]", 70),
    COMMANDER("COMMANDER", "&4&l[司令官]", 100);

    private final String id;
    private final String displayRaw;
    private final int weight;

    Rank(String id, String displayRaw, int weight) {
        this.id = id;
        this.displayRaw = displayRaw;
        this.weight = weight;
    }

    public String getId() {
        return id;
    }

    public String getDisplayRaw() {
        return displayRaw;
    }

    public String getDisplay() {
        return ChatColor.translateAlternateColorCodes('&', displayRaw);
    }

    public int getWeight() {
        return weight;
    }

    /**
     * この階級が対象より上位かどうか
     */
    public boolean isHigherThan(Rank other) {
        return this.weight > other.weight;
    }

    /**
     * この階級が対象より下位かどうか
     */
    public boolean isLowerThan(Rank other) {
        return this.weight < other.weight;
    }

    /**
     * PTSが必要かどうか (重み25以下)
     */
    public boolean requiresPTS() {
        return this.weight <= 25;
    }

    /**
     * 次の階級を取得 (昇進)
     */
    public Rank getNextRank() {
        Rank[] ranks = values();
        for (int i = 0; i < ranks.length - 1; i++) {
            if (ranks[i] == this) {
                return ranks[i + 1];
            }
        }
        return null; // 最高階級
    }

    /**
     * 前の階級を取得 (降格)
     */
    public Rank getPreviousRank() {
        Rank[] ranks = values();
        for (int i = 1; i < ranks.length; i++) {
            if (ranks[i] == this) {
                return ranks[i - 1];
            }
        }
        return null; // 最低階級
    }

    /**
     * IDから階級を取得
     */
    public static Rank fromId(String id) {
        if (id == null) return PRIVATE;
        for (Rank rank : values()) {
            if (rank.id.equalsIgnoreCase(id)) {
                return rank;
            }
        }
        return PRIVATE;
    }

    /**
     * 重みから階級を取得
     */
    public static Rank fromWeight(int weight) {
        Rank result = PRIVATE;
        for (Rank rank : values()) {
            if (rank.weight <= weight && rank.weight > result.weight) {
                result = rank;
            }
        }
        return result;
    }
}
