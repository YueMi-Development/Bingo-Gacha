package org.yuemi.bingogacha.plugin.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.yuemi.bingogacha.api.BingoGachaApi;
import org.yuemi.bingogacha.api.model.CardTemplate;
import org.yuemi.bingogacha.api.model.PlayerCard;
import org.yuemi.bingogacha.plugin.config.BingoConfig;
import org.yuemi.bingogacha.plugin.service.BingoGachaService;

public class CardShopGui implements BingoGuiHolder {

    private final Player player;
    private final BingoGachaApi api;
    private final BingoGachaService service;
    private final BingoConfig config;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Inventory inventory;

    private final Map<Integer, CardTemplate> shopSlots = new HashMap<>();

    private int currentPage = 0;
    private static final int TEMPLATES_PER_PAGE = 28;

    public CardShopGui(
            @NotNull Player player,
            @NotNull BingoGachaApi api,
            @NotNull BingoGachaService service,
            @NotNull BingoConfig config,
            int page
    ) {
        this.player = player;
        this.api = api;
        this.service = service;
        this.config = config;
        this.currentPage = page;

        // Filter templates that are buyable
        List<CardTemplate> buyableTemplates = api.getTemplates().stream()
                .filter(CardTemplate::isBuyable)
                .toList();

        int totalTemplates = buyableTemplates.size();
        int maxPages = Math.max(1, (totalTemplates + TEMPLATES_PER_PAGE - 1) / TEMPLATES_PER_PAGE);

        this.inventory = Bukkit.createInventory(this, 54, miniMessage.deserialize(
                "<gold><bold>Card Shop <gray>(" + (currentPage + 1) + "/" + maxPages + ")"
        ));

        setupDecorations();
        render(buyableTemplates);
    }

    private void setupDecorations() {
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.displayName(miniMessage.deserialize(" "));
            filler.setItemMeta(meta);
        }

