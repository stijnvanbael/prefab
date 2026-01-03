package be.appify.prefab.core.problem;

import org.zalando.problem.AbstractThrowableProblem;

import java.net.URI;

import static org.zalando.problem.Status.BAD_REQUEST;

/** Problem to signal a bad request by the user (HTTP 400) */
public class BadRequestProblem extends AbstractThrowableProblem {
    private static final URI TYPE = URI.create("https://prefab.appify.be/problems/bad-request");

    /**
     * Create a new BadRequestProblem with a detail message
     * @param detail the detail message
     */
    public BadRequestProblem(String detail) {
        super(TYPE, "Bad request", BAD_REQUEST, detail);
    }
}
