package utils;

import java.io.IOException;
import java.io.OutputStream;

public record HttpResponse(HttpStatus status, ContentType contentType, byte[] bytes) {

    public void sendResponse(OutputStream outputStream) throws IOException {
        outputStream.write(("""
                HTTP/1.1 %d %s\r
                Content-Type: %s\r
                Content-Length: %d\r
                \r
                """)
                .formatted(
                        status.getStatusCode(),
                        status.getStatusMessage(),
                        contentType.getContentType(),
                        bytes.length
                ).getBytes());

        outputStream.write(bytes);
        outputStream.flush();
    }
}
