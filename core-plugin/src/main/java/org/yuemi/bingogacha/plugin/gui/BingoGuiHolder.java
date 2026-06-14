package org.yuemi.bingogacha.plugin.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public interface BingoGuiHolder extends InventoryHolder {

    /**
     * Handles the click event for this GUI.
     *
     * @param event the inventory click event
     */
    void handleClick(@NotNull InventoryClickEvent event);

    @Override
    @NotNull
    Inventory getInventory();
}
