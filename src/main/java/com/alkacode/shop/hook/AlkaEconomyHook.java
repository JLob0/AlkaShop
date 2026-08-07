package com.alkacode.shop.hook;

import com.alkacode.economy.AlkaEconomyPlugin;
import com.alkacode.economy.EconomyManager;
import org.bukkit.Bukkit;

import java.util.Locale;
import java.util.UUID;

public final class AlkaEconomyHook {

    private final EconomyManager economyManager;

    public AlkaEconomyHook(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    public static AlkaEconomyHook resolve() {
        AlkaEconomyPlugin plugin = (AlkaEconomyPlugin) Bukkit.getPluginManager().getPlugin("AlkaEconomy");
        if (plugin == null) {
            return null;
        }
        return new AlkaEconomyHook(plugin.getEconomyManager());
    }

    private String resolveCurrency(String name) {
        if (name == null) {
            return null;
        }
        String id = name.toLowerCase(Locale.ROOT);
        return economyManager.isValidCurrency(id) ? id : null;
    }

    public boolean isValidCurrency(String name) {
        return resolveCurrency(name) != null;
    }

    public void deposit(UUID uuid, String currency, double amount) {
        String id = resolveCurrency(currency);
        if (id != null) {
            economyManager.addBalance(uuid, id, amount);
        }
    }

    public String format(double amount) {
        return EconomyManager.formatValue(amount);
    }
}
