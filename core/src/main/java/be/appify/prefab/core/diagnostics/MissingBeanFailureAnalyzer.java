package be.appify.prefab.core.diagnostics;

import java.util.Map;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Base class for analyzers that explain why a bean Prefab depends on is missing.
 *
 * <p>Each subclass names the bean types it knows about, by class name so that the analyzer also loads when the
 * library providing the type is not on the classpath. Types it does not know are left to Spring Boot's own
 * analysis.</p>
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public abstract class MissingBeanFailureAnalyzer extends AbstractFailureAnalyzer<NoSuchBeanDefinitionException> {
    private final Map<String, FailureReport> reportsByType;

    /**
     * Constructs a new MissingBeanFailureAnalyzer.
     *
     * @param reportsByType
     *         the report to give for each fully qualified bean type name
     */
    protected MissingBeanFailureAnalyzer(Map<String, FailureReport> reportsByType) {
        this.reportsByType = Map.copyOf(reportsByType);
    }

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, NoSuchBeanDefinitionException cause) {
        if (cause.getNumberOfBeansFound() > 1 || cause.getBeanType() == null) {
            return null;
        }
        var report = reportsByType.get(cause.getBeanType().getName());
        return report == null ? null : report.analysisOf(rootFailure);
    }
}
