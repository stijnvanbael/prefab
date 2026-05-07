package kafka.multitopic.infrastructure.kafka;
import be.appify.prefab.core.kafka.KafkaJsonTypeResolver;
import kafka.multitopic.Refund;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class RefundCreatedKafkaEventTypeRegistrar {
    public RefundCreatedKafkaEventTypeRegistrar(KafkaJsonTypeResolver typeResolver,
            @Value("${topic.refund.name}") String refundCreatedTopic) {
        typeResolver.registerType(refundCreatedTopic, Refund.Created.class);
    }
}
