package com.alkacode.shop.listener;

import com.alkacode.shop.ShopServices;
import com.alkacode.shop.model.PlayerShopData;
import com.alkacode.shop.util.AutoSellNotifier;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Map;

/** Logica compartilhada entre DropCollectedListener e PlayerPickupListener. */
final class AutoSellSupport {

    private AutoSellSupport() {
    }

    static boolean isActive(ShopServices services, Player player, Material material) {
        if (services.configManager.config().getBoolean("force-autosell", false)) {
            return true;
        }
        if (!player.hasPermission("alkashop.autosell")) {
            return false;
        }

        String category = services.priceManager.getCategory(material);
        if (!category.isBlank() && !player.hasPermission("alkashop.autosell." + category)) {
            return false;
        }

        PlayerShopData data = services.playerDataManager.get(player);
        return data.isAutoSellEnabled(material);
    }

    static void notify(ShopServices services, Player player, Map<String, Double> totals) {
        AutoSellNotifier.notify(services.configManager, player, totals);
    }
}
