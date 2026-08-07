package com.alkacode.shop.model;

import org.bukkit.Material;

import java.util.Map;
import java.util.Set;

public final class ShopPrice {

    private final Material material;
    private final Map<String, Double> prices;

    public ShopPrice(Material material, Map<String, Double> prices) {
        this.material = material;
        this.prices = prices;
    }

    public Material material() {
        return material;
    }

    public double getPrice(String currency) {
        return prices.getOrDefault(currency.toLowerCase(), 0.0);
    }

    public boolean hasPrice(String currency) {
        return getPrice(currency) > 0;
    }

    public Set<String> getCurrencies() {
        return prices.keySet();
    }

    public Map<String, Double> prices() {
        return prices;
    }
}
