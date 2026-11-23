package be.appify.prefab.mongodb.diagnostics;

import be.appify.prefab.core.diagnostics.FailureReport;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Explains a MongoDB connection string the driver refuses.
 *
 * <p>The report does not repeat the connection string, because it may contain credentials.</p>
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
class MongoConnectionStringFailureAnalyzer extends AbstractFailureAnalyzer<IllegalArgumentException> {
    private static final String DRIVER_MESSAGE = "connection string is invalid";

    private static final FailureReport REPORT = new FailureReport(
            "The MongoDB connection string in spring.mongodb.uri is invalid.",
            "Use the form mongodb://[user:password@]host[:port]/<database>[?options], or mongodb+srv://... "
                    + "for a DNS seed list. Percent-encode special characters in the user name and password.");

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, IllegalArgumentException cause) {
        return cause.getMessage() != null && cause.getMessage().contains(DRIVER_MESSAGE)
                ? REPORT.analysisOf(rootFailure)
                : null;
    }
}
