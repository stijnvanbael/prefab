package be.appify.prefab.core.spring;

import be.appify.prefab.core.spring.converters.ByteArrayToFileConverter;
import be.appify.prefab.core.spring.converters.FileToByteArrayConverter;
import be.appify.prefab.core.spring.converters.PrefabJdbcMappingContext;
import be.appify.prefab.core.spring.converters.ReferenceToStringConverter;
import be.appify.prefab.core.spring.converters.StringToReferenceConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.relational.RelationalManagedTypes;
import org.springframework.data.relational.core.mapping.DefaultNamingStrategy;
import org.springframework.data.relational.core.mapping.NamingStrategy;

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

    @Override
    public JdbcMappingContext jdbcMappingContext(
            Optional<NamingStrategy> namingStrategy,
            JdbcCustomConversions customConversions,
            RelationalManagedTypes jdbcManagedTypes
    ) {
        var mappingContext = new PrefabJdbcMappingContext(namingStrategy.orElse(new DefaultNamingStrategy()));
        mappingContext.setSimpleTypeHolder(customConversions.getSimpleTypeHolder());
        mappingContext.setManagedTypes(jdbcManagedTypes);
        return mappingContext;
    }
}
