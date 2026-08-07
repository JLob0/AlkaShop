package com.alkacode.shop.model;

import java.util.UUID;

public final class PlayerShopData {

    private final UUID uuid;
    private boolean autoSellEnabled;
    private double totalSoldCoins;
    private double totalSoldEscarion;
    private int totalItemsSold;
    private int totalTransactions;

    public PlayerShopData(UUID uuid, boolean autoSellEnabled, double totalSoldCoins, double totalSoldEscarion,
                           int totalItemsSold, int totalTransactions) {
        this.uuid = uuid;
        this.autoSellEnabled = autoSellEnabled;
        this.totalSoldCoins = totalSoldCoins;
        this.totalSoldEscarion = totalSoldEscarion;
        this.totalItemsSold = totalItemsSold;
        this.totalTransactions = totalTransactions;
    }

    public UUID uuid() { return uuid; }
    public boolean autoSellEnabled() { return autoSellEnabled; }
    public void autoSellEnabled(boolean autoSellEnabled) { this.autoSellEnabled = autoSellEnabled; }
    public double totalSoldCoins() { return totalSoldCoins; }
    public double totalSoldEscarion() { return totalSoldEscarion; }
    public int totalItemsSold() { return totalItemsSold; }
    public int totalTransactions() { return totalTransactions; }

    /** Soma um resultado de venda (moeda -> total) as estatisticas do jogador. */
    public void registerSale(int itemsSold, java.util.Map<String, Double> totals) {
        this.totalItemsSold += itemsSold;
        this.totalTransactions += 1;
        this.totalSoldCoins += totals.getOrDefault("coins", 0.0);
        this.totalSoldEscarion += totals.getOrDefault("escarion", 0.0);
    }
}
