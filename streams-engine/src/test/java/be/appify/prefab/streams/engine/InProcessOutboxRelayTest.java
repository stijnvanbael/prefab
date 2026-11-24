package be.appify.prefab.streams.engine;

import be.appify.prefab.core.domain.Keyed;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InProcessOutboxRelayTest {
    record Input(String key) implements Keyed<String> {}
    record Trigger(String key) implements Keyed<String> {}
    record Output(String key) implements Keyed<String> {}

    @Test
    void relaysAnOutputCommittedAfterAnotherRelayScannedTheTopic() throws Exception {
        var source = new InMemoryStreamSource();
        var sink = new InMemoryStreamSink();
        var repository = new PausingRepository();
        var streams = new InProcessPrefabStreams(source, sink, repository,
                new InMemoryTopologyTestHarness.InMemoryTopicResolver("engine"));
        streams.from(Trigger.class).end();
        streams.from(Input.class).map(input -> new Output(input.key())).to(Output.class);

        try (var topology = streams.start(); var executor = Executors.newFixedThreadPool(2)) {
            repository.pauseNextEmptyScan.set(true);
            var first = executor.submit(() -> source.deliver("engine-trigger",
                    new StreamMessage("trigger", "key", new Trigger("key"), Map.of(), Instant.EPOCH)));
            try {
                assertThat(repository.scanned.await(5, TimeUnit.SECONDS)).isTrue();
                var second = executor.submit(() -> source.deliver("engine-input",
                        new StreamMessage("input", "key", new Input("key"), Map.of(), Instant.EPOCH)));
                second.get(5, TimeUnit.SECONDS);
            } finally {
                repository.resume.countDown();
            }
            first.get(5, TimeUnit.SECONDS);

            assertThat(sink.drain("engine-output")).singleElement()
                    .extracting(StreamMessage::value).isEqualTo(new Output("key"));
        }
    }

    @Test
    void relaysOutputsProducedBySynchronousDownstreamDelivery() {
        var source = new InMemoryStreamSource();
        var sink = new InMemoryStreamSink(source);
        var streams = new InProcessPrefabStreams(source, sink, new InMemoryStreamStateRepository(),
                new InMemoryTopologyTestHarness.InMemoryTopicResolver("engine"));
        streams.from(Input.class).map(input -> new Output(input.key())).to(Output.class);
        streams.from(Output.class).map(output -> new Input(output.key())).to("final-output");

        try (var topology = streams.start()) {
            source.deliver("engine-input",
                    new StreamMessage("input", "key", new Input("key"), Map.of(), Instant.EPOCH));

            assertThat(sink.drain("final-output")).singleElement()
                    .extracting(StreamMessage::value).isEqualTo(new Input("key"));
        }
    }

    private static final class PausingRepository implements StreamStateRepository {
        private final InMemoryStreamStateRepository delegate = new InMemoryStreamStateRepository();
        private final AtomicBoolean pauseNextEmptyScan = new AtomicBoolean();
        private final CountDownLatch scanned = new CountDownLatch(1);
        private final CountDownLatch resume = new CountDownLatch(1);

        @Override
        public Transaction beginTransaction() {
            return delegate.beginTransaction();
        }

        @Override
        public void heartbeatMember(String groupId, String memberId, String topologyId, Instant now, Duration lease) {
            delegate.heartbeatMember(groupId, memberId, topologyId, now, lease);
        }

        @Override
        public List<String> activeMembers(String groupId, Instant now) {
            return delegate.activeMembers(groupId, now);
        }

        @Override
        public void removeMember(String groupId, String memberId) {
            delegate.removeMember(groupId, memberId);
        }

        @Override
        public void registerScheduledTask(String groupId, String scheduleId, Duration interval, Instant now) {
            delegate.registerScheduledTask(groupId, scheduleId, interval, now);
        }

        @Override
        public java.util.Optional<ScheduledTaskLease> claimScheduledTask(
                String groupId, String scheduleId, String owner, Instant now, Duration lease
        ) {
            return delegate.claimScheduledTask(groupId, scheduleId, owner, now, lease);
        }

        @Override
        public boolean renewScheduledTask(ScheduledTaskLease lease, Instant now, Duration duration) {
            return delegate.renewScheduledTask(lease, now, duration);
        }

        @Override
        public void cleanupExpired(Instant now, Duration retention) {
            delegate.cleanupExpired(now, retention);
        }

        @Override
        public List<OutboxRecord> leaseOutbox(String topic, String owner, Instant now, Duration lease, int limit) {
            var records = delegate.leaseOutbox(topic, owner, now, lease, limit);
            if (records.isEmpty() && pauseNextEmptyScan.compareAndSet(true, false)) {
                scanned.countDown();
                try {
                    if (!resume.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to resume the outbox relay");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while pausing the outbox relay", e);
                }
            }
            return records;
        }

        @Override
        public void completeOutbox(String owner, String recordId) {
            delegate.completeOutbox(owner, recordId);
        }
    }
}
