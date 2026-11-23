package be.appify.prefab.streams.engine;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/** Synchronous source used by the in-memory topology test harness. */
public final class InMemoryStreamSource implements StreamSource {
    private final Map<String, CopyOnWriteArrayList<MessageHandler>> handlers = new ConcurrentHashMap<>();
    private final ThreadLocal<String> deliveringTopic = new ThreadLocal<>();

    @Override
    public boolean ordered(String topic) {
        return true;
    }

    @Override
    public Subscription subscribe(String topic, MessageHandler handler) {
        var subscribers = handlers.computeIfAbsent(topic, ignored -> new CopyOnWriteArrayList<>());
        subscribers.add(handler);
        return () -> subscribers.remove(handler);
    }

    /** Delivers a message synchronously to every current subscriber. */
    public void deliver(String topic, StreamMessage message) {
        List<MessageHandler> subscribers = List.copyOf(handlers.getOrDefault(topic, new CopyOnWriteArrayList<>()));
        var previousTopic = deliveringTopic.get();
        deliveringTopic.set(topic);
        try {
            for (var handler : subscribers) {
                var acknowledged = new AtomicBoolean();
                handler.onMessage(message, () -> acknowledged.set(true));
                if (!acknowledged.get()) {
                    throw new IllegalStateException("Stream message was not acknowledged for topic " + topic);
                }
            }
        } finally {
            if (previousTopic == null) {
                deliveringTopic.remove();
            } else {
                deliveringTopic.set(previousTopic);
            }
        }
    }

    boolean isDelivering(String topic) {
        return topic.equals(deliveringTopic.get());
    }
}
