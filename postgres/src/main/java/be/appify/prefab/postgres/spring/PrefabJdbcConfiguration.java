package be.appify.prefab.postgres.spring;

import be.appify.prefab.core.spring.data.jdbc.PolymorphicReadingConverter;
import be.appify.prefab.postgres.spring.data.jdbc.ByteArrayToFileConverter;
import be.appify.prefab.postgres.spring.data.jdbc.FileToByteArrayConverter;
import be.appify.prefab.postgres.spring.data.jdbc.PrefabDataAccessStrategy;
import be.appify.prefab.postgres.spring.data.jdbc.PrefabJdbcAggregateTemplate;
import be.appify.prefab.postgres.spring.data.jdbc.PrefabJdbcMappingContext;
import be.appify.prefab.postgres.spring.data.jdbc.PrefabMappingJdbcConverter;
import be.appify.prefab.postgres.spring.data.jdbc.PrefabNamingStrategy;
import be.appify.prefab.postgres.spring.data.jdbc.SingleValueRecordSimpleTypeHolder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
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
import tools.jackson.databind.json.JsonMapper;

/**
 * Spring auto-configuration for Prefab PostgreSQL/JDBC persistence support.
 * <p>
 * Add {@code prefab-postgres} as a Maven dependency to enable PostgreSQL-backed repositories. This configuration is
 * automatically applied via Spring Boot's auto-configuration mechanism.
 * </p>
 */
@Configuration
@ComponentScan("be.appify.prefab.postgres.spring.data.jdbc")
public class PrefabJdbcConfiguration extends AbstractJdbcConfiguration {

    @Autowired(required = false)
    private List<PolymorphicReadingConverter> polymorphicReadingConverters = List.of();

    @Autowired
    private JsonMapper jsonMapper;

    /**
     * Constructs a new PrefabJdbcConfiguration.
     */
    public PrefabJdbcConfiguration() {
    }

    @Override
    public List<?> userConverters() {
        var converters = new ArrayList<>();
        converters.add(new FileToByteArrayConverter());
        converters.add(new ByteArrayToFileConverter());
        converters.addAll(polymorphicReadingConverters);
        return converters;
    }

    @Override
    public JdbcMappingContext jdbcMappingContext(
            Optional<NamingStrategy> namingStrategy,
            JdbcCustomConversions customConversions,
            RelationalManagedTypes jdbcManagedTypes
    ) {
        var mappingContext = new PrefabJdbcMappingContext(namingStrategy.orElse(new PrefabNamingStrategy()));
        mappingContext.setSimpleTypeHolder(new SingleValueRecordSimpleTypeHolder(customConversions.getSimpleTypeHolder()));
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
                new DefaultJdbcTypeFactory(operations.getJdbcOperations(), dialect.getArraySupport()),
                jsonMapper
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
