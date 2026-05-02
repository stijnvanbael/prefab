package be.appify.prefab.mongodb.spring;

import be.appify.prefab.mongodb.spring.data.mongodb.PrefabMongoMappingContext;
import be.appify.prefab.mongodb.spring.data.mongodb.PrefabMongoTemplate;
import be.appify.prefab.mongodb.spring.data.mongodb.ReferenceToStringConverter;
import be.appify.prefab.mongodb.spring.data.mongodb.StringToReferenceConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring auto-configuration for Prefab MongoDB persistence support.
 * <p>
 * Add {@code prefab-mongodb} as a Maven dependency to enable MongoDB-backed repositories. This configuration is
 * automatically applied via Spring Boot's auto-configuration mechanism and registers the converters needed to
 * store {@link be.appify.prefab.core.service.Reference} values as plain strings in MongoDB documents.
 * </p>
 * <p>
 * It also replaces the default {@link MongoMappingContext} with {@link PrefabMongoMappingContext}, which adds
 * support for sealed interfaces annotated with {@link be.appify.prefab.core.annotations.Aggregate} as MongoDB
 * entity types (e.g. polymorphic aggregate roots).
 * </p>
 */
@Configuration
@ComponentScan("be.appify.prefab.mongodb.spring.data.mongodb")
public class PrefabMongoConfiguration {

    @Autowired
    private JsonMapper jsonMapper;

    /**
     * Constructs a new PrefabMongoConfiguration.
     */
    public PrefabMongoConfiguration() {
    }

    /**
     * Provides custom MongoDB type conversions that allow Prefab single-value types (such as
     * {@link be.appify.prefab.core.service.Reference}) to be stored as plain scalar values in MongoDB documents
     * rather than sub-documents.
     *
     * @return the custom MongoDB conversions
     */
    @Bean
    @ConditionalOnMissingBean
    public MongoCustomConversions mongoCustomConversions() {
        return MongoCustomConversions.create(adapter -> {
            adapter.registerConverter(new ReferenceToStringConverter());
            adapter.registerConverter(new StringToReferenceConverter());
        });
    }

    /**
     * Replaces the default {@link MongoMappingContext} with a Prefab-aware variant that recognises sealed
     * interfaces annotated with {@link be.appify.prefab.core.annotations.Aggregate} as valid MongoDB entity
     * types, enabling polymorphic aggregate repositories backed by a single MongoDB collection.
     *
     * @return the Prefab-customised MongoDB mapping context
     */
    @Bean
    @ConditionalOnMissingBean
    public MongoMappingContext mongoMappingContext() {
        return new PrefabMongoMappingContext();
    }

    /**
     * Provides a {@link MappingMongoConverter} that registers {@link java.time.Instant} and other
     * {@code java.time} types as simple types, preventing Spring Data from attempting to reflectively
     * construct them via private constructors (which fails under the Java module system).
     *
     * @param mongoDatabaseFactory
     *         the factory used to resolve database references
     * @param conversions
     *         the custom conversions to apply, including Prefab converters
     * @param mappingContext
     *         the mapping context used to map entities to MongoDB documents
     * @return the configured {@link MappingMongoConverter}
     */
    @Bean
    @ConditionalOnMissingBean
    public MappingMongoConverter mappingMongoConverter(
            MongoDatabaseFactory mongoDatabaseFactory,
            MongoCustomConversions conversions,
            MongoMappingContext mappingContext
    ) {
        var dbRefResolver = new DefaultDbRefResolver(mongoDatabaseFactory);
        var converter = new MappingMongoConverter(dbRefResolver, mappingContext);
        converter.setCustomConversions(conversions);
        mappingContext.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
        mappingContext.afterPropertiesSet();
        converter.afterPropertiesSet();
        return converter;
    }

    /**
     * Provides a {@link PrefabMongoTemplate} that flushes domain events to the transactional outbox
     * (or publishes them directly) after every aggregate save.
     *
     * @param mongoDatabaseFactory the factory used to obtain database connections
     * @param converter            the configured MongoDB converter
     * @return the Prefab-customised {@link MongoTemplate}
     */
    @Bean
    @ConditionalOnMissingBean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory, MappingMongoConverter converter) {
        return new PrefabMongoTemplate(mongoDatabaseFactory, converter, jsonMapper);
    }
}
