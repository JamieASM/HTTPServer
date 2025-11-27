package utils.request.types;

import utils.HttpResponse;

import utils.request.DefaultResponses;
import utils.request.HttpRequest;

import utils.request.enums.ContentType;
import utils.request.enums.HttpStatus;

import utils.store.Concert;
import utils.store.Purchase;
import utils.store.Store;

import javax.json.JsonReader;
import javax.json.Json;
import javax.json.JsonObject;

import java.io.StringReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

/**
 * Helper class to process '/ticket' HTTP requests.
 */
public class TicketRequest {
    private final DefaultResponses defaultResponses = new DefaultResponses();
    private final Store store;

    private HttpRequest req;

    /**
     * Constructor for TicketRequest.
     * @param store The primary storage for concerts and purchases.
     */
    public TicketRequest(Store store) {
        this.store = store;
    }

    /**
     * Processes '/ticket' requests.
     * @param request The request made by the client.
     * @return A HttpResponse object.
     */
    public HttpResponse handleRequest(HttpRequest request) {
        this.req = request;

        // Check the method type. Valid methods: GET, POST.
        if (
                req.method().equals("GET")
                && req.headers().get("Accept").equals("application/json")
        ) {
            return handleGetRequest();
        }
        else if (
                req.method().equals("POST")
                && req.headers().get("Accept").equals("application/json")
                && req.headers().get("Content-Type").equals("application/json")
        ) {
            return handlePostRequest();
        } // otherwise, the requested method is illegal
        else {
            return defaultResponses.make500("Unsupported or malformed HTTP request.");
        }
    }

    /**
     * Processes a 'GET /tickets' requests.
     * Allowed /tickets requests may look like '/tickets' or '/tickets/{ID}'.
     * @return A HTTPResponse object.
     */
    private HttpResponse handleGetRequest() {
        // split so that we have something like ["tickets", "{id}"]
        List<String> parts = Arrays.stream(req.path().split("/"))
                .filter(s -> !s.isEmpty())
                .toList();

        // if the parts are now empty, something has gone wrong
        if (parts.isEmpty()) {
            return defaultResponses.make500("Missing path parameter");
        }

        // if the client wants all tickets
        if (parts.size() == 1) {
            return new HttpResponse(
                    HttpStatus.OK,
                    ContentType.json,
                    store.toJson().toString().getBytes(),
                    new HashMap<>()
            );
        }

        // otherwise, the client wants a specific concert
        int concertID = Integer.parseInt(parts.get(1));

        // try to get the concert
        Concert concert = store.getConcert(concertID);

        if (concert == null) {
            return defaultResponses.make500("Concert not found");
        }

        // so the concert requested must be valid.
        return new HttpResponse(
                HttpStatus.OK,
                ContentType.json,
                store.getConcert(concertID).toJson().toString().getBytes(),
                new HashMap<>()
        );
    }

    /**
     * Processes a 'POST /tickets/refund' request.
     * This request must contain a JSON object with ticketIDs.
     * @return A HttpResponse object.
     */
    private HttpResponse handlePostRequest() {
        // split so that we have something like ["tickets", "refund"]
        List<String> parts = Arrays.stream(req.path().split("/"))
                .filter(s -> !s.isEmpty())
                .toList();

        // the second part should be 'refund' and there a should be content in the body
        if (!parts.get(1).equalsIgnoreCase("refund") || parts.size() != 2) {
            return defaultResponses.make400("Unsupported or malformed HTTP request.");
        }

        if (req.body() == null || req.body().isEmpty()) {
            return defaultResponses.make400("Missing body.");
        }

        // We can now assume the request is mostly valid. So let's parse the JSON body
        List<String> ticketIds = parseTicketIds();
        HashMap<Concert, Integer> ticketsPerConcert = new HashMap<>();
        List<String> invalidIds = new ArrayList<>();

        for (String ticketId : ticketIds) {
            Purchase found = null;
            boolean exists = false;

            for (Purchase purchase : store.getPurchases()) {
                if (purchase.getTicketIDs().contains(ticketId)) {
                    found = purchase;
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                invalidIds.add(ticketId);
            }

            if (found == null) {
                continue;
            }

            Concert concert = found.getConcert();
            ticketsPerConcert.merge(concert, 1, Integer::sum);
        }

        // Handle invalid IDs
        if (!invalidIds.isEmpty()) {
            return defaultResponses.make400("Invalid ticket IDs: " + invalidIds);
        }

        // now we know the number of ticketsPerConcert, so let's add those tickets again
        for (Map.Entry<Concert, Integer> entry : ticketsPerConcert.entrySet()) {
            entry.getKey().increaseCount(entry.getValue());
        }

        JsonObject responseJson = Json.createObjectBuilder()
                .add("refundedCount", ticketIds.size())
                .build();

        return new HttpResponse(
                HttpStatus.OK,
                ContentType.json,
                responseJson.toString().getBytes(),
                new HashMap<>()
        );
    }

    /**
     * Processes a JSON body to get a list of ticket ids.
     * @return A list of Ticket Ids.
     */
    private List<String> parseTicketIds() {
        System.out.println(req.body());

        JsonReader reader = Json.createReader(new StringReader(req.body()));
        JsonObject jsonObject = reader.readObject();

        if (!jsonObject.containsKey("ticketIDs")) {
            throw new IllegalArgumentException("Ticket ID missing");
        }

        return jsonObject.getJsonArray("ticketIDs").stream()
                .map(jsonValue -> {
                    switch (jsonValue.getValueType()) {
                        case STRING:
                            return jsonValue.toString().replace("\"", ""); // remove quotes
                        default:
                            throw new IllegalArgumentException("ticketIds must be an array of strings");
                    }
                })
                .toList();
    }
}
