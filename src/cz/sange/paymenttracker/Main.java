package cz.sange.paymenttracker;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

// TODO unit tests them all

public class Main {

    private static final String CONFIG_PATH = "src/main/resources/config.properties";
    private static final int REPORTER_PERIOD = 60_000; // 60s

    // filled from config
    private String apiKey;
    private String url;

    public static void main(String[] args) {
        Main m = new Main();
        m.loadConfig();
        m.startTrackingPayments(args);
    }

    private void startTrackingPayments(String[] args) {
        ExchangeRateProvider rateProvider = new ExchangeRateProvider(url + apiKey);
        PaymentTracker paymentTracker = new PaymentTracker(rateProvider);
        paymentTracker.readInputFile(args);
        schedulePeriodicPaymentTracker(paymentTracker);
        paymentTracker.readConsoleInput();
    }

    private void schedulePeriodicPaymentTracker(PaymentTracker paymentTracker) {
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            public void run() {
                paymentTracker.printTraffic();
            }
        };

        Calendar calendar = new GregorianCalendar();
        calendar.setTime(new Date());
        calendar.add(Calendar.SECOND, REPORTER_PERIOD / 1000);

        // start postponed
        timer.scheduleAtFixedRate(task, calendar.getTime(), REPORTER_PERIOD);
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
            url = prop.getProperty("URL");
        } catch (IOException ex) {
            System.err.println("Error reading config file");
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    System.err.println("Error closing config file");
                }
            }
        }
    }
}
