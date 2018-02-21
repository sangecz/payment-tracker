package cz.sange.paymenttracker;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Currency.getAvailableCurrencies;

public class PaymentTracker {

    private static final String PAYMENT_DELIM = " ";
    private static final String PAYMENT_PATTERN = "[\\w]{3}" + PAYMENT_DELIM + "[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?";

    private static final String QUIT_WORD = "quit";
    private static final int EXIT_CODE_OK = 0;

    private Set<String> currencies;
    private Map<String, BigDecimal> traffic = new ConcurrentHashMap<>();
    private List<String> lines = Collections.synchronizedList(new ArrayList<>());
    private ExchangeRateProvider ratesProvider;


    PaymentTracker(ExchangeRateProvider ratesProvider) {
        this.ratesProvider = ratesProvider;
        currencies = getAvailableCurrencies()
                .stream()
                .map(Currency::getCurrencyCode)
                .collect(Collectors.toSet());
    }

    public List<String> getLines() {
        return lines;
    }

    public Map<String, BigDecimal> getTraffic() {
        return traffic;
    }

    /**
     * Prints payment traffic.
     */
    public void printTraffic() {
        traffic = getMergedTraffic(lines, traffic);

        // clears lines array to be ready for another round of input lines
        lines.clear();

        Map<String, Double> rates = ratesProvider.getCurrentExchangeRates();

        String result = traffic.entrySet()
                .stream()
                .filter(entry -> !entry.getValue().equals(BigDecimal.ZERO)) // filters non zero amounts
                .map(Utils.getTrafficOutput(rates))
                .collect(Collectors.joining("\n"));

        if(!result.isEmpty()) {
            System.out.println("---\n" + result);
        }
    }

    /**
     * Kicks off 'infinite' reading console line by line adding lines to list for later processing.
     * When 'quit' is read, program exits.
     */
    public void readConsoleInput() {
        BufferedReader br = null;

        try {

            System.out.println("Enter payments:");
            br = new BufferedReader(new InputStreamReader(System.in));

            while (true) {

                String line = br.readLine();
                if (line == null || line.isEmpty()) {
                    Thread.sleep(1000);
                    continue;
                }

                if (QUIT_WORD.equals(line)) {
                    System.out.println("Exiting...");
                    printTraffic();
                    System.exit(EXIT_CODE_OK);
                }

                lines.add(line);
            }

        } catch (InterruptedException e) {
            System.err.println("Error while sleeping");
        } catch (IOException e) {
            System.err.println("Error reading console input");
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    System.err.println("Error closing buffered reader");
                }
            }
        }
    }

    /**
     * Reads initial payments from provided input file.
     *
     * @param args program args
     */
    public void readInputFile(String[] args) {
        if (args.length == 0) {
            return; // nothing to do with no args
        }
        if (args.length > 1) {
            System.err.println("At most one input file is expected.");
            return;
        }
        String filePath = args[0];
        Path path = FileSystems.getDefault().getPath(filePath);

        try (Stream<String> stream = Files.lines(path)) {
            traffic = getTrafficFromStream(stream);
        } catch (IOException e) {
            System.err.println("Error reading input file with payments");
        }
    }

    /**
     * Gets merged payment traffic from current traffic and read input lines.
     *
     * @param lines   list of input lines
     * @param traffic hashmap of currency codes and amounts
     * @return hashmap of currency codes and amounts
     */
    public Map<String, BigDecimal> getMergedTraffic(List<String> lines, Map<String, BigDecimal> traffic) {
        Map<String, BigDecimal> inputTraffic = getTrafficFromStream(lines.stream());

        return Stream.of(traffic, inputTraffic)
                .map(Map::entrySet)          // converts maps to entry sets
                .flatMap(Collection::stream) // flattens entry sets
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                BigDecimal::add
                        )
                );
    }

    /**
     * Gets hashmap representing payment traffic from input lines. Invalid input is filtered out silently.
     *
     * @param stream input lines
     * @return hashmap of currency codes and amounts
     */
    public Map<String, BigDecimal> getTrafficFromStream(Stream<String> stream) {
        return stream
                .parallel()
                .filter(l -> l.matches(PAYMENT_PATTERN)) // filters allowed payment lines
                .map(String::toUpperCase)
                .map(l -> l.split(PAYMENT_DELIM)) // maps to pair (CURRENCY_CODE: amount)
                .filter(pair -> currencies.contains(pair[0])) // filters only allowed pairs
                .collect(                                                           // based on currency codes
                        Collectors.toMap(
                                pair -> pair[0],
                                pair -> new BigDecimal(pair[1]),
                                BigDecimal::add  // Duplicated keys are merged by adding their values.
                        )
                );
    }
}
