package be.appify.prefab.example.showcase.streams.kafka;

import be.appify.prefab.example.showcase.streams.OrderRoute;
import be.appify.prefab.streams.PrefabStreams;
import be.appify.prefab.streams.StreamDefinition;
import be.appify.prefab.streams.kafka.KafkaStreamBackend;
import be.appify.prefab.streams.kafka.KafkaStreamBreakoutAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Demonstrates native Kafka key routing without loading on other Prefab Streams backends. */
@Configuration
@ConditionalOnClass(KafkaStreamBackend.class)
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
