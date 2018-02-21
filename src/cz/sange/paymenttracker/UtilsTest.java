package cz.sange.paymenttracker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilsTest {

    private Map<String, Double> rates = new HashMap<>();

    @BeforeEach
    void setUp() {
        rates.put("EUR", 0.8117);
        rates.put("CZK", 20.561899);
    }

    /**
     * Tests whether right traffic string is returned with respect to provided rates.
     */
    @Test
    void getTrafficOutput() {
        Map<String, BigDecimal> traffic = new HashMap<>();
        traffic.put("EUR", new BigDecimal(100));
        traffic.put("CZK", new BigDecimal(200));

        String expected = "EUR 100.00 (USD 123.20)\nCZK 200.00 (USD 9.73)";
        String actual = traffic.entrySet()
                .stream()
                .map(Utils.getTrafficOutput(rates))
                .collect(Collectors.joining("\n"));

        assertEquals(expected, actual);
    }

    /**
     * Tests USD exchange rate calculation to another currency.
     */
    @Test
    void getUsd() {
        String code = "CZK";
        BigDecimal amount = new BigDecimal(200);

        String expected = " (USD 9.73)";
        String actual = Utils.getUsd(code, amount, rates);

        assertEquals(expected, actual);
    }
}