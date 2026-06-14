package org.yuemi.bingogacha.plugin.hook;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VaultHook {

    private Economy economy;

    public void setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            economy = rsp.getProvider();
        }
    }

    public boolean isEnabled() {
        return economy != null;
    }

    @Nullable
    public Economy getEconomy() {
        return economy;
    }

    public boolean hasEnough(@NotNull Player player, double amount) {
        if (!isEnabled()) {
            return false;
        }
        return economy.has(player, amount);
    }

    public boolean withdraw(@NotNull Player player, double amount) {
        if (!isEnabled()) {
            return false;
        }
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public void deposit(@NotNull Player player, double amount) {
        if (!isEnabled()) {
            return;
        }
        economy.depositPlayer(player, amount);
    }
}
