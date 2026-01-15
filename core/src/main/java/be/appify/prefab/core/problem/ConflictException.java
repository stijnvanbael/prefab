package be.appify.prefab.core.problem;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Exception to signal a conflict in the request (HTTP 409) */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {
    /**
     * Create a new ConflictException with a detail message
     *
     * @param detail
     *         the detail message
     */
    public ConflictException(String detail) {
        super(detail);
    }
}
