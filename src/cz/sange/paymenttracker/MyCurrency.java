package cz.sange.paymenttracker;

public class MyCurrency {

    private String name;
    private double rate;

    public MyCurrency(String name, double rate) {
        this.name = name.replace("USD", "");
        this.rate = rate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }
}
