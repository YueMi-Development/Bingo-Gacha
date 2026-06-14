package org.yuemi.bingogacha.plugin.config;

import java.io.File;
import java.util.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yuemi.bingogacha.api.model.*;
import org.yuemi.bingogacha.api.reward.Reward;
import org.yuemi.bingogacha.api.reward.RewardManager;

public class BingoConfig {

    private final JavaPlugin plugin;
    private final RewardManager rewardManager;

    private final Map<String, CardTemplate> templates = new HashMap<>();
    private String storageType = "sqlite";
    private final Map<String, Object> dbSettings = new HashMap<>();

    // Display configurations
    private String inactivePointMaterial = "RED_STAINED_GLASS_PANE";
    private String inactivePointName = "<red>? Locked Slot";
    private int inactivePointCustomModelData = 0;

    private String activePointMaterial = "GREEN_STAINED_GLASS_PANE";
    private String activePointName = "<green>✔ Unlocked Slot";
    private int activePointCustomModelData = 0;

    private String rollButtonMaterial = "GOLD_BLOCK";
    private String rollButtonName = "<yellow><bold>Roll Gacha!";
    private int rollButtonCustomModelData = 0;

    private String fillMaterial = "GRAY_STAINED_GLASS_PANE";
    private String fillName = " ";
    private int fillCustomModelData = 0;

    public BingoConfig(@NotNull JavaPlugin plugin, @NotNull RewardManager rewardManager) {
        this.plugin = plugin;
        this.rewardManager = rewardManager;
    }

