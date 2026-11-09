package be.appify.prefab.test.mongodb;

import be.appify.prefab.core.spring.PrefabCoreConfiguration;
import be.appify.prefab.test.TestContainerNameResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.testcontainers.service.connection.ServiceConnectionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.PropertyResolver;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Optional;

/**
 * Autoconfiguration for MongoDB test support.
 * <p>
 * When {@code spring-boot-starter-data-mongodb} is on the classpath (i.e., when {@code prefab-mongodb} is a
 * dependency), this configuration automatically starts a MongoDB Testcontainer and wires it up via
 * {@link ServiceConnection}. Tests no longer need to import a custom {@code MongoDbContainerConfiguration}.
 * </p>
 */
@AutoConfiguration(before = ServiceConnectionAutoConfiguration.class)
@ConditionalOnClass(MongoTemplate.class)
@ConditionalOnBean(PrefabCoreConfiguration.class)
public class MongoDbTestAutoConfiguration {
    private static final String MONGO_IMAGE = "mongo:9";

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
    MongoDBContainer mongoDBContainer(PropertyResolver propertyResolver) {
        var containerName = TestContainerNameResolver.resolveSharedContainerName(
                propertyResolver, "mongodb", "prefab.test.mongodb.container-name");
        TestContainerNameResolver.removeStaleContainer(containerName, MONGO_IMAGE);
        return new MongoDBContainer(DockerImageName.parse(MONGO_IMAGE))
                .withReuse(true)
                .withCreateContainerCmdModifier(cmd -> cmd.withName(containerName));
    }

    /**
     * Gives every application its own database on the shared container unless one is configured explicitly.
     *
     * @param propertyResolver the property resolver to fetch the application name
     * @return a registrar defaulting {@code spring.data.mongodb.database} to the application name
     */
    @Bean
    DynamicPropertyRegistrar mongoDatabaseRegistrar(PropertyResolver propertyResolver) {
        return registry -> registry.add("spring.data.mongodb.database", () ->
                Optional.ofNullable(propertyResolver.getProperty("spring.data.mongodb.database"))
                        .orElseGet(() -> TestContainerNameResolver.resolveApplicationIdentifier(propertyResolver)));
    }
}
