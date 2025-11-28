package be.appify.prefab.core.problem;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.zalando.problem.spring.web.advice.ProblemHandling;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@ControllerAdvice
public class ExceptionHandling implements ProblemHandling {
    @ResponseStatus(value = UNAUTHORIZED)
    @ExceptionHandler(InsufficientAuthenticationException.class)
    public void handleInsufficientAuthentication() {
        // Nothing to do
    }

    @ResponseStatus(value = FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public void handleAccessDenied() {
        // Nothing to do
    }
}
