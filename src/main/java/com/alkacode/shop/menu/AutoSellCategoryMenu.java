package com.alkacode.shop.menu;

import com.alkacode.shop.ShopServices;
import com.alkacode.shop.model.PlayerShopData;
import com.alkacode.shop.util.ItemUtil;
import com.alkacode.shop.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
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
        super(viewer, TextUtil.parse(services.configManager.menus().getString("auto-sell-category.title",
                "<#55FF55><b>✦ Auto-Venda</b></#55FF55> <dark_gray>» </dark_gray><category>"), Map.of("category", displayName)), 54);
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
        ItemStack filler = ItemUtil.build(services.configManager.menus().getConfigurationSection("common.filler"), Map.of());
        for (int i = 36; i < 54; i++) {
            if (i != BACK_SLOT && i != PREV_SLOT && i != NEXT_SLOT) {
                setItem(i, filler);
            }
        }

        ItemStack back = ItemUtil.build(services.configManager.menus().getConfigurationSection("auto-sell-category.back-button"), Map.of());
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
            ItemStack empty = ItemUtil.build(services.configManager.menus().getConfigurationSection("auto-sell-category.empty-state"), Map.of());
            setItem(4, empty);
        }

        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, materials.size());

        for (int i = start; i < end; i++) {
            Material mat = materials.get(i);
            boolean enabled = data.isAutoSellEnabled(mat);

            ConfigurationSection template = services.configManager.menus()
                    .getConfigurationSection(enabled ? "auto-sell-category.item-enabled" : "auto-sell-category.item-disabled");
            ItemStack icon = new ItemStack(mat);
            var meta = icon.getItemMeta();
            Map<String, String> placeholders = Map.of("material", mat.name());
            if (template != null) {
                meta.displayName(TextUtil.parse(template.getString("name", ""), placeholders));
                meta.lore(TextUtil.parseList(template.getStringList("lore"), placeholders));
            }
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.values());
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
            ItemStack prev = ItemUtil.build(services.configManager.menus().getConfigurationSection("common.prev-page"), Map.of());
            setItem(PREV_SLOT, prev, e -> {
                currentPage--;
                renderPage();
            });
        } else {
            setItem(PREV_SLOT, null, null);
        }

        if (currentPage < totalPages - 1) {
            ItemStack next = ItemUtil.build(services.configManager.menus().getConfigurationSection("common.next-page"), Map.of());
            setItem(NEXT_SLOT, next, e -> {
                currentPage++;
                renderPage();
            });
        } else {
            setItem(NEXT_SLOT, null, null);
        }
    }
}
