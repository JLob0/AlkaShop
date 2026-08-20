package com.alkacode.shop.util;

import com.alkacode.shop.config.ConfigManager;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.Sound;

import java.util.Map;

/**
 * Feedback (actionbar + som) de uma venda automatica - unico lugar que decide isso,
 * usado tanto pelo auto-sell interno do AlkaShop (pickup do chao) quanto pela API
 * publica (AlkaShopAPIProvider#notifyAutoSell, chamada por integracoes de terceiros
 * como AlkaMines/AlkaDrop) - garante que desligar auto-sell.actionbar no config.yml
 * desliga em TODO lugar que auto-venda acontece, nao so num dos caminhos.
 */
public final class AutoSellNotifier {

    private AutoSellNotifier() {
    }

    public static void notify(ConfigManager configManager, Player player, Map<String, Double> totals) {
        if (totals == null || totals.isEmpty()) {
            return;
        }
        boolean round = configManager.config().getBoolean("selling.round-values", true);
        int decimals = configManager.config().getInt("selling.decimal-places", 2);
        String amount = PriceFormatter.formatTotals(totals, round, decimals, configManager::currencyColor);

        if (configManager.config().getBoolean("auto-sell.actionbar", true)) {
            player.sendActionBar(TextUtil.parse(configManager.message("sell.sold-auto"), Map.of("amount", amount)));
        }
        String soundName = configManager.config().getString("auto-sell.sound", "");
        if (!soundName.isBlank()) {
            Sound sound = Registry.SOUNDS.get(NamespacedKey.minecraft(soundName.toLowerCase()));
            if (sound != null) {
                float volume = (float) configManager.config().getDouble("auto-sell.sound-volume", 0.5);
                player.playSound(player.getLocation(), sound, volume, 1f);
            }
        }
    }
}
