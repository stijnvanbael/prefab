package be.appify.prefab.postgres.diagnostics;

import be.appify.prefab.core.diagnostics.FailureReport;
import be.appify.prefab.core.diagnostics.MissingBeanFailureAnalyzer;
import java.util.Map;

/** Explains missing JDBC beans in an application that uses {@code prefab-postgres}. */
class JdbcBeanFailureAnalyzer extends MissingBeanFailureAnalyzer {

    private static final FailureReport DATA_SOURCE = new FailureReport(
            "prefab-postgres needs a JDBC DataSource, but the application context does not contain one.",
            "Check that DataSourceAutoConfiguration is not excluded and that spring-boot-starter-data-jdbc is on the "
                    + "classpath. Set spring.datasource.url (jdbc:postgresql://localhost:5432/<database>), "
                    + "spring.datasource.username and spring.datasource.password, or define a DataSource bean.");

    JdbcBeanFailureAnalyzer() {
        super(Map.of(
                "javax.sql.DataSource", DATA_SOURCE,
                "org.springframework.jdbc.core.JdbcOperations", DATA_SOURCE,
                "org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations", DATA_SOURCE));
    }
}
