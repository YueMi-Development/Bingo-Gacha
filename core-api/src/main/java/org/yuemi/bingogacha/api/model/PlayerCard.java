package org.yuemi.bingogacha.api.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public class PlayerCard {

    private int id;
    private final UUID playerUuid;
    private final String templateId;
    private final Set<Integer> unlockedSlots;
    private boolean completed;
    private final long createdAt;
    private long completedAt;

    public PlayerCard(
            int id,
            @NotNull UUID playerUuid,
            @NotNull String templateId,
            @NotNull Set<Integer> unlockedSlots,
            boolean completed,
            long createdAt,
            long completedAt
    ) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.templateId = templateId;
        this.unlockedSlots = new HashSet<>(unlockedSlots);
        this.completed = completed;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @NotNull
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    @NotNull
    public String getTemplateId() {
        return templateId;
    }

    @NotNull
    public Set<Integer> getUnlockedSlots() {
        return unlockedSlots;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
    }

    public void unlockSlot(int slotIndex) {
        this.unlockedSlots.add(slotIndex);
    }

    public boolean isSlotUnlocked(int slotIndex) {
        return this.unlockedSlots.contains(slotIndex);
    }
}
