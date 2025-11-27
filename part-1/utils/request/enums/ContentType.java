package utils.request.enums;

/**
 * Enum class representing the type of content a HTTP response contains.
 */
public enum ContentType {
    html("text/html"),
    css("text/css"),
    javascript("text/javascript"),
    json("application/json"),
    textPlain("text/plain");

    private final String contentType;

    /**
     * Constructor for the ContentType class.
     * @param contentType The type of content, as represented by a string.
     */
    ContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Retrieves the content type.
     * @return The content type.
     */
    public String getContentType() {
        return contentType;
    }
}
