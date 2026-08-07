package com.alkacode.shop.model;

import java.util.UUID;

/** Snapshot imutavel de PlayerShopData, exposto via AlkaShopAPI#getStats. */
public record ShopPlayerStats(UUID uuid, boolean autoSellEnabled, double totalSoldCoins, double totalSoldEscarion,
                               int totalItemsSold, int totalTransactions) {

    public static ShopPlayerStats of(PlayerShopData data) {
        return new ShopPlayerStats(data.uuid(), data.autoSellEnabled(), data.totalSoldCoins(),
                data.totalSoldEscarion(), data.totalItemsSold(), data.totalTransactions());
    }
}
