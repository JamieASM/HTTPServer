package utils.request.types;

import utils.HttpResponse;

import utils.queue.interfaces.IQueue;

import utils.request.DefaultResponses;
import utils.request.HttpRequest;
import utils.request.enums.ContentType;
import utils.request.enums.HttpStatus;

import utils.store.Concert;
import utils.store.Purchase;
import utils.store.Store;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import java.io.StringReader;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Helper class to process '/queue' HTTP requests.
 */
public class QueueRequest {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    private final DefaultResponses defaultResponses = new DefaultResponses();
    private final Store store;
    private final IQueue queue;

    private HttpRequest req;

    /**
     * Constructor for QueueRequest.
     * @param store The primary storage for concerts and purchases.
     */
    public QueueRequest(Store store, IQueue queue) {
        this.store = store;
        this.queue = queue;
    }

    /**
     * Processes '/queue' requests.
     * @param request Teh request made by the client.
     * @return A HttpResponse Object.
     */
    public HttpResponse handleRequest(HttpRequest request) {
        this.req = request;

        // Check the method type. Valid methods: POST, GET, DELETE
        if (
                req.method().equals("GET")
                && req.headers().size() == 1
                && req.headers().get("Accept").equals("application/json")
        ) {
            return handleGetRequest();
        }
        else if (
                req.method().equals("POST")
                && req.headers().size() == 4
                && req.headers().get("Accept").equals("application/json")
                && req.headers().get("Content-Type").equals("application/json")
        ) {
            return handlePostRequest();
        }
        else if (
                req.method().equals("DELETE")
                && req.headers().size() == 2
                && req.headers().get("Accept").equals("application/json")
        ) {
            return handleDeleteRequest();
        }
        else {
            return defaultResponses.make500("Unsupported or malformed HTTP request.");
        }
    }

    /**
     * Processes a 'GET /queue/{concertID}/{queueID}' request.
     * @return A HttpResponse object.
     */
    private HttpResponse handleGetRequest() {
        // split so that we have something like ["queue", "{concertID}, "{id}"]
        List<String> parts = Arrays.stream(req.path().split("/"))
                .filter(s -> !s.isEmpty())
                .toList();

        // if the parts are now empty, something has gone wrong
        if (parts.size() != 3) {
            return defaultResponses.make500("Malformed path parameter");
        }

        int concertID = Integer.parseInt(parts.get(1));
        int queueID = Integer.parseInt(parts.get(2));

        // check tha the concert exists
        Concert concert = store.getConcert(concertID);
        Purchase purchase = store.getPurchase(queueID);

        if (concert == null || purchase == null) {
            return defaultResponses.make404();
        }

        int position = queue.getPosition(queueID);

        // otherwise, a purchase does exist and can be returned.
        JsonObject json = purchase.toJson(position);

        return new HttpResponse(
                HttpStatus.OK,
                ContentType.json,
                json.toString().getBytes(),
                new HashMap<>()
        );
    }

    /**
     * Processes a 'POST /queue/{concertID}' request.
     * This request must contain a JSON object with a number of tickets to be purchased.
     * @return A HttpResponse object.
     */
    private HttpResponse handlePostRequest() {
        // split so that we have something like ["queue", "{concertID}"]
        List<String> parts = Arrays.stream(req.path().split("/"))
                .filter(s -> !s.isEmpty())
                .toList();

        // if the parts are now empty, something has gone wrong
        if (parts.size() != 2) {
            return defaultResponses.make400("Malformed path parameter");
        }

        if (req.body() == null || req.body().isEmpty()) {
            return defaultResponses.make400("Missing body.");
        }

        // validate the concert
        int concertID = Integer.parseInt(parts.get(1));
        Concert concert = store.getConcert(concertID);

        if (concert == null) {
            return defaultResponses.make400("Invalid request: Invalid artist name");
        }

        // now we know that the concert is valid, we need to see if there are tickets available
        int numberOfTickets = parseNumberOfTickets();

        // check that this isn't more tickets than is available.
        if (concert.getCount() < numberOfTickets) {
            return defaultResponses.make200("The number of tickets requested exceeds the number of tickets available.");
        }

        // now we can assume everything is okay to fully process the request.

        // reserve an ID
        int id = queue.reserveId();

        // make a new purchase instance
        Purchase purchase = new Purchase(concert, id, numberOfTickets);
        store.addPurchase(purchase);

        // Add after a random delay (5-10 seconds)
        int delay = (int)(Math.random() * 6) + 5;

        scheduler.schedule(() -> {
            queue.enqueue(purchase);
        }, delay, TimeUnit.SECONDS);

        HashMap<String, String> headers = new HashMap<>();
        headers.put("Location", "/queue/" + id);

        return new HttpResponse(
                HttpStatus.CREATED,
                ContentType.json,
                ("{\"id\": " + id + "}").getBytes(),
                headers
        );
    }

    /**
     * Parses a JSON body to get the number of tickets the client would like to purchase.
     * @return The number of tickets the client wants to purchase.
     */
    private int parseNumberOfTickets() {
        JsonReader reader = Json.createReader(new StringReader(req.body()));
        JsonObject jsonObject = reader.readObject();
        int numberOfTickets = jsonObject.getInt("tickets");
        reader.close();

        return numberOfTickets;
    }

    /**
     * Processes a 'DELETE /queue/{queueID}' request.
     * @return A HttpResponse object.
     */
    public HttpResponse handleDeleteRequest() {
        // split so that we have something like ["queue", "{queueID}"]
        List<String> parts = Arrays.stream(req.path().split("/"))
                .filter(s -> !s.isEmpty())
                .toList();

        // if the parts are now empty, something has gone wrong
        if (parts.size() != 2) {
            return defaultResponses.make400("Malformed path parameter");
        }

        int queueID = Integer.parseInt(parts.get(1));

        int position = queue.getPosition(queueID);

        // if the position is negative, the ticket is not a member of the queue.
        if (position <= 0) {
            return defaultResponses.make404();
        }

        // otherwise, it is a member of the queue, so we need to remove it.
        queue.removeTicket(queueID);

        return defaultResponses.make200("Deleted.");
    }
}
