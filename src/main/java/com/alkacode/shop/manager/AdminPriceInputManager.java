package com.alkacode.shop.manager;

import com.alkacode.shop.ShopServices;
import com.alkacode.shop.menu.AdminPriceCategoryMenu;
import com.alkacode.shop.util.PriceFormatter;
import com.alkacode.shop.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Estado de "aguardando digitar no chat" da GUI de admin de precos (ver AdminPriceCategoryMenu).
 * So um pedido pendente por jogador de cada vez - um novo pedido sobrescreve o anterior. Sempre
 * reabre a tela de categoria de onde veio o pedido depois de processar (sucesso ou erro).
 */
public final class AdminPriceInputManager {

    private final ShopServices services;
    private final Map<UUID, PendingInput> pending = new ConcurrentHashMap<>();

    public AdminPriceInputManager(ShopServices services) {
        this.services = services;
    }

    public boolean isPending(UUID uuid) {
        return pending.containsKey(uuid);
    }

    /** Clique esquerdo num item existente, ou arrastar um item novo pra um slot vazio (isNewMaterial=true). */
    public void promptSetPrice(Player admin, Material material, String screenCategory, String screenDisplayName, boolean isNewMaterial) {
        pending.put(admin.getUniqueId(), new PendingInput(material, screenCategory, screenDisplayName, isNewMaterial, false));
        admin.closeInventory();
        admin.sendMessage(TextUtil.parse(services.configManager.prefix()
                + "<green>Digite '<white>moeda valor</white>' (ex: <white>escarion 3</white>) ou so '<white>valor</white>' "
                + "(assume gold) pra definir o preco de <white>" + material.name() + "</white>."));
        admin.sendMessage(TextUtil.parse(services.configManager.prefix()
                + "<gray>Moedas validas: <white>" + String.join(", ", services.economyHook.currencyIds())
                + "</white>. Digite <red>cancelar</red><gray> pra desistir."));
    }

    /** Clique direito num item existente - remove UMA moeda especifica dele. */
    public void promptRemoveCurrency(Player admin, Material material, String screenCategory, String screenDisplayName) {
        Map<String, Double> current = services.priceManager.directPricesOf(material);
        if (current.isEmpty()) {
            return;
        }
        pending.put(admin.getUniqueId(), new PendingInput(material, screenCategory, screenDisplayName, false, true));
        admin.closeInventory();
        StringBuilder atual = new StringBuilder();
        for (Map.Entry<String, Double> entry : current.entrySet()) {
            if (!atual.isEmpty()) {
                atual.append(", ");
            }
            atual.append(entry.getKey()).append("=").append(PriceFormatter.format(entry.getValue(), true, 2));
        }
        admin.sendMessage(TextUtil.parse(services.configManager.prefix()
                + "<yellow>Precos atuais de <white>" + material.name() + "</white>: <white>" + atual + "</white>."));
        admin.sendMessage(TextUtil.parse(services.configManager.prefix()
                + "<green>Digite a moeda que quer remover, ou <red>cancelar</red><green>."));
    }

    public void handleChatInput(Player admin, String input) {
        PendingInput request = pending.remove(admin.getUniqueId());
        if (request == null) {
            return;
        }
        if (input.equalsIgnoreCase("cancelar")) {
            admin.sendMessage(TextUtil.parse(services.configManager.prefix() + "<yellow>Operacao cancelada."));
            reopen(admin, request);
            return;
        }

        if (request.removeMode()) {
            handleRemoveCurrency(admin, request, input);
        } else {
            handleSetPrice(admin, request, input);
        }
    }

    private void handleSetPrice(Player admin, PendingInput request, String input) {
        String[] parts = input.trim().split("\\s+");
        String currency;
        String rawValue;
        if (parts.length == 1) {
            currency = "gold";
            rawValue = parts[0];
        } else if (parts.length == 2) {
            currency = parts[0].toLowerCase();
            rawValue = parts[1];
        } else {
            admin.sendMessage(TextUtil.parse(services.configManager.prefix()
                    + "<red>Uso: 'moeda valor' ou so 'valor'. Tente de novo (abra a GUI e clique de novo)."));
            reopen(admin, request);
            return;
        }

        if (!services.economyHook.isValidCurrency(currency)) {
            admin.sendMessage(TextUtil.parse(services.configManager.prefix()
                    + "<red>Moeda invalida: <white>" + currency + "</white>. Validas: "
                    + String.join(", ", services.economyHook.currencyIds()) + "."));
            reopen(admin, request);
            return;
        }

        double value;
        try {
            value = Double.parseDouble(rawValue.replace(",", "."));
        } catch (NumberFormatException e) {
            admin.sendMessage(TextUtil.parse(services.configManager.prefix() + "<red>Valor invalido: <white>" + rawValue + "</white>."));
            reopen(admin, request);
            return;
        }
        if (value <= 0) {
            admin.sendMessage(TextUtil.parse(services.configManager.prefix() + "<red>O valor precisa ser maior que zero."));
            reopen(admin, request);
            return;
        }

        services.priceManager.setPrice(request.material(), currency, value);
        if (request.isNewMaterial()) {
            services.priceManager.setCategory(request.material(), "outros".equals(request.screenCategory()) ? null : request.screenCategory());
        }
        admin.sendMessage(TextUtil.parse(services.configManager.prefix() + services.configManager.message("admin.price-set"),
                Map.of("material", request.material().name(), "currency", currency, "value", PriceFormatter.format(value, true, 2))));
        reopen(admin, request);
    }

    private void handleRemoveCurrency(Player admin, PendingInput request, String input) {
        String currency = input.trim().toLowerCase();
        boolean removed = services.priceManager.removePrice(request.material(), currency);
        if (!removed) {
            admin.sendMessage(TextUtil.parse(services.configManager.prefix()
                    + "<red>" + request.material().name() + " nao tinha preco em <white>" + currency + "</white>."));
            reopen(admin, request);
            return;
        }
        admin.sendMessage(TextUtil.parse(services.configManager.prefix() + services.configManager.message("admin.price-removed"),
                Map.of("material", request.material().name(), "currency", currency)));
        reopen(admin, request);
    }

    private void reopen(Player admin, PendingInput request) {
        new AdminPriceCategoryMenu(admin, services, request.screenCategory(), request.screenDisplayName()).open();
    }

    private record PendingInput(Material material, String screenCategory, String screenDisplayName,
                                 boolean isNewMaterial, boolean removeMode) {
    }
}
