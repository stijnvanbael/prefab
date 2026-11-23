package be.appify.prefab.streams.snssqs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import be.appify.prefab.core.sns.SnsSerializer;
import be.appify.prefab.core.sns.SqsMessageMetadata;
import be.appify.prefab.core.sns.SqsSubscription;
import be.appify.prefab.core.sns.SqsSubscriptionRequest;
import be.appify.prefab.core.sns.SqsUtil;
import io.awspring.cloud.sns.core.SnsNotification;
import io.awspring.cloud.sns.core.SnsTemplate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import tools.jackson.databind.json.JsonMapper;

/**
 * A minimal in-memory SNS/SQS: topics with one queue per subscription, FIFO deduplication and redelivery. It stands
 * in for the AWS clients behind {@link SqsUtil} and {@link SnsTemplate}, so the source, sink and engine can run
 * together in a unit test.
 */
final class FakeSnsSqs {
    private final JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
    private final SqsUtil sqsUtil = mock(SqsUtil.class);
    private final SnsTemplate snsTemplate = mock(SnsTemplate.class);
    private final SnsSerializer serializer = mock(SnsSerializer.class);
    private final List<SqsSubscriptionRequest<Object>> subscriptions = new ArrayList<>();
    private final List<Delivery> history = new CopyOnWriteArrayList<>();
    private final ConcurrentLinkedQueue<Delivery> pending = new ConcurrentLinkedQueue<>();
    private final List<Publication> publications = new CopyOnWriteArrayList<>();
    private final Set<String> deduplicated = ConcurrentHashMap.newKeySet();
    private final List<String> deleted = new CopyOnWriteArrayList<>();
    private final AtomicInteger messageCounter = new AtomicInteger();
    private Instant clock = Instant.parse("2026-01-01T10:00:00Z");

    /** A message that was published to a topic. */
    record Publication(String topic, String payload, Map<String, String> attributes, String groupId,
                       String deduplicationId) {
    }

