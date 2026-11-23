package be.appify.prefab.core.diagnostics;

import java.util.Map;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.util.PlaceholderResolutionException;

/** Explains the required properties Prefab reads without a default. */
class MissingPropertyFailureAnalyzer extends AbstractFailureAnalyzer<PlaceholderResolutionException> {

    private static final Map<String, FailureReport> REPORTS = Map.of(
            "spring.application.name", new FailureReport(
                    "The property spring.application.name is not set. Prefab derives the Kafka consumer group, "
                            + "the dead-letter topic and the Pub/Sub and SQS subscription names from it.",
                    "Set spring.application.name in application.yml, for example spring.application.name: my-app."),
            "spring.cloud.gcp.project-id", new FailureReport(
                    "The property spring.cloud.gcp.project-id is not set. Prefab needs it to create Pub/Sub topics "
                            + "and subscriptions.",
                    "Set spring.cloud.gcp.project-id to your Google Cloud project, or to any name when you use "
                            + "the Pub/Sub emulator."));

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, PlaceholderResolutionException cause) {
        var report = REPORTS.get(cause.getPlaceholder());
        return report == null ? null : report.analysisOf(rootFailure);
    }
}
