package kafka.dltdisabled.infrastructure.kafka;

import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@Configuration
public class UserExporterKafkaConsumerConfig {
    @Bean
    @Qualifier("userExporterKafkaErrorHandler")
    CommonErrorHandler userExporterKafkaErrorHandler(
            @Value("${prefab.dlt.retries.limit:5}") Integer maxRetries,
            @Value("${prefab.dlt.retries.minimum-backoff-ms:1000}") Long initialRetryInterval,
            @Value("${prefab.dlt.retries.maximum-backoff-ms:30000}") Long maxRetryInterval,
            @Value("${prefab.dlt.retries.backoff-multiplier:1.5}") Double backoffMultiplier,
            @Value("#{'${prefab.dlt.non-retryable-exceptions:}'.split(',')}") List<String> nonRetryableExceptions) {
        var backoff = new ExponentialBackOffWithMaxRetries(maxRetries);
        backoff.setInitialInterval(initialRetryInterval);
        backoff.setMultiplier(backoffMultiplier);
        backoff.setMaxInterval(maxRetryInterval);
        return new DefaultErrorHandler(backoff);
    }
}
