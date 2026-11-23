package be.appify.prefab.core.spring;

import io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiErrorResponse;
import io.github.wimdeblauwe.errorhandlingspringbootstarter.ApiExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * Reports a missing or unreadable part of a {@code multipart/form-data} request as {@code 400 Bad Request}.
 * Without it, the error handling starter treats these exceptions as unexpected and answers {@code 500}.
 * An upload that exceeds the servlet container's size limit keeps its own status (413).
 */
@Component
class MultipartExceptionHandler implements ApiExceptionHandler {
    private static final String CODE = "INVALID_MULTIPART_REQUEST";

    @Override
    public boolean canHandle(Throwable exception) {
        return exception instanceof MissingServletRequestPartException
                || (exception instanceof MultipartException && !(exception instanceof MaxUploadSizeExceededException));
    }

    @Override
    public ApiErrorResponse handle(Throwable exception) {
        return new ApiErrorResponse(HttpStatus.BAD_REQUEST, CODE, messageOf(exception));
    }

    private static String messageOf(Throwable exception) {
        return switch (exception) {
            case MissingServletRequestPartException missing ->
                    "Required part '%s' is missing from the multipart request".formatted(missing.getRequestPartName());
            default -> "The multipart request could not be read: " + exception.getMessage();
        };
    }
}
