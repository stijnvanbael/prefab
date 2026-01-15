package be.appify.prefab.core.problem;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@ControllerAdvice
class ExceptionHandling {
    @ResponseStatus(value = UNAUTHORIZED)
    @ExceptionHandler(InsufficientAuthenticationException.class)
    void handleInsufficientAuthentication() {
        // Nothing to do
    }

    @ResponseStatus(value = FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    void handleAccessDenied() {
        // Nothing to do
    }
}
