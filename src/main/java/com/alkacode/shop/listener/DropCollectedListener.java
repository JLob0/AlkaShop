package com.alkacode.shop.listener;

import com.alkacode.drop.event.DropCollectedEvent;
import com.alkacode.shop.ShopServices;
import com.alkacode.shop.model.enums.SellType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * Integracao com AlkaDrop (softdepend) - se instalado, cada item coletado dispara
 * DropCollectedEvent antes de ir pro inventario. Se auto-venda estiver ativa e o
 * item for vendavel, marcamos sold=true e o AlkaDrop nao adiciona o item ao
 * inventario (ja foi convertido em moeda aqui).
 */
public final class DropCollectedListener implements Listener {

    private final ShopServices services;

    public DropCollectedListener(ShopServices services) {
        this.services = services;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDropCollected(DropCollectedEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
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
        event.setSold(true);
        AutoSellSupport.notify(services, player, totals);
    }
}
