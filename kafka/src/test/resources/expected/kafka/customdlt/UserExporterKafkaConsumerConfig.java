package kafka.customdlt.infrastructure.kafka;

import be.appify.prefab.core.kafka.KafkaUtil;
import be.appify.prefab.core.util.Classes;
import com.google.common.collect.Streams;
import java.util.List;
import java.util.Map;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@Configuration
public class UserExporterKafkaConsumerConfig {
    @Bean
    @Qualifier("userExporterKafkaErrorHandler")
    CommonErrorHandler userExporterKafkaErrorHandler(
            @Value("${prefab.dlt.retries.backoff-multiplier:1.5}") Double backoffMultiplier,
            @Value("#{'${prefab.dlt.non-retryable-exceptions:}'.split(',')}") List<String> nonRetryableExceptions,
            @Qualifier("userExporterDeadLetterPublishingRecoverer") DeadLetterPublishingRecoverer deadLetteringRecoverer) {
        var backoff = new ExponentialBackOffWithMaxRetries(10);
        backoff.setInitialInterval(100L);
        backoff.setMultiplier(backoffMultiplier);
        backoff.setMaxInterval(10000L);
        var errorHandler = new DefaultErrorHandler(deadLetteringRecoverer, backoff);
        var customExceptions = nonRetryableExceptions.stream()
                .filter(name -> !name.isBlank())
                .map(Classes::classWithName);
        var notRetryable = Streams.concat(KafkaUtil.DEFAULT_NOT_RETRYABLE.stream(), customExceptions)
                .toArray(Class[]::new);
        errorHandler.addNotRetryableExceptions(notRetryable);
        return errorHandler;
    }

    @Bean
    @Qualifier("userExporterDeadLetterPublishingRecoverer")
    DeadLetterPublishingRecoverer userExporterDeadLetterPublishingRecoverer(
            KafkaTemplate<?, ?> kafkaTemplate) {
        return new DeadLetterPublishingRecoverer(
                    Map.of(Object.class, kafkaTemplate),
                    (record, ex) -> new TopicPartition("${custom.dlt.name}", -1));
    }
}
