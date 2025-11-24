package utils.queue.interfaces;

import utils.queue.common.QueueFullException;
import utils.queue.common.QueueEmptyException;
import utils.store.Concert;

public interface IQueue {
    /**
     * Adds an element to the end of the queue.
     *
     * @param concert the concert to be queued
     */
    int enqueue(Concert concert);

    /**
     * Removes the element at the head of the queue.
     *
     * @return the element removed
     */
    Object dequeue();

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
}
