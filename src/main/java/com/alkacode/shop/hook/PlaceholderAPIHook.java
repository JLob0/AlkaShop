package com.alkacode.shop.hook;

import com.alkacode.shop.ShopServices;
import com.alkacode.shop.model.PlayerShopData;
import com.alkacode.shop.util.PriceFormatter;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public final class PlaceholderAPIHook extends PlaceholderExpansion {

    private final ShopServices services;

    public PlaceholderAPIHook(ShopServices services) {
        this.services = services;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "alkashop";
    }

    @Override
    public @NotNull String getAuthor() {
        return "MestreDEV";
    }

    @Override
    public @NotNull String getVersion() {
        return services.plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.startsWith("price_")) {
            return resolvePrice(params.substring("price_".length()));
        }
        if (player == null) {
            return "";
        }
        PlayerShopData data = services.playerDataManager.get(player.getUniqueId());
        boolean round = services.configManager.config().getBoolean("selling.round-values", true);
        int decimals = services.configManager.config().getInt("selling.decimal-places", 2);

        return switch (params) {
            case "autosell" -> String.valueOf(data.autoSellEnabled());
            case "total_sold_coins" -> PriceFormatter.format(data.totalSoldCoins(), round, decimals);
            case "total_sold_escarion" -> PriceFormatter.format(data.totalSoldEscarion(), round, decimals);
            case "total_items" -> String.valueOf(data.totalItemsSold());
            case "total_transactions" -> String.valueOf(data.totalTransactions());
            default -> null;
        };
    }

    private String resolvePrice(String rest) {
        Material material = Material.matchMaterial(rest);
        if (material != null) {
            return formatPrice(material, "coins");
        }
        int lastUnderscore = rest.lastIndexOf('_');
        if (lastUnderscore <= 0) {
            return "0";
        }
        String materialPart = rest.substring(0, lastUnderscore);
        String currency = rest.substring(lastUnderscore + 1);
        material = Material.matchMaterial(materialPart);
        if (material == null) {
            return "0";
        }
        return formatPrice(material, currency);
    }

    private String formatPrice(Material material, String currency) {
        boolean round = services.configManager.config().getBoolean("selling.round-values", true);
        int decimals = services.configManager.config().getInt("selling.decimal-places", 2);
        return PriceFormatter.format(services.priceManager.getPrice(material, currency), round, decimals);
    }
}
