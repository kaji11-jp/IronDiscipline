package com.irondiscipline.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 無線チャンネル
 */
public class RadioChannel {

    private final String frequency;
    private final Set<UUID> members;

    public RadioChannel(String frequency) {
        this.frequency = frequency;
        this.members = new HashSet<>();
    }

    public String getFrequency() {
        return frequency;
    }

    public Set<UUID> getMembers() {
        return new HashSet<>(members);
    }

    public void addMember(UUID playerId) {
        members.add(playerId);
    }

    public void removeMember(UUID playerId) {
        members.remove(playerId);
    }

    public boolean hasMember(UUID playerId) {
        return members.contains(playerId);
    }

    public int getMemberCount() {
        return members.size();
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }
}
