package com.alkacode.shop.util;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public final class PriceFormatter {

    private static final Locale LOCALE = Locale.of("pt", "BR");

    private PriceFormatter() {
    }

    /** Valor exato, sem abreviar - usado em contextos de admin/preco (setprice, debug, PAPI) onde precisao importa mais que leitura rapida. */
    public static String format(double value, boolean round, int decimalPlaces) {
        if (round && value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        return String.format(LOCALE, "%." + decimalPlaces + "f", value);
    }

    /**
     * Valor abreviado (K/M/B/T) a partir de 1000 - usado no feedback de venda pro
     * jogador (formatTotals), onde ler rapido importa mais que o numero exato. Ex:
     * 2031.11 -> "2031,11", 1100000 -> "1,1M", 45300000000 -> "45,3B".
     */
    public static String formatCompact(double value, boolean round, int decimalPlaces) {
        double abs = Math.abs(value);
        if (abs >= 1_000_000_000_000.0) {
            return formatSuffixed(value, 1_000_000_000_000.0, "T");
        }
        if (abs >= 1_000_000_000.0) {
            return formatSuffixed(value, 1_000_000_000.0, "B");
        }
        if (abs >= 1_000_000.0) {
            return formatSuffixed(value, 1_000_000.0, "M");
        }
        if (abs >= 1_000.0) {
            return formatSuffixed(value, 1_000.0, "K");
        }
        return format(value, round, decimalPlaces);
    }

    private static String formatSuffixed(double value, double divisor, String suffix) {
        double scaled = value / divisor;
        String formatted = String.format(LOCALE, "%.1f", scaled);
        if (formatted.endsWith(",0")) {
            formatted = formatted.substring(0, formatted.length() - 2);
        }
        return formatted + suffix;
    }

    /** "450 gold e 3 escarion" (ou "1,1M gold" se for grande) - usado no placeholder {@code <totals>} das mensagens de venda, cada moeda na cor "default" (sem colorResolver). */
    public static String formatTotals(Map<String, Double> totals, boolean round, int decimalPlaces) {
        return formatTotals(totals, round, decimalPlaces, currency -> "");
    }

    /**
     * Mesma coisa, mas cada moeda vem envolta na sua propria tag MiniMessage de cor
     * (ex: "<#FFD700>450 gold</#FFD700> e <#55FF55>3 escarion</#55FF55>") - colorResolver
     * normalmente e {@code configManager::currencyColor} (selling.currency-colors no
     * config.yml). Cada segmento fecha a propria tag, entao funciona nested dentro de
     * qualquer tag de cor que a mensagem em messages.yml ja tenha em volta do placeholder.
     */
    public static String formatTotals(Map<String, Double> totals, boolean round, int decimalPlaces,
                                        Function<String, String> colorResolver) {
        Map<String, Double> nonZero = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : totals.entrySet()) {
            if (entry.getValue() > 0) {
                nonZero.put(entry.getKey(), entry.getValue());
            }
        }
        if (nonZero.isEmpty()) {
            return "0";
        }
        StringBuilder builder = new StringBuilder();
        int index = 0;
        for (Map.Entry<String, Double> entry : nonZero.entrySet()) {
            if (index > 0) {
                builder.append(index == nonZero.size() - 1 ? " e " : ", ");
            }
            String color = colorResolver.apply(entry.getKey());
            String segment = formatCompact(entry.getValue(), round, decimalPlaces) + " " + entry.getKey();
            if (color == null || color.isBlank()) {
                builder.append(segment);
            } else {
                builder.append(color).append(segment).append("</").append(color, 1, color.length());
            }
            index++;
        }
        return builder.toString();
    }
}
