package com.irondiscipline.manager;

import com.irondiscipline.IronDiscipline;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.logging.Level;

/**
 * 設定マネージャー
 * config.ymlからの値取得とメッセージ処理
 */
public class ConfigManager {

    private final IronDiscipline plugin;
    private FileConfiguration config;
    private FileConfiguration messagesConfig;

    public ConfigManager(IronDiscipline plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        loadMessages();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        loadMessages();
    }

    private void loadMessages() {
        String locale = getLocale();
        String fileName = "messages_" + locale + ".yml";
        File messagesFile = new File(plugin.getDataFolder(), "locales/" + fileName);

        if (!messagesFile.exists()) {
            try {
                plugin.saveResource("locales/" + fileName, false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().log(Level.WARNING, "Locale file " + fileName + " not found in jar. Using default.", e);
            }
        }

        if (messagesFile.exists()) {
            messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        } else {
            // Fallback to ja_JP if the selected locale doesn't exist
            File defaultFile = new File(plugin.getDataFolder(), "locales/messages_ja_JP.yml");
            if (defaultFile.exists()) {
                messagesConfig = YamlConfiguration.loadConfiguration(defaultFile);
            } else {
                // Should not happen if saveResource works or if ja_JP is in jar
                plugin.getLogger().severe("Could not load any message file!");
                messagesConfig = new YamlConfiguration();
            }
        }
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
        // Try getting from messages first (for multilingual support), then config
        String prefix = messagesConfig.getString("pts_request_prefix");
        if (prefix == null) {
            prefix = config.getString("pts.request_prefix", "&8[&ePTS要請&8]");
        }
        return colorize(prefix);
    }

    // ===== Radio =====

    public String getDefaultFrequency() {
        return config.getString("radio.default_frequency", "118.0");
    }

    public String getRadioFormat() {
        String format = messagesConfig.getString("radio_format");
        if (format == null) {
            format = config.getString("radio.format", "&8[&b無線 %freq%&8] &7%rank% %player%&8: &f%message%");
        }
        return format;
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
        String message = messagesConfig.getString("jail_blocked_message"); // Check locale file first (if I added it there)
        if (message == null) {
             message = config.getString("jail.blocked_message", "&c貴官は拘留中だ。発言は許可されていない。");
        }
        return colorize(message);
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
        return colorize(messagesConfig.getString("prefix", "&8[&cIronDiscipline&8] "));
    }

    // ===== Warnings =====

    public int getWarningJailThreshold() {
        return config.getInt("warnings.thresholds.jail", 3);
    }

    public int getWarningKickThreshold() {
        return config.getInt("warnings.thresholds.kick", 5);
    }

    // ===== Promotion =====

    public boolean isTimeBasedPromotionEnabled() {
        return config.getBoolean("promotion.time_based.enabled", false);
    }

    public int getTimeBasedPromotionInterval() {
        return config.getInt("promotion.time_based.interval", 300);
    }

    public boolean isTimeBasedCheckAFK() {
        return config.getBoolean("promotion.time_based.check_afk", true);
    }

    public int getServerPlaytimeRequirement(String rankId) {
        return config.getInt("promotion.time_based.rules." + rankId.toUpperCase(), -1);
    }

    public int getExamQuizTimeout() {
        return config.getInt("promotion.exam.quiz_timeout", 45);
    }

    public String getMessage(String key) {
        String message = messagesConfig.getString(key);
        if (message == null) {
            // Fallback to main config for backward compatibility
            message = config.getString("messages." + key, key);
        }
        return getPrefix() + colorize(message);
    }

    public String getMessage(String key, String... replacements) {
        // We do not call getMessage(key) here because it prepends prefix.
        // We want to replace FIRST then prepend prefix, or prepend prefix then replace.
        // The original implementation called getMessage(key) which added prefix, then did replacements.
        // Replacements might be in the prefix too? Unlikely but possible.
        // Let's check original:
        // String message = getMessage(key); -> returns prefix + colorized(message)
        // message.replace(...)

        String message = messagesConfig.getString(key);
        if (message == null) {
            message = config.getString("messages." + key, key);
        }

        // Colorize first or last? Original: getMessage calls colorize.
        message = colorize(message);
        String fullMessage = getPrefix() + message;

        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                fullMessage = fullMessage.replace(replacements[i], replacements[i + 1]);
            }
        }
        return fullMessage;
    }

    public String getRawMessage(String key) {
        String message = messagesConfig.getString(key);
        if (message == null) {
            message = config.getString("messages." + key, key);
        }
        return colorize(message);
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

    public String getDiscordNotificationRoleId() {
        return config.getString("discord.notification_role_id", "");
    }

    public String getDiscordConsoleRoleId() {
        return config.getString("discord.console_role_id", "");
    }

    public String getDiscordRankRoleId(String rankId) {
        return config.getString("discord.rank_roles." + rankId.toUpperCase(), "");
    }

    public String getDonationInfo() {
        return config.getString("discord.donation_info", "");
    }

    /**
     * Discord設定を更新して保存
     */
    public void setDiscordSetting(String key, Object value) {
        config.set("discord." + key, value);
        plugin.saveConfig();
    }

    /**
     * 階級ロール設定を更新
     */
    public void setDiscordRankRole(String rankId, String roleId) {
        config.set("discord.rank_roles." + rankId.toUpperCase(), roleId);
        plugin.saveConfig();
    }

    // ===== Utility =====

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
