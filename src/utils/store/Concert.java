package utils.store;

import javax.json.Json;
import javax.json.JsonObject;

/**
 * Object which represents a concert.
 */
public class Concert {
    private final String artist;
    private final String venue;
    private final String dateTime;
    private final int id;

    private int count;

    /**
     * Constructor for the Concert class.
     * @param artist The artist performing the concert.
     * @param count The number of tickers available for the concert.
     * @param venue The place the concert will be performed at.
     * @param dateTime The day and time the concert will be performed.
     * @param id The id for the concert.
     */
    public Concert(String artist, int count, String venue, String dateTime, int id) {
        this.artist = artist;
        this.count = count;
        this.venue = venue;
        this.dateTime = dateTime;
        this.id = id;
    }

    /**
     * Retrieves the artist performing the concert.
     * @return The artist performing the concert.
     */
    public String getArtist() {
        return artist;
    }

    /**
     * Retrieves the number of tickets currently available.
     * @return The number of tickets currently available.
     */
    public int getCount() {
        return count;
    }

    /**
     * Retrieves the location of the concert.
     * @return The location of the concert.
     */
    public String getVenue() {
        return venue;
    }

    /**
     * Retrieves the day and time when the concert will be performed.
     * @return The day and time the concert will be performed.
     */
    public String getDateTime() {
        return dateTime;
    }

    /**
     * Retrieves the unique ID of the concert.
     * @return The unique ID of the concert, as expressed by an integer.
     */
    public int getId() {
        return id;
    }

    /**
     * Decreases the number of tickets available for the concert.
     * @param amount The number of tickets to remove.
     */
    public void reduceCount(int amount) {
        this.count -= amount;
    }

    /**
     * Increases the number of tickets available for the concert.
     * @param amount The number of tickets to add.
     */
    public void increaseCount(int amount) {
        this.count += amount;
    }

    /**
     * Generates a JSON object to represent the concert.
     * @return A JSON object representing the concert.
     */
    public JsonObject toJson() {
        return Json.createObjectBuilder()
                .add("id", getId())
                .add("count", getCount())
                .add("artist", getArtist())
                .add("venue", getVenue())
                .add("dateTime", getDateTime())
                .build();
    }
}