    public void load() {
        plugin.saveDefaultConfig();
        
        File cardsDir = new File(plugin.getDataFolder(), "cards");
        if (!cardsDir.exists()) {
            cardsDir.mkdirs();
            try {
                plugin.saveResource("cards/example_3x3.yml", false);
                plugin.saveResource("cards/example_5x5.yml", false);
            } catch (IllegalArgumentException e) {
                // Occurs if resources are not found or already copied
            }
        }

        // Load config.yml
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        int configVersion = config.getInt("config-version", 1);
        plugin.getLogger().info("Loading config.yml (version " + configVersion + ")");

        storageType = config.getString("storage.type", "sqlite").toLowerCase();
        
        ConfigurationSection storageSec = config.getConfigurationSection("storage");
        if (storageSec != null) {
            dbSettings.clear();
            for (String key : storageSec.getKeys(true)) {
                dbSettings.put(key, storageSec.get(key));
            }
        }

        // Load display options
        ConfigurationSection displaySec = config.getConfigurationSection("display");
        if (displaySec != null) {
            inactivePointMaterial = displaySec.getString("inactive-point.material", "RED_STAINED_GLASS_PANE");
            inactivePointName = displaySec.getString("inactive-point.name", "<red>? Locked Slot");
            inactivePointCustomModelData = displaySec.getInt("inactive-point.custom-model-data", 0);

            activePointMaterial = displaySec.getString("active-point.material", "GREEN_STAINED_GLASS_PANE");
            activePointName = displaySec.getString("active-point.name", "<green>✔ Unlocked Slot");
            activePointCustomModelData = displaySec.getInt("active-point.custom-model-data", 0);

            rollButtonMaterial = displaySec.getString("roll-button.material", "GOLD_BLOCK");
            rollButtonName = displaySec.getString("roll-button.name", "<yellow><bold>Roll Gacha!");
            rollButtonCustomModelData = displaySec.getInt("roll-button.custom-model-data", 0);

            fillMaterial = displaySec.getString("fill.material", "GRAY_STAINED_GLASS_PANE");
            fillName = displaySec.getString("fill.name", " ");
            fillCustomModelData = displaySec.getInt("fill.custom-model-data", 0);
        }

        // Load card templates from cards/ directory
        templates.clear();
        File[] files = cardsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String id = file.getName().substring(0, file.getName().length() - 4);
                YamlConfiguration cardConfig = YamlConfiguration.loadConfiguration(file);
                int cardVersion = cardConfig.getInt("config-version", 1);
                plugin.getLogger().info("Loading card template '" + id + "' (version " + cardVersion + ")");
                
                try {
                    CardTemplate template = parseTemplate(id, cardConfig);
                    templates.put(id, template);
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to parse card template from file '" + file.getName() + "': " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    @NotNull
    private CardTemplate parseTemplate(@NotNull String id, @NotNull ConfigurationSection sec) {
        String title = sec.getString("title", id);
        
        String sizeStr = sec.getString("size", "3x3").toLowerCase();
        CardSize size = sizeStr.equals("5x5") ? CardSize.FIVE_BY_FIVE : CardSize.THREE_BY_THREE;

        String compStr = sec.getString("completion-type", "STRAIGHT_LINE").toUpperCase();
        CompletionType completionType = compStr.equals("FULL_CARD") ? CompletionType.FULL_CARD : CompletionType.STRAIGHT_LINE;

        boolean repeatable = sec.getBoolean("repeatable", true);

        // Parse cost
        Map<String, Object> rollCost = new HashMap<>();
        ConfigurationSection costSec = sec.getConfigurationSection("roll-cost");
        if (costSec != null) {
            for (String k : costSec.getKeys(false)) {
                rollCost.put(k, costSec.get(k));
            }
        }

        // Parse rewards
        List<?> rewardList = sec.getList("rewards");
        List<Reward> rewards = rewardList != null ? rewardManager.parseRewards(rewardList) : new ArrayList<>();

        List<?> unusedList = sec.getList("unused-point-rewards");
        List<Reward> unusedPointRewards = unusedList != null ? rewardManager.parseRewards(unusedList) : new ArrayList<>();

        // Parse slots
        Map<Integer, CardSlot> slots = new HashMap<>();
        ConfigurationSection slotsSec = sec.getConfigurationSection("slots");
        
        int totalSlots = size.getTotalSlots();
        for (int i = 0; i < totalSlots; i++) {
            ConfigurationSection slotSec = slotsSec != null ? slotsSec.getConfigurationSection(String.valueOf(i)) : null;
            if (slotSec != null) {
                String material = slotSec.getString("material", "STONE");
                String name = slotSec.getString("name");
                List<String> lore = slotSec.contains("lore") ? slotSec.getStringList("lore") : null;
                Integer customModelData = slotSec.contains("custom-model-data") ? slotSec.getInt("custom-model-data") : null;
                slots.put(i, new CardSlot(i, material, name, lore, customModelData));
            } else {
                // Fallback / default stone slot
                slots.put(i, new CardSlot(i, "STONE", "<gray>Slot #" + (i + 1), new ArrayList<>(), null));
            }
        }

        // Parse display section overrides
        Map<String, Object> displayOverrides = new HashMap<>();
        ConfigurationSection displaySec = sec.getConfigurationSection("display");
        if (displaySec != null) {
            for (String key : displaySec.getKeys(false)) {
                ConfigurationSection itemSec = displaySec.getConfigurationSection(key);
                if (itemSec != null) {
                    Map<String, Object> itemMap = new HashMap<>();
                    for (String itemKey : itemSec.getKeys(false)) {
                        itemMap.put(itemKey, itemSec.get(itemKey));
                    }
                    displayOverrides.put(key, itemMap);
                }
            }
        }

        return new CardTemplate(id, title, size, completionType, repeatable, rollCost, rewards, unusedPointRewards, slots, displayOverrides);
    }

    @NotNull
    public String getStorageType() {
        return storageType;
    }

    @NotNull
    public Map<String, Object> getDbSettings() {
        return dbSettings;
    }

    @NotNull
    public Map<String, CardTemplate> getTemplates() {
        return templates;
    }

    @Nullable
    public CardTemplate getTemplate(@NotNull String id) {
        return templates.get(id);
    }

    @NotNull
    public String getInactivePointMaterial() {
        return inactivePointMaterial;
    }

    @NotNull
    public String getInactivePointName() {
        return inactivePointName;
    }

    public int getInactivePointCustomModelData() {
        return inactivePointCustomModelData;
    }

    @NotNull
    public String getActivePointMaterial() {
        return activePointMaterial;
    }

    @NotNull
    public String getActivePointName() {
        return activePointName;
    }

    public int getActivePointCustomModelData() {
        return activePointCustomModelData;
    }

    @NotNull
    public String getRollButtonMaterial() {
        return rollButtonMaterial;
    }

    @NotNull
    public String getRollButtonName() {
        return rollButtonName;
    }

    public int getRollButtonCustomModelData() {
        return rollButtonCustomModelData;
    }

    @NotNull
    public String getFillMaterial() {
        return fillMaterial;
    }

    @NotNull
    public String getFillName() {
        return fillName;
    }

    public int getFillCustomModelData() {
        return fillCustomModelData;
    }
}
