package com.alkacode.shop.util;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PriceFormatter {

    private PriceFormatter() {
    }

    public static String format(double value, boolean round, int decimalPlaces) {
        if (round && value == Math.floor(value)) {
            return String.valueOf((long) value);
        }
        return String.format("%." + decimalPlaces + "f", value);
    }

    /** "450 coins e 3 escarion" - usado no placeholder {@code <totals>} das mensagens de venda. */
    public static String formatTotals(Map<String, Double> totals, boolean round, int decimalPlaces) {
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
            builder.append(format(entry.getValue(), round, decimalPlaces)).append(' ').append(entry.getKey());
            index++;
        }
        return builder.toString();
    }
}
