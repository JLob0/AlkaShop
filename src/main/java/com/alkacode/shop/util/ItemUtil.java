package com.alkacode.shop.util;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
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
        item.setItemMeta(meta);
        return item;
    }
}
