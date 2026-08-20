package com.alkacode.shop.util;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public final class ItemUtil {

    private ItemUtil() {
    }

    public static ItemStack build(ConfigurationSection section, Map<String, String> placeholders) {
        if (section == null) {
            return new ItemStack(Material.STONE);
        }
        Material material = Material.matchMaterial(section.getString("material", "STONE"));
        ItemStack item = new ItemStack(material != null ? material : Material.STONE);
        ItemMeta meta = item.getItemMeta();
        if (section.contains("name")) {
            meta.displayName(TextUtil.parse(section.getString("name"), placeholders));
        }
        if (section.contains("lore")) {
            meta.lore(TextUtil.parseList(section.getStringList("lore"), placeholders));
        }
        // Sem isso, icones de botao que usam material de ferramenta/arma (ex: IRON_PICKAXE
        // pra representar a categoria "Minerios") ganham de graca as linhas vanilla "When in
        // Main Hand: X Attack Damage" no tooltip - o item e so um botao, nunca vai ser
        // empunhado de verdade.
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }
}
