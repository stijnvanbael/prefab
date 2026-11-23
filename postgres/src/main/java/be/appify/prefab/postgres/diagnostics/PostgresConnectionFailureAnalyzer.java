package be.appify.prefab.postgres.diagnostics;

import be.appify.prefab.core.diagnostics.FailureReport;
import java.util.Map;
import org.postgresql.util.PSQLException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Explains why the application cannot connect to PostgreSQL at startup.
 *
 * <p>The report names the failing step, from the SQLState reported by the driver, without repeating the connection
 * URL or credentials.</p>
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
class PostgresConnectionFailureAnalyzer extends AbstractFailureAnalyzer<PSQLException> {
    private static final String CONNECTION_SETTINGS = "spring.datasource.url, spring.datasource.username and "
            + "spring.datasource.password";

    private static final Map<String, FailureReport> REPORTS_BY_SQL_STATE = Map.of(
            "08001", new FailureReport(
                    "The application could not connect to PostgreSQL.",
                    "Check that the database is running and reachable from this host, and that the host and port in "
                            + "spring.datasource.url are correct."),
            "28000", authenticationFailed(),
            "28P01", authenticationFailed(),
            "3D000", new FailureReport(
                    "The PostgreSQL database named in spring.datasource.url does not exist.",
                    "Create the database, or correct the database name in spring.datasource.url."));

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, PSQLException cause) {
        var report = REPORTS_BY_SQL_STATE.get(cause.getSQLState());
        return report == null ? null : report.analysisOf(rootFailure);
    }

    private static FailureReport authenticationFailed() {
        return new FailureReport(
                "PostgreSQL rejected the credentials of the application.",
                "Check " + CONNECTION_SETTINGS + ", and that the user may connect to the database.");
    }
}
