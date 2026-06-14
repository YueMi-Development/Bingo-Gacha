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

    private int currentPage = 0;
    private static final int CARDS_PER_PAGE = 28;

    public CardListGui(
            @NotNull Player player,
            @NotNull BingoGachaApi api,
            @NotNull BingoGachaService service,
            @NotNull BingoConfig config
    ) {
        this(player, api, service, config, 0);
    }

    public CardListGui(
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

        List<PlayerCard> playerCards = api.getPlayerCardRepository().loadPlayerCards(player.getUniqueId());
        int totalCards = playerCards.size();
        int maxPages = Math.max(1, (totalCards + CARDS_PER_PAGE - 1) / CARDS_PER_PAGE);

        this.inventory = Bukkit.createInventory(this, 54, miniMessage.deserialize(
                "<gold><bold>Bingo Gacha Cards <gray>(" + (currentPage + 1) + "/" + maxPages + ")"
        ));

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
    }

    public void render() {
        // Clear old slots and reset grid items to air
        activeCardSlots.clear();

        // Clear card slots (10-16, 19-25, 28-34, 37-43)
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                inventory.setItem(row * 9 + col, null);
            }
        }

        List<PlayerCard> playerCards = api.getPlayerCardRepository().loadPlayerCards(player.getUniqueId());
        int totalCards = playerCards.size();
        int maxPages = Math.max(1, (totalCards + CARDS_PER_PAGE - 1) / CARDS_PER_PAGE);
        if (currentPage >= maxPages) {
            currentPage = maxPages - 1;
        }

        // 1. Render Active Cards for current page
        int cardSlotIndex = 10;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        int startIndex = currentPage * CARDS_PER_PAGE;
        int endIndex = Math.min(startIndex + CARDS_PER_PAGE, totalCards);

        for (int i = startIndex; i < endIndex; i++) {
            PlayerCard card = playerCards.get(i);
            CardTemplate template = api.getTemplate(card.getTemplateId());
            if (template == null) {
                continue;
            }

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
        }

        // 2. Render Page Buttons (Previous page: slot 45, Next page: slot 53)
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

        // 3. Render Card Shop Button at slot 49
        ItemStack shopItem = new ItemStack(Material.EMERALD);
        ItemMeta shopMeta = shopItem.getItemMeta();
        if (shopMeta != null) {
            shopMeta.displayName(miniMessage.deserialize("<green><bold>Card Shop"));
            shopMeta.lore(List.of(
                    miniMessage.deserialize("<gray>Click to browse and buy new bingo cards!")
            ));
            shopItem.setItemMeta(shopMeta);
        }
        inventory.setItem(49, shopItem);
    }

    @Override
    public void handleClick(@NotNull InventoryClickEvent event) {
        int slot = event.getSlot();

        if (slot == 45 && currentPage > 0) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
            player.openInventory(new CardListGui(player, api, service, config, currentPage - 1).getInventory());
            return;
        }

        if (slot == 53) {
            List<PlayerCard> playerCards = api.getPlayerCardRepository().loadPlayerCards(player.getUniqueId());
            int maxPages = Math.max(1, (playerCards.size() + CARDS_PER_PAGE - 1) / CARDS_PER_PAGE);
            if (currentPage < maxPages - 1) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                player.openInventory(new CardListGui(player, api, service, config, currentPage + 1).getInventory());
                return;
            }
        }

        if (slot == 49) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
            player.openInventory(new CardShopGui(player, api, service, config, 0).getInventory());
            return;
        }

        if (activeCardSlots.containsKey(slot)) {
            PlayerCard card = activeCardSlots.get(slot);
            CardTemplate template = api.getTemplate(card.getTemplateId());
            if (template != null) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                player.openInventory(new GachaGridGui(player, card, template, api, service, config).getInventory());
            }
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
