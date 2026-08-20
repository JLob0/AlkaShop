package com.alkacode.shop.listener;

import com.alkacode.shop.menu.ShopMenu;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class ShopMenuListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ShopMenu menu)) {
            return;
        }
        boolean clickedTop = event.getClickedInventory() == event.getInventory();
        if (!clickedTop) {
            // Clique no inventario do PROPRIO jogador - so cancela se o menu nao pediu pra
            // liberar (ver ShopMenu#allowsBottomInventoryClicks). Cancelar aqui sempre
            // impediria ate o primeiro clique que pega o item pro cursor, e nenhum menu
            // conseguiria receber item arrastado do inventario do jogador.
            if (!menu.allowsBottomInventoryClicks()) {
                event.setCancelled(true);
            }
            return;
        }
        if (menu.isInteractiveSlot(event.getSlot())) {
            return;
        }
        event.setCancelled(true);
        menu.handleClick(event);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof ShopMenu menu)) {
            return;
        }
        int topSize = event.getInventory().getSize();
        boolean allInteractive = event.getRawSlots().stream()
                .allMatch(slot -> slot >= topSize || menu.isInteractiveSlot(slot));
        if (!allInteractive) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof ShopMenu menu) {
            menu.handleClose(event);
        }
    }
}
