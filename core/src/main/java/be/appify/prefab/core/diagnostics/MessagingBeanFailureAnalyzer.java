package be.appify.prefab.core.diagnostics;

import java.util.Map;

/** Explains missing Kafka, Pub/Sub and SNS/SQS beans. */
class MessagingBeanFailureAnalyzer extends MissingBeanFailureAnalyzer {

    private static final FailureReport PUB_SUB = new FailureReport(
            "Prefab needs a Google Cloud Pub/Sub bean, but Spring Cloud GCP did not create it.",
            "Check that spring.cloud.gcp.pubsub.enabled is not false, that spring.cloud.gcp.project-id is set and "
                    + "that credentials are available (spring.cloud.gcp.credentials.*). Against the emulator, set "
                    + "spring.cloud.gcp.pubsub.emulator-host.");

    private static final FailureReport SNS = new FailureReport(
            "Prefab needs an SNS client, but Spring Cloud AWS did not create it.",
            "Add spring-cloud-aws-starter-sns to the dependencies and set spring.cloud.aws.region.static "
                    + "and the credentials. Against LocalStack, also set spring.cloud.aws.sns.endpoint.");

    MessagingBeanFailureAnalyzer() {
        super(Map.of(
                "org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails", new FailureReport(
                        "Prefab needs Kafka connection details, but Spring Boot did not create them.",
                        "Add spring-boot-starter-kafka to the dependencies, and make sure Kafka auto-configuration "
                                + "is not excluded. Set spring.kafka.bootstrap-servers to your brokers."),
                "com.google.cloud.spring.pubsub.PubSubAdmin", PUB_SUB,
                "com.google.cloud.spring.pubsub.core.PubSubTemplate", PUB_SUB,
                "com.google.cloud.spring.pubsub.core.subscriber.PubSubSubscriberTemplate", PUB_SUB,
                "io.awspring.cloud.sns.core.SnsTemplate", SNS,
                "software.amazon.awssdk.services.sns.SnsClient", SNS,
                "software.amazon.awssdk.services.sqs.SqsAsyncClient", new FailureReport(
                        "Prefab needs an SQS client, but Spring Cloud AWS did not create it.",
                        "Add spring-cloud-aws-starter-sqs to the dependencies and set spring.cloud.aws.region.static "
                                + "and the credentials. Against LocalStack, also set spring.cloud.aws.sqs.endpoint.")));
    }
}
