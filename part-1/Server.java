import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.*;

public class Server {

    private static int port;
    private static String documentRoot; // TODO: is this needed?

    private static ServerSocket  server;
    private static Properties prop;

    /* create a regex to parse headers as per RFC9112 */
    private static final String headerRegex = "^(?<name>[\\w-]+):\\s*(?<value>.*)\\s*$";
    private static final int TIMEOUT = 30000;

    public static void main(String[] args) {
        // load in our properties
        loadProperties();

        // start the server
        port = Integer.parseInt(prop.getProperty("serverPort"));
        startServer();

        while (true) {
            try {
                // wait for connection
                Socket connection = server.accept();
                System.out.println("New connection: " + connection.getInetAddress());

                // serve the files provided in starter/public


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

    private static void loadProperties() {
        try {
            // parse the properties file
            // from https://www.baeldung.com/java-properties
            String rootPath = Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("")).getPath();
            String root = "CS2003-C3-ticket-chief";

            // remove anything that is after 'CS2003-C3-ticket-chief'
            int idx = rootPath.indexOf(root);

            if (idx != -1) {
                // keep up to the end of the folder name
                rootPath = rootPath.substring(0, idx + root.length());
            }

            String appConfigPath = rootPath + "/cs2003-C3.properties";

            prop = new Properties();
            prop.load(new FileInputStream(appConfigPath));
        } catch (IOException e) {
            System.out.println("Properties file not found");
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
            String[] parts = line.split(" ");

            String requestMethod = parts[0];
            String path = parts[1];

            if ("GET".equalsIgnoreCase(requestMethod) && "/".equals(path)) {
                writeResponse(outputStream);
            }

            socket.close();
        } catch (IOException e) {
            System.out.println("IO Exception: " + e.getMessage());
        }
    }

    private static void writeResponse(OutputStream outputStream) throws IOException {
        String message = "Test";
        String httpResponse = """
                HTTP/1.1 200 OK
                Content-Type: text/plain
                Content-Length:%d
                %s
                """.formatted(message.length(), message);

        outputStream.write(httpResponse.getBytes());
        outputStream.flush();
        outputStream.close();
    }
}
