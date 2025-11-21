import java.io.*;
import java.net.*;
import java.util.Properties;

// used https://www.youtube.com/watch?v=5wQWJAvMDGg
public class HttpServer {

    private static int port;

    private static ServerSocket  server;
    private static Properties prop;

    /* create a regex to parse headers as per RFC9112 */
    private static final String headerRegex = "^(?<name>[\\w-]+):\\s*(?<value>.*)\\s*$";
    private static final int TIMEOUT = 30000;

    public static void main(String[] args) {
        // load in our properties
        Properties prop = PropertiesParser.loadProperties();
        assert prop != null;

        // start the server
        port = Integer.parseInt(prop.getProperty("serverPort"));
        startServer();

        while (true) {
            try (
                    Socket connection = server.accept();
                    InputStream inputStream = connection.getInputStream();
                    OutputStream outputStream = connection.getOutputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            ) {
                // wait for connection
                System.out.println("New connection: " + connection.getInetAddress());
                HttpRequestHandler requestHandler = new HttpRequestHandler(reader, outputStream, prop.getProperty("documentRoot"));

                requestHandler.handleRequest();
            }
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
}
