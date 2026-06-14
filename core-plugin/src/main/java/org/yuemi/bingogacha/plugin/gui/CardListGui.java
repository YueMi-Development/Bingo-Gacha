package org.yuemi.bingogacha.plugin.gui;

import java.text.SimpleDateFormat;
import java.util.Date;
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

public class CardListGui implements BingoGuiHolder {

    private final Player player;
    private final BingoGachaApi api;
    private final BingoGachaService service;
    private final BingoConfig config;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Inventory inventory;

    private final Map<Integer, PlayerCard> activeCardSlots = new HashMap<>();
    private final Map<Integer, CardTemplate> templateSlots = new HashMap<>();

    public CardListGui(
            @NotNull Player player,
            @NotNull BingoGachaApi api,
            @NotNull BingoGachaService service,
            @NotNull BingoConfig config
    ) {
        this.player = player;
        this.api = api;
        this.service = service;
        this.config = config;
        this.inventory = Bukkit.createInventory(this, 54, miniMessage.deserialize("<gold><bold>Bingo Gacha Cards"));
        
        setupDecorations();
        render();
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

        // Category headers
        ItemStack activeHeader = new ItemStack(Material.BOOK);
        ItemMeta activeMeta = activeHeader.getItemMeta();
        if (activeMeta != null) {
            activeMeta.displayName(miniMessage.deserialize("<gold><bold>Your Active Cards"));
            activeHeader.setItemMeta(activeMeta);
        }
        inventory.setItem(4, activeHeader);

        ItemStack claimHeader = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta claimMeta = claimHeader.getItemMeta();
        if (claimMeta != null) {
            claimMeta.displayName(miniMessage.deserialize("<green><bold>Available Templates to Claim"));
            claimHeader.setItemMeta(claimMeta);
        }
        inventory.setItem(40, claimHeader);
    }

    public void render() {
        // Clear old slots
        activeCardSlots.clear();
        templateSlots.clear();

        // 1. Render Active Cards (slots 10-16, 19-25, 28-34)
        List<PlayerCard> playerCards = api.getPlayerCardRepository().loadPlayerCards(player.getUniqueId());
        
        int cardSlotIndex = 10;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        for (PlayerCard card : playerCards) {
            CardTemplate template = api.getTemplate(card.getTemplateId());
            if (template == null) {
                continue;
            }

            // SkipCompleted cards or display them at the end? Let's display both, marking completed
            ItemStack cardItem = new ItemStack(card.isCompleted() ? Material.MAP : Material.PAPER);
            ItemMeta meta = cardItem.getItemMeta();
            if (meta != null) {
                String title = card.isCompleted() 
                        ? "<green>[Completed] " + template.getTitle() 
                        : "<gold>[Active] " + template.getTitle();
                meta.displayName(miniMessage.deserialize(title));

                List<String> lore = List.of(
                        miniMessage.serialize(miniMessage.deserialize("<gray>Size: " + (template.getSize() == org.yuemi.bingogacha.api.model.CardSize.FIVE_BY_FIVE ? "5x5" : "3x3"))),
                        miniMessage.serialize(miniMessage.deserialize("<gray>Completion: " + template.getCompletionType())),
                        miniMessage.serialize(miniMessage.deserialize("<gray>Progress: <yellow>" + card.getUnlockedSlots().size() + "/" + template.getSize().getTotalSlots())),
                        miniMessage.serialize(miniMessage.deserialize("<gray>Created: <white>" + sdf.format(new Date(card.getCreatedAt())))),
                        "",
                        card.isCompleted() 
                                ? miniMessage.serialize(miniMessage.deserialize("<green>Completed at: " + sdf.format(new Date(card.getCompletedAt()))))
                                : miniMessage.serialize(miniMessage.deserialize("<yellow>Click to open Board!"))
                );
                meta.lore(lore.stream().map(miniMessage::deserialize).toList());
                cardItem.setItemMeta(meta);
            }

            inventory.setItem(cardSlotIndex, cardItem);
            activeCardSlots.put(cardSlotIndex, card);

            cardSlotIndex++;
            if (cardSlotIndex % 9 == 8) {
                cardSlotIndex += 2; // skip border
            }
            if (cardSlotIndex >= 35) {
                break; // Limit of active cards rendered
            }
        }

        // 2. Render Claimable Templates (slots 28-34 or 37-43)
        // Let's use slot 37-43 (or row 5) for templates
        int templateSlotIndex = 37;
        for (CardTemplate template : api.getTemplates()) {
            // Check if player has active card of this template
            boolean hasActive = playerCards.stream().anyMatch(c -> c.getTemplateId().equals(template.getId()) && !c.isCompleted());
            boolean hasCompleted = playerCards.stream().anyMatch(c -> c.getTemplateId().equals(template.getId()) && c.isCompleted());

            // If not repeatable and already completed, skip rendering template
            if (!template.isRepeatable() && hasCompleted) {
                continue;
            }
            if (hasActive) {
                // Already active, don't show claim option
                continue;
            }

            ItemStack claimItem = new ItemStack(Material.GOLDEN_CARROT);
            ItemMeta meta = claimItem.getItemMeta();
            if (meta != null) {
                meta.displayName(miniMessage.deserialize("<green>" + template.getTitle()));
                
                String costText = "Free";
                Map<String, Object> cost = template.getRollCost();
                if (!cost.isEmpty()) {
                    String type = (String) cost.get("type");
                    if ("vault".equalsIgnoreCase(type)) {
                        costText = "$" + cost.get("amount") + " per roll";
                    } else if ("item".equalsIgnoreCase(type)) {
                        costText = cost.get("amount") + "x " + cost.get("material") + " per roll";
                    }
                }

                List<String> lore = List.of(
                        miniMessage.serialize(miniMessage.deserialize("<gray>Size: <white>" + (template.getSize() == org.yuemi.bingogacha.api.model.CardSize.FIVE_BY_FIVE ? "5x5" : "3x3"))),
                        miniMessage.serialize(miniMessage.deserialize("<gray>Cost: <yellow>" + costText)),
                        miniMessage.serialize(miniMessage.deserialize("<gray>Repeatable: <white>" + template.isRepeatable())),
                        "",
                        miniMessage.serialize(miniMessage.deserialize("<green>Click to start this card!"))
                );
                meta.lore(lore.stream().map(miniMessage::deserialize).toList());
                claimItem.setItemMeta(meta);
            }

            inventory.setItem(templateSlotIndex, claimItem);
            templateSlots.put(templateSlotIndex, template);

            templateSlotIndex++;
            if (templateSlotIndex % 9 == 8) {
                templateSlotIndex += 2;
            }
            if (templateSlotIndex >= 44) {
                break;
            }
        }
    }

    @Override
    public void handleClick(@NotNull InventoryClickEvent event) {
        int slot = event.getSlot();

        if (activeCardSlots.containsKey(slot)) {
            PlayerCard card = activeCardSlots.get(slot);
            CardTemplate template = api.getTemplate(card.getTemplateId());
            if (template != null) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                player.openInventory(new GachaGridGui(player, card, template, api, service, config).getInventory());
            }
        } else if (templateSlots.containsKey(slot)) {
            CardTemplate template = templateSlots.get(slot);
            PlayerCard claimed = service.claimCard(player, template);
            if (claimed != null) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                player.sendMessage(miniMessage.deserialize("<green>Successfully claimed a new bingo card!"));
                // Re-render and re-open list or directly open the board
                player.openInventory(new GachaGridGui(player, claimed, template, api, service, config).getInventory());
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.sendMessage(miniMessage.deserialize("<red>You cannot claim this card! You might already have it active."));
            }
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
