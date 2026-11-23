package be.appify.prefab.mongodb.diagnostics;

import be.appify.prefab.core.diagnostics.FailureReport;
import com.mongodb.MongoTimeoutException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/** Explains why the application cannot reach MongoDB when it needs it at startup. */
@Order(Ordered.HIGHEST_PRECEDENCE)
class MongoConnectionFailureAnalyzer extends AbstractFailureAnalyzer<MongoTimeoutException> {
    private static final FailureReport REPORT = new FailureReport(
            "The application could not connect to MongoDB within the server selection timeout.",
            "Check that MongoDB is running and reachable from this host, and that the host, port and credentials "
                    + "in spring.mongodb.uri are correct.");

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, MongoTimeoutException cause) {
        return REPORT.analysisOf(rootFailure);
    }
}
