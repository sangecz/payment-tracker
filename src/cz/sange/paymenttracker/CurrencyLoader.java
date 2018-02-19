package cz.sange.paymenttracker;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CurrencyLoader {

    private static final String CURRENCY_CODE_PATTERN = "[\\w]{3}";
    private static final String CURRENCIES_FILE = "src/main/resources/currencies.csv";
    private static final String CURRENCY_LINE_PATTERN = "^" + CURRENCY_CODE_PATTERN + ",[\\w\\s]+";

    private Map<String, String> currencies = new HashMap<>();

    CurrencyLoader() {
        try {
            loadAvailableCurrencies();
        } catch (IOException e) {
            System.err.println("Error loading currencies from file");
            e.printStackTrace();
        }
    }

    public Map<String, String> getCurrencies() {
        return currencies;
    }

    private void loadAvailableCurrencies() throws IOException {
        Path path = FileSystems.getDefault().getPath(CURRENCIES_FILE);
        Stream<String> stream = Files.lines(path);

        currencies = stream
                .skip(1) // skips the file header
                .filter(l -> l.matches(CURRENCY_LINE_PATTERN)) // matches lines with currency code
                .map(String::toUpperCase)
                .collect(Collectors.toMap( // collects
                        l -> l.split(",")[0],
                        l -> l.split(",")[1])
                );
    }
}
