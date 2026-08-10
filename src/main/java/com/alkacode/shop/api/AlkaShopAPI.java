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
     * true se o jogador tiver auto-venda ativa em QUALQUER capacidade agora (tudo
     * ligado, ou pelo menos um material especifico escolhido) - considera
     * force-autosell e a permissao alkashop.autosell. Uso pensado pra integracoes de
     * terceiros que so querem saber "esse jogador usa auto-venda?" sem material
     * especifico em maos ainda. Pra decidir se um DROP especifico deve ser vendido,
     * prefira {@link #isAutoSellActive(Player, Material)}.
     */
    boolean isAutoSellActive(Player player);

    /**
     * true se a auto-venda deveria se aplicar a este jogador para ESTE material agora -
     * considera force-autosell, o toggle "vender tudo", o material especifico escolhido
     * (/venderautomatico <material>), a permissao alkashop.autosell e a permissao de
     * categoria (alkashop.autosell.<categoria>, se o material tiver uma). Pensado pra
     * integracoes de terceiros (ex: AlkaMines) que dao itens diretamente ao jogador e
     * querem respeitar a auto-venda sem conhecer nenhum preco (a mina nunca sabe o
     * preco de nada, so pergunta "devo vender isso pra este jogador?").
     */
    boolean isAutoSellActive(Player player, Material material);
}
