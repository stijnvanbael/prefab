package be.appify.prefab.core.problem;

import org.zalando.problem.AbstractThrowableProblem;

import java.net.URI;

import static org.zalando.problem.Status.BAD_REQUEST;

/**
 * Problem indicating that a required field is missing.
 */
public class RequiredProblem extends AbstractThrowableProblem {
    private static final URI TYPE = URI.create("https://prefab.appify.be/problems/required");

    /**
     * Constructs a new RequiredProblem with the specified detail message.
     *
     * @param detail The detail message providing additional context about the required field.
     */
    public RequiredProblem(String detail) {
        super(TYPE, "Required", BAD_REQUEST, "[%s] is required".formatted(detail));
    }
}