        // Fill border
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inventory.setItem(i, filler);
            }
        }

        // Shop Header
        ItemStack shopHeader = new ItemStack(Material.GOLD_INGOT);
        ItemMeta headerMeta = shopHeader.getItemMeta();
        if (headerMeta != null) {
            headerMeta.displayName(miniMessage.deserialize("<gold><bold>Buy New Bingo Cards"));
            shopHeader.setItemMeta(headerMeta);
        }
        inventory.setItem(4, shopHeader);
    }

    public void render(List<CardTemplate> buyableTemplates) {
        shopSlots.clear();

        // Clear active display slots (10-16, 19-25, 28-34, 37-43)
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                inventory.setItem(row * 9 + col, null);
            }
        }

        int totalTemplates = buyableTemplates.size();
        int maxPages = Math.max(1, (totalTemplates + TEMPLATES_PER_PAGE - 1) / TEMPLATES_PER_PAGE);
        if (currentPage >= maxPages) {
            currentPage = maxPages - 1;
        }

        int slotIndex = 10;
        int startIndex = currentPage * TEMPLATES_PER_PAGE;
        int endIndex = Math.min(startIndex + TEMPLATES_PER_PAGE, totalTemplates);

        for (int i = startIndex; i < endIndex; i++) {
            CardTemplate template = buyableTemplates.get(i);

            ItemStack item = new ItemStack(Material.GOLDEN_CARROT);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(miniMessage.deserialize("<green>" + template.getTitle()));

                String rollCostText = "Free";
                Map<String, Object> rollCost = template.getRollCost();
                if (!rollCost.isEmpty()) {
                    String type = (String) rollCost.get("type");
                    if ("vault".equalsIgnoreCase(type)) {
                        rollCostText = "$" + rollCost.get("amount") + " per roll";
                    } else if ("item".equalsIgnoreCase(type)) {
                        rollCostText = rollCost.get("amount") + "x " + rollCost.get("material") + " per roll";
                    }
                }

                String buyCostText = "Free";
                Map<String, Object> buyCost = template.getBuyCost();
                if (!buyCost.isEmpty()) {
                    String type = (String) buyCost.get("type");
                    if ("vault".equalsIgnoreCase(type)) {
                        buyCostText = "$" + buyCost.get("amount");
                    } else if ("item".equalsIgnoreCase(type)) {
                        buyCostText = buyCost.get("amount") + "x " + buyCost.get("material");
                    }
                }

                List<String> lore = List.of(
                        miniMessage.serialize(miniMessage.deserialize("<gray>Size: <white>" + (template.getSize() == org.yuemi.bingogacha.api.model.CardSize.FIVE_BY_FIVE ? "5x5" : "3x3"))),
                        miniMessage.serialize(miniMessage.deserialize("<gray>Buy Cost: <yellow>" + buyCostText)),
                        miniMessage.serialize(miniMessage.deserialize("<gray>Roll Cost: <yellow>" + rollCostText)),
                        miniMessage.serialize(miniMessage.deserialize("<gray>Repeatable: <white>" + template.isRepeatable())),
                        "",
                        miniMessage.serialize(miniMessage.deserialize("<green>Click to purchase!"))
                );
                meta.lore(lore.stream().map(miniMessage::deserialize).toList());
                item.setItemMeta(meta);
            }

            inventory.setItem(slotIndex, item);
            shopSlots.put(slotIndex, template);

            slotIndex++;
            if (slotIndex % 9 == 8) {
                slotIndex += 2; // skip borders
            }
        }

        // Render Page Buttons
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.displayName(miniMessage.deserialize(" "));
            filler.setItemMeta(fillerMeta);
        }

        if (currentPage > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            if (prevMeta != null) {
                prevMeta.displayName(miniMessage.deserialize("<yellow><bold>◀ Previous Page (Page " + currentPage + "/" + maxPages + ")"));
                prev.setItemMeta(prevMeta);
            }
            inventory.setItem(45, prev);
        } else {
            inventory.setItem(45, filler);
        }

        if (currentPage < maxPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            if (nextMeta != null) {
                nextMeta.displayName(miniMessage.deserialize("<yellow><bold>Next Page ▶ (Page " + (currentPage + 2) + "/" + maxPages + ")"));
                next.setItemMeta(nextMeta);
            }
            inventory.setItem(53, next);
        } else {
            inventory.setItem(53, filler);
        }

        // Back to Card List button
        ItemStack backItem = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backItem.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(miniMessage.deserialize("<red><bold>Back to Card List"));
            backItem.setItemMeta(backMeta);
        }
        inventory.setItem(49, backItem);
    }

    @Override
    public void handleClick(@NotNull InventoryClickEvent event) {
        int slot = event.getSlot();

        List<CardTemplate> buyableTemplates = api.getTemplates().stream()
                .filter(CardTemplate::isBuyable)
                .toList();

        if (slot == 45 && currentPage > 0) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
            player.openInventory(new CardShopGui(player, api, service, config, currentPage - 1).getInventory());
            return;
        }

        if (slot == 53) {
            int maxPages = Math.max(1, (buyableTemplates.size() + TEMPLATES_PER_PAGE - 1) / TEMPLATES_PER_PAGE);
            if (currentPage < maxPages - 1) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                player.openInventory(new CardShopGui(player, api, service, config, currentPage + 1).getInventory());
                return;
            }
        }

        if (slot == 49) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
            player.openInventory(new CardListGui(player, api, service, config, 0).getInventory());
            return;
        }

        if (shopSlots.containsKey(slot)) {
            CardTemplate template = shopSlots.get(slot);
            int result = service.buyCard(player, template);
            if (result == 0) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                player.sendMessage(miniMessage.deserialize("<green>Successfully purchased card template: " + template.getTitle()));

                // Open the board of the newly purchased card
                List<PlayerCard> cards = api.getPlayerCardRepository().loadPlayerCards(player.getUniqueId());
                PlayerCard newCard = cards.stream()
                        .filter(c -> c.getTemplateId().equals(template.getId()) && !c.isCompleted())
                        .findFirst()
                        .orElse(null);

                if (newCard != null) {
                    player.openInventory(new GachaGridGui(player, newCard, template, api, service, config).getInventory());
                } else {
                    player.openInventory(new CardListGui(player, api, service, config, 0).getInventory());
                }
            } else if (result == 1) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.sendMessage(miniMessage.deserialize("<red>You cannot buy this card! You might already have it active or completed."));
            } else if (result == 2) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.sendMessage(miniMessage.deserialize("<red>You do not have enough money/items to buy this card!"));
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.sendMessage(miniMessage.deserialize("<red>This card template is not buyable."));
            }
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
