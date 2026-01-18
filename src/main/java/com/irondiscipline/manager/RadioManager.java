package com.irondiscipline.manager;

import com.irondiscipline.IronDiscipline;
import com.irondiscipline.model.RadioChannel;
import com.irondiscipline.model.Rank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 無線マネージャー
 * 周波数ベースの限定通信システム
 */
public class RadioManager {

    private final IronDiscipline plugin;
    
    // 周波数チャンネル (周波数 -> チャンネル)
    private final Map<String, RadioChannel> channels = new ConcurrentHashMap<>();
    
    // プレイヤーの現在周波数 (UUID -> 周波数)
    private final Map<UUID, String> playerFrequencies = new ConcurrentHashMap<>();

    public RadioManager(IronDiscipline plugin) {
        this.plugin = plugin;
    }

    /**
     * プレイヤーを周波数に参加させる
     */
    public void joinFrequency(Player player, String frequency) {
        // 正規化（数値フォーマット）
        frequency = normalizeFrequency(frequency);
        
        UUID playerId = player.getUniqueId();
        
        // 既に別の周波数にいる場合は離脱
        String currentFreq = playerFrequencies.get(playerId);
        if (currentFreq != null) {
            leaveFrequency(player);
        }
        
        // チャンネル取得または作成
        RadioChannel channel = channels.computeIfAbsent(frequency, RadioChannel::new);
        channel.addMember(playerId);
        playerFrequencies.put(playerId, frequency);
        
        // 参加通知
        player.sendMessage(plugin.getConfigManager().getMessage("radio_joined",
            "%freq%", frequency));
        
        // サウンド
        player.playSound(player.getLocation(), 
            org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.5f);
    }

    /**
     * プレイヤーを周波数から離脱させる
     */
    public void leaveFrequency(Player player) {
        UUID playerId = player.getUniqueId();
        String frequency = playerFrequencies.remove(playerId);
        
        if (frequency != null) {
            RadioChannel channel = channels.get(frequency);
            if (channel != null) {
                channel.removeMember(playerId);
                
                // 空チャンネルを削除
                if (channel.isEmpty()) {
                    channels.remove(frequency);
                }
            }
            
            player.sendMessage(plugin.getConfigManager().getMessage("radio_left"));
        }
    }

    /**
     * 無線メッセージを送信
     */
    public void broadcast(Player sender, String message) {
        UUID senderId = sender.getUniqueId();
        String frequency = playerFrequencies.get(senderId);
        
        if (frequency == null) {
            sender.sendMessage(ChatColor.RED + "無線に参加していない。/radio <周波数> で参加せよ。");
            return;
        }
        
        RadioChannel channel = channels.get(frequency);
        if (channel == null) {
            return;
        }
        
        // 送信者の階級を取得
        Rank senderRank = plugin.getRankManager().getRank(sender);
        
        // フォーマット組み立て
        String format = plugin.getConfigManager().getRadioFormat();
        String formattedMessage = ChatColor.translateAlternateColorCodes('&', format
            .replace("%freq%", frequency)
            .replace("%rank%", senderRank.getDisplay())
            .replace("%player%", sender.getName())
            .replace("%message%", message));
        
        // 同じ周波数の全員に送信
        for (UUID memberId : channel.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) {
                member.sendMessage(formattedMessage);
                
                // 無線ノイズサウンド
                member.playSound(member.getLocation(),
                    org.bukkit.Sound.BLOCK_NOTE_BLOCK_BIT, 0.3f, 0.5f);
            }
        }
    }

    /**
     * プレイヤーの現在周波数を取得
     */
    public String getPlayerFrequency(UUID playerId) {
        return playerFrequencies.get(playerId);
    }

    /**
     * 周波数にいるかどうか
     */
    public boolean isInFrequency(UUID playerId) {
        return playerFrequencies.containsKey(playerId);
    }

    /**
     * 周波数の正規化
     */
    private String normalizeFrequency(String freq) {
        try {
            // 数値として解析し、小数点1桁に
            double value = Double.parseDouble(freq);
            return String.format("%.1f", value);
        } catch (NumberFormatException e) {
            return freq.toUpperCase(); // 数値でなければそのまま
        }
    }

    /**
     * プレイヤー退出時のクリーンアップ
     */
    public void cleanup(UUID playerId) {
        String frequency = playerFrequencies.remove(playerId);
        if (frequency != null) {
            RadioChannel channel = channels.get(frequency);
            if (channel != null) {
                channel.removeMember(playerId);
                if (channel.isEmpty()) {
                    channels.remove(frequency);
                }
            }
        }
    }

    /**
     * チャンネルのメンバー数を取得
     */
    public int getChannelMemberCount(String frequency) {
        RadioChannel channel = channels.get(normalizeFrequency(frequency));
        return channel != null ? channel.getMemberCount() : 0;
    }
}