    private record Delivery(String sqsMessageId, SqsSubscriptionRequest<Object> subscription, String payload,
                            Map<String, String> attributes, String groupId, Instant sentAt) {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    FakeSnsSqs() {
        when(sqsUtil.subscribe(any())).thenAnswer(call -> {
            subscriptions.add(call.getArgument(0));
            return (SqsSubscription) () -> { };
        });
        when(sqsUtil.ensureTopicExists(anyString(), anyBoolean()))
                .thenAnswer(call -> "arn:" + call.getArgument(0));
        when(serializer.serialize(anyString(), any())).thenAnswer(call -> mapper.writeValueAsString(call.getArgument(1)));
        doAnswer(call -> {
            var notification = (SnsNotification<String>) call.getArgument(1);
            var attributes = new LinkedHashMap<String, String>();
            notification.getHeaders().forEach((name, value) -> attributes.put(name, String.valueOf(value)));
            var topic = ((String) call.getArgument(0)).substring("arn:".length());
            publications.add(new Publication(topic, notification.getPayload(), attributes, notification.getGroupId(),
                    notification.getDeduplicationId()));
            route(topic, notification.getPayload(), attributes, notification.getGroupId(),
                    notification.getDeduplicationId());
            return null;
        }).when(snsTemplate).sendNotification(anyString(), any(SnsNotification.class));
    }

    SqsUtil sqsUtil() {
        return sqsUtil;
    }

    SnsTemplate snsTemplate() {
        return snsTemplate;
    }

    SnsSerializer serializer() {
        return serializer;
    }

    /** Publishes a message from outside the topology, the way an application publishes an event. */
    void publish(String topic, Object event, Map<String, String> attributes, String groupId) {
        try {
            route(topic, mapper.writeValueAsString(event), attributes, groupId, "external-" + messageCounter.incrementAndGet());
        } catch (RuntimeException e) {
            throw new IllegalStateException("Cannot publish " + event, e);
        }
    }

    /** Publishes a tombstone from outside the topology. */
    void publishTombstone(String topic, String key, String groupId) {
        route(topic, "null", Map.of("prefab-tombstone", "true", "prefab-key", key), groupId,
                "external-" + messageCounter.incrementAndGet());
    }

    /** Lets time pass, which moves the {@code SentTimestamp} of later messages. */
    void advance(Duration duration) {
        clock = clock.plus(duration);
    }

    /** Delivers messages until every queue is empty, as the pollers of the instances would. */
    void drain() {
        while (!pending.isEmpty()) {
            deliver(pending.poll());
        }
    }

    /**
     * Delivers messages the way the pollers of one instance do: one thread per queue, all running at the same time,
     * until every queue is empty.
     */
    void drainConcurrently() {
        var executor = Executors.newCachedThreadPool();
        try {
            var queues = new ConcurrentHashMap<SqsSubscriptionRequest<Object>, LinkedBlockingQueue<Delivery>>();
            for (var subscription : subscriptions) {
                queues.put(subscription, new LinkedBlockingQueue<>());
            }
            while (true) {
                Delivery next;
                while ((next = pending.poll()) != null) {
                    queues.get(next.subscription()).add(next);
                }
                var work = new ArrayList<Future<?>>();
                for (var queue : queues.values()) {
                    if (!queue.isEmpty()) {
                        work.add(executor.submit(() -> {
                            Delivery delivery;
                            while ((delivery = queue.poll()) != null) {
                                deliver(delivery);
                            }
                        }));
                    }
                }
                for (var future : work) {
                    future.get();
                }
                if (pending.isEmpty() && queues.values().stream().allMatch(Collection::isEmpty)) {
                    return;
                }
            }
        } catch (ExecutionException e) {
            throw new IllegalStateException("Delivery failed", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted", e);
        } finally {
            executor.shutdownNow();
        }
    }

    /** Delivers a message again, as SQS does after the visibility timeout of a message that was not deleted. */
    void redeliverAll() {
        List.copyOf(history).forEach(this::deliver);
    }

    List<Publication> publications(String topic) {
        return publications.stream().filter(publication -> publication.topic().equals(topic)).toList();
    }

    List<String> queueNames() {
        return subscriptions.stream().map(SqsSubscriptionRequest::queueName).toList();
    }

    List<Duration> visibilityTimeouts() {
        return subscriptions.stream().map(request -> request.visibilityTimeout().orElseThrow()).toList();
    }

    List<String> deletedMessages() {
        return List.copyOf(deleted);
    }

    <T> List<T> payloads(String topic, Class<T> type) {
        return publications(topic).stream().map(publication -> read(publication.payload(), type)).toList();
    }

    private void route(String topic, String payload, Map<String, String> attributes, String groupId,
                       String deduplicationId) {
        if (groupId != null && !deduplicated.add(topic + "/" + deduplicationId)) {
            return;
        }
        for (var subscription : subscriptions) {
            if (stripFifo(subscription.topic()).equals(stripFifo(topic))) {
                var delivery = new Delivery("sqs-" + messageCounter.incrementAndGet(), subscription, payload, attributes, groupId,
                        clock);
                history.add(delivery);
                pending.add(delivery);
            }
        }
    }

    private void deliver(Delivery delivery) {
        var metadata = new SqsMessageMetadata(delivery.sqsMessageId(), delivery.attributes(), delivery.sentAt(),
                Optional.ofNullable(delivery.groupId()));
        var event = metadata.tombstone() ? null : read(delivery.payload(), delivery.subscription().type());
        delivery.subscription().messageConsumer().orElseThrow()
                .accept(event, metadata, () -> deleted.add(delivery.sqsMessageId()));
    }

    private <T> T read(String payload, Class<T> type) {
        return mapper.readValue(payload, type);
    }

    private static String stripFifo(String topic) {
        return topic.endsWith(".fifo") ? topic.substring(0, topic.length() - ".fifo".length()) : topic;
    }
}
