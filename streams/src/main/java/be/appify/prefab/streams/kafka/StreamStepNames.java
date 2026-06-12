package be.appify.prefab.streams.kafka;

import java.util.concurrent.atomic.AtomicInteger;

public final class StreamStepNames {
    private final AtomicInteger branchSequence = new AtomicInteger();

    String nextBranchName() {
        return "branch-%d".formatted(branchSequence.incrementAndGet());
    }
}


