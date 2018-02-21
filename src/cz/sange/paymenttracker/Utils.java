package cz.sange.paymenttracker;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.function.Function;

public class Utils {

    private static final int SCALE = 2;

    /**
     * Formats currency code and amount, eg. EUR 5.5 (USD 4.9)
     *
     * @return mapper function
     */
    public static Function<Map.Entry<String, BigDecimal>, String> getTrafficOutput(Map<String, Double> rates) {
        return e -> {
            String code = e.getKey();
            BigDecimal amount = e.getValue();
            return code + " " + amount.setScale(SCALE, RoundingMode.HALF_UP) + getUsd(code, amount, rates);
        };
    }

    public static String getUsd(String code, BigDecimal amount, Map<String, Double> rates) {
        return code.equals("USD") || rates == null ?
                "" :
                " (USD " + amount.divide(new BigDecimal(rates.get(code)), SCALE, RoundingMode.HALF_UP) + ")";
    }
}
