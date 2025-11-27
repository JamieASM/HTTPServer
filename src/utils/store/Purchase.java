package utils.store;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Object representing a ticket purchase.
 */
public class Purchase {
    private final Concert concert;
    private final int id;
    private final int numberOfTickets;
    private final List<String> ticketIDs;

    /**
     * Constructor for the Purchase class.
     * @param concert The concert the purchase is for.
     * @param id the unique id for the purchase.
     * @param numberOfTickets The number of tickets purchased.
     */
    public Purchase(Concert concert, int id, int numberOfTickets) {
        this.concert = concert;
        this.id = id;
        this.numberOfTickets = numberOfTickets;
        this.ticketIDs = new ArrayList<>();
    }

    /**
     * Retrieves the ID for the purchase.
     * @return The unique purchaseID
     */
    public int getId() {
        return id;
    }

    /**
     * Retrieves the concert.
     * @return The concert.
     */
    public Concert getConcert() {
        return concert;
    }

    /**
     * Retrieves the number of tickets purchased.
     * @return The number of tickets purchased.
     */
    public int getNumberOfTickets() {
        return numberOfTickets;
    }

    /**
     * Retrieves the ids of all individual tickets purchased.
     * @return A list of ticket IDs.
     */
    public List<String> getTicketIDs() {
        return ticketIDs;
    }

    /**
     * Assigns ticketIDs to all the tickets purchased in this object.
     * @param ticketIDs The List of ticket IDs.
     */
    public void setTicketIDs(List<String> ticketIDs) {
        this.ticketIDs.clear();
        this.ticketIDs.addAll(ticketIDs);
    }

    /**
     * Generates a JSON representation of the purchase.
     * @param position The position the purchase is at in the queue
     * @return A JSON object representing the purchase.
     */
    public JsonObject toJson(int position) {
        return Json.createObjectBuilder()
                .add("id", id)
                .add("tickets", numberOfTickets)
                .add("position", position)
                .add("ticketIds", toJsonArray())
                .build();
    }

    /**
     * Creates a JSON array of ticket IDs.
     * @return A JSON array of ticket IDs.
     */
    private JsonArrayBuilder toJsonArray() {
        JsonArrayBuilder builder = Json.createArrayBuilder();

        for (String id : ticketIDs) {
            builder.add(id);
        }

        builder.build();
        return builder;
    }
}
