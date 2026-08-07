package com.alkacode.shop.manager;

import com.alkacode.shop.model.PlayerShopData;
import com.alkacode.shop.storage.DatabaseManager;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerDataManager {

    private final DatabaseManager database;
    private final Map<UUID, PlayerShopData> cache = new ConcurrentHashMap<>();

    public PlayerDataManager(DatabaseManager database) {
        this.database = database;
    }

    public void loadForJoin(Player player) {
        cache.put(player.getUniqueId(), database.loadSync(player.getUniqueId()));
    }

    public void unload(Player player) {
        PlayerShopData data = cache.remove(player.getUniqueId());
        if (data != null) {
            database.save(data);
        }
    }

    public PlayerShopData get(UUID uuid) {
        return cache.computeIfAbsent(uuid, database::loadSync);
    }

    public PlayerShopData get(Player player) {
        return get(player.getUniqueId());
    }

    public void save(PlayerShopData data) {
        database.save(data);
    }
}
