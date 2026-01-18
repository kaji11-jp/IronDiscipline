package com.irondiscipline.manager;

import com.irondiscipline.IronDiscipline;
import com.irondiscipline.model.Rank;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.awt.Color;
import java.util.UUID;

/**
 * Discord Bot マネージャー
 */
public class DiscordManager extends ListenerAdapter {

    private final IronDiscipline plugin;
    private JDA jda;
    private String notificationChannelId;
    private String guildId;
    private String unverifiedRoleId;
    private String verifiedRoleId;
    private boolean enabled = false;
    
    // 寄付システム
    private int donationGoal = 5000;  // 月間目標（円）
    private int donationCurrent = 0;  // 現在の寄付額
    private String donationInfo = "";  // 寄付先情報

    public DiscordManager(IronDiscipline plugin) {
        this.plugin = plugin;
    }

    /**
     * Botを起動
     */
    public boolean start(String botToken, String channelId, String guildId, String unverifiedRoleId, String verifiedRoleId) {
        if (botToken == null || botToken.isEmpty()) {
            plugin.getLogger().warning("Discord Bot Token が設定されていません");
            return false;
        }

        this.notificationChannelId = channelId;
        this.guildId = guildId;
        this.unverifiedRoleId = unverifiedRoleId;
        this.verifiedRoleId = verifiedRoleId;

        try {
            jda = JDABuilder.createDefault(botToken)
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .setActivity(Activity.playing("鉄の規律"))
                .addEventListeners(this)
                .build();

            // コマンド登録
            jda.updateCommands().addCommands(
                Commands.slash("link", "Minecraftアカウントと連携"),
                Commands.slash("unlink", "連携を解除"),
                Commands.slash("status", "サーバー状態を表示"),
                Commands.slash("players", "オンラインプレイヤー一覧"),
                Commands.slash("playtime", "勤務時間を確認"),
                Commands.slash("rank", "自分の階級を確認"),
                Commands.slash("warn", "プレイヤーに警告")
                    .addOption(OptionType.USER, "user", "対象ユーザー", true)
                    .addOption(OptionType.STRING, "reason", "理由", true),
                Commands.slash("announce", "ゲーム内アナウンス")
                    .addOption(OptionType.STRING, "message", "メッセージ", true),
                Commands.slash("donate", "サーバー運営費の寄付情報"),
                Commands.slash("setgoal", "寄付目標を設定（管理者）")
                    .addOption(OptionType.INTEGER, "goal", "月間目標金額（円）", true)
                    .addOption(OptionType.INTEGER, "current", "現在の寄付額（円）", true)
            ).queue();

            enabled = true;
            plugin.getLogger().info("Discord Bot 起動成功");
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Discord Bot 起動失敗: " + e.getMessage());
            return false;
        }
    }

    /**
     * Botを停止
     */
    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
            plugin.getLogger().info("Discord Bot 停止");
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String cmd = event.getName();

