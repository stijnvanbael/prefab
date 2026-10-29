package be.appify.prefab.core.kafka;

import be.appify.prefab.core.annotations.Event;
import be.appify.prefab.core.annotations.EventMigration;
import be.appify.prefab.core.versioning.EventVersions;
import be.appify.prefab.core.versioning.Migration;
import be.appify.prefab.core.versioning.EventMigrator;
import be.appify.prefab.core.versioning.UnsupportedEventVersionException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.core.convert.support.GenericConversionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DynamicDeserializerVersioningTest {

    @Event(topic = "users", version = "2.1.0")
    record UserCreated(String fullName) {
    }

    record UserCreatedV1(String firstName, String lastName) {
    }

    @EventMigration(from = "1", to = "2")
    static class Migrate implements Migration<UserCreatedV1, UserCreated> {
        @Override
        public UserCreated migrate(UserCreatedV1 old) {
            return new UserCreated(old.firstName() + " " + old.lastName());
        }
    }

    private final EventMigrator migrator = new EventMigrator(List.of(new Migrate()));

    private DynamicDeserializer deserializer(Event.Serialization serialization, GenericConversionService conversion) {
        var registry = new EventRegistry();
        registry.register("users", UserCreated.class, serialization);
        var properties = new KafkaProperties();
        KafkaSchemaRegistrySupport.enableMockSchemaRegistry(properties);
        return new DynamicDeserializer(properties, conversion, registry, migrator);
    }

    private static RecordHeaders version(String version) {
        var headers = new RecordHeaders();
        if (version != null) {
            headers.add(EventVersions.EVENT_VERSION_HEADER, version.getBytes(StandardCharsets.UTF_8));
        }
        return headers;
    }

    private static byte[] json(String json) {
        return json.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void migratesJsonMessagesOfAnOlderMajor() {
        var result = deserializer(Event.Serialization.JSON, new GenericConversionService()).deserialize("users",
                version("1.3.0"), json("{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\"}"));

        assertThat(result).isEqualTo(new UserCreated("Ada Lovelace"));
    }

    @Test
    void readsJsonMessagesOfAHigherMinorAndIgnoresUnknownFields() {
        var result = deserializer(Event.Serialization.JSON, new GenericConversionService()).deserialize("users",
                version("2.9.0"), json("{\"fullName\":\"Ada\",\"newField\":1}"));

        assertThat(result).isEqualTo(new UserCreated("Ada"));
    }

    @Test
    void readsJsonMessagesWithoutHeaderAsVersionOne() {
        var result = deserializer(Event.Serialization.JSON, new GenericConversionService()).deserialize("users",
                version(null), json("{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\"}"));

        assertThat(result).isEqualTo(new UserCreated("Ada Lovelace"));
    }

    @Test
    void rejectsJsonMessagesOfAHigherMajor() {
        var deserializer = deserializer(Event.Serialization.JSON, new GenericConversionService());

        assertThatThrownBy(() -> deserializer.deserialize("users", version("3.0.0"), json("{}")))
                .isInstanceOf(UnsupportedEventVersionException.class);
    }

    @Test
    void migratesAvroMessagesOfAnOlderMajor() throws Exception {
        var schema = new Schema.Parser().parse("""
                {"type":"record","name":"DynamicDeserializerVersioningTest_UserCreated",
                 "namespace":"be.appify.prefab.core.kafka",
                 "fields":[{"name":"firstName","type":"string"},{"name":"lastName","type":"string"}]}""");
        var record = new GenericData.Record(schema);
        record.put("firstName", "Ada");
        record.put("lastName", "Lovelace");
        var deserializer = deserializer(Event.Serialization.AVRO, new GenericConversionService());
        var toEvent = DynamicDeserializer.class.getDeclaredMethod("toEvent", String.class, GenericRecord.class,
                be.appify.prefab.core.versioning.MessageVersions.class);
        toEvent.setAccessible(true);

        var result = toEvent.invoke(deserializer, "users", record,
                be.appify.prefab.core.versioning.MessageVersions.from(name -> "prefab-event-version".equals(name) ? "1.0.0" : null));

        assertThat(result).isEqualTo(new UserCreated("Ada Lovelace"));
    }

    @Test
    void serializerStampsTheVersionHeader() {
        var registry = new EventRegistry();
        registry.register("users", UserCreated.class, Event.Serialization.JSON);
        var serializer = new DynamicSerializer(new KafkaProperties(), new GenericConversionService(), registry);
        var headers = new RecordHeaders();

        serializer.serialize("users", headers, new UserCreated("Ada"));

        assertThat(new String(headers.lastHeader(EventVersions.EVENT_VERSION_HEADER).value(), StandardCharsets.UTF_8))
                .isEqualTo("2.1.0");
    }

    @Test
    void serializerDoesNotStampKeys() {
        var registry = new EventRegistry();
        var serializer = new DynamicSerializer(new KafkaProperties(), new GenericConversionService(), registry);
        var headers = new RecordHeaders();

        serializer.serialize("users", headers, "key");

        assertThat(headers.toArray()).isEmpty();
    }
}
