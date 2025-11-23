package JsonParser;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import java.util.HashMap;

public class Store {
    private HashMap<String, Concert> concerts;

    public Store(String json) {
        try {
            this.concerts = parseJson(Json.createReader(new FileInputStream(json)));
        }
        catch (FileNotFoundException e) {
            System.err.println("File not found: " + e.getMessage());
        }
    }

    private HashMap<String, Concert> parseJson(JsonReader reader) {
        JsonArray jsonArray = reader.readArray();

        HashMap<String, Concert> map = new HashMap<>();

        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject jsonObject = jsonArray.getJsonObject(i);

            map.put(jsonObject.getString("artist").toLowerCase(), new Concert(
                    jsonObject.getString("artist"),
                    jsonObject.getInt("count"),
                    jsonObject.getString("venue"),
                    jsonObject.getString("datetime")
            ));
        }

        return map;
    }

    public Concert getConcert(String artist) {
        return concerts.get(artist);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");

        for (String artist : concerts.keySet()) {
            sb.append(concerts.get(artist).toString());
        }

        sb.append("]");

        return sb.toString();
    }
}