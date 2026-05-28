package be.appify.prefab.core.sns;

import be.appify.prefab.core.annotations.PublishTo;
import io.awspring.cloud.sns.core.SnsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.CreateTopicRequest;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utility class for managing SNS topics, SQS queues, and for subscribing to messages with optional dead-letter
 * handling.
 */
@Component
@ConditionalOnClass(SnsTemplate.class)
public class SqsUtil implements DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(SqsUtil.class);

    private final String deadLetterQueueName;
    private final Integer maxRetries;
    private final SnsClient snsClient;
    private final SqsAsyncClient sqsClient;
    private final SqsDeserializer sqsDeserializer;
    private final RetryTemplate retryTemplate;
    private final List<ExecutorService> pollingExecutors = new CopyOnWriteArrayList<>();
    private final Map<Class<?>, String> typeToTopic = new ConcurrentHashMap<>();
    private final Map<Class<?>, Set<String>> typeToTopics = new ConcurrentHashMap<>();
    private final Map<Class<?>, PublishTo> publishToStrategies = new ConcurrentHashMap<>();

    /**
     * Constructs a new SqsUtil.
     *
     * @param applicationName
     *         the Spring application name
     * @param deadLetterQueueName
     *         the dead-letter queue name (optional)
     * @param maxRetries
     *         the maximum number of receive attempts before routing to the DLQ
     * @param minimumBackoff
     *         the minimum backoff time in milliseconds
     * @param maximumBackoff
     *         the maximum backoff time in milliseconds
     * @param backoffMultiplier
     *         the backoff multiplier
     * @param snsClient
     *         the SNS sync client
     * @param sqsClient
     *         the SQS async client
     * @param sqsDeserializer
     *         the SQS deserializer
     */
    public SqsUtil(
            @Value("${spring.application.name}") String applicationName,
            @Value("${prefab.dlt.name:}") String deadLetterQueueName,
            @Value("${prefab.dlt.retries.limit:5}") Integer maxRetries,
            @Value("${prefab.dlt.retries.minimum-backoff-ms:1000}") Integer minimumBackoff,
            @Value("${prefab.dlt.retries.maximum-backoff-ms:30000}") Integer maximumBackoff,
            @Value("${prefab.dlt.retries.multiplier:1.5}") Double backoffMultiplier,
            SnsClient snsClient,
            SqsAsyncClient sqsClient,
            SqsDeserializer sqsDeserializer
    ) {
        this.deadLetterQueueName = !isEmpty(deadLetterQueueName) ? deadLetterQueueName : applicationName + ".dlt";
        this.maxRetries = maxRetries;
        this.snsClient = snsClient;
        this.sqsClient = sqsClient;
        this.sqsDeserializer = sqsDeserializer;
        this.retryTemplate = new RetryTemplate(org.springframework.core.retry.RetryPolicy.builder()
                .maxRetries(maxRetries)
                .delay(java.time.Duration.ofMillis(minimumBackoff))
                .maxDelay(java.time.Duration.ofMillis(maximumBackoff))
                .multiplier(backoffMultiplier)
                .build());
    }

    /**
     * Ensures that the specified SNS topic exists, creating it if necessary.
     *
     * @param topicName
     *         the topic name
     * @return the topic ARN
     */
    public String ensureTopicExists(String topicName) {
        try {
            var createResponse = snsClient.createTopic(
                    CreateTopicRequest.builder().name(topicName).build()
            );
            return createResponse.topicArn();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to create SNS topic [%s], make sure SNS is available".formatted(topicName), e);
        }
    }

    /**
     * Subscribes to an SNS topic via an SQS queue, creating the queue if necessary.
     *
     * @param request
     *         the subscription request containing queue name, topic, type, and consumer
     * @param <T>
     *         the type of the messages
     */
    public <T> void subscribe(SqsSubscriptionRequest<T> request) {
        var topicArn = ensureTopicExists(request.topic());
        var queueUrl = ensureQueueExists(request.queueName());
        var queueArn = getQueueArn(queueUrl);

        if (request.isUsingDefaultDeadLetterQueueName()) {
            configureDlq(queueUrl, deadLetterQueueName, maxRetries);
        } else if (request.deadLetterQueueName() != null) {
            configureDlq(queueUrl, request.deadLetterQueueName(), maxRetries);
        }

        subscribeQueueToTopic(topicArn, queueArn);
        startPolling(request, queueUrl);
    }

    /**
     * Registers an event type in the deserializer allowlist for safe deserialization from SNS Subject headers.
     * Permitted subtypes of sealed interfaces are registered recursively.
     *
     * @param typeName
     *         the fully-qualified class name that may appear as the SNS message Subject
     * @param type
     *         the corresponding Java class
     */
    public void registerType(String typeName, Class<?> type) {
        sqsDeserializer.registerType(typeName, type);
    }

    /**
     * Registers a topic for an event type so the generic publisher can resolve it at runtime. Permitted subtypes of
     * sealed interfaces are registered recursively.
     *
     * @param topic
     *         the SNS topic name
     * @param type
     *         the Java class of the event
     */
    public void registerEventTopic(String topic, Class<?> type) {
        typeToTopic.put(type, topic);
        typeToTopics.computeIfAbsent(type, ignored -> new LinkedHashSet<>()).add(topic);
        if (type.isSealed()) {
            for (var subtype : type.getPermittedSubclasses()) {
                registerEventTopic(topic, subtype);
            }
        }
    }

    /**
     * Stores the publish-to strategy for an event type.
     * Called by generated registrar beans during application startup.
     *
     * @param type      the event type
     * @param publishTo the strategy that governs which topics are targeted at dispatch time
     */
    public void registerPublishTo(Class<?> type, PublishTo publishTo) {
        publishToStrategies.put(type, publishTo);
    }

    /**
     * Resolves the topics to which the given event should be dispatched, applying the registered
     * {@link PublishTo} strategy. Defaults to {@link PublishTo#FIRST} when no strategy is registered.
     *
     * @param event the event instance
     * @return the ordered list of target topic names
     * @throws IllegalArgumentException if no topics are registered for the event type
     */
    public List<String> topicsForDispatch(Object event) {
        var type = event.getClass();
        var topics = typeToTopics.getOrDefault(type, Set.of());
        if (topics.isEmpty()) {
            var primary = tryTopicForType(type)
                    .orElseThrow(() -> new IllegalArgumentException("No topics registered for type: " + type.getName()));
            topics = Set.of(primary);
        }
        var strategy = publishToStrategies.getOrDefault(type, PublishTo.FIRST);
        return switch (strategy) {
            case ALL -> List.copyOf(topics);
            case FIRST -> List.of(topics.iterator().next());
        };
    }

    /**
     * Resolves the SNS topic for a given event type.
     *
     * @param type
     *         the event class
     * @return the registered topic name
     * @throws IllegalArgumentException
     *         if no topic is registered for the type
     */
    public String topicForType(Class<?> type) {
        return tryTopicForType(type)
                .orElseThrow(() -> new IllegalArgumentException("No topic registered for type: " + type.getName()));
    }

    /**
     * Resolves the SNS topic for a given event type if one is registered.
     *
     * @param type
     *         the event class
     * @return the registered topic name if found
     * @throws IllegalStateException
     *         if multiple equally specific topics match the type
     */
    public Optional<String> tryTopicForType(Class<?> type) {
        var topic = typeToTopic.get(type);
        if (topic != null) {
            return Optional.of(topic);
        }
        String selectedTopic = null;
        Integer selectedDistance = null;
        for (var entry : typeToTopic.entrySet()) {
            if (!entry.getKey().isAssignableFrom(type)) {
                continue;
            }
            var distance = hierarchyDistance(type, entry.getKey());
            if (distance.isEmpty()) {
                continue;
            }
            if (selectedDistance == null || distance.get() < selectedDistance) {
                selectedDistance = distance.get();
                selectedTopic = entry.getValue();
            } else if (distance.get().equals(selectedDistance) && !entry.getValue().equals(selectedTopic)) {
                throw new IllegalStateException(
                        "Ambiguous topics registered for type %s: %s and %s"
                                .formatted(type.getName(), selectedTopic, entry.getValue())
                );
            }
        }
        return Optional.ofNullable(selectedTopic);
    }

    private static Optional<Integer> hierarchyDistance(Class<?> source, Class<?> target) {
        if (source.equals(target)) {
            return Optional.of(0);
        }
        if (!target.isAssignableFrom(source)) {
            return Optional.empty();
        }
        var queue = new java.util.ArrayDeque<Class<?>>();
        var distances = new java.util.HashMap<Class<?>, Integer>();
        queue.add(source);
        distances.put(source, 0);
        while (!queue.isEmpty()) {
            var current = queue.remove();
            var distance = distances.get(current);
            if (current.equals(target)) {
                return Optional.of(distance);
            }
            var superclass = current.getSuperclass();
            if (superclass != null && distances.putIfAbsent(superclass, distance + 1) == null) {
                queue.add(superclass);
            }
            for (var iface : current.getInterfaces()) {
                if (distances.putIfAbsent(iface, distance + 1) == null) {
                    queue.add(iface);
                }
            }
        }
        return Optional.empty();
    }

    private String ensureQueueExists(String queueName) {
        var sanitizedName = queueName.replaceAll("[^a-zA-Z0-9_-]", "-");
        if (!sanitizedName.equals(queueName)) {
            log.warn("SQS queue name [{}] contains invalid characters and was sanitized to [{}]", queueName,
                    sanitizedName);
        }
        try {
            return sqsClient.createQueue(
                    CreateQueueRequest.builder().queueName(sanitizedName).build()
            ).get().queueUrl();
        } catch (Exception e) {
            try {
                return sqsClient.getQueueUrl(
                        GetQueueUrlRequest.builder().queueName(sanitizedName).build()
                ).get().queueUrl();
            } catch (Exception ex) {
                throw new IllegalStateException(
                        "Failed to create or get SQS queue [%s]".formatted(sanitizedName), ex);
            }
        }
    }

    private String getQueueArn(String queueUrl) {
        try {
            var attributes = sqsClient.getQueueAttributes(
                    GetQueueAttributesRequest.builder()
                            .queueUrl(queueUrl)
                            .attributeNames(QueueAttributeName.QUEUE_ARN)
                            .build()
            ).get().attributes();
            return attributes.get(QueueAttributeName.QUEUE_ARN);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to get queue ARN for [%s]".formatted(queueUrl), e);
        }
    }

    private void configureDlq(String queueUrl, String dlqName, Integer maxReceiveCount) {
        var dlqUrl = ensureQueueExists(dlqName);
        var dlqArn = getQueueArn(dlqUrl);
        var redrivePolicy = "{\"maxReceiveCount\":%d,\"deadLetterTargetArn\":\"%s\"}"
                .formatted(maxReceiveCount, dlqArn);
        try {
            sqsClient.setQueueAttributes(
                    SetQueueAttributesRequest.builder()
                            .queueUrl(queueUrl)
                            .attributes(Map.of(QueueAttributeName.REDRIVE_POLICY, redrivePolicy))
                            .build()
            ).get();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to configure DLQ for queue [%s]".formatted(queueUrl), e);
        }
    }

    private void subscribeQueueToTopic(String topicArn, String queueArn) {
        try {
            snsClient.subscribe(
                    SubscribeRequest.builder()
                            .topicArn(topicArn)
                            .protocol("sqs")
                            .endpoint(queueArn)
                            .build()
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to subscribe queue [%s] to topic [%s]".formatted(queueArn, topicArn), e);
        }
    }

    private <T> void startPolling(SqsSubscriptionRequest<T> request, String queueUrl) {
        var pollingExecutor = Executors.newSingleThreadExecutor(r -> {
            var thread = new Thread(r, "sqs-poller-" + request.queueName());
            thread.setDaemon(true);
            return thread;
        });
        pollingExecutors.add(pollingExecutor);
        pollingExecutor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    var messages = sqsClient.receiveMessage(
                            ReceiveMessageRequest.builder()
                                    .queueUrl(queueUrl)
                                    .maxNumberOfMessages(10)
                                    .waitTimeSeconds(20)
                                    .messageAttributeNames("All")
                                    .build()
                    ).get().messages();
                    for (var message : messages) {
                        processMessage(request, queueUrl, message);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    log.error("Error polling SQS queue [{}]", request.queueName(), e);
                }
            }
        });
    }

    private <T> void processMessage(SqsSubscriptionRequest<T> request, String queueUrl, Message message) {
        try {
            request.retryTemplate().orElse(retryTemplate).execute(() -> {
                try {
                    T event = sqsDeserializer.deserialize(request.topic(), message.body(), request.type());
                    request.consumer().accept(event);
                    deleteMessage(queueUrl, message);
                } catch (Exception e) {
                    log.warn("Error processing SQS message: {}, cause: {}", truncate(message.body(), 200),
                            e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
                    throw e;
                }
                return null;
            });
        } catch (RetryException e) {
            log.error("Retries exhausted when processing SQS message: {}", message, e);
            throw new RuntimeException(e);
        }
    }

    private void deleteMessage(String queueUrl, Message message) {
        try {
            sqsClient.deleteMessage(
                    DeleteMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .receiptHandle(message.receiptHandle())
                            .build()
            ).get();
        } catch (Exception e) {
            log.warn("Failed to delete SQS message: {}", message.messageId(), e);
        }
    }

    /**
     * Deletes all SQS queues.
     */
    public void deleteAllQueues() {
        try {
            var queueUrls = sqsClient.listQueues().get().queueUrls();
            for (var url : queueUrls) {
                sqsClient.deleteQueue(
                                software.amazon.awssdk.services.sqs.model.DeleteQueueRequest.builder().queueUrl(url).build())
                        .get();
            }
        } catch (Exception e) {
            log.warn("Failed to delete all SQS queues", e);
        }
    }

    /**
     * Deletes all SNS topics.
     */
    public void deleteAllTopics() {
        try {
            var topics = snsClient.listTopics().topics();
            for (var topic : topics) {
                snsClient.deleteTopic(software.amazon.awssdk.services.sns.model.DeleteTopicRequest.builder()
                        .topicArn(topic.topicArn()).build());
            }
        } catch (Exception e) {
            log.warn("Failed to delete all SNS topics", e);
        }
    }

    /**
     * Shuts down all SQS polling executors, stopping message consumption.
     */
    @Override
    public void destroy() {
        pollingExecutors.forEach(ExecutorService::shutdownNow);
    }

    private static String truncate(String body, int maxLength) {
        if (body == null) {
            return "<null>";
        }
        return body.length() <= maxLength
                ? body
                : body.substring(0, maxLength) + "...[truncated " + (body.length() - maxLength) + " chars]";
    }
}
