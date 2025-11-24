package utils;

import utils.enums.ContentType;
import utils.enums.HttpStatus;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public record HttpResponse(HttpStatus status, ContentType contentType, byte[] bytes, Map<String, String> headers) {

    public void sendResponse(OutputStream outputStream) throws IOException {
        outputStream.write("HTTP/1.1 %d %s\n\n".formatted(status.getStatusCode(), status.getStatusMessage()).getBytes());
        outputStream.write(getHeadersString().getBytes());

        outputStream.write(("""
                Content-Type: %s\r
                Content-Length: %d\r
                \r
                """)
                .formatted(
                        contentType.getContentType(),
                        bytes.length
                ).getBytes());

        outputStream.write(bytes);
        outputStream.flush();
    }

    private String getHeadersString() {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, String> header : headers.entrySet()) {
            String key = header.getKey();
            String value = header.getValue();

            sb.append(String.format("%s: %s\r\n", key, value));
        }

        return sb.toString();
    }
}
