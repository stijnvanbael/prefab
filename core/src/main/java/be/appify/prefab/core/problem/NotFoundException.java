package be.appify.prefab.core.problem;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception indicating that a requested resource was not found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException {
    private static final URI TYPE = URI.create("https://prefab.appify.be/problems/not-found");

    /**
     * Constructs a new NotFoundException with the specified detail message.
     * @param detail The detail message providing additional context about the resource not found.
     */
    public NotFoundException(String detail) {
        super("[%s] was not found".formatted(detail));
    }
}
