import utils.queue.interfaces.IQueue;
import utils.queue.Queue;

import utils.store.Concert;
import utils.store.Purchase;
import utils.store.Store;

import utils.HttpRequest;
import utils.HttpResponse;

import utils.enums.HttpStatus;
import utils.enums.ContentType;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.IOException;
import java.io.File;
import java.io.StringReader;

import java.nio.file.Files;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Processes all requests made by the client to the HTTP server.
 */
public class HttpRequestHandler {
    private final BufferedReader reader;
    private final OutputStream outputStream;
    private final String documentRoot;

    private static final Store store = new Store("./tickets.json");
    private static final IQueue queue = new Queue(store);
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    private HttpRequest req;

    /**
     * Constructor for the HttpRequestHandler class.
     * @param reader To read client requests.
     * @param outputStream To write server responses.
     * @param documentRoot The root of all client side resources.
     */
    public HttpRequestHandler(BufferedReader reader, OutputStream outputStream, String documentRoot) {
        this.reader = reader;
        this.outputStream = outputStream;
        this.documentRoot = documentRoot;
    }

    /**
     * Processes requests made by the client to the server.
     */
    protected void handleRequest() {
        try {
            // read the request
            req = buildRequest();
            HttpResponse res;

            // get the path of the request
            String path = req.path().toLowerCase();

            if (path.equals("/")) {
                res = serveIndex();
            }
            else if (path.equals("/styles.css")) {
                res = serveStatic("/styles.css", ContentType.css);
            }
            else if (path.equals("/scripts/index.js")) {
                res = serveStatic("/scripts/index.js", ContentType.javascript);
            }
            else if (path.startsWith("/tickets")) {
                res = handleTicketRequest();
            }
            else if (path.startsWith("/queue")) {
                res = handleQueueRequest(path);
            }
            else { // otherwise return a 404 error
               res = make404();
            }

            res.sendResponse(outputStream);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads the clients request and generates an appropriate request object.
     * @return An object representing the request.
     * @throws IOException Should not be thrown.
     */
    private HttpRequest buildRequest() throws IOException {
        String start = reader.readLine();
        String[] parts = start.split(" ");

        // i.e. GET /tickets HTTP/1.1
        String method = parts[0];
        String path = parts[1];

        // Get the headers
        Map<String, String> headers = parseHeaders();

        // Get the body (if there is one)
        int contentLength = headers.containsKey("Content-Length")
                ? Integer.parseInt(headers.get("Content-Length"))
                : 0;

        // build the string using the chars of the content
        char[] chars = new char[contentLength];
        reader.read(chars, 0, contentLength);

        String body = new String(chars);

        return new HttpRequest(method, path, headers, body);
    }

    // SERVING FILES

    /**
     * Attempts to serve a file from the document root to the client.
     * @param file The file the client has requested.
     * @param contentType The type of content the requested file contains (i.e. html, css).
     * @return A HttpResponse object.
     * @throws IOException Thrown when the file cannot be read.
     */
    private HttpResponse serveStatic(String file, ContentType contentType) throws IOException {
        File f  = new File(documentRoot + file);

        if (!f.exists()) {
            return make404();
        }

        return new HttpResponse(
                HttpStatus.OK,
                contentType,
                Files.readAllBytes(f.toPath()),
                new HashMap<>()
        );
    }

    /**
     * Generates an HttpResponse for the root HTML file (index.html).
     * @return A HttpResponse object.
     * @throws IOException if the file cannot be read.
     */
    private HttpResponse serveIndex() throws IOException {
        return serveStatic("/index.html", ContentType.html);
    }

    /**
     * Creates a 404 error object.
     * @return A HttpResponse object.
     */
    private HttpResponse make404() {
        return new HttpResponse(
                HttpStatus.NOT_FOUND,
                ContentType.textPlain,
                "404 File Not Found".getBytes(),
                new HashMap<>()
        );
    }

    /**
     * Creates a 500 error object.
     * @param message The reasoning for the error.
     * @return A HttpResponse object.
     */
    private HttpResponse make500(String message) {
        return new HttpResponse(
                HttpStatus.SERVER_ERROR,
                ContentType.textPlain,
                message.getBytes(),
                new HashMap<>()
        );
    }

    /**
     * Creates a 400 error object.
     * @param message The reasoning for the error.
     * @return A HttpResponse object.
     */
    private HttpResponse make400(String message) {
        return new HttpResponse(
                HttpStatus.BAD_REQUEST,
                ContentType.textPlain,
                message.getBytes(),
                new HashMap<>()
        );
    }

    /**
     * Creates a 200 object.
     * @param message The associated message for the response.
     * @return A HttpResponse object.
     */
    private HttpResponse make200(String message) {
        return new HttpResponse(
                HttpStatus.OK,
                ContentType.textPlain,
                message.getBytes(),
                new HashMap<>()
        );
    }

    private HttpResponse queue404() {
        JsonObject json = Json.createObjectBuilder()
                .add("status", "waiting..")
                .add("position", -3)
                .build();

        return new HttpResponse(
                HttpStatus.OK,
                ContentType.json,
                json.toString().getBytes(),
                new HashMap<>()
        );
    }

    /**
     *
     * @return
     * @throws IOException
     */
    private HashMap<String, String> parseHeaders() throws IOException {
        HashMap<String, String> headers = new HashMap<>();
        String line;

        while((line = reader.readLine()) != null && !line.isEmpty()) {
            int colon = line.indexOf(':');
            if (colon != -1) {
                String key = line.substring(0, colon).trim();
                String value = line.substring(colon + 1).trim();
                headers.put(key, value);
            }
        }

        return headers;
    }

    // GET /tickets

    public HttpResponse handleTicketRequest() {
        /*
        For example,
        GET /tickets HTTP/1.1
        Accept: application/json
         */

        if (!req.method().equals("GET")) {
            return make500("Only GET is allowed for /tickets.");
        }

        if (!req.headers().get("Accept").equals("application/json")) {
            return make500("Accept must be application/json.");
        }

        if (req.headers().containsKey("Content-Length")) {
            return make500("Unexpected content was provided.");
        }

        // otherwise, return all the tickets
        return new HttpResponse(
                HttpStatus.OK,
                ContentType.json,
                store.toString().getBytes(),
                new HashMap<>()
        );
    }

    // /queue REQUESTS

    private HttpResponse handleQueueRequest(String path) throws IOException {
        if (req.method().equals("GET")) {
            return handleQueueGetRequest(path);
        }
        else if (req.method().equals("POST")) {
            return handleQueuePostRequest(path.substring("/queue".length()).replaceAll("-", " "));
        }

        return make500("The /queue request made is invalid.");
    }

    // POST

    private HttpResponse handleQueuePostRequest(String artist) {
        // check the request headers
        if (!req.headers().get("Accept").equals("application/json")) {
            return make500("Accept must be application/json");
        }
        else if (!req.headers().get("Content-Type").equals("application/json")) {
            return make500("Content-Type must be application/json");
        }

        // check that an artist has been attached.
        if (artist.isBlank() || artist.equals("/")) {
            return make400("Invalid request: Missing artist name");
        }

        artist = artist.substring(1);

        // get the concert
        Concert concert = store.getConcert(artist);

        // check that the concert exists
        if (concert == null) {
            return make400("Invalid request: Invalid artist name");
        }

        // now we know that the concert is valid, we need to see if there are tickets available
        int numberOfTickets = parseNumberOfTickets();

        if (concert.getCount() <= numberOfTickets) {
            return make200("The number of tickets requested exceeds the number of tickets available.");
        }

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

    private int parseNumberOfTickets() {
        JsonReader reader = Json.createReader(new StringReader(req.body()));
        JsonObject jsonObject = reader.readObject();
        int numberOfTickets = jsonObject.getInt("tickets");
        reader.close();

        return numberOfTickets;
    }

    // GET

    private HttpResponse handleQueueGetRequest(String path) {
        if (!path.startsWith("/queue/")) {
            return make404();
        }

        // get the id
        String ticketId = path.substring("/queue/".length());
        int id;

        try {
            id = Integer.parseInt(ticketId);
        } catch (NumberFormatException e) {
            return make500("Ticket id must be an integer");
        }

        // See the position of the id
        int position = queue.getPosition(id);

        Purchase purchase = store.getPurchase(id);
        System.out.println(store.getPurchase(id));

        if (purchase == null) {
            return make404();
        }

        JsonObject json = purchase.toJson(position);

        return new HttpResponse(
                HttpStatus.OK,
                ContentType.json,
                json.toString().getBytes(),
                new HashMap<>()
        );
    }
}
