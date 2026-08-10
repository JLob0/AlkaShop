package com.alkacode.shop.listener;

import com.alkacode.shop.ShopServices;
import com.alkacode.shop.model.enums.SellType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * Fallback de auto-venda para quando AlkaDrop nao esta instalado (ou pro item que
 * veio do chao por qualquer outro motivo - bau, drop natural, etc). Sem anotacao
 * {@code @EventHandler} - registrado com prioridade dinamica (config
 * `event-priority`) pelo AlkaShopPlugin, ja que a anotacao exige uma constante em
 * tempo de compilacao.
 */
public final class PlayerPickupListener implements Listener {

    private final ShopServices services;

    public PlayerPickupListener(ShopServices services) {
        this.services = services;
    }

    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack item = event.getItem().getItemStack();
        if (!AutoSellSupport.isActive(services, player, item.getType())) {
            return;
        }

        if (!services.priceManager.isSellable(item.getType())) {
            return;
        }

        Map<String, Double> totals = services.sellManager.sell(player, List.of(item.clone()), SellType.AUTO);
        if (totals.isEmpty()) {
            return;
        }

        event.setCancelled(true);
        event.getItem().remove();
        AutoSellSupport.notify(services, player, totals);
    }
}
