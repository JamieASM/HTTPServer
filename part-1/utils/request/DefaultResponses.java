package utils.request;

import utils.HttpResponse;
import utils.request.enums.ContentType;
import utils.request.enums.HttpStatus;

import java.util.HashMap;

public class DefaultResponses {
    /**
     * Creates a 404 error object.
     * @return A HttpResponse object.
     */
    public HttpResponse make404() {
        return new HttpResponse(
                HttpStatus.NOT_FOUND,
                ContentType.textPlain,
                "404 File Not Found".getBytes(),
                new HashMap<>()
        );
    }

    /**
     * Creates a 500 error object.
     * @param message The reasoning for the error.
     * @return A HttpResponse object.
     */
    public HttpResponse make500(String message) {
        return new HttpResponse(
                HttpStatus.SERVER_ERROR,
                ContentType.textPlain,
                message.getBytes(),
                new HashMap<>()
        );
    }

    /**
     * Creates a 400 error object.
     * @param message The reasoning for the error.
     * @return A HttpResponse object.
     */
    public HttpResponse make400(String message) {
        return new HttpResponse(
                HttpStatus.BAD_REQUEST,
                ContentType.textPlain,
                message.getBytes(),
                new HashMap<>()
        );
    }

    /**
     * Creates a 200 object.
     * @param message The associated message for the response.
     * @return A HttpResponse object.
     */
    public HttpResponse make200(String message) {
        return new HttpResponse(
                HttpStatus.OK,
                ContentType.textPlain,
                message.getBytes(),
                new HashMap<>()
        );
    }
}
