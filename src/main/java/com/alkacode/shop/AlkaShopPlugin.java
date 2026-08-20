package com.alkacode.shop;

import com.alkacode.shop.api.AlkaShopAPI;
import com.alkacode.shop.api.AlkaShopAPIProvider;
import com.alkacode.shop.command.ShopAdminCommand;
import com.alkacode.shop.command.VenderCommand;
import com.alkacode.shop.config.ConfigManager;
import com.alkacode.shop.hook.AlkaDropHook;
import com.alkacode.shop.hook.AlkaEconomyHook;
import com.alkacode.shop.hook.AlkaVipsHook;
import com.alkacode.shop.hook.PlaceholderAPIHook;
import com.alkacode.shop.hook.RankUpHook;
import com.alkacode.shop.listener.AdminPriceChatListener;
import com.alkacode.shop.listener.DropCollectedListener;
import com.alkacode.shop.listener.PlayerJoinListener;
import com.alkacode.shop.listener.PlayerPickupListener;
import com.alkacode.shop.listener.ShopMenuListener;
import com.alkacode.shop.manager.AdminPriceInputManager;
import com.alkacode.shop.manager.PlayerDataManager;
import com.alkacode.shop.manager.PriceManager;
import com.alkacode.shop.manager.SellManager;
import com.alkacode.shop.storage.AlkaShopRepository;
import com.alkacode.core.plugin.AlkaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.plugin.ServicePriority;

public final class AlkaShopPlugin extends AlkaPlugin {

    private AlkaShopRepository database;
    private PlayerDataManager playerDataManager;

    @Override
    protected void onPluginEnable() {
        ConfigManager configManager = new ConfigManager(this);
        configManager.load();

        database = new AlkaShopRepository(getAlkaAPI(), getLogger(), getDataFolder());

        PriceManager priceManager = new PriceManager(this);
        priceManager.load();

        AlkaEconomyHook economyHook = AlkaEconomyHook.resolve();
        if (economyHook == null) {
            getLogger().severe("AlkaEconomy nao encontrado - AlkaShop nao pode depositar nenhuma venda.");
        }

        // AlkaVips e AlkaRankUp (softdepend) nao tem ordem garantida de onEnable - mesma
        // licao ja documentada no AlkaMinesHook/AlkaDropHook: resolve 1 tick depois, nunca
        // sincrono aqui, senao o hook fica permanentemente vazio se eles habilitarem depois
        // do AlkaShop nesse boot especifico. As AtomicReference precisam existir antes do
        // SellManager pra ele ja nascer com o Supplier certo (nunca o hook resolvido direto).
        java.util.concurrent.atomic.AtomicReference<AlkaVipsHook> alkaVipsHookRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        java.util.concurrent.atomic.AtomicReference<RankUpHook> rankUpHookRef = new java.util.concurrent.atomic.AtomicReference<>(null);
        Bukkit.getScheduler().runTask(this, () -> {
            alkaVipsHookRef.set(AlkaVipsHook.tryHook(getLogger()));
            rankUpHookRef.set(RankUpHook.tryHook(getLogger()));
        });

        playerDataManager = new PlayerDataManager(database);
        SellManager sellManager = new SellManager(priceManager, playerDataManager, economyHook, configManager,
                rankUpHookRef::get, alkaVipsHookRef::get);

        ShopServices services = new ShopServices(this, configManager, priceManager, playerDataManager, sellManager, economyHook);
        services.adminPriceInput = new AdminPriceInputManager(services);
        services.alkaVipsHookSupplier = alkaVipsHookRef::get;
        services.rankUpHookSupplier = rankUpHookRef::get;

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
    protected void onPluginDisable() {
        if (playerDataManager != null) {
            playerDataManager.saveAllSync();
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
        getServer().getPluginManager().registerEvents(new AdminPriceChatListener(this, services.adminPriceInput), this);

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
