package org.yuemi.bingogacha.plugin.hook;

import java.util.List;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yuemi.bingogacha.api.model.PlayerCard;
import org.yuemi.bingogacha.api.repository.PlayerCardRepositoryInterface;

public class PlaceholderApiHook extends PlaceholderExpansion {

    private final JavaPlugin plugin;
    private final PlayerCardRepositoryInterface repository;

    public PlaceholderApiHook(@NotNull JavaPlugin plugin, @NotNull PlayerCardRepositoryInterface repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "bingogacha";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().isEmpty() ? "YueMi" : plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        List<PlayerCard> cards = repository.loadPlayerCards(player.getUniqueId());

        if (params.equalsIgnoreCase("active_cards")) {
            long activeCount = cards.stream().filter(c -> !c.isCompleted()).count();
            return String.valueOf(activeCount);
        }

        if (params.equalsIgnoreCase("completed_cards")) {
            long completedCount = cards.stream().filter(PlayerCard::isCompleted).count();
            return String.valueOf(completedCount);
        }

        return null;
    }
}
