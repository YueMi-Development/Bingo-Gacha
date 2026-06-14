package org.yuemi.bingogacha.api.reward;

import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface RewardManager {

    /**
     * Registers a new reward parser for the specified type.
     *
     * @param type   the type identifier (e.g. "command", "economy")
     * @param parser the parser implementation
     */
    void registerParser(@NotNull String type, @NotNull RewardParser parser);

    /**
     * Parses a single reward from a configuration map.
     *
     * @param configMap the config map
     * @return the parsed Reward, or null if parsing failed or type is unregistered
     */
    @Nullable
    Reward parseReward(@NotNull Map<String, Object> configMap);

    /**
     * Parses a list of rewards from a configuration list.
     *
     * @param configList the list of reward configurations
     * @return a list of parsed Rewards
     */
    @NotNull
    List<Reward> parseRewards(@NotNull List<?> configList);
}
