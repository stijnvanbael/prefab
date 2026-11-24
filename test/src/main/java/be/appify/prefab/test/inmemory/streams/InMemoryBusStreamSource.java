package be.appify.prefab.test.inmemory.streams;

import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.domain.Keyed;
import be.appify.prefab.core.domain.TypedEvent;
import be.appify.prefab.streams.engine.StreamMessage;
import be.appify.prefab.streams.engine.StreamSource;
import be.appify.prefab.streams.tracing.MessageTracing;
import be.appify.prefab.test.inmemory.messaging.EncodedMessage;
import be.appify.prefab.test.inmemory.messaging.InMemoryEventBus;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reads the sources of a topology from the {@link InMemoryEventBus}.
 *
 * <p>Every source subscribes to its topic on the bus, so a topology sees the events the application publishes and
 * the output of other topologies. The bus delivers synchronously and in publication order, which makes every source
 * ordered. A message that a stream published carries its message identity and event time in its headers; an event
 * that the application published has neither, so it gets a fresh identity and the time of delivery.
 */
public final class InMemoryBusStreamSource implements StreamSource {
    /** Header that carries the event time of a message that a stream published. */
    static final String EVENT_TIME_HEADER = "prefab-event-time";

    private final InMemoryEventBus bus;
    private final Clock clock;

    /**
     * Creates a source.
     *
     * @param bus the bus that carries the events
     */
    public InMemoryBusStreamSource(InMemoryEventBus bus) {
        this(bus, Clock.systemUTC());
    }

    InMemoryBusStreamSource(InMemoryEventBus bus, Clock clock) {
        this.bus = bus;
        this.clock = clock;
    }

    @Override
    public boolean ordered(String topic) {
        return true;
    }

    @Override
    public Subscription subscribe(String topic, MessageHandler handler) {
        throw new UnsupportedOperationException(
                "The in-memory bus source needs the value type of a topic; subscribe with a SourceBinding");
    }

    @Override
    public Subscription subscribe(SourceBinding binding, MessageHandler handler) {
        var subscription = bus.subscribe(binding.topic(), "streams-" + binding.name(),
                message -> deliver(binding, message, handler));
        return subscription::close;
    }

    private void deliver(SourceBinding binding, EncodedMessage encoded, MessageHandler handler) {
        var event = encoded.decode();
        var value = TypedEvent.payload(event);
        if (!accepts(binding, encoded, value)) {
            return;
        }
        var acknowledged = new AtomicBoolean();
        handler.onMessage(message(binding, encoded, value), () -> acknowledged.set(true));
        if (!acknowledged.get()) {
            throw new IllegalStateException("Stream engine did not acknowledge a message for " + binding.name());
        }
    }

    /** Several event types can share a topic; a source only takes the ones it is bound to. */
    private static boolean accepts(SourceBinding binding, EncodedMessage encoded, Object value) {
        if (!binding.valueType().isInstance(value)) {
            return false;
        }
        return binding.eventTypeId() == null
                || binding.eventTypeId().equals(encoded.headers().get(EventType.HEADER));
    }

    private StreamMessage message(SourceBinding binding, EncodedMessage encoded, Object value) {
        if (!(value instanceof Keyed<?> keyed)) {
            throw new IllegalStateException("Event %s on topic '%s' has no key, so a stream cannot read it"
                    .formatted(value.getClass().getName(), binding.topic()));
        }
        var headers = encoded.headers();
        var messageId = headers.getOrDefault(MessageTracing.MESSAGE_ID_HEADER, UUID.randomUUID().toString());
        return new StreamMessage(messageId, keyed.key(), value, headers, eventTime(headers));
    }

    private Instant eventTime(java.util.Map<String, String> headers) {
        var published = headers.get(EVENT_TIME_HEADER);
        if (published == null) {
            return clock.instant();
        }
        try {
            return Instant.parse(published);
        } catch (DateTimeParseException e) {
            throw new IllegalStateException(
                    "Header %s is not an ISO-8601 instant: %s".formatted(EVENT_TIME_HEADER, published), e);
        }
    }
}
