package be.appify.prefab.core.sns;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.EventMigration;
import be.appify.prefab.core.kafka.EventRegistry;
import be.appify.prefab.core.spring.JsonUtil;
import be.appify.prefab.core.versioning.EventMigrator;
import be.appify.prefab.core.versioning.Migration;
import be.appify.prefab.core.versioning.UnsupportedEventVersionException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.support.GenericConversionService;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SqsDeserializerVersioningTest {

    @Event(topic = "users", version = "2.0.0")
    record UserCreated(String fullName) {
    }

    record UserCreatedV1(String name) {
    }

    @EventMigration(from = "1", to = "2")
    static class Migrate implements Migration<UserCreatedV1, UserCreated> {
        @Override
        public UserCreated migrate(UserCreatedV1 old) {
            return new UserCreated(old.name().toUpperCase());
        }
    }

    private final JsonMapper mapper = JsonMapper.builder().build();
    private final EventRegistry registry = new EventRegistry();
    private final SqsDeserializer deserializer = new SqsDeserializer(new JsonUtil(mapper), registry,
            new GenericConversionService(), new EventMigrator(List.of(new Migrate())), mapper);

    private String envelope(String payload, String version) {
        return mapper.writeValueAsString(java.util.Map.of(
                "Message", payload,
                "Subject", UserCreated.class.getName(),
                "MessageAttributes", java.util.Map.of("prefab-event-version", java.util.Map.of("Type", "String", "Value", version))));
    }

    @Test
    void migratesMessagesOfAnOlderMajor() {
        registry.register("users", UserCreated.class, Event.Serialization.JSON);

        var event = deserializer.deserialize("users", envelope("{\"name\":\"ada\"}", "1.0.0"), UserCreated.class);

        assertThat(event).isEqualTo(new UserCreated("ADA"));
    }

    @Test
    void readsMessagesWithoutVersionAttributeAsInitialVersion() {
        var body = "{\"Message\":\"{\\\"name\\\":\\\"ada\\\"}\",\"Subject\":\"%s\"}".formatted(UserCreated.class.getName());
        registry.register("users", UserCreated.class, Event.Serialization.JSON);

        assertThat(deserializer.deserialize("users", body, UserCreated.class)).isEqualTo(new UserCreated("ADA"));
    }

    @Test
    void rejectsMessagesOfAHigherMajor() {
        registry.register("users", UserCreated.class, Event.Serialization.JSON);

        assertThatThrownBy(() -> deserializer.deserialize("users", envelope("{}", "3.0.0"), UserCreated.class))
                .isInstanceOf(UnsupportedEventVersionException.class);
    }
}
