import utils.queue.interfaces.IQueue;
import utils.queue.Queue;

import utils.request.DefaultResponses;
import utils.request.types.QueueRequest;
import utils.request.types.TicketRequest;
import utils.store.Store;

import utils.request.HttpRequest;
import utils.HttpResponse;

import utils.request.enums.HttpStatus;
import utils.request.enums.ContentType;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.IOException;
import java.io.File;

import java.nio.file.Files;

import java.util.HashMap;
import java.util.Map;

/**
 * Processes all requests made by the client to the HTTP server.
 */
public class HttpRequestHandler {
    private final BufferedReader reader;
    private final OutputStream outputStream;
    private final String documentRoot;

    private static final Store store = new Store("./tickets.json");
    private static final IQueue queue = new Queue(store);

    private final DefaultResponses defaultResponses = new DefaultResponses();
    private final TicketRequest ticketRequest = new TicketRequest(store);
    private final QueueRequest queueRequest = new QueueRequest(store, queue);

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
     * Deciphers the type of request the client requires, and begins processing it.
     */
    protected void handleRequest() {
        try {
            // read the request
            HttpRequest req = buildRequest();
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
                res = ticketRequest.handleRequest(req);
            }
            else if (path.startsWith("/queue")) {
                res = queueRequest.handleRequest(req);
            }
            else { // otherwise return a 404 error
               res = defaultResponses.make404();
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
            return defaultResponses.make404();
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
     * Creates a map of headers from the client's request.
     * @return A map of headers.
     * @throws IOException Should not be thrown.
     */
    private HashMap<String, String> parseHeaders() throws IOException {
        HashMap<String, String> headers = new HashMap<>();
        String line;

        // iterates through every header
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
}
