package org.yuemi.bingogacha.plugin.service;

import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yuemi.bingogacha.api.event.PlayerCardCompleteEvent;
import org.yuemi.bingogacha.api.model.*;
import org.yuemi.bingogacha.api.reward.Reward;
import org.yuemi.bingogacha.api.repository.PlayerCardRepositoryInterface;
import org.yuemi.bingogacha.plugin.config.BingoConfig;
import org.yuemi.bingogacha.plugin.hook.VaultHook;

public class BingoGachaService {

    private final JavaPlugin plugin;
    private final PlayerCardRepositoryInterface repository;
    private final VaultHook vaultHook;
    private final BingoConfig config;
    
    // Concurrent map to track player cooldown timestamps safely
    private final Map<UUID, Long> rollCooldowns = new java.util.concurrent.ConcurrentHashMap<>();

    // Mathematical representation of bingo lines
    private static final List<int[]> LINES_3X3 = List.of(
            // Rows
            new int[]{0, 1, 2}, new int[]{3, 4, 5}, new int[]{6, 7, 8},
            // Columns
            new int[]{0, 3, 6}, new int[]{1, 4, 7}, new int[]{2, 5, 8},
            // Diagonals
            new int[]{0, 4, 8}, new int[]{2, 4, 6}
    );

    private static final List<int[]> LINES_5X5 = List.of(
            // Rows
            new int[]{0, 1, 2, 3, 4}, new int[]{5, 6, 7, 8, 9}, new int[]{10, 11, 12, 13, 14}, new int[]{15, 16, 17, 18, 19}, new int[]{20, 21, 22, 23, 24},
            // Columns
            new int[]{0, 5, 10, 15, 20}, new int[]{1, 6, 11, 16, 21}, new int[]{2, 7, 12, 17, 22}, new int[]{3, 8, 13, 18, 23}, new int[]{4, 9, 14, 19, 24},
            // Diagonals
            new int[]{0, 6, 12, 18, 24}, new int[]{4, 8, 12, 16, 20}
    );

    public BingoGachaService(
            @NotNull JavaPlugin plugin,
            @NotNull PlayerCardRepositoryInterface repository,
            @NotNull VaultHook vaultHook,
            @NotNull BingoConfig config
    ) {
        this.plugin = plugin;
        this.repository = repository;
        this.vaultHook = vaultHook;
        this.config = config;
    }

    /**
     * Claims a new card template for the player.
     *
     * @param player   the player
     * @param template the card template
     * @return the newly created card, or null if cannot claim (e.g. non-repeatable template already claimed)
     */
    @Nullable
    public PlayerCard claimCard(@NotNull Player player, @NotNull CardTemplate template) {
        List<PlayerCard> currentCards = repository.loadPlayerCards(player.getUniqueId());
        
        // If template is not repeatable, check if they already have one
        if (!template.isRepeatable()) {
            boolean alreadyHas = currentCards.stream().anyMatch(c -> c.getTemplateId().equals(template.getId()));
            if (alreadyHas) {
                return null;
            }
        } else {
            // Even if repeatable, maybe check if they already have an UNCOMPLETED card of this template
            boolean hasActive = currentCards.stream().anyMatch(c -> c.getTemplateId().equals(template.getId()) && !c.isCompleted());
            if (hasActive) {
                return null;
            }
        }

        PlayerCard newCard = new PlayerCard(
                -1,
                player.getUniqueId(),
                template.getId(),
                new HashSet<>(),
                false,
                System.currentTimeMillis(),
                0
        );
        repository.savePlayerCard(newCard);
        return newCard;
    }

