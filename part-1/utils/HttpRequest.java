package utils;

import java.util.Map;

public record HttpRequest(String method, String path, Map<String, String> headers, String body) {
}
