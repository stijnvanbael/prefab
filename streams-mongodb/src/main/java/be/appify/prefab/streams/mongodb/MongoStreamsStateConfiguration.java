package be.appify.prefab.streams.mongodb;

import be.appify.prefab.streams.engine.JsonStreamStateCodec;
import be.appify.prefab.streams.engine.StreamStateCodec;
import be.appify.prefab.streams.engine.StreamStateRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;

/** Registers the MongoDB {@link StreamStateRepository} when a {@link MongoTemplate} exists. */
@AutoConfiguration(afterName = "org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration")
@ConditionalOnClass(MongoTemplate.class)
@ConditionalOnBean(MongoTemplate.class)
@EnableConfigurationProperties(MongoStreamsProperties.class)
public class MongoStreamsStateConfiguration {

    @Bean
    @ConditionalOnMissingBean(StreamStateCodec.class)
    StreamStateCodec streamStateCodec() {
        return new JsonStreamStateCodec();
    }

    @Bean
    @ConditionalOnMissingBean(StreamStateRepository.class)
    MongoStreamStateRepository mongoStreamStateRepository(
            MongoTemplate template,
            StreamStateCodec codec,
            MongoStreamsProperties properties
    ) {
        var repository = new MongoStreamStateRepository(template, codec);
        if (properties.initializeSchema() == MongoStreamsProperties.InitializeSchema.ALWAYS) {
            MongoStreamSchema.initialize(template);
        }
        return repository;
    }
}
