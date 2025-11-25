package utils.queue;

import utils.queue.common.QueueEmptyException;
import utils.queue.common.QueueFullException;
import utils.queue.interfaces.IQueue;

import utils.store.Concert;
import utils.store.Purchase;
import utils.store.Store;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Queue implements IQueue {
    private final int CAPACITY;
    private final List<Purchase> queue;
    private final Store store;
    private final AtomicInteger queueId = new AtomicInteger(0);
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor();
    private final AtomicInteger ticketCounter = new AtomicInteger(1);

    public Queue(Store store) {
        this.store = store;
        this.CAPACITY = 128; // fair size?
        this.queue = new ArrayList<>();

        worker.scheduleAtFixedRate(this::processQueue, 0, 100, TimeUnit.MILLISECONDS);
    }

    @Override
    public void enqueue(Concert concert, int numberOfTickets, int id) {
        try {
            if (queue.size() < CAPACITY) {
                Purchase purchase = new Purchase(concert, id, numberOfTickets);
                // add to the queue and store
                store.addPurchase(purchase);
                queue.add(purchase);
            } else {
                throw new QueueFullException();
            }
        }
        catch (QueueFullException e) {
            System.out.println("Queue full");
        }
    }

    @Override
    public void dequeue() {
        try {
            if(queue.isEmpty()) {
                throw new QueueEmptyException();
            }

            Purchase purchase = queue.removeFirst();
            // get the purchase ids
            List<String> ids = getTicketIds(purchase.getNumberOfTickets());
            purchase.setTicketIDs(ids);
        } catch (QueueEmptyException e) {
            System.out.println("Queue empty");
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

    @Override
    public int reserveId() {
        return queueId.getAndIncrement();
    }

    public int getPosition(int id) {
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).getId() == id) {
                return i;
            }
        }

        System.out.println("Not a member of this queue");
        return -1;
    }

    private List<String> getTicketIds(int numberOfTickets) {
        List<String> ticketIds = new ArrayList<>();

        for (int i = 0; i < numberOfTickets; i++) {
            ticketIds.add(makePurchaseID());
        }

        return ticketIds;
    }

    private String makePurchaseID() {
        return "T-" + ticketCounter.getAndIncrement();
    }

    private void processQueue() {
        synchronized (queue) {
            if (!queue.isEmpty()) {
                Purchase purchase = queue.removeFirst();

                // assign ticket IDs
                List<String> ids = getTicketIds(purchase.getNumberOfTickets());
                purchase.setTicketIDs(ids);

                // reduce tickets
                purchase.getConcert().reduceCount(purchase.getNumberOfTickets());
            }
        }
    }
}
