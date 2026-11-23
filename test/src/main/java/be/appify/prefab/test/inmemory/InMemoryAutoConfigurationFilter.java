package be.appify.prefab.test.inmemory;

import java.util.Set;
import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter;
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

/**
 * Drops the auto-configurations that connect to real stores and brokers when tests run in memory.
 *
 * @see TestInfrastructureMode
 */
public class InMemoryAutoConfigurationFilter implements AutoConfigurationImportFilter, EnvironmentAware {

    /**
     * Auto-configurations that are excluded in in-memory mode. {@code PrefabRegistryConfiguration} is deliberately
     * absent: it has no transport dependency and is still needed.
     */
    static final Set<String> EXCLUDED = Set.of(
            // Spring Boot: DataSource, JDBC, Flyway, Data JDBC
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
            "org.springframework.boot.jdbc.autoconfigure.DataSourceInitializationAutoConfiguration",
            "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration",
            "org.springframework.boot.jdbc.autoconfigure.JdbcClientAutoConfiguration",
            "org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration",
            "org.springframework.boot.jdbc.autoconfigure.JndiDataSourceAutoConfiguration",
            "org.springframework.boot.jdbc.autoconfigure.XADataSourceAutoConfiguration",
            "org.springframework.boot.jdbc.autoconfigure.health.DataSourceHealthContributorAutoConfiguration",
            "org.springframework.boot.jdbc.autoconfigure.metrics.DataSourcePoolMetricsAutoConfiguration",
            "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
            "org.springframework.boot.flyway.autoconfigure.FlywayEndpointAutoConfiguration",
            "org.springframework.boot.data.jdbc.autoconfigure.DataJdbcRepositoriesAutoConfiguration",
            // Spring Boot: MongoDB, Data MongoDB
            "org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration",
            "org.springframework.boot.mongodb.autoconfigure.MongoReactiveAutoConfiguration",
            "org.springframework.boot.mongodb.autoconfigure.health.MongoHealthContributorAutoConfiguration",
            "org.springframework.boot.mongodb.autoconfigure.health.MongoReactiveHealthContributorAutoConfiguration",
            "org.springframework.boot.mongodb.autoconfigure.metrics.MongoMetricsAutoConfiguration",
            "org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration",
            "org.springframework.boot.data.mongodb.autoconfigure.DataMongoReactiveAutoConfiguration",
            "org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration",
            "org.springframework.boot.data.mongodb.autoconfigure.DataMongoReactiveRepositoriesAutoConfiguration",
            // Spring Boot: Kafka
            "org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration",
            "org.springframework.boot.kafka.autoconfigure.metrics.KafkaMetricsAutoConfiguration",
            // Spring Cloud GCP: Pub/Sub
            "com.google.cloud.spring.autoconfigure.pubsub.GcpPubSubAutoConfiguration",
            "com.google.cloud.spring.autoconfigure.pubsub.GcpPubSubEmulatorAutoConfiguration",
            "com.google.cloud.spring.autoconfigure.pubsub.GcpPubSubReactiveAutoConfiguration",
            "com.google.cloud.spring.autoconfigure.pubsub.health.PubSubHealthIndicatorAutoConfiguration",
            "com.google.cloud.spring.autoconfigure.pubsub.health.PubSubSubscriptionHealthIndicatorAutoConfiguration",
            // Spring Cloud AWS: SNS, SQS
            "io.awspring.cloud.autoconfigure.sns.SnsAutoConfiguration",
            "io.awspring.cloud.autoconfigure.sqs.SqsAutoConfiguration",
            // Prefab transports
            "be.appify.prefab.postgres.spring.PrefabJdbcConfiguration",
            "be.appify.prefab.mongodb.spring.PrefabMongoConfiguration",
            "be.appify.prefab.core.kafka.KafkaConfiguration",
            "be.appify.prefab.core.pubsub.PubSubConfiguration",
            "be.appify.prefab.core.sns.SnsConfiguration",
            // prefab-test container auto-configurations
            "be.appify.prefab.test.persistence.PostgresTestAutoConfiguration",
            "be.appify.prefab.test.kafka.KafkaTestcontainerAutoConfiguration",
            "be.appify.prefab.test.kafka.KafkaTestAutoConfiguration",
            "be.appify.prefab.test.pubsub.PubSubTestAutoConfiguration",
            "be.appify.prefab.test.sns.SnsTestAutoConfiguration",
            "be.appify.prefab.test.mongodb.MongoDbTestAutoConfiguration");

    private boolean inMemory;

    /** Constructs a new InMemoryAutoConfigurationFilter. */
    public InMemoryAutoConfigurationFilter() {
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.inMemory = TestInfrastructureMode.inMemory(environment);
    }

    @Override
    public boolean[] match(String[] autoConfigurationClasses, AutoConfigurationMetadata autoConfigurationMetadata) {
        var matches = new boolean[autoConfigurationClasses.length];
        for (var i = 0; i < matches.length; i++) {
            matches[i] = !(inMemory && EXCLUDED.contains(autoConfigurationClasses[i]));
        }
        return matches;
    }
}
