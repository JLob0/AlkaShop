package com.alkacode.shop.command;

import com.alkacode.shop.ShopServices;
import com.alkacode.shop.menu.AutoSellConfigMenu;
import com.alkacode.shop.menu.MainSellMenu;
import com.alkacode.shop.menu.VirtualSellMenu;
import com.alkacode.shop.model.PlayerShopData;
import com.alkacode.shop.util.PriceFormatter;
import com.alkacode.shop.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public final class VenderCommand implements CommandExecutor {

    private final ShopServices services;

    public VenderCommand(ShopServices services) {
        this.services = services;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("venderpreco")) {
            return handlePriceInfo(sender, args);
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.parse(services.configManager.prefix() + services.configManager.message("general.player-only")));
            return true;
        }
        if (!player.hasPermission("alkashop.use")) {
            services.sendMessage(player, "general.no-permission", Map.of());
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "vender" -> new MainSellMenu(player, services).open();
            case "vendertudo" -> {
                if (args.length >= 1) {
                    sellCategory(player, args[0]);
                } else {
                    sellAll(player);
                }
            }
            case "vendermao" -> sellHand(player);
            case "vendersel" -> new VirtualSellMenu(player, services).open();
            case "venderautomatico" -> handleAutoSell(player, args);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void sellAll(Player player) {
        Map<String, Double> totals = services.sellManager.sellAllFromInventory(player);
        if (totals.isEmpty()) {
            services.sendMessage(player, "sell.no-items", Map.of());
            return;
        }
        services.sendMessage(player, "sell.sold-all", Map.of("totals", formatTotals(totals)));
    }

    private void sellCategory(Player player, String category) {
        Map<String, Double> totals = services.sellManager.sellAllByCategory(player, category);
        if (totals.isEmpty()) {
            services.sendMessage(player, "sell.no-items-category", Map.of("category", category));
            return;
        }
        services.sendMessage(player, "sell.sold-category", Map.of("category", category, "totals", formatTotals(totals)));
    }

    private void sellHand(Player player) {
        String itemName = player.getInventory().getItemInMainHand().getType().name();
        Map<String, Double> totals = services.sellManager.sellHand(player);
        if (totals == null || totals.isEmpty()) {
            services.sendMessage(player, "sell.not-sellable", Map.of());
            return;
        }
        services.sendMessage(player, "sell.sold-hand", Map.of("item", itemName, "totals", formatTotals(totals)));
    }

    private void handleAutoSell(Player player, String[] args) {
        if (!player.hasPermission("alkashop.autosell")) {
            services.sendMessage(player, "general.no-permission", Map.of());
            return;
        }

        PlayerShopData data = services.playerDataManager.get(player);

        if (args.length == 0) {
            new AutoSellConfigMenu(player, services).open();
            return;
        }

        switch (args[0].toLowerCase()) {
            case "todos", "all" -> {
                if (!player.hasPermission("alkashop.autosell.all")) {
                    services.sendMessage(player, "general.no-permission", Map.of());
                    return;
                }
                data.autoSellAll(!data.autoSellAll());
                if (data.autoSellAll()) {
                    data.clearMaterials();
                }
                services.playerDataManager.save(data);
                services.sendMessage(player, data.autoSellAll() ? "sell.auto-sell-all-enabled" : "sell.auto-sell-all-disabled", Map.of());
            }
            case "nenhum", "none", "limpar" -> {
                data.autoSellAll(false);
                data.clearMaterials();
                services.playerDataManager.save(data);
                services.sendMessage(player, "sell.auto-sell-cleared", Map.of());
            }
            default -> {
                Material material = Material.matchMaterial(args[0].toUpperCase());
                if (material == null || !services.priceManager.isSellable(material)) {
                    services.sendMessage(player, "sell.unknown-material", Map.of("material", args[0]));
                    return;
                }
                String category = services.priceManager.getCategory(material);
                if (!category.isBlank() && !player.hasPermission("alkashop.autosell." + category)) {
                    services.sendMessage(player, "general.no-permission", Map.of());
                    return;
                }
                data.toggleMaterial(material);
                services.playerDataManager.save(data);
                boolean enabled = data.isAutoSellEnabled(material);
                services.sendMessage(player, enabled ? "sell.auto-sell-material-enabled" : "sell.auto-sell-material-disabled",
                        Map.of("material", material.name()));
            }
        }
    }

    private boolean handlePriceInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("alkashop.use")) {
            sender.sendMessage(TextUtil.parse(services.configManager.prefix() + services.configManager.message("general.no-permission")));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(TextUtil.parse(services.configManager.prefix() + services.configManager.message("general.invalid-usage"),
                    Map.of("usage", "/venderpreco <material>")));
            return true;
        }
        Material material = Material.matchMaterial(args[0].toUpperCase());
        if (material == null) {
            sender.sendMessage(TextUtil.parse(services.configManager.prefix() + services.configManager.message("general.unknown-material"),
                    Map.of("value", args[0])));
            return true;
        }
        Map<String, Double> prices = services.priceManager.resolvedPrices(material);
        if (prices.isEmpty()) {
            sender.sendMessage(TextUtil.parse(services.configManager.prefix() + services.configManager.message("sell.no-price")));
            return true;
        }
        boolean round = services.configManager.config().getBoolean("selling.round-values", true);
        int decimals = services.configManager.config().getInt("selling.decimal-places", 2);

        sender.sendMessage(TextUtil.parse(services.configManager.message("sell.price-info-header"),
                Map.of("material", material.name())));
        for (Map.Entry<String, Double> entry : prices.entrySet()) {
            sender.sendMessage(TextUtil.parse(services.configManager.message("sell.price-line"), Map.of(
                    "currency", entry.getKey(), "value", PriceFormatter.format(entry.getValue(), round, decimals))));
        }
        return true;
    }

    private String formatTotals(Map<String, Double> totals) {
        boolean round = services.configManager.config().getBoolean("selling.round-values", true);
        int decimals = services.configManager.config().getInt("selling.decimal-places", 2);
        return PriceFormatter.formatTotals(totals, round, decimals);
    }
}
