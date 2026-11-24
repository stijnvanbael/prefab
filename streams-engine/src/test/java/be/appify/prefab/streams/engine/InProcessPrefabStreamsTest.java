package be.appify.prefab.streams.engine;

import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.streams.AggregationWindow;
import be.appify.prefab.streams.ContextualStreamProcessor;
import be.appify.prefab.streams.GlobalStore;
import be.appify.prefab.streams.JoinWindow;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.StatefulStreamProcessor;
import be.appify.prefab.streams.StreamBackend;
import be.appify.prefab.streams.StreamProcessor;
import be.appify.prefab.streams.StreamProcessorContext;
import be.appify.prefab.streams.StreamRecord;
import be.appify.prefab.streams.testing.TopologySession;
import be.appify.prefab.streams.testing.TopologyTestHarness;
import be.appify.prefab.streams.tracing.TracingConfig;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InProcessPrefabStreamsTest {
    @Test
    void serviceLoadedHarnessRunsStatelessOperatorsAndMerges() {
        var harness = TopologyTestHarness.create("engine");
        var streams = harness.streams();
        var left = streams.from(Word.class)
                .filter(word -> word.text().startsWith("a"))
                .map(word -> new Word(word.id(), word.text().toUpperCase()))
                .flatMap(word -> List.of(word, new Word(word.id(), word.text() + "!")));
        var right = streams.from(Word.class).branch(word -> word.text().startsWith("b"));
        streams.merge(left, right).repartition("explicit-route").to(Word.class);

        try (var topology = harness.start()) {
            topology.input(Word.class).send(new Word("1", "apple"));
            topology.input(Word.class).send(new Word("2", "berry"));
            topology.input(Word.class).send(new Word("3", "cherry"));

            assertThat(topology.output(Word.class).values())
                    .containsExactly(new Word("1", "APPLE"), new Word("1", "APPLE!"), new Word("2", "berry"));
        }
    }

    @Test
    void subtypeBranchAndKeyByUseLogicalInMemoryRouting() {
        var harness = new InMemoryTopologyTestHarness("engine");
        var streams = harness.streams();
        streams.from(Kinded.class)
                .branch(Kinded.Created.class)
                .keyBy(String.class, Kinded.Created::asWord)
                .repartition()
                .to(Word.class);

        try (var topology = harness.start()) {
            topology.input(Kinded.class).send(new Kinded.Created("1", "hi"));
            topology.input(Kinded.class).send(new Kinded.Deleted("2"));

            assertThat(topology.output(Word.class).values()).containsExactly(new Word("1", "hi"));
        }
    }

    @Test
    void groupAggregateAndWindowedAggregateMaintainPerKeyState() {
        var harness = new InMemoryTopologyTestHarness("engine");
        var streams = harness.streams();
        streams.from(Word.class)
                .groupBy(String.class, Word::id)
                .aggregate(Count.class, () -> new Count(null, 0),
                        (word, count) -> new Count(word.id(), count.count() + 1))
                .to(Count.class);
        streams.from(Word.class)
                .groupBy(String.class, Word::id)
                .windowedAggregate(
                        AggregationWindow.tumbling(Duration.ofSeconds(10), Duration.ofSeconds(2)),
                        WindowCount.class,
                        () -> new WindowCount(null, 0),
                        (word, count) -> new WindowCount(word.id(), count.count() + 1))
                .to(WindowCount.class);

        try (var topology = harness.start()) {
            var input = topology.input(Word.class);
            input.send(new Word("1", "a"), Instant.ofEpochSecond(1));
            input.send(new Word("1", "b"), Instant.ofEpochSecond(2));
            input.send(new Word("1", "c"), Instant.ofEpochSecond(12));

            assertThat(topology.output(Count.class).values())
                    .containsExactly(new Count("1", 1), new Count("1", 2), new Count("1", 3));
            assertThat(topology.output(WindowCount.class).values())
                    .containsExactly(new WindowCount("1", 1), new WindowCount("1", 2), new WindowCount("1", 1));
        }
    }

    @Test
    void listAggregationAndDeduplicationRetainTheirDslSemantics() {
        var harness = new InMemoryTopologyTestHarness("engine");
        var streams = harness.streams();
        streams.from(Word.class)
                .aggregate(Word::id, values -> new Count(values.getFirst().id(), values.size()), count -> true)
                .to(Count.class);
        streams.from(Word.class)
                .deduplicate(Word::text, Duration.ofSeconds(10))
                .to(Word.class);

        try (var topology = harness.start()) {
            var input = topology.input(Word.class);
            input.sendWithMessageId(new Word("1", "same"), "a", Instant.ofEpochSecond(1));
            input.sendWithMessageId(new Word("1", "same"), "b", Instant.ofEpochSecond(5));
            input.sendWithMessageId(new Word("1", "same"), "c", Instant.ofEpochSecond(12));

            assertThat(topology.output(Count.class).values())
                    .containsExactly(new Count("1", 1), new Count("1", 2), new Count("1", 3));
            assertThat(topology.output(Word.class).values())
                    .containsExactly(new Word("1", "same"), new Word("1", "same"));
        }
    }

    @Test
    void joinMatchesByKeyAndEventTime() {
        var harness = new InMemoryTopologyTestHarness("engine");
        harness.streams().from(Word.class)
                .join(harness.streams().from(Status.class), JoinWindow.of(Duration.ofSeconds(5), Duration.ZERO),
                        (word, status) -> new Joined(word.id(), word.text(), status.value()))
                .to(Joined.class);

        try (var topology = harness.start()) {
            topology.input(Word.class).send(new Word("1", "hello"), Instant.ofEpochSecond(2));
            topology.input(Status.class).send(new Status("1", "ready"), Instant.ofEpochSecond(5));

            assertThat(topology.output(Joined.class).values())
                    .containsExactly(new Joined("1", "hello", "ready"));
        }
    }

    @Test
    void duplicateInputMessageDoesNotProduceOutputTwice() {
        var harness = new InMemoryTopologyTestHarness("engine");
        harness.streams().from(Word.class).map(word -> new Word(word.id(), word.text())).to(Word.class);
        try (var topology = harness.start()) {
            var input = topology.input(Word.class);
            input.sendWithMessageId(new Word("1", "once"), "same-message");
            input.sendWithMessageId(new Word("1", "once"), "same-message");

            assertThat(topology.output(Word.class).values()).containsExactly(new Word("1", "once"));
        }
    }

    @Test
    void processorsReceiveContextOnTheBrokerDeliveryThread() {
        var harness = new InMemoryTopologyTestHarness("engine");
        var processor = new CountingProcessor();
        harness.streams().from(Word.class).process(processor).to(Count.class);
        try (var topology = harness.start()) {
            var input = topology.input(Word.class);

            CompletableFuture.runAsync(() -> input.send(new Word("1", "one"))).join();

            assertThat(topology.output(Count.class).values()).containsExactly(new Count("1", 1));
            assertThat(processor.initializations).hasValue(1);
        }
    }

    @Test
    void distinctSourceNodesHaveIndependentSubscriptionsAndInboxIdentities() {
        var delivery = new InMemoryStreamSource();
        var bindings = new ArrayList<StreamSource.SourceBinding>();
        var source = new StreamSource() {
            @Override
            public Subscription subscribe(String topic, MessageHandler handler) {
                return delivery.subscribe(topic, handler);
            }

            @Override
            public Subscription subscribe(SourceBinding binding, MessageHandler handler) {
                bindings.add(binding);
                return delivery.subscribe(binding.topic(), handler);
            }
        };
        var sink = new InMemoryStreamSink(delivery);
        var engine = new InProcessPrefabStreams(source, sink, new InMemoryStreamStateRepository(),
                new InMemoryTopologyTestHarness.InMemoryTopicResolver("engine"));
        var shared = engine.from(Word.class);
        shared.filter(word -> true).to("first-output");
        shared.filter(word -> true).to("second-output");
        engine.from(Word.class).to("third-output");
        try (var topology = engine.start()) {
            var input = new StreamMessage("id", "1", new Word("1", "one"), Map.of(), Instant.EPOCH);

            delivery.deliver("engine-word", input);
            delivery.deliver("engine-word", input);

            assertThat(bindings).hasSize(2).extracting(StreamSource.SourceBinding::name).doesNotHaveDuplicates();
            assertThat(sink.drain("first-output")).hasSize(1);
            assertThat(sink.drain("second-output")).hasSize(1);
            assertThat(sink.drain("third-output")).hasSize(1);
        }
    }

    @Test
    void failedOutboxPublishIsRetriedWithoutReprocessingInput() {
        var source = new InMemoryStreamSource();
        var publishAttempts = new AtomicInteger();
        var published = new ArrayList<StreamMessage>();
        var topics = new StreamTopicResolver() {
            @Override
            public String topicFor(Class<?> type) {
                return type.getSimpleName();
            }

            @Override
            public String topicFor(EventType<?> type) {
                return type.rawType().getSimpleName();
            }
        };
        var engine = new InProcessPrefabStreams(source, (topic, message) -> {
            if (publishAttempts.incrementAndGet() == 1) {
                throw new IllegalStateException("temporary publish failure");
            }
            published.add(message);
        }, new InMemoryStreamStateRepository(), topics);
        engine.from(Word.class).map(word -> new Count(word.id(), 1)).to(Count.class);

        try (var topology = engine.start()) {
            var input = new StreamMessage("message-1", "1", new Word("1", "once"), Map.of(), Instant.now());
            assertThatThrownBy(() -> source.deliver("Word", input))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("temporary publish failure");

            source.deliver("Word", input);

            assertThat(publishAttempts).hasValue(2);
            assertThat(published).hasSize(1);
        }
    }

    @Test
    void breakoutIsRejectedWhenTopologyStarts() {
        var breakoutHarness = new InMemoryTopologyTestHarness("engine");
        breakoutHarness.streams().from(Word.class)
                .breakout(new TestBreakoutAdapter())
                .to(Word.class);
        assertThatThrownBy(breakoutHarness::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("in-process")
                .hasMessageContaining("breakout");
    }

    @Test
    void wallClockPunctuationForwardsAndSkipsMissedIntervals() {
        var harness = new InMemoryTopologyTestHarness("engine");
        var executions = new AtomicInteger();
        harness.streams().from(Word.class).process(new SchedulingProcessor(executions)).to("timer-output");

        try (var topology = harness.start()) {
            topology.advanceWallClock(Duration.ofSeconds(1));
            assertThat(executions).hasValue(1);
            assertThat(topology.output("timer-output", Word.class).values())
                    .containsExactly(new Word("timer", "1"));

            topology.advanceWallClock(Duration.ofSeconds(5));
            assertThat(executions).hasValue(2);
            assertThat(topology.output("timer-output", Word.class).values())
                    .containsExactly(new Word("timer", "2"));

            topology.advanceWallClock(Duration.ofSeconds(1));
            assertThat(executions).hasValue(3);
        }
    }

    @Test
    void scheduledOccurrencesRebalanceAfterJoiningAndLeavingMembers() {
        var now = Instant.parse("2030-01-01T00:00:00Z");
        var clock = new AdjustableClock(now);
        var repository = new InMemoryStreamStateRepository();
        var firstSource = new InMemoryStreamSource();
        var secondSource = new InMemoryStreamSource();
        var firstSink = new InMemoryStreamSink();
        var secondSink = new InMemoryStreamSink();
        var firstResolver = new InMemoryTopologyTestHarness.InMemoryTopicResolver("punctuation-group");
        var secondResolver = new InMemoryTopologyTestHarness.InMemoryTopicResolver("punctuation-group");
        var executions = new AtomicInteger();
        var firstEngine = testEngine(firstSource, firstSink, repository, firstResolver, clock);
        var secondEngine = testEngine(secondSource, secondSink, repository, secondResolver, clock);
        firstEngine.from(Word.class).process(new SchedulingProcessor(executions)).to("timer-output");
        secondEngine.from(Word.class).process(new SchedulingProcessor(executions)).to("timer-output");

        var firstTopology = firstEngine.start();
        var secondTopology = secondEngine.start();
        try {
            clock.advance(Duration.ofSeconds(1));
            firstTopology.runScheduledTasks();
            secondTopology.runScheduledTasks();
            firstTopology.runScheduledTasks();
            secondTopology.runScheduledTasks();
            assertThat(executions).hasValue(1);
            var firstMemberOwned = !firstSink.isEmpty("timer-output");
            var secondMemberOwned = !secondSink.isEmpty("timer-output");
            assertThat(firstMemberOwned ^ secondMemberOwned)
                    .as("first member owns output: %s, second member owns output: %s",
                            firstMemberOwned, secondMemberOwned)
                    .isTrue();

            if (firstMemberOwned) {
                firstTopology.close();
                clock.advance(Duration.ofSeconds(1));
                secondTopology.runScheduledTasks();
            } else {
                secondTopology.close();
                clock.advance(Duration.ofSeconds(1));
                firstTopology.runScheduledTasks();
            }
            assertThat(executions).hasValue(2);
        } finally {
            firstTopology.close();
            secondTopology.close();
        }

        var beforeJoin = List.of("member-a");
        var afterJoin = List.of("member-a", "member-b");
        var movedToJoiner = java.util.stream.LongStream.range(0, 100)
                .mapToObj(index -> "registration-" + index)
                .anyMatch(registration -> InProcessPrefabStreams.assignedMember(registration, afterJoin)
                        .equals("member-b"));
        assertThat(movedToJoiner).isTrue();
        assertThat(InProcessPrefabStreams.assignedMember("registration-1", beforeJoin))
                .isEqualTo("member-a");
    }

    @Test
    void changedScheduleIntervalJoinsAfterTheOldTopologyDrains() {
        var clock = new AdjustableClock(Instant.parse("2030-01-01T00:00:00Z"));
        var repository = new InMemoryStreamStateRepository();
        var executions = new AtomicInteger();
        var firstEngine = testEngine(new InMemoryStreamSource(), new InMemoryStreamSink(), repository,
                new InMemoryTopologyTestHarness.InMemoryTopicResolver("punctuation-group"), clock);
        firstEngine.from(Word.class)
                .process(new SchedulingProcessor(executions, Duration.ofSeconds(1)))
                .to("timer-output");
        var firstTopology = firstEngine.start();

        var changedEngine = testEngine(new InMemoryStreamSource(), new InMemoryStreamSink(), repository,
                new InMemoryTopologyTestHarness.InMemoryTopicResolver("punctuation-group"), clock);
        changedEngine.from(Word.class)
                .process(new SchedulingProcessor(executions, Duration.ofSeconds(2)))
                .to("timer-output");
        assertThatThrownBy(changedEngine::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Topology declaration changed");

        firstTopology.close();
        var changedTopology = changedEngine.start();
        try {
            clock.advance(Duration.ofSeconds(2));
            changedTopology.runScheduledTasks();

            assertThat(executions).hasValue(1);
        } finally {
            changedTopology.close();
        }
    }

    @Test
    void tracingAddsOutputMessageIdentityAndCorrelation() {
        var harness = new InMemoryTopologyTestHarness("engine", true);
        harness.streams().from(Word.class).map(word -> new Word(word.id(), word.text())).to(Word.class);
        try (var topology = harness.start()) {
            topology.input(Word.class).sendWithMessageId(new Word("1", "trace"), "origin");

            var message = topology.output(Word.class).messages().getFirst();
            assertThat(message.messageId()).isPresent().get().isNotEqualTo("origin");
            assertThat(message.correlationIds()).containsExactly("origin");
        }
    }

    @Test
    void statefulProcessorUsesTransactionalStore() {
        var harness = new InMemoryTopologyTestHarness("engine");
        harness.streams().from(Word.class).process(new CountingProcessor()).to(Count.class);
        try (var topology = harness.start()) {
            var input = topology.input(Word.class);
            input.send(new Word("1", "a"));
            input.send(new Word("1", "b"));

            assertThat(topology.output(Count.class).values())
                    .containsExactly(new Count("1", 1), new Count("1", 2));
        }
    }

    @Test
    void globalStoreUpdatesAreVisibleToProcessorsInTheSameHarness() {
        var harness = new InMemoryTopologyTestHarness("engine");
        var profiles = harness.streams().globalStore(Profile.class);
        harness.streams().from(Lookup.class).process(new ProfileLookupProcessor(profiles)).to(Answer.class);

        try (var topology = harness.start()) {
            topology.input(Profile.class).send(new Profile("customer-1", "gold"));
            topology.input(Lookup.class).send(new Lookup("customer-1"));

            assertThat(topology.output(Answer.class).value()).isEqualTo(new Answer("customer-1", "gold"));
        }
    }

    private static InProcessPrefabStreams testEngine(
            InMemoryStreamSource source,
            InMemoryStreamSink sink,
            InMemoryStreamStateRepository repository,
            InMemoryTopologyTestHarness.InMemoryTopicResolver resolver,
            AdjustableClock clock
    ) {
        return new InProcessPrefabStreams(
                source, sink, repository, resolver, TracingConfig.disabled(), Duration.ofDays(7),
                "punctuation-group", clock, false);
    }

    private record Word(String id, String text) implements Keyed<String> {
        @Override
        public String key() {
            return id;
        }
    }

    private record Count(String id, int count) implements Keyed<String> {
        @Override
        public String key() {
            return id;
        }
    }

    private record WindowCount(String id, int count) implements Keyed<String> {
        @Override
        public String key() {
            return id;
        }
    }

    private record Status(String id, String value) implements Keyed<String> {
        @Override
        public String key() {
            return id;
        }
    }

    private record Joined(String id, String text, String status) implements Keyed<String> {
        @Override
        public String key() {
            return id;
        }
    }

    private record Profile(String id, String tier) implements Keyed<String> {
        @Override
        public String key() {
            return id;
        }
    }

    private record Lookup(String id) implements Keyed<String> {
        @Override
        public String key() {
            return id;
        }
    }

    private record Answer(String id, String tier) implements Keyed<String> {
        @Override
        public String key() {
            return id;
        }
    }

    private sealed interface Kinded extends Keyed<String> permits Kinded.Created, Kinded.Deleted {
        @Override
        String key();

        record Created(String id, String text) implements Kinded {
            @Override
            public String key() {
                return id;
            }

            Word asWord() {
                return new Word(id, text);
            }
        }

        record Deleted(String id) implements Kinded {
            @Override
            public String key() {
                return id;
            }
        }
    }

    private static final class CountingProcessor extends StatefulStreamProcessor<String, Word, String, Count> {
        private final AtomicInteger initializations = new AtomicInteger();

        private CountingProcessor() {
            super(Count.class);
        }

        @Override
        public void initContext(StreamProcessorContext<String, Count> context) {
            initializations.incrementAndGet();
            super.initContext(context);
        }

        @Override
        public void process(StreamRecord<String, Word> record) {
            var value = store(Count.class).putOrUpdate(
                    record.key(),
                    () -> new Count(record.key(), 1),
                    count -> new Count(count.id(), count.count() + 1));
            forward(record.key(), value);
        }
    }

    private static final class SchedulingProcessor extends ContextualStreamProcessor<String, Word, String, Word> {
        private final AtomicInteger executions;
        private final Duration interval;

        private SchedulingProcessor(AtomicInteger executions) {
            this(executions, Duration.ofSeconds(1));
        }

        private SchedulingProcessor(AtomicInteger executions, Duration interval) {
            this.executions = executions;
            this.interval = interval;
        }

        @Override
        public void process(StreamRecord<String, Word> record) {
            forward(record);
        }

        @Override
        public void initContext(StreamProcessorContext<String, Word> context) {
            super.initContext(context);
            context.schedule(interval, now -> {
                var sequence = executions.incrementAndGet();
                forward(new StreamRecord<>("timer", new Word("timer", Integer.toString(sequence)), now, Map.of()));
            });
        }
    }

    private static final class ProfileLookupProcessor
            extends ContextualStreamProcessor<String, Lookup, String, Answer> {
        private final GlobalStore<String, Profile> profiles;

        private ProfileLookupProcessor(GlobalStore<String, Profile> profiles) {
            this.profiles = profiles;
        }

        @Override
        public Collection<GlobalStore<?, ?>> globalStores() {
            return List.of(profiles);
        }

        @Override
        public void process(StreamRecord<String, Lookup> record) {
            var tier = profiles.get(record.key()).map(Profile::tier).orElse("unknown");
            forward(record.key(), new Answer(record.key(), tier));
        }
    }

    private static final class TestBreakoutAdapter implements
            be.appify.prefab.streams.StreamBreakoutAdapter<String, Word, String, Word, Object, Object> {
        @Override
        public StreamBackend backend() {
            return InProcessPrefabStreams.BACKEND;
        }

        @Override
        public Class<Object> nativeInputType() {
            return Object.class;
        }

        @Override
        public Object apply(Object nativeInput) {
            return nativeInput;
        }
    }
}
