package com.alkacode.shop.event;

import com.alkacode.shop.model.enums.SellType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ItemSellEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Material material;
    private final int amount;
    private final Map<String, Double> totals;
    private final SellType sellType;
    private boolean cancelled;

    public ItemSellEvent(Player player, Material material, int amount, Map<String, Double> totals, SellType sellType) {
        this.player = player;
        this.material = material;
        this.amount = amount;
        this.totals = totals;
        this.sellType = sellType;
    }

    public Player getPlayer() { return player; }
    public Material getMaterial() { return material; }
    public int getAmount() { return amount; }
    public Map<String, Double> getTotals() { return totals; }
    public SellType getSellType() { return sellType; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancel) { this.cancelled = cancel; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
