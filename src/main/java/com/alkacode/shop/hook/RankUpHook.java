package com.alkacode.shop.hook;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Integracao soft com o AlkaRankUp - so pra pegar o multiplicador de venda por
 * prestigio (com.alkacode.rankup.api.AlkaRankUpAPI#getSellMultiplier). AlkaShop nunca
 * sabe o que e "prestigio" nem "rank", so pergunta "qual o multiplicador desse
 * jogador?" e aplica em cima do total ja calculado - mesma filosofia de bordas ja
 * usada no AlkaMinesHook (a mina nao sabe precos, a loja nao sabe ranks).
 *
 * Reflexao pura - NUNCA importar com.alkacode.rankup.* direto aqui (mesmo motivo
 * documentado em AlkaVipsHook/AlkaDropHook).
 */
public final class RankUpHook {

    private final Object api;
    private final Method getSellMultiplierMethod;
    private final Logger logger;

    private RankUpHook(Object api, Method getSellMultiplierMethod, Logger logger) {
        this.api = api;
        this.getSellMultiplierMethod = getSellMultiplierMethod;
        this.logger = logger;
    }

    public static RankUpHook tryHook(Logger logger) {
        if (Bukkit.getPluginManager().getPlugin("AlkaRankUp") == null) {
            return null;
        }
        try {
            Class<?> apiClass = Class.forName("com.alkacode.rankup.api.AlkaRankUpAPI");
            RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(apiClass);
            if (registration == null) {
                return null;
            }
            Method getSellMultiplier = apiClass.getMethod("getSellMultiplier", UUID.class);

            logger.info("AlkaRankUp detectado - bonus de venda por prestigio vai ser aplicado.");
            return new RankUpHook(registration.getProvider(), getSellMultiplier, logger);
        } catch (Throwable t) {
            logger.log(Level.WARNING, "AlkaRankUp encontrado mas a API nao carregou via reflexao (versao incompativel?).", t);
            return null;
        }
    }

    /** 1.0 = sem bonus (tambem o fallback em qualquer falha - nunca deixa a venda quebrar por causa do hook). */
    public double getSellMultiplier(UUID uuid) {
        try {
            return (double) getSellMultiplierMethod.invoke(api, uuid);
        } catch (Throwable t) {
            logger.log(Level.FINE, "Hook do AlkaRankUp falhou em getSellMultiplier.", t);
            return 1.0;
        }
    }
}
