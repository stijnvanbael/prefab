package be.appify.prefab.core.spring;

import be.appify.prefab.core.util.ServiceLocator;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/** Spring-based implementation of ServiceLocator using ApplicationContext. */
@Component
public class SpringServiceLocator implements ServiceLocator {
    private final ApplicationContext applicationContext;

    SpringServiceLocator(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public <T> T getInstance(Class<T> serviceType) {
        return applicationContext.getBean(serviceType);
    }
}
