package com.alkacode.shop.menu;

import com.alkacode.shop.ShopServices;
import com.alkacode.shop.model.PlayerShopData;
import com.alkacode.shop.util.ItemUtil;
import com.alkacode.shop.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.List;
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

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        var fMeta = filler.getItemMeta();
        fMeta.displayName(TextUtil.parse(" "));
        filler.setItemMeta(fMeta);
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
        ItemStack back = new ItemStack(Material.ARROW);
        var meta = back.getItemMeta();
        meta.displayName(TextUtil.parse("<#FFD700>◀ Voltar ao mercado"));
        back.setItemMeta(meta);
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
        ItemStack toggleAll = new ItemStack(allEnabled ? Material.LIME_WOOL : Material.RED_WOOL);
        var meta = toggleAll.getItemMeta();
        meta.displayName(TextUtil.parse(allEnabled
                ? "<#55FF55><b>✦ AUTO-VENDA TOTAL: LIGADA</b></#55FF55>"
                : "<#FF5555><b>✦ AUTO-VENDA TOTAL: DESLIGADA</b></#FF5555>"));
        meta.lore(List.of(
                TextUtil.parse("<gray>Vende todo item que voce minerar,"),
                TextUtil.parse("<gray>colher ou pegar do chao - sem excecao."),
                TextUtil.parse(" "),
                TextUtil.parse("<white>Acesso: " + services.vipTierTag()),
                TextUtil.parse(" "),
                TextUtil.parse("<yellow>Clique para " + (allEnabled ? "desligar" : "ligar") + ".")
        ));
        meta.addItemFlags(ItemFlag.values());
        toggleAll.setItemMeta(meta);
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
