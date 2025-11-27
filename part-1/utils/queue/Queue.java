package utils.queue;

import utils.queue.common.QueueFullException;
import utils.queue.interfaces.IQueue;

import utils.store.Purchase;
import utils.store.Store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.Map;
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
    private final Map<Integer, Long> queueTimestamps = new HashMap<>();
    private final Map<Integer, Boolean> completedPurchases = new HashMap<>();

    private static final long MIN_QUEUE_TIME = 10000; // 10 seconds

    public Queue(Store store) {
        this.store = store;
        this.CAPACITY = 128; // fair size?
        this.queue = new ArrayList<>();

        worker.scheduleAtFixedRate(this::dequeue, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    public void enqueue(Purchase purchase) {
        try {
            if (queue.size() < CAPACITY) {
                synchronized (queue) {
                    queue.add(purchase);
                    queueTimestamps.put(purchase.getId(), System.currentTimeMillis());
                }
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
        synchronized (queue) {
            if (!queue.isEmpty()) {
                Purchase purchase = queue.getFirst();
                Long entryTimestamp = queueTimestamps.get(purchase.getId());

                if (entryTimestamp != null) {
                    long timeInQueue = System.currentTimeMillis() - entryTimestamp;

                    // Only process if it's been in queue long enough
                    if (timeInQueue >= MIN_QUEUE_TIME) {
                        queue.removeFirst();
                        queueTimestamps.remove(purchase.getId());

                        // assign ticket IDs
                        List<String> ids = getTicketIds(purchase.getNumberOfTickets());
                        purchase.setTicketIDs(ids);

                        // reduce tickets
                        purchase.getConcert().reduceCount(purchase.getNumberOfTickets());
                        completedPurchases.put(purchase.getId(), true);

                        System.out.println("Purchase " + purchase.getId() + " processed after " + timeInQueue + "ms");
                    } else {
                        System.out.println("Purchase " + purchase.getId() + " waiting... (" +
                                (MIN_QUEUE_TIME - timeInQueue) + "ms remaining)");
                    }
                }
            }
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
        // is purchase
        if (completedPurchases.getOrDefault(id, false)) {
            return -1;
        }

        synchronized (queue) {
            for (int i = 0; i < queue.size(); i++) {
                // see if this purchase is in the queue
                if (queue.get(i).getId() == id) {
                    return i;
                }
            }
        }

        return -3;
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
}
