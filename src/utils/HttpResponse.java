package utils;

import utils.request.enums.ContentType;
import utils.request.enums.HttpStatus;

import java.io.IOException;
import java.io.OutputStream;

import java.util.Map;

/**
 * Represents Http responses as an java object.
 * @param status The status of a response (i.e. OK, CREATED...).
 * @param contentType The type of content the response contains (i.e. text/html, text/css...).
 * @param bytes The content to be sent to the client in the form of a byte array.
 * @param headers The headers of the response.
 */
public record HttpResponse(HttpStatus status, ContentType contentType, byte[] bytes, Map<String, String> headers) {

    /**
     * Builds and send a HTTP response to the client.
     * @param outputStream The output stream to write the response.
     * @throws IOException Thrown if there is an error with the output stream.
     */
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
