package com.alkacode.shop.hook;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Integracao soft com o AlkaVips (softdepend) - pega o nome de exibicao do tier de
 * menor order cadastrado (tag placeholder nas features gated por VIP, ver
 * AutoSellConfigMenu/MainSellMenu) e o multiplicador de venda combinado (Perk Tree +
 * Server Boost) aplicado no preco final em SellManager.
 *
 * Reflexao pura - NUNCA importar com.alkacode.vips.* direto aqui (mesmo motivo
 * documentado no AlkaMinesHook/AlkaVipsHook do AlkaDrop: mismatch de versao entre os
 * jars vira LinkageError capaz de derrubar o onPluginEnable inteiro).
 */
public final class AlkaVipsHook {

    private final Object api;
    private final Method getVipTypesOrderedMethod;
    private final Method displayMethod;
    private final Method getPerkSellMultiplierMethod;
    private final Object boostApi;
    private final Method getBoostSellMultiplierMethod;
    private final Logger logger;

    private AlkaVipsHook(Object api, Method getVipTypesOrderedMethod, Method displayMethod,
                          Method getPerkSellMultiplierMethod, Object boostApi, Method getBoostSellMultiplierMethod,
                          Logger logger) {
        this.api = api;
        this.getVipTypesOrderedMethod = getVipTypesOrderedMethod;
        this.displayMethod = displayMethod;
        this.getPerkSellMultiplierMethod = getPerkSellMultiplierMethod;
        this.boostApi = boostApi;
        this.getBoostSellMultiplierMethod = getBoostSellMultiplierMethod;
        this.logger = logger;
    }

    public static AlkaVipsHook tryHook(Logger logger) {
        if (Bukkit.getPluginManager().getPlugin("AlkaVips") == null) {
            return null;
        }
        try {
            Class<?> apiClass = Class.forName("com.alkacode.vips.api.AlkaVipsAPI");
            RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(apiClass);
            if (registration == null) {
                return null;
            }
            Class<?> vipTypeInfoClass = Class.forName("com.alkacode.vips.api.VipTypeInfo");

            Method getVipTypesOrdered = apiClass.getMethod("getVipTypesOrdered");
            Method display = vipTypeInfoClass.getMethod("display");
            Method getPerkSellMultiplier = apiClass.getMethod("getPerkSellMultiplier", java.util.UUID.class);

            // AlkaVipsBoostAPI e um servico Bukkit separado (Server Boost / "VIP Solidario") -
            // registro independente, pode nao existir mesmo com AlkaVips presente em versoes antigas.
            Object boostApiInstance = null;
            Method getBoostSellMultiplier = null;
            try {
                Class<?> boostApiClass = Class.forName("com.alkacode.vips.api.AlkaVipsBoostAPI");
                RegisteredServiceProvider<?> boostRegistration = Bukkit.getServicesManager().getRegistration(boostApiClass);
                if (boostRegistration != null) {
                    boostApiInstance = boostRegistration.getProvider();
                    getBoostSellMultiplier = boostApiClass.getMethod("getSellMultiplier");
                }
            } catch (Throwable t) {
                logger.log(Level.FINE, "AlkaVipsBoostAPI nao disponivel (versao antiga do AlkaVips?) - "
                        + "multiplicador de venda vai considerar so a Perk Tree.", t);
            }

            logger.info("AlkaVips detectado - lores de auto-venda vao mostrar a tag do tier VIP"
                    + (getBoostSellMultiplier != null ? " e o multiplicador de venda (Perk Tree + Boost) esta ativo."
                    : " e o multiplicador de venda da Perk Tree esta ativo."));
            return new AlkaVipsHook(registration.getProvider(), getVipTypesOrdered, display,
                    getPerkSellMultiplier, boostApiInstance, getBoostSellMultiplier, logger);
        } catch (Throwable t) {
            logger.log(Level.WARNING, "AlkaVips encontrado mas a API nao carregou (versao incompativel?) - "
                    + "lores de auto-venda vao usar uma tag generica de VIP.", t);
            return null;
        }
    }

    /** Nome de exibicao (ex: "[VIP]") do tier de menor order cadastrado, ou vazio se nao houver nenhum/a chamada falhar. */
    public Optional<String> firstVipDisplay() {
        try {
            CompletableFuture<?> future = (CompletableFuture<?>) getVipTypesOrderedMethod.invoke(api);
            List<?> ordered = (List<?>) future.get(50, TimeUnit.MILLISECONDS);
            if (ordered == null || ordered.isEmpty()) {
                return Optional.empty();
            }
            return Optional.ofNullable((String) displayMethod.invoke(ordered.get(0)));
        } catch (Throwable t) {
            logger.log(Level.FINE, "Hook do AlkaVips falhou em firstVipDisplay.", t);
            return Optional.empty();
        }
    }

    /** Multiplicador de venda combinado: perk da Perk Tree (SELL_MULTIPLIER) x boost de
     * servidor ativo. 1.0 se nada aplicavel/a chamada falhar (nunca lanca, sempre seguro
     * de multiplicar direto no preco). */
    public double getSellMultiplier(java.util.UUID uuid) {
        double multiplier = 1.0;
        try {
            CompletableFuture<?> future = (CompletableFuture<?>) getPerkSellMultiplierMethod.invoke(api, uuid);
            Double perkValue = (Double) future.get(50, TimeUnit.MILLISECONDS);
            if (perkValue != null && perkValue > 0) {
                multiplier *= perkValue;
            }
        } catch (Throwable t) {
            logger.log(Level.FINE, "Hook do AlkaVips falhou em getPerkSellMultiplier.", t);
        }
        if (boostApi != null && getBoostSellMultiplierMethod != null) {
            try {
                Double boostValue = (Double) getBoostSellMultiplierMethod.invoke(boostApi);
                if (boostValue != null && boostValue > 0) {
                    multiplier *= boostValue;
                }
            } catch (Throwable t) {
                logger.log(Level.FINE, "Hook do AlkaVips falhou em getBoostSellMultiplier.", t);
            }
        }
        return multiplier;
    }
}
