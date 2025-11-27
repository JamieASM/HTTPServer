package utils.request.enums;

/**
 * Enum class to represent HTTP status types and their associated messages.
 */
public enum HttpStatus {
    OK(200, "OK"),
    SERVER_ERROR(500, "Internal Server Error"),
    CREATED(201, "Created"),
    BAD_REQUEST(400, "Bad Request"),
    NOT_FOUND(404, "Not Found");

    private final int statusCode;
    private final String statusMessage;

    /**
     * Constructor for HttpStatus
     * @param statusCode The status code.
     * @param statusMessage The associated message.
     */
    HttpStatus(int statusCode, String statusMessage) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
    }

    /**
     * Retrieves the status code.
     * @return The status code.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Retrieves the status message.
     * @return The status message.
     */
    public String getStatusMessage() {
        return statusMessage;
    }
}
