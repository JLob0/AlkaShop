package com.alkacode.shop.hook;

import org.bukkit.Bukkit;

/**
 * AlkaDrop e softdepend - so usado pra saber se o DropCollectedListener vai
 * realmente receber eventos. A classe DropCollectedEvent carrega em tempo de
 * compilacao independente do plugin estar instalado (compileOnly); se nao estiver,
 * o evento simplesmente nunca e disparado, entao registrar o listener sempre e
 * seguro - isto so serve pra logar no console.
 */
public final class AlkaDropHook {

    private AlkaDropHook() {
    }

    public static boolean isPresent() {
        return Bukkit.getPluginManager().getPlugin("AlkaDrop") != null;
    }
}