        switch (cmd) {
            case "link" -> handleLink(event);
            case "unlink" -> handleUnlink(event);
            case "status" -> handleStatus(event);
            case "players" -> handlePlayers(event);
            case "playtime" -> handlePlaytime(event);
            case "rank" -> handleRank(event);
            case "warn" -> handleWarn(event);
            case "announce" -> handleAnnounce(event);
            case "donate" -> handleDonate(event);
            case "setgoal" -> handleSetGoal(event);
        }
    }

    private void handleLink(SlashCommandInteractionEvent event) {
        long discordId = event.getUser().getIdLong();
        
        if (plugin.getLinkManager().isLinked(discordId)) {
            event.reply("既に連携済みです。解除するには `/unlink` を使用してください。").setEphemeral(true).queue();
            return;
        }

        String code = plugin.getLinkManager().generateLinkCode(discordId);
        
        EmbedBuilder eb = new EmbedBuilder()
            .setTitle("🔗 アカウント連携")
            .setDescription("Minecraft内で以下のコマンドを実行してください：")
            .addField("コマンド", "`/link " + code + "`", false)
            .addField("有効期限", "5分", false)
            .setColor(Color.BLUE)
            .setFooter("鉄の規律");

        event.replyEmbeds(eb.build()).setEphemeral(true).queue();
    }

    private void handleUnlink(SlashCommandInteractionEvent event) {
        long discordId = event.getUser().getIdLong();
        
        if (plugin.getLinkManager().unlinkByDiscord(discordId)) {
            event.reply("✅ 連携を解除しました。").setEphemeral(true).queue();
        } else {
            event.reply("連携されていません。").setEphemeral(true).queue();
        }
    }

    private void handleStatus(SlashCommandInteractionEvent event) {
        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();
        int linked = plugin.getLinkManager().getLinkCount();

        EmbedBuilder eb = new EmbedBuilder()
            .setTitle("📊 サーバー状態")
            .addField("オンライン", online + " / " + max, true)
            .addField("連携済み", linked + "人", true)
            .setColor(Color.GREEN)
            .setFooter("鉄の規律");

        event.replyEmbeds(eb.build()).queue();
    }

    private void handlePlayers(SlashCommandInteractionEvent event) {
        StringBuilder sb = new StringBuilder();
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            Rank rank = plugin.getRankManager().getRank(p);
            String div = plugin.getDivisionManager().getDivision(p.getUniqueId());
            String divDisplay = div != null ? plugin.getDivisionManager().getDivisionDisplayName(div) : "";
            
            sb.append("**").append(p.getName()).append("** - ")
              .append(rank.getId()).append(" ").append(divDisplay).append("\n");
        }

        if (sb.length() == 0) {
            sb.append("オンラインプレイヤーなし");
        }

        EmbedBuilder eb = new EmbedBuilder()
            .setTitle("👥 オンラインプレイヤー")
            .setDescription(sb.toString())
            .setColor(Color.CYAN)
            .setFooter("鉄の規律");

        event.replyEmbeds(eb.build()).queue();
    }

    private void handlePlaytime(SlashCommandInteractionEvent event) {
        long discordId = event.getUser().getIdLong();
        UUID minecraftId = plugin.getLinkManager().getMinecraftId(discordId);

        if (minecraftId == null) {
            event.reply("アカウントが連携されていません。`/link` で連携してください。").setEphemeral(true).queue();
            return;
        }

        String playtime = plugin.getPlaytimeManager().getFormattedPlaytime(minecraftId);
        String playerName = Bukkit.getOfflinePlayer(minecraftId).getName();

        EmbedBuilder eb = new EmbedBuilder()
            .setTitle("⏱️ 勤務時間")
            .addField(playerName != null ? playerName : "Unknown", playtime, false)
            .setColor(Color.ORANGE)
            .setFooter("鉄の規律");

        event.replyEmbeds(eb.build()).setEphemeral(true).queue();
    }

    private void handleRank(SlashCommandInteractionEvent event) {
        long discordId = event.getUser().getIdLong();
        UUID minecraftId = plugin.getLinkManager().getMinecraftId(discordId);

        if (minecraftId == null) {
            event.reply("アカウントが連携されていません。").setEphemeral(true).queue();
            return;
        }

        Player player = Bukkit.getPlayer(minecraftId);
        Rank rank = player != null ? plugin.getRankManager().getRank(player) : Rank.PRIVATE;
        String div = plugin.getDivisionManager().getDivision(minecraftId);

        EmbedBuilder eb = new EmbedBuilder()
            .setTitle("🎖️ 階級情報")
            .addField("階級", rank.getId(), true)
            .addField("部隊", div != null ? div : "なし", true)
            .setColor(Color.YELLOW)
            .setFooter("鉄の規律");

        event.replyEmbeds(eb.build()).setEphemeral(true).queue();
    }

    private void handleWarn(SlashCommandInteractionEvent event) {
        var targetOption = event.getOption("user");
        var reasonOption = event.getOption("reason");

        if (targetOption == null || reasonOption == null) {
            event.reply("パラメータが不足しています。").setEphemeral(true).queue();
            return;
        }

        long targetDiscordId = targetOption.getAsUser().getIdLong();
        String reason = reasonOption.getAsString();

        UUID targetMinecraft = plugin.getLinkManager().getMinecraftId(targetDiscordId);
        if (targetMinecraft == null) {
            event.reply("対象ユーザーはMinecraftと連携されていません。").setEphemeral(true).queue();
            return;
        }

        Player target = Bukkit.getPlayer(targetMinecraft);
        if (target == null || !target.isOnline()) {
            event.reply("対象プレイヤーはオフラインです。").setEphemeral(true).queue();
            return;
        }

        // 警告実行
        Bukkit.getScheduler().runTask(plugin, () -> {
            int count = plugin.getWarningManager().addWarning(targetMinecraft, target.getName(), reason, null);
            target.sendMessage("§c§l【警告】§r§c " + reason + " §7(警告" + count + "回目)");
        });

        event.reply("✅ " + target.getName() + " に警告を与えました。理由: " + reason).queue();
    }

    private void handleAnnounce(SlashCommandInteractionEvent event) {
        var msgOption = event.getOption("message");
        if (msgOption == null) return;

        String message = msgOption.getAsString();

        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendTitle("§6§l【通達】", "§f" + message, 10, 100, 20);
                p.sendMessage("§6§l【Discord通達】§r §f" + message);
            }
        });

        event.reply("✅ アナウンスを送信しました: " + message).queue();
    }

    private void handleDonate(SlashCommandInteractionEvent event) {
        int percent = donationGoal > 0 ? (donationCurrent * 100 / donationGoal) : 0;
        if (percent > 100) percent = 100;
        
        // プログレスバー生成
        int bars = 20;
        int filled = (percent * bars) / 100;
        StringBuilder progressBar = new StringBuilder();
        for (int i = 0; i < bars; i++) {
            progressBar.append(i < filled ? "█" : "░");
        }

        EmbedBuilder eb = new EmbedBuilder()
            .setTitle("💰 サーバー運営費")
            .setDescription("サーバー維持のためのご支援をお願いします！")
            .addField("月間目標", "¥" + String.format("%,d", donationGoal), true)
            .addField("現在の達成額", "¥" + String.format("%,d", donationCurrent), true)
            .addField("達成率", percent + "%", true)
            .addField("進捗", "`" + progressBar.toString() + "` " + percent + "%", false)
            .setColor(percent >= 100 ? Color.GREEN : (percent >= 50 ? Color.YELLOW : Color.RED))
            .setFooter("ご支援ありがとうございます！");

        // 寄付先情報があれば追加
        String info = plugin.getConfigManager().getDonationInfo();
        if (info != null && !info.isEmpty()) {
            eb.addField("寄付方法", info, false);
        }

        event.replyEmbeds(eb.build()).queue();
    }

    private void handleSetGoal(SlashCommandInteractionEvent event) {
        // 管理者権限チェック
        if (event.getMember() == null || !event.getMember().hasPermission(net.dv8tion.jda.api.Permission.ADMINISTRATOR)) {
            event.reply("❌ このコマンドは管理者のみ使用可能です。").setEphemeral(true).queue();
            return;
        }

        var goalOption = event.getOption("goal");
        var currentOption = event.getOption("current");

        if (goalOption == null || currentOption == null) {
            event.reply("パラメータが不足しています。").setEphemeral(true).queue();
            return;
        }

        donationGoal = goalOption.getAsInt();
        donationCurrent = currentOption.getAsInt();

        int percent = donationGoal > 0 ? (donationCurrent * 100 / donationGoal) : 0;

        event.reply("✅ 寄付目標を更新しました！\n目標: ¥" + String.format("%,d", donationGoal) + 
            " / 現在: ¥" + String.format("%,d", donationCurrent) + " (" + percent + "%)").queue();
    }

    // ===== 通知機能 =====

    /**
     * 通知チャンネルにメッセージ送信
     */
    public void sendNotification(String title, String message, Color color) {
        if (!enabled || jda == null || notificationChannelId == null || notificationChannelId.isEmpty()) {
            return;
        }

        TextChannel channel = jda.getTextChannelById(notificationChannelId);
        if (channel == null) return;

        EmbedBuilder eb = new EmbedBuilder()
            .setTitle(title)
            .setDescription(message)
            .setColor(color)
            .setTimestamp(java.time.Instant.now())
            .setFooter("鉄の規律");

        channel.sendMessageEmbeds(eb.build()).queue();
    }

    public void notifyJoin(Player player) {
        sendNotification("📥 参加", "**" + player.getName() + "** がサーバーに参加しました", Color.GREEN);
    }

    public void notifyQuit(Player player) {
        sendNotification("📤 退出", "**" + player.getName() + "** がサーバーから退出しました", Color.GRAY);
    }

    public void notifyWarning(String playerName, String reason, int count) {
        sendNotification("⚠️ 警告", "**" + playerName + "** に警告 (" + count + "回目)\n理由: " + reason, Color.ORANGE);
    }

    public void notifyJail(String playerName, String reason) {
        sendNotification("🔒 隔離", "**" + playerName + "** が隔離されました\n理由: " + reason, Color.RED);
    }

    public void notifyUnjail(String playerName) {
        sendNotification("🔓 釈放", "**" + playerName + "** が釈放されました", Color.GREEN);
    }

    public boolean isEnabled() {
        return enabled;
    }

    // ===== ロール管理 =====

    /**
     * Discordサーバーに参加した時に未認証ロールを付与
     */
    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (unverifiedRoleId == null || unverifiedRoleId.isEmpty()) return;

        Role unverifiedRole = event.getGuild().getRoleById(unverifiedRoleId);
        if (unverifiedRole != null) {
            event.getGuild().addRoleToMember(event.getMember(), unverifiedRole).queue();
            plugin.getLogger().info("Discord: " + event.getUser().getName() + " に未認証ロールを付与");
        }
    }

    /**
     * 連携完了時に認証済みロールを付与し、ニックネームを変更
     */
    public void onLinkComplete(long discordId, String minecraftName, Rank rank) {
        if (!enabled || jda == null || guildId == null || guildId.isEmpty()) return;

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) return;

        guild.retrieveMemberById(discordId).queue(member -> {
            if (member == null) return;

            // 未認証ロールを削除
            if (unverifiedRoleId != null && !unverifiedRoleId.isEmpty()) {
                Role unverifiedRole = guild.getRoleById(unverifiedRoleId);
                if (unverifiedRole != null) {
                    guild.removeRoleFromMember(member, unverifiedRole).queue();
                }
            }

            // 認証済みロールを付与
            if (verifiedRoleId != null && !verifiedRoleId.isEmpty()) {
                Role verifiedRole = guild.getRoleById(verifiedRoleId);
                if (verifiedRole != null) {
                    guild.addRoleToMember(member, verifiedRole).queue();
                }
            }

            // ニックネーム変更 [階級]MinecraftName
            String nickname = "[" + rank.getId() + "]" + minecraftName;
            if (nickname.length() > 32) {
                nickname = nickname.substring(0, 32);
            }
            member.modifyNickname(nickname).queue(
                success -> plugin.getLogger().info("Discord: " + minecraftName + " のニックネームを変更"),
                error -> plugin.getLogger().warning("Discord: ニックネーム変更失敗: " + error.getMessage())
            );

        }, error -> {});
    }

    /**
     * 階級変更時にニックネームを更新
     */
    public void updateNickname(long discordId, String minecraftName, Rank rank) {
        if (!enabled || jda == null || guildId == null || guildId.isEmpty()) return;

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) return;

        guild.retrieveMemberById(discordId).queue(member -> {
            if (member == null) return;

            String nickname = "[" + rank.getId() + "]" + minecraftName;
            if (nickname.length() > 32) {
                nickname = nickname.substring(0, 32);
            }
            member.modifyNickname(nickname).queue();
        }, error -> {});
    }

    /**
     * 連携解除時にロールとニックネームをリセット
     */
    public void onUnlink(long discordId) {
        if (!enabled || jda == null || guildId == null || guildId.isEmpty()) return;

        Guild guild = jda.getGuildById(guildId);
        if (guild == null) return;

        guild.retrieveMemberById(discordId).queue(member -> {
            if (member == null) return;

            // 認証済みロールを削除
            if (verifiedRoleId != null && !verifiedRoleId.isEmpty()) {
                Role verifiedRole = guild.getRoleById(verifiedRoleId);
                if (verifiedRole != null) {
                    guild.removeRoleFromMember(member, verifiedRole).queue();
                }
            }

            // 未認証ロールを付与
            if (unverifiedRoleId != null && !unverifiedRoleId.isEmpty()) {
                Role unverifiedRole = guild.getRoleById(unverifiedRoleId);
                if (unverifiedRole != null) {
                    guild.addRoleToMember(member, unverifiedRole).queue();
                }
            }

            // ニックネームをリセット
            member.modifyNickname(null).queue();
        }, error -> {});
    }
}
