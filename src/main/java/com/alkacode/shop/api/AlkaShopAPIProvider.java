package com.alkacode.shop.api;

import com.alkacode.shop.config.ConfigManager;
import com.alkacode.shop.manager.PlayerDataManager;
import com.alkacode.shop.manager.PriceManager;
import com.alkacode.shop.manager.SellManager;
import com.alkacode.shop.model.PlayerShopData;
import com.alkacode.shop.model.ShopPlayerStats;
import com.alkacode.shop.model.enums.SellType;
import com.alkacode.shop.util.AutoSellNotifier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class AlkaShopAPIProvider implements AlkaShopAPI {

    private final PriceManager priceManager;
    private final SellManager sellManager;
    private final PlayerDataManager playerDataManager;
    private final ConfigManager configManager;

    public AlkaShopAPIProvider(PriceManager priceManager, SellManager sellManager, PlayerDataManager playerDataManager,
                                ConfigManager configManager) {
        this.priceManager = priceManager;
        this.sellManager = sellManager;
        this.playerDataManager = playerDataManager;
        this.configManager = configManager;
    }

    @Override
    public double getPrice(Material material, String currency) {
        return priceManager.getPrice(material, currency);
    }

    @Override
    public boolean isSellable(Material material) {
        return priceManager.isSellable(material);
    }

    @Override
    public Map<String, Double> sellItem(Player player, ItemStack item) {
        return sellManager.sell(player, List.of(item), SellType.SELECTED);
    }

    @Override
    public Map<String, Double> sellItems(Player player, List<ItemStack> items) {
        return sellManager.sell(player, items, SellType.SELECTED);
    }

    @Override
    public CompletableFuture<ShopPlayerStats> getStats(UUID playerUuid) {
        return CompletableFuture.completedFuture(ShopPlayerStats.of(playerDataManager.get(playerUuid)));
    }

    @Override
    public boolean isAutoSellActive(Player player) {
        if (configManager.config().getBoolean("force-autosell", false)) {
            return true;
        }
        if (!player.hasPermission("alkashop.autosell")) {
            return false;
        }
        PlayerShopData data = playerDataManager.get(player.getUniqueId());
        return data.autoSellAll() || !data.autoSellMaterials().isEmpty();
    }

    @Override
    public boolean isAutoSellActive(Player player, Material material) {
        if (configManager.config().getBoolean("force-autosell", false)) {
            return true;
        }
        if (!player.hasPermission("alkashop.autosell")) {
            return false;
        }
        String category = priceManager.getCategory(material);
        if (!category.isBlank() && !player.hasPermission("alkashop.autosell." + category)) {
            return false;
        }
        PlayerShopData data = playerDataManager.get(player.getUniqueId());
        return data.isAutoSellEnabled(material);
    }

    @Override
    public void notifyAutoSell(Player player, Map<String, Double> totals) {
        AutoSellNotifier.notify(configManager, player, totals);
    }
}
