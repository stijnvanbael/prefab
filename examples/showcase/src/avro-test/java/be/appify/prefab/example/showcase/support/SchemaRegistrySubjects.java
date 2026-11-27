package be.appify.prefab.example.showcase.support;

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry;
import java.util.List;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;

/**
 * Lists subjects from the schema registry used by a showcase test.
 *
 * <p>In-memory tests use Confluent's shared mock scope, while container tests expose the registry over HTTP.
 */
public final class SchemaRegistrySubjects {

    private static final String MOCK_SCHEMA_REGISTRY_SCOPE = "schema-url";
    private static final int CLIENT_CACHE_CAPACITY = 100;

    private SchemaRegistrySubjects() {
    }

    /**
     * Lists subjects from the active producer schema registry.
     *
     * @param kafkaProperties the active Kafka properties
     * @return the registered schema subjects
     * @throws Exception when the registry cannot be queried
     */
    public static List<String> list(KafkaProperties kafkaProperties) throws Exception {
        var schemaRegistryUrl = kafkaProperties.getProducer().getProperties().get("schema.registry.url");
        var client = schemaRegistryUrl == null || schemaRegistryUrl.toString().isBlank()
                ? mockClient()
                : new CachedSchemaRegistryClient(schemaRegistryUrl.toString(), CLIENT_CACHE_CAPACITY);
        return List.copyOf(client.getAllSubjects());
    }

    private static SchemaRegistryClient mockClient() {
        return MockSchemaRegistry.getClientForScope(MOCK_SCHEMA_REGISTRY_SCOPE);
    }
}
