package org.yuemi.bingogacha.plugin;

import java.sql.SQLException;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.yuemi.bingogacha.api.BingoGachaApi;
import org.yuemi.bingogacha.api.BingoGachaApiProvider;
import org.yuemi.bingogacha.plugin.command.BingoCommand;
import org.yuemi.bingogacha.plugin.config.BingoConfig;
import org.yuemi.bingogacha.plugin.gui.BingoGuiManager;
import org.yuemi.bingogacha.plugin.hook.PlaceholderApiHook;
import org.yuemi.bingogacha.plugin.hook.VaultHook;
import org.yuemi.bingogacha.plugin.repository.DatabaseManager;
import org.yuemi.bingogacha.plugin.repository.PlayerCardRepository;
import org.yuemi.bingogacha.plugin.reward.RewardManagerImpl;
import org.yuemi.bingogacha.plugin.service.BingoGachaService;

public final class BingoGachaPlugin extends JavaPlugin implements Listener {

    private VaultHook vaultHook;
    private RewardManagerImpl rewardManager;
    private BingoConfig config;
    private DatabaseManager databaseManager;
    private PlayerCardRepository repository;
    private BingoGachaService service;
    private BingoGachaApiImpl api;
    private PlaceholderApiHook papiHook;

    @Override
    public void onEnable() {
        // 1. Setup Vault Hook
        vaultHook = new VaultHook();
        vaultHook.setup();
        if (vaultHook.isEnabled()) {
            getLogger().info("Hooked into Vault Economy successfully.");
        } else {
            getLogger().warning("Vault Economy not found! Economy roll costs and economy rewards will not function.");
        }

        // 2. Setup Reward Manager
        rewardManager = new RewardManagerImpl(this, vaultHook);

        // 3. Load Config
        config = new BingoConfig(this, rewardManager);
        config.load();

        // 4. Setup Database
        String storageType = config.getStorageType();
        databaseManager = new DatabaseManager(this, storageType, config.getDbSettings());
        try {
            databaseManager.init();
            getLogger().info("Successfully connected to the database (" + storageType + ").");
        } catch (SQLException e) {
            getLogger().severe("Could not initialize database connection pool: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 5. Setup Repository
        repository = new PlayerCardRepository(this, databaseManager);

        // 6. Setup Service
        service = new BingoGachaService(this, repository, vaultHook);

        // 7. Setup API
        api = new BingoGachaApiImpl(rewardManager, repository, config);
        BingoGachaApiProvider.register(api);

        getServer().getServicesManager().register(
                BingoGachaApi.class,
                api,
                this,
                ServicePriority.Normal
        );

        // 8. Register Event Listeners
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new BingoGuiManager(), this);

        // Load data for online players (in case of reload)
        for (org.bukkit.entity.Player online : Bukkit.getOnlinePlayers()) {
            repository.loadIntoCache(online.getUniqueId());
        }

        // 9. Register Commands
        BingoCommand cmd = new BingoCommand(this, api, service, config);
        var commandInstance = getCommand("bingo");
        if (commandInstance != null) {
            commandInstance.setExecutor(cmd);
            commandInstance.setTabCompleter(cmd);
        }

        // 10. Register PlaceholderAPI Hook
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            papiHook = new PlaceholderApiHook(this, repository);
            papiHook.register();
            getLogger().info("PlaceholderAPI expansion registered successfully.");
        }
    }

    @Override
    public void onDisable() {
        // Unregister PlaceholderAPI
        if (papiHook != null) {
            papiHook.unregister();
        }

        // Unregister API
        if (api != null) {
            getServer().getServicesManager().unregister(BingoGachaApi.class, api);
            BingoGachaApiProvider.unregister();
        }

        // Close Database connections
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        repository.loadIntoCache(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        repository.invalidateCache(event.getPlayer().getUniqueId());
    }
}
