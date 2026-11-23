package be.appify.prefab.core.diagnostics;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/** Reports a {@link PrefabConfigurationException} with its description and action. */
class PrefabConfigurationFailureAnalyzer extends AbstractFailureAnalyzer<PrefabConfigurationException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, PrefabConfigurationException cause) {
        return new FailureAnalysis(cause.description(), cause.action(), rootFailure);
    }
}
