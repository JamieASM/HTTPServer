package utils.store;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import java.util.ArrayList;
import java.util.List;

public class Purchase {
    private final Concert concert;
    private final int id;
    private final int numberOfTickets;
    private final List<String> ticketIDs;

    public Purchase(Concert concert, int id, int numberOfTickets) {
        this.concert = concert;
        this.id = id;
        this.numberOfTickets = numberOfTickets;
        this.ticketIDs = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public Concert getConcert() {
        return concert;
    }

    public int getNumberOfTickets() {
        return numberOfTickets;
    }

    public void setTicketIDs(List<String> ticketIDs) {
        this.ticketIDs.clear();
        this.ticketIDs.addAll(ticketIDs);
    }

    public JsonObject toJson(int position) {
        if (position < 0) {
            return Json.createObjectBuilder()
                    .add("id", id)
                    .add("tickets", numberOfTickets)
                    .add("position", 0)
                    .add("ticketIds", toJsonArray())
                    .build();
        }
        else if (position > 0) {
            return Json.createObjectBuilder()
                    .add("id", id)
                    .add("tickets", numberOfTickets)
                    .add("position", position)
                    .add("ticketIds", Json.createArrayBuilder().build())
                    .build();
        }

        // otherwise, this is an error
        return null;
    }

    private JsonArrayBuilder toJsonArray() {
        JsonArrayBuilder builder = Json.createArrayBuilder();

        for (String id : ticketIDs) {
            builder.add(id);
        }

        builder.build();

        return builder;
    }
}
