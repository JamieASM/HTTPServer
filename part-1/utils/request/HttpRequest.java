package utils.request;

import java.util.Map;

/**
 * A representation of a HTTP request.
 * @param method The method for the request (i.e. GET, POST...).
 * @param path The path for the resources (i.e. 'index.html', '/tickets'...).
 * @param headers The map of headers for the request.
 * @param body The main body of the request.
 */
public record HttpRequest(String method, String path, Map<String, String> headers, String body) {
}
