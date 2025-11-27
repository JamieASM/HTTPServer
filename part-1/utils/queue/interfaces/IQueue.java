package utils.queue.interfaces;

import utils.queue.common.QueueFullException;
import utils.queue.common.QueueEmptyException;
import utils.store.Concert;
import utils.store.Purchase;

import java.util.concurrent.atomic.AtomicInteger;

public interface IQueue {
    /**
     * Adds an element to the end of the queue.
     *
     * @param concert the concert to be queued
     * @param number the number of tickets purchased
     */
    void enqueue(Purchase purchase);

    /**
     * Removes the element at the head of the queue.
     *
     * @return the element removed
     */
    void dequeue();

    /**
     * Returns the number of elements in the queue.
     * @return the number of elements in the queue
     */
    int size();

    /**
     * Checks whether the queue is empty.
     * @return true if the queue is empty
     */
    boolean isEmpty();

    /**
     * Removes all elements from the queue.
     */
    void clear();

    /**
     * Gets the current position of a ticket purchase in the queue
     * @param id The id of the ticket
     * @return The position of the ticket
     */
    int getPosition(int id);

    int reserveId();
}
