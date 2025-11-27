package utils.queue.interfaces;

import utils.queue.common.QueueFullException;
import utils.store.Purchase;

/**
 * The interface class for a Queue
 */
public interface IQueue {
    /**
     * Appends a new purchase to the queue.
     * @param purchase An object representing the purchase request.
     * @throws QueueFullException Thrown when the Queue has been filled.
     */
    void enqueue(Purchase purchase) throws QueueFullException;

    /**
     * Removes the first element added to the Queue.
     */
    void dequeue();

    /**
     * Gets the current position of a specified purchase request from the queue.
     * @param id The id of the purchase request
     * @return The current position of the request.
     * If the request has already been completed, -1 is returned.
     * If the request does not exist -3 is returned.
     */
    int getPosition(int id);

    /**
     * Generates an id for the next purchase request.
     * @return The id of the next purchase request.
     */
    int reserveId();

    /**
     * Removes a specific purchase request from the queue.
     * This only occurs if the client has requested that the purchase to be cancelled.
     * @param id The id of the purchase request.
     */
    void remove(int id);
}
