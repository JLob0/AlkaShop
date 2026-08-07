package com.alkacode.shop.event;

import org.bukkit.Material;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Disparado a cada consulta de preco resolvido - outros plugins podem ajustar {@link #setPrice(double)}. */
public class PriceLookupEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Material material;
    private final String currency;
    private double price;

    public PriceLookupEvent(Material material, String currency, double price) {
        this.material = material;
        this.currency = currency;
        this.price = price;
    }

    public Material getMaterial() { return material; }
    public String getCurrency() { return currency; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
