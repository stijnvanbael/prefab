package be.appify.prefab.core.spring;

import be.appify.prefab.core.spring.converters.ByteArrayToFileConverter;
import be.appify.prefab.core.spring.converters.FileToByteArrayConverter;
import be.appify.prefab.core.spring.converters.ReferenceToStringConverter;
import be.appify.prefab.core.spring.converters.StringToReferenceConverter;
import java.util.List;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;

/**
 * Spring configuration for Prefab core components.
 */
@Configuration
@ComponentScan({ "be.appify.prefab.core.spring", "be.appify.prefab.core.problem" })
public class PrefabConfiguration extends AbstractJdbcConfiguration {
    /**
     * Constructs a new PrefabConfiguration.
     */
    public PrefabConfiguration() {
    }

    @Override
    public List<?> userConverters() {
        return List.of(
                new ReferenceToStringConverter(),
                new StringToReferenceConverter(),
                new FileToByteArrayConverter(),
                new ByteArrayToFileConverter()
        );
    }
}
