package be.appify.prefab.streams.kafka;

import java.util.concurrent.atomic.AtomicInteger;

public final class StreamStepNames {
    private final AtomicInteger branchSequence = new AtomicInteger();
    private final AtomicInteger branchSubtypeSequence = new AtomicInteger();
    private final AtomicInteger filterSequence = new AtomicInteger();
    private final AtomicInteger flatMapSequence = new AtomicInteger();
    private final AtomicInteger joinSequence = new AtomicInteger();
    private final AtomicInteger mapSequence = new AtomicInteger();
    private final AtomicInteger mergeSequence = new AtomicInteger();
    private final AtomicInteger processSequence = new AtomicInteger();

    String nextBranchName() {
        return "branch-%d".formatted(branchSequence.incrementAndGet());
    }

    String nextBranchSubtypeName() {
        return "branch-subtype-%d".formatted(branchSubtypeSequence.incrementAndGet());
    }

    String nextFilterName() {
        return "filter-%d".formatted(filterSequence.incrementAndGet());
    }

    String nextFlatMapName() {
        return "flat-map-%d".formatted(flatMapSequence.incrementAndGet());
    }

    String nextJoinName() {
        return "join-%d".formatted(joinSequence.incrementAndGet());
    }

    String nextMapName() {
        return "map-%d".formatted(mapSequence.incrementAndGet());
    }

    String nextMergeName() {
        return "merge-%d".formatted(mergeSequence.incrementAndGet());
    }

    String nextProcessName() {
        return "process-%d".formatted(processSequence.incrementAndGet());
    }
}
