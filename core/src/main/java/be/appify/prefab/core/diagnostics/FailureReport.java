package be.appify.prefab.core.diagnostics;

import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * What to tell the developer about a startup failure: what is wrong and what to do about it.
 *
 * @param description
 *         what is wrong, naming the property or bean involved
 * @param action
 *         what to do about it
 */
public record FailureReport(String description, String action) {

    /**
     * Turns the report into a Spring Boot failure analysis.
     *
     * @param failure
     *         the original exception, which stays available for debugging
     * @return the failure analysis printed at startup
     */
    public FailureAnalysis analysisOf(Throwable failure) {
        return new FailureAnalysis(description, action, failure);
    }
}
