package utils;

import utils.queue.interfaces.IQueue;
import utils.store.Concert;

public class MyRunnable implements Runnable {
    private final IQueue queue;
    private final Concert concert;

    private int id;

    public MyRunnable(IQueue queue, Concert concert) {
        this.queue = queue;
        this.concert = concert;
    }

    @Override
    public void run() {
        int min = 5;
        int delay = (int)(Math.random() * 6) + min;

        try {
            Thread.sleep((long)delay * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        id = queue.enqueue(concert);
        concert.reduceCount();
    }

    public int getId() {
        return id;
    }
}
