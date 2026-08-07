package com.alkacode.shop.listener;

import com.alkacode.shop.ShopServices;
import com.alkacode.shop.model.PlayerShopData;
import com.alkacode.shop.util.PriceFormatter;
import com.alkacode.shop.util.TextUtil;
import org.bukkit.entity.Player;

import java.util.Map;

/** Logica compartilhada entre DropCollectedListener e PlayerPickupListener. */
final class AutoSellSupport {

    private AutoSellSupport() {
    }

    static boolean isActive(ShopServices services, Player player) {
        if (services.configManager.config().getBoolean("force-autosell", false)) {
            return true;
        }
        PlayerShopData data = services.playerDataManager.get(player);
        return data.autoSellEnabled() && player.hasPermission("alkashop.autosell");
    }

    static void notify(ShopServices services, Player player, Map<String, Double> totals) {
        boolean round = services.configManager.config().getBoolean("selling.round-values", true);
        int decimals = services.configManager.config().getInt("selling.decimal-places", 2);
        String amount = PriceFormatter.formatTotals(totals, round, decimals);

        if (services.configManager.config().getBoolean("auto-sell.actionbar", true)) {
            player.sendActionBar(TextUtil.parse(services.configManager.message("sell.sold-auto"), Map.of("amount", amount)));
        }
        String soundName = services.configManager.config().getString("auto-sell.sound", "");
        if (!soundName.isBlank()) {
            var sound = org.bukkit.Registry.SOUNDS.get(org.bukkit.NamespacedKey.minecraft(soundName.toLowerCase()));
            if (sound != null) {
                float volume = (float) services.configManager.config().getDouble("auto-sell.sound-volume", 0.5);
                player.playSound(player.getLocation(), sound, volume, 1f);
            }
        }
    }
}
