package org.yuemi.bingogacha.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BingoGachaApiProvider {

    private static BingoGachaApi api;

    private BingoGachaApiProvider() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    @Nullable
    public static BingoGachaApi getApi() {
        return api;
    }

    public static void register(@NotNull BingoGachaApi apiInstance) {
        if (api != null) {
            throw new IllegalStateException("API is already registered");
        }
        api = apiInstance;
    }

    public static void unregister() {
        api = null;
    }
}
