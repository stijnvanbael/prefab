package be.appify.prefab.core.spring;

import be.appify.prefab.core.spring.data.jdbc.ByteArrayToFileConverter;
import be.appify.prefab.core.spring.data.jdbc.FileToByteArrayConverter;
import be.appify.prefab.core.spring.data.jdbc.PrefabJdbcMappingContext;
import be.appify.prefab.core.spring.data.jdbc.PrefabNamingStrategy;
import be.appify.prefab.core.spring.data.jdbc.ReferenceToStringConverter;
import be.appify.prefab.core.spring.data.jdbc.StringToReferenceConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.relational.RelationalManagedTypes;
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
        var mappingContext = new PrefabJdbcMappingContext(namingStrategy.orElse(new PrefabNamingStrategy()));
        mappingContext.setSimpleTypeHolder(customConversions.getSimpleTypeHolder());
        mappingContext.setManagedTypes(jdbcManagedTypes);
        return mappingContext;
    }
}
