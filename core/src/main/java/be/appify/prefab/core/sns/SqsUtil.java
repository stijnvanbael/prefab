package be.appify.prefab.core.sns;

import be.appify.prefab.core.domain.EventType;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.kafka.UnknownEventTypeException;
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
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utility class for managing SNS topics, SQS queues, and for subscribing to messages with optional dead-letter handling.
 *
 * <p>Topic-to-type mappings and publish-to strategies are stored in the shared {@link EventRegistry}
 * so that a single source of truth is maintained across all messaging platforms.
 */
@Component
@ConditionalOnClass(SnsTemplate.class)
public class SqsUtil implements SqsSubscriptions, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(SqsUtil.class);

    private final String deadLetterQueueName;
    private final Integer maxRetries;
    private final SnsClient snsClient;
    private final SqsAsyncClient sqsClient;
    private final SqsDeserializer sqsDeserializer;
    private final SqsMessageDecoder decoder;
    private final RetryTemplate retryTemplate;
    private final List<ExecutorService> pollingExecutors = new CopyOnWriteArrayList<>();
    private final EventRegistry eventRegistry;

    /**
     * Constructs a new SqsUtil.
     *
     * @param applicationName     the Spring application name
     * @param deadLetterQueueName the dead-letter queue name (optional)
     * @param maxRetries          the maximum number of receive attempts before routing to the DLQ
     * @param minimumBackoff      the minimum backoff time in milliseconds
     * @param maximumBackoff      the maximum backoff time in milliseconds
     * @param backoffMultiplier   the backoff multiplier
     * @param snsClient           the SNS sync client
     * @param sqsClient           the SQS async client
     * @param sqsDeserializer     the SQS deserializer
     * @param eventRegistry       the shared event registry
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
            SqsDeserializer sqsDeserializer,
            EventRegistry eventRegistry
    ) {
        this.deadLetterQueueName = !isEmpty(deadLetterQueueName) ? deadLetterQueueName : applicationName + ".dlt";
        this.maxRetries = maxRetries;
        this.snsClient = snsClient;
        this.sqsClient = sqsClient;
        this.sqsDeserializer = sqsDeserializer;
        this.decoder = new SqsMessageDecoder(sqsDeserializer);
        this.eventRegistry = eventRegistry;
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
        return ensureTopicExists(topicName, topicName.endsWith(".fifo"));
    }

    /**
     * Ensures that the specified SNS topic exists, creating a FIFO topic when requested.
     *
     * @param topicName topic name, optionally ending in {@code .fifo}
     * @param fifo whether the topic should use FIFO delivery
     * @return the topic ARN
     */
    public String ensureTopicExists(String topicName, boolean fifo) {
        var fifoTopic = fifo || topicName.endsWith(".fifo");
        var effectiveTopicName = fifoTopic ? withFifoSuffix(topicName) : topicName;
        try {
            var request = CreateTopicRequest.builder().name(effectiveTopicName);
            if (fifoTopic) {
                request.attributes(Map.of("FifoTopic", "true", "ContentBasedDeduplication", "false"));
            }
            var createResponse = snsClient.createTopic(request.build());
            return createResponse.topicArn();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to create SNS topic [%s], make sure SNS is available".formatted(effectiveTopicName), e);
        }
    }

    /**
     * Subscribes to an SNS topic via an SQS queue, creating the queue if necessary.
     *
     * @param request
     *         the subscription request containing queue name, topic, type, and consumer
     * @param <T>
     *         the type of the messages
     * @return a handle that stops polling the queue; the queue and its subscription to the topic are kept
     */
    @Override
    public <T> SqsSubscription subscribe(SqsSubscriptionRequest<T> request) {
        var fifo = isFifoSubscription(request);
        var topicArn = ensureTopicExists(request.topic(), fifo);
        var queueUrl = ensureQueueExists(request.queueName(), fifo);
        var queueArn = getQueueArn(queueUrl);
        request.visibilityTimeout().ifPresent(timeout -> setVisibilityTimeout(queueUrl, timeout));

        if (request.isUsingDefaultDeadLetterQueueName()) {
            configureDlq(queueUrl, deadLetterQueueName, maxRetries, fifo);
        } else if (request.deadLetterQueueName() != null) {
            configureDlq(queueUrl, request.deadLetterQueueName(), maxRetries, fifo);
        }

        subscribeQueueToTopic(topicArn, queueArn);
        return startPolling(request, queueUrl);
    }

    /**
     * Resolves the topics to which the given event should be dispatched, applying the registered
     * {@link be.appify.prefab.core.annotations.PublishTo} strategy. Delegates to the shared {@link EventRegistry}.
     *
     * @param event the event instance
     * @return the ordered list of target topic names
     * @throws IllegalArgumentException if no topics are registered for the event type
     */
    public List<String> topicsForDispatch(Object event) {
        return eventRegistry.topicsForDispatch(event);
    }

    /**
     * Resolves the SNS topic for a given event type.
     *
     * @param type the event class
     * @return the registered topic name
     * @throws IllegalArgumentException if no topic is registered for the type
     * @throws IllegalStateException    if multiple topics are registered for the type
     */
    public String topicForType(Class<?> type) {
        return eventRegistry.topicForType(type);
    }

    /**
     * Resolves the SNS topic for a given event type if exactly one is registered.
     *
     * @param type the event class
     * @return the registered topic name if found
     * @throws IllegalStateException if multiple topics are registered for the type
     */
    public Optional<String> tryTopicForType(Class<?> type) {
        return eventRegistry.tryTopicForType(type);
    }

    /**
     * Checks registration without requiring the event to have exactly one topic.
     *
     * @param type event class
     * @return whether any binding of the event is registered
     */
    public boolean hasTopicForType(Class<?> type) {
        return eventRegistry.hasTopicForType(type);
    }

    /**
     * Checks whether a concrete binding is registered.
     *
     * @param type concrete event binding
     * @return whether the binding has registered topics
     */
    public boolean hasTopicForType(EventType<?> type) {
        return !eventRegistry.topicsForType(type).isEmpty();
    }

    /**
     * Checks whether a partitioning key is registered for the event.
     *
     * @param event event instance
     * @return whether the event is configured for FIFO delivery
     */
    public boolean hasPartitioningKey(Object event) {
        return eventRegistry.hasPartitioningKey(event);
    }

    /**
     * Returns the partitioning key for the given event, when one is registered.
     *
     * @param event event instance
     * @return the partitioning key, or empty when no key extractor is registered
     */
    public Optional<String> keyFor(Object event) {
        return eventRegistry.keyFor(event);
    }

    private String ensureQueueExists(String queueName, boolean fifo) {
        var fifoQueue = fifo || queueName.endsWith(".fifo");
        var effectiveQueueName = fifoQueue ? withFifoSuffix(queueName) : queueName;
        var queueStem = fifoQueue
                ? effectiveQueueName.substring(0, effectiveQueueName.length() - ".fifo".length())
                : effectiveQueueName;
        var sanitizedName = queueStem.replaceAll("[^a-zA-Z0-9_-]", "-") + (fifoQueue ? ".fifo" : "");
        if (!sanitizedName.equals(effectiveQueueName)) {
            log.warn("SQS queue name [{}] contains invalid characters and was sanitized to [{}]", queueName,
                    sanitizedName);
        }
        try {
            var request = CreateQueueRequest.builder().queueName(sanitizedName);
            if (fifoQueue) {
                request.attributes(Map.of(QueueAttributeName.FIFO_QUEUE, "true"));
            }
            return sqsClient.createQueue(request.build()).get().queueUrl();
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

    private void configureDlq(String queueUrl, String dlqName, Integer maxReceiveCount, boolean fifo) {
        var dlqUrl = ensureQueueExists(dlqName, fifo);
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

    private void setVisibilityTimeout(String queueUrl, Duration timeout) {
        try {
            sqsClient.setQueueAttributes(
                    SetQueueAttributesRequest.builder()
                            .queueUrl(queueUrl)
                            .attributes(Map.of(QueueAttributeName.VISIBILITY_TIMEOUT,
                                    Long.toString(Math.max(1, timeout.toSeconds()))))
                            .build()
            ).get();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to set the visibility timeout of queue [%s]".formatted(queueUrl), e);
        }
    }

    private boolean isFifoSubscription(SqsSubscriptionRequest<?> request) {
        return request.fifo()
                || request.topic().endsWith(".fifo")
                || request.eventType()
                        .map(eventRegistry::hasPartitioningKey)
                        .orElseGet(() -> eventRegistry.hasPartitioningKey(request.type()));
    }

    private static String withFifoSuffix(String name) {
        return name.endsWith(".fifo") ? name : name + ".fifo";
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

    private <T> SqsSubscription startPolling(SqsSubscriptionRequest<T> request, String queueUrl) {
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
                                    .messageSystemAttributeNames(MessageSystemAttributeName.ALL)
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
        return () -> {
            pollingExecutor.shutdownNow();
            pollingExecutors.remove(pollingExecutor);
        };
    }

    <T> void processMessage(SqsSubscriptionRequest<T> request, String queueUrl, Message message) {
        var metadata = request.messageConsumer().map(ignored -> metadataOf(message));
        var tombstone = metadata.filter(SqsMessageMetadata::tombstone).isPresent();
        Optional<T> decoded;
        try {
            decoded = tombstone ? Optional.empty() : decoder.decode(request, message.body());
        } catch (UnknownEventTypeException e) {
            log.warn("Ignoring SQS message with unknown event type [{}] on topic [{}]",
                    e.eventTypeName(), request.topic());
            deleteMessage(queueUrl, message);
            return;
        }
        if (!tombstone && decoded.isEmpty()) {
            log.debug("Skipping SQS message for another concrete binding on topic [{}]", request.topic());
            deleteMessage(queueUrl, message);
            return;
        }
        // A tombstone carries no payload: message consumers receive null together with its metadata.
        var event = decoded.orElse(null);
        try {
            request.retryTemplate().orElse(retryTemplate).execute(() -> {
                try {
                    if (metadata.isPresent()) {
                        request.messageConsumer().orElseThrow()
                                .accept(event, metadata.get(), () -> deleteMessage(queueUrl, message));
                    } else {
                        request.consumer().accept(event);
                        deleteMessage(queueUrl, message);
                    }
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

    private SqsMessageMetadata metadataOf(Message message) {
        var systemAttributes = message.attributes();
        var sentTimestamp = Optional.ofNullable(systemAttributes.get(MessageSystemAttributeName.SENT_TIMESTAMP))
                .map(millis -> Instant.ofEpochMilli(Long.parseLong(millis)))
                .orElseGet(Instant::now);
        var groupId = Optional.ofNullable(systemAttributes.get(MessageSystemAttributeName.MESSAGE_GROUP_ID));
        return new SqsMessageMetadata(
                message.messageId(),
                sqsDeserializer.parseNotification(message.body()).attributes(),
                sentTimestamp,
                groupId);
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
