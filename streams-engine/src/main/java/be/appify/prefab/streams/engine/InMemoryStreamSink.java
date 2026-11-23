package be.appify.prefab.streams.engine;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Synchronous sink used by the in-memory topology test harness. */
public final class InMemoryStreamSink implements StreamSink {
    private final Map<String, ConcurrentLinkedQueue<StreamMessage>> messages = new ConcurrentHashMap<>();
    private final InMemoryStreamSource source;

    /** Creates a sink that records published messages without feeding another source. */
    public InMemoryStreamSink() {
        this(null);
    }

    InMemoryStreamSink(InMemoryStreamSource source) {
        this.source = source;
    }

    @Override
    public void publish(String topic, StreamMessage message) {
        messages.computeIfAbsent(topic, ignored -> new ConcurrentLinkedQueue<>()).add(message);
        if (source != null && !source.isDelivering(topic)) {
            source.deliver(topic, message);
        }
    }

    /** Removes and returns all messages published to a topic. */
    public List<StreamMessage> drain(String topic) {
        var queue = messages.computeIfAbsent(topic, ignored -> new ConcurrentLinkedQueue<>());
        var drained = new java.util.ArrayList<StreamMessage>();
        StreamMessage message;
        while ((message = queue.poll()) != null) {
            drained.add(message);
        }
        return List.copyOf(drained);
    }

    /** Removes and returns messages matching an event binding, leaving other bindings available. */
    public List<StreamMessage> drain(String topic, String eventTypeId) {
        if (eventTypeId == null) {
            return drain(topic);
        }
        var queue = messages.computeIfAbsent(topic, ignored -> new ConcurrentLinkedQueue<>());
        var matched = new java.util.ArrayList<StreamMessage>();
        var retained = new java.util.ArrayList<StreamMessage>();
        StreamMessage message;
        while ((message = queue.poll()) != null) {
            if (eventTypeId.equals(message.headers().get(be.appify.prefab.core.domain.EventType.HEADER))) {
                matched.add(message);
            } else {
                retained.add(message);
            }
        }
        queue.addAll(retained);
        return List.copyOf(matched);
    }

    /** Returns whether the topic currently has no messages. */
    public boolean isEmpty(String topic) {
        return messages.getOrDefault(topic, new ConcurrentLinkedQueue<>()).isEmpty();
    }

    /** Returns whether no message for an event binding is available on the topic. */
    public boolean isEmpty(String topic, String eventTypeId) {
        var queue = messages.get(topic);
        return queue == null || queue.stream().noneMatch(message ->
                eventTypeId.equals(message.headers().get(be.appify.prefab.core.domain.EventType.HEADER)));
    }
}
