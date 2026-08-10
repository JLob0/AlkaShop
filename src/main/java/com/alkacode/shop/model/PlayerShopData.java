package com.alkacode.shop.model;

import org.bukkit.Material;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PlayerShopData {

    private final UUID uuid;
    private boolean autoSellAll;
    private final Set<Material> autoSellMaterials;
    private double totalSoldCoins;
    private double totalSoldEscarion;
    private int totalItemsSold;
    private int totalTransactions;

    public PlayerShopData(UUID uuid, boolean autoSellAll, Set<Material> autoSellMaterials,
                           double totalSoldCoins, double totalSoldEscarion,
                           int totalItemsSold, int totalTransactions) {
        this.uuid = uuid;
        this.autoSellAll = autoSellAll;
        this.autoSellMaterials = autoSellMaterials != null ? new HashSet<>(autoSellMaterials) : new HashSet<>();
        this.totalSoldCoins = totalSoldCoins;
        this.totalSoldEscarion = totalSoldEscarion;
        this.totalItemsSold = totalItemsSold;
        this.totalTransactions = totalTransactions;
    }

    public UUID uuid() { return uuid; }

    public boolean autoSellAll() { return autoSellAll; }
    public void autoSellAll(boolean autoSellAll) { this.autoSellAll = autoSellAll; }

    public Set<Material> autoSellMaterials() { return new HashSet<>(autoSellMaterials); }

    /** true se o material for vendido automaticamente (ou por estar no "todos", ou individualmente escolhido). */
    public boolean isAutoSellEnabled(Material material) {
        return autoSellAll || autoSellMaterials.contains(material);
    }

    /** Ativa/desativa um material especifico. Se isso ligar o primeiro material, desliga "todos" automaticamente. */
    public void toggleMaterial(Material material) {
        if (autoSellMaterials.contains(material)) {
            autoSellMaterials.remove(material);
        } else {
            autoSellMaterials.add(material);
            if (autoSellAll) {
                autoSellAll = false;
            }
        }
    }

    public void clearMaterials() {
        autoSellMaterials.clear();
    }

    public double totalSoldCoins() { return totalSoldCoins; }
    public double totalSoldEscarion() { return totalSoldEscarion; }
    public int totalItemsSold() { return totalItemsSold; }
    public int totalTransactions() { return totalTransactions; }

    /** Soma um resultado de venda (moeda -> total) as estatisticas do jogador. */
    public void registerSale(int itemsSold, Map<String, Double> totals) {
        this.totalItemsSold += itemsSold;
        this.totalTransactions += 1;
        this.totalSoldCoins += totals.getOrDefault("coins", 0.0);
        this.totalSoldEscarion += totals.getOrDefault("escarion", 0.0);
    }
}
