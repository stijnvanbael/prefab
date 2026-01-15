package be.appify.prefab.core.problem;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Exception to signal a bad request by the user (HTTP 400) */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {
    /**
     * Create a new BadRequestException with a detail message
     * @param detail the detail message
     */
    public BadRequestException(String detail) {
        super(detail);
    }
}
