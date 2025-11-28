package be.appify.prefab.core.problem;

import org.zalando.problem.AbstractThrowableProblem;

import java.net.URI;

import static org.zalando.problem.Status.BAD_REQUEST;

public class RequiredProblem extends AbstractThrowableProblem {
    private static final URI TYPE = URI.create("https://prefab.appify.be/problems/required");

    public RequiredProblem(String detail) {
        super(TYPE, "Required", BAD_REQUEST, "[%s] is required".formatted(detail));
    }
}
