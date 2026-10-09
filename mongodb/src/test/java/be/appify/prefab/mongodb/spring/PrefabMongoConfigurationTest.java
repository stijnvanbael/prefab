package be.appify.prefab.mongodb.spring;

import be.appify.prefab.core.spring.PrefabCoreConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoExceptionTranslator;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PrefabMongoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PrefabMongoConfiguration.class));

    @Test
    void configuration_withoutPrefabCore_isInactive() {
        runner.run(context -> assertThat(context).doesNotHaveBean(PrefabMongoConfiguration.class));
    }

    @Test
    void configuration_withPrefabCore_isActive() {
        runner.withUserConfiguration(CoreMarker.class)
                .withBean(MongoDatabaseFactory.class, this::databaseFactory)
                .run(context -> assertThat(context).hasSingleBean(PrefabMongoConfiguration.class));
    }

    @Test
    void configuration_withoutSpringDataMongo_isInactive() {
        runner.withUserConfiguration(CoreMarker.class)
                .withClassLoader(new FilteredClassLoader(MongoMappingContext.class))
                .run(context -> assertThat(context).doesNotHaveBean(PrefabMongoConfiguration.class));
    }

    private MongoDatabaseFactory databaseFactory() {
        var factory = mock(MongoDatabaseFactory.class);
        when(factory.getExceptionTranslator()).thenReturn(new MongoExceptionTranslator());
        return factory;
    }

    @Configuration(proxyBeanMethods = false)
    static class CoreMarker {
        @Bean
        PrefabCoreConfiguration prefabCoreConfiguration() {
            return new PrefabCoreConfiguration();
        }
    }
}