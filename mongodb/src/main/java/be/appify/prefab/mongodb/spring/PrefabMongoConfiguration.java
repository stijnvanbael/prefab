package be.appify.prefab.mongodb.spring;

import be.appify.prefab.mongodb.spring.data.mongodb.ReferenceToStringConverter;
import be.appify.prefab.mongodb.spring.data.mongodb.StringToReferenceConverter;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

/**
 * Spring auto-configuration for Prefab MongoDB persistence support.
 * <p>
 * Add {@code prefab-mongodb} as a Maven dependency to enable MongoDB-backed repositories. This configuration is
 * automatically applied via Spring Boot's auto-configuration mechanism and registers the converters needed to
 * store {@link be.appify.prefab.core.service.Reference} values as plain strings in MongoDB documents.
 * </p>
 */
@Configuration
public class PrefabMongoConfiguration {

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
}
