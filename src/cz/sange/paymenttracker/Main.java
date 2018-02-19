package cz.sange.paymenttracker;

import java.util.*;

// FIXME threadsafe
// TODO input paths
// TODO unit tests them all
// TODO BigDecimal misto double

public class Main {

    private static final int REPORTER_PERIOD = 10_000; // 60s

    public static void main(String[] args) {
        Main m = new Main();
        m.startTrackingPayments(args);
    }

    private void startTrackingPayments(String[] args) {
        PaymentTracker paymentTracker = null;

        paymentTracker = new PaymentTracker(args);
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
        timer.scheduleAtFixedRate(task, new Date(), REPORTER_PERIOD);
    }
}
