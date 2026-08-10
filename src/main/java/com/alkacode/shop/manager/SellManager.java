package com.alkacode.shop.manager;

import com.alkacode.shop.config.ConfigManager;
import com.alkacode.shop.event.ItemSellEvent;
import com.alkacode.shop.hook.AlkaEconomyHook;
import com.alkacode.shop.model.PlayerShopData;
import com.alkacode.shop.model.enums.SellType;
import com.alkacode.shop.util.InventoryUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Nucleo da venda: computa totais, deposita via AlkaEconomy, dispara ItemSellEvent
 * (agrupado por Material) e atualiza estatisticas - NAO mexe em nenhum inventario,
 * quem chama e responsavel por remover os itens de onde quer que estejam (inventario,
 * mao, bau virtual). Isso deixa o mesmo metodo reutilizavel pela AlkaShopAPI publica.
 */
public final class SellManager {

    private final PriceManager priceManager;
    private final PlayerDataManager playerDataManager;
    private final AlkaEconomyHook economyHook;
    private final ConfigManager configManager;

    public SellManager(PriceManager priceManager, PlayerDataManager playerDataManager, AlkaEconomyHook economyHook,
                        ConfigManager configManager) {
        this.priceManager = priceManager;
        this.playerDataManager = playerDataManager;
        this.economyHook = economyHook;
        this.configManager = configManager;
    }

    /** /vendertudo - varre e remove os itens vendaveis do inventario (respeitando os toggles de config.yml). */
    public Map<String, Double> sellAllFromInventory(Player player) {
        PlayerInventory inventory = player.getInventory();
        boolean sellArmor = configManager.config().getBoolean("selling.sell-armor", false);
        boolean sellOffhand = configManager.config().getBoolean("selling.sell-offhand", true);
        boolean sellHotbar = configManager.config().getBoolean("selling.sell-hotbar", true);

        List<Integer> slots = InventoryUtil.sellableSlots(inventory, sellArmor, sellOffhand, sellHotbar);
        List<ItemStack> toSell = new ArrayList<>();
        List<Integer> soldSlots = new ArrayList<>();
        for (int slot : slots) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && priceManager.isSellable(item.getType())) {
                toSell.add(item.clone());
                soldSlots.add(slot);
            }
        }
        if (toSell.isEmpty()) {
            return Map.of();
        }

        Map<String, Double> totals = sell(player, toSell, SellType.ALL);
        if (!totals.isEmpty()) {
            for (int slot : soldSlots) {
                inventory.setItem(slot, null);
            }
        }
        return totals;
    }

    /** /vendertudo <categoria> - varre e remove so os itens vendaveis dessa categoria do inventario. */
    public Map<String, Double> sellAllByCategory(Player player, String category) {
        Set<Material> catMaterials = priceManager.getMaterialsByCategory(category);
        if (catMaterials.isEmpty()) {
            return Map.of();
        }

        PlayerInventory inventory = player.getInventory();
        boolean sellArmor = configManager.config().getBoolean("selling.sell-armor", false);
        boolean sellOffhand = configManager.config().getBoolean("selling.sell-offhand", true);
        boolean sellHotbar = configManager.config().getBoolean("selling.sell-hotbar", true);

        List<Integer> slots = InventoryUtil.sellableSlots(inventory, sellArmor, sellOffhand, sellHotbar);
        List<ItemStack> toSell = new ArrayList<>();
        List<Integer> soldSlots = new ArrayList<>();
        for (int slot : slots) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && catMaterials.contains(item.getType()) && priceManager.isSellable(item.getType())) {
                toSell.add(item.clone());
                soldSlots.add(slot);
            }
        }
        if (toSell.isEmpty()) {
            return Map.of();
        }

        Map<String, Double> totals = sell(player, toSell, SellType.ALL);
        if (!totals.isEmpty()) {
            for (int slot : soldSlots) {
                inventory.setItem(slot, null);
            }
        }
        return totals;
    }

    /** /vendermao - vende o item na mao principal. Retorna null se nao houver item ou nao for vendavel. */
    public Map<String, Double> sellHand(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (InventoryUtil.isEmpty(hand) || !priceManager.isSellable(hand.getType())) {
            return null;
        }
        Map<String, Double> totals = sell(player, List.of(hand.clone()), SellType.HAND);
        if (!totals.isEmpty()) {
            player.getInventory().setItemInMainHand(null);
        }
        return totals;
    }

    public Map<String, Double> sell(Player player, List<ItemStack> items, SellType sellType) {
        Map<Material, Integer> counts = new LinkedHashMap<>();
        for (ItemStack item : items) {
            if (InventoryUtil.isEmpty(item) || !priceManager.isSellable(item.getType())) {
                continue;
            }
            counts.merge(item.getType(), item.getAmount(), Integer::sum);
        }

        Map<String, Double> totals = new LinkedHashMap<>();
        int itemsSold = 0;

        for (Map.Entry<Material, Integer> entry : counts.entrySet()) {
            Material material = entry.getKey();
            int amount = entry.getValue();

            Map<String, Double> itemTotals = new LinkedHashMap<>();
            for (String currency : priceManager.resolvedPrices(material).keySet()) {
                double unitPrice = priceManager.getPrice(material, currency);
                if (unitPrice > 0) {
                    itemTotals.put(currency, unitPrice * amount);
                }
            }
            if (itemTotals.isEmpty()) {
                continue;
            }

            ItemSellEvent event = new ItemSellEvent(player, material, amount, itemTotals, sellType);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                continue;
            }

            for (Map.Entry<String, Double> currencyEntry : event.getTotals().entrySet()) {
                totals.merge(currencyEntry.getKey(), currencyEntry.getValue(), Double::sum);
            }
            itemsSold += amount;
        }

        if (itemsSold == 0) {
            return totals;
        }

        for (Map.Entry<String, Double> entry : totals.entrySet()) {
            if (economyHook != null) {
                economyHook.deposit(player.getUniqueId(), entry.getKey(), entry.getValue());
            }
        }

        PlayerShopData data = playerDataManager.get(player);
        data.registerSale(itemsSold, totals);
        playerDataManager.save(data);

        return totals;
    }
}
