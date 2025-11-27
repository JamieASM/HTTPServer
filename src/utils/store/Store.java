package utils.store;

import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonArrayBuilder;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stores all information about concerts and purchases
 */
public class Store {
    private HashMap<Integer, Concert> concerts;
    private HashMap<Integer, Purchase> purchases;

    // counter
    private final AtomicInteger concertID = new AtomicInteger(0);

    /**
     * Constructor for the Store class.
     * @param json The path to the JSON file.
     */
    public Store(String json) {
        try {
            this.concerts = parseJson(Json.createReader(new FileInputStream(json)));
            this.purchases = new HashMap<>();
        }
        catch (FileNotFoundException e) {
            System.err.println("File not found: " + e.getMessage());
        }
    }

    /**
     * Parses the JSON file.
     * @param reader JSON reader object
     * @return A hash map where the key is the concert ID, and the value is the concert object.
     */
    private HashMap<Integer, Concert> parseJson(JsonReader reader) {
        // set up
        JsonArray jsonArray = reader.readArray();
        HashMap<Integer, Concert> map = new HashMap<>();

        // iterate through the array
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject jsonObject = jsonArray.getJsonObject(i);

            int id = concertID.getAndIncrement();

            map.put(id, new Concert(
                    jsonObject.getString("artist"),
                    jsonObject.getInt("count"),
                    jsonObject.getString("venue"),
                    jsonObject.getString("datetime"),
                    id
            ));
        }

        return map;
    }

    /**
     * Creates a JSON object that contains every concert in the store.
     * @return The concerts in the store as represented in JSON.
     */
    public JsonObject toJson() {
        JsonArrayBuilder builder = Json.createArrayBuilder();

        for (Concert concert : concerts.values()) {
            builder.add(concert.toJson());
        }

        return Json.createObjectBuilder().add("concerts", builder).build();
    }

    /**
     * Adds a new purchase to the purchase list.
     * @param purchase The purchase to be added.
     */
    public void addPurchase(Purchase purchase) {
        purchases.put(purchase.getId(), purchase);
    }

    /**
     * Retrieves a concert based on its unique ID.
     * @param id The ID of the concert.
     * @return The concert object, or null if the concert does not exist.
     */
    public Concert getConcert(int id) {
        return concerts.getOrDefault(id, null);
    }

    /**
     * Retrieves a purchase based on its unique ID.
     * @param id The ID of the purchase.
     * @return The purchase object, or null if the concert does not exist.
     */
    public Purchase getPurchase(Integer id) {
        return purchases.getOrDefault(id, null);
    }

    /**
     * Retrieves the list of all purchases.
     * @return A list of all purchases made.
     */
    public List<Purchase> getPurchases() {
        return new ArrayList<>(purchases.values());
    }
}