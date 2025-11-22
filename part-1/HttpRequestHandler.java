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

public class HttpRequestHandler {
    private final BufferedReader reader;
    private final OutputStream outputStream;
    private final Store store;

    private final String documentRoot;

    public HttpRequestHandler(BufferedReader reader, OutputStream outputStream, String documentRoot) {
        this.reader = reader;
        this.outputStream = outputStream;
        this.documentRoot = documentRoot;
        this.store = new Store("./tickets.json");
    }

    protected void handleRequest() {
        try {
            // Read the request
            String line = reader.readLine();
            if (line == null || line.isEmpty()) {
                return;
            }

            // for debugging
            System.out.println(line);

            // parse the request
            HttpRequest req = parseRequest(line);
            serveContent(req);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private HttpRequest parseRequest(String line) {
        String[] parts = line.split(" ");
        if (parts.length < 2) {
            return new HttpRequest("GET", "/");
        }
        return new HttpRequest(parts[0], parts[1]);
    }

    private HttpResponse make404() {
        return new HttpResponse(
                HttpStatus.NOTFOUND,
                ContentType.textPlain,
                "404 File Not Found".getBytes()
        );
    }

    private void serveContent(HttpRequest req) throws IOException {
        // Assume we haven't got the content.
        HttpResponse res = make404();

        // get the path of the request
        String path = req.path().toLowerCase();

        if (path.equals("/")) {
            res = new HttpResponse(
                    HttpStatus.OK,
                    ContentType.html,
                    Files.readAllBytes(new File(documentRoot + "/index.html").toPath())
            );
        }
        else if (path.equals("/styles.css")) {
            res = new HttpResponse(
                    HttpStatus.OK,
                    ContentType.css,
                    Files.readAllBytes(new File(documentRoot + "/styles.css").toPath())
            );
        }
        else if (path.startsWith("/tickets")) {
            res = handleTicketRequest(req, path.substring("/tickets".length()));
        }
        else if (path.startsWith("/queue")) {
            res = handleQueueRequest(req, path.substring("/queue".length()));
        }

        res.sendResponse(outputStream);
    }

    public HttpResponse handleTicketRequest(HttpRequest req, String path) throws IOException {
        // can only be a get request
        if (!req.method().equals("GET")) {
            return make404();
        }

        // if we want the information for all artists.
        if (path.isBlank() || path.equals("/")) {
            return new HttpResponse(
                    HttpStatus.OK,
                    ContentType.textPlain,
                    store.getConcerts()
            );
        }

        String artist = path.substring(1).replace("-", " ");

        if (store.getConcert(artist) != null) {
            return new HttpResponse(
                    HttpStatus.OK,
                    ContentType.textPlain,
                    store.getConcert(artist).toString().getBytes()
            );
        }

        // otherwise there is no artist in the store
        return make404();
    }

    private HttpResponse handleQueueRequest(HttpRequest req, String path) throws IOException {
        return make404();
    }
}
