package com.alkacode.shop.menu;

import com.alkacode.shop.ShopServices;
import com.alkacode.shop.model.PlayerShopData;
import com.alkacode.shop.util.ItemUtil;
import com.alkacode.shop.util.PriceFormatter;
import com.alkacode.shop.util.TextUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.function.Consumer;

public final class MainSellMenu extends AbstractShopMenu {

    private final ShopServices services;

    public MainSellMenu(Player viewer, ShopServices services) {
        super(viewer, TextUtil.parse(services.configManager.menus().getString("main-menu.title", "<dark_gray>Vender itens")),
                services.configManager.menus().getInt("main-menu.size", 27));
        this.services = services;
        build();
    }

    private void build() {
        ConfigurationSection items = services.configManager.menus().getConfigurationSection("main-menu.items");
        if (items == null) {
            return;
        }

        placeButton(items.getConfigurationSection("selected"), Map.of(), e -> {
            viewer.closeInventory();
            new VirtualSellMenu(viewer, services).open();
        });
        placeButton(items.getConfigurationSection("all"), Map.of(), e -> {
            viewer.closeInventory();
            sellAll();
        });
        placeButton(items.getConfigurationSection("hand"), Map.of(), e -> {
            viewer.closeInventory();
            sellHand();
        });

        if (viewer.hasPermission("alkashop.autosell")) {
            PlayerShopData data = services.playerDataManager.get(viewer);
            boolean allEnabled = data.autoSellAll();
            int activeCount = data.autoSellMaterials().size();
            String statusText = allEnabled ? "TODOS ATIVOS" : (activeCount > 0 ? activeCount + " ITENS" : "DESATIVADO");

            Map<String, String> placeholders = Map.of("status", statusText);
            placeButton(items.getConfigurationSection("auto-sell"), placeholders, e -> {
                viewer.closeInventory();
                new AutoSellConfigMenu(viewer, services).open();
            });
        }
    }

    private void placeButton(ConfigurationSection section, Map<String, String> placeholders,
                              Consumer<org.bukkit.event.inventory.InventoryClickEvent> action) {
        if (section == null) {
            return;
        }
        int slot = section.getInt("slot", -1);
        if (slot < 0) {
            return;
        }
        ItemStack item = ItemUtil.build(section, placeholders);
        setItem(slot, item, action);
    }

    private void sellAll() {
        Map<String, Double> totals = services.sellManager.sellAllFromInventory(viewer);
        if (totals.isEmpty()) {
            services.sendMessage(viewer, "sell.no-items", Map.of());
            return;
        }
        boolean round = services.configManager.config().getBoolean("selling.round-values", true);
        int decimals = services.configManager.config().getInt("selling.decimal-places", 2);
        services.sendMessage(viewer, "sell.sold-all", Map.of("totals", PriceFormatter.formatTotals(totals, round, decimals)));
    }

    private void sellHand() {
        String itemName = viewer.getInventory().getItemInMainHand().getType().name();
        Map<String, Double> totals = services.sellManager.sellHand(viewer);
        if (totals == null || totals.isEmpty()) {
            services.sendMessage(viewer, "sell.not-sellable", Map.of());
            return;
        }
        boolean round = services.configManager.config().getBoolean("selling.round-values", true);
        int decimals = services.configManager.config().getInt("selling.decimal-places", 2);
        services.sendMessage(viewer, "sell.sold-hand", Map.of(
                "item", itemName,
                "totals", PriceFormatter.formatTotals(totals, round, decimals)));
    }
}
