package org.yuemi.bingogacha.api.reward;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface Reward {

    /**
     * Gives the reward to the player.
     *
     * @param player the player who is receiving the reward
     */
    void give(@NotNull Player player);
}
