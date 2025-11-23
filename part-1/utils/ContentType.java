package utils;

public enum ContentType {
    html("text/html"),
    css("text/css"),
    javascript("text/javascript"),
    json("application/json"),
    textPlain("text/plain");

    private final String contentType;

    ContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }
}
