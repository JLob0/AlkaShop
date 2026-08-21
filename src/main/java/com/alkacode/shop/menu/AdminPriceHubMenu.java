package com.alkacode.shop.menu;

import com.alkacode.shop.ShopServices;
import com.alkacode.shop.util.ItemUtil;
import com.alkacode.shop.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/** Hub da GUI de admin de precos: um botao por categoria, cada um abrindo {@link AdminPriceCategoryMenu}. */
public final class AdminPriceHubMenu extends AbstractShopMenu {

    private final ShopServices services;
    private final ConfigurationSection config;

    public AdminPriceHubMenu(Player viewer, ShopServices services) {
        super(viewer, TextUtil.parse(services.configManager.menus().getString("admin-price-hub.title", "<#FFD700><b>⚙ Precos - Admin</b></#FFD700>")),
                services.configManager.menus().getInt("admin-price-hub.size", 27));
        this.services = services;
        this.config = services.configManager.menus().getConfigurationSection("admin-price-hub");
        build();
    }

    private void build() {
        if (config == null) {
            return;
        }
        ItemStack filler = ItemUtil.build(services.configManager.menus().getConfigurationSection("common.filler"), Map.of());
        for (int i = 0; i < getInventory().getSize(); i++) {
            setItem(i, filler);
        }

        ConfigurationSection categories = config.getConfigurationSection("categories");
        if (categories != null) {
            for (String category : categories.getKeys(false)) {
                buildCategoryButton(categories.getConfigurationSection(category), category);
            }
        }
    }

    private void buildCategoryButton(ConfigurationSection section, String category) {
        if (section == null) {
            return;
        }
        int slot = section.getInt("slot", -1);
        if (slot < 0) {
            return;
        }
        Map<String, String> placeholders = Map.of(
                "direct", String.valueOf(countDirect(category)),
                "total", String.valueOf(countTotal(category))
        );
        ItemStack item = ItemUtil.build(section, placeholders);
        setItem(slot, item, e -> {
            viewer.closeInventory();
            new AdminPriceCategoryMenu(viewer, services, category, section.getString("name", category)).open();
        });
    }

    /** Quantos materiais dessa categoria tem preco DIRETO (editaveis de verdade aqui, nao herdados). */
    private int countDirect(String category) {
        int count = 0;
        for (Material mat : services.priceManager.allDirectMaterials()) {
            String matCategory = services.priceManager.getCategory(mat);
            boolean matches = "outros".equals(category) ? matCategory.isBlank() : matCategory.equals(category);
            if (matches) {
                count++;
            }
        }
        return count;
    }

    /** Total de vendaveis dessa categoria (direto + herdado) - o que o jogador ve no menu dele. */
    private int countTotal(String category) {
        int count = 0;
        for (Material mat : services.priceManager.allResolved().keySet()) {
            String matCategory = services.priceManager.getCategory(mat);
            boolean matches = "outros".equals(category) ? matCategory.isBlank() : matCategory.equals(category);
            if (matches) {
                count++;
            }
        }
        return count;
    }
}
