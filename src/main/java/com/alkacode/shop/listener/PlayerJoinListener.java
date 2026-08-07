package com.alkacode.shop.listener;

import com.alkacode.shop.manager.PlayerDataManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerJoinListener implements Listener {

    private final PlayerDataManager playerDataManager;

    public PlayerJoinListener(PlayerDataManager playerDataManager) {
        this.playerDataManager = playerDataManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        playerDataManager.loadForJoin(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        playerDataManager.unload(event.getPlayer());
    }
}
