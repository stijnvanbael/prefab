package be.appify.prefab.asyncapi;

import be.appify.prefab.core.spring.PrefabCoreConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Import;

/**
 * Spring configuration for Prefab AsyncAPI documentation support.
 *
 * <p>Activates when {@code prefab-async-api} is on the classpath and {@link PrefabCoreConfiguration} is present,
 * via {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}.
 */
@AutoConfiguration
@ConditionalOnBean(PrefabCoreConfiguration.class)
@Import(AsyncApiController.class)
public class AsyncApiConfiguration {
}