    /**
     * Rolls a random locked slot on the player's card.
     *
     * @param player   the player
     * @param card     the player's active card
     * @param template the template configuration of the card
     * @return roll result code:
     *         0 = Success
     *         1 = Card already completed
     *         2 = Not enough money/items to roll
     *         3 = No locked slots remaining (error state)
     */
    public int rollCard(@NotNull Player player, @NotNull PlayerCard card, @NotNull CardTemplate template) {
        if (card.isCompleted()) {
            return 1;
        }

        // Cooldown check
        long lastRoll = rollCooldowns.getOrDefault(player.getUniqueId(), 0L);
        long cooldownMs = config.getRollCooldownMs();
        if (System.currentTimeMillis() - lastRoll < cooldownMs) {
            return 4; // Code 4 = On cooldown
        }

        int totalSlotsCount = template.getSize().getTotalSlots();
        Set<Integer> unlocked = card.getUnlockedSlots();
        
        if (unlocked.size() >= totalSlotsCount) {
            return 3;
        }

        // Deduct roll cost
        if (!chargeCost(player, template)) {
            return 2;
        }

        // Record cooldown timestamp
        rollCooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        // Find locked slots
        List<Integer> locked = new ArrayList<>();
        for (int i = 0; i < totalSlotsCount; i++) {
            if (!unlocked.contains(i)) {
                locked.add(i);
            }
        }

        // Pick one randomly
        Random random = new Random();
        int rolledIndex = locked.get(random.nextInt(locked.size()));
        card.unlockSlot(rolledIndex);

        // Check completion
        boolean completed = checkCompletion(card.getUnlockedSlots(), template);
        if (completed) {
            card.setCompleted(true);
            card.setCompletedAt(System.currentTimeMillis());
            
            // Give main completion rewards
            for (Reward reward : template.getRewards()) {
                reward.give(player);
            }

            // If completion type is STRAIGHT_LINE, give unused point rewards
            if (template.getCompletionType() == CompletionType.STRAIGHT_LINE) {
                int unusedCount = totalSlotsCount - card.getUnlockedSlots().size();
                for (int i = 0; i < unusedCount; i++) {
                    for (Reward r : template.getUnusedPointRewards()) {
                        r.give(player);
                    }
                }
            }

            // Call Bukkit custom event
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                Bukkit.getPluginManager().callEvent(new PlayerCardCompleteEvent(player, card, template));
            });
        }

        // Save progress
        repository.updatePlayerCard(card);
        return 0; // Success
    }

    private boolean checkCompletion(@NotNull Set<Integer> unlocked, @NotNull CardTemplate template) {
        if (template.getCompletionType() == CompletionType.FULL_CARD) {
            return unlocked.size() >= template.getSize().getTotalSlots();
        }

        // STRAIGHT_LINE check
        List<int[]> lines = template.getSize() == CardSize.FIVE_BY_FIVE ? LINES_5X5 : LINES_3X3;
        for (int[] line : lines) {
            boolean lineComplete = true;
            for (int idx : line) {
                if (!unlocked.contains(idx)) {
                    lineComplete = false;
                    break;
                }
            }
            if (lineComplete) {
                return true;
            }
        }
        return false;
    }

    private boolean chargeCost(@NotNull Player player, @NotNull CardTemplate template) {
        Map<String, Object> cost = template.getRollCost();
        if (cost.isEmpty()) {
            return true;
        }
        String type = (String) cost.get("type");
        if (type == null) {
            return true;
        }

        if (type.equalsIgnoreCase("vault")) {
            double amount = ((Number) cost.getOrDefault("amount", 0.0)).doubleValue();
            if (amount <= 0) return true;
            if (!vaultHook.hasEnough(player, amount)) {
                return false;
            }
            return vaultHook.withdraw(player, amount);
        } else if (type.equalsIgnoreCase("item")) {
            String materialStr = (String) cost.get("material");
            int amount = ((Number) cost.getOrDefault("amount", 1)).intValue();
            if (materialStr == null) return true;
            Material material = Material.matchMaterial(materialStr);
            if (material == null) return false;

            if (!player.getInventory().containsAtLeast(new ItemStack(material), amount)) {
                return false;
            }
            player.getInventory().removeItem(new ItemStack(material, amount));
            return true;
        }
        return true;
    }
}
