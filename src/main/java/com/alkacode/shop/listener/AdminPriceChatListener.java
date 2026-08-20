package com.alkacode.shop.listener;

import com.alkacode.shop.manager.AdminPriceInputManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/** Captura o preco/moeda digitado no chat pelo admin (ver AdminPriceCategoryMenu). So age com input pendente. */
public final class AdminPriceChatListener implements Listener {

    private final JavaPlugin plugin;
    private final AdminPriceInputManager inputManager;

    public AdminPriceChatListener(JavaPlugin plugin, AdminPriceInputManager inputManager) {
        this.plugin = plugin;
        this.inputManager = inputManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!inputManager.isPending(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        String input = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        Bukkit.getScheduler().runTask(plugin, () -> inputManager.handleChatInput(player, input));
    }
}
