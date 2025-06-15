package be.appify.prefab.processor.spring;

import be.appify.prefab.core.util.ServiceLocator;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class SpringServiceLocator implements ServiceLocator {
    private final ApplicationContext applicationContext;

    public SpringServiceLocator(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public <T> T getInstance(Class<T> serviceType) {
        return applicationContext.getBean(serviceType);
    }
}
