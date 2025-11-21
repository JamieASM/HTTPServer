import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class HttpRequestHandler {
    private final BufferedReader reader;
    private final OutputStream outputStream;

    private final String documentRoot;

    public HttpRequestHandler(BufferedReader reader, OutputStream outputStream, String documentRoot) {
        this.reader = reader;
        this.outputStream = outputStream;
        this.documentRoot = documentRoot;
    }

    protected void handleRequest() {
        try {
            // Read the request
            String request = reader.readLine();
            if (request == null || request.isEmpty()) { return; }

            // for debugging
            System.out.println(request);

            // get the file path
            String[] parts = request.split(" ");
            String filePath = parts[1];

            if (filePath.equals("/")) {
                filePath = documentRoot + "/index.html";
            } // we want to be able to take '/{something}', check that there exist a file with that name, and return it.
            else {
                filePath = documentRoot + "/" + filePath;
                File file = new File(filePath);
                if (!file.exists()) {
                    send404(outputStream);
                    return;
                }
            }

            serveFile(Path.of(filePath), outputStream);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void serveFile(Path path, OutputStream outputStream) throws IOException {
        try {
            byte[] fileContent = Files.readAllBytes(path);
            String fileType = getFileType(path.getFileName().toString());

            outputStream.write(("HTTP/1.1 200 OK\r\n" +
                    "Content-Type: " + fileType + "\r\n" +
                    "Content-Length: " + fileContent.length + "\r\n" +
                    "\r\n").getBytes());

            outputStream.write(fileContent);
            outputStream.flush();
        }
        catch (IOException e) {
            System.out.printf("Could not serve file '%s': %s%n", path.getFileName(), e.getMessage());
            send404(outputStream);
        }
    }

    private String getFileType(String fileName) {
        if (fileName.endsWith(".html")) {
            return "text/html";
        }
        else if (fileName.endsWith(".css")) {
            return "text/css";
        }
        else if (fileName.endsWith(".js")) {
            return "application/javascript";
        } else { // shouldn't get to this point
            return "text/plain";
        }
    }

    private void send404(OutputStream outputStream) throws IOException {
        String response = "HTTP/1.1 404 Not Found\r\n\r\n404 File Not Found";
        outputStream.write(response.getBytes());
        outputStream.flush();
    }
}
