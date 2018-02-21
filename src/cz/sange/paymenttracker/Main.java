package cz.sange.paymenttracker;

import java.util.*;

public class Main {

    private static final int REPORTER_PERIOD = 60_000; // 60s
    private static final String API_KEY = ""; // TODO enter your API key
    private static final String API = "http://apilayer.net/api/live?access_key=" + API_KEY;

    public static void main(String[] args) {
        Main m = new Main();
        m.startTrackingPayments(args);
    }

    private void startTrackingPayments(String[] args) {
        ExchangeRateProvider rateProvider = new ExchangeRateProvider(API);
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
}
