package be.appify.prefab.processor.spring;

import be.appify.prefab.core.domain.ReferenceProvider;
import be.appify.prefab.core.repository.Repository;
import be.appify.prefab.core.service.IdCache;
import be.appify.prefab.core.service.Reference;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class SpringDataReferenceProvider extends ReferenceProvider {
    private final Map<Class<?>, Repository<?>> referenceProviders = new HashMap<>();

    public void register(Repository<?> repository) {
        referenceProviders.put(repository.aggregateType(), repository);
    }

    public <T> Reference<T> referenceTo(AggregateReference<?, String> reference, Class<T> type) {
        if (reference == null) {
            return null;
        }
        return new SpringDataReference<>(reference.getId(), getRepository(type));
    }

    @SuppressWarnings("unchecked")
    private <T> Repository<T> getRepository(Class<T> type) {
        return (Repository<T>) referenceProviders.get(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Reference<T> referenceTo(T aggregate) {
        var repository = getRepository((Class<T>) aggregate.getClass());
        return new SpringDataReference<>(IdCache.INSTANCE.getId(aggregate), repository);
    }
}
