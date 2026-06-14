package org.yuemi.bingogacha.plugin.gui;

import java.util.ArrayList;
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
import org.yuemi.bingogacha.api.model.CardSize;
import org.yuemi.bingogacha.api.model.PlayerCard;
import org.yuemi.bingogacha.plugin.config.BingoConfig;
import org.yuemi.bingogacha.plugin.service.BingoGachaService;

public class GachaGridGui implements BingoGuiHolder {

    private final Player player;
    private final PlayerCard card;
    private final CardTemplate template;
    private final BingoGachaApi api;
    private final BingoGachaService service;
    private final BingoConfig config;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Inventory inventory;

    private int rollButtonSlot;
    private int backButtonSlot;
    private int infoButtonSlot;

    public GachaGridGui(
            @NotNull Player player,
            @NotNull PlayerCard card,
            @NotNull CardTemplate template,
            @NotNull BingoGachaApi api,
            @NotNull BingoGachaService service,
            @NotNull BingoConfig config
    ) {
        this.player = player;
        this.card = card;
        this.template = template;
        this.api = api;
        this.service = service;
        this.config = config;

        int sizeSlots = template.getSize() == CardSize.FIVE_BY_FIVE ? 54 : 27;
        this.inventory = Bukkit.createInventory(this, sizeSlots, miniMessage.deserialize(template.getTitle()));

        setupButtonSlots();
        setupDecorations();
        render();
    }

    private void setupButtonSlots() {
        if (template.getSize() == CardSize.FIVE_BY_FIVE) {
            rollButtonSlot = 49;
            backButtonSlot = 45;
            infoButtonSlot = 47;
        } else {
            // 3x3
            rollButtonSlot = 15;
            backButtonSlot = 18;
            infoButtonSlot = 11;
        }
    }

    private void setupDecorations() {
        Material fillerMat = Material.matchMaterial(getDisplayMaterial("fill", config.getFillMaterial()));
        if (fillerMat == null) {
            fillerMat = Material.GRAY_STAINED_GLASS_PANE;
        }
        ItemStack border = new ItemStack(fillerMat);
        ItemMeta meta = border.getItemMeta();
        if (meta != null) {
            meta.displayName(miniMessage.deserialize(getDisplayName("fill", config.getFillName())));
            int cmd = getDisplayCustomModelData("fill", config.getFillCustomModelData());
            if (cmd > 0) {
                meta.setCustomModelData(cmd);
            }
            border.setItemMeta(meta);
        }

        int totalSlots = template.getSize() == CardSize.FIVE_BY_FIVE ? 54 : 27;
        for (int i = 0; i < totalSlots; i++) {
            if (isFillerSlot(i)) {
                inventory.setItem(i, border);
            }
        }
    }

    private boolean isFillerSlot(int slot) {
        if (template.getSize() == CardSize.FIVE_BY_FIVE) {
            int x = slot % 9;
            int y = slot / 9;
            if (x >= 2 && x <= 6 && y >= 0 && y <= 4) {
                return false;
            }
            return slot != rollButtonSlot && slot != backButtonSlot && slot != infoButtonSlot;
        } else {
            int x = slot % 9;
            int y = slot / 9;
            if (x >= 3 && x <= 5 && y >= 0 && y <= 2) {
                return false;
            }
            return slot != rollButtonSlot && slot != backButtonSlot && slot != infoButtonSlot;
        }
    }

    private int getSlotIndex(int gridIndex) {
        if (template.getSize() == CardSize.FIVE_BY_FIVE) {
            int x = gridIndex % 5;
            int y = gridIndex / 5;
            return y * 9 + x + 2;
        } else {
            int x = gridIndex % 3;
            int y = gridIndex / 3;
            return y * 9 + x + 3;
        }
    }

    private String getDisplayMaterial(String key, String fallback) {
        Map<String, Object> overrides = template.getDisplayOverrides();
        if (overrides != null && overrides.containsKey(key)) {
            Map<?, ?> map = (Map<?, ?>) overrides.get(key);
            if (map != null && map.containsKey("material")) {
                return (String) map.get("material");
            }
        }
        return fallback;
    }

    private String getDisplayName(String key, String fallback) {
        Map<String, Object> overrides = template.getDisplayOverrides();
        if (overrides != null && overrides.containsKey(key)) {
            Map<?, ?> map = (Map<?, ?>) overrides.get(key);
            if (map != null && map.containsKey("name")) {
                return (String) map.get("name");
            }
        }
        return fallback;
    }

    private int getDisplayCustomModelData(String key, int fallback) {
        Map<String, Object> overrides = template.getDisplayOverrides();
        if (overrides != null && overrides.containsKey(key)) {
            Map<?, ?> map = (Map<?, ?>) overrides.get(key);
            if (map != null && map.containsKey("custom-model-data")) {
                Object val = map.get("custom-model-data");
                if (val instanceof Number) {
                    return ((Number) val).intValue();
                }
            }
        }
        return fallback;
    }

