package be.appify.prefab.postgres.spring;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresDriverEnvironmentPostProcessorTest {

    private final PostgresDriverEnvironmentPostProcessor processor = new PostgresDriverEnvironmentPostProcessor();

    @Test
    void setsPostgresDriverClassNameByDefault() {
        var environment = new MockEnvironment();

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.driver-class-name"))
                .isEqualTo("org.postgresql.Driver");
    }

    @Test
    void developerSuppliedDriverClassNameTakesPrecedence() {
        var environment = new MockEnvironment();
        environment.setProperty("spring.datasource.driver-class-name", "com.example.CustomDriver");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.driver-class-name"))
                .isEqualTo("com.example.CustomDriver");
    }

    @Test
    void doesNotInterfereWithOtherDatasourceProperties() {
        var environment = new MockEnvironment();
        environment.setProperty("spring.datasource.url", "jdbc:postgresql://localhost/mydb");
        environment.setProperty("spring.datasource.username", "admin");
        environment.setProperty("spring.datasource.password", "secret");

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.url")).isEqualTo("jdbc:postgresql://localhost/mydb");
        assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("admin");
        assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("secret");
        assertThat(environment.getProperty("spring.datasource.driver-class-name"))
                .isEqualTo("org.postgresql.Driver");
    }

    @Test
    void isIdempotentWhenAppliedMultipleTimes() {
        var environment = new MockEnvironment();

        processor.postProcessEnvironment(environment, new SpringApplication());
        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getPropertySources())
                .filteredOn(ps -> ps.getName().equals("prefabPostgresDefaults"))
                .hasSize(1);
    }
}

