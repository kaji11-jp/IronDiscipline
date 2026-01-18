package com.irondiscipline.manager;

import com.irondiscipline.IronDiscipline;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 設定マネージャー
 * config.ymlからの値取得とメッセージ処理
 */
public class ConfigManager {

    private final IronDiscipline plugin;
    private FileConfiguration config;

    public ConfigManager(IronDiscipline plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    // ===== General =====
    
    public boolean isDebug() {
        return config.getBoolean("general.debug", false);
    }

    public String getLocale() {
        return config.getString("general.locale", "ja_JP");
    }

    // ===== Database =====
    
    public String getDatabaseType() {
        return config.getString("database.type", "h2");
    }

    public String getMySQLHost() {
        return config.getString("database.mysql.host", "localhost");
    }

    public int getMySQLPort() {
        return config.getInt("database.mysql.port", 3306);
    }

    public String getMySQLDatabase() {
        return config.getString("database.mysql.database", "irondiscipline");
    }

    public String getMySQLUsername() {
        return config.getString("database.mysql.username", "root");
    }

    public String getMySQLPassword() {
        return config.getString("database.mysql.password", "");
    }

    // ===== Ranks =====
    
    public String getRankMetaKey() {
        return config.getString("ranks.meta_key", "military_rank");
    }

    // ===== PTS =====
    
    public int getPTSRequireBelowWeight() {
        return config.getInt("pts.require_below_weight", 25);
    }

    public int getDefaultGrantDuration() {
        return config.getInt("pts.default_grant_duration", 60);
    }

    public boolean isSneakRequestEnabled() {
        return config.getBoolean("pts.sneak_request.enabled", true);
    }

    public int getDoubleSneakThreshold() {
        return config.getInt("pts.sneak_request.double_sneak_threshold", 500);
    }

    public String getPTSRequestPrefix() {
        return colorize(config.getString("pts.request_prefix", "&8[&ePTS要請&8]"));
    }

    // ===== Radio =====
    
    public String getDefaultFrequency() {
        return config.getString("radio.default_frequency", "118.0");
    }

    public String getRadioFormat() {
        return config.getString("radio.format", "&8[&b無線 %freq%&8] &7%rank% %player%&8: &f%message%");
    }

    // ===== Jail =====
    
    public Location getJailLocation() {
        String world = config.getString("jail.location.world", "world");
        double x = config.getDouble("jail.location.x", 0);
        double y = config.getDouble("jail.location.y", 64);
        double z = config.getDouble("jail.location.z", 0);
        
        if (plugin.getServer().getWorld(world) == null) {
            return null;
        }
        return new Location(plugin.getServer().getWorld(world), x, y, z);
    }

    public void setJailLocation(Location location) {
        config.set("jail.location.world", location.getWorld().getName());
        config.set("jail.location.x", location.getX());
        config.set("jail.location.y", location.getY());
        config.set("jail.location.z", location.getZ());
        plugin.saveConfig();
    }

    public String getJailBlockedMessage() {
        return colorize(config.getString("jail.blocked_message", "&c貴官は拘留中だ。発言は許可されていない。"));
    }

    // ===== KillLog =====
    
    public int getKillLogRetentionDays() {
        return config.getInt("killlog.retention_days", 30);
    }

    public boolean isDetailedKillLog() {
        return config.getBoolean("killlog.detailed", true);
    }

    // ===== Messages =====
    
    public String getPrefix() {
        return colorize(config.getString("messages.prefix", "&8[&c鉄の規律&8] "));
    }

    public String getMessage(String key) {
        String message = config.getString("messages." + key, key);
        return getPrefix() + colorize(message);
    }

    public String getMessage(String key, String... replacements) {
        String message = getMessage(key);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }
        return message;
    }

    public String getRawMessage(String key) {
        return colorize(config.getString("messages." + key, key));
    }

    // ===== Discord =====
    
    public boolean isDiscordEnabled() {
        return config.getBoolean("discord.enabled", false);
    }

    public String getDiscordBotToken() {
        return config.getString("discord.bot_token", "");
    }

    public String getDiscordNotificationChannel() {
        return config.getString("discord.notification_channel_id", "");
    }

    public String getDiscordGuildId() {
        return config.getString("discord.guild_id", "");
    }

    public String getDiscordUnverifiedRoleId() {
        return config.getString("discord.unverified_role_id", "");
    }

    public String getDiscordVerifiedRoleId() {
        return config.getString("discord.verified_role_id", "");
    }

    public String getDonationInfo() {
        return config.getString("discord.donation_info", "");
    }

    // ===== Utility =====
    
    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
