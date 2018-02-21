package cz.sange.paymenttracker;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PaymentTrackerTest {

    private PaymentTracker paymentTracker;
    private MathContext mc = new MathContext(5);
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    @BeforeEach
    void setUp() {
        ExchangeRateProvider rateProvider = new ExchangeRateProvider("dummy");
        Map<String, Double> rates = new HashMap<>();
        rates.put("EUR", 0.8117);
        rates.put("CZK", 20.561899);
        rateProvider.setRates(rates);

        paymentTracker = new PaymentTracker(rateProvider);

        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(System.out);
        System.setErr(System.err);
    }

    /**
     * Tests correctness of printed output when no file input and with file provided.
     */
    @Test
    void printTraffic() {
        String expectedOut;

        // no payments provided yet
        paymentTracker.printTraffic();
        expectedOut = "";

        assertEquals(expectedOut, outContent.toString());

        // payments provided from input file
        String[] args1 = {"input.txt"};
        paymentTracker.readInputFile(args1);
        paymentTracker.printTraffic();
        expectedOut += "---\n" +
                "EUR -64.40 (USD -79.35)\n" +
                "CZK 1000.00 (USD 48.63)\n" +
                "USD 200.00\n";

        assertEquals(expectedOut, outContent.toString());
    }

    /**
     * Tests program reaction when provided arbitrary number of inputs.
     */
    @Test
    void readInputFile() {
        Map<String, BigDecimal> actual;
        Map<String, BigDecimal> expected;

        // zero args
        String[] args0 = {};
        paymentTracker.readInputFile(args0);
        actual = paymentTracker.getTraffic();
        expected = new HashMap<>();

        assertEquals(expected, actual);

        // one arg
        String[] args1 = {"input.txt"};
        paymentTracker.readInputFile(args1);

        expected = new HashMap<>();
        expected.put("USD", new BigDecimal(200));
        expected.put("CZK", new BigDecimal(1000));
        expected.put("EUR", new BigDecimal(-64.404444));
        BigDecimal expectedSum = getSum(expected);

        BigDecimal actualSum = getSum(paymentTracker.getTraffic());

        assertEquals(expectedSum.round(mc), actualSum.round(mc));

        // one arg invalid
        String[] args1e = {"error.txt"};
        paymentTracker.readInputFile(args1e);
        String expectedErr = "Error reading input file with payments\n";

        assertEquals(expectedErr, errContent.toString());
        errContent.reset();

        // multiple args
        String[] args2 = {"input.txt", "asdasd"};
        paymentTracker.readInputFile(args2);
        expectedErr = "At most one input file is expected.\n";

        assertEquals(expectedErr, errContent.toString());
    }


    /**
     * Test whether correctly merges old traffic (net amount) with new payments.
     */
    @Test
    void getMergedTraffic() {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Czk 200");
        lines.add("EUR 100");
        lines.add("EUR -50");
        lines.add("USD +150");
        lines.add("XYZ -50");

        Map<String, BigDecimal> traffic = new HashMap<>();
        traffic.put("CZK", new BigDecimal(200));
        traffic.put("EUR", BigDecimal.ZERO);
        traffic.put("USD", new BigDecimal(100));

        Map<String, BigDecimal> expected = new HashMap<>();
        expected.put("CZK", new BigDecimal(400));
        expected.put("EUR", new BigDecimal(50));
        expected.put("USD", new BigDecimal(250));

        Map<String, BigDecimal> actual = paymentTracker.getMergedTraffic(lines, traffic);

        assertTrue(expected.equals(actual));
    }

    /**
     * Tests whether method omits invalid inputs
     * and correctly aggregates multiple payments in same currency
     */
    @Test
    void getTraffic() {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Czk 200");
        lines.add("EUR 100");
        lines.add("EUR -50");
        lines.add("sgsgdsfgdf ");
        lines.add("sgsgdsfg dfsd fsdfs\nsdfsfsd");
        lines.add("sgsgdfsd\n ssdfgsd");
        lines.add("USD -50");
        lines.add("USD +150");
        lines.add("EUR -50");
        lines.add("XYZ -50");

        Map<String, BigDecimal> expected = new HashMap<>();
        expected.put("CZK", new BigDecimal(200));
        expected.put("EUR", BigDecimal.ZERO);
        expected.put("USD", new BigDecimal(100));

        Map<String, BigDecimal> actual = paymentTracker.getTrafficFromStream(lines.stream());

        assertTrue(expected.equals(actual));
    }

    private BigDecimal getSum(Map<String, BigDecimal> map) {
        return map
                .entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}