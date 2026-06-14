package org.yuemi.bingogacha.api;

import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yuemi.bingogacha.api.model.CardTemplate;
import org.yuemi.bingogacha.api.repository.PlayerCardRepositoryInterface;
import org.yuemi.bingogacha.api.reward.RewardManager;

public interface BingoGachaApi {

    /**
     * @return the active reward manager
     */
    @NotNull
    RewardManager getRewardManager();

    /**
     * @return the active player card repository
     */
    @NotNull
    PlayerCardRepositoryInterface getPlayerCardRepository();

    /**
     * Retrieves a card template by its ID.
     *
     * @param id the template ID
     * @return the template, or null if not found
     */
    @Nullable
    CardTemplate getTemplate(@NotNull String id);

    /**
     * @return a collection of all loaded card templates
     */
    @NotNull
    Collection<CardTemplate> getTemplates();
}
