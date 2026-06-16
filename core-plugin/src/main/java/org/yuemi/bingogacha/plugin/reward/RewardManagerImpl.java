package org.yuemi.bingogacha.plugin.reward;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yuemi.bingogacha.api.reward.Reward;
import org.yuemi.bingogacha.api.reward.RewardManager;
import org.yuemi.bingogacha.api.reward.RewardParser;
import org.yuemi.bingogacha.plugin.hook.ItemPluginHook;
import org.yuemi.bingogacha.plugin.hook.VaultHook;

public class RewardManagerImpl implements RewardManager {

    private final JavaPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, RewardParser> parsers = new HashMap<>();
    private final VaultHook vaultHook;

    public RewardManagerImpl(@NotNull JavaPlugin plugin, @NotNull VaultHook vaultHook) {
        this.plugin = plugin;
        this.vaultHook = vaultHook;
        registerStandardParsers();
    }

    private void registerStandardParsers() {
        registerParser("command", new CommandRewardParser(plugin));
        registerParser("economy", new EconomyRewardParser(vaultHook));
        registerParser("item", new ItemRewardParser(plugin, miniMessage));
        registerParser("custom-item", new CustomItemRewardParser(plugin));
    }

    @Override
    public void registerParser(@NotNull String type, @NotNull RewardParser parser) {
        parsers.put(type.toLowerCase(), parser);
    }

    @Override
    public @Nullable Reward parseReward(@NotNull Map<String, Object> configMap) {
        String type = (String) configMap.get("type");
        if (type == null) {
            return null;
        }
        RewardParser parser = parsers.get(type.toLowerCase());
        if (parser == null) {
            return null;
        }
        return parser.parse(configMap);
    }

    @Override
    public @NotNull List<Reward> parseRewards(@NotNull List<?> configList) {
        List<Reward> rewards = new ArrayList<>();
        for (Object obj : configList) {
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) obj;
                Reward reward = parseReward(map);
                if (reward != null) {
                    rewards.add(reward);
                }
            }
        }
        return rewards;
    }

    // Static nested implementations of Reward / RewardParser

    private static class CommandReward implements Reward {
        private final JavaPlugin plugin;
        private final String command;

        public CommandReward(JavaPlugin plugin, String command) {
            this.plugin = plugin;
            this.command = command;
        }

        @Override
        public void give(@NotNull Player player) {
            String processed = command.replace("%player%", player.getName());
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processed);
            });
        }
    }

    private static class CommandRewardParser implements RewardParser {
        private final JavaPlugin plugin;

        public CommandRewardParser(JavaPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public @Nullable Reward parse(@NotNull Map<String, Object> configMap) {
            String command = (String) configMap.get("command");
            return command != null ? new CommandReward(plugin, command) : null;
        }
    }

    private static class EconomyReward implements Reward {
        private final VaultHook vaultHook;
        private final double amount;

        public EconomyReward(VaultHook vaultHook, double amount) {
            this.vaultHook = vaultHook;
            this.amount = amount;
        }

        @Override
        public void give(@NotNull Player player) {
            org.yuemi.libs.api.YueMiLibsApi api = org.yuemi.libs.api.YueMiLibsProvider.getApi();
            if (api != null) {
                org.yuemi.libs.api.economy.EconomyProvider provider = api.getEconomy().getActiveProvider();
                if (provider != null && provider.isAvailable()) {
                    provider.deposit(player, amount);
                    return;
                }
            }
            if (vaultHook.isEnabled()) {
                vaultHook.deposit(player, amount);
            }
        }
    }

    private static class EconomyRewardParser implements RewardParser {
        private final VaultHook vaultHook;

        public EconomyRewardParser(VaultHook vaultHook) {
            this.vaultHook = vaultHook;
        }

        @Override
        public @Nullable Reward parse(@NotNull Map<String, Object> configMap) {
            Number num = (Number) configMap.get("amount");
            return num != null ? new EconomyReward(vaultHook, num.doubleValue()) : null;
        }
    }

    private static class ItemReward implements Reward {
        private final JavaPlugin plugin;
        private final MiniMessage miniMessage;
        private final String materialName;
        private final int amount;
        private final String displayName;
        private final List<String> lore;

        public ItemReward(JavaPlugin plugin, MiniMessage miniMessage, String materialName, int amount, String displayName, List<String> lore) {
            this.plugin = plugin;
            this.miniMessage = miniMessage;
            this.materialName = materialName;
            this.amount = amount;
            this.displayName = displayName;
            this.lore = lore;
        }

        @Override
        public void give(@NotNull Player player) {
            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                return;
            }
            ItemStack item = new ItemStack(material, amount);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (displayName != null) {
                    meta.displayName(miniMessage.deserialize(displayName));
                }
                if (lore != null) {
                    meta.lore(lore.stream().map(miniMessage::deserialize).toList());
                }
                item.setItemMeta(meta);
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
                for (ItemStack remaining : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), remaining);
                }
            });
        }
    }

    private static class ItemRewardParser implements RewardParser {
        private final JavaPlugin plugin;
        private final MiniMessage miniMessage;

        public ItemRewardParser(JavaPlugin plugin, MiniMessage miniMessage) {
            this.plugin = plugin;
            this.miniMessage = miniMessage;
        }

        @Override
        public @Nullable Reward parse(@NotNull Map<String, Object> configMap) {
            String material = (String) configMap.get("material");
            if (material == null) return null;
            int amount = ((Number) configMap.getOrDefault("amount", 1)).intValue();
            String name = (String) configMap.get("name");
            @SuppressWarnings("unchecked")
            List<String> lore = (List<String>) configMap.get("lore");
            return new ItemReward(plugin, miniMessage, material, amount, name, lore);
        }
    }

    private static class CustomItemReward implements Reward {
        private final JavaPlugin plugin;
        private final String pluginName;
        private final String itemId;
        private final int amount;

        public CustomItemReward(JavaPlugin plugin, String pluginName, String itemId, int amount) {
            this.plugin = plugin;
            this.pluginName = pluginName;
            this.itemId = itemId;
            this.amount = amount;
        }

        @Override
        public void give(@NotNull Player player) {
            org.yuemi.libs.api.YueMiLibsApi api = org.yuemi.libs.api.YueMiLibsProvider.getApi();
            if (api != null) {
                String key = pluginName.toLowerCase() + ":" + itemId;
                if (api.getItems().giveItem(player, key, amount)) {
                    return;
                }
            }

            ItemStack item = ItemPluginHook.getCustomItem(pluginName, itemId);
            if (item == null) {
                return;
            }
            item.setAmount(amount);
            Bukkit.getScheduler().runTask(plugin, () -> {
                Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
                for (ItemStack remaining : overflow.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), remaining);
                }
            });
        }
    }

    private static class CustomItemRewardParser implements RewardParser {
        private final JavaPlugin plugin;

        public CustomItemRewardParser(JavaPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public @Nullable Reward parse(@NotNull Map<String, Object> configMap) {
            String pluginName = (String) configMap.get("plugin");
            String id = (String) configMap.get("id");
            if (pluginName == null || id == null) return null;
            int amount = ((Number) configMap.getOrDefault("amount", 1)).intValue();
            return new CustomItemReward(plugin, pluginName, id, amount);
        }
    }
}
