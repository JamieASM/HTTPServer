import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Properties;

// used https://www.youtube.com/watch?v=5wQWJAvMDGg
public class HttpServer {

    private static int port;
    private static String documentRoot; // TODO: is this needed?

    private static ServerSocket  server;
    private static Properties prop;

    /* create a regex to parse headers as per RFC9112 */
    private static final String headerRegex = "^(?<name>[\\w-]+):\\s*(?<value>.*)\\s*$";
    private static final int TIMEOUT = 30000;

    public static void main(String[] args) {
        // load in our properties
        Properties prop = PropertiesParser.loadProperties();

        // start the server
        assert prop != null;
        port = Integer.parseInt(prop.getProperty("serverPort"));
        startServer();

        while (true) {
            try {
                // wait for connection
                Socket connection = server.accept();
                System.out.println("New connection: " + connection.getInetAddress());

                // serve the files provided in starter/public
                handleRequest(connection);

//                /* input and output */
//                PrintWriter tx = new PrintWriter(connection.getOutputStream(), true);
//                BufferedReader rx = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//
//                /* process the HTTP request */
//
//                /* first line is the start-line (see RFC7230 sec 3) */
//                String startLine = rx.readLine();
//                System.out.println("HTTP start-line: " + startLine);
//
//                /* next come the headers */
//
//                Pattern p = Pattern.compile(headerRegex);
//
//                /* loop over headers and put them into a HashMap */
//
//                String input;
//                HashMap<String, String> headers = new HashMap<>();
//                while ((input = rx.readLine()) != null && !input.isEmpty()) {
//                    Matcher m = p.matcher(input);
//                    boolean found = m.find();
//                    if (found) {
//                        headers.put(m.group("name"),m.group("value"));
//                    }
//                }
//
//                System.out.println("HTTP headers: " + headers);
//
//                /* next is the body but we will ignore it for simplicity */
//
//                /* return an inappropriate (?) HTTP status */
//                /* note that HTTP uses CRLF line breaks, encoded as \r\n */
//
//                String response = "HTTP/1.1 418 I'm a teapot\r\n\r\n\r\n";
//
//                tx.println(response);

                connection.close();
            }

            // keep the server running; use Ctrl-C to quit the while loop

            catch (IOException e) {
                System.err.println("IO Exception: " + e.getMessage());
            }
        }
    }



    private static void startServer() {
        try {
            server = new ServerSocket(port);
            System.out.println("Starting server: " + server);
        } catch (IOException e) {
            System.err.println("IO Exception: " + e.getMessage());
        }
    }

    private static void handleRequest(Socket socket) {
        try(InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream()){

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line = reader.readLine();
            List<String>  parts = List.of(line.split(" "));

            String requestMethod = parts.get(0);
            String path = parts.get(1);

            if ("GET".equalsIgnoreCase(requestMethod) && "/index.html".equals(path)) {
                HttpResponse response = HttpResponse.from(parts);

                writeResponse(outputStream, response);
            }

            socket.close();
        } catch (IOException e) {
            System.out.println("IO Exception: " + e.getMessage());
        }
    }

    private static void writeResponse(OutputStream outputStream, HttpResponse response) throws IOException {
        String httpHeader = """
                HTTP/1.1 %d %s
                Content-Type: %s
                Content-Length:%d
                """
                .formatted(response.code(), response.status(), response.contentType(), response.content().length);

        outputStream.write(httpHeader.getBytes());
        outputStream.write(response.content());
        outputStream.flush();
        outputStream.close();
    }
}
