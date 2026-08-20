package com.alkacode.shop.command;

import com.alkacode.shop.ShopServices;
import com.alkacode.shop.menu.AdminPriceHubMenu;
import com.alkacode.shop.model.PlayerShopData;
import com.alkacode.shop.util.PriceFormatter;
import com.alkacode.shop.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public final class ShopAdminCommand implements CommandExecutor {

    private final ShopServices services;

    public ShopAdminCommand(ShopServices services) {
        this.services = services;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(TextUtil.parse(services.configManager.prefix() + "<red>Uso: /alkashop <reload|setprice|removeprice|gui|debug|toggle|info>"));
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "reload" -> reload(sender);
            case "setprice" -> setPrice(sender, args);
            case "removeprice" -> removePrice(sender, args);
            case "gui" -> gui(sender);
            case "debug" -> debug(sender);
            case "toggle" -> toggle(sender, args);
            case "info" -> info(sender, args);
            default -> {
                sender.sendMessage(TextUtil.parse(services.configManager.prefix() + "<red>Uso: /alkashop <reload|setprice|removeprice|gui|debug|toggle|info>"));
                yield true;
            }
        };
    }

    private boolean gui(CommandSender sender) {
        if (!sender.hasPermission("alkashop.admin.setprice")) {
            sender.sendMessage(TextUtil.parse(services.configManager.prefix() + services.configManager.message("general.no-permission")));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(TextUtil.parse(services.configManager.prefix() + services.configManager.message("general.player-only")));
            return true;
        }
        new AdminPriceHubMenu(player, services).open();
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission("alkashop.admin.reload")) {
            sender.sendMessage(TextUtil.parse(services.configManager.prefix() + services.configManager.message("general.no-permission")));
            return true;
        }
        services.configManager.reload();
        services.priceManager.load();
        sender.sendMessage(TextUtil.parse(services.configManager.prefix() + services.configManager.message("general.reloaded")));
        return true;
    }

    private boolean setPrice(CommandSender sender, String[] args) {
        if (!sender.hasPermission("alkashop.admin.setprice")) {
            sender.sendMessage(TextUtil.parse(services.configManager.prefix() + services.configManager.message("general.no-permission")));
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage(TextUtil.parse(services.configManager.prefix() + "<red>Uso: /alkashop setprice <material> <moeda> <valor>"));
            return true;
        }
        Material material = Material.matchMaterial(args[1].toUpperCase());
        if (material == null) {
            sender.sendMessage(TextUtil.parse(services.configManager.prefix() + services.configManager.message("general.unknown-material"),
                    Map.of("value", args[1])));
            return true;
        }
        double value;
        try {
            value = Double.parseDouble(args[3].replace(",", "."));
        } catch (NumberFormatException e) {
            sender.sendMessage(TextUtil.parse(services.configManager.prefix() + services.configManager.message("general.invalid-amount"),
                    Map.of("value", args[3])));
            return true;
        }
        services.priceManager.setPrice(material, args[2], value);
        sender.sendMessage(TextUtil.parse(services.configManager.prefix() + services.configManager.message("admin.price-set"), Map.of(
                "material", material.name(), "currency", args[2].toLowerCase(), "value", PriceFormatter.format(value, true, 2))));
        return true;
    }

    private boolean removePrice(CommandSender sender, String[] args) {
        if (!sender.hasPermission("alkashop.admin.setprice")) {
            sender.sendMessage(TextUtil.parse(services.configManager.prefix() + services.configManager.message("general.no-permission")));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(TextUtil.parse(services.configManager.prefix() + "<red>Uso: /alkashop removeprice <material> <moeda>"));
            return true;
        }
        Material material = Material.matchMaterial(args[1].toUpperCase());
        if (material == null) {
            sender.sendMessage(TextUtil.parse(services.configManager.prefix() + services.configManager.message("general.unknown-material"),
                    Map.of("value", args[1])));
            return true;
        }
        services.priceManager.removePrice(material, args[2]);
        sender.sendMessage(TextUtil.parse(services.configManager.prefix() + services.configManager.message("admin.price-removed"),
                Map.of("material", material.name(), "currency", args[2].toLowerCase())));
        return true;
    }

    private boolean debug(CommandSender sender) {
        if (!sender.hasPermission("alkashop.admin.debug")) {
            sender.sendMessage(TextUtil.parse(services.configManager.prefix() + services.configManager.message("general.no-permission")));
            return true;
        }
        sender.sendMessage(TextUtil.parse(services.configManager.message("admin.debug-header"),
                Map.of("count", String.valueOf(services.priceManager.resolvedCount()))));
        for (var entry : services.priceManager.allResolved().entrySet()) {
            sender.sendMessage(TextUtil.parse("<gray>- <white>" + entry.getKey().name() + "</white>: <yellow>" + entry.getValue()));
        }
        return true;
    }

    private boolean toggle(CommandSender sender, String[] args) {
        if (!sender.hasPermission("alkashop.admin.others")) {
            sender.sendMessage(TextUtil.parse(services.configManager.prefix() + services.configManager.message("general.no-permission")));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(TextUtil.parse(services.configManager.prefix() + "<red>Uso: /alkashop toggle <jogador>"));
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        PlayerShopData data = services.playerDataManager.get(target.getUniqueId());
        data.autoSellAll(!data.autoSellAll());
        if (data.autoSellAll()) {
            data.clearMaterials();
        }
        services.playerDataManager.save(data);
        sender.sendMessage(TextUtil.parse(services.configManager.prefix() + services.configManager.message("admin.toggle-success"), Map.of(
                "name", String.valueOf(target.getName()), "status", data.autoSellAll() ? "ATIVADA" : "DESATIVADA")));
        return true;
    }

    private boolean info(CommandSender sender, String[] args) {
        if (!sender.hasPermission("alkashop.admin.info")) {
            sender.sendMessage(TextUtil.parse(services.configManager.prefix() + services.configManager.message("general.no-permission")));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(TextUtil.parse(services.configManager.prefix() + "<red>Uso: /alkashop info <jogador>"));
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        PlayerShopData data = services.playerDataManager.get(target.getUniqueId());
        boolean round = services.configManager.config().getBoolean("selling.round-values", true);
        int decimals = services.configManager.config().getInt("selling.decimal-places", 2);

        String autoSellStatus = data.autoSellAll() ? "TODOS"
                : data.autoSellMaterials().isEmpty() ? "DESATIVADA" : data.autoSellMaterials().size() + " ITENS";

        sender.sendMessage(TextUtil.parse(services.configManager.message("admin.info-header"),
                Map.of("name", String.valueOf(target.getName()))));
        for (var line : TextUtil.parseLines(services.configManager.message("admin.info-line"), Map.of(
                "autosell", autoSellStatus,
                "coins", PriceFormatter.format(data.totalSoldCoins(), round, decimals),
                "escarion", PriceFormatter.format(data.totalSoldEscarion(), round, decimals),
                "items", String.valueOf(data.totalItemsSold()),
                "transactions", String.valueOf(data.totalTransactions())))) {
            sender.sendMessage(line);
        }
        return true;
    }
}
