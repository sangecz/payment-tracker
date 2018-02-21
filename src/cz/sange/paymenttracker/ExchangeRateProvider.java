package cz.sange.paymenttracker;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ExchangeRateProvider {

    private Map<String, Double> rates;
    private Gson gson = new Gson();
    private String url;

    public ExchangeRateProvider(String url) {
        this.url = url;
    }

    public void setRates(Map<String, Double> rates) {
        this.rates = rates;
    }

    /**
     * Gets exchange rates from JSON response, based USD.
     */
    public Map<String, Double> getCurrentExchangeRates() {
        HttpURLConnection con = null;
        try {
            URL obj = new URL(url);
            con = (HttpURLConnection) obj.openConnection();
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

            if (map == null) {
                return null;
            }

            rates = map.entrySet().stream()
                    .map(e -> new MyCurrency(e.getKey(), e.getValue()))
                    .collect(Collectors.toMap(MyCurrency::getName, MyCurrency::getRate));

        } catch (IOException e) {
            System.err.println("Error updating exchange rates.");
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
        return rates;
    }
}
