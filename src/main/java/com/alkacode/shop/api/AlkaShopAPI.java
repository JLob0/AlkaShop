package com.alkacode.shop.api;

import com.alkacode.shop.model.ShopPlayerStats;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface AlkaShopAPI {

    /** Preco unitario de um material numa moeda. 0.0 se nao vendavel. */
    double getPrice(Material material, String currency);

    /** true se o material tiver preco em qualquer moeda. */
    boolean isSellable(Material material);

    /** Vende um ItemStack para um jogador - nao remove o item de lugar nenhum, so paga e retorna os totais. */
    Map<String, Double> sellItem(Player player, ItemStack item);

    /** Vende uma lista de ItemStacks - mesma semantica de {@link #sellItem}. */
    Map<String, Double> sellItems(Player player, List<ItemStack> items);

    CompletableFuture<ShopPlayerStats> getStats(UUID playerUuid);

    /**
     * true se a auto-venda deveria se aplicar a este jogador agora - considera
     * force-autosell, o toggle pessoal (/venderautomatico) e a permissao
     * alkashop.autosell. Pensado pra integracoes de terceiros (ex: AlkaMines) que
     * dao itens diretamente ao jogador e querem respeitar a auto-venda sem
     * conhecer nenhum preco (a mina nunca sabe o preco de nada, so pergunta "devo
     * vender isso pra este jogador?").
     */
    boolean isAutoSellActive(Player player);
}
