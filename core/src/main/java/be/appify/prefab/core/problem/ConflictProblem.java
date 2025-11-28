package be.appify.prefab.core.problem;

import org.zalando.problem.AbstractThrowableProblem;

import java.net.URI;

import static org.zalando.problem.Status.CONFLICT;

public class ConflictProblem extends AbstractThrowableProblem {
    private static final URI TYPE = URI.create("https://prefab.appify.be/problems/conflict");

    public ConflictProblem(String detail) {
        super(TYPE, "Conflict", CONFLICT, detail);
    }
}
