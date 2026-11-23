package be.appify.prefab.core.diagnostics;

import be.appify.prefab.core.util.UnknownClassException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/** Explains a class name in a Prefab property that cannot be loaded. */
class UnknownClassFailureAnalyzer extends AbstractFailureAnalyzer<UnknownClassException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, UnknownClassException cause) {
        return new FailureAnalysis(
                "The class [%s] could not be loaded. Prefab loads classes by name from the property "
                        .formatted(cause.className()) + "prefab.dlt.non-retryable-exceptions.",
                "Use the fully qualified name of an exception class on the classpath, separated by commas, "
                        + "and use $ for nested classes (com.acme.Outer$Inner).",
                rootFailure);
    }
}
