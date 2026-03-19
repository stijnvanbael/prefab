package be.appify.prefab.core.spring;

import be.appify.prefab.core.spring.data.jdbc.ByteArrayToFileConverter;
import be.appify.prefab.core.spring.data.jdbc.FileToByteArrayConverter;
import be.appify.prefab.core.spring.data.jdbc.PrefabDataAccessStrategy;
import be.appify.prefab.core.spring.data.jdbc.PrefabJdbcAggregateTemplate;
import be.appify.prefab.core.spring.data.jdbc.PrefabJdbcMappingContext;
import be.appify.prefab.core.spring.data.jdbc.PrefabMappingJdbcConverter;
import be.appify.prefab.core.spring.data.jdbc.PrefabNamingStrategy;
import be.appify.prefab.core.spring.data.jdbc.ReferenceToStringConverter;
import be.appify.prefab.core.spring.data.jdbc.StringToReferenceConverter;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.jdbc.core.convert.DataAccessStrategy;
import org.springframework.data.jdbc.core.convert.DefaultJdbcTypeFactory;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions;
import org.springframework.data.jdbc.core.convert.RelationResolver;
import org.springframework.data.jdbc.core.dialect.JdbcDialect;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.relational.RelationalManagedTypes;
import org.springframework.data.relational.core.mapping.NamingStrategy;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

/**
 * Spring configuration for Prefab core components.
 */
@Configuration
@ComponentScan("be.appify.prefab.core.spring")
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

    @Bean
    @Override
    public JdbcConverter jdbcConverter(
            JdbcMappingContext mappingContext,
            NamedParameterJdbcOperations operations,
            @Lazy RelationResolver relationResolver,
            JdbcCustomConversions conversions,
            JdbcDialect dialect
    ) {
        return new PrefabMappingJdbcConverter(
                mappingContext,
                relationResolver,
                conversions,
                new DefaultJdbcTypeFactory(operations.getJdbcOperations(), dialect.getArraySupport())
        );
    }

    @Bean
    @Override
    public DataAccessStrategy dataAccessStrategyBean(
            NamedParameterJdbcOperations operations,
            JdbcConverter jdbcConverter,
            JdbcMappingContext context,
            JdbcDialect dialect
    ) {
        DataAccessStrategy defaultStrategy = super.dataAccessStrategyBean(operations, jdbcConverter, context, dialect);
        return new PrefabDataAccessStrategy(defaultStrategy);
    }

    @Bean
    @Override
    public JdbcAggregateTemplate jdbcAggregateTemplate(
            ApplicationContext applicationContext,
            JdbcMappingContext mappingContext,
            JdbcConverter converter,
            DataAccessStrategy dataAccessStrategy
    ) {
        return new PrefabJdbcAggregateTemplate(applicationContext, mappingContext, converter, dataAccessStrategy);
    }
}
