package org.yuemi.bingogacha.plugin.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.jetbrains.annotations.NotNull;

public class BingoGuiManager implements Listener {

    @EventHandler
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof BingoGuiHolder holder) {
            // Cancel default inventory actions (prevent taking items)
            event.setCancelled(true);
            
            // Only handle clicks inside the custom GUI inventory, not the player's inventory
            if (event.getClickedInventory() != null && event.getClickedInventory().getHolder() instanceof BingoGuiHolder) {
                holder.handleClick(event);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(@NotNull InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof BingoGuiHolder) {
            event.setCancelled(true);
        }
    }
}
