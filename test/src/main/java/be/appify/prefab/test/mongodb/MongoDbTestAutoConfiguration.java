package be.appify.prefab.test.mongodb;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Autoconfiguration for MongoDB test support.
 * <p>
 * When {@code spring-boot-starter-data-mongodb} is on the classpath (i.e., when {@code prefab-mongodb} is a
 * dependency), this configuration automatically starts a MongoDB Testcontainer and wires it up via
 * {@link ServiceConnection}. Tests no longer need to import a custom {@code MongoDbContainerConfiguration}.
 * </p>
 */
@TestConfiguration(proxyBeanMethods = false)
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnClass(MongoTemplate.class)
public class MongoDbTestAutoConfiguration {

    /**
     * Constructs a new MongoDbTestAutoConfiguration.
     */
    public MongoDbTestAutoConfiguration() {
    }

    /**
     * Creates and starts a MongoDB test container. The {@link ServiceConnection} annotation allows Spring Boot to
     * automatically configure the MongoDB connection details.
     *
     * @return a running MongoDB container
     */
    @Bean
    @ServiceConnection
    @ConditionalOnMissingBean(MongoDBContainer.class)
    MongoDBContainer mongoDBContainer() {
        return new MongoDBContainer(DockerImageName.parse("mongo:7"));
    }
}
