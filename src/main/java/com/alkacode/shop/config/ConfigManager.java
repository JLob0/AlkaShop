package com.alkacode.shop.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

public final class ConfigManager {

    private final JavaPlugin plugin;

    private FileConfiguration config;
    private FileConfiguration messages;
    private FileConfiguration menus;
    private boolean debug;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        config = loadResource("config.yml");
        messages = loadResource("messages.yml");
        menus = loadResource("menus.yml");
        debug = config.getBoolean("debug", false);
    }

    public void reload() {
        load();
    }

    private FileConfiguration loadResource(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            plugin.saveResource(name, false);
        }
        FileConfiguration loaded = YamlConfiguration.loadConfiguration(file);
        try (var in = plugin.getResource(name)) {
            if (in != null) {
                loaded.setDefaults(YamlConfiguration.loadConfiguration(
                        new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)));
            }
        } catch (java.io.IOException e) {
            plugin.getLogger().warning("Falha ao carregar defaults de " + name + ": " + e.getMessage());
        }
        return loaded;
    }

    public FileConfiguration config() { return config; }
    public FileConfiguration messages() { return messages; }
    public FileConfiguration menus() { return menus; }

    public boolean debug() { return debug; }
    public void debug(boolean debug) { this.debug = debug; }

    public String prefix() {
        return messages.getString("prefix", "");
    }

    public String message(String path) {
        return messages.getString(path, path);
    }

    public List<String> messageList(String path) {
        return messages.getStringList(path);
    }
}
