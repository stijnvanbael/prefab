package be.appify.prefab.test;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Test container configuration properties for Prefab.
 *
 * <p>Allows customization of Docker container names for all test infrastructure containers.
 * Each container type can be assigned a fixed, predictable name for debugging and identification.
 */
@ConfigurationProperties(prefix = "prefab.test")
public record PrefabTestContainerProperties(
        Postgres postgres,
        Localstack localstack,
        Pubsub pubsub,
        Kafka kafka) {

    /**
     * Postgres test container properties.
     */
    public record Postgres(
            String containerName) {

        public Postgres() {
            this(null);
        }
    }

    /**
     * LocalStack test container properties.
     */
    public record Localstack(
            String containerName) {

        public Localstack() {
            this(null);
        }
    }

    /**
     * Pub/Sub emulator test container properties.
     */
    public record Pubsub(
            String containerName) {

        public Pubsub() {
            this(null);
        }
    }

    /**
     * Kafka and Schema Registry test container properties.
     */
    public record Kafka(
            String containerName,
            SchemaRegistry schemaRegistry) {

        public Kafka() {
            this(null, new SchemaRegistry());
        }

        public Kafka(String containerName) {
            this(containerName, new SchemaRegistry());
        }

        /**
         * Schema Registry test container properties.
         */
        public record SchemaRegistry(
                String containerName) {

            public SchemaRegistry() {
                this(null);
            }
        }
    }

    public PrefabTestContainerProperties() {
        this(new Postgres(), new Localstack(), new Pubsub(), new Kafka());
    }
}

