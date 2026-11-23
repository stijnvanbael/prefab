package be.appify.prefab.test.inmemory.persistence;

import java.beans.Introspector;
import java.util.List;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.util.ClassUtils;

/**
 * Registers an in-memory repository for every repository interface in the auto-configuration packages, that is, in
 * the packages of the application. The beans are registered programmatically, not through a Spring Data store module,
 * so that JDBC and MongoDB repository scanning is unaffected in containers mode.
 */
public class InMemoryRepositoriesRegistrar implements ImportBeanDefinitionRegistrar, BeanFactoryAware, EnvironmentAware {
    static final String STORE = "inMemoryStore";
    static final String EXAMPLES = "inMemoryExamplePredicate";

    private BeanFactory beanFactory;
    private Environment environment;

    /** Constructs a new InMemoryRepositoriesRegistrar. */
    public InMemoryRepositoriesRegistrar() {
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        var scanner = new RepositoryInterfaceScanner(environment);
        packages().stream()
                .flatMap(basePackage -> scanner.findCandidateComponents(basePackage).stream())
                .map(BeanDefinition::getBeanClassName)
                .distinct()
                .forEach(className -> register(className, registry));
    }

    private List<String> packages() {
        return AutoConfigurationPackages.has(beanFactory) ? AutoConfigurationPackages.get(beanFactory) : List.of();
    }

    private void register(String className, BeanDefinitionRegistry registry) {
        var type = ClassUtils.resolveClassName(className, getClass().getClassLoader());
        if (type.isAnnotationPresent(NoRepositoryBean.class) || type.getTypeParameters().length > 0) {
            return;
        }
        var definition = BeanDefinitionBuilder.rootBeanDefinition(InMemoryRepositoryFactoryBean.class)
                .addConstructorArgValue(className)
                .addPropertyValue("store", new RuntimeBeanReference(STORE))
                .addPropertyValue("examples", new RuntimeBeanReference(EXAMPLES))
                .getBeanDefinition();
        var simpleName = Introspector.decapitalize(type.getSimpleName());
        registry.registerBeanDefinition(registry.containsBeanDefinition(simpleName) ? className : simpleName, definition);
    }

    private static final class RepositoryInterfaceScanner extends ClassPathScanningCandidateComponentProvider {

        private RepositoryInterfaceScanner(Environment environment) {
            super(false, environment);
            addIncludeFilter(new AssignableTypeFilter(Repository.class));
        }

        @Override
        protected boolean isCandidateComponent(AnnotatedBeanDefinition definition) {
            return definition.getMetadata().isInterface() && definition.getMetadata().isIndependent();
        }
    }
}
