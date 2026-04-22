package be.appify.prefab.test.persistence;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresTestEnvironmentPostProcessorTest {

    private final PostgresTestEnvironmentPostProcessor processor = new PostgresTestEnvironmentPostProcessor();

    @Test
    void setsTcDriverClassNameByDefault() {
        var environment = environmentWithAppName("myapp");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.driver-class-name"))
                .isEqualTo("org.testcontainers.jdbc.ContainerDatabaseDriver");
    }

    @Test
    void setsTcUrlUsingApplicationName() {
        var environment = environmentWithAppName("myapp");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:tc:postgresql:16.1:///myapp?TC_REUSABLE=true&currentSchema=myapp");
    }

    @Test
    void convertsApplicationNameToLowercase() {
        var environment = environmentWithAppName("MyApp");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:tc:postgresql:16.1:///myapp?TC_REUSABLE=true&currentSchema=myapp");
    }

    @Test
    void convertsDotsAndDashesToUnderscoresInApplicationName() {
        var environment = environmentWithAppName("prefab.kafka-example");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:tc:postgresql:16.1:///prefab_kafka_example?TC_REUSABLE=true&currentSchema=prefab_kafka_example");
    }

    @Test
    void developerSuppliedUrlTakesPrecedence() {
        var environment = environmentWithAppName("myapp");
        environment.setProperty("spring.datasource.url", "jdbc:postgresql://custom-host/mydb");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://custom-host/mydb");
    }

    @Test
    void developerSuppliedDriverClassNameTakesPrecedence() {
        var environment = environmentWithAppName("myapp");
        environment.setProperty("spring.datasource.driver-class-name", "org.postgresql.Driver");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.driver-class-name"))
                .isEqualTo("org.postgresql.Driver");
    }

    @Test
    void fallsBackToApplicationWhenApplicationNameNotSet() {
        var environment = new MockEnvironment();

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:tc:postgresql:16.1:///application?TC_REUSABLE=true&currentSchema=application");
    }

    @Test
    void isIdempotentWhenAppliedMultipleTimes() {
        var environment = environmentWithAppName("myapp");

        processor.postProcessEnvironment(environment, new SpringApplication());
        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getPropertySources())
                .filteredOn(ps -> ps.getName().equals("prefabPostgresTestDefaults"))
                .hasSize(1);
    }

    @Test
    void tcDriverTakesPriorityOverProductionPostgresDefault() {
        var environment = environmentWithAppName("myapp");
        var postgresDefaults = new org.springframework.core.env.MapPropertySource(
                "prefabPostgresDefaults",
                Map.of("spring.datasource.driver-class-name", "org.postgresql.Driver")
        );
        environment.getPropertySources().addLast(postgresDefaults);

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.driver-class-name"))
                .isEqualTo("org.testcontainers.jdbc.ContainerDatabaseDriver");
    }

    private static MockEnvironment environmentWithAppName(String name) {
        var environment = new MockEnvironment();
        environment.setProperty("spring.application.name", name);
        return environment;
    }
}

