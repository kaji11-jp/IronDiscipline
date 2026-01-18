package com.irondiscipline;

import com.irondiscipline.command.*;
import com.irondiscipline.listener.*;
import com.irondiscipline.manager.*;
import com.irondiscipline.util.RankUtil;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * IronDiscipline (鉄の規律)
 * 軍事RP向け規律システムプラグイン
 */
public class IronDiscipline extends JavaPlugin {

    private static IronDiscipline instance;
    
    // Managers
    private ConfigManager configManager;
    private StorageManager storageManager;
    private RankManager rankManager;
    private PTSManager ptsManager;
    private JailManager jailManager;
    private RadioManager radioManager;
    private ExamManager examManager;
    private DivisionManager divisionManager;
    private WarningManager warningManager;
    private PlaytimeManager playtimeManager;
    private ExamQuestionManager examQuestionManager;
    private LinkManager linkManager;
    private DiscordManager discordManager;
    
    // Utilities
    private RankUtil rankUtil;
    
    // LuckPerms API
    private LuckPerms luckPerms;

    @Override
    public void onEnable() {
        instance = this;
        
        // バナー表示
        logBanner();
        
        // 設定読み込み
        saveDefaultConfig();
        this.configManager = new ConfigManager(this);
        
        // LuckPerms連携
        if (!hookLuckPerms()) {
            getLogger().severe("LuckPermsが見つかりません！プラグインを無効化します。");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        
        // Manager初期化
        initializeManagers();
        
        // リスナー登録
        registerListeners();
        
        // コマンド登録
        registerCommands();
        
        getLogger().info(ChatColor.GREEN + "鉄の規律 v" + getDescription().getVersion() + " 起動完了！");
    }

    @Override
    public void onDisable() {
        // データ保存
        if (storageManager != null) {
            storageManager.shutdown();
        }
        if (jailManager != null) {
            jailManager.saveAll();
        }
        if (playtimeManager != null) {
            playtimeManager.saveAll();
        }
        if (discordManager != null) {
            discordManager.shutdown();
        }
        
        getLogger().info("鉄の規律 シャットダウン完了");
    }

    private void logBanner() {
        getLogger().info("========================================");
        getLogger().info("   鉄の規律 (IronDiscipline)");
        getLogger().info("   Military RP Discipline System");
        getLogger().info("   Version: " + getDescription().getVersion());
        getLogger().info("========================================");
    }

    private boolean hookLuckPerms() {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            return false;
        }
        try {
            luckPerms = LuckPermsProvider.get();
            getLogger().info("LuckPerms連携成功");
            return true;
        } catch (IllegalStateException e) {
            getLogger().log(Level.SEVERE, "LuckPerms API取得失敗", e);
            return false;
        }
    }

    private void initializeManagers() {
        this.storageManager = new StorageManager(this);
        this.rankManager = new RankManager(this, luckPerms);
        this.ptsManager = new PTSManager(this);
        this.jailManager = new JailManager(this);
        this.radioManager = new RadioManager(this);
        this.examManager = new ExamManager(this);
        this.divisionManager = new DivisionManager(this);
        this.warningManager = new WarningManager(this);
        this.playtimeManager = new PlaytimeManager(this);
        this.examQuestionManager = new ExamQuestionManager(this);
        this.linkManager = new LinkManager(this);
        this.discordManager = new DiscordManager(this);
        this.rankUtil = new RankUtil(this);
        
        // Discord Bot 起動
        initDiscord();
        
        getLogger().info("マネージャー初期化完了");
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CombatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new JoinQuitListener(this), this);
        Bukkit.getPluginManager().registerEvents(new GestureListener(this), this);
        
        getLogger().info("イベントリスナー登録完了");
    }

    private void registerCommands() {
        getCommand("promote").setExecutor(new PromoteCommand(this));
        getCommand("demote").setExecutor(new DemoteCommand(this));
        getCommand("grant").setExecutor(new GrantCommand(this));
        getCommand("radio").setExecutor(new RadioCommand(this));
        getCommand("radiobroadcast").setExecutor(new RadioBroadcastCommand(this));
        getCommand("jail").setExecutor(new JailCommand(this));
        getCommand("unjail").setExecutor(new UnjailCommand(this));
        getCommand("setjail").setExecutor(new SetJailCommand(this));
        getCommand("killlog").setExecutor(new KillLogCommand(this));
        getCommand("irondiscipline").setExecutor(new IronDisciplineCommand(this));
        
        ExamCommand examCmd = new ExamCommand(this);
        getCommand("exam").setExecutor(examCmd);
        getCommand("exam").setTabCompleter(examCmd);
        
        // Phase 3 commands
        WarnCommand warnCmd = new WarnCommand(this);
        getCommand("warn").setExecutor(warnCmd);
        getCommand("warn").setTabCompleter(warnCmd);
        getCommand("warnings").setExecutor(warnCmd);
        getCommand("warnings").setTabCompleter(warnCmd);
        getCommand("clearwarnings").setExecutor(warnCmd);
        getCommand("unwarn").setExecutor(warnCmd);
        
        PlaytimeCommand playtimeCmd = new PlaytimeCommand(this);
        getCommand("playtime").setExecutor(playtimeCmd);
        getCommand("playtime").setTabCompleter(playtimeCmd);
        
        DivisionCommand divCmd = new DivisionCommand(this);
        getCommand("division").setExecutor(divCmd);
        getCommand("division").setTabCompleter(divCmd);
        
        // Phase 4 - Discord連携
        getCommand("link").setExecutor(new LinkCommand(this));
        
        getLogger().info("コマンド登録完了");
    }

    private void initDiscord() {
        String botToken = configManager.getDiscordBotToken();
        String channelId = configManager.getDiscordNotificationChannel();
        String guildId = configManager.getDiscordGuildId();
        String unverifiedRoleId = configManager.getDiscordUnverifiedRoleId();
        String verifiedRoleId = configManager.getDiscordVerifiedRoleId();
        
        if (botToken != null && !botToken.isEmpty() && configManager.isDiscordEnabled()) {
            discordManager.start(botToken, channelId, guildId, unverifiedRoleId, verifiedRoleId);
        } else {
            getLogger().info("Discord連携は無効です（config.ymlで設定可能）");
        }
    }

    /**
     * 設定をリロードする
     */
    public void reload() {
        reloadConfig();
        configManager.reload();
        getLogger().info("設定リロード完了");
    }

    // ===== Getters =====
    
    public static IronDiscipline getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public RankManager getRankManager() {
        return rankManager;
    }

    public PTSManager getPTSManager() {
        return ptsManager;
    }

    public JailManager getJailManager() {
        return jailManager;
    }

    public RadioManager getRadioManager() {
        return radioManager;
    }

    public ExamManager getExamManager() {
        return examManager;
    }

    public RankUtil getRankUtil() {
        return rankUtil;
    }

    public DivisionManager getDivisionManager() {
        return divisionManager;
    }

    public WarningManager getWarningManager() {
        return warningManager;
    }

    public PlaytimeManager getPlaytimeManager() {
        return playtimeManager;
    }

    public ExamQuestionManager getExamQuestionManager() {
        return examQuestionManager;
    }

    public LinkManager getLinkManager() {
        return linkManager;
    }

    public DiscordManager getDiscordManager() {
        return discordManager;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }
}
