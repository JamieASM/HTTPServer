package utils.store;

public class Ticket {
    private final Concert concert;
    private int queueID;
    private String purchasedID;
    private boolean purchased;

    public Ticket(Concert concert, int queueID) {
        // a newly made ticket cannot have yet been purchased
        this.concert = concert;
        this.queueID = queueID;
        this.purchasedID = "";
        purchased = false;
    }

    public void setPurchased(String id) {
        // if the item is purchased, it is no longer in the queue
        this.queueID = -1;
        this.purchasedID = id;
        this.purchased = true;
    }

    public Concert getConcert() {
        return concert;
    }

    public int getQueueID() {
        return queueID;
    }

    public String getPurchasedID() {
        return purchasedID;
    }

    public boolean isPurchased() {
        return purchased;
    }
}
