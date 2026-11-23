package be.appify.prefab.test.inmemory.messaging;

import be.appify.prefab.core.domain.DomainEventDispatcher;
import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.domain.TypedEvent;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.UnknownEventTypeException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * A synchronous in-process replacement for a message broker, used by integration tests in in-memory mode.
 * <p>
 * Events are encoded when they are published, so serialization problems surface in the publishing call. Delivery
 * happens after the active transaction committed, or immediately when there is none. Messages are delivered in the
 * order they were published per thread; messages published while a handler runs are queued and delivered by the
 * outermost delivery rather than nested. Every subscriber decodes its own copy of the event.
 * <p>
 * A subscriber that throws does not affect the publisher: the failure is logged and recorded, and the
 * test execution listener fails the test that caused it. A test that expects a handler to fail can clear the
 * recorded failures with {@link #drainFailures()}.
 */
public class InMemoryEventBus implements DomainEventDispatcher {
    private static final Logger log = LoggerFactory.getLogger(InMemoryEventBus.class);
    // ScopedValue is unavailable because the project compiles for release 21; the queue is removed again when the
    // outermost delivery ends.
    private static final ThreadLocal<Deque<EncodedMessage>> DELIVERY_QUEUE = new ThreadLocal<>();

    private final EventRegistry eventRegistry;
    private final List<MessageCodec> codecs;
    private final Map<String, List<Subscription>> subscriptions = new ConcurrentHashMap<>();
    private final List<HandlerFailure> failures = new CopyOnWriteArrayList<>();
    private final TransactionOperations outsideTransaction;

    /**
     * Constructs a bus.
     *
     * @param eventRegistry the registry that resolves topics for events
     * @param codecs        the codecs of the supported messaging platforms
     */
    public InMemoryEventBus(EventRegistry eventRegistry, List<MessageCodec> codecs) {
        this(eventRegistry, codecs, TransactionOperations.withoutTransaction());
    }

    /**
     * Constructs a bus that delivers outside the transaction that published the events.
     *
     * @param eventRegistry      the registry that resolves topics for events
     * @param codecs             the codecs of the supported messaging platforms
     * @param outsideTransaction runs a delivery with the current transaction suspended, so that a transactional
     *                           handler starts a transaction of its own instead of joining the one that just
     *                           committed
     */
    public InMemoryEventBus(
            EventRegistry eventRegistry,
            List<MessageCodec> codecs,
            TransactionOperations outsideTransaction
    ) {
        this.eventRegistry = eventRegistry;
        this.codecs = List.copyOf(codecs);
        this.outsideTransaction = outsideTransaction;
    }

    @Override
    public boolean canDispatch(Class<?> eventType) {
        return codecs.stream().anyMatch(codec -> codec.supports(eventType));
    }

    @Override
    public boolean canDispatch(EventType<?> eventType) {
        return codecs.stream().anyMatch(codec -> codec.supports(eventType));
    }

    @Override
    public void dispatch(Object event, String... topicOverrides) {
        var topics = topicOverrides.length > 0 ? List.of(topicOverrides) : eventRegistry.topicsForDispatch(event);
        var codec = codecFor(event);
        var messages = topics.stream().map(topic -> codec.encode(topic, event)).toList();
        deliverAfterCommit(messages);
    }

    /**
     * Subscribes to a topic.
     *
     * @param topic    the topic to listen to
     * @param name     a name identifying the subscriber in logs and failure reports
     * @param listener the callback invoked once per message
     * @return a handle that cancels the subscription
     */
    public Subscription subscribe(String topic, String name, MessageListener listener) {
        var subscription = new Subscription(topic, name, listener);
        subscriptions.computeIfAbsent(topic, key -> new CopyOnWriteArrayList<>()).add(subscription);
        return subscription;
    }

    /**
     * Returns the handler failures recorded since the last reset.
     *
     * @return the recorded failures
     */
    public List<HandlerFailure> failures() {
        return List.copyOf(failures);
    }

    /**
     * Returns and clears the recorded handler failures.
     *
     * @return the failures that were recorded
     */
    public List<HandlerFailure> drainFailures() {
        var drained = List.copyOf(failures);
        failures.removeAll(drained);
        return drained;
    }

    /** Forgets the recorded handler failures. */
    public void reset() {
        failures.clear();
    }

    private MessageCodec codecFor(Object event) {
        return codecs.stream()
                .filter(codec -> event instanceof TypedEvent<?> typed
                        ? codec.supports(typed.type()) : codec.supports(event.getClass()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No messaging platform claims event " + event));
    }

    private void deliverAfterCommit(List<EncodedMessage> messages) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            deliver(messages);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deliver(messages);
            }
        });
    }

    private void deliver(List<EncodedMessage> messages) {
        var active = DELIVERY_QUEUE.get();
        if (active != null) {
            active.addAll(messages);
            return;
        }
        var queue = new ArrayDeque<>(messages);
        DELIVERY_QUEUE.set(queue);
        try {
            // The committed transaction is still bound while afterCommit runs; delivering outside of it keeps
            // transactional handlers from joining it and lets their own events be delivered after their commit.
            outsideTransaction.executeWithoutResult(status -> drain(queue));
        } finally {
            DELIVERY_QUEUE.remove();
        }
    }

    private void drain(Queue<EncodedMessage> queue) {
        while (!queue.isEmpty()) {
            deliverToSubscribers(queue.poll());
        }
    }

    private void deliverToSubscribers(EncodedMessage message) {
        var topicSubscriptions = new ArrayList<>(subscriptions.getOrDefault(message.topic(), List.of()));
        for (var subscription : topicSubscriptions) {
            try {
                subscription.listener().onMessage(message);
            } catch (UnknownEventTypeException e) {
                log.warn("Ignoring message with unknown event type on topic [{}] for [{}]: {}",
                        message.topic(), subscription.name(), e.getMessage());
            } catch (Exception e) {
                log.error("Subscriber [{}] failed to handle a message on topic [{}]",
                        subscription.name(), message.topic(), e);
                failures.add(new HandlerFailure(message.topic(), subscription.name(), e));
            }
        }
    }

    /** Callback for messages on a topic. */
    @FunctionalInterface
    public interface MessageListener {

        /**
         * Handles a message.
         *
         * @param message the message, decoded on demand
         * @throws Exception when handling fails
         */
        void onMessage(EncodedMessage message) throws Exception;
    }

    /** Handle for a subscription on the bus. */
    public final class Subscription implements AutoCloseable {
        private final String topic;
        private final String name;
        private final MessageListener listener;

        private Subscription(String topic, String name, MessageListener listener) {
            this.topic = topic;
            this.name = name;
            this.listener = listener;
        }

        String name() {
            return name;
        }

        MessageListener listener() {
            return listener;
        }

        /** Stops delivering messages to this subscriber. */
        @Override
        public void close() {
            subscriptions.getOrDefault(topic, List.of()).remove(this);
        }
    }
}
