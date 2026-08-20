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

    /**
     * true pros menus que esperam o jogador pegar item do PROPRIO inventario (bottom) pra
     * arrastar pro menu (ex: bau virtual, GUI de admin de precos) - sem isso o clique que
     * pega o item pro cursor seria cancelado e ele nunca conseguiria nem comecar a arrastar.
     * Menus que nao esperam nenhuma interacao no inventario do jogador devem manter false
     * (padrao) - com bottom liberado, um item shift-clicado poderia cair num slot vazio do
     * menu e ficar preso la se o menu nao devolver item nenhum ao fechar.
     */
    default boolean allowsBottomInventoryClicks() {
        return false;
    }
}