    public void render() {
        int totalSlots = template.getSize().getTotalSlots();
        
        Material activeMat = Material.matchMaterial(getDisplayMaterial("active-point", config.getActivePointMaterial()));
        if (activeMat == null) activeMat = Material.GREEN_STAINED_GLASS_PANE;
        
        Material inactiveMat = Material.matchMaterial(getDisplayMaterial("inactive-point", config.getInactivePointMaterial()));
        if (inactiveMat == null) inactiveMat = Material.RED_STAINED_GLASS_PANE;

        for (int i = 0; i < totalSlots; i++) {
            int invSlot = getSlotIndex(i);

            if (card.isSlotUnlocked(i)) {
                // Render Active Point
                ItemStack item = new ItemStack(activeMat);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.displayName(miniMessage.deserialize(getDisplayName("active-point", config.getActivePointName()) + " (Slot #" + (i + 1) + ")"));
                    int cmd = getDisplayCustomModelData("active-point", config.getActivePointCustomModelData());
                    if (cmd > 0) {
                        meta.setCustomModelData(cmd);
                    }
                    item.setItemMeta(meta);
                }
                inventory.setItem(invSlot, item);
            } else {
                // Render Inactive Point
                ItemStack item = new ItemStack(inactiveMat);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.displayName(miniMessage.deserialize(getDisplayName("inactive-point", config.getInactivePointName()) + " (Slot #" + (i + 1) + ")"));
                    int cmd = getDisplayCustomModelData("inactive-point", config.getInactivePointCustomModelData());
                    if (cmd > 0) {
                        meta.setCustomModelData(cmd);
                    }
                    item.setItemMeta(meta);
                }
                inventory.setItem(invSlot, item);
            }
        }

        // Render Back Button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(miniMessage.deserialize("<red><bold>Back to List"));
            back.setItemMeta(backMeta);
        }
        inventory.setItem(backButtonSlot, back);

        // Render Info Button
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.displayName(miniMessage.deserialize("<gold><bold>Card Details"));
            
            String status = card.isCompleted() ? "<green>Completed" : "<yellow>In Progress";
            
            List<String> loreList = new ArrayList<>();
            loreList.add(miniMessage.serialize(miniMessage.deserialize("<gray>Status: " + status)));
            loreList.add(miniMessage.serialize(miniMessage.deserialize("<gray>Progress: <yellow>" + card.getUnlockedSlots().size() + "/" + totalSlots)));
            loreList.add("");
            loreList.add(miniMessage.serialize(miniMessage.deserialize("<gold>Rewards:")));
            
            Map<String, Object> cost = template.getRollCost();
            String costText = "Free";
            if (!cost.isEmpty()) {
                String type = (String) cost.get("type");
                if ("vault".equalsIgnoreCase(type)) {
                    costText = "$" + cost.get("amount");
                } else if ("item".equalsIgnoreCase(type)) {
                    costText = cost.get("amount") + "x " + cost.get("material");
                }
            }
            loreList.add(miniMessage.serialize(miniMessage.deserialize("<gray>Cost per roll: <yellow>" + costText)));

            infoMeta.lore(loreList.stream().map(miniMessage::deserialize).toList());
            info.setItemMeta(infoMeta);
        }
        inventory.setItem(infoButtonSlot, info);

        // Render Roll Button
        Material rollMat = Material.matchMaterial(getDisplayMaterial("roll-button", config.getRollButtonMaterial()));
        if (rollMat == null) rollMat = Material.GOLD_BLOCK;
        
        ItemStack roll = new ItemStack(card.isCompleted() ? Material.BARRIER : rollMat);
        ItemMeta rollMeta = roll.getItemMeta();
        if (rollMeta != null) {
            if (card.isCompleted()) {
                rollMeta.displayName(miniMessage.deserialize("<green><bold>✔ Completed!"));
                rollMeta.lore(List.of(miniMessage.deserialize("<gray>You have fully finished this card.")));
            } else {
                rollMeta.displayName(miniMessage.deserialize(getDisplayName("roll-button", config.getRollButtonName())));
                int cmd = getDisplayCustomModelData("roll-button", config.getRollButtonCustomModelData());
                if (cmd > 0) {
                    rollMeta.setCustomModelData(cmd);
                }
                
                Map<String, Object> cost = template.getRollCost();
                String costText = "Free";
                if (!cost.isEmpty()) {
                    String type = (String) cost.get("type");
                    if ("vault".equalsIgnoreCase(type)) {
                        costText = "$" + cost.get("amount");
                    } else if ("item".equalsIgnoreCase(type)) {
                        costText = cost.get("amount") + "x " + cost.get("material");
                    }
                }
                
                rollMeta.lore(List.of(
                        miniMessage.deserialize("<gray>Click to roll a random slot!"),
                        miniMessage.deserialize("<gray>Cost: <gold>" + costText)
                ));
            }
            roll.setItemMeta(rollMeta);
        }
        inventory.setItem(rollButtonSlot, roll);
    }

    @Override
    public void handleClick(@NotNull InventoryClickEvent event) {
        int clickedSlot = event.getSlot();

        if (clickedSlot == backButtonSlot) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
            player.openInventory(new CardListGui(player, api, service, config).getInventory());
        } else if (clickedSlot == rollButtonSlot) {
            if (card.isCompleted()) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.sendMessage(miniMessage.deserialize("<red>This card is already completed!"));
                return;
            }

            int result = service.rollCard(player, card, template);
            if (result == 0) {
                if (card.isCompleted()) {
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    player.sendMessage(miniMessage.deserialize("<green><bold>BINGO! <yellow>You have completed the card and claimed your rewards!"));
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    player.sendMessage(miniMessage.deserialize("<green>Successfully rolled a slot!"));
                }
                render();
            } else if (result == 2) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.sendMessage(miniMessage.deserialize("<red>You cannot afford the cost of this roll!"));
            } else if (result == 4) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.5f);
                player.sendMessage(miniMessage.deserialize("<red>Please wait a moment before rolling again!"));
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.sendMessage(miniMessage.deserialize("<red>An error occurred or no slots are remaining."));
            }
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
