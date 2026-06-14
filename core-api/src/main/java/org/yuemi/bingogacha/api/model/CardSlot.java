package org.yuemi.bingogacha.api.model;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CardSlot {

    private final int index;
    private final String material;
    private final String name;
    private final List<String> lore;
    private final Integer customModelData;

    public CardSlot(
            int index,
            @NotNull String material,
            @Nullable String name,
            @Nullable List<String> lore,
            @Nullable Integer customModelData
    ) {
        this.index = index;
        this.material = material;
        this.name = name;
        this.lore = lore;
        this.customModelData = customModelData;
    }

    public int getIndex() {
        return index;
    }

    @NotNull
    public String getMaterial() {
        return material;
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public List<String> getLore() {
        return lore;
    }

    @Nullable
    public Integer getCustomModelData() {
        return customModelData;
    }
}
