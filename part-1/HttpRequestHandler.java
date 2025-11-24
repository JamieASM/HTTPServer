import utils.store.Concert;
import utils.store.Store;

import utils.HttpRequest;
import utils.HttpResponse;

import utils.enums.HttpStatus;
import utils.enums.ContentType;

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

            System.out.println("Path: " + path);

            if (path.equals("/")) {
                res = serveIndex();
            }
            else if (path.equals("/styles.css")) {
                res = new HttpResponse(
                        HttpStatus.OK,
                        ContentType.css,
                        Files.readAllBytes(new File(documentRoot + "/styles.css").toPath()),
                        new HashMap<>()
                );
            }
            else if (path.startsWith("/tickets")) {
                res = handleTicketRequest(path.substring("/tickets".length()));
            }
            else if (path.startsWith("/queue/")) {
                res = handleQueueRequest(path.substring("/queue".length()));
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
                "404 File Not Found".getBytes(),
                new HashMap<>()
        );
    }

    private HttpResponse serveIndex() throws IOException {
        return new HttpResponse(
                HttpStatus.OK,
                ContentType.html,
                Files.readAllBytes(new File(documentRoot + "/index.html").toPath()),
                new HashMap<>()
        );
    }

    public HttpResponse handleTicketRequest(String path) {
        /*
        For example,
        GET /tickets HTTP/1.1
        Accept: application/json
         */

        if (
                req.method().equals("GET") &&
                req.headers().get("Accept").equals("application/json") &&
                !req.headers().containsKey("Content-Length")
        ) {
            // if we want the information for all artists.
            if (path.isBlank() || path.equals("/")) {
                return new HttpResponse(
                        HttpStatus.OK,
                        ContentType.json,
                        store.toString().getBytes(),
                        new HashMap<>()
                );
            }

            String artist = path.substring(1).replace("-", " ");

            if (store.getConcert(artist) != null) {
                return new HttpResponse(
                        HttpStatus.OK,
                        ContentType.json,
                        store.getConcert(artist).toString().getBytes(),
                        new HashMap<>()
                );
            }
        }

        // otherwise it is a 500 error
        return new HttpResponse(
                HttpStatus.SERVER_ERROR,
                ContentType.textPlain,
                "500 Internal Server Error".getBytes(),
                new HashMap<>()
        );
    }

    // TODO: Fix Queue
    private HttpResponse handleQueueRequest(String path) throws IOException {
        /*
         * For example:
         * POST /queue HTTP/1.1
         * Host: example.com
         * Accept: application/json
         * Content-Type: application/json
         *
         * {
         * "tickets": 2
         * }
         */

        if (
                req.method().equals("POST") &&
                        req.headers().get("Accept").equals("application/json") &&
                        req.headers().get("Content-Type").equals("application/json") &&
                        Integer.parseInt(req.headers().get("Content-Length")) > 0) {

            // bad request if no artist is provided
            if (path.isBlank() || path.equals("/")) {
                return new HttpResponse(
                        HttpStatus.BAD_REQUEST,
                        ContentType.textPlain,
                        "Invalid request: Missing artist name".getBytes(),
                        new HashMap<>()
                );
            }

            Concert concert = store.getConcert(path.substring(1).replace("-", " "));

            // if the concert doesn't exist, its another bad request
            if (concert == null) {
                return new HttpResponse(
                        HttpStatus.BAD_REQUEST,
                        ContentType.textPlain,
                        "Invalid Request: The provided artist name is invalid".getBytes(),
                        new HashMap<>()
                );
            }

            // otherwise, we need to check if the concert has remaining tickets
            if (concert.getCount() > 0) {
                // TODO: Add purchase to the queue
                int id = 3;
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Location", path + "/" + id);

                return new HttpResponse(
                        HttpStatus.CREATED,
                        ContentType.json,
                        """
                                        {
                                        "id": %d
                                        }
                                """.formatted(id).getBytes(),
                        headers
                );
            } else { // nothing has been able to be created
                return new HttpResponse(
                        HttpStatus.OK,
                        ContentType.json,
                        "{}".getBytes(),
                        new HashMap<>()
                );
            }
        }

        return serveIndex();
    }
}
