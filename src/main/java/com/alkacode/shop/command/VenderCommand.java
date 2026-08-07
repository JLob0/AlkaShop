package com.alkacode.shop.command;

import com.alkacode.shop.ShopServices;
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
            case "vendertudo" -> sellAll(player);
            case "vendermao" -> sellHand(player);
            case "vendersel" -> new VirtualSellMenu(player, services).open();
            case "venderautomatico" -> toggleAutoSell(player);
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

    private void sellHand(Player player) {
        String itemName = player.getInventory().getItemInMainHand().getType().name();
        Map<String, Double> totals = services.sellManager.sellHand(player);
        if (totals == null || totals.isEmpty()) {
            services.sendMessage(player, "sell.not-sellable", Map.of());
            return;
        }
        services.sendMessage(player, "sell.sold-hand", Map.of("item", itemName, "totals", formatTotals(totals)));
    }

    private void toggleAutoSell(Player player) {
        if (!player.hasPermission("alkashop.autosell")) {
            services.sendMessage(player, "general.no-permission", Map.of());
            return;
        }
        PlayerShopData data = services.playerDataManager.get(player);
        data.autoSellEnabled(!data.autoSellEnabled());
        services.playerDataManager.save(data);
        String path = data.autoSellEnabled() ? "sell.auto-sell-enabled" : "sell.auto-sell-disabled";
        services.sendMessage(player, path, Map.of());
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
