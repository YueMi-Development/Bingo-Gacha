package org.yuemi.bingogacha.plugin;

import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yuemi.bingogacha.api.BingoGachaApi;
import org.yuemi.bingogacha.api.model.CardTemplate;
import org.yuemi.bingogacha.api.repository.PlayerCardRepositoryInterface;
import org.yuemi.bingogacha.api.reward.RewardManager;
import org.yuemi.bingogacha.plugin.config.BingoConfig;

public class BingoGachaApiImpl implements BingoGachaApi {

    private final RewardManager rewardManager;
    private final PlayerCardRepositoryInterface playerCardRepository;
    private final BingoConfig config;

    public BingoGachaApiImpl(
            @NotNull RewardManager rewardManager,
            @NotNull PlayerCardRepositoryInterface playerCardRepository,
            @NotNull BingoConfig config
    ) {
        this.rewardManager = rewardManager;
        this.playerCardRepository = playerCardRepository;
        this.config = config;
    }

    @Override
    public @NotNull RewardManager getRewardManager() {
        return rewardManager;
    }

    @Override
    public @NotNull PlayerCardRepositoryInterface getPlayerCardRepository() {
        return playerCardRepository;
    }

    @Override
    public @Nullable CardTemplate getTemplate(@NotNull String id) {
        return config.getTemplate(id);
    }

    @Override
    public @NotNull Collection<CardTemplate> getTemplates() {
        return config.getTemplates().values();
    }
}
