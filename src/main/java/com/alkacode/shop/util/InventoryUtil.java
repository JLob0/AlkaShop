package com.alkacode.shop.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;

public final class InventoryUtil {

    private InventoryUtil() {
    }

    /**
     * Slots considerados por "vender tudo", respeitando os toggles de
     * config.yml (selling.sell-armor/offhand/hotbar). Storage (9-35) sempre entra.
     */
    public static List<Integer> sellableSlots(PlayerInventory inventory, boolean sellArmor, boolean sellOffhand, boolean sellHotbar) {
        List<Integer> slots = new ArrayList<>();
        int start = sellHotbar ? 0 : 9;
        for (int i = start; i <= 35; i++) {
            if (inventory.getItem(i) != null) {
                slots.add(i);
            }
        }
        if (sellOffhand && inventory.getItemInOffHand().getType() != org.bukkit.Material.AIR) {
            slots.add(40);
        }
        if (sellArmor) {
            for (int i = 36; i <= 39; i++) {
                if (inventory.getItem(i) != null) {
                    slots.add(i);
                }
            }
        }
        return slots;
    }

    public static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir();
    }
}
