package org.yuemi.bingogacha.api.model;

import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yuemi.bingogacha.api.reward.Reward;

public class CardTemplate {

    private final String id;
    private final String title;
    private final CardSize size;
    private final CompletionType completionType;
    private final boolean repeatable;
    private final Map<String, Object> rollCost;
    private final List<Reward> rewards;
    private final List<Reward> unusedPointRewards;
    private final Map<Integer, CardSlot> slots;
    private final Map<String, Object> displayOverrides;

    public CardTemplate(
            @NotNull String id,
            @NotNull String title,
            @NotNull CardSize size,
            @NotNull CompletionType completionType,
            boolean repeatable,
            @NotNull Map<String, Object> rollCost,
            @NotNull List<Reward> rewards,
            @NotNull List<Reward> unusedPointRewards,
            @NotNull Map<Integer, CardSlot> slots,
            @NotNull Map<String, Object> displayOverrides
    ) {
        this.id = id;
        this.title = title;
        this.size = size;
        this.completionType = completionType;
        this.repeatable = repeatable;
        this.rollCost = rollCost;
        this.rewards = rewards;
        this.unusedPointRewards = unusedPointRewards;
        this.slots = slots;
        this.displayOverrides = displayOverrides;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getTitle() {
        return title;
    }

    @NotNull
    public CardSize getSize() {
        return size;
    }

    @NotNull
    public CompletionType getCompletionType() {
        return completionType;
    }

    public boolean isRepeatable() {
        return repeatable;
    }

    @NotNull
    public Map<String, Object> getRollCost() {
        return rollCost;
    }

    @NotNull
    public List<Reward> getRewards() {
        return rewards;
    }

    @NotNull
    public List<Reward> getUnusedPointRewards() {
        return unusedPointRewards;
    }

    @NotNull
    public Map<Integer, CardSlot> getSlots() {
        return slots;
    }

    @Nullable
    public CardSlot getSlot(int index) {
        return slots.get(index);
    }

    @NotNull
    public Map<String, Object> getDisplayOverrides() {
        return displayOverrides;
    }
}
