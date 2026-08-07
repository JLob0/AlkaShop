package com.alkacode.shop;

import com.alkacode.shop.api.AlkaShopAPI;
import com.alkacode.shop.api.AlkaShopAPIProvider;
import com.alkacode.shop.command.ShopAdminCommand;
import com.alkacode.shop.command.VenderCommand;
import com.alkacode.shop.config.ConfigManager;
import com.alkacode.shop.hook.AlkaDropHook;
import com.alkacode.shop.hook.AlkaEconomyHook;
import com.alkacode.shop.hook.PlaceholderAPIHook;
import com.alkacode.shop.listener.DropCollectedListener;
import com.alkacode.shop.listener.PlayerJoinListener;
import com.alkacode.shop.listener.PlayerPickupListener;
import com.alkacode.shop.listener.ShopMenuListener;
import com.alkacode.shop.manager.PlayerDataManager;
import com.alkacode.shop.manager.PriceManager;
import com.alkacode.shop.manager.SellManager;
import com.alkacode.shop.storage.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.logging.Level;

public final class AlkaShopPlugin extends JavaPlugin {

    private DatabaseManager database;
    private PlayerDataManager playerDataManager;

    @Override
    public void onEnable() {
        ConfigManager configManager = new ConfigManager(this);
        configManager.load();

        database = new DatabaseManager(getLogger());
        try {
            database.open(getDataFolder());
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Falha ao abrir o banco SQLite - desabilitando o AlkaShop.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        PriceManager priceManager = new PriceManager(this);
        priceManager.load();

        AlkaEconomyHook economyHook = AlkaEconomyHook.resolve();
        if (economyHook == null) {
            getLogger().severe("AlkaEconomy nao encontrado - AlkaShop nao pode depositar nenhuma venda.");
        }

        playerDataManager = new PlayerDataManager(database);
        SellManager sellManager = new SellManager(priceManager, playerDataManager, economyHook, configManager);

        ShopServices services = new ShopServices(this, configManager, priceManager, playerDataManager, sellManager, economyHook);

        registerCommands(services);
        registerListeners(services, configManager);

        for (Player player : Bukkit.getOnlinePlayers()) {
            playerDataManager.loadForJoin(player);
        }

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderAPIHook(services).register();
            getLogger().info("Expansao do PlaceholderAPI registrada.");
        }

        if (AlkaDropHook.isPresent()) {
            getLogger().info("AlkaDrop detectado - auto-venda vai integrar via DropCollectedEvent.");
        } else {
            getLogger().info("AlkaDrop nao encontrado - auto-venda vai usar o fallback de pickup do chao.");
        }

        AlkaShopAPI api = new AlkaShopAPIProvider(priceManager, sellManager, playerDataManager, configManager);
        getServer().getServicesManager().register(AlkaShopAPI.class, api, this, ServicePriority.Normal);

        getLogger().info("AlkaShop habilitado.");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                playerDataManager.unload(player);
            }
        }
        if (database != null) {
            database.close();
        }
    }

    private void registerCommands(ShopServices services) {
        VenderCommand venderCommand = new VenderCommand(services);
        setExecutor("vender", venderCommand);
        setExecutor("vendertudo", venderCommand);
        setExecutor("vendermao", venderCommand);
        setExecutor("vendersel", venderCommand);
        setExecutor("venderautomatico", venderCommand);
        setExecutor("venderpreco", venderCommand);

        setExecutor("alkashop", new ShopAdminCommand(services));
    }

    private void setExecutor(String name, org.bukkit.command.CommandExecutor executor) {
        var command = getCommand(name);
        if (command != null) {
            command.setExecutor(executor);
        }
    }

    /**
     * PlayerPickupListener nao usa {@code @EventHandler} - a prioridade vem de
     * `event-priority` no config.yml, lida em runtime, ja que a anotacao exige uma
     * constante em tempo de compilacao. Mudar esse valor exige reiniciar o servidor.
     */
    private void registerListeners(ShopServices services, ConfigManager configManager) {
        getServer().getPluginManager().registerEvents(new ShopMenuListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(playerDataManager), this);
        getServer().getPluginManager().registerEvents(new DropCollectedListener(services), this);

        EventPriority priority = parsePriority(configManager.config().getString("event-priority", "HIGH"));
        PlayerPickupListener pickupListener = new PlayerPickupListener(services);
        getServer().getPluginManager().registerEvent(EntityPickupItemEvent.class, pickupListener, priority,
                (listener, event) -> ((PlayerPickupListener) listener).onPickup((EntityPickupItemEvent) event), this, true);
    }

    private EventPriority parsePriority(String raw) {
        try {
            return EventPriority.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return EventPriority.HIGH;
        }
    }
}
