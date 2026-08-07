package com.alkacode.shop.menu;

import com.alkacode.shop.ShopServices;
import com.alkacode.shop.model.enums.SellType;
import com.alkacode.shop.util.ItemUtil;
import com.alkacode.shop.util.PriceFormatter;
import com.alkacode.shop.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Bau virtual (54 slots): 0-44 e area livre pra depositar itens, 45-48/50-52 sao
 * preenchidos com vidro (nao especificados na especificacao, mas sem eles um item
 * colocado ali ficaria fora da varredura do CONFIRMAR sem o jogador perceber), 49
 * confirma a venda e 53 cancela devolvendo tudo.
 */
public final class VirtualSellMenu extends AbstractShopMenu {

    private static final int STORAGE_END = 44;

    private final ShopServices services;
    private int confirmSlot;
    private int cancelSlot;
    private boolean resolved;

    public VirtualSellMenu(Player viewer, ShopServices services) {
        super(viewer, TextUtil.parse(services.configManager.menus().getString("virtual-chest.title", "<dark_gray>Coloque os itens para vender")),
                services.configManager.menus().getInt("virtual-chest.size", 54));
        this.services = services;
        build();
    }

    private void build() {
        ConfigurationSection root = services.configManager.menus().getConfigurationSection("virtual-chest");
        confirmSlot = root != null ? root.getInt("confirm-slot", 49) : 49;
        cancelSlot = root != null ? root.getInt("cancel-slot", 53) : 53;

        markInteractive(0, STORAGE_END);

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        var meta = filler.getItemMeta();
        meta.displayName(TextUtil.parse(" "));
        filler.setItemMeta(meta);
        for (int slot = STORAGE_END + 1; slot < inventory.getSize(); slot++) {
            if (slot != confirmSlot && slot != cancelSlot) {
                setItem(slot, filler);
            }
        }

        if (root != null) {
            setItem(confirmSlot, ItemUtil.build(root.getConfigurationSection("confirm-item"), Map.of()), e -> confirm());
            setItem(cancelSlot, ItemUtil.build(root.getConfigurationSection("cancel-item"), Map.of()), e -> cancel());
        }
    }

    private void confirm() {
        resolved = true;
        List<ItemStack> toSell = new ArrayList<>();
        List<Integer> soldSlots = new ArrayList<>();
        for (int slot = 0; slot <= STORAGE_END; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && services.priceManager.isSellable(item.getType())) {
                toSell.add(item.clone());
                soldSlots.add(slot);
            }
        }

        if (toSell.isEmpty()) {
            services.sendMessage(viewer, "sell.no-items", Map.of());
            returnRemainingItems();
            viewer.closeInventory();
            return;
        }

        Map<String, Double> totals = services.sellManager.sell(viewer, toSell, SellType.SELECTED);
        for (int slot : soldSlots) {
            inventory.setItem(slot, null);
        }

        boolean round = services.configManager.config().getBoolean("selling.round-values", true);
        int decimals = services.configManager.config().getInt("selling.decimal-places", 2);
        services.sendMessage(viewer, "sell.sold-selected", Map.of("totals", PriceFormatter.formatTotals(totals, round, decimals)));

        returnRemainingItems();
        viewer.closeInventory();
    }

    private void cancel() {
        resolved = true;
        returnRemainingItems();
        viewer.closeInventory();
    }

    private void returnRemainingItems() {
        for (int slot = 0; slot <= STORAGE_END; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType().isAir()) {
                continue;
            }
            Map<Integer, ItemStack> leftover = viewer.getInventory().addItem(item);
            for (ItemStack drop : leftover.values()) {
                viewer.getWorld().dropItemNaturally(viewer.getLocation(), drop);
            }
            inventory.setItem(slot, null);
        }
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
        // Se o jogador fechou sem clicar em CONFIRMAR/CANCELAR (ex: tecla ESC), os
        // itens depositados nao podem ficar presos num inventario temporario.
        if (!resolved) {
            resolved = true;
            returnRemainingItems();
        }
    }
}
