package com.alkacode.shop.menu;

import com.alkacode.shop.ShopServices;
import com.alkacode.shop.model.PlayerShopData;
import com.alkacode.shop.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Menu de configuracao granular da auto-venda - liga tudo de uma vez ou material por material, com filtro por categoria. */
public final class AutoSellConfigMenu extends AbstractShopMenu {

    private static final int ITEMS_PER_PAGE = 36;
    private static final int PREV_SLOT = 45;
    private static final int NEXT_SLOT = 53;
    private static final int TOGGLE_ALL_SLOT = 49;
    private static final int FILTER_SLOT = 47;

    private final ShopServices services;
    private final List<Material> allSellables;
    private int currentPage = 0;
    private String currentFilter = "all"; // "all" ou nome da categoria

    public AutoSellConfigMenu(Player viewer, ShopServices services) {
        super(viewer, TextUtil.parse("<dark_green><bold>Configurar Auto-Venda</bold></dark_green>"), 54);
        this.services = services;
        this.allSellables = new ArrayList<>(services.priceManager.allResolved().keySet());
        this.allSellables.sort(java.util.Comparator.comparing(Material::name));
        build();
    }

    private void build() {
        PlayerShopData data = services.playerDataManager.get(viewer);

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        var fMeta = filler.getItemMeta();
        fMeta.displayName(TextUtil.parse(" "));
        filler.setItemMeta(fMeta);
        for (int i = 36; i < 54; i++) {
            if (i != PREV_SLOT && i != NEXT_SLOT && i != TOGGLE_ALL_SLOT && i != FILTER_SLOT) {
                setItem(i, filler);
            }
        }

        boolean allEnabled = data.autoSellAll();
        ItemStack toggleAll = new ItemStack(allEnabled ? Material.LIME_WOOL : Material.RED_WOOL);
        var taMeta = toggleAll.getItemMeta();
        taMeta.displayName(TextUtil.parse(allEnabled
                ? "<green><bold>AUTO-VENDA: TODOS ATIVOS</bold></green>"
                : "<red><bold>AUTO-VENDA: TODOS DESATIVADOS</bold></red>"));
        taMeta.lore(List.of(
                TextUtil.parse("<gray>Clique para " + (allEnabled ? "desativar" : "ativar") + " todos os itens."),
                TextUtil.parse("<dark_gray>Requer permissao: alkashop.autosell.all")
        ));
        toggleAll.setItemMeta(taMeta);
        setItem(TOGGLE_ALL_SLOT, toggleAll, e -> {
            if (!viewer.hasPermission("alkashop.autosell.all")) {
                services.sendMessage(viewer, "general.no-permission", java.util.Map.of());
                return;
            }
            data.autoSellAll(!allEnabled);
            if (data.autoSellAll()) {
                data.clearMaterials();
            }
            services.playerDataManager.save(data);
            refresh();
        });

        ItemStack filter = new ItemStack(Material.HOPPER);
        var fiMeta = filter.getItemMeta();
        fiMeta.displayName(TextUtil.parse("<yellow><bold>Filtrar por categoria</bold></yellow>"));
        fiMeta.lore(List.of(
                TextUtil.parse("<gray>Atual: <white>" + (currentFilter.equals("all") ? "Todas" : currentFilter) + "</white>"),
                TextUtil.parse(" "),
                TextUtil.parse("<yellow>Clique para mudar")
        ));
        filter.setItemMeta(fiMeta);
        setItem(FILTER_SLOT, filter, e -> {
            cycleFilter();
            refresh();
        });

        renderPage();
    }

    private void cycleFilter() {
        Set<String> categories = services.priceManager.getAllCategories();
        List<String> options = new ArrayList<>();
        options.add("all");
        options.addAll(categories.stream().sorted().toList());

        int idx = options.indexOf(currentFilter);
        currentFilter = options.get((idx + 1) % options.size());
        currentPage = 0;
    }

    private void renderPage() {
        PlayerShopData data = services.playerDataManager.get(viewer);

        List<Material> filtered = new ArrayList<>();
        for (Material mat : allSellables) {
            if (currentFilter.equals("all") || currentFilter.equals(services.priceManager.getCategory(mat))) {
                filtered.add(mat);
            }
        }

        int totalPages = Math.max(1, (filtered.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        if (currentPage >= totalPages) {
            currentPage = totalPages - 1;
        }

        for (int i = 0; i < 36; i++) {
            setItem(i, null, null);
        }

        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, filtered.size());

        for (int i = start; i < end; i++) {
            Material mat = filtered.get(i);
            boolean enabled = data.isAutoSellEnabled(mat);
            String category = services.priceManager.getCategory(mat);
            String catDisplay = category.isBlank() ? "Geral" : category.substring(0, 1).toUpperCase() + category.substring(1);

            ItemStack icon = new ItemStack(mat);
            var meta = icon.getItemMeta();
            meta.displayName(TextUtil.parse((enabled ? "<green>" : "<red>") + mat.name()));
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            lore.add(TextUtil.parse("<gray>Categoria: <white>" + catDisplay));
            lore.add(TextUtil.parse("<gray>Status: " + (enabled ? "<green>ATIVADO" : "<red>DESATIVADO")));
            lore.add(TextUtil.parse(" "));
            lore.add(TextUtil.parse("<yellow>Clique para " + (enabled ? "desativar" : "ativar")));
            meta.lore(lore);
            icon.setItemMeta(meta);

            int slot = i - start;
            setItem(slot, icon, e -> {
                if (!viewer.hasPermission("alkashop.autosell")) {
                    services.sendMessage(viewer, "general.no-permission", java.util.Map.of());
                    return;
                }
                String cat = services.priceManager.getCategory(mat);
                if (!cat.isBlank() && !viewer.hasPermission("alkashop.autosell." + cat)) {
                    services.sendMessage(viewer, "general.no-permission", java.util.Map.of());
                    return;
                }
                data.toggleMaterial(mat);
                services.playerDataManager.save(data);
                renderPage();
            });
        }

        if (currentPage > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            var pMeta = prev.getItemMeta();
            pMeta.displayName(TextUtil.parse("<yellow>Pagina anterior"));
            prev.setItemMeta(pMeta);
            setItem(PREV_SLOT, prev, e -> {
                currentPage--;
                renderPage();
            });
        } else {
            setItem(PREV_SLOT, null, null);
        }

        if (currentPage < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            var nMeta = next.getItemMeta();
            nMeta.displayName(TextUtil.parse("<yellow>Proxima pagina"));
            next.setItemMeta(nMeta);
            setItem(NEXT_SLOT, next, e -> {
                currentPage++;
                renderPage();
            });
        } else {
            setItem(NEXT_SLOT, null, null);
        }
    }

    private void refresh() {
        viewer.closeInventory();
        new AutoSellConfigMenu(viewer, services).open();
    }
}
