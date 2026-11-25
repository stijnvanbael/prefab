package be.appify.prefab.example.showcase.streams.kafka;

import be.appify.prefab.example.showcase.streams.OrderRoute;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.StreamDefinition;
import be.appify.prefab.streams.kafka.KafkaStreamBackend;
import be.appify.prefab.streams.kafka.KafkaStreamBreakoutAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Demonstrates native Kafka key routing without loading on other Prefab Streams backends. Breakout is not supported by
 * the in-process engine, so the topology is left out when tests run in memory ({@code prefab.test.containers=false}).
 */
@Configuration
@ConditionalOnClass(KafkaStreamBackend.class)
@ConditionalOnProperty(name = "prefab.test.containers", havingValue = "true", matchIfMissing = true)
class KafkaOrderTypeBreakoutConfiguration {
    @Bean
    StreamDefinition kafkaOrderTypeBreakout(PrefabStreams streams) {
        return streams.from(OrderRoute.class)
                .breakout(new KafkaStreamBreakoutAdapter<>(
                        nativeStream -> nativeStream.selectKey((key, route) -> route.orderType())
                                .mapValues((key, route) -> new KafkaOrderTypeRoute(key, route.order()))))
                .to(KafkaOrderTypeRoute.class);
    }
}
