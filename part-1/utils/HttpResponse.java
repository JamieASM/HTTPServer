package utils;

import utils.enums.ContentType;
import utils.enums.HttpStatus;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public record HttpResponse(HttpStatus status, ContentType contentType, byte[] bytes, Map<String, String> headers) {

    public void sendResponse(OutputStream outputStream) throws IOException {
        StringBuilder builder = new StringBuilder();

        // Get the first line
        builder.append("HTTP/1.1 ")
                .append(status.getStatusCode()).append(" ")
                .append(status.getStatusMessage()).append("\r\n");

        // Append the always required headers
        builder.append("Content-Type: ").append(contentType.getContentType()).append("\r\n");
        builder.append("Content-Length: ").append(bytes.length).append("\r\n");

        // Add any extra headers
        for (Map.Entry<String, String> h : headers.entrySet()) {
            builder.append(h.getKey()).append(": ").append(h.getValue()).append("\r\n");
        }

        // finish the headers
        builder.append("\r\n");

        // Send the info
        outputStream.write(builder.toString().getBytes());
        outputStream.write(bytes);
        outputStream.flush();
    }
}
