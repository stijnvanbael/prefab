package be.appify.prefab.core.problem;

import org.zalando.problem.AbstractThrowableProblem;

import java.net.URI;

import static org.zalando.problem.Status.NOT_FOUND;

public class NotFoundProblem extends AbstractThrowableProblem {
    private static final URI TYPE = URI.create("https://prefab.appify.be/problems/not-found");

    public NotFoundProblem(String detail) {
        super(TYPE, "Not found", NOT_FOUND, "[%s] was not found".formatted(detail));
    }
}
