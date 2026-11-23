package be.appify.prefab.test.inmemory;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.transaction.PlatformTransactionManager;


/**
 * Registers the in-memory test infrastructure when {@code prefab.test.containers} is {@code false}.
 */
@AutoConfiguration(before = TransactionAutoConfiguration.class)
@Conditional(InMemoryTestAutoConfiguration.InMemoryModeCondition.class)
public class InMemoryTestAutoConfiguration {

    /** Constructs a new InMemoryTestAutoConfiguration. */
    public InMemoryTestAutoConfiguration() {
    }

    /**
     * Provides a transaction manager that needs no DataSource.
     *
     * @return the resource-less transaction manager
     */
    @Bean
    @ConditionalOnMissingBean(PlatformTransactionManager.class)
    InMemoryTransactionManager inMemoryTransactionManager() {
        return new InMemoryTransactionManager();
    }

    /** Matches when tests run in memory. */
    static class InMemoryModeCondition implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return TestInfrastructureMode.inMemory(context.getEnvironment());
        }
    }
}
