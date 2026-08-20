package com.alkacode.shop.menu;

import com.alkacode.shop.ShopServices;
import com.alkacode.shop.model.PlayerShopData;
import com.alkacode.shop.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Grade paginada de materiais de UMA categoria (ou "outros", para materiais sem categoria), com toggle individual. */
public final class AutoSellCategoryMenu extends AbstractShopMenu {

    private static final int ITEMS_PER_PAGE = 36;
    private static final int BACK_SLOT = 45;
    private static final int PREV_SLOT = 48;
    private static final int NEXT_SLOT = 50;

    private final ShopServices services;
    private final String category;
    private final List<Material> materials;
    private int currentPage = 0;

    public AutoSellCategoryMenu(Player viewer, ShopServices services, String category, String displayName) {
        super(viewer, TextUtil.parse("<#55FF55><b>✦ Auto-Venda</b></#55FF55> <dark_gray>» </dark_gray>" + displayName), 54);
        this.services = services;
        this.category = category;
        this.materials = new ArrayList<>();
        for (Material mat : services.priceManager.allResolved().keySet()) {
            String matCategory = services.priceManager.getCategory(mat);
            boolean matches = "outros".equals(category) ? matCategory.isBlank() : matCategory.equals(category);
            if (matches) {
                materials.add(mat);
            }
        }
        materials.sort(java.util.Comparator.comparing(Material::name));
        build();
    }

    private void build() {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        var fMeta = filler.getItemMeta();
        fMeta.displayName(TextUtil.parse(" "));
        filler.setItemMeta(fMeta);
        for (int i = 36; i < 54; i++) {
            if (i != BACK_SLOT && i != PREV_SLOT && i != NEXT_SLOT) {
                setItem(i, filler);
            }
        }

        ItemStack back = new ItemStack(Material.ARROW);
        var bMeta = back.getItemMeta();
        bMeta.displayName(TextUtil.parse("<#55FF55>◀ Voltar as categorias"));
        back.setItemMeta(bMeta);
        setItem(BACK_SLOT, back, e -> {
            viewer.closeInventory();
            new AutoSellConfigMenu(viewer, services).open();
        });

        renderPage();
    }

    private void renderPage() {
        PlayerShopData data = services.playerDataManager.get(viewer);

        int totalPages = Math.max(1, (materials.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        if (currentPage >= totalPages) {
            currentPage = totalPages - 1;
        }

        for (int i = 0; i < 36; i++) {
            setItem(i, null, null);
        }

        if (materials.isEmpty()) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            var meta = empty.getItemMeta();
            meta.displayName(TextUtil.parse("<red>Nenhum item nesta categoria"));
            empty.setItemMeta(meta);
            setItem(4, empty);
        }

        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, materials.size());

        for (int i = start; i < end; i++) {
            Material mat = materials.get(i);
            boolean enabled = data.isAutoSellEnabled(mat);

            ItemStack icon = new ItemStack(mat);
            var meta = icon.getItemMeta();
            meta.displayName(TextUtil.parse((enabled ? "<#55FF55>✦ " : "<gray>") + mat.name()));
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            lore.add(TextUtil.parse(enabled
                    ? "<gray>Status: <#55FF55><b>ATIVADO</b></#55FF55>"
                    : "<gray>Status: <dark_gray><b>DESATIVADO</b></dark_gray>"));
            lore.add(TextUtil.parse(enabled
                    ? "<gray>Todo drop desse item vira moeda na hora."
                    : "<gray>Esse item vai pro seu inventario normalmente."));
            lore.add(TextUtil.parse(" "));
            lore.add(TextUtil.parse("<yellow>Clique para " + (enabled ? "desativar" : "ativar")));
            meta.lore(lore);
            meta.addItemFlags(ItemFlag.values());
            icon.setItemMeta(meta);

            int slot = i - start;
            setItem(slot, icon, e -> {
                if (!viewer.hasPermission("alkashop.autosell")) {
                    services.sendMessage(viewer, "general.no-permission", Map.of());
                    return;
                }
                if (!"outros".equals(category) && !viewer.hasPermission("alkashop.autosell." + category)) {
                    services.sendMessage(viewer, "general.no-permission", Map.of());
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
            pMeta.displayName(TextUtil.parse("<yellow>◀ Pagina anterior"));
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
            nMeta.displayName(TextUtil.parse("<yellow>Proxima pagina ▶"));
            next.setItemMeta(nMeta);
            setItem(NEXT_SLOT, next, e -> {
                currentPage++;
                renderPage();
            });
        } else {
            setItem(NEXT_SLOT, null, null);
        }
    }
}
