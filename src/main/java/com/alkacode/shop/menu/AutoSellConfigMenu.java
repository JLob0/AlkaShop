package com.alkacode.shop.menu;

import com.alkacode.shop.ShopServices;
import com.alkacode.shop.model.PlayerShopData;
import com.alkacode.shop.util.ItemUtil;
import com.alkacode.shop.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/** Hub de auto-venda: um botao "ativar/desativar tudo" e um botao por categoria, cada um abrindo {@link AutoSellCategoryMenu}. */
public final class AutoSellConfigMenu extends AbstractShopMenu {

    private final ShopServices services;
    private final ConfigurationSection config;

    public AutoSellConfigMenu(Player viewer, ShopServices services) {
        super(viewer, TextUtil.parse(services.configManager.menus().getString("auto-sell-hub.title", "<#55FF55><b>✦ Auto-Venda</b></#55FF55>")),
                services.configManager.menus().getInt("auto-sell-hub.size", 27));
        this.services = services;
        this.config = services.configManager.menus().getConfigurationSection("auto-sell-hub");
        build();
    }

    private void build() {
        if (config == null) {
            return;
        }
        PlayerShopData data = services.playerDataManager.get(viewer);

        ItemStack filler = ItemUtil.build(services.configManager.menus().getConfigurationSection("common.filler"), Map.of());
        for (int i = 0; i < getInventory().getSize(); i++) {
            setItem(i, filler);
        }

        buildToggleAll(data);
        buildBackButton();

        ConfigurationSection categories = config.getConfigurationSection("categories");
        if (categories != null) {
            for (String category : categories.getKeys(false)) {
                buildCategoryButton(categories.getConfigurationSection(category), category, data);
            }
        }
    }

    private void buildBackButton() {
        ItemStack back = ItemUtil.build(config.getConfigurationSection("back-button"), Map.of());
        setItem(18, back, e -> {
            viewer.closeInventory();
            new MainSellMenu(viewer, services).open();
        });
    }

    private void buildToggleAll(PlayerShopData data) {
        int slot = config.getInt("toggle-all-slot", -1);
        if (slot < 0) {
            return;
        }
        boolean allEnabled = data.autoSellAll();
        Map<String, String> placeholders = Map.of("tier", services.vipTierTag());
        ItemStack toggleAll = ItemUtil.build(config.getConfigurationSection(allEnabled ? "toggle-all-on" : "toggle-all-off"), placeholders);
        setItem(slot, toggleAll, e -> {
            if (!viewer.hasPermission("alkashop.autosell.all")) {
                services.sendMessage(viewer, "general.no-permission", Map.of());
                return;
            }
            data.autoSellAll(!allEnabled);
            if (data.autoSellAll()) {
                data.clearMaterials();
            }
            services.playerDataManager.save(data);
            refresh();
        });
    }

    private void buildCategoryButton(ConfigurationSection section, String category, PlayerShopData data) {
        if (section == null) {
            return;
        }
        int slot = section.getInt("slot", -1);
        if (slot < 0) {
            return;
        }
        int[] counts = countActiveAndTotal(category, data);
        Map<String, String> placeholders = Map.of(
                "active", String.valueOf(counts[0]),
                "total", String.valueOf(counts[1]),
                "tier", services.vipTierTag()
        );
        ItemStack item = ItemUtil.build(section, placeholders);
        setItem(slot, item, e -> {
            viewer.closeInventory();
            new AutoSellCategoryMenu(viewer, services, category, section.getString("name", category)).open();
        });
    }

    /** {active, total} vendaveis nessa categoria - active conta como "tudo" se autoSellAll estiver ligado. */
    private int[] countActiveAndTotal(String category, PlayerShopData data) {
        int total = 0;
        int active = 0;
        for (Material mat : services.priceManager.allResolved().keySet()) {
            String matCategory = services.priceManager.getCategory(mat);
            boolean matches = "outros".equals(category) ? matCategory.isBlank() : matCategory.equals(category);
            if (!matches) {
                continue;
            }
            total++;
            if (data.isAutoSellEnabled(mat)) {
                active++;
            }
        }
        return new int[]{active, total};
    }

    private void refresh() {
        viewer.closeInventory();
        new AutoSellConfigMenu(viewer, services).open();
    }
}
