package org.yuemi.bingogacha.plugin.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.yuemi.bingogacha.api.model.PlayerCard;
import org.yuemi.bingogacha.api.repository.PlayerCardRepositoryInterface;

public class PlayerCardRepository implements PlayerCardRepositoryInterface {

    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;
    private final Map<UUID, List<PlayerCard>> cache = new ConcurrentHashMap<>();

    public PlayerCardRepository(@NotNull JavaPlugin plugin, @NotNull DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public void loadIntoCache(@NotNull UUID playerUuid) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            List<PlayerCard> cards = queryPlayerCards(playerUuid);
            cache.put(playerUuid, cards);
        });
    }

    public void invalidateCache(@NotNull UUID playerUuid) {
        cache.remove(playerUuid);
    }

    @NotNull
    private List<PlayerCard> queryPlayerCards(@NotNull UUID playerUuid) {
        List<PlayerCard> list = new ArrayList<>();
        String sql = "SELECT * FROM bingo_player_cards WHERE player_uuid = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String templateId = rs.getString("template_id");
                    boolean completed = rs.getBoolean("completed");
                    String slotsStr = rs.getString("unlocked_slots");
                    long createdAt = rs.getLong("created_at");
                    long completedAt = rs.getLong("completed_at");

                    Set<Integer> unlockedSlots = new HashSet<>();
                    if (slotsStr != null && !slotsStr.trim().isEmpty()) {
                        for (String part : slotsStr.split(",")) {
                            try {
                                unlockedSlots.add(Integer.parseInt(part.trim()));
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }

                    list.add(new PlayerCard(id, playerUuid, templateId, unlockedSlots, completed, createdAt, completedAt));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error loading player cards for " + playerUuid + ": " + e.getMessage());
            e.printStackTrace();
        }
        return list;
    }

    @NotNull
    @Override
    public List<PlayerCard> loadPlayerCards(@NotNull UUID playerUuid) {
        return cache.computeIfAbsent(playerUuid, this::queryPlayerCards);
    }

    @Override
    public void savePlayerCard(@NotNull PlayerCard card) {
        List<PlayerCard> list = cache.computeIfAbsent(card.getPlayerUuid(), k -> new ArrayList<>());
        if (!list.contains(card)) {
            list.add(card);
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "INSERT INTO bingo_player_cards (player_uuid, template_id, completed, unlocked_slots, created_at, completed_at) VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                
                ps.setString(1, card.getPlayerUuid().toString());
                ps.setString(2, card.getTemplateId());
                ps.setBoolean(3, card.isCompleted());
                ps.setString(4, card.getUnlockedSlots().stream().map(String::valueOf).collect(Collectors.joining(",")));
                ps.setLong(5, card.getCreatedAt());
                ps.setLong(6, card.getCompletedAt());
                
                ps.executeUpdate();
                
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        card.setId(rs.getInt(1));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error saving player card: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public void updatePlayerCard(@NotNull PlayerCard card) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "UPDATE bingo_player_cards SET completed = ?, unlocked_slots = ?, completed_at = ? WHERE id = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                
                ps.setBoolean(1, card.isCompleted());
                ps.setString(2, card.getUnlockedSlots().stream().map(String::valueOf).collect(Collectors.joining(",")));
                ps.setLong(3, card.getCompletedAt());
                ps.setInt(4, card.getId());
                
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error updating player card ID " + card.getId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @Override
    public void deletePlayerCard(int cardId) {
        for (List<PlayerCard> list : cache.values()) {
            list.removeIf(c -> c.getId() == cardId);
        }

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = "DELETE FROM bingo_player_cards WHERE id = ?";
            try (Connection conn = databaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                
                ps.setInt(1, cardId);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error deleting player card ID " + cardId + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
