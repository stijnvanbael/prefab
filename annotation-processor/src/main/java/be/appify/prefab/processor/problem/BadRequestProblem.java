package be.appify.prefab.processor.problem;

import org.zalando.problem.AbstractThrowableProblem;

import java.net.URI;

import static org.zalando.problem.Status.BAD_REQUEST;

public class BadRequestProblem extends AbstractThrowableProblem {
    private static final URI TYPE = URI.create("https://prefab.appify.be/problems/bad-request");

    public BadRequestProblem(String detail) {
        super(TYPE, "Bad request", BAD_REQUEST, detail);
    }
}
