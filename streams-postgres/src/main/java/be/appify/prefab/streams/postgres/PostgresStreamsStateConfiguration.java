package be.appify.prefab.streams.postgres;

import be.appify.prefab.streams.engine.JsonStreamStateCodec;
import be.appify.prefab.streams.engine.StreamStateCodec;
import be.appify.prefab.streams.engine.StreamStateRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/** Registers the Postgres {@link StreamStateRepository} when a JDBC template and transaction manager exist. */
@AutoConfiguration(afterName = {
        "org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration",
        "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration"
})
@ConditionalOnClass(JdbcTemplate.class)
@ConditionalOnBean({JdbcTemplate.class, PlatformTransactionManager.class})
@EnableConfigurationProperties(PostgresStreamsProperties.class)
public class PostgresStreamsStateConfiguration {

    @Bean
    @ConditionalOnMissingBean(StreamStateCodec.class)
    StreamStateCodec streamStateCodec() {
        return new JsonStreamStateCodec();
    }

    @Bean
    @ConditionalOnMissingBean(StreamStateRepository.class)
    PostgresStreamStateRepository postgresStreamStateRepository(
            JdbcTemplate jdbc,
            PlatformTransactionManager transactionManager,
            StreamStateCodec codec,
            PostgresStreamsProperties properties
    ) {
        if (properties.initializeSchema() == PostgresStreamsProperties.InitializeSchema.ALWAYS) {
            PostgresStreamSchema.initialize(jdbc);
        }
        return new PostgresStreamStateRepository(jdbc, transactionManager, codec);
    }
}
