package com.alkacode.shop.menu;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class AbstractShopMenu implements ShopMenu {

    protected final Player viewer;
    protected final Inventory inventory;
    private final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();
    private int interactiveFrom = -1;
    private int interactiveTo = -1;

    protected AbstractShopMenu(Player viewer, Component title, int size) {
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, size, title);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    protected void setItem(int slot, ItemStack item) {
        setItem(slot, item, null);
    }

    protected void setItem(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        inventory.setItem(slot, item);
        if (action != null) {
            actions.put(slot, action);
        } else {
            actions.remove(slot);
        }
    }

    protected void markInteractive(int fromInclusive, int toInclusive) {
        this.interactiveFrom = fromInclusive;
        this.interactiveTo = toInclusive;
    }

    @Override
    public boolean isInteractiveSlot(int slot) {
        return slot >= interactiveFrom && slot <= interactiveTo;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        Consumer<InventoryClickEvent> action = actions.get(event.getSlot());
        if (action != null && event.getClickedInventory() == inventory) {
            action.accept(event);
        }
    }

    public void open() {
        viewer.openInventory(inventory);
    }
}
