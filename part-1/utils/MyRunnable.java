package utils;

import utils.queue.interfaces.IQueue;
import utils.store.Concert;

public class MyRunnable implements Runnable {
    private final IQueue queue;
    private final Concert concert;
    private final int number;

    private int id;

    public MyRunnable(IQueue queue, Concert concert, int number) {
        this.queue = queue;
        this.concert = concert;
        this.number = number;
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

        id = queue.enqueue(concert, number);
        concert.reduceCount();
    }

    public int getId() {
        return id;
    }
}
