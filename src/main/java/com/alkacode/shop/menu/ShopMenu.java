package com.alkacode.shop.menu;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

public interface ShopMenu extends InventoryHolder {

    void handleClick(InventoryClickEvent event);

    default void handleClose(InventoryCloseEvent event) {
    }

    /**
     * Slots onde o jogador pode livremente colocar/tirar itens (ex: area de deposito
     * do bau virtual de venda) - clique nesses slots NAO e cancelado pelo listener
     * global. Todo o resto do menu (botoes) continua bloqueado por padrao.
     */
    default boolean isInteractiveSlot(int slot) {
        return false;
    }
}
