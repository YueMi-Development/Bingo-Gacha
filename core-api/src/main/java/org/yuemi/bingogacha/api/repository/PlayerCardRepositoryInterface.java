package org.yuemi.bingogacha.api.repository;

import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.yuemi.bingogacha.api.model.PlayerCard;

public interface PlayerCardRepositoryInterface {

    /**
     * Loads all active or completed player cards for the given player.
     *
     * @param playerUuid the player's UUID
     * @return a list of player cards
     */
    @NotNull
    List<PlayerCard> loadPlayerCards(@NotNull UUID playerUuid);

    /**
     * Saves a new player card to the storage and sets its database ID.
     *
     * @param card the player card to save
     */
    void savePlayerCard(@NotNull PlayerCard card);

    /**
     * Updates an existing player card's progress (unlocked slots, completion status) in storage.
     *
     * @param card the player card to update
     */
    void updatePlayerCard(@NotNull PlayerCard card);

    /**
     * Deletes a player card from storage.
     *
     * @param cardId the card's database ID
     */
    void deletePlayerCard(int cardId);
}
