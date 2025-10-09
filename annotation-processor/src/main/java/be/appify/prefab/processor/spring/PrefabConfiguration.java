package be.appify.prefab.processor.spring;

import be.appify.prefab.processor.spring.converters.ByteArrayToFileConverter;
import be.appify.prefab.processor.spring.converters.FileToByteArrayConverter;
import be.appify.prefab.processor.spring.converters.ReferenceToStringConverter;
import be.appify.prefab.processor.spring.converters.StringToReferenceConverter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;

import java.util.List;

@Configuration
@ComponentScan({ "be.appify.prefab.processor.spring", "be.appify.prefab.processor.problem" })
public class PrefabConfiguration extends AbstractJdbcConfiguration {

    private final ReferenceFactory referenceFactory;

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
