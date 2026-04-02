package be.appify.prefab.example.mongodb;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Test configuration that starts a MongoDB container and wires it up via {@link ServiceConnection}.
 * <p>
 * This configuration is typically imported directly in tests using
 * {@code @Import(MongoDbContainerConfiguration.class)}.
 * </p>
 */
@TestConfiguration(proxyBeanMethods = false)
public class MongoDbContainerConfiguration {

    /** Constructs a new MongoDbContainerConfiguration. */
    public MongoDbContainerConfiguration() {
    }

    /**
     * Creates and starts a MongoDB test container. The {@link ServiceConnection} annotation
     * allows Spring Boot to automatically configure the MongoDB connection details.
     *
     * @return a running MongoDB container
     */
    @Bean
    @ServiceConnection
    MongoDBContainer mongoDBContainer() {
        return new MongoDBContainer(DockerImageName.parse("mongo:7"));
    }
}
