package be.appify.prefab.core.problem;

import org.zalando.problem.AbstractThrowableProblem;

import java.net.URI;

import static org.zalando.problem.Status.CONFLICT;

/** Problem to signal a conflict in the request (HTTP 409) */
public class ConflictProblem extends AbstractThrowableProblem {
    private static final URI TYPE = URI.create("https://prefab.appify.be/problems/conflict");

    /**
     * Create a new ConflictProblem with a detail message
     * @param detail the detail message
     */
    public ConflictProblem(String detail) {
        super(TYPE, "Conflict", CONFLICT, detail);
    }
}
