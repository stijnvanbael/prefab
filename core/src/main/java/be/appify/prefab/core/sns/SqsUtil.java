package be.appify.prefab.core.sns;

import be.appify.prefab.core.spring.JsonUtil;
import io.awspring.cloud.sns.core.SnsTemplate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Utility class for managing SNS topics, SQS queues, and for subscribing to messages with optional dead-letter
 * handling.
 */
@Component
@ConditionalOnClass(SnsTemplate.class)
public class SqsUtil {
    private static final Logger log = LoggerFactory.getLogger(SqsUtil.class);

    private final String applicationName;
    private final String deadLetterQueueName;
    private final Integer maxRetries;
    private final SnsClient snsClient;
    private final SqsAsyncClient sqsClient;
    private final JsonUtil jsonUtil;
    private final RetryTemplate retryTemplate;
    private final ConcurrentMap<String, Class<?>> messageTypes = new ConcurrentHashMap<>();

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
     * @param jsonUtil
     *         the JSON utility
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
            JsonUtil jsonUtil
    ) {
        this.applicationName = applicationName;
        this.deadLetterQueueName = !isEmpty(deadLetterQueueName) ? deadLetterQueueName : applicationName + ".dlt";
        this.maxRetries = maxRetries;
        this.snsClient = snsClient;
        this.sqsClient = sqsClient;
        this.jsonUtil = jsonUtil;
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

    private String ensureQueueExists(String queueName) {
        var sanitizedName = queueName.replaceAll("[^a-zA-Z0-9_-]", "-");
        if (!sanitizedName.equals(queueName)) {
            log.warn("SQS queue name [{}] contains invalid characters and was sanitized to [{}]", queueName, sanitizedName);
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
                    var payload = extractPayload(message);
                    var typeName = extractTypeName(message);
                    T event = deserialize(payload, typeName, request.type());
                    request.consumer().accept(event);
                    deleteMessage(queueUrl, message);
                } catch (Exception e) {
                    log.warn("Error processing SQS message: {}, cause: {}", message.body(),
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

    @SuppressWarnings("unchecked")
    private String extractPayload(Message message) {
        try {
            var body = (Map<String, Object>) jsonUtil.parseJson(message.body(), Map.class);
            if (body.containsKey("Message")) {
                return (String) body.get("Message");
            }
            return message.body();
        } catch (Exception e) {
            return message.body();
        }
    }

    @SuppressWarnings("unchecked")
    private String extractTypeName(Message message) {
        try {
            var body = (Map<String, Object>) jsonUtil.parseJson(message.body(), Map.class);
            if (body.containsKey("Subject")) {
                return (String) body.get("Subject");
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> T deserialize(String payload, String typeName, Class<T> type) {
        if (typeName != null) {
            var consumedType = messageTypes.computeIfAbsent(typeName, key -> {
                try {
                    return Class.forName(typeName);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Could not find class for type: " + typeName, e);
                }
            });
            if (type.isAssignableFrom(consumedType)) {
                return (T) jsonUtil.parseJson(payload, consumedType);
            }
        }
        return jsonUtil.parseJson(payload, type);
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
     * Gets the application name.
     *
     * @return the application name
     */
    public String applicationName() {
        return applicationName;
    }
}
