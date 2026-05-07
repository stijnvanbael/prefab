package kafka.single.infrastructure.kafka;
import be.appify.prefab.core.kafka.KafkaJsonTypeResolver;
import kafka.single.UserCreated;
import org.springframework.stereotype.Component;
@Component
public class UserCreatedKafkaEventTypeRegistrar {
    public UserCreatedKafkaEventTypeRegistrar(KafkaJsonTypeResolver typeResolver) {
        typeResolver.registerType("prefab.user", UserCreated.class);
    }
}
