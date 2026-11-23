package be.appify.prefab.core.diagnostics;

import be.appify.prefab.core.spring.PrefabCoreConfiguration;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Explains a missing Prefab bean in an application that never enabled Prefab.
 *
 * <p>Every Prefab auto-configuration only activates next to {@link PrefabCoreConfiguration}, so without
 * {@code @EnablePrefab} the beans the generated code needs silently do not exist.</p>
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
class PrefabNotEnabledFailureAnalyzer extends AbstractFailureAnalyzer<NoSuchBeanDefinitionException>
        implements BeanFactoryAware {
    private static final String PREFAB_PACKAGE = "be.appify.prefab.";

    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, NoSuchBeanDefinitionException cause) {
        if (cause.getNumberOfBeansFound() > 1 || !isPrefabType(cause) || prefabIsEnabled()) {
            return null;
        }
        return new FailureAnalysis(
                "Prefab is not enabled: no bean of type %s was found, and the application context does not contain "
                        .formatted(cause.getBeanType().getName()) + "PrefabCoreConfiguration.",
                "Add @EnablePrefab to your @SpringBootApplication class "
                        + "(be.appify.prefab.core.spring.EnablePrefab).",
                rootFailure);
    }

    private static boolean isPrefabType(NoSuchBeanDefinitionException cause) {
        return cause.getBeanType() != null && cause.getBeanType().getName().startsWith(PREFAB_PACKAGE);
    }

    private boolean prefabIsEnabled() {
        return beanFactory instanceof ListableBeanFactory listable
                && listable.getBeanNamesForType(PrefabCoreConfiguration.class, true, false).length > 0;
    }
}
