package org.yuemi.bingogacha.plugin.hook;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ItemPluginHook {

    /**
     * Resolves a custom item stack from third-party plugins.
     *
     * @param pluginName the custom item provider (e.g. ItemsAdder, Oraxen, MMOItems)
     * @param itemId     the custom item ID
     * @return the ItemStack, or null if the plugin is not installed or resolution failed
     */
    @Nullable
    public static ItemStack getCustomItem(@NotNull String pluginName, @NotNull String itemId) {
        if (pluginName.equalsIgnoreCase("ItemsAdder") && Bukkit.getPluginManager().isPluginEnabled("ItemsAdder")) {
            try {
                Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
                java.lang.reflect.Method getInstance = customStackClass.getMethod("getInstance", String.class);
                Object customStack = getInstance.invoke(null, itemId);
                if (customStack != null) {
                    java.lang.reflect.Method getItemStack = customStackClass.getMethod("getItemStack");
                    return (ItemStack) getItemStack.invoke(customStack);
                }
            } catch (Throwable ignored) {
            }
        }

        if (pluginName.equalsIgnoreCase("Oraxen") && Bukkit.getPluginManager().isPluginEnabled("Oraxen")) {
            try {
                Class<?> oraxenItemsClass = Class.forName("io.thomy.oraxen.api.OraxenItems");
                java.lang.reflect.Method getItemById = oraxenItemsClass.getMethod("getItemById", String.class);
                return (ItemStack) getItemById.invoke(null, itemId);
            } catch (Throwable t1) {
                try {
                    Class<?> oraxenItemsClass = Class.forName("io.thomy.oraxen.items.OraxenItems");
                    java.lang.reflect.Method getItemById = oraxenItemsClass.getMethod("getItemById", String.class);
                    return (ItemStack) getItemById.invoke(null, itemId);
                } catch (Throwable ignored) {
                }
            }
        }

        if (pluginName.equalsIgnoreCase("MMOItems") && Bukkit.getPluginManager().isPluginEnabled("MMOItems")) {
            try {
                String[] parts = itemId.split(":", 2);
                if (parts.length == 2) {
                    String type = parts[0];
                    String id = parts[1];
                    Class<?> mmoItemsClass = Class.forName("net.Indyuce.mmoitems.MMOItems");
                    java.lang.reflect.Method get = mmoItemsClass.getMethod("get");
                    Object mmoItemsInstance = get.invoke(null);
                    java.lang.reflect.Method getItem = mmoItemsInstance.getClass().getMethod("getItem", String.class, String.class);
                    return (ItemStack) getItem.invoke(mmoItemsInstance, type, id);
                }
            } catch (Throwable ignored) {
            }
        }

        return null;
    }
}
