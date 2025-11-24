package utils.store;

public class Ticket {
    private final Concert concert;

    private int queueID;
    private String purchasedID;
    private boolean purchased;
    private int numberPurchased;

    public Ticket(Concert concert, int queueID, int numberPurchased) {
        // a newly made ticket cannot have yet been purchased
        this.concert = concert;
        this.queueID = queueID;
        this.purchasedID = "";
        this.purchased = false;
        this.numberPurchased = 0;
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
