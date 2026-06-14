package org.yuemi.bingogacha.api.reward;

import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface RewardParser {

    /**
     * Parses a reward from the configuration map.
     *
     * @param configMap the config map containing reward details
     * @return the parsed Reward, or null if parsing failed
     */
    @Nullable
    Reward parse(@NotNull Map<String, Object> configMap);
}
