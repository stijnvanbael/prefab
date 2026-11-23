package be.appify.prefab.test.inmemory;

import be.appify.prefab.core.spring.data.FullTextQueryExecutor;
import be.appify.prefab.core.spring.data.RangeQueryExecutor;
import be.appify.prefab.test.inmemory.persistence.AggregateKeySpaceResolver;
import be.appify.prefab.test.inmemory.persistence.AggregateProperties;
import be.appify.prefab.test.inmemory.persistence.ExamplePredicate;
import be.appify.prefab.test.inmemory.persistence.InMemoryFullTextQueryExecutor;
import be.appify.prefab.test.inmemory.persistence.InMemoryRangeQueryExecutor;
import be.appify.prefab.test.inmemory.persistence.InMemoryRepositoriesRegistrar;
import be.appify.prefab.test.inmemory.persistence.InMemoryStore;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;
import org.springframework.data.keyvalue.core.KeyValueAdapter;
import org.springframework.data.keyvalue.core.KeyValueOperations;
import org.springframework.data.keyvalue.core.KeyValueTemplate;
import org.springframework.data.keyvalue.core.PredicateQueryEngine;
import org.springframework.data.keyvalue.core.mapping.KeySpaceResolver;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.map.MapKeyValueAdapter;

/**
 * Backs every repository interface of the application with Spring Data KeyValue when {@code prefab.test.containers}
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
    KeySpaceResolver inMemoryKeySpaceResolver() {
        return new AggregateKeySpaceResolver();
    }

    @Bean
    KeyValueMappingContext<?, ?> inMemoryMappingContext(KeySpaceResolver inMemoryKeySpaceResolver) {
        var context = new KeyValueMappingContext<>();
        context.setKeySpaceResolver(inMemoryKeySpaceResolver);
        return context;
    }

    @Bean
    KeyValueAdapter inMemoryKeyValueAdapter() {
        return new MapKeyValueAdapter(ConcurrentHashMap.class, new PredicateQueryEngine());
    }

    @Bean
    KeyValueOperations inMemoryKeyValueTemplate(
            KeyValueAdapter inMemoryKeyValueAdapter,
            KeyValueMappingContext<?, ?> inMemoryMappingContext
    ) {
        return new KeyValueTemplate(inMemoryKeyValueAdapter, inMemoryMappingContext);
    }

    @Bean
    AggregateProperties inMemoryAggregateProperties(KeyValueMappingContext<?, ?> inMemoryMappingContext) {
        return new AggregateProperties(inMemoryMappingContext);
    }

    @Bean
    InMemoryStore inMemoryStore(
            KeyValueAdapter inMemoryKeyValueAdapter,
            KeySpaceResolver inMemoryKeySpaceResolver,
            AggregateProperties inMemoryAggregateProperties
    ) {
        return new InMemoryStore(inMemoryKeyValueAdapter, inMemoryKeySpaceResolver, inMemoryAggregateProperties);
    }

    @Bean
    ExamplePredicate inMemoryExamplePredicate(
            KeyValueMappingContext<?, ?> inMemoryMappingContext,
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
