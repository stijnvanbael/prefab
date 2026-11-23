package be.appify.prefab.postgres.diagnostics;

import be.appify.prefab.postgres.spring.data.jdbc.JdbcFullTextQueryExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.jdbc.core.dialect.JdbcPostgresDialect;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import static be.appify.prefab.postgres.diagnostics.StartupFailures.startExpectingFailure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class StartupDiagnosticsTest {

    @Test
    void unreachableDatabase(CapturedOutput output) {
        startExpectingFailure(MigratingApplication.class,
                "spring.datasource.url=jdbc:postgresql://localhost:1/app",
                "spring.datasource.username=app",
                "spring.datasource.password=secret");

        assertThat(output)
                .contains("The application could not connect to PostgreSQL")
                .contains("the host and port in spring.datasource.url are correct")
                .doesNotContain("secret");
    }

    @Test
    void missingDataSource(CapturedOutput output) {
        startExpectingFailure(ApplicationWithoutDataSource.class);

        assertThat(output)
                .contains("prefab-postgres needs a JDBC DataSource")
                .contains("spring.datasource.url (jdbc:postgresql://localhost:5432/<database>)");
    }

    @Test
    void unknownFullTextLanguage(CapturedOutput output) {
        startExpectingFailure(ApplicationWithUnknownLanguage.class);

        assertThat(output)
                .contains("Unknown PostgreSQL text search configuration 'klingon' in property prefab.full-text.language")
                .contains("Set prefab.full-text.language to the name of a text search configuration");
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration({DataSourceAutoConfiguration.class, FlywayAutoConfiguration.class})
    static class MigratingApplication {
    }

    @Configuration(proxyBeanMethods = false)
    static class ApplicationWithoutDataSource {
        @Bean
        Object repository(NamedParameterJdbcOperations jdbc) {
            return jdbc;
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ApplicationWithUnknownLanguage {
        @Bean
        JdbcFullTextQueryExecutor fullTextQueryExecutor() {
            var jdbc = mock(NamedParameterJdbcOperations.class);
            when(jdbc.queryForObject(any(String.class), any(SqlParameterSource.class), eq(Boolean.class)))
                    .thenReturn(false);
            var executor = new JdbcFullTextQueryExecutor(jdbc, mock(JdbcMappingContext.class),
                    mock(JdbcConverter.class), JdbcPostgresDialect.INSTANCE, "klingon");
            executor.validateConfiguration();
            return executor;
        }
    }
}
