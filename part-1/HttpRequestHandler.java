import JsonParser.Concert;
import JsonParser.Store;

import utils.ContentType;
import utils.HttpRequest;
import utils.HttpResponse;
import utils.HttpStatus;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class HttpRequestHandler {
    private final BufferedReader reader;
    private final OutputStream outputStream;
    private final Store store;

    private final String documentRoot;

    private HttpRequest req;

    public HttpRequestHandler(BufferedReader reader, OutputStream outputStream, String documentRoot) {
        this.reader = reader;
        this.outputStream = outputStream;
        this.documentRoot = documentRoot;
        this.store = new Store("./tickets.json");
    }

    protected void handleRequest() {
        try {
            req = buildRequest();
            HttpResponse res;

            // get the path of the request
            String path = req.path().toLowerCase();

            if (path.equals("/styles.css")) {
                res = new HttpResponse(
                        HttpStatus.OK,
                        ContentType.css,
                        Files.readAllBytes(new File(documentRoot + "/styles.css").toPath())
                );
            }
            else if (path.startsWith("/tickets")) {
                res = handleTicketRequest(path.substring("/tickets".length()));
            }
            else if (path.startsWith("/queue/")) {
                res = handleQueueRequest(path.substring("/queue".length()));
            }
            else {
                res = serveIndex();
            }

            res.sendResponse(outputStream);

        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private HttpRequest buildRequest() throws IOException {
        // declare our variables
        String method;
        String path;
        Map<String, String> headers;
        String body;

        // GET, URL
        String[] line = reader.readLine().split(" ");
        method = line[0];
        path = line[1];

        // headers
        headers = parseHeaders();

        // body
        int contentLength = headers.containsKey("Content-Length") ? Integer.parseInt(headers.get("Content-Length")) : 0;

        char[] bodyChars = new char[contentLength];
        reader.read(bodyChars, 0, contentLength);
        body = new String(bodyChars);
        System.out.println("Body received: " + body);

        return new HttpRequest(method, path, headers, body);
    }

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

    private HttpResponse make404() {
        return new HttpResponse(
                HttpStatus.NOT_FOUND,
                ContentType.textPlain,
                "404 File Not Found".getBytes()
        );
    }

    private HttpResponse serveIndex() throws IOException {
        return new HttpResponse(
                HttpStatus.OK,
                ContentType.html,
                Files.readAllBytes(new File(documentRoot + "/index.html").toPath())
        );
    }

    public HttpResponse handleTicketRequest(String path) {
        /*
        For example:
        GET /tickets HTTP/1.1
        Accept: application/json
         */

        if (req.method().equals("GET") && req.headers().get("Accept").equals("application/json") && !req.headers().containsKey("Content-Length")) {
            // if we want the information for all artists.
            if (path.isBlank() || path.equals("/")) {
                return new HttpResponse(
                        HttpStatus.OK,
                        ContentType.json,
                        store.toString().getBytes()
                );
            }

            String artist = path.substring(1).replace("-", " ");

            if (store.getConcert(artist) != null) {
                return new HttpResponse(
                        HttpStatus.OK,
                        ContentType.json,
                        store.getConcert(artist).toString().getBytes()
                );
            }
        }

        // otherwise it is a 500 error
        return new HttpResponse(
                HttpStatus.SERVER_ERROR,
                ContentType.textPlain,
                "500 Internal Server Error".getBytes()
        );
    }

    // TODO: Fix Queue
    private HttpResponse handleQueueRequest(String path) {
        if (req.method().equals("POST")) {
            // assume that this is for a specific artist
            String artist = path.substring(1).replace("-", " ");
            Concert concert = store.getConcert(artist);

            if (concert != null) {
                // TODO: Add this to our queue
                concert.reduceCount();

                return new HttpResponse(
                        HttpStatus.OK,
                        ContentType.textPlain,
                        String.format("Submitted a new ticket request for %s", artist).getBytes()
                );
            }
        }
        else if (req.method().equals("GET")) {
            String id = path.replace(":", "");

            // TODO: get the position from the queue

            return new HttpResponse(
                    HttpStatus.OK,
                    ContentType.textPlain,
                    "You are at position 3".getBytes()
            );
        }

        return make404();
    }
}
