package com.alkacode.shop;

import com.alkacode.shop.config.ConfigManager;
import com.alkacode.shop.hook.AlkaEconomyHook;
import com.alkacode.shop.manager.PlayerDataManager;
import com.alkacode.shop.manager.PriceManager;
import com.alkacode.shop.manager.SellManager;
import com.alkacode.shop.util.TextUtil;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

/** Agrega managers/services num unico objeto pra simplificar a injecao em comandos/menus/listeners. */
public final class ShopServices {

    public final JavaPlugin plugin;
    public final ConfigManager configManager;
    public final PriceManager priceManager;
    public final PlayerDataManager playerDataManager;
    public final SellManager sellManager;
    public final AlkaEconomyHook economyHook;

    public ShopServices(JavaPlugin plugin, ConfigManager configManager, PriceManager priceManager,
                         PlayerDataManager playerDataManager, SellManager sellManager, AlkaEconomyHook economyHook) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.priceManager = priceManager;
        this.playerDataManager = playerDataManager;
        this.sellManager = sellManager;
        this.economyHook = economyHook;
    }

    public void sendMessage(Player player, String path, Map<String, String> placeholders) {
        player.sendMessage(TextUtil.parse(configManager.prefix() + configManager.message(path), placeholders));
    }
}
