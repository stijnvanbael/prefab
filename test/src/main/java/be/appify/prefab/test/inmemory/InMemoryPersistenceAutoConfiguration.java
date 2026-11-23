package be.appify.prefab.test.inmemory;

import be.appify.prefab.core.spring.data.FullTextQueryExecutor;
import be.appify.prefab.core.spring.data.RangeQueryExecutor;
import be.appify.prefab.test.inmemory.persistence.AggregateKeySpaceResolver;
import be.appify.prefab.test.inmemory.persistence.AggregateProperties;
import be.appify.prefab.test.inmemory.persistence.ExamplePredicate;
import be.appify.prefab.test.inmemory.persistence.InMemoryFullTextQueryExecutor;
import be.appify.prefab.test.inmemory.persistence.InMemoryMappingContext;
import be.appify.prefab.test.inmemory.persistence.InMemoryRangeQueryExecutor;
import be.appify.prefab.test.inmemory.persistence.InMemoryRepositoriesRegistrar;
import be.appify.prefab.test.inmemory.persistence.InMemoryStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;

/**
 * Backs every repository interface of the application with an in-memory store when {@code prefab.test.containers}
 * is {@code false}, together with the in-memory range and full-text query executors.
 *
 * @see InMemoryRepositoriesRegistrar
 */
@AutoConfiguration
@Conditional(InMemoryTestAutoConfiguration.InMemoryModeCondition.class)
@Import(InMemoryRepositoriesRegistrar.class)
public class InMemoryPersistenceAutoConfiguration {

    /** Constructs a new InMemoryPersistenceAutoConfiguration. */
    public InMemoryPersistenceAutoConfiguration() {
    }

    @Bean
    AggregateKeySpaceResolver inMemoryKeySpaceResolver() {
        return new AggregateKeySpaceResolver();
    }

    @Bean
    InMemoryMappingContext inMemoryMappingContext() {
        return new InMemoryMappingContext();
    }

    @Bean
    AggregateProperties inMemoryAggregateProperties(InMemoryMappingContext inMemoryMappingContext) {
        return new AggregateProperties(inMemoryMappingContext);
    }

    @Bean
    InMemoryStore inMemoryStore(
            AggregateKeySpaceResolver inMemoryKeySpaceResolver,
            AggregateProperties inMemoryAggregateProperties
    ) {
        return new InMemoryStore(inMemoryKeySpaceResolver, inMemoryAggregateProperties);
    }

    @Bean
    ExamplePredicate inMemoryExamplePredicate(
            InMemoryMappingContext inMemoryMappingContext,
            AggregateProperties inMemoryAggregateProperties
    ) {
        return new ExamplePredicate(inMemoryMappingContext, inMemoryAggregateProperties);
    }

    @Bean
    @ConditionalOnMissingBean(RangeQueryExecutor.class)
    InMemoryRangeQueryExecutor inMemoryRangeQueryExecutor(InMemoryStore inMemoryStore, ExamplePredicate examples) {
        return new InMemoryRangeQueryExecutor(inMemoryStore, examples);
    }

    @Bean
    @ConditionalOnMissingBean(FullTextQueryExecutor.class)
    InMemoryFullTextQueryExecutor inMemoryFullTextQueryExecutor(
            InMemoryStore inMemoryStore,
            InMemoryRangeQueryExecutor inMemoryRangeQueryExecutor
    ) {
        return new InMemoryFullTextQueryExecutor(inMemoryStore, inMemoryRangeQueryExecutor);
    }
}
