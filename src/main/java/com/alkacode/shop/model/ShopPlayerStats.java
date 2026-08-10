package com.alkacode.shop.model;

import org.bukkit.Material;

import java.util.Set;
import java.util.UUID;

/** Snapshot imutavel de PlayerShopData, exposto via AlkaShopAPI#getStats. */
public record ShopPlayerStats(UUID uuid, boolean autoSellAll, Set<Material> autoSellMaterials,
                               double totalSoldCoins, double totalSoldEscarion,
                               int totalItemsSold, int totalTransactions) {

    public static ShopPlayerStats of(PlayerShopData data) {
        return new ShopPlayerStats(data.uuid(), data.autoSellAll(), data.autoSellMaterials(), data.totalSoldCoins(),
                data.totalSoldEscarion(), data.totalItemsSold(), data.totalTransactions());
    }
}
