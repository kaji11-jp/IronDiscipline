package com.irondiscipline.model;

import java.util.UUID;

/**
 * 戦闘ログエントリ
 */
public class KillLog {

    private final long id;
    private final long timestamp;
    private final UUID killerId;
    private final String killerName;
    private final UUID victimId;
    private final String victimName;
    private final String weapon;
    private final double distance;
    private final String world;
    private final double x;
    private final double y;
    private final double z;

    public KillLog(long id, long timestamp, UUID killerId, String killerName,
                   UUID victimId, String victimName, String weapon, double distance,
                   String world, double x, double y, double z) {
        this.id = id;
        this.timestamp = timestamp;
        this.killerId = killerId;
        this.killerName = killerName;
        this.victimId = victimId;
        this.victimName = victimName;
        this.weapon = weapon;
        this.distance = distance;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    // Builder pattern for creation
    public static Builder builder() {
        return new Builder();
    }

    public long getId() { return id; }
    public long getTimestamp() { return timestamp; }
    public UUID getKillerId() { return killerId; }
    public String getKillerName() { return killerName; }
    public UUID getVictimId() { return victimId; }
    public String getVictimName() { return victimName; }
    public String getWeapon() { return weapon; }
    public double getDistance() { return distance; }
    public String getWorld() { return world; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }

    /**
     * 人間向けフォーマット済み距離
     */
    public String getFormattedDistance() {
        return String.format("%.1fm", distance);
    }

    public static class Builder {
        private long id;
        private long timestamp = System.currentTimeMillis();
        private UUID killerId;
        private String killerName;
        private UUID victimId;
        private String victimName;
        private String weapon = "不明";
        private double distance;
        private String world;
        private double x, y, z;

        public Builder id(long id) { this.id = id; return this; }
        public Builder timestamp(long timestamp) { this.timestamp = timestamp; return this; }
        public Builder killer(UUID id, String name) { this.killerId = id; this.killerName = name; return this; }
        public Builder victim(UUID id, String name) { this.victimId = id; this.victimName = name; return this; }
        public Builder weapon(String weapon) { this.weapon = weapon; return this; }
        public Builder distance(double distance) { this.distance = distance; return this; }
        public Builder location(String world, double x, double y, double z) {
            this.world = world; this.x = x; this.y = y; this.z = z;
            return this;
        }

        public KillLog build() {
            return new KillLog(id, timestamp, killerId, killerName, victimId, victimName,
                             weapon, distance, world, x, y, z);
        }
    }
}
