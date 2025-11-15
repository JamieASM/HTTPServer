import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;

public record HttpResponse(String status, int code, String contentType, byte[] content) {
    private static final String contentTypeHtml = "text/html; charset=utf-8";
    private static final String root = Objects.requireNonNull(PropertiesParser.loadProperties()).getProperty("documentRoot");

    public static HttpResponse from(List<String> parts) {
        if (parts.size() < 2) {
            return new HttpResponse("Bad Request", 400, contentTypeHtml, new byte[]{});
        } else {
            String requestMethod = parts.get(0);
            String requestURI = parts.get(1);

            if (requestMethod.equalsIgnoreCase("GET")) {
                return processPath(requestURI);
            }
            else {
               return new HttpResponse("Bad Request", 400, contentTypeHtml, new byte[]{});
            }
        }
    }

    private static HttpResponse processPath(String requestURI) {
        File file;
        File tempFile = new File(root + requestURI);

        if (tempFile.isDirectory()) {
            file = new File(root + "index.html");
        } else {
            file = tempFile;
        }
        try {
            File canonicalFile = file.getCanonicalFile();

            if (canonicalFile.canRead() && canonicalFile.getPath().startsWith(root))  {
                byte[] content = Files.readAllBytes(canonicalFile.toPath());
                String contentType = Files.probeContentType(canonicalFile.toPath());

                return new HttpResponse("OK", 200, contentTypeHtml, content);
            } else {
                return new HttpResponse("Bad Request", 400, contentTypeHtml, new byte[]{});
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
}
