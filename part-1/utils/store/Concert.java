package utils.store;

public class Concert {
    private int count;
    private final String artist;
    private final String venue;
    private final String dateTime;

    public Concert(String artist, int count, String venue, String dateTime) {
        this.artist = artist;
        this.count = count;
        this.venue = venue;
        this.dateTime = dateTime;
    }

    public String getArtist() {
        return artist;
    }

    public int getCount() {
        return count;
    }

    public String getVenue() {
        return venue;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void reduceCount(int amount) {
        this.count -= amount;
    }

    @Override
    public String toString() {
        return """
                {
                "count": "%d",
                "artist": "%s",
                "venue": "%s",
                "datetime": "%s"
                }
                """.formatted(
                getCount(),
                getArtist(),
                getVenue(),
                getDateTime());
    }
}
