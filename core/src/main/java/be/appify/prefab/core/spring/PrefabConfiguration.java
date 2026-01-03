package be.appify.prefab.core.spring;

import be.appify.prefab.core.spring.converters.ByteArrayToFileConverter;
import be.appify.prefab.core.spring.converters.FileToByteArrayConverter;
import be.appify.prefab.core.spring.converters.ReferenceToStringConverter;
import be.appify.prefab.core.spring.converters.StringToReferenceConverter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;

import java.util.List;

/**
 * Spring configuration for Prefab core components.
 */
@Configuration
@ComponentScan({ "be.appify.prefab.core.spring", "be.appify.prefab.core.problem" })
public class PrefabConfiguration extends AbstractJdbcConfiguration {

    private final ReferenceFactory referenceFactory;

    /**
     * Constructs a new PrefabConfiguration with the given ReferenceFactory.
     * @param referenceFactory the ReferenceFactory to use for reference conversions
     */
    public PrefabConfiguration(ReferenceFactory referenceFactory) {
        this.referenceFactory = referenceFactory;
    }

    @Override
    public List<?> userConverters() {
        return List.of(
                new ReferenceToStringConverter(),
                new StringToReferenceConverter(referenceFactory),
                new FileToByteArrayConverter(),
                new ByteArrayToFileConverter()
        );
    }
}
