package be.appify.prefab.streams.engine;

import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.versioning.EventVersions;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.tracing.TracingConfig;
import be.appify.prefab.streams.testing.TopologyTestHarness;
import java.util.Objects;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Provides synchronous, container-free topology tests backed by the in-process engine. */
public final class InMemoryTopologyTestHarness implements TopologyTestHarness {
    private final InMemoryStreamSource source = new InMemoryStreamSource();
    private final InMemoryStreamSink sink = new InMemoryStreamSink(source);
    private final InMemoryStreamStateRepository repository = new InMemoryStreamStateRepository();
    private final InMemoryTopicResolver topicResolver;
    private final InProcessPrefabStreams streams;
    private final String appId;

    /** Creates a harness for one test topology. */
    public InMemoryTopologyTestHarness(String appId) {
        this(appId, false);
    }

    /** Creates a harness for one test topology. */
    public InMemoryTopologyTestHarness(String appId, boolean tracing) {
        this.appId = Objects.requireNonNull(appId, "appId must not be null");
        topicResolver = new InMemoryTopicResolver(appId);
        streams = new InProcessPrefabStreams(
                source,
                sink,
                repository,
                topicResolver,
                tracing ? TracingConfig.withTracing() : TracingConfig.disabled());
    }

    @Override
    public PrefabStreams streams() {
        return streams;
    }

    @Override
    public InMemoryTopologySession start() {
        return new InMemoryTopologySession(appId, topicResolver, source, sink, repository, streams.start());
    }

    /** Registers the in-memory implementation as a test harness factory. */
    public static final class Factory implements TopologyTestHarness.Factory {
        @Override
        public String backendName() {
            return InProcessPrefabStreams.BACKEND.name();
        }

        @Override
        public TopologyTestHarness create(String appId, boolean tracing) {
            return new InMemoryTopologyTestHarness(appId, tracing);
        }
    }

    static final class InMemoryTopicResolver implements StreamTopicResolver {
        private final String appId;
        private final Map<Class<?>, String> classTopics = new ConcurrentHashMap<>();
        private final Map<String, String> eventTopics = new ConcurrentHashMap<>();

        InMemoryTopicResolver(String appId) {
            this.appId = appId;
        }

        @Override
        public String topicFor(Class<?> type) {
            return classTopics.getOrDefault(type, appId + "-" + kebabCase(type.getSimpleName()));
        }

        @Override
        public String topicFor(EventType<?> type) {
            var registered = eventTopics.get(type.id());
            if (registered != null) {
                return registered;
            }
            var topics = EventVersions.declarationFor(type).topic();
            if (topics.length == 0) {
                throw new IllegalArgumentException("No topic declared for " + type);
            }
            return topics[0];
        }

        @Override
        public void register(Class<?> type, String topic) {
            classTopics.put(type, topic);
        }

        @Override
        public void register(EventType<?> type, String topic) {
            eventTopics.put(type.id(), topic);
        }

        private static String kebabCase(String value) {
            return value.replaceAll("([a-z0-9])([A-Z])", "$1-$2")
                    .replaceAll("([A-Z])([A-Z][a-z])", "$1-$2")
                    .toLowerCase(java.util.Locale.ROOT);
        }
    }
}
