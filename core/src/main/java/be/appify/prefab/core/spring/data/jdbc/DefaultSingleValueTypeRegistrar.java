package be.appify.prefab.core.spring.data.jdbc;

import be.appify.prefab.core.service.Reference;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Default {@link SingleValueTypeRegistrar} that registers {@link Reference} as a single-value type.
 */
@Component
public class DefaultSingleValueTypeRegistrar implements SingleValueTypeRegistrar {
    /**
     * Constructs a new DefaultSingleValueTypeRegistrar.
     */
    public DefaultSingleValueTypeRegistrar() {
    }

    @Override
    public List<Class<?>> singleValueTypes() {
        return List.of(Reference.class);
    }
}
