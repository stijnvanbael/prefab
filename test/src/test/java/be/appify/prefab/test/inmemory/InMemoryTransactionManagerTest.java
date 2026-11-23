package be.appify.prefab.test.inmemory;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class InMemoryTransactionManagerTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    InMemoryTestAutoConfiguration.class, TransactionAutoConfiguration.class))
            .withUserConfiguration(ServiceConfiguration.class);

    @Test
    void transactionManagerIsRegisteredInMemoryMode() {
        runner.withPropertyValues("prefab.test.containers=false")
                .run(context -> assertThat(context).hasSingleBean(InMemoryTransactionManager.class));
    }

    @Test
    void transactionManagerIsAbsentInContainersMode() {
        runner.withPropertyValues("prefab.test.containers=true")
                .run(context -> assertThat(context).doesNotHaveBean(PlatformTransactionManager.class));
    }

    @Test
    void transactionManagerBacksOffWhenAnotherOneExists() {
        runner.withPropertyValues("prefab.test.containers=false")
                .withBean("custom", PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
                .run(context -> assertThat(context).hasSingleBean(PlatformTransactionManager.class)
                        .doesNotHaveBean(InMemoryTransactionManager.class));
    }

    @Test
    void transactionalServiceRunsAfterCommitCallbackOnCommit() {
        runner.withPropertyValues("prefab.test.containers=false").run(context -> {
            var service = context.getBean(Service.class);

            service.work();

            assertThat(service.events()).containsExactly("body", "afterCommit");
        });
    }

    @Test
    void rollbackSkipsAfterCommitCallback() {
        runner.withPropertyValues("prefab.test.containers=false").run(context -> {
            var service = context.getBean(Service.class);

            assertThatThrownBy(service::fail).isInstanceOf(IllegalStateException.class);

            assertThat(service.events()).containsExactly("body", "afterCompletion");
        });
    }

    @Test
    void nestedTransactionParticipatesInOuterTransaction() {
        runner.withPropertyValues("prefab.test.containers=false").run(context -> {
            var service = context.getBean(Service.class);

            service.outer();

            assertThat(service.events()).containsExactly("inner", "afterCommit");
        });
    }

    @Test
    void requiresNewSuspendsOuterTransaction() {
        runner.withPropertyValues("prefab.test.containers=false").run(context -> {
            var template = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
            var events = new ArrayList<String>();

            template.executeWithoutResult(outer -> {
                var requiresNew = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
                requiresNew.setPropagationBehavior(
                        TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                requiresNew.executeWithoutResult(inner -> events.add("inner"));
                events.add("outer-still-active=" + TransactionSynchronizationManager.isActualTransactionActive());
            });

            assertThat(events).containsExactly("inner", "outer-still-active=true");
        });
    }

    @Configuration
    static class ServiceConfiguration {
        @Bean
        Service service() {
            return new Service();
        }
    }

    public static class Service {
        private final List<String> events = new ArrayList<>();

        public List<String> events() {
            return events;
        }

        @Transactional
        public void work() {
            events.add("body");
            registerCallbacks();
        }

        @Transactional
        public void fail() {
            events.add("body");
            registerCallbacks();
            throw new IllegalStateException("boom");
        }

        @Transactional
        public void outer() {
            inner();
        }

        @Transactional
        public void inner() {
            events.add("inner");
            registerCallbacks();
        }

        private void registerCallbacks() {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    events.add("afterCommit");
                }

                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        events.add("afterCompletion");
                    }
                }
            });
        }
    }
}
