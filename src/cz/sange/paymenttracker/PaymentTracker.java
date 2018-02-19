package cz.sange.paymenttracker;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Currency.getAvailableCurrencies;

public class PaymentTracker {

    private static final String CURRENCY_RATES_URL = "http://apilayer.net/api/live?access_key=";
    private static final String CONFIG_PATH = "src/main/resources/currency-rates.properties";

    private static final String CURRENCY_CODE_PATTERN = "[\\w]{3}";
    private static final String PAYMENT_DELIM = " ";
    private static final String PAYMENT_PATTERN = CURRENCY_CODE_PATTERN + PAYMENT_DELIM + "[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?";

    private static final String QUIT_WORD = "quit";
    private static final int EXIT_CODE_OK = 0;
    private static final int EXIT_CODE_ONLY_ONE_INPUT_FILE = 1;

    private Set<Currency> currencies;
    private Map<String, Double> traffic = new HashMap<>();
    private List<String> lines = Collections.synchronizedList(new ArrayList<>());
    private Map<String, Double> rates = new HashMap<>();
    private String apiKey;
    private DecimalFormat df = new DecimalFormat("#.00");
    private Gson gson = new Gson();


    PaymentTracker(String[] args) {
        loadConfig();


        currencies = getAvailableCurrencies();

        readInputFile(args);
    }

    /**
     * Loads config file.
     */
    private void loadConfig() {
        Properties prop = new Properties();
        InputStream input = null;

        try {
            input = new FileInputStream(CONFIG_PATH);
            prop.load(input);

            apiKey = prop.getProperty("API_KEY");
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Prints payment traffic.
     */
    public void printTraffic() {
        traffic = getMergedTraffic(lines, traffic);

        // clears lines array to be ready for another round of input lines
        lines.clear();

        updateExchangeRates();

        String result = traffic.entrySet()
                .stream()
                .filter(entry -> entry.getValue() != 0) // filters non zero amounts
                .map(output())
                .collect(Collectors.joining("\n"));
        System.out.println(result);
    }

    /**
     * Formats currency code and amount, eg. EUR 5.5 (USD 4.9)
     * @return mapper function
     */
    private Function<Map.Entry<String, Double>, String> output() {
        return e -> {
            String code = e.getKey();
            double amount = e.getValue();
            return code + " " + amount + ( !code.equals("USD") ? "(USD " + amount / rates.get(code) + ")" : "");
        };
    }

    /**
     * Reads initial payments from provided input file.
     *
     * @param args program args
     */
    private void readInputFile(String[] args) {
        if (args.length == 0) {
            return; // nothing to do with no args
        }
        if (args.length > 1) {
            System.err.println("At most one input file is expected.");
            System.exit(EXIT_CODE_ONLY_ONE_INPUT_FILE);
        }
        String filePath = args[0];
        Path path = FileSystems.getDefault().getPath(filePath);

        try (Stream<String> stream = Files.lines(path)) {
            traffic = getTraffic(stream);
        } catch (IOException e) {
            System.err.println("Error reading input file with payments");
            e.printStackTrace();
        }
    }

    /**
     * Kicks off 'infinite' reading console line by line adding lines to list for later processing.
     * When 'quit' is read, program exits.
     */
    public void readConsoleInput() {
        BufferedReader br = null;

        try {

            System.out.print("Enter payments : \n");
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
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Error reading console input");
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    System.err.println("Error closing buffered reader");
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Gets merged payment traffic from current traffic and read input lines.
     *
     * @param lines   list of input lines
     * @param traffic hashmap of currency codes and amounts
     * @return hashmap of currency codes and amounts
     */
    private synchronized Map<String, Double> getMergedTraffic(List<String> lines, Map<String, Double> traffic) {
        Map<String, Double> inputTraffic = getTraffic(lines.stream());

        return Stream.of(traffic, inputTraffic)
                .map(Map::entrySet)          // converts maps to entry sets
                .flatMap(Collection::stream) // flattens entry sets
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                mapMergeAdder()
                        )
                );
    }

    /**
     * Gets hashmap representing payment traffic from input lines. Invalid input is filtered out silently.
     *
     * @param stream input lines
     * @return hashmap of currency codes and amounts
     */
    private Map<String, Double> getTraffic(Stream<String> stream) {
        return stream
                .filter(l -> l.matches(PAYMENT_PATTERN)) // filters allowed payment lines
                .map(String::toUpperCase)
                .map(l -> l.split(PAYMENT_DELIM)) // maps to pair (CURRENCY_CODE: amount)
                .filter(pair -> currencies.containsKey(pair[0])) // filters only allowed pairs based on currency codes
                .collect(
                        Collectors.toMap(
                                pair -> pair[0],
                                pair -> Double.parseDouble(pair[1]),
                                mapMergeAdder()
                        )
                );
    }

    /**
     * Duplicated keys are merged by adding their values.
     *
     * @return added value
     */
    private BinaryOperator<Double> mapMergeAdder() {
        return (oldValue, newValue) -> oldValue + newValue;
    }

    /**
     * Gets exchange rates from JSON response, based USD.
     * @return
     */
    private void updateExchangeRates() {
        try {
            String url = CURRENCY_RATES_URL + apiKey;

            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JsonObject jsonObject = gson.fromJson(response.toString(), JsonObject.class).getAsJsonObject();
            Map<String, Double> map = new HashMap<>();
            map = (Map<String, Double>) gson.fromJson(jsonObject.get("quotes"), map.getClass());

            rates = map.entrySet().stream()
                    .map(e -> new MyCurrency(e.getKey(), e.getValue()))
                    .collect(Collectors.toMap(MyCurrency::getName, MyCurrency::getRate));

        } catch (IOException e) {
            System.err.println("Error updating exchange rates.");
            e.printStackTrace();
        }
    }
}
