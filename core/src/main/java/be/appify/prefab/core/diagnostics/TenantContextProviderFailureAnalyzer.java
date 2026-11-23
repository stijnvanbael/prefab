package be.appify.prefab.core.diagnostics;

import be.appify.prefab.core.tenant.TenantContextProvider;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/** Explains why the application has more than one {@link TenantContextProvider}. */
class TenantContextProviderFailureAnalyzer extends AbstractFailureAnalyzer<NoUniqueBeanDefinitionException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, NoUniqueBeanDefinitionException cause) {
        if (cause.getBeanType() == null || !TenantContextProvider.class.isAssignableFrom(cause.getBeanType())) {
            return null;
        }
        return new FailureAnalysis(
                "Multi-tenancy needs exactly one TenantContextProvider, but the application context contains "
                        + cause.getNumberOfBeansFound() + ": " + cause.getBeanNamesFound() + ".",
                "Remove all but one TenantContextProvider bean, or mark the one to use as @Primary.",
                rootFailure);
    }
}
