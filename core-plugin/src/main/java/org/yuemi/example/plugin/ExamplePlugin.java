package org.yuemi.example.plugin;

import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.yuemi.example.api.ExampleApi;

public final class ExamplePlugin extends JavaPlugin {

    private ExampleApi api;

    @Override
    public void onEnable() {
        this.api = new ExampleApiImpl();

        getServer().getServicesManager().register(
                ExampleApi.class,
                api,
                this,
                ServicePriority.Normal
        );
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregister(ExampleApi.class, api);
    }
}
