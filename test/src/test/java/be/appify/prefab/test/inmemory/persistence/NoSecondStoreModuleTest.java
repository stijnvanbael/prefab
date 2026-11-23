package be.appify.prefab.test.inmemory.persistence;

import be.appify.prefab.test.inmemory.InMemoryPersistenceAutoConfiguration;
import be.appify.prefab.test.inmemory.persistence.fixture.FixtureConfiguration;
import be.appify.prefab.test.inmemory.persistence.fixture.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test: Spring Data counts the {@code RepositoryFactorySupport} entries in all {@code spring.factories}
 * files to find out whether more than one store module is present. With a second one next to JDBC or MongoDB it
 * enters strict mode, in which only repositories of annotated types or of store specific interfaces are claimed. The
 * generated repositories are neither, so every application in containers mode would lose its repositories. The
 * in-memory backend therefore must not bring a store module: no {@code spring-data-keyvalue}, and no registration of
 * its own factory, which {@link InMemoryRepositoriesRegistrar} creates instead.
 */
class NoSecondStoreModuleTest {

    @Test
    void repositoryFactoryRegistrations_containNoKeyValueAndNoPrefabFactory() {
        var factories = SpringFactoriesLoader.loadFactoryNames(
                RepositoryFactorySupport.class, getClass().getClassLoader());

        assertThat(factories)
                .noneMatch(name -> name.startsWith("org.springframework.data.keyvalue"))
                .noneMatch(name -> name.startsWith("be.appify.prefab"));
    }

    @Test
    void registrar_stillCreatesTheRepositoryBeansWithoutAFactoriesEntry() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(InMemoryPersistenceAutoConfiguration.class))
                .withUserConfiguration(FixtureConfiguration.class)
                .withPropertyValues("prefab.test.containers=false")
                .run(context -> assertThat(context).hasSingleBean(OrderRepository.class));
    }
}
