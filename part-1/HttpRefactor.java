import utils.queue.interfaces.IQueue;
import utils.queue.Queue;

import utils.store.Concert;
import utils.store.Store;

import utils.HttpRequest;
import utils.HttpResponse;

import utils.enums.HttpStatus;
import utils.enums.ContentType;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class HttpRefactor {

//    private final BufferedReader reader;
//    private final OutputStream outputStream;
//    private final Store store;
//    private final String documentRoot;
//    private final IQueue queue;
//
//    private HttpRequest req;
//
//    public HttpRefactor(BufferedReader reader, OutputStream outputStream, String documentRoot) {
//        this.reader = reader;
//        this.outputStream = outputStream;
//        this.documentRoot = documentRoot;
//
//        // Load tickets.json from properties file (but hardcoded here for now)
//        this.store = new Store("./tickets.json");
//
//        // Single global queue (Part 1)
//        this.queue = new Queue(store);
//    }
//
//    protected void handleRequest() {
//        try {
//            req = buildRequest();
//            HttpResponse res;
//
//            String path = req.path().toLowerCase();
//            System.out.println("Path: " + path);
//
//            if (path.equals("/")) {
//                res = serveIndex();
//            }
//            else if (path.equals("/styles.css")) {
//                res = serveStatic("styles.css", ContentType.css);
//            }
//            else if (path.equals("/tickets")) {
//                res = handleTickets();
//            }
//            else if (path.startsWith("/queue")) {
//                res = handleQueue(path);
//            }
//            else {
//                res = make404();
//            }
//
//            res.sendResponse(outputStream);
//
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    /* ============================================================
//       REQUEST PARSING
//       ============================================================ */
//
//    private HttpRequest buildRequest() throws IOException {
//        String start = reader.readLine();
//        String[] parts = start.split(" ");
//
//        String method = parts[0];
//        String path = parts[1];
//
//        Map<String, String> headers = parseHeaders();
//
//        int contentLength = headers.containsKey("Content-Length")
//                ? Integer.parseInt(headers.get("Content-Length"))
//                : 0;
//
//        char[] chars = new char[contentLength];
//        reader.read(chars, 0, contentLength);
//
//        String body = new String(chars);
//
//        return new HttpRequest(method, path, headers, body);
//    }
//
//    private HashMap<String, String> parseHeaders() throws IOException {
//        HashMap<String, String> headers = new HashMap<>();
//        String line;
//
//        while ((line = reader.readLine()) != null && !line.isEmpty()) {
//            int colon = line.indexOf(':');
//            if (colon != -1) {
//                headers.put(line.substring(0, colon), line.substring(colon + 1).trim());
//            }
//        }
//
//        return headers;
//    }
//
//    /* ============================================================
//       STATIC FILE SERVING
//       ============================================================ */
//
//    private HttpResponse serveIndex() throws IOException {
//        return serveStatic("index.html", ContentType.html);
//    }
//
//    private HttpResponse serveStatic(String file, ContentType type) throws IOException {
//        File f = new File(documentRoot + "/" + file);
//
//        if (!f.exists()) return make404();
//
//        return new HttpResponse(
//                HttpStatus.OK,
//                type,
//                Files.readAllBytes(f.toPath()),
//                new HashMap<>()
//        );
//    }
//
//    private HttpResponse make404() {
//        return new HttpResponse(
//                HttpStatus.NOT_FOUND,
//                ContentType.textPlain,
//                "404 Not Found".getBytes(),
//                new HashMap<>()
//        );
//    }
//
//    /* ============================================================
//       /tickets  (GET only)
//       ============================================================ */
//
//    private HttpResponse handleTickets() {
//
//        if (!req.method().equals("GET"))
//            return make500("Only GET is allowed for /tickets");
//
//        if (!"application/json".equals(req.headers().get("Accept")))
//            return make500("Accept must be application/json");
//
//        // Return entire concert object
//        return new HttpResponse(
//                HttpStatus.OK,
//                ContentType.json,
//                store.toString().getBytes(),
//                new HashMap<>()
//        );
//    }
//
//    /* ============================================================
//       /queue
//       Part 1:
//         POST /queue   → create request
//         GET /queue/{id} → check request status
//       ============================================================ */
//
//    private HttpResponse handleQueue(String path) throws IOException {
//
//        // Case 1: POST /queue
//        if (path.equals("/queue") && req.method().equals("POST")) {
//            return handleQueuePost();
//        }
//
//        // Case 2: GET /queue/{id}
//        if (req.method().equals("GET")) {
//            return handleQueueGet(path);
//        }
//
//        return make500("Unsupported /queue operation");
//    }
//
//    /* ============================================================
//       POST /queue
//       ============================================================ */
//
//    private HttpResponse handleQueuePost() {
//
//        if (!"application/json".equals(req.headers().get("Accept")))
//            return make500("Accept must be application/json");
//
//        if (!"application/json".equals(req.headers().get("Content-Type")))
//            return make500("Content-Type must be application/json");
//
//        int ticketCount = parseTicketCount();
//        Concert concert = store.getConcert(); // the only concert (Part 1)
//
//        if (concert.getCount() <= 0) {
//            // No tickets left → return 200 with empty body
//            return new HttpResponse(
//                    HttpStatus.OK,
//                    ContentType.json,
//                    "{}".getBytes(),
//                    new HashMap<>()
//            );
//        }
//
//        // Submit queue request
//        MyRunnable runnable = new MyRunnable(queue, concert, ticketCount);
//        runnable.run();
//        int id = runnable.getId();
//
//        HashMap<String, String> headers = new HashMap<>();
//        headers.put("Location", "/queue/" + id);
//
//        return new HttpResponse(
//                HttpStatus.CREATED,
//                ContentType.json,
//                ("{\"id\": " + id + "}").getBytes(),
//                headers
//        );
//    }
//
//    /* ============================================================
//       GET /queue/{id}
//       ============================================================ */
//
//    private HttpResponse handleQueueGet(String path) {
//
//        if (!path.startsWith("/queue/"))
//            return make400("Missing queue id");
//
//        String idStr = path.substring("/queue/".length());
//
//        int id;
//        try { id = Integer.parseInt(idStr); }
//        catch (NumberFormatException e) { return make404(); }
//
//        // Query queue
//        int position = queue.getPosition(id);
//        Ticket[] tickets = store.getTicketsForRequest(id);
//
//        JsonObject json;
//
//        if (position == 0)
//        {
//            json = Json.createObjectBuilder()
//                    .add("id", id)
//                    .add("tickets", tickets.length)
//                    .add("position", 0)
//                    .add("ticketIds", Json.createArrayBuilder(
//                            tickets == null ? new String[]{} :
//                                    java.util.Arrays.stream(tickets).map(Ticket::getId).toArray(String[]::new)
//                    ))
//                    .build();
//        }
//        else if (position > 0)
//        {
//            json = Json.createObjectBuilder()
//                    .add("id", id)
//                    .add("tickets", queue.getTicketCount(id))
//                    .add("position", position)
//                    .add("ticketIds", Json.createArrayBuilder().build())
//                    .build();
//        }
//        else {
//            return make404(); // unknown id
//        }
//
//        return new HttpResponse(
//                HttpStatus.OK,
//                ContentType.json,
//                json.toString().getBytes(),
//                new HashMap<>()
//        );
//    }
//
//    /* ============================================================
//       Helpers
//       ============================================================ */
//
//    private int parseTicketCount() {
//        JsonReader r = Json.createReader(new StringReader(req.body()));
//        JsonObject obj = r.readObject();
//        return obj.getInt("tickets");
//    }
//
//    private HttpResponse make500(String message) {
//        return new HttpResponse(
//                HttpStatus.SERVER_ERROR,
//                ContentType.textPlain,
//                message.getBytes(),
//                new HashMap<>()
//        );
//    }
//
//    private HttpResponse make400(String message) {
//        return new HttpResponse(
//                HttpStatus.BAD_REQUEST,
//                ContentType.textPlain,
//                message.getBytes(),
//                new HashMap<>()
//        );
//    }
}
