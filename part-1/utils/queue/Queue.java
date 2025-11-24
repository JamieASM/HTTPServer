package utils.queue;

import utils.queue.common.QueueEmptyException;
import utils.queue.common.QueueFullException;
import utils.queue.interfaces.IQueue;
import utils.store.Concert;
import utils.store.Store;
import utils.store.Ticket;

import java.util.ArrayList;
import java.util.List;

public class Queue implements IQueue {
    private final int CAPACITY;
    private final List<Ticket> queue;
    private final Store store;

    private int queueId;
    private int purchasedTicketId;

    public Queue(Store store) {
        this.store = store;
        this.CAPACITY = 128; // fair size?
        this.queue = new ArrayList<>(CAPACITY);
        this.queueId = 0;
        this.purchasedTicketId = 0;
    }

    @Override
    public int enqueue(Concert concert) {
        try {
            if (queue.size() < CAPACITY) {
                // add to the queue
                queue.add(new Ticket(concert, queueId++));
                return queueId;
            } else {
                throw new QueueFullException();
            }
        }
        catch (QueueFullException e) {
            System.out.println("Queue full");
            return -1; // indicates that it is wrong as queueID can never be negative
        }
    }

    @Override
    public String dequeue() {
        try {
            if(queue.isEmpty()) {
                throw new QueueEmptyException();
            }

            Ticket ticket = queue.removeFirst();
            // set the purchaseID
            String purchasedId = makePurchaseID();
            ticket.setPurchased(purchasedId);
            store.addPurchasedTicket(ticket);

            return purchasedId;
        } catch (QueueEmptyException e) {
            System.out.println("Queue empty");
            return null;
        }
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public void clear() {
        queue.clear();
    }

    public int getPosition(int id) {
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).getQueueID() == id) {
                return i;
            }
        }

        System.out.println("Error: a ticket with that id was not found");
        return -1;
    }

    private String makePurchaseID() {
        purchasedTicketId++;

        // TODO: nicely format if there is time
        return "T-" + purchasedTicketId;
    }
}
